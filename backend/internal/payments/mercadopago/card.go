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

// SavedCardDetails is a subset of GET /v1/customers/{id}/cards/{id} needed to charge the card.
type SavedCardDetails struct {
	ID              string
	PaymentMethodID string
	IssuerID        string
	ExpirationMonth int
	ExpirationYear  int
	FirstSixDigits  string
	LastFourDigits  string
}

// GetCustomerCard loads a saved card for the customer (numeric or string card id).
func (c *Client) GetCustomerCard(ctx context.Context, customerID, cardID string) (*SavedCardDetails, error) {
	customerID = strings.TrimSpace(customerID)
	cardID = strings.TrimSpace(cardID)
	if customerID == "" || cardID == "" {
		return nil, fmt.Errorf("mercadopago: customer id and card id required")
	}
	if c.accessToken == "" {
		return nil, fmt.Errorf("mercadopago: access token not configured")
	}

	u := fmt.Sprintf("%s/v1/customers/%s/cards/%s",
		c.apiBaseURL(),
		url.PathEscape(customerID),
		url.PathEscape(cardID),
	)
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, u, nil)
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
		return nil, fmt.Errorf("mercadopago: get card: status %d: %s", resp.StatusCode, strings.TrimSpace(string(raw)))
	}

	var payload struct {
		ID              interface{} `json:"id"`
		ExpirationMonth int         `json:"expiration_month"`
		ExpirationYear  int         `json:"expiration_year"`
		FirstSixDigits  string      `json:"first_six_digits"`
		LastFourDigits  string      `json:"last_four_digits"`
		PaymentMethod   *struct {
			ID string `json:"id"`
		} `json:"payment_method"`
		Issuer *struct {
			ID interface{} `json:"id"`
		} `json:"issuer"`
	}
	if err := json.Unmarshal(raw, &payload); err != nil {
		return nil, fmt.Errorf("mercadopago: decode card: %w", err)
	}

	out := &SavedCardDetails{
		ID:              strings.TrimSpace(fmt.Sprint(payload.ID)),
		ExpirationMonth: payload.ExpirationMonth,
		ExpirationYear:  payload.ExpirationYear,
		FirstSixDigits:  strings.TrimSpace(payload.FirstSixDigits),
		LastFourDigits:  strings.TrimSpace(payload.LastFourDigits),
	}
	if payload.PaymentMethod != nil {
		out.PaymentMethodID = strings.TrimSpace(payload.PaymentMethod.ID)
	}
	if payload.Issuer != nil && payload.Issuer.ID != nil {
		out.IssuerID = strings.TrimSpace(fmt.Sprint(payload.Issuer.ID))
	}
	if out.PaymentMethodID == "" {
		return nil, fmt.Errorf("mercadopago: card response missing payment_method.id")
	}
	if out.IssuerID == "" {
		// Some cards omit issuer; issuer_id may still be required for payment create — caller may fail MP-side.
		out.IssuerID = ""
	}
	return out, nil
}
