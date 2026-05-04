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

// CreatePreapprovalWithPlanInput creates a subscription (preapproval) linked to a preapproval_plan_id from the MP dashboard.
// See: https://www.mercadopago.com.ar/developers/en/docs/subscriptions/integration-configuration/subscription-associated-plan
type CreatePreapprovalWithPlanInput struct {
	PreapprovalPlanID string
	Reason            string
	ExternalReference string
	PayerEmail        string
	CardTokenID       string
	BackURL           string
	Metadata          map[string]string
}

// PreapprovalCreated is a minimal view of the POST /preapproval response.
type PreapprovalCreated struct {
	ID     string
	Status string
	// PayerID is set when MP returns a payer id (optional).
	PayerID string
}

// CreatePreapprovalWithPlan calls POST /preapproval to subscribe a payer to a plan.
func (c *Client) CreatePreapprovalWithPlan(ctx context.Context, in CreatePreapprovalWithPlanInput) (PreapprovalCreated, error) {
	if c.accessToken == "" {
		return PreapprovalCreated{}, fmt.Errorf("mercadopago: access token not configured")
	}
	in.PreapprovalPlanID = strings.TrimSpace(in.PreapprovalPlanID)
	in.CardTokenID = strings.TrimSpace(in.CardTokenID)
	in.PayerEmail = strings.TrimSpace(in.PayerEmail)
	if in.PreapprovalPlanID == "" || in.CardTokenID == "" || in.PayerEmail == "" {
		return PreapprovalCreated{}, fmt.Errorf("mercadopago: preapproval_plan_id, card_token_id and payer_email are required")
	}
	if in.Reason == "" {
		in.Reason = "Suscripción"
	}
	body := map[string]any{
		"preapproval_plan_id": in.PreapprovalPlanID,
		"reason":              in.Reason,
		"external_reference":  strings.TrimSpace(in.ExternalReference),
		"payer_email":         in.PayerEmail,
		"card_token_id":       in.CardTokenID,
		"status":              "authorized",
	}
	if strings.TrimSpace(in.BackURL) != "" {
		body["back_url"] = strings.TrimSpace(in.BackURL)
	}
	if len(in.Metadata) > 0 {
		body["metadata"] = in.Metadata
	}

	rawBody, _ := json.Marshal(body)
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, apiBase+"/preapproval", bytes.NewReader(rawBody))
	if err != nil {
		return PreapprovalCreated{}, err
	}
	req.Header.Set("Authorization", "Bearer "+c.accessToken)
	req.Header.Set("Content-Type", "application/json")

	resp, err := c.http.Do(req)
	if err != nil {
		return PreapprovalCreated{}, err
	}
	defer resp.Body.Close()
	raw, _ := io.ReadAll(resp.Body)
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return PreapprovalCreated{}, fmt.Errorf("mercadopago: create preapproval: status %d: %s", resp.StatusCode, strings.TrimSpace(string(raw)))
	}
	var out struct {
		ID     interface{} `json:"id"`
		Status string      `json:"status"`
		Payer  *struct {
			ID interface{} `json:"id"`
		} `json:"payer"`
	}
	if err := json.Unmarshal(raw, &out); err != nil {
		return PreapprovalCreated{}, fmt.Errorf("mercadopago: decode preapproval: %w", err)
	}
	id := strings.TrimSpace(fmt.Sprint(out.ID))
	if id == "" || id == "<nil>" {
		return PreapprovalCreated{}, fmt.Errorf("mercadopago: preapproval response missing id")
	}
	payerID := ""
	if out.Payer != nil {
		payerID = strings.TrimSpace(fmt.Sprint(out.Payer.ID))
	}
	return PreapprovalCreated{
		ID:      id,
		Status:  strings.TrimSpace(out.Status),
		PayerID: payerID,
	}, nil
}
