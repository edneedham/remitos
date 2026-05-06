package jobs

import (
	"context"
	"sync"
	"time"

	"github.com/google/uuid"
	"server/internal/billing"
	"server/internal/logger"
	"server/internal/repository"
)

// renewalSweepConcurrency bounds parallel Mercado Pago renewal attempts per tick (avoid hammering MP API).
const renewalSweepConcurrency = 8

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

	workers := renewalSweepConcurrency
	if workers > len(ids) {
		workers = len(ids)
	}
	sem := make(chan struct{}, workers)
	var wg sync.WaitGroup
	var failMu sync.Mutex
	failed := 0

	for _, id := range ids {
		wg.Add(1)
		sem <- struct{}{}
		go func(cid uuid.UUID) {
			defer wg.Done()
			defer func() { <-sem }()
			_, err := svc.Run(ctx, billing.RenewalRunInput{
				CompanyID:    cid,
				AmountMinor:  0,
				Currency:     "ARS",
				Description:  "Suscripción mensual",
				ExtendMonths: 1,
			})
			if err != nil {
				failMu.Lock()
				failed++
				failMu.Unlock()
				logger.Log.Warn().Err(err).Str("company_id", cid.String()).Msg("billing renewal sweep: run failed")
			}
		}(id)
	}
	wg.Wait()
	logger.Log.Info().
		Int("candidates", len(ids)).
		Int("failed", failed).
		Int("succeeded", len(ids)-failed).
		Msg("billing renewal sweep: tick complete")
}
