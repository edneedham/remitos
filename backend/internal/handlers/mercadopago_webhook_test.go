package handlers

import (
	"net/http/httptest"
	"testing"
)

func TestExtractPaymentIDFromRequest_JSON(t *testing.T) {
	req := httptest.NewRequest("POST", "/", nil)
	id := extractPaymentIDFromRequest(req, []byte(`{"type":"payment","data":{"id":123456789}}`))
	if id != "123456789" {
		t.Fatalf("got %q", id)
	}
}

func TestExtractPaymentIDFromRequest_largeInteger(t *testing.T) {
	req := httptest.NewRequest("POST", "/", nil)
	// Must not be mangled by float64 (json.Number path).
	id := extractPaymentIDFromRequest(req, []byte(`{"type":"payment","data":{"id":9990000000012345}}`))
	if id != "9990000000012345" {
		t.Fatalf("got %q", id)
	}
}

func TestExtractPaymentIDFromRequest_query(t *testing.T) {
	req := httptest.NewRequest("POST", "/?topic=payment&id=555", nil)
	id := extractPaymentIDFromRequest(req, nil)
	if id != "555" {
		t.Fatalf("got %q", id)
	}
}
