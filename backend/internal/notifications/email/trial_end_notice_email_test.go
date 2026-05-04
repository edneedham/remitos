package email

import (
	"strings"
	"testing"
	"time"
)

func TestTrialEndingSoon_IncludesDatePhrase(t *testing.T) {
	t.Parallel()
	m := TrialEndingSoon(
		"trial@test.com",
		"Beta Co",
		"7 de mayo de 2026",
		"https://www.example.com",
	)
	if !strings.Contains(m.HTMLBody, "en 3 días") {
		t.Fatal("expected days phrase")
	}
	if !strings.Contains(m.HTMLBody, "7 de mayo") {
		t.Fatal("expected date phrase")
	}
	if !strings.Contains(m.HTMLBody, "Beta Co") {
		t.Fatal("expected company name")
	}
}

func TestFormatTrialEndDateEs(t *testing.T) {
	t.Parallel()
	loc, err := time.LoadLocation("America/Argentina/Buenos_Aires")
	if err != nil {
		t.Fatal(err)
	}
	ts := time.Date(2026, 5, 7, 23, 0, 0, 0, time.UTC)
	s := FormatTrialEndDateEs(ts, loc)
	if !strings.Contains(s, "mayo") || !strings.Contains(s, "2026") {
		t.Fatalf("unexpected: %q", s)
	}
}
