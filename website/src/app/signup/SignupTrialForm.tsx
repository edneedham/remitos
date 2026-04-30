'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useRouter } from 'next/navigation';
import { ArrowRight, Loader2 } from 'lucide-react';
import { getApiBaseUrl } from '../lib/apiUrl';
import {
  SIGNUP_TRIAL_API_FIELD_MAP,
  validateSignupTrialAccount,
  validateSignupTrialAccountField,
  type SignupTrialAccountErrors,
  type SignupTrialAccountField,
} from '../lib/validations/signupTrial';
import { saveWebSession } from '../lib/webAuth';

export type SignupTrialFormVariant = 'card' | 'embedded';

const ACCOUNT_FIELD_IDS: SignupTrialAccountField[] = [
  'companyName',
  'companyCode',
  'email',
  'password',
  'passwordConfirm',
];

const ACCOUNT_FIELD_DOM_ID: Record<SignupTrialAccountField, string> = {
  companyName: 'su-company',
  companyCode: 'su-code',
  email: 'su-email',
  password: 'su-password',
  passwordConfirm: 'su-password-confirm',
};

const VALIDATION_DEBOUNCE_MS = 350;

function scrollAndFocusById(elementId: string) {
  const el = document.getElementById(elementId);
  if (!el) return;
  el.scrollIntoView({ behavior: 'smooth', block: 'center' });
  window.requestAnimationFrame(() => {
    if (el instanceof HTMLElement && typeof el.focus === 'function') {
      const tag = el.tagName.toLowerCase();
      if (tag === 'input' || tag === 'select' || tag === 'textarea' || tag === 'button') {
        el.focus({ preventScroll: true });
      }
    }
  });
}

export default function SignupTrialForm({
  variant = 'card',
  onSignupSuccess,
}: {
  variant?: SignupTrialFormVariant;
  onSignupSuccess?: () => void;
}) {
  const router = useRouter();

  const [companyName, setCompanyName] = useState('');
  const [companyCode, setCompanyCode] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [accountFieldErrors, setAccountFieldErrors] =
    useState<SignupTrialAccountErrors>({});

  const accountValuesRef = useRef({
    companyName,
    companyCode,
    email,
    password,
    passwordConfirm,
  });
  accountValuesRef.current = {
    companyName,
    companyCode,
    email,
    password,
    passwordConfirm,
  };

  const accountDebounceTimersRef = useRef<
    Partial<Record<SignupTrialAccountField, ReturnType<typeof setTimeout>>>
  >({});

  useEffect(() => {
    const accountTimers = accountDebounceTimersRef.current;
    return () => {
      Object.values(accountTimers).forEach((t) => {
        if (t) clearTimeout(t);
      });
    };
  }, []);

  const canSubmit = useMemo(
    () => !loading,
    [loading],
  );

  const scrollFirstAccountErrorIntoView = useCallback(
    (errs: SignupTrialAccountErrors) => {
      for (const key of ACCOUNT_FIELD_IDS) {
        if (errs[key]) {
          scrollAndFocusById(ACCOUNT_FIELD_DOM_ID[key]);
          break;
        }
      }
    },
    [],
  );

  const cancelAccountFieldDebounce = useCallback(
    (field: SignupTrialAccountField) => {
      const t = accountDebounceTimersRef.current[field];
      if (t) {
        clearTimeout(t);
        delete accountDebounceTimersRef.current[field];
      }
    },
    [],
  );

  const cancelAllAccountDebounces = useCallback(() => {
    const timers = accountDebounceTimersRef.current;
    for (const field of ACCOUNT_FIELD_IDS) {
      const t = timers[field];
      if (t) {
        clearTimeout(t);
        delete timers[field];
      }
    }
  }, []);

  const applyDebouncedAccountFieldValidation = useCallback(
    (field: SignupTrialAccountField) => {
      const values = accountValuesRef.current;
      const fields: SignupTrialAccountField[] =
        field === 'password' || field === 'passwordConfirm'
          ? ['password', 'passwordConfirm']
          : [field];
      setAccountFieldErrors((prev) => {
        const next = { ...prev };
        for (const f of fields) {
          const err = validateSignupTrialAccountField(f, values);
          if (err) next[f] = err;
          else delete next[f];
        }
        return next;
      });
    },
    [],
  );

  const scheduleDebouncedAccountValidation = useCallback(
    (field: SignupTrialAccountField) => {
      const prev = accountDebounceTimersRef.current[field];
      if (prev) clearTimeout(prev);
      accountDebounceTimersRef.current[field] = setTimeout(() => {
        delete accountDebounceTimersRef.current[field];
        applyDebouncedAccountFieldValidation(field);
      }, VALIDATION_DEBOUNCE_MS);
    },
    [applyDebouncedAccountFieldValidation],
  );

  const runAccountValidation = useCallback(() => {
    const errs = validateSignupTrialAccount({
      companyName,
      companyCode,
      email,
      password,
      passwordConfirm,
    });
    setAccountFieldErrors(errs);
    return errs;
  }, [
    companyCode,
    companyName,
    email,
    password,
    passwordConfirm,
  ]);

  const clearAccountFieldError = useCallback((field: SignupTrialAccountField) => {
    setAccountFieldErrors((prev) => {
      if (!prev[field]) return prev;
      const next = { ...prev };
      delete next[field];
      return next;
    });
  }, []);

  const handleAccountBlur = useCallback(
    (field: SignupTrialAccountField) => {
      cancelAccountFieldDebounce(field);
      if (field === 'password') {
        cancelAccountFieldDebounce('passwordConfirm');
      }
      if (field === 'passwordConfirm') {
        cancelAccountFieldDebounce('password');
      }

      const fieldsToValidate: SignupTrialAccountField[] =
        field === 'password' || field === 'passwordConfirm'
          ? ['password', 'passwordConfirm']
          : [field];

      setAccountFieldErrors((prev) => {
        const next = { ...prev };
        const values = accountValuesRef.current;
        for (const f of fieldsToValidate) {
          const err = validateSignupTrialAccountField(f, values);
          if (err) next[f] = err;
          else delete next[f];
        }
        return next;
      });
    },
    [cancelAccountFieldDebounce],
  );

  const submit = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault();
      setError(null);
      cancelAllAccountDebounces();

      const accountErrs = runAccountValidation();
      if (Object.keys(accountErrs).length > 0) {
        window.requestAnimationFrame(() => {
          scrollFirstAccountErrorIntoView(accountErrs);
        });
        return;
      }

      const api = getApiBaseUrl();
      if (!api) {
        setError(
          'Falta configurar NEXT_PUBLIC_API_URL (URL del servidor de la API).',
        );
        return;
      }

      setLoading(true);
      try {
        const res = await fetch(`${api}/auth/signup`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            email: email.trim(),
            password,
            company_name: companyName.trim(),
            company_code: companyCode.trim().toUpperCase(),
          }),
        });
        const data = (await res.json().catch(() => ({}))) as {
          message?: string;
          error?: string;
          fields?: Record<string, string>;
          token?: string;
          refresh_token?: string;
        };
        if (!res.ok) {
          if (data.fields && typeof data.fields === 'object') {
            const fromApi: SignupTrialAccountErrors = {};
            for (const [k, v] of Object.entries(data.fields)) {
              const formKey = SIGNUP_TRIAL_API_FIELD_MAP[k];
              if (formKey && typeof v === 'string' && v.length > 0) {
                fromApi[formKey] = v;
              }
            }
            if (Object.keys(fromApi).length > 0) {
              setAccountFieldErrors((prev) => ({ ...prev, ...fromApi }));
              window.requestAnimationFrame(() => {
                scrollFirstAccountErrorIntoView(fromApi);
              });
            }
          }
          setError(
            data.message ||
              (typeof data.error === 'string' ? data.error : '') ||
              'No se pudo completar el registro. Probá de nuevo.',
          );
          return;
        }
        if (!data.token || !data.refresh_token) {
          setError('Respuesta inválida del servidor.');
          return;
        }
        saveWebSession(data.token, data.refresh_token);
        if (onSignupSuccess) {
          onSignupSuccess();
          return;
        }
        router.push('/trial-started');
        router.refresh();
      } catch {
        setError('Error de red. Verificá la conexión y la URL de la API.');
      } finally {
        setLoading(false);
      }
    },
    [
      companyCode,
      companyName,
      email,
      cancelAllAccountDebounces,
      password,
      passwordConfirm,
      onSignupSuccess,
      router,
      scrollFirstAccountErrorIntoView,
      runAccountValidation,
    ],
  );

  const handleFormSubmit = useCallback(
    (e: React.FormEvent) => {
      e.preventDefault();
      void submit(e);
    },
    [submit],
  );

  const formShell =
    variant === 'card'
      ? 'rounded-xl bg-white border border-gray-200 p-6 shadow-sm text-left lg:p-8'
      : 'w-full min-w-0 max-w-full -mt-1 text-left sm:-mt-2';

  const mainStackClass = variant === 'embedded' ? 'space-y-4' : 'space-y-6';

  const accountInputClass = (field: SignupTrialAccountField) =>
    `w-full px-4 py-3 border rounded-lg ${
      accountFieldErrors[field]
        ? 'border-red-500 focus:ring-2 focus:ring-red-500 focus:border-red-500'
        : 'border-gray-300 focus:ring-2 focus:ring-blue-500 focus:border-blue-500'
    }`;

  return (
    <form
      onSubmit={handleFormSubmit}
      className={formShell}
      noValidate
    >
      <div className={mainStackClass}>
        <div className="min-w-0 space-y-1.5 sm:max-w-md">
          <h2 className="text-lg font-semibold text-gray-900">
            Empresa y cuenta
          </h2>
          <p className="text-sm leading-snug text-gray-500 break-words sm:leading-relaxed">
            Probá gratis 7 días con hasta 2 depósitos, 1 dispositivo por depósito y hasta 500 documentos.
          </p>
        </div>

        <div className="space-y-5">
          <div>
            <label
              htmlFor="su-company"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Nombre de la empresa
            </label>
            <input
              id="su-company"
              value={companyName}
              onChange={(e) => {
                setCompanyName(e.target.value);
                clearAccountFieldError('companyName');
                scheduleDebouncedAccountValidation('companyName');
              }}
              onBlur={() => handleAccountBlur('companyName')}
              autoComplete="organization"
              maxLength={200}
              aria-invalid={Boolean(accountFieldErrors.companyName)}
              aria-required
              aria-describedby={
                accountFieldErrors.companyName ? 'su-company-error' : undefined
              }
              className={accountInputClass('companyName')}
            />
            {accountFieldErrors.companyName && (
              <p
                id="su-company-error"
                className="mt-1.5 text-sm text-red-600"
                role="alert"
              >
                {accountFieldErrors.companyName}
              </p>
            )}
          </div>
          <div>
            <label
              htmlFor="su-code"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Código de empresa
            </label>
            <input
              id="su-code"
              value={companyCode}
              onChange={(e) => {
                setCompanyCode(e.target.value.toUpperCase());
                clearAccountFieldError('companyCode');
                scheduleDebouncedAccountValidation('companyCode');
              }}
              onBlur={() => handleAccountBlur('companyCode')}
              maxLength={32}
              autoCapitalize="characters"
              spellCheck={false}
              aria-invalid={Boolean(accountFieldErrors.companyCode)}
              aria-required
              aria-describedby={
                accountFieldErrors.companyCode ? 'su-code-error' : undefined
              }
              className={`${accountInputClass('companyCode')} font-mono uppercase`}
            />
            {accountFieldErrors.companyCode && (
              <p
                id="su-code-error"
                className="mt-1.5 text-sm text-red-600"
                role="alert"
              >
                {accountFieldErrors.companyCode}
              </p>
            )}
            <p className="mt-1 text-xs text-gray-500">
              Letras, números, guiones o guiones bajos.
            </p>
          </div>

          <div>
            <label
              htmlFor="su-email"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Correo electrónico
            </label>
            <input
              id="su-email"
              type="email"
              inputMode="email"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                clearAccountFieldError('email');
                scheduleDebouncedAccountValidation('email');
              }}
              onBlur={() => handleAccountBlur('email')}
              autoComplete="email"
              aria-invalid={Boolean(accountFieldErrors.email)}
              aria-required
              aria-describedby={
                accountFieldErrors.email ? 'su-email-error' : undefined
              }
              className={accountInputClass('email')}
            />
            {accountFieldErrors.email && (
              <p
                id="su-email-error"
                className="mt-1.5 text-sm text-red-600"
                role="alert"
              >
                {accountFieldErrors.email}
              </p>
            )}
          </div>
          <div>
            <label
              htmlFor="su-password"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Contraseña
            </label>
            <input
              id="su-password"
              type="password"
              value={password}
              onChange={(e) => {
                setPassword(e.target.value);
                clearAccountFieldError('password');
                clearAccountFieldError('passwordConfirm');
                scheduleDebouncedAccountValidation('password');
                scheduleDebouncedAccountValidation('passwordConfirm');
              }}
              onBlur={() => handleAccountBlur('password')}
              autoComplete="new-password"
              maxLength={72}
              aria-invalid={Boolean(accountFieldErrors.password)}
              aria-required
              aria-describedby={
                accountFieldErrors.password
                  ? 'su-password-error'
                  : 'su-password-hint'
              }
              className={accountInputClass('password')}
            />
            {accountFieldErrors.password ? (
              <p
                id="su-password-error"
                className="mt-1.5 text-sm text-red-600"
                role="alert"
              >
                {accountFieldErrors.password}
              </p>
            ) : (
              <p id="su-password-hint" className="mt-1 text-xs text-gray-500">
                Entre 8 y 72 caracteres.
              </p>
            )}
          </div>
          <div>
            <label
              htmlFor="su-password-confirm"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Confirmar contraseña
            </label>
            <input
              id="su-password-confirm"
              type="password"
              value={passwordConfirm}
              onChange={(e) => {
                setPasswordConfirm(e.target.value);
                clearAccountFieldError('passwordConfirm');
                scheduleDebouncedAccountValidation('passwordConfirm');
              }}
              onBlur={() => handleAccountBlur('passwordConfirm')}
              autoComplete="new-password"
              maxLength={72}
              aria-invalid={Boolean(accountFieldErrors.passwordConfirm)}
              aria-required
              aria-describedby={
                accountFieldErrors.passwordConfirm
                  ? 'su-password-confirm-error'
                  : undefined
              }
              className={accountInputClass('passwordConfirm')}
            />
            {accountFieldErrors.passwordConfirm && (
              <p
                id="su-password-confirm-error"
                className="mt-1.5 text-sm text-red-600"
                role="alert"
              >
                {accountFieldErrors.passwordConfirm}
              </p>
            )}
          </div>
        </div>

      </div>

      <div className="mt-8 space-y-5 border-t border-gray-100 pt-8">
        {error && (
          <p
            id="signup-form-alert"
            tabIndex={-1}
            className="text-sm text-red-700 bg-red-50 border border-red-100 rounded-lg px-3 py-2 outline-none"
          >
            {error}
          </p>
        )}

        <button
          type="submit"
          disabled={!canSubmit}
          className="w-full inline-flex items-center justify-center px-4 py-3 bg-blue-600 text-white font-semibold rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:pointer-events-none"
        >
          {loading ? (
            <Loader2 className="h-5 w-5 animate-spin" />
          ) : (
            <>
              Crear cuenta
              <ArrowRight className="ml-2 h-4 w-4" />
            </>
          )}
        </button>
      </div>
    </form>
  );
}
