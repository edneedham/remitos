import type { Entitlement } from './entitlementTypes';

/**
 * True when the logged-in company should complete paid activation before using
 * entitled product features (mirrors server-side download entitlement, excluding archived / inactive companies).
 */
export function needsActivateSubscription(entitlement: Entitlement | null): boolean {
  if (!entitlement) {
    return false;
  }
  if (entitlement.archived_at) {
    return false;
  }
  const st = (entitlement.company_status ?? 'active').toLowerCase().trim();
  if (st && st !== 'active') {
    return false;
  }
  if (entitlement.can_download_app === true) {
    return false;
  }
  return true;
}
