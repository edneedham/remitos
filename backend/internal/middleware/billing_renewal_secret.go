package middleware

import (
	"crypto/subtle"
	"net/http"
)

// BillingRenewalSecret gates cron/internal renewal endpoints (constant-time compare).
// Requires callers to send header X-Billing-Secret matching the configured secret (same byte length).
func BillingRenewalSecret(secret string) func(http.Handler) http.Handler {
	want := []byte(secret)
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			got := []byte(r.Header.Get("X-Billing-Secret"))
			if len(want) == 0 || len(got) != len(want) || subtle.ConstantTimeCompare(got, want) != 1 {
				http.Error(w, "Unauthorized", http.StatusUnauthorized)
				return
			}
			next.ServeHTTP(w, r)
		})
	}
}
