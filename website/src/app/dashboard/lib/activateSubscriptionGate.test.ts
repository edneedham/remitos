import { describe, expect, it } from 'vitest';
import { needsActivateSubscription } from './activateSubscriptionGate';
import type { Entitlement } from './entitlementTypes';

function base(): Entitlement {
  return {
    can_download_app: false,
    subscription_plan: 'trial',
    company_status: 'active',
  };
}

describe('needsActivateSubscription', () => {
  it('is false when entitlement is null', () => {
    expect(needsActivateSubscription(null)).toBe(false);
  });

  it('is false when download is allowed', () => {
    expect(needsActivateSubscription({ ...base(), can_download_app: true })).toBe(
      false,
    );
  });

  it('is false when archived', () => {
    expect(
      needsActivateSubscription({
        ...base(),
        archived_at: new Date().toISOString(),
      }),
    ).toBe(false);
  });

  it('is false when company is not active', () => {
    expect(
      needsActivateSubscription({ ...base(), company_status: 'suspended' }),
    ).toBe(false);
  });

  it('is true when active company cannot download', () => {
    expect(needsActivateSubscription(base())).toBe(true);
  });
});
