package email

import (
	"strings"
	"testing"
	"time"
)

func TestSubscriptionLapsed_ContainsExpiryPhrase(t *testing.T) {
	t.Parallel()
	m := SubscriptionLapsed(
		"own@test.com",
		"Gamma SA",
		"1 de mayo de 2026",
		"https://x.example",
	)
	if !strings.Contains(m.HTMLBody, "Gamma SA") {
		t.Fatal("expected company")
	}
	if !strings.Contains(m.HTMLBody, "1 de mayo") {
		t.Fatal("expected date phrase")
	}
	if !strings.Contains(m.HTMLBody, `href="https://x.example/dashboard"`) {
		t.Fatal("expected dashboard link")
	}
}

func TestFormatLapseDateEs(t *testing.T) {
	t.Parallel()
	loc, err := time.LoadLocation("America/Argentina/Buenos_Aires")
	if err != nil {
		t.Fatal(err)
	}
	ts := time.Date(2026, 4, 30, 12, 0, 0, 0, time.UTC)
	s := FormatLapseDateEs(ts, loc)
	if !strings.Contains(s, "2026") {
		t.Fatalf("unexpected: %q", s)
	}
}
