package email

import (
	"strings"
	"testing"
	"time"
)

func TestSubscriptionRenewalUpcoming_ContainsAmountsAndDate(t *testing.T) {
	t.Parallel()
	expires := time.Date(2026, 5, 7, 18, 0, 0, 0, time.UTC)
	m := SubscriptionRenewalUpcoming(
		"owner@example.com",
		"Acme SA",
		10,
		13200,
		expires,
		"https://site.example",
	)
	if m.To != "owner@example.com" {
		t.Fatalf("to: %q", m.To)
	}
	if !strings.Contains(m.HTMLBody, "en 3 días") {
		t.Fatal("expected days phrase")
	}
	if !strings.Contains(m.HTMLBody, "$10 USD") && !strings.Contains(m.HTMLBody, "10 USD") {
		t.Fatal("expected USD amount")
	}
	if !strings.Contains(m.HTMLBody, "13.200") {
		t.Fatal("expected formatted ARS")
	}
	if !strings.Contains(m.HTMLBody, "tipo de cambio") {
		t.Fatal("expected FX disclaimer")
	}
	if !strings.Contains(m.TextBody, "$10 USD") {
		t.Fatal("text: expected USD")
	}
}

func TestFormatARSWholeWithDots(t *testing.T) {
	t.Parallel()
	cases := []struct {
		in   int64
		want string
	}{
		{13200, "13.200"},
		{999, "999"},
		{1000, "1.000"},
		{1000000, "1.000.000"},
	}
	for _, tc := range cases {
		if got := formatARSWholeWithDots(tc.in); got != tc.want {
			t.Fatalf("%d: got %q want %q", tc.in, got, tc.want)
		}
	}
}
