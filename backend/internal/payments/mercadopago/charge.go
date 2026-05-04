package mercadopago

import (
	"context"
	"fmt"
	"strings"
)

// RenewalChargeInput drives an off-session renewal charge attempt for a saved customer/card pair.
type RenewalChargeInput struct {
	PayerEmail        string
	CustomerID        string
	CardID            string
	CompanyID         string
	AmountARS         float64
	Description       string
	ExternalReference string
	Metadata          map[string]string
}

// RenewalChargeOutput is the normalized result of a Mercado Pago payment attempt.
type RenewalChargeOutput struct {
	PaymentID string
	Approved  bool
}

// ChargeRenewal attempts to collect a renewal using POST /v1/payments and a saved card.
// When stubAutoCharge is true, no HTTP call is made (development or dry runs).
func (c *Client) ChargeRenewal(ctx context.Context, in RenewalChargeInput, stubAutoCharge bool) (RenewalChargeOutput, error) {
	in.PayerEmail = strings.TrimSpace(in.PayerEmail)
	in.CustomerID = strings.TrimSpace(in.CustomerID)
	in.CardID = strings.TrimSpace(in.CardID)
	in.CompanyID = strings.TrimSpace(in.CompanyID)
	in.ExternalReference = strings.TrimSpace(in.ExternalReference)

	if stubAutoCharge {
		ref := in.ExternalReference
		if ref == "" {
			ref = "renewal"
		}
		return RenewalChargeOutput{
			PaymentID: fmt.Sprintf("stub_%s", ref),
			Approved:  true,
		}, nil
	}

	if in.CustomerID == "" || in.CardID == "" {
		return RenewalChargeOutput{}, fmt.Errorf("mercadopago: missing saved customer or card id for renewal")
	}

	if c.accessToken == "" {
		return RenewalChargeOutput{}, fmt.Errorf("mercadopago: access token not configured")
	}

	card, err := c.GetCustomerCard(ctx, in.CustomerID, in.CardID)
	if err != nil {
		return RenewalChargeOutput{}, err
	}

	meta := make(map[string]string, 4)
	for k, v := range in.Metadata {
		if strings.TrimSpace(k) != "" {
			meta[k] = v
		}
	}
	if in.CompanyID != "" {
		meta["company_id"] = in.CompanyID
	}
	if in.ExternalReference != "" {
		meta["invoice_id"] = in.ExternalReference
	}
	meta["billing"] = "subscription_renewal"

	// Mercado Pago often accepts the saved card id as `token` together with `payer.id` (customer id).
	// If charges are rejected, use Card Brick / automatic-payments (payment profile) per region rules.
	token := card.ID
	if token == "" {
		token = in.CardID
	}

	out, err := c.CreateCardPayment(ctx, CreateCardPaymentInput{
		TransactionAmount: in.AmountARS,
		Description:       in.Description,
		PayerEmail:        in.PayerEmail,
		CustomerID:        in.CustomerID,
		Token:             token,
		PaymentMethodID:   card.PaymentMethodID,
		IssuerID:          card.IssuerID,
		ExternalReference: in.ExternalReference,
		Metadata:          meta,
		IdempotencyKey:    in.ExternalReference,
	})
	if err != nil {
		return RenewalChargeOutput{}, err
	}
	status := strings.ToLower(strings.TrimSpace(out.Status))
	approved := status == "approved"
	return RenewalChargeOutput{
		PaymentID: out.PaymentID,
		Approved:  approved,
	}, nil
}
