import { describe, expect, it } from 'vitest';
import {
  approximateMonthlyPeriodStartMs,
  computeUpgradeProrationDueMinor,
} from './prorationPreview';

describe('computeUpgradeProrationDueMinor', () => {
  it('charges half the monthly difference when half the period remains', () => {
    const periodEndMs = Date.UTC(2026, 4, 31, 23, 59, 59);
    const periodStartMs = approximateMonthlyPeriodStartMs(periodEndMs);
    const midMs = periodStartMs + (periodEndMs - periodStartMs) / 2;

    const oldMinor = 100_000; // ARS 1000.00
    const newMinor = 200_000; // ARS 2000.00

    const out = computeUpgradeProrationDueMinor({
      nowMs: midMs,
      subscriptionExpiresMs: periodEndMs,
      currentMonthlyMinor: oldMinor,
      newMonthlyMinor: newMinor,
    });

    expect(out).not.toBeNull();
    expect(out!.fractionRemaining).toBeCloseTo(0.5, 5);
    expect(out!.dueNowMinor).toBe(50_000);
  });

  it('returns null when period already ended', () => {
    const end = Date.now() - 86_400_000;
    const out = computeUpgradeProrationDueMinor({
      nowMs: Date.now(),
      subscriptionExpiresMs: end,
      currentMonthlyMinor: 100_000,
      newMonthlyMinor: 200_000,
    });
    expect(out).toBeNull();
  });
});
