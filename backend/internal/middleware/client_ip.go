package middleware

import (
	"net"
	"net/http"
	"strings"
)

// ClientIP returns the host part of r.RemoteAddr (after chi RealIP / proxies).
func ClientIP(r *http.Request) string {
	if r == nil {
		return ""
	}
	host, _, err := net.SplitHostPort(r.RemoteAddr)
	if err != nil {
		return strings.TrimSpace(r.RemoteAddr)
	}
	return host
}
