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
	"server/internal/logger"
	"server/internal/payments/mercadopago"
	"server/internal/repository"
)

// MercadoPagoWebhookHandler processes Mercado Pago notifications.
//
// - topic/type **payment**: `data.id` is a payment id → GET /v1/payments/{id}.
// - **subscription_authorized_payment** (Planes y suscripciones): `data.id` is an authorized-payment (invoice) id → GET /authorized_payments/{id} → nested `payment.id`.
// - Other subscription topics (e.g. preapproval) are acknowledged with 200 and optional info logs only.
//
// Internal cron renewal must not run for companies with mp_preapproval_id.
type MercadoPagoWebhookHandler struct {
	Pool      *pgxpool.Pool
	Invoices  *repository.InvoiceRepository
	Companies *repository.CompanyRepository
	MP        *mercadopago.Client
}

func NewMercadoPagoWebhookHandler(pool *pgxpool.Pool, inv *repository.InvoiceRepository, companies *repository.CompanyRepository, mp *mercadopago.Client) *MercadoPagoWebhookHandler {
	return &MercadoPagoWebhookHandler{
		Pool:      pool,
		Invoices:  inv,
		Companies: companies,
		MP:        mp,
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
		if apID := extractAuthorizedPaymentResourceID(body); apID != "" {
			pid, rerr := h.MP.GetPaymentIDFromAuthorizedPayment(r.Context(), apID)
			if rerr != nil {
				msg := rerr.Error()
				if strings.Contains(msg, "status 404") || strings.Contains(msg, "status 401") || strings.Contains(msg, "status 403") {
					logger.Log.Warn().Err(rerr).Str("authorized_payment_id", apID).Msg("mp webhook: GET authorized_payments failed")
					w.WriteHeader(http.StatusOK)
					return
				}
				logger.Log.Error().Err(rerr).Str("authorized_payment_id", apID).Msg("mp webhook: authorized_payment fetch")
				w.WriteHeader(http.StatusInternalServerError)
				return
			}
			if pid == "" {
				logger.Log.Info().Str("authorized_payment_id", apID).Msg("mp webhook: authorized payment has no nested payment yet (pending)")
				w.WriteHeader(http.StatusOK)
				return
			}
			paymentID = pid
		}
	}
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
	// This topic carries an authorized-payment id, not a payment id (handled separately in PostNotification).
	if strings.Contains(typ, "subscription_authorized_payment") {
		return ""
	}
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

// extractAuthorizedPaymentResourceID returns data.id for subscription_authorized_payment notifications.
func extractAuthorizedPaymentResourceID(body []byte) string {
	var envelope struct {
		Type string          `json:"type"`
		Data json.RawMessage `json:"data"`
	}
	if json.Unmarshal(body, &envelope) != nil {
		return ""
	}
	t := strings.ToLower(strings.TrimSpace(envelope.Type))
	if !strings.Contains(t, "subscription_authorized_payment") {
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
		// Wrong resource type (e.g. id is not a payment), test/prod mismatch, or stale id — acknowledge webhook.
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

	if err := h.Invoices.InsertPaidInvoiceTx(ctx, tx, companyID, amountMinor, currency, desc, p.ID); err != nil {
		return err
	}
	extended := false
	if priorCount > 0 {
		if _, err := h.Companies.ExtendPaidSubscriptionPeriod(ctx, tx, companyID, 1); err != nil {
			return err
		}
		extended = true
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

	return nil
}

func (h *MercadoPagoWebhookHandler) resolveCompanyID(ctx context.Context, p *mercadopago.PaymentDetails) (uuid.UUID, error) {
	if p.Metadata != nil {
		if raw := strings.TrimSpace(p.Metadata["company_id"]); raw != "" {
			if id, err := uuid.Parse(raw); err == nil {
				return id, nil
			}
		}
	}
	if raw := strings.TrimSpace(p.ExternalReference); raw != "" {
		if id, err := uuid.Parse(raw); err == nil {
			return id, nil
		}
	}
	if raw := strings.TrimSpace(p.PreapprovalID); raw != "" {
		return h.Companies.GetCompanyIDByMpPreapprovalID(ctx, raw)
	}
	return uuid.Nil, nil
}
