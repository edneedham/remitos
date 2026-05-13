package handlers

import (
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

func TestSignup_WaitlistOnly_Returns403(t *testing.T) {
	t.Parallel()
	h := &AuthHandler{waitlistOnly: true}
	req := httptest.NewRequest(http.MethodPost, "/auth/signup", strings.NewReader(`{}`))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()
	h.Signup(w, req)
	if w.Code != http.StatusForbidden {
		t.Fatalf("expected 403, got %d body=%s", w.Code, w.Body.String())
	}
}

func TestRegister_WaitlistOnly_Returns403(t *testing.T) {
	t.Parallel()
	h := &AuthHandler{waitlistOnly: true}
	req := httptest.NewRequest(http.MethodPost, "/auth/registrarse", strings.NewReader(`{}`))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()
	h.Register(w, req)
	if w.Code != http.StatusForbidden {
		t.Fatalf("expected 403, got %d body=%s", w.Code, w.Body.String())
	}
}
