package middleware

import (
	"net/http"
	"strings"
)

const (
	// CookieWebAccess is the httpOnly JWT access cookie for browser sessions.
	CookieWebAccess = "enpunto_access"
	// CookieWebRefresh is the httpOnly refresh token cookie for browser sessions.
	CookieWebRefresh = "enpunto_refresh"
	// CookieWebHint is a non-secret marker so the SPA can detect an active browser session without reading JWTs.
	CookieWebHint = "enpunto_web_hint"
)

// RequestIsHTTPS reports whether the original client connection was HTTPS (TLS or common proxy headers).
func RequestIsHTTPS(r *http.Request) bool {
	if r.TLS != nil {
		return true
	}
	return strings.EqualFold(r.Header.Get("X-Forwarded-Proto"), "https")
}

// SetWebSessionCookies sets httpOnly access/refresh cookies and a readable session hint for the SPA.
func SetWebSessionCookies(w http.ResponseWriter, accessToken, refreshToken string, secure bool) {
	sameSite := http.SameSiteLaxMode
	http.SetCookie(w, &http.Cookie{
		Name:     CookieWebAccess,
		Value:    accessToken,
		Path:     "/",
		MaxAge:   15 * 60,
		HttpOnly: true,
		Secure:   secure,
		SameSite: sameSite,
	})
	http.SetCookie(w, &http.Cookie{
		Name:     CookieWebRefresh,
		Value:    refreshToken,
		Path:     "/",
		MaxAge:   30 * 24 * 60 * 60,
		HttpOnly: true,
		Secure:   secure,
		SameSite: sameSite,
	})
	http.SetCookie(w, &http.Cookie{
		Name:     CookieWebHint,
		Value:    "1",
		Path:     "/",
		MaxAge:   30 * 24 * 60 * 60,
		HttpOnly: false,
		Secure:   secure,
		SameSite: sameSite,
	})
}

// ClearWebSessionCookies expires session cookies on the response.
func ClearWebSessionCookies(w http.ResponseWriter, secure bool) {
	sameSite := http.SameSiteLaxMode
	expire := func(name string, httpOnly bool) {
		http.SetCookie(w, &http.Cookie{
			Name:     name,
			Value:    "",
			Path:     "/",
			MaxAge:   -1,
			HttpOnly: httpOnly,
			Secure:   secure,
			SameSite: sameSite,
		})
	}
	expire(CookieWebAccess, true)
	expire(CookieWebRefresh, true)
	expire(CookieWebHint, false)
}
