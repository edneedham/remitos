package handlers

import (
	"context"
	"fmt"
	"io"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"

	"cloud.google.com/go/storage"
	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
	"server/internal/logger"
	"server/internal/middleware"
	"server/internal/repository"
)

// ImageHandler handles image upload and retrieval operations
type ImageHandler struct {
	imageRepo     *repository.ImageRepository
	deviceRepo    *repository.DeviceRepository
	storageClient *storage.Client
	bucketName    string
}

// NewImageHandler creates a new ImageHandler
// Returns nil if GCS is not configured, allowing the app to start without image upload functionality
func NewImageHandler(imageRepo *repository.ImageRepository, deviceRepo *repository.DeviceRepository) (*ImageHandler, error) {
	bucketName := os.Getenv("GCS_BUCKET_NAME")
	if bucketName == "" {
		// GCS not configured, return nil but no error - app can still start
		return nil, nil
	}

	ctx := context.Background()

	// Initialize GCS client
	// On Cloud Run, this uses the default service account automatically
	// On local dev, it uses GOOGLE_APPLICATION_CREDENTIALS if set
	client, err := storage.NewClient(ctx)
	if err != nil {
		return nil, fmt.Errorf("failed to create storage client: %w", err)
	}

	return &ImageHandler{
		imageRepo:     imageRepo,
		deviceRepo:    deviceRepo,
		storageClient: client,
		bucketName:    bucketName,
	}, nil
}

// UploadImageRequest represents the request to upload an image
type UploadImageRequest struct {
	EntityType string `json:"entity_type"`
	EntityID   int64  `json:"entity_id"`
}

// UploadImageResponse represents the response after uploading an image
type UploadImageResponse struct {
	ImageID   string `json:"image_id"`
	SignedURL string `json:"signed_url"`
	GcsPath   string `json:"gcs_path"`
	ExpiresAt string `json:"expires_at"`
}

// SignedURLResponse represents a generated signed URL
type SignedURLResponse struct {
	SignedURL string `json:"signed_url"`
	ExpiresAt string `json:"expires_at"`
}

// Upload handles image upload to Google Cloud Storage
func (h *ImageHandler) Upload(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()

	// Get user claims for authorization
	claims := middleware.GetUserClaims(r)
	if claims.UserID == "" {
		RespondWithError(w, ErrCodeUnauthorized, "No autorizado", http.StatusUnauthorized)
		return
	}

	// Get device ID from header to determine warehouse
	deviceIDHeader := r.Header.Get("X-Device-ID")
	if deviceIDHeader == "" {
		RespondWithError(w, ErrCodeInvalidRequest, "X-Device-ID header requerido", http.StatusBadRequest)
		return
	}

	deviceID, err := uuid.Parse(deviceIDHeader)
	if err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "ID de dispositivo inválido", http.StatusBadRequest)
		return
	}

	// Get device to determine warehouse
	device, err := h.deviceRepo.GetByID(ctx, deviceID)
	if err != nil || device == nil {
		RespondWithError(w, ErrCodeNotFound, "Dispositivo no encontrado", http.StatusNotFound)
		return
	}

	// Parse multipart form with 10MB max memory
	if err := r.ParseMultipartForm(10 << 20); err != nil {
		logger.Log.Error().Err(err).Msg("Failed to parse multipart form")
		RespondWithError(w, ErrCodeInvalidRequest, "Error al procesar la imagen", http.StatusBadRequest)
		return
	}

	// Get uploaded file
	file, header, err := r.FormFile("image")
	if err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "No se pudo obtener la imagen", http.StatusBadRequest)
		return
	}
	defer file.Close()

	// Validate file size (max 10MB)
	if header.Size > 10<<20 {
		RespondWithError(w, ErrCodeInvalidRequest, "La imagen es demasiado grande (máximo 10MB)", http.StatusBadRequest)
		return
	}

	// Validate content type
	contentType := header.Header.Get("Content-Type")
	if !isValidImageType(contentType) {
		RespondWithError(w, ErrCodeInvalidRequest, "Tipo de archivo no válido. Solo se permiten imágenes JPEG y PNG", http.StatusBadRequest)
		return
	}

	// Get entity information
	entityType := r.FormValue("entity_type")
	entityIDStr := r.FormValue("entity_id")

	if entityType == "" || entityIDStr == "" {
		RespondWithError(w, ErrCodeInvalidRequest, "Se requiere entity_type y entity_id", http.StatusBadRequest)
		return
	}

	entityID, err := strconv.ParseInt(entityIDStr, 10, 64)
	if err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "entity_id debe ser un número", http.StatusBadRequest)
		return
	}

	// Validate entity type
	if !isValidEntityType(entityType) {
		RespondWithError(w, ErrCodeInvalidRequest, "Tipo de entidad no válido", http.StatusBadRequest)
		return
	}

	// Read file data
	data, err := io.ReadAll(file)
	if err != nil {
		logger.Log.Error().Err(err).Msg("Failed to read image data")
		RespondWithError(w, ErrCodeInternalError, "Error al leer la imagen", http.StatusInternalServerError)
		return
	}

	// Get warehouse ID from device
	warehouseID := device.WarehouseID
	userID, _ := uuid.Parse(claims.UserID)

	// Generate GCS path (São Paulo region - southamerica-east1)
	timestamp := time.Now().Format("20060102_150405")
	gcsPath := fmt.Sprintf("remitos/%s/%s/%s_%s_%d.jpg",
		warehouseID.String(),
		time.Now().Format("2006/01/02"),
		entityType,
		timestamp,
		entityID,
	)

	// Upload to GCS
	bucket := h.storageClient.Bucket(h.bucketName)
	obj := bucket.Object(gcsPath)

	writer := obj.NewWriter(ctx)
	writer.ContentType = contentType
	writer.Metadata = map[string]string{
		"entity_type": entityType,
		"entity_id":   entityIDStr,
		"uploaded_by": claims.UserID,
	}

	if _, err := writer.Write(data); err != nil {
		logger.Log.Error().Err(err).Msg("Failed to write to GCS")
		RespondWithError(w, ErrCodeInternalError, "Error al subir la imagen", http.StatusInternalServerError)
		return
	}

	if err := writer.Close(); err != nil {
		logger.Log.Error().Err(err).Msg("Failed to close GCS writer")
		RespondWithError(w, ErrCodeInternalError, "Error al finalizar la subida", http.StatusInternalServerError)
		return
	}

	// Create database record
	image := &repository.Image{
		ID:           uuid.New(),
		GcsPath:      gcsPath,
		ContentType:  contentType,
		FileSize:     header.Size,
		EntityType:   entityType,
		EntityID:     entityID,
		WarehouseID:  &warehouseID,
		UploadedBy:   &userID,
		UploadedAt:   time.Now(),
		StorageClass: "STANDARD",
	}

	if err := h.imageRepo.Create(ctx, image); err != nil {
		logger.Log.Error().Err(err).Msg("Failed to save image record")
		// Try to delete the GCS object since DB save failed
		_ = obj.Delete(ctx)
		RespondWithError(w, ErrCodeInternalError, "Error al guardar información de la imagen", http.StatusInternalServerError)
		return
	}

	// Generate signed URL
	signedURL, expiresAt, err := h.generateSignedURL(ctx, gcsPath)
	if err != nil {
		logger.Log.Error().Err(err).Msg("Failed to generate signed URL")
		RespondWithError(w, ErrCodeInternalError, "Error al generar URL de acceso", http.StatusInternalServerError)
		return
	}

	logger.Log.Info().
		Str("image_id", image.ID.String()).
		Str("gcs_path", gcsPath).
		Str("entity_type", entityType).
		Int64("entity_id", entityID).
		Msg("Image uploaded successfully")

	resp := UploadImageResponse{
		ImageID:   image.ID.String(),
		SignedURL: signedURL,
		GcsPath:   gcsPath,
		ExpiresAt: expiresAt.Format(time.RFC3339),
	}

	RespondWithJSON(w, http.StatusCreated, resp)
}

// GetSignedURL generates a new signed URL for an existing image
func (h *ImageHandler) GetSignedURL(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()

	// Get user claims for authorization
	claims := middleware.GetUserClaims(r)
	if claims.UserID == "" {
		RespondWithError(w, ErrCodeUnauthorized, "No autorizado", http.StatusUnauthorized)
		return
	}

	// Get image ID from URL
	imageID := chi.URLParam(r, "id")
	if imageID == "" {
		RespondWithError(w, ErrCodeInvalidRequest, "ID de imagen requerido", http.StatusBadRequest)
		return
	}

	id, err := uuid.Parse(imageID)
	if err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "ID de imagen inválido", http.StatusBadRequest)
		return
	}

	// Get image from database
	image, err := h.imageRepo.GetByID(ctx, id)
	if err != nil {
		logger.Log.Error().Err(err).Str("image_id", imageID).Msg("Image not found")
		RespondWithError(w, ErrCodeNotFound, "Imagen no encontrada", http.StatusNotFound)
		return
	}

	// Note: For now, we rely on the fact that the user has a valid JWT token
	// to access images. For more granular control, you could check if the
	// image belongs to the user's company/warehouse by joining with the entity table.

	// Generate signed URL
	signedURL, expiresAt, err := h.generateSignedURL(ctx, image.GcsPath)
	if err != nil {
		logger.Log.Error().Err(err).Msg("Failed to generate signed URL")
		RespondWithError(w, ErrCodeInternalError, "Error al generar URL de acceso", http.StatusInternalServerError)
		return
	}

	resp := SignedURLResponse{
		SignedURL: signedURL,
		ExpiresAt: expiresAt.Format(time.RFC3339),
	}

	RespondWithJSON(w, http.StatusOK, resp)
}

// generateSignedURL creates a signed URL for accessing a GCS object
func (h *ImageHandler) generateSignedURL(ctx context.Context, gcsPath string) (string, time.Time, error) {
	// Get expiry duration from environment or use default (7 days)
	expiryHours := 168 // 7 days
	if hours := os.Getenv("GCS_SIGNED_URL_EXPIRY_HOURS"); hours != "" {
		if h, err := strconv.Atoi(hours); err == nil {
			expiryHours = h
		}
	}

	expiresAt := time.Now().Add(time.Duration(expiryHours) * time.Hour)

	bucket := h.storageClient.Bucket(h.bucketName)

	// Generate signed URL using the bucket
	url, err := bucket.SignedURL(gcsPath, &storage.SignedURLOptions{
		Method:  "GET",
		Expires: expiresAt,
	})

	if err != nil {
		return "", time.Time{}, fmt.Errorf("failed to generate signed URL: %w", err)
	}

	return url, expiresAt, nil
}

// isValidImageType checks if the content type is a valid image type
func isValidImageType(contentType string) bool {
	validTypes := []string{
		"image/jpeg",
		"image/jpg",
		"image/png",
		"image/webp",
	}

	for _, valid := range validTypes {
		if strings.EqualFold(contentType, valid) {
			return true
		}
	}

	return false
}

// isValidEntityType checks if the entity type is valid
func isValidEntityType(entityType string) bool {
	validTypes := []string{
		"inbound_note",
		"outbound_list",
		"scan",
	}

	for _, valid := range validTypes {
		if entityType == valid {
			return true
		}
	}

	return false
}

// Routes returns the router for image endpoints
func (h *ImageHandler) Routes() *chi.Mux {
	r := chi.NewRouter()
	r.Post("/upload", h.Upload)
	r.Get("/{id}/url", h.GetSignedURL)
	return r
}
