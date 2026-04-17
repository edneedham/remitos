package mercadopago

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"strings"
	"time"
)

const apiBase = "https://api.mercadopago.com"

// Client calls Mercado Pago to create a customer and save a card from a JS SDK card token (no charge).
// See: https://www.mercadopago.com/developers/en/reference/customers/_customers/post
// and https://www.mercadopago.com/developers/en/reference/cards/_customers_customer_id_cards/post
type Client struct {
	accessToken string
	http        *http.Client
}

func New(accessToken string) *Client {
	return &Client{
		accessToken: strings.TrimSpace(accessToken),
		http: &http.Client{
			Timeout: 30 * time.Second,
		},
	}
}

// StubResult is returned when the server runs without MERCADOPAGO_ACCESS_TOKEN but mock signup is enabled.
const StubCustomerID = "stub_mp_customer"
const StubCardID = "stub_mp_card"

// SaveCard creates a customer (if needed) and attaches the card token. No payment is created.
func (c *Client) SaveCard(ctx context.Context, email, cardToken string) (customerID, cardID string, err error) {
	email = strings.TrimSpace(email)
	cardToken = strings.TrimSpace(cardToken)
	if email == "" || cardToken == "" {
		return "", "", fmt.Errorf("mercadopago: email and card token required")
	}
	if c.accessToken == "" {
		return "", "", fmt.Errorf("mercadopago: access token not configured")
	}

	customerID, err = c.createCustomer(ctx, email)
	if err != nil {
		return "", "", err
	}
	cardID, err = c.attachCard(ctx, customerID, cardToken)
	if err != nil {
		return "", "", err
	}
	return customerID, cardID, nil
}

func (c *Client) createCustomer(ctx context.Context, email string) (string, error) {
	body, _ := json.Marshal(map[string]string{"email": email})
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, apiBase+"/v1/customers", bytes.NewReader(body))
	if err != nil {
		return "", err
	}
	req.Header.Set("Authorization", "Bearer "+c.accessToken)
	req.Header.Set("Content-Type", "application/json")

	resp, err := c.http.Do(req)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()
	raw, _ := io.ReadAll(resp.Body)
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return "", fmt.Errorf("mercadopago: create customer: status %d: %s", resp.StatusCode, strings.TrimSpace(string(raw)))
	}
	var out struct {
		ID interface{} `json:"id"`
	}
	if err := json.Unmarshal(raw, &out); err != nil {
		return "", fmt.Errorf("mercadopago: decode customer: %w", err)
	}
	return fmt.Sprint(out.ID), nil
}

func (c *Client) attachCard(ctx context.Context, customerID, cardToken string) (string, error) {
	path := fmt.Sprintf("%s/v1/customers/%s/cards", apiBase, customerID)
	body, _ := json.Marshal(map[string]string{"token": cardToken})
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, path, bytes.NewReader(body))
	if err != nil {
		return "", err
	}
	req.Header.Set("Authorization", "Bearer "+c.accessToken)
	req.Header.Set("Content-Type", "application/json")

	resp, err := c.http.Do(req)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()
	raw, _ := io.ReadAll(resp.Body)
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return "", fmt.Errorf("mercadopago: attach card: status %d: %s", resp.StatusCode, strings.TrimSpace(string(raw)))
	}
	var out struct {
		ID interface{} `json:"id"`
	}
	if err := json.Unmarshal(raw, &out); err != nil {
		return "", fmt.Errorf("mercadopago: decode card: %w", err)
	}
	return fmt.Sprint(out.ID), nil
}
