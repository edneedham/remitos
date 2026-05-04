'use client';

import { useState } from 'react';
import Link from 'next/link';
import { Loader2 } from 'lucide-react';
import { getApiBaseUrl } from '../lib/apiUrl';

export default function ForgotPasswordForm() {
  const [companyCode, setCompanyCode] = useState('');
  const [username, setUsername] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<{
    company_code?: string;
    username?: string;
  }>({});
  const [loading, setLoading] = useState(false);
  const [done, setDone] = useState(false);

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
      const res = await fetch(`${api}/auth/forgot-password`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          company_code: companyCode.trim().toUpperCase(),
          username: username.trim(),
        }),
      });
      const data = (await res.json().catch(() => ({}))) as {
        message?: string;
        error?: string;
        fields?: Record<string, string>;
      };

      if (!res.ok) {
        const nextFields: { company_code?: string; username?: string } = {};
        if (data.fields && typeof data.fields === 'object') {
          const f = data.fields;
          if (typeof f.company_code === 'string')
            nextFields.company_code = f.company_code;
          if (typeof f.username === 'string')
            nextFields.username = f.username;
        }
        setFieldErrors(nextFields);
        setError(
          data.message ||
            (typeof data.error === 'string' ? data.error : '') ||
            'No se pudo procesar la solicitud.',
        );
        return;
      }

      setDone(true);
    } catch {
      setError('Error de red. Verificá la conexión y la URL de la API.');
    } finally {
      setLoading(false);
    }
  }

  if (done) {
    return (
      <div className="mx-auto max-w-md space-y-6 px-4 py-12">
        <div className="rounded-xl border border-green-100 bg-green-50 p-6 text-center">
          <p className="text-gray-800">
            Si los datos coinciden con una cuenta, te enviamos un correo con un
            enlace para restablecer la contraseña.
          </p>
          <p className="mt-4 text-sm text-gray-600">
            Revisá la carpeta de spam si no lo ves en la bandeja de entrada.
          </p>
        </div>
        <p className="text-center text-sm text-gray-600">
          <Link
            href="/login"
            className="font-medium text-blue-600 hover:text-blue-700 hover:underline"
          >
            Volver al inicio de sesión
          </Link>
        </p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-md space-y-8 px-4 py-12">
      <div className="space-y-2 text-center">
        <h1 className="text-3xl font-bold tracking-tight text-gray-900">
          ¿Olvidaste tu contraseña?
        </h1>
        <p className="text-sm leading-relaxed text-gray-600">
          Ingresá el mismo código de empresa y correo o usuario que usás para
          entrar.
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
            htmlFor="forgot-company"
            className="mb-1 block text-sm font-medium text-gray-700"
          >
            Código de empresa
          </label>
          <input
            id="forgot-company"
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
            htmlFor="forgot-user"
            className="mb-1 block text-sm font-medium text-gray-700"
          >
            Correo o usuario
          </label>
          <input
            id="forgot-user"
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

        <button
          type="submit"
          disabled={loading}
          className="flex w-full items-center justify-center rounded-lg bg-blue-600 px-4 py-3 font-semibold text-white transition-colors hover:bg-blue-700 disabled:opacity-50"
        >
          {loading ? (
            <Loader2 className="h-5 w-5 animate-spin" />
          ) : (
            'Enviar instrucciones'
          )}
        </button>
      </form>

      <p className="text-center text-sm text-gray-600">
        <Link
          href="/login"
          className="font-medium text-blue-600 hover:text-blue-700 hover:underline"
        >
          Volver al inicio de sesión
        </Link>
      </p>
    </div>
  );
}
