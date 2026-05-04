package mercadopago

import (
	"context"
	"testing"
)

func TestChargeRenewal_StubApproves(t *testing.T) {
	c := New("")
	out, err := c.ChargeRenewal(context.Background(), RenewalChargeInput{
		ExternalReference: "inv-1",
	}, true)
	if err != nil {
		t.Fatal(err)
	}
	if !out.Approved || out.PaymentID == "" {
		t.Fatalf("got %+v", out)
	}
}

func TestChargeRenewal_LiveRequiresIntegration(t *testing.T) {
	c := New("TEST-TOKEN")
	_, err := c.ChargeRenewal(context.Background(), RenewalChargeInput{
		PayerEmail:        "a@b.com",
		CustomerID:        "cust",
		CardID:            "card",
		AmountARS:         10,
		Description:       "test",
		ExternalReference: "inv-2",
	}, false)
	if err == nil {
		t.Fatal("expected error until Orders/Subscriptions integration")
	}
}
