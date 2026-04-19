package validation

import (
	"strings"

	"server/internal/models"
)

// NormalizeLoginRequest trims canonical fields before Struct validation.
// Password is intentionally left unchanged.
func NormalizeLoginRequest(req *models.LoginRequest) {
	req.CompanyCode = strings.TrimSpace(strings.ToUpper(req.CompanyCode))
	req.Username = strings.TrimSpace(req.Username)
	req.DeviceName = strings.TrimSpace(req.DeviceName)
}
