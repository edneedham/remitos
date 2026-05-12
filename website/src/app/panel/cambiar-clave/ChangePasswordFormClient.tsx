'use client';

import { useState } from 'react';
import Button from '../../ui/components/shared/Button';
import StatusBanner from '../../ui/components/shared/StatusBanner';
import TextField from '../../ui/components/shared/TextField';
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
          router.replace('/ingresar');
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
      router.replace('/ingresar?password-changed=1');
      router.refresh();
    } catch {
      setError('Error de red. Intentá de nuevo.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="bg-gray-50 px-4 pb-12 pt-6">
      <div className="mx-auto max-w-[92rem]">
        <div className="max-w-lg space-y-6">
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
        {error && <StatusBanner variant="error">{error}</StatusBanner>}

        <TextField
          id="cp-current"
          label="Contraseña actual"
          type="password"
          value={current}
          onChange={(e) => setCurrent(e.target.value)}
          autoComplete="current-password"
          required
          error={fieldErrors.current_password}
        />

        <TextField
          id="cp-next"
          label="Nueva contraseña"
          type="password"
          value={next}
          onChange={(e) => setNext(e.target.value)}
          autoComplete="new-password"
          required
          minLength={8}
          maxLength={72}
          error={fieldErrors.new_password}
        />

        <TextField
          id="cp-confirm"
          label="Confirmar nueva contraseña"
          type="password"
          value={confirm}
          onChange={(e) => setConfirm(e.target.value)}
          autoComplete="new-password"
          required
          minLength={8}
          maxLength={72}
        />

        <Button type="submit" isLoading={loading} variant="primary">
          Actualizar contraseña
        </Button>
          </form>
        </div>
      </div>
    </div>
  );
}
