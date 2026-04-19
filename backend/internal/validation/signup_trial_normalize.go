package validation

import (
	"strings"

	"server/internal/models"
)

// NormalizeSignupTrialRequest trims and canonicalizes signup fields before Struct validation.
// Password is intentionally left unchanged.
func NormalizeSignupTrialRequest(req *models.SignupTrialRequest) {
	req.Email = strings.TrimSpace(strings.ToLower(req.Email))
	req.CompanyName = strings.TrimSpace(req.CompanyName)
	req.CompanyCode = strings.TrimSpace(strings.ToUpper(req.CompanyCode))
	req.CardToken = strings.TrimSpace(req.CardToken)
}
