package middleware

import (
	"context"
	"net/http"
	"strings"

	"github.com/google/uuid"
)

type reqIDKey struct{}

// RequestID ensures each request has an ID (client may send X-Request-ID). Echoes it on the response.
func RequestID(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		id := strings.TrimSpace(r.Header.Get("X-Request-ID"))
		if id == "" {
			id = uuid.New().String()
		}
		w.Header().Set("X-Request-ID", id)
		ctx := context.WithValue(r.Context(), reqIDKey{}, id)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// GetRequestID returns the request correlation ID or empty when middleware did not run.
func GetRequestID(r *http.Request) string {
	if v, ok := r.Context().Value(reqIDKey{}).(string); ok {
		return v
	}
	return ""
}
