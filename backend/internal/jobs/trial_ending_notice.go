package jobs

import (
	"context"
	"strings"
	"time"

	"server/internal/logger"
	notifymail "server/internal/notifications/email"
	"server/internal/notifications/inapp"
	"server/internal/repository"
)

const trialEndingNoticeTicker = time.Hour

// RunTrialEndingNoticeOnce sends a 3-day trial ending reminder (Argentina calendar).
func RunTrialEndingNoticeOnce(
	ctx context.Context,
	companyRepo *repository.CompanyRepository,
	mailer notifymail.Sender,
	publicSiteURL string,
	bc *inapp.Broadcaster,
) {
	rows, err := companyRepo.ListCompaniesForTrialEndingNotice(ctx)
	if err != nil {
		logger.Log.Error().Err(err).Msg("trial ending notice: list companies")
		return
	}
	loc, err := time.LoadLocation("America/Argentina/Buenos_Aires")
	if err != nil {
		loc = time.UTC
	}

	var sent int
	for _, row := range rows {
		emailAddr := strings.TrimSpace(row.OwnerEmail)
		if emailAddr == "" {
			continue
		}
		phrase := notifymail.FormatTrialEndDateEs(row.TrialEndsAt, loc)
		msg := notifymail.TrialEndingSoon(emailAddr, row.CompanyName, phrase, publicSiteURL)
		if err := mailer.Send(ctx, msg); err != nil {
			logger.Log.Error().Err(err).Str("company_id", row.CompanyID.String()).Msg("trial ending notice email failed")
			continue
		}
		if err := companyRepo.MarkTrialEndingNoticeSent(ctx, row.CompanyID); err != nil {
			logger.Log.Error().Err(err).Str("company_id", row.CompanyID.String()).Msg("trial ending notice: mark sent failed")
			continue
		}
		if bc != nil {
			bc.TrialEndingSoon(ctx, row.CompanyID, row.CompanyName, phrase)
		}
		sent++
	}
	if sent > 0 {
		logger.Log.Info().Int("sent", sent).Int("candidates", len(rows)).Msg("trial ending notices processed")
	}
}

// StartTrialEndingNoticeLoop runs RunTrialEndingNoticeOnce hourly until ctx is cancelled.
func StartTrialEndingNoticeLoop(
	ctx context.Context,
	companyRepo *repository.CompanyRepository,
	mailer notifymail.Sender,
	publicSiteURL string,
	bc *inapp.Broadcaster,
) {
	ticker := time.NewTicker(trialEndingNoticeTicker)
	defer ticker.Stop()

	RunTrialEndingNoticeOnce(ctx, companyRepo, mailer, publicSiteURL, bc)
	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			RunTrialEndingNoticeOnce(ctx, companyRepo, mailer, publicSiteURL, bc)
		}
	}
}
