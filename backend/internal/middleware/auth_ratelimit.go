package middleware

import (
	"net/http"
	"sync"

	"golang.org/x/time/rate"
)

const authEndpointBurst = 24

// AuthEndpointsRateLimit applies a per-client-IP token bucket to reduce brute-force
// and spam traffic on unauthenticated /auth routes (~30 requests/minute steady-state).
func AuthEndpointsRateLimit() func(http.Handler) http.Handler {
	const steadyPerMinute = 30
	lim := rate.Limit(steadyPerMinute / 60.0)

	var mu sync.Mutex
	byIP := make(map[string]*rate.Limiter)

	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			ip := ClientIP(r)
			mu.Lock()
			limiter, ok := byIP[ip]
			if !ok {
				limiter = rate.NewLimiter(lim, authEndpointBurst)
				byIP[ip] = limiter
			}
			mu.Unlock()

			if !limiter.Allow() {
				http.Error(w, "Too Many Requests", http.StatusTooManyRequests)
				return
			}
			next.ServeHTTP(w, r)
		})
	}
}
