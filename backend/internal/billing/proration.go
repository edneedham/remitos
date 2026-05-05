package billing

import (
	"errors"
	"math"
	"time"
)

// UpgradeProrationBreakdown mirrors website/src/app/panel/lib/prorationPreview.ts so the
// UI preview and the server charge agree.
//
// All monetary values are ARS centavos (1/100 ARS) per billing_invoices convention.
type UpgradeProrationBreakdown struct {
	FractionRemaining              float64
	DueNowMinor                    int64
	CurrentPlanRemainingValueMinor int64
	NewPlanRemainingValueMinor     int64
	PeriodStart                    time.Time
	PeriodEnd                      time.Time
}

// approximateMonthlyPeriodStart mirrors the JS `setMonth(getMonth() - 1)` behavior used in
// the panel preview so server and UI agree on the period boundaries.
func approximateMonthlyPeriodStart(periodEnd time.Time) time.Time {
	return periodEnd.AddDate(0, -1, 0)
}

// ComputeUpgradeProrationDueMinor returns how much (in ARS centavos) to charge a user moving
// from currentMonthlyMinor to newMonthlyMinor partway through a paid period. Returns
// (nil, nil) when no proration applies (period already over, or zero-length period).
func ComputeUpgradeProrationDueMinor(now, periodEnd time.Time, currentMonthlyMinor, newMonthlyMinor int64) (*UpgradeProrationBreakdown, error) {
	if periodEnd.IsZero() {
		return nil, errors.New("period end must be set")
	}
	if !periodEnd.After(now) {
		return nil, nil
	}
	if currentMonthlyMinor < 0 || newMonthlyMinor < 0 {
		return nil, errors.New("monthly amounts must be non-negative")
	}

	periodStart := approximateMonthlyPeriodStart(periodEnd)
	totalMs := float64(periodEnd.Sub(periodStart).Milliseconds())
	remainingMs := float64(periodEnd.Sub(now).Milliseconds())
	if totalMs <= 0 || remainingMs <= 0 {
		return nil, nil
	}

	frac := remainingMs / totalMs
	if frac > 1 {
		frac = 1
	}
	if frac < 0 {
		frac = 0
	}

	currentRemainingMinor := int64(math.Round(float64(currentMonthlyMinor) * frac))
	newRemainingMinor := int64(math.Round(float64(newMonthlyMinor) * frac))
	due := newRemainingMinor - currentRemainingMinor
	if due < 0 {
		due = 0
	}

	return &UpgradeProrationBreakdown{
		FractionRemaining:              frac,
		DueNowMinor:                    due,
		CurrentPlanRemainingValueMinor: currentRemainingMinor,
		NewPlanRemainingValueMinor:     newRemainingMinor,
		PeriodStart:                    periodStart,
		PeriodEnd:                      periodEnd,
	}, nil
}
