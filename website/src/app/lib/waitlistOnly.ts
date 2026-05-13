/**
 * When true, the marketing site uses the waitlist instead of self-serve signup.
 * Must match WAITLIST_ONLY on the API so direct /auth/signup calls are rejected.
 */
export function isWaitlistOnly(): boolean {
  return process.env.NEXT_PUBLIC_WAITLIST_ONLY === 'true';
}

/** Primary marketing URL for “get access” (signup or waitlist). */
export function marketingSignupPath(): string {
  return isWaitlistOnly() ? '/lista-de-espera' : '/registro';
}

/** Trial CTA from pricing with optional plan preselection. */
export function trialSignupHref(planId: string): string {
  const q = `plan=${encodeURIComponent(planId)}`;
  return isWaitlistOnly()
    ? `/lista-de-espera?${q}`
    : `/registro?${q}`;
}

/** True when pathname is the active marketing signup route (registro or lista-de-espera). */
export function isMarketingSignupPath(pathname: string): boolean {
  if (isWaitlistOnly()) {
    return (
      pathname === '/lista-de-espera' ||
      pathname.startsWith('/lista-de-espera/')
    );
  }
  return pathname === '/registro' || pathname.startsWith('/registro/');
}

/** CTA label for the primary “get access” control in the header (signup mode). */
export function marketingSignupLabel(): string {
  return isWaitlistOnly() ? 'Lista de espera' : 'Registro';
}

/**
 * Single guest header CTA when the waitlist is on: replaces separate login + signup.
 * (Deploy with WAITLIST_ONLY on the API and NEXT_PUBLIC_WAITLIST_ONLY on the site.)
 */
export function waitlistGuestHeaderCtaLabel(): string {
  return 'Unirme a la lista de espera';
}
