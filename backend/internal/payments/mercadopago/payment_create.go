package mercadopago

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"strings"
)

// CreateCardPaymentInput creates an off-session card payment (merchant-initiated renewal).
type CreateCardPaymentInput struct {
	TransactionAmount float64
	Description       string
	PayerEmail        string
	CustomerID        string
	// Token is usually the card id for saved cards when charging via Payments API (Mercado Pago may require a fresh Card Brick token in some regions).
	Token             string
	PaymentMethodID   string
	IssuerID          string
	ExternalReference string
	Metadata          map[string]string
	IdempotencyKey    string
}

// CreateCardPaymentOutput is the normalized result of POST /v1/payments.
type CreateCardPaymentOutput struct {
	PaymentID string
	Status    string
}

// CreateCardPayment calls POST /v1/payments with X-Idempotency-Key.
func (c *Client) CreateCardPayment(ctx context.Context, in CreateCardPaymentInput) (*CreateCardPaymentOutput, error) {
	in.PayerEmail = strings.TrimSpace(in.PayerEmail)
	in.CustomerID = strings.TrimSpace(in.CustomerID)
	in.Token = strings.TrimSpace(in.Token)
	in.PaymentMethodID = strings.TrimSpace(in.PaymentMethodID)
	in.IssuerID = strings.TrimSpace(in.IssuerID)
	in.ExternalReference = strings.TrimSpace(in.ExternalReference)
	in.IdempotencyKey = strings.TrimSpace(in.IdempotencyKey)
	if in.TransactionAmount <= 0 {
		return nil, fmt.Errorf("mercadopago: transaction amount must be positive")
	}
	if in.Token == "" || in.PaymentMethodID == "" {
		return nil, fmt.Errorf("mercadopago: token and payment_method_id required")
	}
	if c.accessToken == "" {
		return nil, fmt.Errorf("mercadopago: access token not configured")
	}
	if in.PayerEmail == "" {
		return nil, fmt.Errorf("mercadopago: payer email required")
	}

	body := map[string]any{
		"transaction_amount": in.TransactionAmount,
		"token":              in.Token,
		"description":        in.Description,
		"installments":       1,
		"payment_method_id":  in.PaymentMethodID,
		"payer": map[string]any{
			"email": in.PayerEmail,
			"type":  "customer",
			"id":    in.CustomerID,
		},
		"capture": true,
	}
	if in.IssuerID != "" {
		body["issuer_id"] = in.IssuerID
	}
	if in.ExternalReference != "" {
		body["external_reference"] = in.ExternalReference
	}
	if len(in.Metadata) > 0 {
		meta := make(map[string]any, len(in.Metadata))
		for k, v := range in.Metadata {
			meta[k] = v
		}
		body["metadata"] = meta
	}

	rawBody, err := json.Marshal(body)
	if err != nil {
		return nil, err
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, c.apiBaseURL()+"/v1/payments", bytes.NewReader(rawBody))
	if err != nil {
		return nil, err
	}
	req.Header.Set("Authorization", "Bearer "+c.accessToken)
	req.Header.Set("Content-Type", "application/json")
	if in.IdempotencyKey != "" {
		req.Header.Set("X-Idempotency-Key", in.IdempotencyKey)
	}

	resp, err := c.http.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	raw, _ := io.ReadAll(resp.Body)
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return nil, fmt.Errorf("mercadopago: create payment: status %d: %s", resp.StatusCode, strings.TrimSpace(string(raw)))
	}

	var out struct {
		ID     interface{} `json:"id"`
		Status string      `json:"status"`
	}
	if err := json.Unmarshal(raw, &out); err != nil {
		return nil, fmt.Errorf("mercadopago: decode create payment: %w", err)
	}
	pid := strings.TrimSpace(fmt.Sprint(out.ID))
	if pid == "" || pid == "<nil>" {
		return nil, fmt.Errorf("mercadopago: create payment: missing id")
	}
	return &CreateCardPaymentOutput{
		PaymentID: pid,
		Status:    strings.TrimSpace(out.Status),
	}, nil
}
