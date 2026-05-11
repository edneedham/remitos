package apierror

import (
	"encoding/json"
	"net/http"
)

// Response is the wire shape shared with handlers.ErrorResponse (JSON keys must match).
type Response struct {
	Error   string            `json:"error"`
	Message string            `json:"message"`
	Fields  map[string]string `json:"fields,omitempty"`
}

// Write sends a JSON error body with the given HTTP status.
func Write(w http.ResponseWriter, status int, code, message string, fields map[string]string) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	resp := Response{Error: code, Message: message, Fields: fields}
	if len(fields) == 0 {
		resp.Fields = nil
	}
	_ = json.NewEncoder(w).Encode(resp)
}
