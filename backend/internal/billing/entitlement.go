package billing

import (
	"strings"
	"time"

	"server/internal/models"
)

// CompanyHasAppDownloadAccess returns true if the company may download the Android APK
// (active company in trial or on a paid plan with a non-expired subscription).
func CompanyHasAppDownloadAccess(now time.Time, c *models.Company) bool {
	if c == nil {
		return false
	}
	if c.ArchivedAt != nil {
		return false
	}
	if c.Status != "" && c.Status != "active" {
		return false
	}

	plan := strings.ToLower(strings.TrimSpace(c.SubscriptionPlan))

	if plan == "trial" {
		if c.TrialEndsAt == nil {
			return false
		}
		return now.Before(*c.TrialEndsAt)
	}

	if isPaidPlan(plan) {
		if c.SubscriptionExpiresAt == nil {
			return true
		}
		return now.Before(*c.SubscriptionExpiresAt)
	}

	return false
}

func isPaidPlan(plan string) bool {
	switch plan {
	case "premium", "paid", "subscriber", "standard":
		return true
	default:
		return false
	}
}
