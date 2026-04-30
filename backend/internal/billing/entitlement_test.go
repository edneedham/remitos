package billing

import (
	"testing"
	"time"

	"github.com/google/uuid"
	"server/internal/models"
)

func TestCompanyHasAppDownloadAccess(t *testing.T) {
	now := time.Date(2026, 4, 1, 12, 0, 0, 0, time.UTC)
	trialEndFuture := now.Add(24 * time.Hour)
	trialEndPast := now.Add(-24 * time.Hour)
	subExpiresFuture := now.Add(48 * time.Hour)
	subExpiresPast := now.Add(-48 * time.Hour)

	tests := []struct {
		name string
		c    *models.Company
		want bool
	}{
		{"nil company", nil, false},
		{"archived", &models.Company{
			ID: uuid.New(), Status: "active", SubscriptionPlan: "premium",
			ArchivedAt: ptrTime(now.Add(-time.Hour)),
		}, false},
		{"inactive status", &models.Company{
			ID: uuid.New(), Status: "suspended", SubscriptionPlan: "premium",
		}, false},
		{"trial plan active window", &models.Company{
			ID: uuid.New(), Status: "active", SubscriptionPlan: "trial",
			TrialEndsAt: &trialEndFuture,
		}, true},
		{"pyme label during trial window", &models.Company{
			ID: uuid.New(), Status: "active", SubscriptionPlan: "pyme",
			TrialEndsAt: &trialEndFuture,
		}, true},
		{"trial expired", &models.Company{
			ID: uuid.New(), Status: "active", SubscriptionPlan: "trial",
			TrialEndsAt: &trialEndPast,
		}, false},
		{"trial nil end", &models.Company{
			ID: uuid.New(), Status: "active", SubscriptionPlan: "trial",
		}, false},
		{"pyme after trial with subscription", &models.Company{
			ID: uuid.New(), Status: "active", SubscriptionPlan: "pyme",
			TrialEndsAt:           &trialEndPast,
			SubscriptionExpiresAt: &subExpiresFuture,
		}, true},
		{"pyme after trial subscription expired", &models.Company{
			ID: uuid.New(), Status: "active", SubscriptionPlan: "pyme",
			TrialEndsAt:           &trialEndPast,
			SubscriptionExpiresAt: &subExpiresPast,
		}, false},
		{"premium no expiry", &models.Company{
			ID: uuid.New(), Status: "active", SubscriptionPlan: "premium",
		}, true},
		{"premium expires future", &models.Company{
			ID: uuid.New(), Status: "active", SubscriptionPlan: "premium",
			SubscriptionExpiresAt: &subExpiresFuture,
		}, true},
		{"premium expires past", &models.Company{
			ID: uuid.New(), Status: "active", SubscriptionPlan: "premium",
			SubscriptionExpiresAt: &subExpiresPast,
		}, false},
		{"paid plan", &models.Company{
			ID: uuid.New(), Status: "active", SubscriptionPlan: "paid",
		}, true},
		{"free legacy", &models.Company{
			ID: uuid.New(), Status: "active", SubscriptionPlan: "free",
		}, false},
		{"empty plan", &models.Company{
			ID: uuid.New(), Status: "active", SubscriptionPlan: "",
		}, false},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := CompanyHasAppDownloadAccess(now, tt.c)
			if got != tt.want {
				t.Fatalf("CompanyHasAppDownloadAccess() = %v, want %v", got, tt.want)
			}
		})
	}
}

func ptrTime(t time.Time) *time.Time { return &t }
