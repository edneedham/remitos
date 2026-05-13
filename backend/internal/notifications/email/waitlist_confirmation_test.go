package email

import (
	"strings"
	"testing"
)

func TestWaitlistJoined_IncludesWordmarkWhenURLSet(t *testing.T) {
	t.Parallel()
	m := WaitlistJoined("a@b.com", "https://example.com")
	if m.To != "a@b.com" {
		t.Fatalf("to: %q", m.To)
	}
	if !strings.Contains(m.HTMLBody, "example.com") {
		t.Fatal("expected site link or text in HTML")
	}
}

func TestWaitlistJoined_NoPublicURL_StillSendsBody(t *testing.T) {
	t.Parallel()
	m := WaitlistJoined("a@b.com", "")
	if strings.Contains(m.HTMLBody, "enpunto-wordmark") {
		t.Fatal("did not expect wordmark without public site URL")
	}
	if !strings.Contains(m.TextBody, "lista de espera") {
		t.Fatal("expected copy in text body")
	}
}
