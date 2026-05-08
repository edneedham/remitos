package jobs

import (
	"context"
	"time"

	"server/internal/billing"
	"server/internal/logger"
	"server/internal/repository"
)

// StartBillingFacturaEmitLoop periodically retries AFIP emission for paid invoices that have
// not yet received a CAE (AFIP outage, missing CUIT then fixed, etc.).
func StartBillingFacturaEmitLoop(ctx context.Context, emitter *billing.FacturaEmitter, invoices *repository.InvoiceRepository, poll time.Duration) {
	if emitter == nil || !emitter.BillingEnabled || invoices == nil {
		return
	}
	if poll <= 0 {
		poll = 10 * time.Minute
	}
	tick := time.NewTicker(poll)
	go func() {
		for {
			select {
			case <-ctx.Done():
				tick.Stop()
				return
			case <-tick.C:
				ids, err := invoices.ListPaidInvoicesPendingFactura(context.Background(), 30)
				if err != nil {
					logger.Log.Warn().Err(err).Msg("billing factura sweep: list")
					continue
				}
				for _, id := range ids {
					cctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
					err := emitter.TryEmit(cctx, id)
					cancel()
					if err != nil {
						logger.Log.Debug().Err(err).Str("invoice_id", id.String()).Msg("billing factura sweep: try emit")
					}
				}
			}
		}
	}()
	logger.Log.Info().Dur("poll", poll).Msg("Billing AFIP factura retry loop started")
}
