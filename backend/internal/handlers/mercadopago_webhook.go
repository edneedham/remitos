package handlers

import (
	"context"
	"encoding/json"
	"io"
	"math"
	"net/http"
	"strings"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"server/internal/billing"
	"server/internal/logger"
	"server/internal/notifications/billingmail"
	notifymail "server/internal/notifications/email"
	"server/internal/payments/mercadopago"
	"server/internal/repository"
)

// MercadoPagoWebhookHandler processes Mercado Pago **payment** notifications.
// Merchant renewals set metadata (company_id, invoice_id); the handler updates the pending invoice
// or no-ops if the payment was already recorded.
type MercadoPagoWebhookHandler struct {
	Pool               *pgxpool.Pool
	Invoices           *repository.InvoiceRepository
	Companies          *repository.CompanyRepository
	Users              *repository.UserRepository
	MP                 *mercadopago.Client
	Mailer             notifymail.Sender
	PublicSiteURL      string
	FXBufferFraction   float64
}

func NewMercadoPagoWebhookHandler(
	pool *pgxpool.Pool,
	inv *repository.InvoiceRepository,
	companies *repository.CompanyRepository,
	users *repository.UserRepository,
	mp *mercadopago.Client,
	mailer notifymail.Sender,
	publicSiteURL string,
	fxBufferFraction float64,
) *MercadoPagoWebhookHandler {
	return &MercadoPagoWebhookHandler{
		Pool:             pool,
		Invoices:         inv,
		Companies:        companies,
		Users:            users,
		MP:               mp,
		Mailer:           mailer,
		PublicSiteURL:    publicSiteURL,
		FXBufferFraction: fxBufferFraction,
	}
}

// Ping responds OK so Mercado Pago (or proxies) can GET the webhook URL when validating it. Does not require MP credentials.
func (h *MercadoPagoWebhookHandler) Ping(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write([]byte("ok"))
}

// PostNotification handles MP webhooks. Responds 200 when the payload is accepted (even if ignored) to avoid endless retries.
func (h *MercadoPagoWebhookHandler) PostNotification(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		w.WriteHeader(http.StatusMethodNotAllowed)
		return
	}
	if h.MP == nil || !h.MP.HasAccessToken() {
		w.WriteHeader(http.StatusServiceUnavailable)
		return
	}

	body, err := io.ReadAll(r.Body)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		return
	}

	paymentID := extractPaymentIDFromRequest(r, body)
	if paymentID == "" {
		if t := peekNotificationType(body); t != "" {
			logger.Log.Info().Str("notification_type", t).Msg("mp webhook: acknowledged (no payment id to process)")
		}
		w.WriteHeader(http.StatusOK)
		return
	}

	if err := h.handlePayment(r.Context(), paymentID); err != nil {
		logger.Log.Error().Err(err).Str("mp_payment_id", paymentID).Msg("mercado pago webhook")
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
	w.WriteHeader(http.StatusOK)
}

func extractPaymentIDFromRequest(r *http.Request, body []byte) string {
	q := r.URL.Query()
	if strings.EqualFold(q.Get("topic"), "payment") || strings.EqualFold(q.Get("type"), "payment") {
		if id := strings.TrimSpace(q.Get("id")); id != "" {
			return id
		}
		if id := strings.TrimSpace(q.Get("data.id")); id != "" {
			return id
		}
	}

	var envelope struct {
		Type   string          `json:"type"`
		Action string          `json:"action"`
		Data   json.RawMessage `json:"data"`
	}
	if json.Unmarshal(body, &envelope) != nil {
		return ""
	}
	typ := strings.ToLower(strings.TrimSpace(envelope.Type))
	act := strings.ToLower(strings.TrimSpace(envelope.Action))
	isPayment := typ == "payment" || strings.Contains(typ, "payment") ||
		strings.Contains(act, "payment")
	if !isPayment {
		return ""
	}
	var dataObj map[string]json.RawMessage
	if json.Unmarshal(envelope.Data, &dataObj) != nil {
		return ""
	}
	rawID, ok := dataObj["id"]
	if !ok {
		return ""
	}
	return decodeJSONDataID(rawID)
}

func peekNotificationType(body []byte) string {
	var v struct {
		Type  string `json:"type"`
		Topic string `json:"topic"`
	}
	if json.Unmarshal(body, &v) != nil {
		return ""
	}
	if strings.TrimSpace(v.Type) != "" {
		return strings.TrimSpace(v.Type)
	}
	return strings.TrimSpace(v.Topic)
}

func decodeJSONDataID(raw json.RawMessage) string {
	var s string
	if json.Unmarshal(raw, &s) == nil && strings.TrimSpace(s) != "" {
		return strings.TrimSpace(s)
	}
	var num json.Number
	if json.Unmarshal(raw, &num) == nil && num != "" {
		return strings.TrimSpace(num.String())
	}
	return ""
}

func (h *MercadoPagoWebhookHandler) handlePayment(ctx context.Context, paymentID string) error {
	exists, err := h.Invoices.ExistsMpPaymentID(ctx, paymentID)
	if err != nil {
		return err
	}
	if exists {
		logger.Log.Info().Str("mp_payment_id", paymentID).Msg("mp webhook: duplicate notification skipped")
		return nil
	}

	p, err := h.MP.GetPayment(ctx, paymentID)
	if err != nil {
		msg := err.Error()
		if strings.Contains(msg, "status 404") {
			logger.Log.Warn().Err(err).Str("mp_payment_id", paymentID).Msg("mp webhook: GET /v1/payments not found")
			return nil
		}
		if strings.Contains(msg, "status 401") || strings.Contains(msg, "status 403") {
			logger.Log.Warn().Err(err).Str("mp_payment_id", paymentID).Msg("mp webhook: Mercado Pago API rejected token")
			return nil
		}
		return err
	}
	if !strings.EqualFold(strings.TrimSpace(p.Status), "approved") {
		return nil
	}

	if ok, invUUID, err := h.tryMarkRenewalInvoiceFromWebhook(ctx, p); err != nil {
		return err
	} else if ok {
		logger.Log.Info().
			Str("mp_payment_id", p.ID).
			Msg("mp webhook: renewal invoice marked paid from notification")
		if h.Users != nil && h.Mailer != nil && invUUID != uuid.Nil {
			billingmail.QueuePaymentReceipt(
				h.Invoices,
				h.Users,
				h.Companies,
				h.Mailer,
				h.PublicSiteURL,
				billing.LegalNoticeAR(h.FXBufferFraction),
				invUUID,
			)
		}
		return nil
	}

	companyID, err := h.resolveCompanyID(ctx, p)
	if err != nil {
		return err
	}
	if companyID == uuid.Nil {
		logger.Log.Warn().Str("mp_payment_id", paymentID).Msg("mp webhook: could not map payment to company")
		return nil
	}

	priorCount, err := h.Invoices.CountByCompanyID(ctx, companyID)
	if err != nil {
		return err
	}

	amountMinor := int64(math.Round(p.TransactionAmount * 100))
	if amountMinor <= 0 {
		amountMinor = 1
	}
	currency := strings.ToUpper(strings.TrimSpace(p.CurrencyID))
	if currency == "" {
		currency = "ARS"
	}
	desc := "Suscripción (Mercado Pago)"

	tx, err := h.Pool.Begin(ctx)
	if err != nil {
		return err
	}
	defer func() { _ = tx.Rollback(ctx) }()

	newInvID, err := h.Invoices.InsertPaidInvoiceTx(ctx, tx, companyID, amountMinor, currency, desc, p.ID)
	if err != nil {
		return err
	}
	extended := false
	if priorCount > 0 {
		if _, err := h.Companies.ExtendPaidSubscriptionPeriod(ctx, tx, companyID, 1); err != nil {
			return err
		}
		extended = true
		// Consume any user-scheduled downgrade so the new period starts on the chosen plan.
		if _, err := h.Companies.ApplyPendingPlanIfAny(ctx, tx, companyID, billing.PlanLimitsByID); err != nil {
			return err
		}
	}

	if err := tx.Commit(ctx); err != nil {
		return err
	}

	logger.Log.Info().
		Str("company_id", companyID.String()).
		Str("mp_payment_id", p.ID).
		Int64("amount_minor", amountMinor).
		Str("currency", currency).
		Bool("extended_subscription_period", extended).
		Msg("mp webhook: billing invoice recorded")

	if h.Users != nil && h.Mailer != nil {
		billingmail.QueuePaymentReceipt(
			h.Invoices,
			h.Users,
			h.Companies,
			h.Mailer,
			h.PublicSiteURL,
			billing.LegalNoticeAR(h.FXBufferFraction),
			newInvID,
		)
	}

	return nil
}

// tryMarkRenewalInvoiceFromWebhook updates a pending renewal invoice created by internal billing (metadata.invoice_id or external_reference).
func (h *MercadoPagoWebhookHandler) tryMarkRenewalInvoiceFromWebhook(ctx context.Context, p *mercadopago.PaymentDetails) (bool, uuid.UUID, error) {
	invID, ok := invoiceUUIDFromPayment(p)
	if !ok {
		return false, uuid.Nil, nil
	}
	inv, err := h.Invoices.GetByID(ctx, invID)
	if err != nil {
		return false, uuid.Nil, err
	}
	if inv == nil {
		return false, uuid.Nil, nil
	}
	if p.Metadata != nil {
		if rawCo := strings.TrimSpace(p.Metadata["company_id"]); rawCo != "" {
			if cid, err := uuid.Parse(rawCo); err == nil && cid != inv.CompanyID {
				return false, uuid.Nil, nil
			}
		}
	}

	tx, err := h.Pool.Begin(ctx)
	if err != nil {
		return false, uuid.Nil, err
	}
	defer func() { _ = tx.Rollback(ctx) }()

	updated, err := h.Invoices.MarkPaid(ctx, tx, invID, inv.CompanyID, p.ID)
	if err != nil {
		return false, uuid.Nil, err
	}
	if updated {
		if err := tx.Commit(ctx); err != nil {
			return false, uuid.Nil, err
		}
		return true, invID, nil
	}

	inv2, err := h.Invoices.GetByID(ctx, invID)
	if err != nil {
		_ = tx.Rollback(ctx)
		return false, uuid.Nil, err
	}
	if inv2 != nil && inv2.MpPaymentID != nil && *inv2.MpPaymentID == p.ID && inv2.Status == "paid" {
		_ = tx.Rollback(ctx)
		return true, invID, nil
	}
	_ = tx.Rollback(ctx)
	return false, uuid.Nil, nil
}

func (h *MercadoPagoWebhookHandler) resolveCompanyID(ctx context.Context, p *mercadopago.PaymentDetails) (uuid.UUID, error) {
	if p.Metadata != nil {
		if raw := strings.TrimSpace(p.Metadata["company_id"]); raw != "" {
			if id, err := uuid.Parse(raw); err == nil {
				return id, nil
			}
		}
		if raw := strings.TrimSpace(p.Metadata["invoice_id"]); raw != "" {
			if invID, err := uuid.Parse(raw); err == nil {
				inv, err := h.Invoices.GetByID(ctx, invID)
				if err != nil {
					return uuid.Nil, err
				}
				if inv != nil {
					return inv.CompanyID, nil
				}
			}
		}
	}
	if raw := strings.TrimSpace(p.ExternalReference); raw != "" {
		if invID, err := uuid.Parse(raw); err == nil {
			inv, err := h.Invoices.GetByID(ctx, invID)
			if err != nil {
				return uuid.Nil, err
			}
			if inv != nil {
				return inv.CompanyID, nil
			}
			return invID, nil
		}
	}
	return uuid.Nil, nil
}

func invoiceUUIDFromPayment(p *mercadopago.PaymentDetails) (uuid.UUID, bool) {
	if p.Metadata != nil {
		if raw := strings.TrimSpace(p.Metadata["invoice_id"]); raw != "" {
			if id, err := uuid.Parse(raw); err == nil {
				return id, true
			}
		}
	}
	if raw := strings.TrimSpace(p.ExternalReference); raw != "" {
		if id, err := uuid.Parse(raw); err == nil {
			return id, true
		}
	}
	return uuid.Nil, false
}
