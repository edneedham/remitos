/**
 * Returns a safe in-app path for post-login redirects, or undefined if invalid.
 * Only same-origin relative paths are allowed (no protocol-relative or external URLs).
 */
export function safeRedirectPath(raw: string | null): string | undefined {
  if (raw == null || typeof raw !== 'string') return undefined;
  const t = raw.trim();
  if (t === '' || !t.startsWith('/') || t.startsWith('//')) return undefined;
  if (t.includes('..') || t.includes('\\')) return undefined;
  return t;
}
