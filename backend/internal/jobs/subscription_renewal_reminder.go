package jobs

import (
	"context"
	"strings"
	"time"

	"server/internal/billing"
	"server/internal/logger"
	"server/internal/notifications/inapp"
	notifymail "server/internal/notifications/email"
	"server/internal/repository"
)

const subscriptionRenewalReminderTicker = time.Hour

// RunSubscriptionRenewalReminderOnce sends the 3-day renewal estimate email for eligible companies.
func RunSubscriptionRenewalReminderOnce(
	ctx context.Context,
	companyRepo *repository.CompanyRepository,
	mailer notifymail.Sender,
	quoter billing.USDARSQuoter,
	fxBufferFraction float64,
	publicSiteURL string,
	bc *inapp.Broadcaster,
) {
	if quoter == nil {
		return
	}

	rows, err := companyRepo.ListCompaniesForRenewalReminder(ctx)
	if err != nil {
		logger.Log.Error().Err(err).Msg("subscription renewal reminder: list companies")
		return
	}
	if len(rows) == 0 {
		return
	}

	q, err := quoter.Quote(ctx)
	if err != nil {
		logger.Log.Error().Err(err).Msg("subscription renewal reminder: fx quote")
		return
	}
	charged := billing.ChargedARSPerUSD(q.SellPerUSD, fxBufferFraction)

	var sent int
	for _, row := range rows {
		emailAddr := strings.TrimSpace(row.OwnerEmail)
		if emailAddr == "" {
			continue
		}

		usd, ok := billing.MonthlyListUSD(row.SubscriptionPlan)
		if !ok {
			logger.Log.Warn().
				Str("plan", row.SubscriptionPlan).
				Str("company_id", row.CompanyID.String()).
				Msg("subscription renewal reminder: unknown plan, skip")
			continue
		}

		minor, err := billing.InvoiceAmountMinorARS(usd, charged)
		if err != nil {
			logger.Log.Error().Err(err).Str("company_id", row.CompanyID.String()).Msg("subscription renewal reminder: amount")
			continue
		}
		wholeARS := minor / 100

		msg := notifymail.SubscriptionRenewalUpcoming(
			emailAddr,
			row.CompanyName,
			usd,
			wholeARS,
			row.SubscriptionExpiresAt,
			publicSiteURL,
		)
		if err := mailer.Send(ctx, msg); err != nil {
			logger.Log.Error().Err(err).Str("company_id", row.CompanyID.String()).Msg("subscription renewal reminder email failed")
			continue
		}
		if err := companyRepo.MarkRenewalReminderSent(ctx, row.CompanyID); err != nil {
			logger.Log.Error().Err(err).Str("company_id", row.CompanyID.String()).Msg("subscription renewal reminder: mark sent failed")
			continue
		}
		if bc != nil {
			bc.SubscriptionRenewalUpcoming(ctx, row.CompanyID, row.CompanyName, wholeARS)
		}
		sent++
	}

	if sent > 0 {
		logger.Log.Info().Int("sent", sent).Int("candidates", len(rows)).Msg("subscription renewal reminders processed")
	}
}

// StartSubscriptionRenewalReminderLoop runs RunSubscriptionRenewalReminderOnce periodically until ctx is cancelled.
func StartSubscriptionRenewalReminderLoop(
	ctx context.Context,
	companyRepo *repository.CompanyRepository,
	mailer notifymail.Sender,
	quoter billing.USDARSQuoter,
	fxBufferFraction float64,
	publicSiteURL string,
	bc *inapp.Broadcaster,
) {
	ticker := time.NewTicker(subscriptionRenewalReminderTicker)
	defer ticker.Stop()

	RunSubscriptionRenewalReminderOnce(ctx, companyRepo, mailer, quoter, fxBufferFraction, publicSiteURL, bc)

	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			RunSubscriptionRenewalReminderOnce(ctx, companyRepo, mailer, quoter, fxBufferFraction, publicSiteURL, bc)
		}
	}
}
