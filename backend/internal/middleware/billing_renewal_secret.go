package middleware

import (
	"crypto/subtle"
	"net/http"

	"server/internal/apierror"
	"server/internal/logger"
)

// BillingRenewalSecret gates cron/internal renewal endpoints (constant-time compare).
// Requires callers to send header X-Billing-Secret matching the configured secret (same byte length).
func BillingRenewalSecret(secret string) func(http.Handler) http.Handler {
	want := []byte(secret)
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			got := []byte(r.Header.Get("X-Billing-Secret"))
			if len(want) == 0 || len(got) != len(want) || subtle.ConstantTimeCompare(got, want) != 1 {
				logger.Log.Warn().
					Str("path", r.URL.Path).
					Str("method", r.Method).
					Str("request_id", GetRequestID(r)).
					Msg("billing renewal: unauthorized (missing or invalid secret)")
				apierror.Write(w, http.StatusUnauthorized, string(apierror.Unauthorized), "Unauthorized", nil)
				return
			}
			next.ServeHTTP(w, r)
		})
	}
}
