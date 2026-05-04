package jobs

import (
	"context"
	"time"

	"server/internal/billing"
	"server/internal/logger"
	"server/internal/repository"
)

// StartBillingRenewalSweep periodically attempts Mercado Pago renewals for companies whose paid period has ended.
// Requires merchant-owned billing (saved card + ChargeRenewal); enable only after validating charges in sandbox/production.
func StartBillingRenewalSweep(ctx context.Context, svc *billing.RenewalService, companies *repository.CompanyRepository, poll time.Duration) {
	if poll <= 0 {
		poll = time.Hour
	}
	ticker := time.NewTicker(poll)
	go func() {
		defer ticker.Stop()
		for {
			select {
			case <-ctx.Done():
				return
			case <-ticker.C:
				runBillingRenewalSweep(context.Background(), svc, companies)
			}
		}
	}()
	logger.Log.Info().
		Dur("poll_interval", poll).
		Msg("Billing automatic renewal sweep started")
}

func runBillingRenewalSweep(ctx context.Context, svc *billing.RenewalService, companies *repository.CompanyRepository) {
	ids, err := companies.ListCompanyIDsDueForSubscriptionRenewal(ctx, 200)
	if err != nil {
		logger.Log.Error().Err(err).Msg("billing renewal sweep: list companies")
		return
	}
	if len(ids) == 0 {
		return
	}
	for _, id := range ids {
		_, err := svc.Run(ctx, billing.RenewalRunInput{
			CompanyID:    id,
			AmountMinor:  0,
			Currency:     "ARS",
			Description:  "Suscripción mensual",
			ExtendMonths: 1,
		})
		if err != nil {
			logger.Log.Warn().Err(err).Str("company_id", id.String()).Msg("billing renewal sweep: run failed")
		}
	}
}
