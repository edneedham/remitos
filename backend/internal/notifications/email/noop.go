package email

import "context"

// Noop does not send mail; used when EMAIL_ENABLED is false or Resend is not configured.
type Noop struct{}

func (Noop) Send(ctx context.Context, m Message) error {
	_ = ctx
	_ = m
	return nil
}
