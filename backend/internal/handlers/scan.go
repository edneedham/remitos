package handlers

import (
	"context"
	"encoding/base64"
	"io"
	"mime"
	"net/http"
	"strconv"
	"strings"
	"time"

	vision "cloud.google.com/go/vision/apiv1"
	"cloud.google.com/go/vision/v2/apiv1/visionpb"
	"github.com/go-chi/chi/v5"
	"server/internal/httputil"
	"server/internal/logger"
)

type ScanHandler struct {
	visionClient *vision.ImageAnnotatorClient
}

func NewScanHandler() (*ScanHandler, error) {
	ctx, cancel := context.WithTimeout(context.Background(), 20*time.Second)
	defer cancel()
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

// MultipartFormDataContentType reports whether ct is a multipart request (e.g. multipart/form-data; boundary=...).
func MultipartFormDataContentType(ct string) bool {
	mediaType, _, err := mime.ParseMediaType(ct)
	return err == nil && strings.HasPrefix(strings.ToLower(mediaType), "multipart/")
}

func (h *ScanHandler) Scan(w http.ResponseWriter, r *http.Request) {
	ctx, cancel := context.WithTimeout(r.Context(), 30*time.Second)
	defer cancel()

	contentType := r.Header.Get("Content-Type")
	isMultipart := MultipartFormDataContentType(contentType)

	var imageData []byte = nil
	var filename string

	if isMultipart {
		file, header, err := r.FormFile("image")
		if err != nil {
			RespondWithError(w, r, ErrCodeInvalidRequest, "No se pudo obtener la imagen", http.StatusBadRequest)
			return
		}
		defer file.Close()
		filename = header.Filename

		imageData, err = io.ReadAll(file)
		if err != nil {
			RespondWithError(w, r, ErrCodeInternalError, "Error al leer la imagen", http.StatusInternalServerError, err)
			return
		}
	} else {
		var req ScanRequest
		if !decodeJSONBody(w, r, httputil.MaxJSONScanBody, &req) {
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
			RespondWithError(w, r, ErrCodeInvalidRequest, "Imagen requerida cuando el OCR local no cumple el umbral", http.StatusBadRequest)
			return
		}

		var err error
		imageData, err = base64.StdEncoding.DecodeString(imageBase64)
		if err != nil {
			RespondWithError(w, r, ErrCodeInternalError, "Error al decodificar la imagen", http.StatusInternalServerError, err)
			return
		}
		filename = "base64_image"
	}

	logger.Log.Info().Str("filename", filename).Int("size", len(imageData)).Msg("Processing with Cloud Vision")

	extractedText, ocrConfidence, err := h.callVisionAPI(ctx, imageData)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error al procesar con Vision API", http.StatusInternalServerError, err)
		return
	}

	logger.Log.Info().Str("text", extractedText).Msg("Cloud Vision raw text")

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
