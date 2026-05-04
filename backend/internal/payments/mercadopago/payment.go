package mercadopago

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"strings"
)

// PaymentDetails is a subset of GET /v1/payments/{id} used for webhooks and reconciliation.
type PaymentDetails struct {
	ID                string
	Status            string
	CurrencyID        string
	TransactionAmount float64
	Metadata          map[string]string
	ExternalReference string
}

// GetPayment loads a payment by id (string or numeric id from notifications).
func (c *Client) GetPayment(ctx context.Context, paymentID string) (*PaymentDetails, error) {
	paymentID = strings.TrimSpace(paymentID)
	if paymentID == "" {
		return nil, fmt.Errorf("mercadopago: payment id required")
	}
	if c.accessToken == "" {
		return nil, fmt.Errorf("mercadopago: access token not configured")
	}
	url := fmt.Sprintf("%s/v1/payments/%s", apiBase, paymentID)
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, url, nil)
	if err != nil {
		return nil, err
	}
	req.Header.Set("Authorization", "Bearer "+c.accessToken)

	resp, err := c.http.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	raw, _ := io.ReadAll(resp.Body)
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return nil, fmt.Errorf("mercadopago: get payment: status %d: %s", resp.StatusCode, strings.TrimSpace(string(raw)))
	}

	var payload struct {
		ID                interface{}    `json:"id"`
		Status            string         `json:"status"`
		CurrencyID        string         `json:"currency_id"`
		TransactionAmount float64        `json:"transaction_amount"`
		Metadata          map[string]any `json:"metadata"`
		ExternalReference string         `json:"external_reference"`
	}

	if err := json.Unmarshal(raw, &payload); err != nil {
		return nil, fmt.Errorf("mercadopago: decode payment: %w", err)
	}

	meta := make(map[string]string)
	for k, v := range payload.Metadata {
		meta[k] = strings.TrimSpace(fmt.Sprint(v))
	}

	return &PaymentDetails{
		ID:                strings.TrimSpace(fmt.Sprint(payload.ID)),
		Status:            strings.TrimSpace(payload.Status),
		CurrencyID:        strings.TrimSpace(payload.CurrencyID),
		TransactionAmount: payload.TransactionAmount,
		Metadata:          meta,
		ExternalReference: strings.TrimSpace(payload.ExternalReference),
	}, nil
}
