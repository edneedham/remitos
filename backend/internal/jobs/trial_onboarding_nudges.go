package jobs

import (
	"context"
	"strings"
	"time"

	"server/internal/logger"
	notifymail "server/internal/notifications/email"
	"server/internal/onboarding"
	"server/internal/repository"
)

const trialOnboardingNudgeTicker = 5 * time.Minute

// RunTrialOnboardingNudgeOnce sends at most one onboarding email per eligible company
// (setup / day1 / day3) when trial_activation_at thresholds are met and there are no inbound notes yet.
func RunTrialOnboardingNudgeOnce(ctx context.Context, companyRepo *repository.CompanyRepository, mailer notifymail.Sender, publicSiteURL string) {
	rows, err := companyRepo.ListCompaniesForTrialOnboardingNudges(ctx)
	if err != nil {
		logger.Log.Error().Err(err).Msg("trial onboarding nudges: list companies")
		return
	}

	now := time.Now().UTC()
	var sent int
	for _, row := range rows {
		emailAddr := strings.TrimSpace(row.OwnerEmail)
		if emailAddr == "" {
			continue
		}

		stage := onboarding.PickNudgeStage(now, row.TrialActivationAt, row.SetupSent, row.Day1Sent, row.Day3Sent)
		if stage == "" {
			continue
		}

		var msg notifymail.Message
		switch stage {
		case "setup":
			msg = notifymail.TrialOnboardingNudgeSetup(emailAddr, row.CompanyCode, row.CompanyName, publicSiteURL)
		case "day1":
			msg = notifymail.TrialOnboardingNudgeDay1(emailAddr, row.CompanyCode, row.CompanyName, publicSiteURL)
		case "day3":
			msg = notifymail.TrialOnboardingNudgeDay3(emailAddr, row.CompanyCode, row.CompanyName, publicSiteURL)
		default:
			continue
		}

		if err := mailer.Send(ctx, msg); err != nil {
			logger.Log.Error().Err(err).
				Str("company_id", row.CompanyID.String()).
				Str("stage", stage).
				Msg("trial onboarding nudge email failed")
			continue
		}

		if err := companyRepo.ApplyTrialOnboardingNudgeSent(ctx, row.CompanyID, stage); err != nil {
			logger.Log.Error().Err(err).
				Str("company_id", row.CompanyID.String()).
				Str("stage", stage).
				Msg("trial onboarding nudge: mark sent failed")
			continue
		}
		sent++
	}

	if sent > 0 {
		logger.Log.Info().Int("sent", sent).Int("candidates", len(rows)).Msg("trial onboarding nudges processed")
	}
}

// StartTrialOnboardingNudgeLoop runs RunTrialOnboardingNudgeOnce periodically until ctx is cancelled.
func StartTrialOnboardingNudgeLoop(ctx context.Context, companyRepo *repository.CompanyRepository, mailer notifymail.Sender, publicSiteURL string) {
	ticker := time.NewTicker(trialOnboardingNudgeTicker)
	defer ticker.Stop()

	RunTrialOnboardingNudgeOnce(ctx, companyRepo, mailer, publicSiteURL)

	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			RunTrialOnboardingNudgeOnce(ctx, companyRepo, mailer, publicSiteURL)
		}
	}
}
