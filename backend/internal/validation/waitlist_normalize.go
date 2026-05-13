package validation

import (
	"strings"

	"server/internal/models"
)

// NormalizeWaitlistRequest trims string fields and lowercases email for storage.
func NormalizeWaitlistRequest(req *models.WaitlistRequest) {
	req.Email = strings.ToLower(strings.TrimSpace(req.Email))
	req.FullName = strings.TrimSpace(req.FullName)
	req.CompanyName = strings.TrimSpace(req.CompanyName)
	req.Source = strings.TrimSpace(req.Source)
	req.DeliveryNotesPerDayBand = strings.TrimSpace(req.DeliveryNotesPerDayBand)
	req.ProcessingMode = strings.TrimSpace(req.ProcessingMode)
	req.DigitalApplication = strings.TrimSpace(req.DigitalApplication)
	req.LogisticsPainPoints = strings.TrimSpace(req.LogisticsPainPoints)
	if req.ProcessingMode == "manual" {
		req.DigitalApplication = ""
	}
}
