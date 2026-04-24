package email

import (
	"strings"

	"server/internal/logger"
)

// ConfigureSender returns a Resend-backed sender when enabled and configured; otherwise Noop.
func ConfigureSender(enabled bool, apiKey, from, replyTo string) Sender {
	if !enabled {
		logger.Log.Info().Msg("transactional email disabled (EMAIL_ENABLED=false)")
		return Noop{}
	}
	if strings.TrimSpace(apiKey) == "" || strings.TrimSpace(from) == "" {
		logger.Log.Warn().Msg("EMAIL_ENABLED=true but RESEND_API_KEY or EMAIL_FROM is empty; emails will not be sent")
		return Noop{}
	}
	logger.Log.Info().Str("from", from).Msg("transactional email enabled (Resend)")
	return NewResendSender(strings.TrimSpace(apiKey), strings.TrimSpace(from), strings.TrimSpace(replyTo))
}
