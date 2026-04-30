package billing

import (
	"errors"
	"fmt"
	"math"
	"strings"
)

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

// InvoiceAmountMinorARS converts a USD list price to ARS centavos using usdToARS (ARS charged per 1 USD).
func InvoiceAmountMinorARS(usdMajor float64, usdToARS float64) (int64, error) {
	if usdMajor <= 0 {
		return 0, errors.New("usd amount must be positive")
	}
	if usdToARS <= 0 {
		return 0, errors.New("usd to ars rate must be positive")
	}
	ars := usdMajor * usdToARS
	minor := int64(math.Round(ars * 100))
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
