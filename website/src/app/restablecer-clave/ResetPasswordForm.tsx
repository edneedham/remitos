'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { Loader2 } from 'lucide-react';
import { getApiBaseUrl } from '../lib/apiUrl';

export default function ResetPasswordForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const token = searchParams.get('token')?.trim() ?? '';

  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<{
    password?: string;
    token?: string;
  }>({});
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setFieldErrors({});

    if (password.length < 8) {
      setFieldErrors({ password: 'Mínimo 8 caracteres.' });
      return;
    }
    if (password.length > 72) {
      setFieldErrors({ password: 'Máximo 72 caracteres.' });
      return;
    }
    if (password !== passwordConfirm) {
      setError('Las contraseñas no coinciden.');
      return;
    }

    const api = getApiBaseUrl();
    if (!api) {
      setError(
        'Falta configurar NEXT_PUBLIC_API_URL (URL del servidor de la API).',
      );
      return;
    }

    if (!token) {
      setFieldErrors({
        token: 'Falta el token del enlace. Abrí el link desde el correo.',
      });
      return;
    }

    setLoading(true);
    try {
      const res = await fetch(`${api}/auth/reset-password`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          token,
          new_password: password,
        }),
      });
      const data = (await res.json().catch(() => ({}))) as {
        message?: string;
        error?: string;
        fields?: Record<string, string>;
      };

      if (!res.ok) {
        const nextFields: { password?: string; token?: string } = {};
        if (data.fields && typeof data.fields === 'object') {
          const f = data.fields;
          if (typeof f.new_password === 'string')
            nextFields.password = f.new_password;
          if (typeof f.token === 'string') nextFields.token = f.token;
        }
        setFieldErrors(nextFields);
        setError(
          data.message ||
            (typeof data.error === 'string' ? data.error : '') ||
            'No se pudo actualizar la contraseña.',
        );
        return;
      }

      router.push('/ingresar?reset=1');
      router.refresh();
    } catch {
      setError('Error de red. Verificá la conexión y la URL de la API.');
    } finally {
      setLoading(false);
    }
  }

  if (!token) {
    return (
      <div className="mx-auto max-w-md space-y-6 px-4 py-12">
        <p className="rounded-lg border border-amber-100 bg-amber-50 px-3 py-2 text-sm text-amber-900">
          Abrí esta página desde el enlace que te enviamos por correo, o pedí un
          nuevo restablecimiento.
        </p>
        <p className="text-center text-sm text-gray-600">
          <Link
            href="/olvide-clave"
            className="font-medium text-blue-600 hover:text-blue-700 hover:underline"
          >
            Pedir nuevo correo
          </Link>
        </p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-md space-y-8 px-4 py-12">
      <div className="space-y-2 text-center">
        <h1 className="text-3xl font-bold tracking-tight text-gray-900">
          Nueva contraseña
        </h1>
        <p className="text-sm text-gray-600">
          Elegí una contraseña segura para tu cuenta.
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
        {fieldErrors.token ? (
          <p className="text-sm text-red-600" role="alert">
            {fieldErrors.token}
          </p>
        ) : null}

        <div>
          <label
            htmlFor="reset-pass"
            className="mb-1 block text-sm font-medium text-gray-700"
          >
            Nueva contraseña
          </label>
          <input
            id="reset-pass"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="new-password"
            aria-invalid={fieldErrors.password ? true : undefined}
            className={`w-full rounded-lg border px-4 py-3 focus:ring-2 ${
              fieldErrors.password
                ? 'border-red-400 focus:border-red-500 focus:ring-red-500'
                : 'border-gray-300 focus:border-blue-500 focus:ring-blue-500'
            }`}
            required
            minLength={8}
            maxLength={72}
          />
          {fieldErrors.password ? (
            <p className="mt-1 text-sm text-red-600" role="alert">
              {fieldErrors.password}
            </p>
          ) : null}
        </div>

        <div>
          <label
            htmlFor="reset-pass2"
            className="mb-1 block text-sm font-medium text-gray-700"
          >
            Repetir contraseña
          </label>
          <input
            id="reset-pass2"
            type="password"
            value={passwordConfirm}
            onChange={(e) => setPasswordConfirm(e.target.value)}
            autoComplete="new-password"
            className="w-full rounded-lg border border-gray-300 px-4 py-3 focus:border-blue-500 focus:ring-2 focus:ring-blue-500"
            required
            minLength={8}
            maxLength={72}
          />
        </div>

        <button
          type="submit"
          disabled={loading}
          className="flex w-full items-center justify-center rounded-lg bg-blue-600 px-4 py-3 font-semibold text-white transition-colors hover:bg-blue-700 disabled:opacity-50"
        >
          {loading ? (
            <Loader2 className="h-5 w-5 animate-spin" />
          ) : (
            'Guardar contraseña'
          )}
        </button>
      </form>

      <p className="text-center text-sm text-gray-600">
        <Link
          href="/ingresar"
          className="font-medium text-blue-600 hover:text-blue-700 hover:underline"
        >
          Ir al inicio de sesión
        </Link>
      </p>
    </div>
  );
}
