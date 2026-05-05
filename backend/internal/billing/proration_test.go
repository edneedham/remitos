package billing

import (
	"testing"
	"time"
)

func TestComputeUpgradeProrationDueMinor_HalfPeriod(t *testing.T) {
	periodEnd := time.Date(2026, 5, 31, 23, 59, 59, 0, time.UTC)
	periodStart := approximateMonthlyPeriodStart(periodEnd)
	mid := periodStart.Add(periodEnd.Sub(periodStart) / 2)

	out, err := ComputeUpgradeProrationDueMinor(mid, periodEnd, 100_000, 200_000)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if out == nil {
		t.Fatal("expected breakdown, got nil")
	}
	if out.DueNowMinor != 50_000 {
		t.Errorf("expected 50_000 due, got %d", out.DueNowMinor)
	}
	if !(out.FractionRemaining > 0.499 && out.FractionRemaining < 0.501) {
		t.Errorf("expected fraction ~0.5, got %f", out.FractionRemaining)
	}
}

func TestComputeUpgradeProrationDueMinor_PastPeriod(t *testing.T) {
	now := time.Now()
	out, err := ComputeUpgradeProrationDueMinor(now, now.Add(-24*time.Hour), 100_000, 200_000)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if out != nil {
		t.Fatalf("expected nil breakdown for past period, got %+v", out)
	}
}

func TestComputeUpgradeProrationDueMinor_DowngradeReturnsZero(t *testing.T) {
	periodEnd := time.Date(2026, 5, 31, 23, 59, 59, 0, time.UTC)
	periodStart := approximateMonthlyPeriodStart(periodEnd)
	mid := periodStart.Add(periodEnd.Sub(periodStart) / 2)

	// New plan cheaper than current — proration handler treats this as "no charge",
	// downgrade is scheduled separately for next period.
	out, err := ComputeUpgradeProrationDueMinor(mid, periodEnd, 200_000, 100_000)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if out == nil {
		t.Fatal("expected breakdown, got nil")
	}
	if out.DueNowMinor != 0 {
		t.Errorf("expected 0 due for downgrade, got %d", out.DueNowMinor)
	}
}

func TestComputeUpgradeProrationDueMinor_FullPeriodRemaining(t *testing.T) {
	periodEnd := time.Date(2026, 5, 31, 23, 59, 59, 0, time.UTC)
	periodStart := approximateMonthlyPeriodStart(periodEnd)

	out, err := ComputeUpgradeProrationDueMinor(periodStart, periodEnd, 100_000, 200_000)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if out == nil {
		t.Fatal("expected breakdown, got nil")
	}
	if !(out.FractionRemaining > 0.999 && out.FractionRemaining <= 1.0) {
		t.Errorf("expected fraction ~1, got %f", out.FractionRemaining)
	}
	if out.DueNowMinor != 100_000 {
		t.Errorf("expected 100_000 due, got %d", out.DueNowMinor)
	}
}
