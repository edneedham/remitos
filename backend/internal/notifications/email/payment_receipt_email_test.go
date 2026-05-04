package email

import (
	"strings"
	"testing"
	"time"
)

func TestPaymentReceipt_ContainsInvoiceAndLegal(t *testing.T) {
	t.Parallel()
	m := PaymentReceipt(
		"payer@test.com",
		"Acme",
		"PyME",
		"https://site.example",
		1320000,
		"ARS",
		time.Date(2026, 5, 1, 12, 0, 0, 0, time.UTC),
		"invoice-uuid",
		"123456789",
		"Nota legal de prueba.",
	)
	if m.To != "payer@test.com" {
		t.Fatalf("to: %q", m.To)
	}
	if !strings.Contains(m.HTMLBody, "13.200") {
		t.Fatal("expected formatted ARS amount")
	}
	if !strings.Contains(m.HTMLBody, "invoice-uuid") {
		t.Fatal("expected invoice id")
	}
	if !strings.Contains(m.HTMLBody, "Nota legal") {
		t.Fatal("expected legal footer")
	}
}
