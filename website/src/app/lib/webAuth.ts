import { getApiBaseUrl } from './apiUrl';

const ACCESS_KEY = 'enpunto_web_access_token';
const REFRESH_KEY = 'enpunto_web_refresh_token';

export function saveWebSession(accessToken: string, refreshToken: string): void {
  if (typeof window === 'undefined') return;
  sessionStorage.setItem(ACCESS_KEY, accessToken);
  sessionStorage.setItem(REFRESH_KEY, refreshToken);
}

export function clearWebSession(): void {
  if (typeof window === 'undefined') return;
  sessionStorage.removeItem(ACCESS_KEY);
  sessionStorage.removeItem(REFRESH_KEY);
}

export function getWebAccessToken(): string | null {
  if (typeof window === 'undefined') return null;
  return sessionStorage.getItem(ACCESS_KEY);
}

export function getWebRefreshToken(): string | null {
  if (typeof window === 'undefined') return null;
  return sessionStorage.getItem(REFRESH_KEY);
}

export function hasWebSession(): boolean {
  return Boolean(getWebAccessToken());
}

export async function logoutWebSession(): Promise<void> {
  const api = getApiBaseUrl();
  const token = getWebAccessToken();
  try {
    if (api && token) {
      await fetch(`${api}/auth/logout`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
      });
    }
  } finally {
    clearWebSession();
  }
}

/** Returns false if refresh fails (session invalid). Updates sessionStorage on success. */
export async function refreshWebSession(): Promise<boolean> {
  const api = getApiBaseUrl();
  const refreshToken = getWebRefreshToken();
  if (!api || !refreshToken) {
    return false;
  }
  const res = await fetch(`${api}/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refresh_token: refreshToken }),
  });
  if (!res.ok) {
    return false;
  }
  const data = (await res.json().catch(() => ({}))) as {
    token?: string;
    refresh_token?: string;
  };
  if (!data.token || !data.refresh_token) {
    return false;
  }
  saveWebSession(data.token, data.refresh_token);
  return true;
}

/** GET with Bearer token; retries once after token refresh when the API returns 401. */
export async function fetchWithWebAuth(path: string): Promise<Response> {
  const api = getApiBaseUrl();
  if (!api) {
    return new Response(null, { status: 500 });
  }

  let token = getWebAccessToken();
  if (!token) {
    return new Response(null, { status: 401 });
  }

  let res = await fetch(`${api}${path}`, {
    headers: { Authorization: `Bearer ${token}` },
  });

  if (res.status === 401) {
    const ok = await refreshWebSession();
    token = getWebAccessToken();
    if (ok && token) {
      res = await fetch(`${api}${path}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
    }
  }

  return res;
}
