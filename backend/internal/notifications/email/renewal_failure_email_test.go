package email

import (
	"strings"
	"testing"
)

func TestRenewalChargeFailure_ContainsAmountAndReason(t *testing.T) {
	t.Parallel()
	m := RenewalChargeFailure(
		"owner@test.com",
		"Acme SA",
		150000,
		"ARS",
		"Tarjeta rechazada.",
		"https://site.example",
	)
	if m.To != "owner@test.com" {
		t.Fatalf("to: %q", m.To)
	}
	if !strings.Contains(m.HTMLBody, "1.500") {
		t.Fatal("expected formatted ARS in HTML")
	}
	if !strings.Contains(m.HTMLBody, "Tarjeta rechazada") {
		t.Fatal("expected reason")
	}
	if !strings.Contains(m.HTMLBody, `href="https://site.example/dashboard"`) {
		t.Fatal("expected dashboard link")
	}
}
