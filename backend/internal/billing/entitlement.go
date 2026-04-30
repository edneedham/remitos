package billing

import (
	"strings"
	"time"

	"server/internal/models"
)

// CompanyHasAppDownloadAccess returns true if the company may download the Android APK.
// Access is granted during an active trial window (trial_ends_at in the future), regardless
// of whether subscription_plan is stored as "trial", "pyme", or "empresa"; or when on an
// active paid/commercial plan with a valid subscription period.
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

	// Any catalog label during the published trial window (signup keeps trial_ends_at).
	if c.TrialEndsAt != nil && now.Before(*c.TrialEndsAt) {
		return true
	}

	plan := strings.ToLower(strings.TrimSpace(c.SubscriptionPlan))

	if IsPaidPlan(plan) {
		if c.SubscriptionExpiresAt == nil {
			return true
		}
		return now.Before(*c.SubscriptionExpiresAt)
	}

	return false
}

// IsPaidPlan reports catalog plans that bill as a paid/commercial subscription (not trial/free).
func IsPaidPlan(plan string) bool {
	switch strings.ToLower(strings.TrimSpace(plan)) {
	case "premium", "paid", "subscriber", "standard", "pyme", "empresa", "corporativo":
		return true
	default:
		return false
	}
}
