import { getPlanById } from '../../lib/planCatalog';
import type { Entitlement } from './entitlementTypes';

export function isPaidPlan(plan: string): boolean {
  switch (plan.toLowerCase().trim()) {
    case 'premium':
    case 'paid':
    case 'subscriber':
    case 'standard':
    case 'pyme':
    case 'empresa':
    case 'corporativo':
      return true;
    default:
      return false;
  }
}

export function formatPlanLabel(planRaw: string | undefined): string {
  const trimmed = (planRaw ?? '').trim();
  if (!trimmed) return '—';
  const plan = trimmed.toLowerCase();

  if (plan === 'trial') {
    return 'Prueba';
  }

  const fromCatalog = getPlanById(plan);
  if (fromCatalog) {
    return fromCatalog.name;
  }

  // Legacy DB slug (pre–self-serve catalog); treat like paid tier for display.
  if (plan === 'premium') {
    return getPlanById('empresa')?.name ?? 'Empresa';
  }

  if (['paid', 'subscriber', 'standard'].includes(plan)) {
    return trimmed;
  }

  return trimmed;
}

export function formatDateTime(value?: string): string {
  if (!value) return '—';
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return '—';
  return parsed.toLocaleString('es-AR', {
    dateStyle: 'long',
    timeStyle: 'short',
  });
}

/** Labels and flags derived from entitlement for billing UI and KPI subtitles. */
export function deriveBillingPresentation(entitlement: Entitlement | null, now: number) {
  const plan = (entitlement?.subscription_plan ?? '').toLowerCase().trim();
  const trialEndMs = entitlement?.trial_ends_at
    ? Date.parse(entitlement.trial_ends_at)
    : Number.NaN;
  const paymentEndMs = entitlement?.subscription_expires_at
    ? Date.parse(entitlement.subscription_expires_at)
    : Number.NaN;
  const isArchived = Boolean(entitlement?.archived_at);
  const companyStatus = (entitlement?.company_status ?? 'active')
    .toLowerCase()
    .trim();
  const companyBillingInactive =
    companyStatus !== '' && companyStatus !== 'active';

  const hasActiveTrial =
    !isArchived &&
    !companyBillingInactive &&
    plan === 'trial' &&
    Number.isFinite(trialEndMs) &&
    trialEndMs > now;

  const hasActivePaymentPeriod =
    !isArchived &&
    !companyBillingInactive &&
    isPaidPlan(plan) &&
    (!entitlement?.subscription_expires_at ||
      (Number.isFinite(paymentEndMs) && paymentEndMs > now));

  const isPaid = hasActivePaymentPeriod;

  let billingStatusSummary: string;
  if (isArchived) {
    billingStatusSummary = 'Empresa archivada';
  } else if (companyBillingInactive) {
    billingStatusSummary = 'Empresa no activa para facturación';
  } else if (hasActiveTrial) {
    billingStatusSummary = 'En período de prueba';
  } else if (hasActivePaymentPeriod) {
    billingStatusSummary = 'Suscripción al día';
  } else if (plan === 'trial' && Number.isFinite(trialEndMs) && trialEndMs <= now) {
    billingStatusSummary = 'Prueba finalizada';
  } else {
    billingStatusSummary = 'Sin suscripción paga activa';
  }

  let nextBillingMilestone: string;
  if (isArchived || companyBillingInactive) {
    nextBillingMilestone = '—';
  } else if (hasActiveTrial) {
    nextBillingMilestone = `Fin de prueba: ${formatDateTime(entitlement?.trial_ends_at)}`;
  } else if (
    hasActivePaymentPeriod &&
    entitlement?.subscription_expires_at &&
    Number.isFinite(paymentEndMs)
  ) {
    nextBillingMilestone = `Vencimiento del período pago: ${formatDateTime(entitlement.subscription_expires_at)}`;
  } else if (hasActivePaymentPeriod && !entitlement?.subscription_expires_at) {
    nextBillingMilestone =
      'Período pago activo (sin fecha de vencimiento registrada)';
  } else {
    nextBillingMilestone = '—';
  }

  return {
    plan,
    trialEndMs,
    paymentEndMs,
    isArchived,
    companyBillingInactive,
    hasActiveTrial,
    hasActivePaymentPeriod,
    isPaid,
    billingStatusSummary,
    nextBillingMilestone,
  };
}

/** Compact subtitle for KPI tiles on small viewports (matches `billingStatusSummary`). */
export function shortBillingStatusSummary(full: string): string {
  switch (full) {
    case 'Empresa archivada':
      return 'Archivada';
    case 'Empresa no activa para facturación':
      return 'No activa';
    case 'En período de prueba':
      return 'En prueba';
    case 'Suscripción al día':
      return 'Al día';
    case 'Prueba finalizada':
      return 'Prueba vencida';
    case 'Sin suscripción paga activa':
      return 'Sin plan pago';
    default:
      return full;
  }
}
