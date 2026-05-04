package billing

import "testing"

func TestChargedARSPerUSD(t *testing.T) {
	if v := ChargedARSPerUSD(1000, 0.07); v != 1070 {
		t.Fatalf("got %v want 1070", v)
	}
	if v := ChargedARSPerUSD(1000, 0); v != 1000 {
		t.Fatalf("got %v want 1000", v)
	}
}

func TestInvoiceAmountMinorARS(t *testing.T) {
	got, err := InvoiceAmountMinorARS(29, 1450)
	if err != nil {
		t.Fatal(err)
	}
	// 29 * 1450 = 42050 ARS → whole pesos → 4205000 centavos
	if got != 4_205_000 {
		t.Fatalf("got %d want 4205000", got)
	}
}

func TestInvoiceAmountMinorARS_roundsWholePesos(t *testing.T) {
	// 29 * 1450.4 = 42061.6 → rounds to 42062 pesos
	got, err := InvoiceAmountMinorARS(29, 1450.4)
	if err != nil {
		t.Fatal(err)
	}
	if got != 4_206_200 {
		t.Fatalf("got %d want 4206200", got)
	}
}

func TestPlanMonthlyAmountMinorARS_unknownPlan(t *testing.T) {
	_, err := PlanMonthlyAmountMinorARS("trial", 1400)
	if err == nil {
		t.Fatal("expected error")
	}
}

func TestLegalNoticeAR_defaultBufferWording(t *testing.T) {
	got := LegalNoticeAR(0.07)
	want := "Los precios están denominados en dólares estadounidenses (USD). Los importes en pesos argentinos (ARS) se calcularán a partir del dólar MEP (bolsa) vigente en la fecha de facturación, con un recargo del 7% sobre esa cotización de referencia, y se cobrarán en pesos enteros."
	if got != want {
		t.Fatalf("got %q", got)
	}
}
