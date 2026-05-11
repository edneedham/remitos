'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { Loader2 } from 'lucide-react';
import { getApiBaseUrl } from '../lib/apiUrl';
import { safeRedirectPath } from '../lib/safeRedirectPath';
import {
  validateLoginForm,
  validateLoginFormField,
  type LoginFormErrors,
  type LoginFormField,
} from '../lib/validations/login';
import {
  canAccessWebManagement,
  clearWebSession,
  fetchProfile,
  fetchWithWebAuth,
  hasWebSession,
  saveWebSession,
  isWebCookieSession,
  webCookieFetchInit,
} from '../lib/webAuth';

const LOGIN_FIELD_DOM_ID: Record<LoginFormField, string> = {
  company_code: 'login-company',
  username: 'login-user',
  password: 'login-pass',
};

const LOGIN_FIELD_ORDER: LoginFormField[] = [
  'company_code',
  'username',
  'password',
];

function scrollAndFocusById(elementId: string) {
  const el = document.getElementById(elementId);
  if (!el) return;
  el.scrollIntoView({ behavior: 'smooth', block: 'center' });
  window.requestAnimationFrame(() => {
    if (el instanceof HTMLElement && typeof el.focus === 'function') {
      const tag = el.tagName.toLowerCase();
      if (
        tag === 'input' ||
        tag === 'select' ||
        tag === 'textarea' ||
        tag === 'button'
      ) {
        el.focus({ preventScroll: true });
      }
    }
  });
}

function scrollFirstLoginError(errs: LoginFormErrors) {
  for (const key of LOGIN_FIELD_ORDER) {
    if (errs[key]) {
      scrollAndFocusById(LOGIN_FIELD_DOM_ID[key]);
      break;
    }
  }
}

export default function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const nextPath = safeRedirectPath(searchParams.get('next')) ?? '/panel';
  const justReset = searchParams.get('reset') === '1';
  const passwordChanged = searchParams.get('password-changed') === '1';
  const [companyCode, setCompanyCode] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<{
    company_code?: string;
    username?: string;
    password?: string;
  }>({});
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!hasWebSession()) return;

    let cancelled = false;
    async function validateSession() {
      const api = getApiBaseUrl();
      if (!api) return;
      const res = await fetchWithWebAuth('/auth/me/entitlement');
      if (cancelled) return;
      if (res.status === 401) {
        clearWebSession();
        return;
      }
      if (res.status === 403) {
        clearWebSession();
        setError(
          'Tu rol no tiene acceso al panel web. Iniciá sesión desde la app móvil.',
        );
        return;
      }
      if (res.ok) {
        router.replace(nextPath);
      }
    }
    void validateSession();
    return () => {
      cancelled = true;
    };
  }, [router, nextPath]);

  const clearLoginFieldError = (field: LoginFormField) => {
    setFieldErrors((prev) => {
      if (!prev[field]) return prev;
      const next = { ...prev };
      delete next[field];
      return next;
    });
  };

  const handleLoginFieldBlur = (field: LoginFormField) => {
    const err = validateLoginFormField(field, {
      companyCode,
      username,
      password,
    });
    setFieldErrors((prev) => {
      const next = { ...prev };
      if (err) next[field] = err;
      else delete next[field];
      return next;
    });
  };

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    const clientErrors = validateLoginForm({
      companyCode,
      username,
      password,
    });
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors);
      scrollFirstLoginError(clientErrors);
      return;
    }
    setFieldErrors({});

    const api = getApiBaseUrl();
    if (!api) {
      setError(
        'Falta configurar NEXT_PUBLIC_API_URL (URL del servidor de la API).',
      );
      return;
    }

    setLoading(true);
    try {
      const res = await fetch(`${api}/auth/login`, {
        method: 'POST',
        ...webCookieFetchInit({ 'Content-Type': 'application/json' }),
        body: JSON.stringify({
          company_code: companyCode.trim().toUpperCase(),
          username: username.trim(),
          password,
          device_name: 'web',
        }),
      });
      const data = (await res.json().catch(() => ({}))) as {
        token?: string;
        refresh_token?: string;
        session?: string;
        message?: string;
        error?: string;
        fields?: Record<string, string>;
      };

      if (!res.ok) {
        const nextFields: {
          company_code?: string;
          username?: string;
          password?: string;
        } = {};
        if (data.fields && typeof data.fields === 'object') {
          const f = data.fields;
          if (typeof f.company_code === 'string')
            nextFields.company_code = f.company_code;
          if (typeof f.username === 'string')
            nextFields.username = f.username;
          if (typeof f.password === 'string')
            nextFields.password = f.password;
        }
        setFieldErrors(nextFields);
        setError(
          data.message ||
            (typeof data.error === 'string' ? data.error : '') ||
            'No se pudo iniciar sesión. Revisá los datos.',
        );
        return;
      }

      const cookieSession =
        isWebCookieSession() && data.session === 'cookie';
      if (
        !cookieSession &&
        (!data.token || !data.refresh_token)
      ) {
        setError('Respuesta inválida del servidor.');
        return;
      }

      if (!cookieSession) {
        saveWebSession(data.token!, data.refresh_token!);
      }

      const profile = await fetchProfile();
      if (!profile || !canAccessWebManagement(profile.role)) {
        clearWebSession();
        setError(
          'Tu rol no tiene acceso al panel web. Iniciá sesión desde la app móvil.',
        );
        return;
      }

      router.push(nextPath);
      router.refresh();
    } catch {
      setError('Error de red. Verificá la conexión y la URL de la API.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-[60vh] bg-gray-50 px-4 py-10 lg:py-14">
      <div className="mx-auto w-full max-w-md">
        <section className="flex flex-col rounded-2xl border border-gray-200 bg-white px-5 py-7 shadow-sm sm:px-6 sm:py-8 lg:py-10">
          <header className="space-y-2 text-left sm:space-y-3">
            <h1 className="text-[1.625rem] font-bold leading-snug tracking-tight text-gray-900 sm:text-3xl sm:leading-tight">
              Iniciar sesión en el sitio
            </h1>
            <p className="text-base leading-relaxed text-gray-600 sm:text-sm sm:leading-relaxed">
              Después de entrar vas a la{' '}
              <strong className="font-medium text-gray-800">
                administración de tu cuenta
              </strong>{' '}
              (web). Para trabajar en el depósito con la app, abrí sesión en el
              teléfono: ahí habilitás el uso del{' '}
              <strong className="font-medium text-gray-800">modo app</strong>.
            </p>
          </header>

          <form
            onSubmit={(e) => void handleSubmit(e)}
            className="mt-6 space-y-5 sm:mt-8"
            noValidate
          >
            {justReset && (
              <p
                className="rounded-lg border border-green-100 bg-green-50 px-3 py-2 text-sm text-green-800"
                role="status"
              >
                Contraseña actualizada. Iniciá sesión con tu nueva clave.
              </p>
            )}
            {passwordChanged && (
              <p
                className="rounded-lg border border-green-100 bg-green-50 px-3 py-2 text-sm text-green-800"
                role="status"
              >
                Cambiaste tu contraseña. Iniciá sesión de nuevo.
              </p>
            )}
            {error && (
              <p
                className="rounded-lg border border-red-100 bg-red-50 px-3 py-2 text-sm text-red-700"
                role="alert"
              >
                {error}
              </p>
            )}

            <div>
              <label
                htmlFor="login-company"
                className="mb-1 block text-sm font-medium text-gray-700"
              >
                Código de empresa
              </label>
              <input
                id="login-company"
                value={companyCode}
                onChange={(e) => {
                  setCompanyCode(e.target.value.toUpperCase());
                  clearLoginFieldError('company_code');
                }}
                onBlur={() => handleLoginFieldBlur('company_code')}
                autoComplete="organization"
                aria-invalid={Boolean(fieldErrors.company_code)}
                aria-describedby={
                  fieldErrors.company_code ? 'login-company-error' : undefined
                }
                aria-required
                className={`w-full rounded-lg border px-4 py-3 font-mono uppercase focus:ring-2 ${
                  fieldErrors.company_code
                    ? 'border-red-400 focus:border-red-500 focus:ring-red-500'
                    : 'border-gray-300 focus:border-blue-500 focus:ring-blue-500'
                }`}
              />
              {fieldErrors.company_code ? (
                <p
                  id="login-company-error"
                  className="mt-1 text-sm text-red-600"
                  role="alert"
                >
                  {fieldErrors.company_code}
                </p>
              ) : null}
            </div>

            <div>
              <label
                htmlFor="login-user"
                className="mb-1 block text-sm font-medium text-gray-700"
              >
                Correo o usuario
              </label>
              <input
                id="login-user"
                type="text"
                value={username}
                onChange={(e) => {
                  setUsername(e.target.value);
                  clearLoginFieldError('username');
                }}
                onBlur={() => handleLoginFieldBlur('username')}
                autoComplete="username"
                aria-invalid={Boolean(fieldErrors.username)}
                aria-describedby={
                  fieldErrors.username ? 'login-user-error' : undefined
                }
                aria-required
                className={`w-full rounded-lg border px-4 py-3 focus:ring-2 ${
                  fieldErrors.username
                    ? 'border-red-400 focus:border-red-500 focus:ring-red-500'
                    : 'border-gray-300 focus:border-blue-500 focus:ring-blue-500'
                }`}
              />
              {fieldErrors.username ? (
                <p
                  id="login-user-error"
                  className="mt-1 text-sm text-red-600"
                  role="alert"
                >
                  {fieldErrors.username}
                </p>
              ) : null}
            </div>

            <div>
              <label
                htmlFor="login-pass"
                className="mb-1 block text-sm font-medium text-gray-700"
              >
                Contraseña
              </label>
              <input
                id="login-pass"
                type="password"
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value);
                  clearLoginFieldError('password');
                }}
                onBlur={() => handleLoginFieldBlur('password')}
                autoComplete="current-password"
                aria-invalid={Boolean(fieldErrors.password)}
                aria-describedby={
                  fieldErrors.password ? 'login-pass-error' : undefined
                }
                aria-required
                className={`w-full rounded-lg border px-4 py-3 focus:ring-2 ${
                  fieldErrors.password
                    ? 'border-red-400 focus:border-red-500 focus:ring-red-500'
                    : 'border-gray-300 focus:border-blue-500 focus:ring-blue-500'
                }`}
              />
              {fieldErrors.password ? (
                <p
                  id="login-pass-error"
                  className="mt-1 text-sm text-red-600"
                  role="alert"
                >
                  {fieldErrors.password}
                </p>
              ) : null}
            </div>

            <button
              type="submit"
              disabled={loading}
              className="flex w-full items-center justify-center rounded-lg bg-blue-600 px-4 py-3 font-semibold text-white transition-colors hover:bg-blue-700 disabled:opacity-50"
            >
              {loading ? (
                <Loader2 className="h-5 w-5 animate-spin" />
              ) : (
                'Entrar a mi cuenta'
              )}
            </button>

            <p className="text-center text-sm">
              <Link
                href="/olvide-clave"
                className="font-medium text-blue-600 hover:text-blue-700 hover:underline"
              >
                ¿Olvidaste tu contraseña?
              </Link>
            </p>
          </form>

          <p className="mt-6 border-t border-gray-100 pt-6 text-center text-sm text-gray-600 sm:mt-8 sm:pt-8">
            ¿Todavía no tenés cuenta?{' '}
            <Link
              href="/registro"
              className="font-medium text-blue-600 hover:text-blue-700 hover:underline"
            >
              Registrate
            </Link>
          </p>
        </section>
      </div>
    </div>
  );
}
