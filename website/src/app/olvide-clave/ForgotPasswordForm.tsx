'use client';

import { useState } from 'react';
import Link from 'next/link';
import { ArrowLeft, Loader2 } from 'lucide-react';
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

              <div className="mt-6 rounded-xl border border-green-100 bg-green-50 p-5 text-left sm:p-6">
                <p className="text-gray-800">
                  Si los datos coinciden con una cuenta, te enviamos un correo
                  con un enlace para restablecer la contraseña.
                </p>
                <p className="mt-4 text-sm text-gray-600">
                  Revisá la carpeta de spam si no lo ves en la bandeja de
                  entrada.
                </p>
              </div>

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
                    onChange={(e) => {
                      setCompanyCode(e.target.value.toUpperCase());
                      clearForgotFieldError('company_code');
                    }}
                    onBlur={() => handleForgotFieldBlur('company_code')}
                    autoComplete="organization"
                    aria-invalid={Boolean(fieldErrors.company_code)}
                    aria-describedby={
                      fieldErrors.company_code
                        ? 'forgot-company-error'
                        : undefined
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
                      id="forgot-company-error"
                      className="mt-1 text-sm text-red-600"
                      role="alert"
                    >
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
                    onChange={(e) => {
                      setUsername(e.target.value);
                      clearForgotFieldError('username');
                    }}
                    onBlur={() => handleForgotFieldBlur('username')}
                    autoComplete="username"
                    aria-invalid={Boolean(fieldErrors.username)}
                    aria-describedby={
                      fieldErrors.username ? 'forgot-user-error' : undefined
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
                      id="forgot-user-error"
                      className="mt-1 text-sm text-red-600"
                      role="alert"
                    >
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
