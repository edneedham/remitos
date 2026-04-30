package billing

import "testing"

func TestInvoiceAmountMinorARS(t *testing.T) {
	got, err := InvoiceAmountMinorARS(29, 1450)
	if err != nil {
		t.Fatal(err)
	}
	// 29 * 1450 = 42050 ARS → 4205000 centavos
	if got != 4_205_000 {
		t.Fatalf("got %d want 4205000", got)
	}
}

func TestPlanMonthlyAmountMinorARS_unknownPlan(t *testing.T) {
	_, err := PlanMonthlyAmountMinorARS("trial", 1400)
	if err == nil {
		t.Fatal("expected error")
	}
}
