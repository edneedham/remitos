package billing

import "math"

// SplitGrossARSWithIVA decomposes a gross ARS total (from amount_minor centavos) into neto + IVA
// for Factura A using the given IVA percentage (e.g. 21). Amounts are rounded to 2 decimals.
func SplitGrossARSWithIVA(amountMinor int64, ivaPercent float64) (neto, iva, total float64) {
	total = float64(amountMinor) / 100.0
	if ivaPercent <= 0 {
		ivaPercent = 21
	}
	denom := 1.0 + ivaPercent/100.0
	neto = math.Round((total/denom)*100) / 100
	iva = math.Round((total-neto)*100) / 100
	return neto, iva, total
}
