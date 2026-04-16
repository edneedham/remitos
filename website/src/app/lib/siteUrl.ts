/**
 * Public absolute origin for QR codes and absolute links.
 * Set `NEXT_PUBLIC_SITE_URL` in production (e.g. `https://www.example.com`) so
 * QR payloads stay stable across preview vs production. If unset, falls back
 * to `window.location.origin` (client only).
 */
export function getPublicSiteOrigin(): string {
  const configured = process.env.NEXT_PUBLIC_SITE_URL?.trim();
  if (configured) {
    return configured.replace(/\/$/, '');
  }
  if (typeof window !== 'undefined') {
    return window.location.origin;
  }
  return '';
}
