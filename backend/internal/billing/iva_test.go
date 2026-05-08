package billing

import (
	"math"
	"testing"
)

func TestSplitGrossARSWithIVA(t *testing.T) {
	neto, iva, total := SplitGrossARSWithIVA(13200, 21) // ARS 132.00 centavos -> 1.32? wait
	if math.Abs(total-132.00) > 0.01 {
		t.Fatalf("total %v", total)
	}
	if neto+iva < total-0.02 || neto+iva > total+0.02 {
		t.Fatalf("neto+iva should match total: %v + %v vs %v", neto, iva, total)
	}
}
