import { afterEach, describe, expect, it } from 'vitest';

describe('waitlistOnly', () => {
  afterEach(() => {
    delete process.env.NEXT_PUBLIC_WAITLIST_ONLY;
  });

  it('marketingSignupPath defaults to registro', async () => {
    const { marketingSignupPath } = await import('./waitlistOnly');
    expect(marketingSignupPath()).toBe('/registro');
  });

  it('marketingSignupPath uses lista-de-espera when flag is true', async () => {
    process.env.NEXT_PUBLIC_WAITLIST_ONLY = 'true';
    const { marketingSignupPath } = await import('./waitlistOnly');
    expect(marketingSignupPath()).toBe('/lista-de-espera');
  });

  it('trialSignupHref includes plan query', async () => {
    const { trialSignupHref } = await import('./waitlistOnly');
    expect(trialSignupHref('pyme')).toBe('/registro?plan=pyme');
    process.env.NEXT_PUBLIC_WAITLIST_ONLY = 'true';
    const mod = await import('./waitlistOnly');
    expect(mod.trialSignupHref('empresa')).toBe(
      '/lista-de-espera?plan=empresa',
    );
  });

  it('isMarketingSignupPath reflects waitlist route', async () => {
    delete process.env.NEXT_PUBLIC_WAITLIST_ONLY;
    const mod = await import('./waitlistOnly');
    expect(mod.isMarketingSignupPath('/registro')).toBe(true);
    expect(mod.isMarketingSignupPath('/lista-de-espera')).toBe(false);

    process.env.NEXT_PUBLIC_WAITLIST_ONLY = 'true';
    const mod2 = await import('./waitlistOnly');
    expect(mod2.isMarketingSignupPath('/lista-de-espera')).toBe(true);
    expect(mod2.isMarketingSignupPath('/registro')).toBe(false);
  });

  it('waitlistGuestHeaderCtaLabel is stable copy', async () => {
    const { waitlistGuestHeaderCtaLabel } = await import('./waitlistOnly');
    expect(waitlistGuestHeaderCtaLabel()).toBe(
      'Unirme a la lista de espera',
    );
  });
});
