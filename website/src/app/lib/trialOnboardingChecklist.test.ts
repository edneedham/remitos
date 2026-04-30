import { describe, expect, it } from 'vitest';
import { buildTrialOnboardingChecklist } from './trialOnboardingChecklist';
import type { Entitlement } from '../dashboard/lib/entitlementTypes';

function ent(partial: Partial<Entitlement>): Entitlement {
  return {
    can_download_app: true,
    ...partial,
  };
}

describe('buildTrialOnboardingChecklist', () => {
  it('returns null when first scan is already synced', () => {
    expect(
      buildTrialOnboardingChecklist(
        ent({ remitos_processed_last_30_days: 1 }),
        false,
      ),
    ).toBeNull();
    expect(
      buildTrialOnboardingChecklist(
        ent({ remitos_processed_last_30_days: 5 }),
        false,
      ),
    ).toBeNull();
  });

  it('returns null when entitlement is null', () => {
    expect(buildTrialOnboardingChecklist(null, false)).toBeNull();
  });

  it('shows checklist with no steps done when no devices and no visit', () => {
    const m = buildTrialOnboardingChecklist(
      ent({
        device_count: 0,
        remitos_processed_last_30_days: 0,
      }),
      false,
    );
    expect(m).not.toBeNull();
    expect(m!.completedCount).toBe(0);
    expect(m!.steps[0].done).toBe(false);
    expect(m!.steps[1].done).toBe(false);
    expect(m!.steps[2].done).toBe(false);
  });

  it('marks install done when download page was visited', () => {
    const m = buildTrialOnboardingChecklist(
      ent({
        device_count: 0,
        remitos_processed_last_30_days: 0,
      }),
      true,
    );
    expect(m!.completedCount).toBe(1);
    expect(m!.steps[0].done).toBe(true);
    expect(m!.steps[1].done).toBe(false);
  });

  it('marks install and login done when at least one device is registered', () => {
    const m = buildTrialOnboardingChecklist(
      ent({
        device_count: 1,
        remitos_processed_last_30_days: 0,
      }),
      false,
    );
    expect(m!.completedCount).toBe(2);
    expect(m!.steps[0].done).toBe(true);
    expect(m!.steps[1].done).toBe(true);
  });
});
