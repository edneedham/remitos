package email

import (
	"strings"
	"testing"
	"time"
)

func TestSignupTrialWelcome_HTMLContainsCompanyCodeAndLinks(t *testing.T) {
	t.Parallel()
	end := time.Date(2026, 5, 1, 12, 0, 0, 0, time.UTC)
	m := SignupTrialWelcome("user@example.com", "ACME", "Acme SA", end, "https://site.example")
	if m.To != "user@example.com" {
		t.Fatalf("to: %q", m.To)
	}
	if !strings.Contains(m.HTMLBody, "ACME") {
		t.Fatal("missing company code in HTML")
	}
	if !strings.Contains(m.HTMLBody, `href="https://site.example/dashboard"`) {
		t.Fatal("missing account link")
	}
	if !strings.Contains(m.TextBody, "https://site.example/download") {
		t.Fatal("missing download link in text")
	}
}

func TestSignupTrialWelcome_NoPublicURL_OmitsLinks(t *testing.T) {
	t.Parallel()
	end := time.Date(2026, 5, 1, 12, 0, 0, 0, time.UTC)
	m := SignupTrialWelcome("user@example.com", "ACME", "Acme SA", end, "")
	if strings.Contains(m.HTMLBody, "Enlaces útiles") {
		t.Fatal("did not expect link section")
	}
}
