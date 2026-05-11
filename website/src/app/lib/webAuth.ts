import { getApiBaseUrl } from './apiUrl';

const ACCESS_KEY = 'enpunto_web_access_token';
const REFRESH_KEY = 'enpunto_web_refresh_token';

/** Matches backend middleware.CookieWebHint — non-secret session indicator. */
const COOKIE_HINT_NAME = 'enpunto_web_hint';

export type WebProfile = {
  id: string;
  username: string;
  email?: string;
  company_id: string;
  company_name: string;
  company_code: string;
  role: string;
};

export const WEB_ALLOWED_ROLES = [
  'company_owner',
  'warehouse_admin',
  'read_only',
  'admin',
] as const;

/** When false (env NEXT_PUBLIC_WEB_COOKIE_SESSION=false), legacy sessionStorage tokens are used. */
export function isWebCookieSession(): boolean {
  return process.env.NEXT_PUBLIC_WEB_COOKIE_SESSION !== 'false';
}

/** Extra fetch options for browser sessions (httpOnly cookies + CSRF-oriented header). */
export function webCookieFetchInit(
  headers: Record<string, string> = {},
): Pick<RequestInit, 'credentials' | 'headers'> {
  if (!isWebCookieSession()) {
    return { headers };
  }
  return {
    credentials: 'include',
    headers: {
      ...headers,
      'X-Enpunto-Web': '1',
    },
  };
}

export function canAccessWebManagement(role: string): boolean {
  return (WEB_ALLOWED_ROLES as readonly string[]).includes(role);
}

export function canManageBillingSubscriptions(role: string): boolean {
  return (
    role === 'company_owner' ||
    role === 'warehouse_admin' ||
    role === 'admin'
  );
}

export function canManageOperators(role: string): boolean {
  return canManageBillingSubscriptions(role);
}

function hasSessionHintCookie(): boolean {
  if (typeof document === 'undefined') return false;
  return document.cookie.split(';').some((part) => {
    const name = part.trim().split('=')[0];
    return name === COOKIE_HINT_NAME;
  });
}

/** Legacy: store Bearer tokens (avoid when cookie-session mode is enabled). */
export function saveWebSession(accessToken: string, refreshToken: string): void {
  if (typeof window === 'undefined') return;
  if (isWebCookieSession()) return;
  sessionStorage.setItem(ACCESS_KEY, accessToken);
  sessionStorage.setItem(REFRESH_KEY, refreshToken);
}

export function clearWebSession(): void {
  if (typeof window === 'undefined') return;
  sessionStorage.removeItem(ACCESS_KEY);
  sessionStorage.removeItem(REFRESH_KEY);
  if (isWebCookieSession()) {
    const secure =
      typeof window !== 'undefined' && window.location.protocol === 'https:';
    document.cookie = `${COOKIE_HINT_NAME}=; Path=/; Max-Age=0; SameSite=Lax${
      secure ? '; Secure' : ''
    }`;
  }
}

export function getWebAccessToken(): string | null {
  if (typeof window === 'undefined') return null;
  if (isWebCookieSession()) return null;
  return sessionStorage.getItem(ACCESS_KEY);
}

export function getWebRefreshToken(): string | null {
  if (typeof window === 'undefined') return null;
  if (isWebCookieSession()) return null;
  return sessionStorage.getItem(REFRESH_KEY);
}

export function hasWebSession(): boolean {
  if (isWebCookieSession()) {
    return hasSessionHintCookie();
  }
  return Boolean(getWebAccessToken());
}

export async function logoutWebSession(): Promise<void> {
  const api = getApiBaseUrl();
  try {
    if (api) {
      const token = getWebAccessToken();
      const init: RequestInit = {
        method: 'POST',
        ...webCookieFetchInit(
          token ? { Authorization: `Bearer ${token}` } : {},
        ),
      };
      await fetch(`${api}/auth/logout`, init);
    }
  } finally {
    clearWebSession();
  }
}

export async function refreshWebSession(): Promise<boolean> {
  const api = getApiBaseUrl();
  if (!api) {
    return false;
  }
  if (isWebCookieSession()) {
    const res = await fetch(`${api}/auth/refresh`, {
      method: 'POST',
      ...webCookieFetchInit({ 'Content-Type': 'application/json' }),
      body: '{}',
    });
    return res.ok;
  }

  const refreshToken = getWebRefreshToken();
  if (!refreshToken) {
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

export async function fetchWithWebAuth(path: string): Promise<Response> {
  const api = getApiBaseUrl();
  if (!api) {
    return new Response(null, { status: 500 });
  }

  if (isWebCookieSession()) {
    let res = await fetch(`${api}${path}`, {
      ...webCookieFetchInit(),
    });
    if (res.status === 401) {
      const ok = await refreshWebSession();
      if (ok) {
        res = await fetch(`${api}${path}`, {
          ...webCookieFetchInit(),
        });
      }
    }
    return res;
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

export async function postWithWebAuth(
  path: string,
  body: unknown,
): Promise<Response> {
  return jsonRequestWithWebAuth('POST', path, body);
}

export async function patchWithWebAuth(
  path: string,
  body: unknown,
): Promise<Response> {
  return jsonRequestWithWebAuth('PATCH', path, body);
}

export async function putWithWebAuth(
  path: string,
  body: unknown,
): Promise<Response> {
  return jsonRequestWithWebAuth('PUT', path, body);
}

export async function deleteWithWebAuth(path: string): Promise<Response> {
  return jsonRequestWithWebAuth('DELETE', path, null);
}

async function jsonRequestWithWebAuth(
  method: 'POST' | 'PATCH' | 'PUT' | 'DELETE',
  path: string,
  body: unknown,
): Promise<Response> {
  const api = getApiBaseUrl();
  if (!api) {
    return new Response(null, { status: 500 });
  }

  if (isWebCookieSession()) {
    const send = () =>
      fetch(`${api}${path}`, {
        method,
        ...webCookieFetchInit(
          body !== null ? { 'Content-Type': 'application/json' } : {},
        ),
        ...(body !== null ? { body: JSON.stringify(body) } : {}),
      });

    let res = await send();
    if (res.status === 401) {
      const ok = await refreshWebSession();
      if (ok) {
        res = await send();
      }
    }
    return res;
  }

  let token = getWebAccessToken();
  if (!token) {
    return new Response(null, { status: 401 });
  }

  const send = (t: string) => {
    const init: RequestInit = {
      method,
      headers: {
        Authorization: `Bearer ${t}`,
        ...(body !== null ? { 'Content-Type': 'application/json' } : {}),
      },
      ...(body !== null ? { body: JSON.stringify(body) } : {}),
    };
    return fetch(`${api}${path}`, init);
  };

  let res = await send(token);
  if (res.status === 401) {
    const ok = await refreshWebSession();
    token = getWebAccessToken();
    if (ok && token) {
      res = await send(token);
    }
  }
  return res;
}

export async function fetchProfile(): Promise<WebProfile | null> {
  const res = await fetchWithWebAuth('/auth/me');
  if (!res.ok) {
    if (res.status === 401) {
      clearWebSession();
    }
    return null;
  }
  const body = (await res.json().catch(() => ({}))) as Partial<WebProfile>;
  if (
    typeof body.id !== 'string' ||
    typeof body.username !== 'string' ||
    typeof body.company_id !== 'string' ||
    typeof body.company_name !== 'string' ||
    typeof body.company_code !== 'string' ||
    typeof body.role !== 'string'
  ) {
    return null;
  }
  return {
    id: body.id,
    username: body.username,
    email: typeof body.email === 'string' ? body.email : undefined,
    company_id: body.company_id,
    company_name: body.company_name,
    company_code: body.company_code,
    role: body.role,
  };
}
