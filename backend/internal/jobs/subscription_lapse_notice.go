package jobs

import (
	"context"
	"strings"
	"time"

	"server/internal/logger"
	"server/internal/notifications/inapp"
	notifymail "server/internal/notifications/email"
	"server/internal/repository"
)

const subscriptionLapseNoticeTicker = time.Hour

// RunSubscriptionLapseNoticeOnce emails owners once per lapsed paid period.
func RunSubscriptionLapseNoticeOnce(
	ctx context.Context,
	companyRepo *repository.CompanyRepository,
	mailer notifymail.Sender,
	publicSiteURL string,
	bc *inapp.Broadcaster,
) {
	rows, err := companyRepo.ListCompaniesForSubscriptionLapseNotice(ctx)
	if err != nil {
		logger.Log.Error().Err(err).Msg("subscription lapse notice: list companies")
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
		phrase := notifymail.FormatLapseDateEs(row.SubscriptionExpiresAt, loc)
		msg := notifymail.SubscriptionLapsed(emailAddr, row.CompanyName, phrase, publicSiteURL)
		if err := mailer.Send(ctx, msg); err != nil {
			logger.Log.Error().Err(err).Str("company_id", row.CompanyID.String()).Msg("subscription lapse email failed")
			continue
		}
		if err := companyRepo.MarkSubscriptionLapseNoticeSent(ctx, row.CompanyID); err != nil {
			logger.Log.Error().Err(err).Str("company_id", row.CompanyID.String()).Msg("subscription lapse: mark sent failed")
			continue
		}
		if bc != nil {
			bc.SubscriptionLapsed(ctx, row.CompanyID, row.CompanyName, phrase)
		}
		sent++
	}
	if sent > 0 {
		logger.Log.Info().Int("sent", sent).Int("candidates", len(rows)).Msg("subscription lapse notices processed")
	}
}

// StartSubscriptionLapseNoticeLoop runs RunSubscriptionLapseNoticeOnce hourly.
func StartSubscriptionLapseNoticeLoop(
	ctx context.Context,
	companyRepo *repository.CompanyRepository,
	mailer notifymail.Sender,
	publicSiteURL string,
	bc *inapp.Broadcaster,
) {
	ticker := time.NewTicker(subscriptionLapseNoticeTicker)
	defer ticker.Stop()

	RunSubscriptionLapseNoticeOnce(ctx, companyRepo, mailer, publicSiteURL, bc)
	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			RunSubscriptionLapseNoticeOnce(ctx, companyRepo, mailer, publicSiteURL, bc)
		}
	}
}
