'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { Loader2 } from 'lucide-react';
import { getApiBaseUrl } from '../lib/apiUrl';
import { safeRedirectPath } from '../lib/safeRedirectPath';
import {
  canAccessWebManagement,
  clearWebSession,
  fetchProfile,
  fetchWithWebAuth,
  hasWebSession,
  saveWebSession,
} from '../lib/webAuth';

export default function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const nextPath = safeRedirectPath(searchParams.get('next')) ?? '/dashboard';
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

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
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
        headers: { 'Content-Type': 'application/json' },
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

      if (!data.token || !data.refresh_token) {
        setError('Respuesta inválida del servidor.');
        return;
      }

      saveWebSession(data.token, data.refresh_token);

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
    <div className="mx-auto max-w-md space-y-8 px-4 py-12">
      <div className="space-y-2 text-center">
        <h1 className="text-3xl font-bold tracking-tight text-gray-900">
          Iniciar sesión en el sitio
        </h1>
        <p className="text-sm leading-relaxed text-gray-600">
          Después de entrar vas a la{' '}
          <strong className="font-medium text-gray-800">
            administración de tu cuenta
          </strong>{' '}
          (web). Para trabajar en el depósito con la app, abrí sesión en el
          teléfono: ahí habilitás el uso del{' '}
          <strong className="font-medium text-gray-800">modo app</strong>.
        </p>
      </div>

      <form
        onSubmit={(e) => void handleSubmit(e)}
        className="space-y-4 rounded-xl border border-gray-200 bg-white p-6 shadow-sm"
        noValidate
      >
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
            onChange={(e) => setCompanyCode(e.target.value.toUpperCase())}
            autoComplete="organization"
            aria-invalid={fieldErrors.company_code ? true : undefined}
            className={`w-full rounded-lg border px-4 py-3 font-mono uppercase focus:ring-2 ${
              fieldErrors.company_code
                ? 'border-red-400 focus:border-red-500 focus:ring-red-500'
                : 'border-gray-300 focus:border-blue-500 focus:ring-blue-500'
            }`}
            required
          />
          {fieldErrors.company_code ? (
            <p className="mt-1 text-sm text-red-600" role="alert">
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
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
            aria-invalid={fieldErrors.username ? true : undefined}
            className={`w-full rounded-lg border px-4 py-3 focus:ring-2 ${
              fieldErrors.username
                ? 'border-red-400 focus:border-red-500 focus:ring-red-500'
                : 'border-gray-300 focus:border-blue-500 focus:ring-blue-500'
            }`}
            required
          />
          {fieldErrors.username ? (
            <p className="mt-1 text-sm text-red-600" role="alert">
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
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            aria-invalid={fieldErrors.password ? true : undefined}
            className={`w-full rounded-lg border px-4 py-3 focus:ring-2 ${
              fieldErrors.password
                ? 'border-red-400 focus:border-red-500 focus:ring-red-500'
                : 'border-gray-300 focus:border-blue-500 focus:ring-blue-500'
            }`}
            required
          />
          {fieldErrors.password ? (
            <p className="mt-1 text-sm text-red-600" role="alert">
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
      </form>

      <p className="text-center text-sm text-gray-600">
        ¿Todavía no tenés cuenta?{' '}
        <Link
          href="/signup"
          className="font-medium text-blue-600 hover:text-blue-700 hover:underline"
        >
          Registrate
        </Link>
      </p>
    </div>
  );
}
