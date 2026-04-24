package email

import (
	"context"
	"encoding/json"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

func TestResendSender_Send(t *testing.T) {
	t.Parallel()
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			t.Errorf("method: %s", r.Method)
		}
		if r.Header.Get("Authorization") != "Bearer testkey" {
			t.Errorf("auth header: %q", r.Header.Get("Authorization"))
		}
		b, _ := io.ReadAll(r.Body)
		var got resendRequest
		if err := json.Unmarshal(b, &got); err != nil {
			t.Fatal(err)
		}
		if got.From != "T <a@b.c>" {
			t.Errorf("from: %q", got.From)
		}
		if len(got.To) != 1 || got.To[0] != "u@x.y" {
			t.Errorf("to: %v", got.To)
		}
		if got.Subject != "subj" {
			t.Errorf("subject: %q", got.Subject)
		}
		_ = json.NewEncoder(w).Encode(resendResponse{ID: "re_123"})
		w.WriteHeader(http.StatusOK)
	}))
	t.Cleanup(srv.Close)

	s := &ResendSender{
		HTTPClient: srv.Client(),
		Endpoint:   srv.URL,
		APIKey:     "testkey",
		From:       "T <a@b.c>",
	}
	err := s.Send(context.Background(), Message{
		To:       "u@x.y",
		Subject:  "subj",
		HTMLBody: "<p>x</p>",
		TextBody: "x",
	})
	if err != nil {
		t.Fatal(err)
	}
}

func TestResendSender_Send_HTTPError(t *testing.T) {
	t.Parallel()
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusUnauthorized)
		_, _ = w.Write([]byte(`{"message":"bad key"}`))
	}))
	t.Cleanup(srv.Close)

	s := &ResendSender{
		HTTPClient: srv.Client(),
		Endpoint:   srv.URL,
		APIKey:     "testkey",
		From:       "T <a@b.c>",
	}
	err := s.Send(context.Background(), Message{To: "u@x.y", Subject: "s"})
	if err == nil || !strings.Contains(err.Error(), "401") {
		t.Fatalf("expected 401 error, got %v", err)
	}
}
