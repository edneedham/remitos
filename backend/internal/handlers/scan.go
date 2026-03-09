package handlers

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"io"
	"net/http"
	"strconv"
	"time"

	vision "cloud.google.com/go/vision/apiv1"
	"cloud.google.com/go/vision/v2/apiv1/visionpb"
	"github.com/go-chi/chi/v5"
	"server/internal/logger"
)

type ScanHandler struct {
	visionClient *vision.ImageAnnotatorClient
}

func NewScanHandler() (*ScanHandler, error) {
	ctx := context.Background()
	client, err := vision.NewImageAnnotatorClient(ctx)
	if err != nil {
		return nil, err
	}

	return &ScanHandler{
		visionClient: client,
	}, nil
}

type ScanRequest struct {
	Text       string             `json:"text"`
	Confidence map[string]float64 `json:"confidence"`
}

type ScanResponse struct {
	Text        string             `json:"text"`
	Fields      map[string]string  `json:"fields"`
	Confidence  map[string]float64 `json:"confidence"`
	Source      string             `json:"source"`
	ProcessedAt time.Time          `json:"processed_at"`
}

func (h *ScanHandler) Scan(w http.ResponseWriter, r *http.Request) {
	ctx, cancel := context.WithTimeout(r.Context(), 30*time.Second)
	defer cancel()

	contentType := r.Header.Get("Content-Type")
	isMultipart := len(contentType) > 19 && contentType[0:19] == "multipart/form-data"

	var imageData []byte = nil
	var filename string

	if isMultipart {
		file, header, err := r.FormFile("image")
		if err != nil {
			RespondWithError(w, ErrCodeInvalidRequest, "No se pudo obtener la imagen", http.StatusBadRequest)
			return
		}
		defer file.Close()
		filename = header.Filename

		imageData, err = io.ReadAll(file)
		if err != nil {
			logger.Log.Error().Err(err).Msg("Failed to read image data")
			RespondWithError(w, ErrCodeInternalError, "Error al leer la imagen", http.StatusInternalServerError)
			return
		}
	} else {
		var req ScanRequest
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
			RespondWithError(w, ErrCodeInvalidRequest, "Cuerpo de solicitud inválido", http.StatusBadRequest)
			return
		}

		logger.Log.Info().Msg("Received OCR data from client")

		minConfidence := 0.9
		if conf := r.URL.Query().Get("min_confidence"); conf != "" {
			if parsed, err := strconv.ParseFloat(conf, 64); err == nil {
				minConfidence = parsed
			}
		}

		if meetsThreshold(req.Confidence, minConfidence) {
			logger.Log.Info().Float64("confidence", getAverageConfidence(req.Confidence)).Msg("Client OCR meets threshold, using local data")

			fields := map[string]string{"text": req.Text}
			scanResp := ScanResponse{
				Text:        req.Text,
				Fields:      fields,
				Confidence:  req.Confidence,
				Source:      "mlkit",
				ProcessedAt: time.Now(),
			}
			RespondWithJSON(w, http.StatusOK, scanResp)
			return
		}

		logger.Log.Info().Float64("confidence", getAverageConfidence(req.Confidence)).Float64("threshold", minConfidence).Msg("Client OCR below threshold, processing with Cloud Vision")

		imageBase64 := r.URL.Query().Get("image_base64")
		if imageBase64 == "" {
			RespondWithError(w, ErrCodeInvalidRequest, "Imagen requerida cuando el OCR local no cumple el umbral", http.StatusBadRequest)
			return
		}

		var err error
		imageData, err = base64.StdEncoding.DecodeString(imageBase64)
		if err != nil {
			logger.Log.Error().Err(err).Msg("Failed to decode base64 image")
			RespondWithError(w, ErrCodeInternalError, "Error al decodificar la imagen", http.StatusInternalServerError)
			return
		}
		filename = "base64_image"
	}

	logger.Log.Info().Str("filename", filename).Int("size", len(imageData)).Msg("Processing with Cloud Vision")

	extractedText, ocrConfidence, err := h.callVisionAPI(ctx, imageData)
	if err != nil {
		logger.Log.Error().Err(err).Msg("Cloud Vision API failed")
		RespondWithError(w, ErrCodeInternalError, "Error al procesar con Vision API", http.StatusInternalServerError)
		return
	}

	fields := map[string]string{"text": extractedText}

	scanResp := ScanResponse{
		Text:        extractedText,
		Fields:      fields,
		Confidence:  ocrConfidence,
		Source:      "cloud_vision",
		ProcessedAt: time.Now(),
	}

	logger.Log.Info().Int("text_length", len(extractedText)).Msg("OCR completed successfully")

	RespondWithJSON(w, http.StatusOK, scanResp)
}

func (h *ScanHandler) callVisionAPI(ctx context.Context, imageData []byte) (string, map[string]float64, error) {
	image := &visionpb.Image{
		Content: imageData,
	}

	annotations, err := h.visionClient.DetectDocumentText(ctx, image, nil)
	if err != nil {
		return "", nil, err
	}

	if annotations == nil {
		return "", map[string]float64{"text": 0}, nil
	}

	text := annotations.Text

	// Calculate average confidence from pages
	var totalConfidence float64
	var pageCount int
	for _, page := range annotations.Pages {
		if page.Confidence > 0 {
			totalConfidence += float64(page.Confidence)
			pageCount++
		}
	}

	avgConfidence := 0.0
	if pageCount > 0 {
		avgConfidence = totalConfidence / float64(pageCount)
	}

	confidenceMap := map[string]float64{"text": avgConfidence}
	return text, confidenceMap, nil
}

func meetsThreshold(confidence map[string]float64, minConfidence float64) bool {
	if confidence == nil || len(confidence) == 0 {
		return false
	}
	return getAverageConfidence(confidence) >= minConfidence
}

func getAverageConfidence(confidence map[string]float64) float64 {
	if confidence == nil || len(confidence) == 0 {
		return 0
	}
	var sum float64
	for _, v := range confidence {
		sum += v
	}
	return sum / float64(len(confidence))
}

func (h *ScanHandler) Routes() *chi.Mux {
	r := chi.NewRouter()
	r.Post("/", h.Scan)
	return r
}
