'use client';

import { useState } from 'react';
import { Loader2 } from 'lucide-react';
import { getApiBaseUrl } from '../../lib/apiUrl';
import {
  clearWebSession,
  postWithWebAuth,
} from '../../lib/webAuth';
import { useRouter } from 'next/navigation';

export default function ChangePasswordFormClient() {
  const router = useRouter();
  const [current, setCurrent] = useState('');
  const [next, setNext] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<{
    current_password?: string;
    new_password?: string;
  }>({});
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setFieldErrors({});

    if (next.length < 8 || next.length > 72) {
      setFieldErrors({ new_password: 'La nueva contraseña debe tener entre 8 y 72 caracteres.' });
      return;
    }
    if (next !== confirm) {
      setError('Las contraseñas nuevas no coinciden.');
      return;
    }

    const api = getApiBaseUrl();
    if (!api) {
      setError('Falta configurar NEXT_PUBLIC_API_URL.');
      return;
    }

    setLoading(true);
    try {
      const res = await postWithWebAuth('/auth/change-password', {
        current_password: current,
        new_password: next,
      });
      const data = (await res.json().catch(() => ({}))) as {
        message?: string;
        error?: string;
        fields?: Record<string, string>;
      };

      if (!res.ok) {
        if (res.status === 401) {
          clearWebSession();
          router.replace('/login');
          return;
        }
        const nextFields: {
          current_password?: string;
          new_password?: string;
        } = {};
        if (data.fields && typeof data.fields === 'object') {
          const f = data.fields;
          if (typeof f.current_password === 'string')
            nextFields.current_password = f.current_password;
          if (typeof f.new_password === 'string')
            nextFields.new_password = f.new_password;
        }
        setFieldErrors(nextFields);
        setError(
          data.message ||
            (typeof data.error === 'string' ? data.error : '') ||
            'No se pudo actualizar la contraseña.',
        );
        return;
      }

      clearWebSession();
      router.replace('/login?password-changed=1');
      router.refresh();
    } catch {
      setError('Error de red. Intentá de nuevo.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="mx-auto max-w-lg space-y-6 px-4 py-8">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Cambiar contraseña</h1>
        <p className="mt-2 text-sm text-gray-600">
          Al guardar, cerramos tu sesión en el navegador y en todos los
          dispositivos. Volvé a iniciar sesión con la nueva clave.
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
            htmlFor="cp-current"
            className="mb-1 block text-sm font-medium text-gray-700"
          >
            Contraseña actual
          </label>
          <input
            id="cp-current"
            type="password"
            value={current}
            onChange={(e) => setCurrent(e.target.value)}
            autoComplete="current-password"
            aria-invalid={fieldErrors.current_password ? true : undefined}
            className={`w-full rounded-lg border px-4 py-3 focus:ring-2 ${
              fieldErrors.current_password
                ? 'border-red-400 focus:border-red-500 focus:ring-red-500'
                : 'border-gray-300 focus:border-blue-500 focus:ring-blue-500'
            }`}
            required
          />
          {fieldErrors.current_password ? (
            <p className="mt-1 text-sm text-red-600" role="alert">
              {fieldErrors.current_password}
            </p>
          ) : null}
        </div>

        <div>
          <label
            htmlFor="cp-next"
            className="mb-1 block text-sm font-medium text-gray-700"
          >
            Nueva contraseña
          </label>
          <input
            id="cp-next"
            type="password"
            value={next}
            onChange={(e) => setNext(e.target.value)}
            autoComplete="new-password"
            aria-invalid={fieldErrors.new_password ? true : undefined}
            className={`w-full rounded-lg border px-4 py-3 focus:ring-2 ${
              fieldErrors.new_password
                ? 'border-red-400 focus:border-red-500 focus:ring-red-500'
                : 'border-gray-300 focus:border-blue-500 focus:ring-blue-500'
            }`}
            required
            minLength={8}
            maxLength={72}
          />
          {fieldErrors.new_password ? (
            <p className="mt-1 text-sm text-red-600" role="alert">
              {fieldErrors.new_password}
            </p>
          ) : null}
        </div>

        <div>
          <label
            htmlFor="cp-confirm"
            className="mb-1 block text-sm font-medium text-gray-700"
          >
            Confirmar nueva contraseña
          </label>
          <input
            id="cp-confirm"
            type="password"
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
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
            'Actualizar contraseña'
          )}
        </button>
      </form>
    </div>
  );
}
