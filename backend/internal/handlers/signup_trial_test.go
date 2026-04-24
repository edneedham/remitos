package handlers

import "testing"

func TestResolveSignupCardToken(t *testing.T) {
	t.Run("uses provided token when present", func(t *testing.T) {
		got, err := resolveSignupCardToken("tok_test_123", false)
		if err != nil {
			t.Fatalf("expected nil error, got %v", err)
		}
		if got != "tok_test_123" {
			t.Fatalf("expected provided token, got %q", got)
		}
	})

	t.Run("requires token when mock disabled", func(t *testing.T) {
		_, err := resolveSignupCardToken("", false)
		if err == nil {
			t.Fatal("expected error when token is empty and mock is disabled")
		}
		if err != errCardTokenRequired {
			t.Fatalf("expected errCardTokenRequired, got %v", err)
		}
	})

	t.Run("falls back to mock token when allowed", func(t *testing.T) {
		got, err := resolveSignupCardToken("", true)
		if err != nil {
			t.Fatalf("expected nil error, got %v", err)
		}
		if got != "mock_card_token" {
			t.Fatalf("expected mock card token, got %q", got)
		}
	})
}
