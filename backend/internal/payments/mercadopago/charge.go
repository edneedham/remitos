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
	AmountARS         float64
	Description       string
	ExternalReference string
}

// RenewalChargeOutput is the normalized result of a Mercado Pago payment attempt.
type RenewalChargeOutput struct {
	PaymentID string
	Approved  bool
}

// ChargeRenewal attempts to collect a renewal. When stubAutoCharge is true, no HTTP call is made
// (development or dry runs). Otherwise, a real automatic debit requires Mercado Pago Subscriptions
// (preapproval / Orders recurring): charging a saved card with only customer+card IDs is not enabled here.
func (c *Client) ChargeRenewal(ctx context.Context, in RenewalChargeInput, stubAutoCharge bool) (RenewalChargeOutput, error) {
	_ = ctx
	in.PayerEmail = strings.TrimSpace(in.PayerEmail)
	in.CustomerID = strings.TrimSpace(in.CustomerID)
	in.CardID = strings.TrimSpace(in.CardID)
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

	if c.HasAccessToken() {
		return RenewalChargeOutput{}, fmt.Errorf(
			"mercadopago: live automatic renewal is not wired to Payments API (saved cards require CVV per transaction); integrate Subscriptions with authorized payment or Orders recurring, or set BILLING_STUB_AUTO_CHARGE=true for development",
		)
	}

	return RenewalChargeOutput{}, fmt.Errorf("mercadopago: access token not configured")
}
