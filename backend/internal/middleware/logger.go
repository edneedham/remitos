package middleware

import (
	"net/http"
	"strings"
	"time"

	"github.com/go-chi/chi/v5"
	chimiddleware "github.com/go-chi/chi/v5/middleware"
	"server/internal/logger"
)

func Logger(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()
		wrapped := &responseWriter{ResponseWriter: w, statusCode: http.StatusOK}

		next.ServeHTTP(wrapped, r)

		ev := logger.Log.Info().
			Str("method", r.Method).
			Str("path", r.URL.Path).
			Int("status", wrapped.statusCode).
			Dur("duration", time.Since(start))
		if reqID := GetRequestID(r); reqID != "" {
			ev = ev.Str("request_id", reqID)
		}
		if ip := ClientIP(r); ip != "" {
			ev = ev.Str("client_ip", ip)
		}
		if xff := strings.TrimSpace(r.Header.Get("X-Forwarded-For")); xff != "" {
			ev = ev.Str("forwarded_for", xff)
		}
		if rc := chi.RouteContext(r.Context()); rc != nil {
			if pat := rc.RoutePattern(); pat != "" {
				ev = ev.Str("route", pat)
			}
		}
		ev.Msg("request")
	})
}

type responseWriter struct {
	http.ResponseWriter
	statusCode int
}

func (rw *responseWriter) WriteHeader(code int) {
	rw.statusCode = code
	rw.ResponseWriter.WriteHeader(code)
}

func Router(h http.Handler, corsAllowedOrigins []string) *chi.Mux {
	r := chi.NewRouter()
	if len(corsAllowedOrigins) > 0 {
		r.Use(CORS(corsAllowedOrigins))
	}
	r.Use(RequestID)
	r.Use(chimiddleware.RealIP)
	r.Use(Logger)
	r.NotFound(func(w http.ResponseWriter, r *http.Request) {
		http.Error(w, "Not Found", http.StatusNotFound)
	})
	r.MethodNotAllowed(func(w http.ResponseWriter, r *http.Request) {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
	})
	r.Mount("/", h)
	return r
}
