/**
 * Time-based upgrade proration (difference method): charge now equals the fraction of the
 * billing period remaining × (new monthly amount − old monthly amount).
 * Period boundaries approximate one calendar month ending at `subscription_expires_at`,
 * consistent with “+1 month” activation extension used elsewhere.
 */

export function approximateMonthlyPeriodStartMs(subscriptionExpiresMs: number): number {
  const end = new Date(subscriptionExpiresMs);
  const start = new Date(end);
  start.setMonth(start.getMonth() - 1);
  return start.getTime();
}

export type UpgradeProrationBreakdown = {
  fractionRemaining: number;
  /** ARS centavos */
  dueNowMinor: number;
  /** ARS centavos — portion of current plan price attributable to remaining time */
  currentPlanRemainingValueMinor: number;
  /** ARS centavos — portion of new plan price attributable to remaining time */
  newPlanRemainingValueMinor: number;
  periodStartMs: number;
  periodEndMs: number;
};

export function computeUpgradeProrationDueMinor(params: {
  nowMs: number;
  subscriptionExpiresMs: number;
  currentMonthlyMinor: number;
  newMonthlyMinor: number;
}): UpgradeProrationBreakdown | null {
  const {
    nowMs,
    subscriptionExpiresMs,
    currentMonthlyMinor,
    newMonthlyMinor,
  } = params;

  if (
    !Number.isFinite(subscriptionExpiresMs) ||
    subscriptionExpiresMs <= nowMs
  ) {
    return null;
  }

  const periodEndMs = subscriptionExpiresMs;
  const periodStartMs = approximateMonthlyPeriodStartMs(subscriptionExpiresMs);
  const totalMs = periodEndMs - periodStartMs;
  const remainingMs = periodEndMs - nowMs;

  if (totalMs <= 0 || remainingMs <= 0) {
    return null;
  }

  const fractionRemaining = Math.min(1, Math.max(0, remainingMs / totalMs));
  const currentPlanRemainingValueMinor = Math.round(
    currentMonthlyMinor * fractionRemaining,
  );
  const newPlanRemainingValueMinor = Math.round(
    newMonthlyMinor * fractionRemaining,
  );
  const dueNowMinor = Math.max(
    0,
    newPlanRemainingValueMinor - currentPlanRemainingValueMinor,
  );

  return {
    fractionRemaining,
    dueNowMinor,
    currentPlanRemainingValueMinor,
    newPlanRemainingValueMinor,
    periodStartMs,
    periodEndMs,
  };
}
