'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import Button from '../ui/components/shared/Button';
import StatusBanner from '../ui/components/shared/StatusBanner';
import TextField from '../ui/components/shared/TextField';
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
        <StatusBanner variant="warning">
          Abrí esta página desde el enlace que te enviamos por correo, o pedí un
          nuevo restablecimiento.
        </StatusBanner>
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
        {error && <StatusBanner variant="error">{error}</StatusBanner>}
        {fieldErrors.token ? (
          <p className="text-sm text-red-600" role="alert">
            {fieldErrors.token}
          </p>
        ) : null}

        <TextField
          id="reset-pass"
          label="Nueva contraseña"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete="new-password"
          required
          minLength={8}
          maxLength={72}
          error={fieldErrors.password}
        />

        <TextField
          id="reset-pass2"
          label="Repetir contraseña"
          type="password"
          value={passwordConfirm}
          onChange={(e) => setPasswordConfirm(e.target.value)}
          autoComplete="new-password"
          required
          minLength={8}
          maxLength={72}
        />

        <Button type="submit" isLoading={loading} variant="primary">
          Guardar contraseña
        </Button>
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
