export type SelfServePlanId = 'pyme' | 'empresa';

export type SubscriptionTier =
  | SelfServePlanId
  | 'trial'
  | 'corporativo'
  | 'other';

/**
 * Maps stored subscription_plan (+ optional doc limit) to a coarse tier for upgrade UX.
 */
export function subscriptionTier(
  plan: string | undefined,
  documentsMonthlyLimit?: number,
): SubscriptionTier {
  const p = (plan ?? '').toLowerCase().trim();
  if (p === 'trial' || p === 'free') return 'trial';
  if (p === 'empresa') return 'empresa';
  if (p === 'corporativo') return 'corporativo';
  if (p === 'pyme') return 'pyme';

  if (documentsMonthlyLimit === 10_000) return 'empresa';
  if (documentsMonthlyLimit === 500) return 'pyme';

  if (['premium', 'paid', 'subscriber', 'standard'].includes(p)) {
    return 'pyme';
  }
  return 'other';
}

export type UsageUpgradeAction =
  | { type: 'href'; href: string; label: string }
  | { type: 'none' };

/**
 * When projected usage exceeds the monthly allowance, where the primary CTA should go.
 */
export function resolveUsageUpgradeAction(
  projectedOverage: number | null,
  tier: SubscriptionTier,
  hasActivePaymentPeriod: boolean,
): UsageUpgradeAction {
  if (projectedOverage === null || projectedOverage <= 0) {
    return { type: 'none' };
  }
  if (tier === 'corporativo') {
    return { type: 'none' };
  }
  if (tier === 'empresa') {
    return {
      type: 'href',
      href: '/contact',
      label: 'Hablar con ventas (Corporativo)',
    };
  }
  if (tier === 'trial' && !hasActivePaymentPeriod) {
    return {
      type: 'href',
      href: '/dashboard/activate-subscription',
      label: 'Activar plan de pago',
    };
  }
  return {
    type: 'href',
    href: '/dashboard/billing/upgrade',
    label: 'Ver cambio de plan y costos',
  };
}
