package mercadopago

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
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
	// PreapprovalID links subscription charges to the preapproval resource when present.
	PreapprovalID string
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

	preapprovalID := extractPreapprovalID(payload.Metadata, raw)

	return &PaymentDetails{
		ID:                strings.TrimSpace(fmt.Sprint(payload.ID)),
		Status:            strings.TrimSpace(payload.Status),
		CurrencyID:        strings.TrimSpace(payload.CurrencyID),
		TransactionAmount: payload.TransactionAmount,
		Metadata:          meta,
		ExternalReference: strings.TrimSpace(payload.ExternalReference),
		PreapprovalID:     preapprovalID,
	}, nil
}

func extractPreapprovalID(metadata map[string]any, raw []byte) string {
	if metadata != nil {
		for _, key := range []string{"preapproval_id", "preapprovalId"} {
			if v, ok := metadata[key]; ok {
				s := strings.TrimSpace(fmt.Sprint(v))
				if s != "" && s != "<nil>" {
					return s
				}
			}
		}
	}
	var loose map[string]any
	if json.Unmarshal(raw, &loose) == nil {
		if v, ok := loose["preapproval_id"]; ok {
			s := strings.TrimSpace(fmt.Sprint(v))
			if s != "" && s != "<nil>" {
				return s
			}
		}
	}
	return ""
}

// GetPaymentIDFromAuthorizedPayment loads GET /authorized_payments/{id} (subscription “invoice” / authorized charge)
// and returns nested payment.id when present. Empty string means no payment object yet.
func (c *Client) GetPaymentIDFromAuthorizedPayment(ctx context.Context, authorizedPaymentID string) (string, error) {
	authorizedPaymentID = strings.TrimSpace(authorizedPaymentID)
	if authorizedPaymentID == "" {
		return "", fmt.Errorf("mercadopago: authorized payment id required")
	}
	if c.accessToken == "" {
		return "", fmt.Errorf("mercadopago: access token not configured")
	}
	u := fmt.Sprintf("%s/authorized_payments/%s", apiBase, url.PathEscape(authorizedPaymentID))
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, u, nil)
	if err != nil {
		return "", err
	}
	req.Header.Set("Authorization", "Bearer "+c.accessToken)

	resp, err := c.http.Do(req)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()
	raw, _ := io.ReadAll(resp.Body)
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return "", fmt.Errorf("mercadopago: get authorized_payment: status %d: %s", resp.StatusCode, strings.TrimSpace(string(raw)))
	}
	var out struct {
		Payment *struct {
			ID interface{} `json:"id"`
		} `json:"payment"`
	}
	if err := json.Unmarshal(raw, &out); err != nil {
		return "", fmt.Errorf("mercadopago: decode authorized_payment: %w", err)
	}
	if out.Payment == nil {
		return "", nil
	}
	pid := strings.TrimSpace(fmt.Sprint(out.Payment.ID))
	if pid == "" || pid == "<nil>" {
		return "", nil
	}
	return pid, nil
}
