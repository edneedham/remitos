package httputil

import (
	"errors"
	"io"
	"net/http"
)

const (
	// MaxSyncRequestBody caps POST /sync JSON payload size (mobile batch uploads).
	MaxSyncRequestBody int64 = 32 << 20 // 32 MiB
	// MaxJSONAuthBody caps unauthenticated /auth JSON bodies (login, signup, device, refresh, etc.).
	MaxJSONAuthBody int64 = 1 << 20 // 1 MiB
	// MaxJSONAPIBody caps authenticated JSON mutations (warehouses, admin, billing panels).
	MaxJSONAPIBody int64 = 4 << 20 // 4 MiB
	// MaxJSONInternalBody caps small internal/cron JSON payloads.
	MaxJSONInternalBody int64 = 256 << 10 // 256 KiB
	// MaxJSONScanBody caps POST /scan JSON path (OCR payload metadata).
	MaxJSONScanBody int64 = 4 << 20 // 4 MiB
	// MaxMercadoPagoWebhookBody caps Mercado Pago webhook raw bodies.
	MaxMercadoPagoWebhookBody int64 = 16 << 20 // 16 MiB
)

// MessageJSONBodyTooLarge is returned to API clients when MaxBytesReader rejects a body.
const MessageJSONBodyTooLarge = "El cuerpo de la solicitud es demasiado grande."

// WrapBody returns an io.ReadCloser that limits r to maxBytes. Pass w so oversized
// reads receive http.ErrBodyReadAfterClose / MaxBytesError semantics for the client.
func WrapBody(w http.ResponseWriter, r io.ReadCloser, maxBytes int64) io.ReadCloser {
	if maxBytes <= 0 {
		return r
	}
	return http.MaxBytesReader(w, r, maxBytes)
}

// LimitRequestBody wraps r.Body with MaxBytesReader. Call immediately before json.NewDecoder(r.Body)
// or io.ReadAll(r.Body).
func LimitRequestBody(w http.ResponseWriter, r *http.Request, maxBytes int64) {
	r.Body = WrapBody(w, r.Body, maxBytes)
}

// IsMaxBytesError reports whether err came from http.MaxBytesReader exceeding its limit.
func IsMaxBytesError(err error) bool {
	var maxErr *http.MaxBytesError
	return errors.As(err, &maxErr)
}
