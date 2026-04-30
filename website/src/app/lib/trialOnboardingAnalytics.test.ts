import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

describe('trialOnboardingAnalytics', () => {
  const infoSpy = vi.spyOn(console, 'info').mockImplementation(() => {});

  beforeEach(() => {
    vi.unstubAllEnvs();
    infoSpy.mockClear();
  });

  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it('does not log when NEXT_PUBLIC_ANALYTICS_DEBUG is unset', async () => {
    vi.stubEnv('NEXT_PUBLIC_ANALYTICS_DEBUG', '');
    vi.resetModules();
    const { trackTrialOnboardingEvent } = await import('./trialOnboardingAnalytics');
    trackTrialOnboardingEvent('trial_started', { source: 'test' });
    expect(infoSpy).not.toHaveBeenCalled();
  });

  it('logs [remitos:analytics] detail when NEXT_PUBLIC_ANALYTICS_DEBUG=1', async () => {
    vi.stubEnv('NEXT_PUBLIC_ANALYTICS_DEBUG', '1');
    vi.resetModules();
    const { trackTrialOnboardingEvent } = await import('./trialOnboardingAnalytics');
    trackTrialOnboardingEvent('trial_started', { source: 'signup_plan_selected' });
    expect(infoSpy).toHaveBeenCalledTimes(1);
    expect(infoSpy.mock.calls[0][0]).toBe('[remitos:analytics]');
    expect(infoSpy.mock.calls[0][1]).toEqual({
      event: 'trial_started',
      source: 'signup_plan_selected',
    });
  });

  it('logs when NEXT_PUBLIC_ANALYTICS_DEBUG=true', async () => {
    vi.stubEnv('NEXT_PUBLIC_ANALYTICS_DEBUG', 'true');
    vi.resetModules();
    const { trackTrialOnboardingEvent } = await import('./trialOnboardingAnalytics');
    trackTrialOnboardingEvent('trial_success_screen_viewed', { is_mobile: false });
    expect(infoSpy).toHaveBeenCalledWith('[remitos:analytics]', {
      event: 'trial_success_screen_viewed',
      is_mobile: false,
    });
  });
});
