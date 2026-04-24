package email

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"strings"
	"time"

	"server/internal/logger"
)

const resendAPIURL = "https://api.resend.com/emails"

// ResendSender posts to the Resend HTTP API.
type ResendSender struct {
	HTTPClient *http.Client
	// Endpoint overrides the default Resend API URL (used in tests).
	Endpoint string
	APIKey   string
	From     string
	ReplyTo  string
}

type resendRequest struct {
	From    string   `json:"from"`
	To      []string `json:"to"`
	Subject string   `json:"subject"`
	HTML    string   `json:"html,omitempty"`
	Text    string   `json:"text,omitempty"`
	ReplyTo *string  `json:"reply_to,omitempty"`
}

type resendResponse struct {
	ID string `json:"id"`
}

type resendErrorBody struct {
	Message string `json:"message"`
	Name    string `json:"name"`
}

func (r *ResendSender) Send(ctx context.Context, m Message) error {
	if strings.TrimSpace(r.APIKey) == "" || strings.TrimSpace(r.From) == "" {
		return fmt.Errorf("resend: missing api key or from address")
	}
	to := strings.TrimSpace(m.To)
	if to == "" {
		return fmt.Errorf("resend: empty recipient")
	}

	body := resendRequest{
		From:    r.From,
		To:      []string{to},
		Subject: m.Subject,
		HTML:    m.HTMLBody,
		Text:    m.TextBody,
	}
	if rt := strings.TrimSpace(r.ReplyTo); rt != "" {
		body.ReplyTo = &rt
	}

	payload, err := json.Marshal(body)
	if err != nil {
		return err
	}

	endpoint := strings.TrimSpace(r.Endpoint)
	if endpoint == "" {
		endpoint = resendAPIURL
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, endpoint, bytes.NewReader(payload))
	if err != nil {
		return err
	}
	req.Header.Set("Authorization", "Bearer "+r.APIKey)
	req.Header.Set("Content-Type", "application/json")

	client := r.HTTPClient
	if client == nil {
		client = http.DefaultClient
	}
	resp, err := client.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()

	respBody, _ := io.ReadAll(io.LimitReader(resp.Body, 64<<10))

	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		var re resendErrorBody
		_ = json.Unmarshal(respBody, &re)
		msg := strings.TrimSpace(re.Message)
		if msg == "" {
			msg = strings.TrimSpace(string(respBody))
		}
		return fmt.Errorf("resend: %s: %s", resp.Status, msg)
	}

	var ok resendResponse
	if err := json.Unmarshal(respBody, &ok); err == nil && ok.ID != "" {
		logger.Log.Info().Str("resend_id", ok.ID).Str("to", to).Msg("email sent")
	}
	return nil
}

// NewResendSender returns a sender or nil fields validation failure at call site should use Noop.
func NewResendSender(apiKey, from, replyTo string) *ResendSender {
	return &ResendSender{
		HTTPClient: &http.Client{Timeout: 25 * time.Second},
		APIKey:     apiKey,
		From:       from,
		ReplyTo:    replyTo,
	}
}
