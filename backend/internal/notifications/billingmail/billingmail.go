package billingmail

import (
	"context"
	"fmt"
	"strings"
	"time"

	"github.com/google/uuid"
	"server/internal/logger"
	notifymail "server/internal/notifications/email"
	"server/internal/repository"
)

// QueuePaymentReceipt sends a payment receipt asynchronously (deduped in DB).
func QueuePaymentReceipt(
	invoices *repository.InvoiceRepository,
	users *repository.UserRepository,
	companies *repository.CompanyRepository,
	mailer notifymail.Sender,
	publicSiteURL string,
	legalFooterAR string,
	invoiceID uuid.UUID,
) {
	go func() {
		ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
		defer cancel()
		sendPaymentReceipt(ctx, invoices, users, companies, mailer, publicSiteURL, legalFooterAR, invoiceID)
	}()
}

func sendPaymentReceipt(
	ctx context.Context,
	invoices *repository.InvoiceRepository,
	users *repository.UserRepository,
	companies *repository.CompanyRepository,
	mailer notifymail.Sender,
	publicSiteURL string,
	legalFooterAR string,
	invoiceID uuid.UUID,
) {
	inv, err := invoices.GetByID(ctx, invoiceID)
	if err != nil || inv == nil {
		logger.Log.Error().Err(err).Str("invoice_id", invoiceID.String()).Msg("payment receipt: load invoice")
		return
	}
	if inv.Status != "paid" || inv.MpPaymentID == nil || strings.TrimSpace(*inv.MpPaymentID) == "" {
		return
	}
	if inv.ReceiptEmailSentAt.Valid {
		return
	}

	to, err := users.GetCompanyOwnerPrimaryEmail(ctx, inv.CompanyID)
	if err != nil || strings.TrimSpace(to) == "" {
		logger.Log.Warn().Str("invoice_id", invoiceID.String()).Msg("payment receipt: no owner email")
		return
	}
	co, err := companies.GetByIDForBilling(ctx, inv.CompanyID)
	if err != nil || co == nil {
		logger.Log.Error().Err(err).Str("company_id", inv.CompanyID.String()).Msg("payment receipt: company")
		return
	}

	afipNote := ""
	if inv.FacturaCAE.Valid && inv.FacturaEmittedAt.Valid {
		tipoStr := "?"
		if inv.FacturaTipo.Valid {
			tipoStr = fmt.Sprintf("%d", inv.FacturaTipo.Int32)
		}
		vto := ""
		if inv.FacturaCAEVto.Valid {
			vto = inv.FacturaCAEVto.Time.Format("02/01/2006")
		}
		afipNote = fmt.Sprintf("Factura electrónica tipo %s — CAE %s (vto. %s).", tipoStr, inv.FacturaCAE.String, vto)
	}
	msg := notifymail.PaymentReceipt(
		to,
		co.Name,
		planDisplayName(co.SubscriptionPlan),
		publicSiteURL,
		inv.AmountMinor,
		inv.Currency,
		time.Now().UTC(),
		inv.ID.String(),
		strings.TrimSpace(*inv.MpPaymentID),
		legalFooterAR,
		afipNote,
	)
	if err := mailer.Send(ctx, msg); err != nil {
		logger.Log.Error().Err(err).Str("invoice_id", invoiceID.String()).Msg("payment receipt: send failed")
		return
	}
	ok, err := invoices.SetReceiptEmailSentIfUnset(ctx, invoiceID)
	if err != nil {
		logger.Log.Error().Err(err).Str("invoice_id", invoiceID.String()).Msg("payment receipt: mark sent")
		return
	}
	if !ok {
		logger.Log.Info().Str("invoice_id", invoiceID.String()).Msg("payment receipt: already marked sent (race)")
	}
}

func planDisplayName(plan string) string {
	switch strings.ToLower(strings.TrimSpace(plan)) {
	case "pyme":
		return "PyME"
	case "empresa":
		return "Empresa"
	default:
		p := strings.TrimSpace(plan)
		if p == "" {
			return "Suscripción"
		}
		return p
	}
}

// QueueRenewalChargeFailure emails once per failed renewal invoice (pending row).
func QueueRenewalChargeFailure(
	invoices *repository.InvoiceRepository,
	users *repository.UserRepository,
	companies *repository.CompanyRepository,
	mailer notifymail.Sender,
	publicSiteURL string,
	companyID uuid.UUID,
	invoiceID uuid.UUID,
	amountMinor int64,
	currency string,
	reason string,
) {
	go func() {
		ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
		defer cancel()
		sendRenewalChargeFailure(ctx, invoices, users, companies, mailer, publicSiteURL, companyID, invoiceID, amountMinor, currency, reason)
	}()
}

func sendRenewalChargeFailure(
	ctx context.Context,
	invoices *repository.InvoiceRepository,
	users *repository.UserRepository,
	companies *repository.CompanyRepository,
	mailer notifymail.Sender,
	publicSiteURL string,
	companyID uuid.UUID,
	invoiceID uuid.UUID,
	amountMinor int64,
	currency string,
	reason string,
) {
	inv, err := invoices.GetByID(ctx, invoiceID)
	if err != nil || inv == nil {
		logger.Log.Error().Err(err).Str("invoice_id", invoiceID.String()).Msg("renewal failure email: load invoice")
		return
	}
	if inv.Status != "pending" {
		return
	}
	if inv.RenewalFailureNoticeSentAt.Valid {
		return
	}

	to, err := users.GetCompanyOwnerPrimaryEmail(ctx, companyID)
	if err != nil || strings.TrimSpace(to) == "" {
		logger.Log.Warn().Str("invoice_id", invoiceID.String()).Msg("renewal failure email: no owner email")
		return
	}
	co, err := companies.GetByIDForBilling(ctx, companyID)
	if err != nil || co == nil {
		logger.Log.Error().Err(err).Str("company_id", companyID.String()).Msg("renewal failure email: company")
		return
	}

	msg := notifymail.RenewalChargeFailure(to, co.Name, amountMinor, currency, reason, publicSiteURL)
	if err := mailer.Send(ctx, msg); err != nil {
		logger.Log.Error().Err(err).Str("invoice_id", invoiceID.String()).Msg("renewal failure email: send failed")
		return
	}
	ok, err := invoices.SetRenewalFailureNoticeSentIfUnset(ctx, invoiceID)
	if err != nil {
		logger.Log.Error().Err(err).Str("invoice_id", invoiceID.String()).Msg("renewal failure email: mark sent")
		return
	}
	if !ok {
		logger.Log.Info().Str("invoice_id", invoiceID.String()).Msg("renewal failure email: already marked")
	}
}
