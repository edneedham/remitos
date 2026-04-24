package email

import "context"

// Message is a single outbound transactional email.
type Message struct {
	To       string
	Subject  string
	HTMLBody string
	TextBody string
}

// Sender delivers transactional email (e.g. via Resend).
type Sender interface {
	Send(ctx context.Context, m Message) error
}
