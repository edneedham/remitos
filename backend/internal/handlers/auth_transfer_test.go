//go:build ignore

package handlers

import (
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

func TestHashTransferToken_Deterministic(t *testing.T) {
	first := hashTransferToken("abc123")
	second := hashTransferToken("abc123")
	other := hashTransferToken("xyz789")

	if first == "" {
		t.Fatal("expected non-empty hash")
	}
	if first != second {
		t.Fatal("expected deterministic hash output")
	}
	if first == other {
		t.Fatal("expected different inputs to produce different hashes")
	}
	if len(first) != 64 {
		t.Fatalf("expected sha256 hex length 64, got %d", len(first))
	}
}

func TestStartSessionTransfer_InvalidJSON(t *testing.T) {
	handler := &AuthHandler{}
	req := httptest.NewRequest(http.MethodPost, "/auth/transfer/start", strings.NewReader("{"))
	w := httptest.NewRecorder()

	handler.StartSessionTransfer(w, req)

	if w.Code != http.StatusBadRequest {
		t.Fatalf("expected %d, got %d", http.StatusBadRequest, w.Code)
	}
}

func TestStartSessionTransfer_MissingRefreshToken(t *testing.T) {
	handler := &AuthHandler{}
	req := httptest.NewRequest(http.MethodPost, "/auth/transfer/start", strings.NewReader(`{}`))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	handler.StartSessionTransfer(w, req)

	if w.Code != http.StatusBadRequest {
		t.Fatalf("expected %d, got %d", http.StatusBadRequest, w.Code)
	}
}

func TestClaimSessionTransfer_InvalidJSON(t *testing.T) {
	handler := &AuthHandler{}
	req := httptest.NewRequest(http.MethodPost, "/auth/transfer/claim", strings.NewReader("{"))
	w := httptest.NewRecorder()

	handler.ClaimSessionTransfer(w, req)

	if w.Code != http.StatusBadRequest {
		t.Fatalf("expected %d, got %d", http.StatusBadRequest, w.Code)
	}
}

func TestClaimSessionTransfer_MissingToken(t *testing.T) {
	handler := &AuthHandler{}
	req := httptest.NewRequest(http.MethodPost, "/auth/transfer/claim", strings.NewReader(`{}`))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	handler.ClaimSessionTransfer(w, req)

	if w.Code != http.StatusBadRequest {
		t.Fatalf("expected %d, got %d", http.StatusBadRequest, w.Code)
	}
}

func TestAuthRoutes_TransferClaimRouteIsMounted(t *testing.T) {
	handler := &AuthHandler{}
	router := handler.Routes()
	req := httptest.NewRequest(http.MethodPost, "/transfer/claim", strings.NewReader(`{}`))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	router.ServeHTTP(w, req)

	if w.Code != http.StatusBadRequest {
		t.Fatalf("expected %d, got %d", http.StatusBadRequest, w.Code)
	}
}

func TestAuthRoutes_TransferStartRouteRequiresAuth(t *testing.T) {
	handler := &AuthHandler{}
	router := handler.Routes()
	req := httptest.NewRequest(http.MethodPost, "/transfer/start", strings.NewReader(`{}`))
	req.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()

	router.ServeHTTP(w, req)

	if w.Code != http.StatusUnauthorized {
		t.Fatalf("expected %d, got %d", http.StatusUnauthorized, w.Code)
	}
}
