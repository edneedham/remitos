package validation

import (
	"strings"

	"server/internal/models"
)

// NormalizeSignupRequest trims and canonicalizes signup fields before Struct validation.
// Password is intentionally left unchanged.
func NormalizeSignupRequest(req *models.SignupRequest) {
	req.Email = strings.TrimSpace(strings.ToLower(req.Email))
	req.CompanyName = strings.TrimSpace(req.CompanyName)
	req.CompanyCode = strings.TrimSpace(strings.ToUpper(req.CompanyCode))
	req.CompanyCUIT = digitsOnlyCUIT(req.CompanyCUIT)
}

func digitsOnlyCUIT(s string) string {
	var b strings.Builder
	for _, r := range s {
		if r >= '0' && r <= '9' {
			b.WriteRune(r)
		}
	}
	return b.String()
}
