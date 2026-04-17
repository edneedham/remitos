/** Base URL for the Remitos API (no trailing slash). Example: http://localhost:8080 */
export function getApiBaseUrl(): string {
  const raw = process.env.NEXT_PUBLIC_API_URL;
  if (!raw) {
    return '';
  }
  return raw.replace(/\/$/, '');
}
