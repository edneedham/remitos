package handlers

import (
	"encoding/json"
	"net/http"

	"server/internal/httputil"
)

// decodeJSONBody wraps the request body with a max size, decodes JSON into v, and writes a standard error response on failure.
func decodeJSONBody(w http.ResponseWriter, r *http.Request, maxBytes int64, v any) bool {
	httputil.LimitRequestBody(w, r, maxBytes)
	if err := json.NewDecoder(r.Body).Decode(v); err != nil {
		if httputil.IsMaxBytesError(err) {
			RespondWithError(w, r, ErrCodeInvalidRequest, httputil.MessageJSONBodyTooLarge, http.StatusRequestEntityTooLarge, err)
			return false
		}
		RespondWithError(w, r, ErrCodeInvalidRequest, "Cuerpo de solicitud inválido", http.StatusBadRequest, err)
		return false
	}
	return true
}
