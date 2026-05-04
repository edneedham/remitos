package billing

import (
	"errors"
	"fmt"
	"math"
	"strings"
)

// DefaultFXBufferFraction is applied to the reference MEP (ARS/USD) before converting catalog USD to ARS.
const DefaultFXBufferFraction = 0.07

// ChargedARSPerUSD returns ARS per 1 USD after applying bufferFraction on top of the reference sell rate
// (e.g. MEP venta). bufferFraction 0.07 means the charged rate is reference × 1.07.
func ChargedARSPerUSD(referenceSellPerUSD float64, bufferFraction float64) float64 {
	if bufferFraction < 0 {
		bufferFraction = 0
	}
	return referenceSellPerUSD * (1 + bufferFraction)
}

// LegalNoticeAR is customer-facing Argentine Spanish for USD list + MEP-based ARS charges.
// bufferFraction is the surcharge on the reference MEP rate (e.g. 0.07 → copy says 7%).
// Keep default output in sync with website/src/app/lib/billingLegalNotice.ts.
func LegalNoticeAR(bufferFraction float64) string {
	base := "Los precios están denominados en dólares estadounidenses (USD). Los importes en pesos argentinos (ARS) se calcularán a partir del dólar MEP (bolsa) vigente en la fecha de facturación"
	if bufferFraction <= 0 {
		return base + ", y se cobrarán en pesos enteros."
	}
	pct := int(math.Round(bufferFraction * 100))
	return fmt.Sprintf("%s, con un recargo del %d%% sobre esa cotización de referencia, y se cobrarán en pesos enteros.", base, pct)
}

// MonthlyListPriceUSD is the catalog list price in USD (major units, e.g. 29 = USD 29) before FX.
// Keep in sync with website plan catalog for self-serve plans.
var MonthlyListPriceUSD = map[string]float64{
	"pyme":    29,
	"empresa": 59,
}

// MonthlyListUSD returns the list price in USD major units for a plan id (lowercased).
func MonthlyListUSD(plan string) (float64, bool) {
	v, ok := MonthlyListPriceUSD[strings.ToLower(strings.TrimSpace(plan))]
	return v, ok
}

// InvoiceAmountMinorARS converts a USD list price to ARS centavos using chargedARSPerUSD (ARS per 1 USD
// after buffer on the reference rate). The ARS total is rounded to whole pesos, then expressed as centavos
// (multiples of 100) for Mercado Pago and billing_invoices.
func InvoiceAmountMinorARS(usdMajor float64, chargedARSPerUSD float64) (int64, error) {
	if usdMajor <= 0 {
		return 0, errors.New("usd amount must be positive")
	}
	if chargedARSPerUSD <= 0 {
		return 0, errors.New("charged ars per usd must be positive")
	}
	wholeARS := math.Round(usdMajor * chargedARSPerUSD)
	minor := int64(wholeARS) * 100
	if minor <= 0 {
		return 0, fmt.Errorf("computed amount_minor invalid")
	}
	return minor, nil
}

// PlanMonthlyAmountMinorARS returns invoice amount_minor in ARS centavos for one catalog month of the plan.
func PlanMonthlyAmountMinorARS(plan string, usdToARS float64) (int64, error) {
	usd, ok := MonthlyListUSD(plan)
	if !ok {
		return 0, fmt.Errorf("unknown plan for pricing: %s", plan)
	}
	return InvoiceAmountMinorARS(usd, usdToARS)
}
