'use client';

import { useState } from 'react';
import Link from 'next/link';
import { ArrowLeft } from 'lucide-react';
import Button from '../ui/components/shared/Button';
import StatusBanner from '../ui/components/shared/StatusBanner';
import TextField from '../ui/components/shared/TextField';
import { getApiBaseUrl } from '../lib/apiUrl';
import {
  validateForgotPasswordField,
  validateForgotPasswordForm,
  type ForgotPasswordErrors,
  type ForgotPasswordField,
} from '../lib/validations/login';

const FORGOT_FIELD_DOM_ID: Record<ForgotPasswordField, string> = {
  company_code: 'forgot-company',
  username: 'forgot-user',
};

const FORGOT_FIELD_ORDER: ForgotPasswordField[] = ['company_code', 'username'];

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

function scrollFirstForgotError(errs: ForgotPasswordErrors) {
  for (const key of FORGOT_FIELD_ORDER) {
    if (errs[key]) {
      scrollAndFocusById(FORGOT_FIELD_DOM_ID[key]);
      break;
    }
  }
}

function BackToLoginLink() {
  return (
    <Link
      href="/ingresar"
      className="inline-flex items-center justify-center gap-2 font-medium text-blue-600 hover:text-blue-700 hover:underline"
    >
      <ArrowLeft className="h-4 w-4 shrink-0" aria-hidden />
      Volver al inicio de sesión
    </Link>
  );
}

export default function ForgotPasswordForm() {
  const [companyCode, setCompanyCode] = useState('');
  const [username, setUsername] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<ForgotPasswordErrors>({});
  const [loading, setLoading] = useState(false);
  const [done, setDone] = useState(false);

  const clearForgotFieldError = (field: ForgotPasswordField) => {
    setFieldErrors((prev) => {
      if (!prev[field]) return prev;
      const next = { ...prev };
      delete next[field];
      return next;
    });
  };

  const handleForgotFieldBlur = (field: ForgotPasswordField) => {
    const err = validateForgotPasswordField(field, {
      companyCode,
      username,
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
    const clientErrors = validateForgotPasswordForm({
      companyCode,
      username,
    });
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors);
      scrollFirstForgotError(clientErrors);
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
        const nextFields: ForgotPasswordErrors = {};
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

  return (
    <div className="min-h-[60vh] bg-gray-50 px-4 py-10 lg:py-14">
      <div className="mx-auto w-full max-w-md">
        <section className="flex flex-col rounded-2xl border border-gray-200 bg-white px-5 py-7 shadow-sm sm:px-6 sm:py-8 lg:py-10">
          {done ? (
            <>
              <header className="space-y-2 text-left sm:space-y-3">
                <h1 className="text-[1.625rem] font-bold leading-snug tracking-tight text-gray-900 sm:text-3xl sm:leading-tight">
                  Revisá tu correo
                </h1>
              </header>

              <StatusBanner variant="success" className="mt-6">
                <p>
                  Si los datos coinciden con una cuenta, te enviamos un correo
                  con un enlace para restablecer la contraseña.
                </p>
                <p className="mt-2 text-xs opacity-80">
                  Revisá la carpeta de spam si no lo ves en la bandeja de
                  entrada.
                </p>
              </StatusBanner>

              <p className="mt-6 border-t border-gray-100 pt-6 text-center text-sm text-gray-600 sm:mt-8 sm:pt-8">
                <BackToLoginLink />
              </p>
            </>
          ) : (
            <>
              <header className="space-y-2 text-left sm:space-y-3">
                <h1 className="text-[1.625rem] font-bold leading-snug tracking-tight text-gray-900 sm:text-3xl sm:leading-tight">
                  ¿Olvidaste tu contraseña?
                </h1>
                <p className="text-base leading-relaxed text-gray-600 sm:text-sm sm:leading-relaxed">
                  Ingresá el mismo código de empresa y correo o usuario que usás
                  para entrar.
                </p>
              </header>

              <form
                onSubmit={(e) => void handleSubmit(e)}
                className="mt-6 space-y-5 sm:mt-8"
                noValidate
              >
                {error && <StatusBanner variant="error">{error}</StatusBanner>}

                <TextField
                  id="forgot-company"
                  label="Código de empresa"
                  value={companyCode}
                  onChange={(e) => {
                    setCompanyCode(e.target.value.toUpperCase());
                    clearForgotFieldError('company_code');
                  }}
                  onBlur={() => handleForgotFieldBlur('company_code')}
                  autoComplete="organization"
                  aria-required
                  error={fieldErrors.company_code}
                  inputClassName="font-mono uppercase"
                />

                <TextField
                  id="forgot-user"
                  label="Correo o usuario"
                  type="text"
                  value={username}
                  onChange={(e) => {
                    setUsername(e.target.value);
                    clearForgotFieldError('username');
                  }}
                  onBlur={() => handleForgotFieldBlur('username')}
                  autoComplete="username"
                  aria-required
                  error={fieldErrors.username}
                />

                <Button type="submit" isLoading={loading} variant="primary">
                  Enviar instrucciones
                </Button>
              </form>

              <p className="mt-6 border-t border-gray-100 pt-6 text-center text-sm text-gray-600 sm:mt-8 sm:pt-8">
                <BackToLoginLink />
              </p>
            </>
          )}
        </section>
      </div>
    </div>
  );
}
