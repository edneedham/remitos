/**
 * Client-side trial onboarding instrumentation. Dispatches a DOM event and
 * pushes to dataLayer when present (GTM/GA). Safe to call from client only.
 */

/**
 * Optional UX hint (e.g. Phase 2 checklist). Safe to drop if unused — not authoritative.
 */
export const TRIAL_SUCCESS_SCREEN_VIEWED_KEY =
  'remitos_trial_success_screen_viewed_at';

/** Persisted so we only emit client-side analytics once per browser. Not authoritative. */
export const FIRST_SCAN_ANALYTICS_SENT_KEY =
  'remitos_first_scan_completed_analytics_sent';

export type TrialOnboardingEventName =
  | 'trial_started'
  | 'trial_success_screen_viewed'
  | 'trial_download_clicked'
  | 'trial_qr_viewed'
  | 'trial_fallback_web_upload_clicked'
  | 'trial_copy_link_clicked'
  | 'first_scan_completed';

/** True when NEXT_PUBLIC_ANALYTICS_DEBUG is 1/true/yes (trimmed, case-insensitive). */
export function isAnalyticsDebugEnabled(): boolean {
  try {
    const raw =
      typeof process !== 'undefined' && process.env
        ? process.env.NEXT_PUBLIC_ANALYTICS_DEBUG
        : undefined;
    const v = String(raw ?? '')
      .trim()
      .toLowerCase();
    return v === '1' || v === 'true' || v === 'yes';
  } catch {
    return false;
  }
}

export function trackTrialOnboardingEvent(
  name: TrialOnboardingEventName,
  payload?: Record<string, unknown>,
): void {
  if (typeof window === 'undefined') return;

  const detail = { event: name, ...payload };

  if (isAnalyticsDebugEnabled()) {
    try {
      // eslint-disable-next-line no-console -- intentional debug channel for staging/local validation
      console.info('[remitos:analytics]', detail);
    } catch {
      /* ignore */
    }
  }

  try {
    window.dispatchEvent(
      new CustomEvent('remitos:analytics', { detail }),
    );
  } catch {
    /* ignore */
  }

  const w = window as typeof window & { dataLayer?: unknown[] };
  w.dataLayer = w.dataLayer ?? [];
  try {
    w.dataLayer.push(detail);
  } catch {
    /* ignore */
  }
}

export function markTrialSuccessScreenViewed(): void {
  if (typeof window === 'undefined') return;
  try {
    window.localStorage.setItem(
      TRIAL_SUCCESS_SCREEN_VIEWED_KEY,
      new Date().toISOString(),
    );
  } catch {
    /* ignore */
  }
}
