package handlers

import (
	"net/http"
	"testing"

	"github.com/go-chi/chi/v5"
	srvjwt "server/internal/jwt"
)

func TestDeviceHandler_Routes_registersEndpoints(t *testing.T) {
	jwtSvc := srvjwt.NewService("test-secret-device-routes")
	h := NewDeviceHandler(nil, nil, jwtSvc, nil)
	mux := h.Routes()

	var paths []string
	err := chi.Walk(mux, func(method, route string, _ http.Handler, _ ...func(http.Handler) http.Handler) error {
		paths = append(paths, method+" "+route)
		return nil
	})
	if err != nil {
		t.Fatalf("walk: %v", err)
	}
	if len(paths) < 3 {
		t.Fatalf("expected at least 3 routes, got %v", paths)
	}
}
