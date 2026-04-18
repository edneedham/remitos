'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  CardNumber,
  createCardToken,
  ExpirationDate,
  getIdentificationTypes,
  initMercadoPago,
  SecurityCode,
} from '@mercadopago/sdk-react';
import { ArrowRight, ChevronDown, ChevronUp, Loader2 } from 'lucide-react';
import { getApiBaseUrl } from '../lib/apiUrl';
import {
  validateCardholderName,
  validateIdNumber,
  validateSignupTrialAccount,
  validateSignupTrialAccountField,
  type SignupTrialAccountErrors,
  type SignupTrialAccountField,
} from '../lib/validations/signupTrial';
import MercadoPagoLogo from './MercadoPagoLogo';

type IdType = { id: string; name: string };

const useMockPayment =
  process.env.NEXT_PUBLIC_SIGNUP_USE_MOCK_PAYMENT === 'true';

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

function scrollErrorAlertIntoView() {
  document.getElementById('signup-form-alert')?.scrollIntoView({
    behavior: 'smooth',
    block: 'center',
  });
}

export default function SignupTrialForm({
  variant = 'card',
}: {
  variant?: SignupTrialFormVariant;
}) {
  const publicKey = process.env.NEXT_PUBLIC_MERCADOPAGO_PUBLIC_KEY ?? '';
  const [mpReady, setMpReady] = useState(false);
  const [idTypes, setIdTypes] = useState<IdType[]>([]);

  const [companyName, setCompanyName] = useState('');
  const [companyCode, setCompanyCode] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');
  const [cardholderName, setCardholderName] = useState('');
  const [idType, setIdType] = useState('DNI');
  const [idNumber, setIdNumber] = useState('');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [accountFieldErrors, setAccountFieldErrors] =
    useState<SignupTrialAccountErrors>({});
  const [paymentFieldErrors, setPaymentFieldErrors] = useState<{
    cardholderName?: string;
    idNumber?: string;
  }>({});
  const [done, setDone] = useState<{
    trialEndsAt: string;
    companyCode: string;
  } | null>(null);

  const [paymentSectionOpen, setPaymentSectionOpen] = useState(false);
  const paymentSectionRef = useRef<HTMLDivElement>(null);

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

  const paymentValuesRef = useRef({ cardholderName, idNumber });
  paymentValuesRef.current = { cardholderName, idNumber };

  const paymentDebounceTimersRef = useRef<
    Partial<
      Record<'cardholderName' | 'idNumber', ReturnType<typeof setTimeout>>
    >
  >({});

  useEffect(() => {
    if (useMockPayment || !publicKey) {
      setMpReady(true);
      return;
    }
    initMercadoPago(publicKey);
    setMpReady(true);
    getIdentificationTypes()
      .then((types) => {
        if (Array.isArray(types) && types.length > 0) {
          setIdTypes(types as IdType[]);
          const first = types[0] as IdType;
          if (first?.id) {
            setIdType(String(first.id));
          }
        }
      })
      .catch(() => {
        setIdTypes([{ id: 'DNI', name: 'DNI' }]);
      });
  }, [publicKey]);

  useEffect(() => {
    if (!paymentSectionOpen) return;
    const el = paymentSectionRef.current;
    if (!el) return;
    const id = window.setTimeout(() => {
      el.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }, 80);
    return () => window.clearTimeout(id);
  }, [paymentSectionOpen]);

  useEffect(() => {
    const accountTimers = accountDebounceTimersRef.current;
    const paymentTimers = paymentDebounceTimersRef.current;
    return () => {
      Object.values(accountTimers).forEach((t) => {
        if (t) clearTimeout(t);
      });
      Object.values(paymentTimers).forEach((t) => {
        if (t) clearTimeout(t);
      });
    };
  }, []);

  const paymentSectionValid = useMemo(() => {
    if (useMockPayment) return true;
    if (!publicKey || !mpReady) return false;
    return (
      cardholderName.trim().length > 0 && idNumber.trim().length > 0
    );
  }, [useMockPayment, publicKey, mpReady, cardholderName, idNumber]);

  /** Mercado Pago no listo o falta clave pública (salvo modo mock). */
  const mpBlocksSubmit = useMemo(
    () => !useMockPayment && (!Boolean(publicKey) || !mpReady),
    [mpReady, publicKey, useMockPayment],
  );

  const canSubmit = useMemo(
    () => !loading && !mpBlocksSubmit,
    [loading, mpBlocksSubmit],
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

  const cancelAllPaymentDebounces = useCallback(() => {
    const timers = paymentDebounceTimersRef.current;
    for (const key of ['cardholderName', 'idNumber'] as const) {
      const t = timers[key];
      if (t) {
        clearTimeout(t);
        delete timers[key];
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

  const cancelPaymentFieldDebounce = useCallback(
    (field: 'cardholderName' | 'idNumber') => {
      const t = paymentDebounceTimersRef.current[field];
      if (t) {
        clearTimeout(t);
        delete paymentDebounceTimersRef.current[field];
      }
    },
    [],
  );

  const applyDebouncedPaymentFieldValidation = useCallback(
    (field: 'cardholderName' | 'idNumber') => {
      if (useMockPayment) return;
      const v = paymentValuesRef.current;
      const err =
        field === 'cardholderName'
          ? validateCardholderName(v.cardholderName)
          : validateIdNumber(v.idNumber);
      setPaymentFieldErrors((prev) => {
        const next = { ...prev };
        if (err) next[field] = err;
        else delete next[field];
        return next;
      });
    },
    [useMockPayment],
  );

  const scheduleDebouncedPaymentValidation = useCallback(
    (field: 'cardholderName' | 'idNumber') => {
      if (useMockPayment) return;
      const prev = paymentDebounceTimersRef.current[field];
      if (prev) clearTimeout(prev);
      paymentDebounceTimersRef.current[field] = setTimeout(() => {
        delete paymentDebounceTimersRef.current[field];
        applyDebouncedPaymentFieldValidation(field);
      }, VALIDATION_DEBOUNCE_MS);
    },
    [applyDebouncedPaymentFieldValidation, useMockPayment],
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

  const togglePaymentSection = useCallback(() => {
    setPaymentSectionOpen((open) => !open);
  }, []);

  const submit = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault();
      setError(null);
      cancelAllAccountDebounces();
      cancelAllPaymentDebounces();
      setPaymentFieldErrors({});
      if (mpBlocksSubmit) {
        setError(
          'Esperá a que cargue el formulario de pago o revisá la configuración.',
        );
        window.requestAnimationFrame(() => {
          scrollErrorAlertIntoView();
        });
        return;
      }

      const accountErrs = runAccountValidation();
      if (Object.keys(accountErrs).length > 0) {
        window.requestAnimationFrame(() => {
          scrollFirstAccountErrorIntoView(accountErrs);
        });
        return;
      }

      if (!paymentSectionValid) {
        const panelWasOpen = paymentSectionOpen;
        setPaymentSectionOpen(true);
        if (!useMockPayment) {
          const chErr = validateCardholderName(cardholderName);
          const idErr = validateIdNumber(idNumber);
          setPaymentFieldErrors({
            ...(chErr ? { cardholderName: chErr } : {}),
            ...(idErr ? { idNumber: idErr } : {}),
          });
          const focusId = chErr ? 'su-chname' : idErr ? 'su-idnum' : undefined;
          if (focusId) {
            window.setTimeout(
              () => scrollAndFocusById(focusId),
              panelWasOpen ? 80 : 420,
            );
          }
        }
        setError('Completá los datos de pago.');
        return;
      }
      const api = getApiBaseUrl();
      if (!api) {
        setError(
          'Falta configurar NEXT_PUBLIC_API_URL (URL del servidor de la API).',
        );
        return;
      }

      let cardToken = 'mock_card_token';
      if (!useMockPayment) {
        if (!publicKey) {
          setError(
            'Falta NEXT_PUBLIC_MERCADOPAGO_PUBLIC_KEY para tokenizar la tarjeta.',
          );
          return;
        }
        const tok = await createCardToken({
          cardholderName: cardholderName.trim(),
          identificationType: idType,
          identificationNumber: idNumber.trim(),
        });
        if (!tok?.id) {
          setError('No se pudo tokenizar la tarjeta. Revisá los datos.');
          return;
        }
        cardToken = tok.id;
      }

      setLoading(true);
      try {
        const res = await fetch(`${api}/auth/signup/trial`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            email: email.trim(),
            password,
            company_name: companyName.trim(),
            company_code: companyCode.trim().toUpperCase(),
            card_token: cardToken,
          }),
        });
        const data = (await res.json().catch(() => ({}))) as {
          message?: string;
          error?: string;
          trial_ends_at?: string;
          company_code?: string;
        };
        if (!res.ok) {
          setError(
            data.message ||
              (typeof data.error === 'string' ? data.error : '') ||
              'No se pudo completar el registro. Probá de nuevo.',
          );
          return;
        }
        setDone({
          trialEndsAt: data.trial_ends_at ?? '',
          companyCode: data.company_code ?? companyCode.trim().toUpperCase(),
        });
      } catch {
        setError('Error de red. Verificá la conexión y la URL de la API.');
      } finally {
        setLoading(false);
      }
    },
    [
      cardholderName,
      companyCode,
      companyName,
      email,
      idNumber,
      idType,
      cancelAllAccountDebounces,
      cancelAllPaymentDebounces,
      mpBlocksSubmit,
      password,
      passwordConfirm,
      paymentSectionValid,
      publicKey,
      paymentSectionOpen,
      scrollFirstAccountErrorIntoView,
      runAccountValidation,
      useMockPayment,
    ],
  );

  const handleFormSubmit = useCallback(
    (e: React.FormEvent) => {
      e.preventDefault();
      void submit(e);
    },
    [submit],
  );

  if (done) {
    return (
      <div className="rounded-xl bg-white border border-gray-200 p-8 text-left shadow-sm space-y-3">
        <h2 className="text-xl font-semibold text-gray-900">
          Cuenta lista — prueba de 7 días
        </h2>
        <p className="text-gray-600 text-sm">
          Tu período de prueba incluye <strong>1 empresa</strong>,{' '}
          <strong>1 depósito</strong> y hasta <strong>2 usuarios</strong>. No se
          cobra hasta que termine la prueba.
        </p>
        {done.trialEndsAt && (
          <p className="text-sm text-gray-700">
            La prueba vence el{' '}
            <strong>
              {new Date(done.trialEndsAt).toLocaleString('es-AR', {
                dateStyle: 'long',
                timeStyle: 'short',
              })}
            </strong>
            .
          </p>
        )}
        <p className="text-sm text-gray-700">
          Código de empresa:{' '}
          <strong className="font-mono">{done.companyCode}</strong>
        </p>
        <p className="text-sm text-gray-500">
          Podés iniciar sesión en la app con este código y tu correo o usuario.
        </p>
      </div>
    );
  }

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

  const paymentInputClass = (hasError: boolean) =>
    `w-full h-12 px-4 border rounded-lg box-border ${
      hasError
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
        <div className="min-w-0 space-y-1.5 sm:max-w-xl">
          <h2 className="text-lg font-semibold text-gray-900">
            Empresa y cuenta
          </h2>
          <p className="text-sm leading-snug text-gray-500 break-words sm:leading-relaxed">
            Probá gratis 7 días con 1 depósito y hasta 2 usuarios.
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

        <div className="mt-6 flex flex-col items-center border-t border-gray-100 pt-5">
          <button
            type="button"
            id="signup-payment-toggle"
            aria-expanded={paymentSectionOpen}
            aria-controls="signup-payment-panel"
            onClick={togglePaymentSection}
            className="flex w-full max-w-xs flex-col items-center justify-center gap-1 rounded-xl py-2 text-gray-500 transition-colors hover:bg-gray-50 hover:text-gray-800 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2"
          >
            <span className="sr-only">
              {paymentSectionOpen
                ? 'Ocultar datos de pago'
                : 'Mostrar datos de pago'}
            </span>
            {paymentSectionOpen ? (
              <ChevronUp className="h-8 w-8" aria-hidden strokeWidth={2} />
            ) : (
              <ChevronDown className="h-8 w-8" aria-hidden strokeWidth={2} />
            )}
          </button>
        </div>
      </div>

      <div
        id="signup-payment-panel"
        ref={paymentSectionRef}
        role="region"
        aria-label="Datos de pago"
        className={`grid overflow-hidden transition-[grid-template-rows] duration-500 ease-[cubic-bezier(0.4,0,0.2,1)] motion-reduce:transition-none motion-reduce:duration-0 ${
          paymentSectionOpen ? 'grid-rows-[1fr]' : 'grid-rows-[0fr]'
        }`}
      >
        <div className="min-h-0 overflow-hidden">
          <div
            className={`space-y-6 border-t border-gray-100 pt-6 ${
              paymentSectionOpen ? 'signup-payment-reveal' : ''
            }`}
          >
          <div className="space-y-5">
          {!useMockPayment && (
            <>
              <div className="space-y-3">
                <MercadoPagoLogo />
                <p className="text-xs text-gray-500 leading-relaxed">
                  Los pagos se procesan de forma segura con Mercado Pago; En
                  Punto no almacena el número de tu tarjeta. No se cobra durante
                  la prueba.
                </p>
              </div>
              {mpReady && publicKey ? (
                <>
                  <h2 className="text-lg font-semibold text-gray-900">
                    Datos de la tarjeta
                  </h2>
                  <div>
                    <label
                      htmlFor="su-chname"
                      className="block text-sm font-medium text-gray-700 mb-1"
                    >
                      Nombre del titular
                    </label>
                    <input
                      id="su-chname"
                      value={cardholderName}
                      onChange={(e) => {
                        setCardholderName(e.target.value);
                        setPaymentFieldErrors((p) => {
                          if (!p.cardholderName) return p;
                          const next = { ...p };
                          delete next.cardholderName;
                          return next;
                        });
                        scheduleDebouncedPaymentValidation('cardholderName');
                      }}
                      onBlur={() => {
                        cancelPaymentFieldDebounce('cardholderName');
                        if (useMockPayment) return;
                        const err = validateCardholderName(cardholderName);
                        setPaymentFieldErrors((prev) => {
                          const next = { ...prev };
                          if (err) next.cardholderName = err;
                          else delete next.cardholderName;
                          return next;
                        });
                      }}
                      autoComplete="cc-name"
                      aria-invalid={Boolean(paymentFieldErrors.cardholderName)}
                      aria-describedby={
                        paymentFieldErrors.cardholderName
                          ? 'su-chname-error'
                          : undefined
                      }
                      className={paymentInputClass(
                        Boolean(paymentFieldErrors.cardholderName),
                      )}
                    />
                    {paymentFieldErrors.cardholderName && (
                      <p
                        id="su-chname-error"
                        className="mt-1.5 text-sm text-red-600"
                        role="alert"
                      >
                        {paymentFieldErrors.cardholderName}
                      </p>
                    )}
                  </div>
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Tipo de documento
                      </label>
                      <select
                        value={idType}
                        onChange={(e) => setIdType(e.target.value)}
                        className="w-full h-12 px-3 border border-gray-300 rounded-lg box-border focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                      >
                        {idTypes.length === 0 ? (
                          <option value="DNI">DNI</option>
                        ) : (
                          idTypes.map((t) => (
                            <option key={t.id} value={t.id}>
                              {t.name}
                            </option>
                          ))
                        )}
                      </select>
                    </div>
                    <div>
                      <label
                        htmlFor="su-idnum"
                        className="block text-sm font-medium text-gray-700 mb-1"
                      >
                        Número
                      </label>
                      <input
                        id="su-idnum"
                        value={idNumber}
                        onChange={(e) => {
                          setIdNumber(e.target.value);
                          setPaymentFieldErrors((p) => {
                            if (!p.idNumber) return p;
                            const next = { ...p };
                            delete next.idNumber;
                            return next;
                          });
                          scheduleDebouncedPaymentValidation('idNumber');
                        }}
                        onBlur={() => {
                          cancelPaymentFieldDebounce('idNumber');
                          if (useMockPayment) return;
                          const err = validateIdNumber(idNumber);
                          setPaymentFieldErrors((prev) => {
                            const next = { ...prev };
                            if (err) next.idNumber = err;
                            else delete next.idNumber;
                            return next;
                          });
                        }}
                        autoComplete="off"
                        aria-invalid={Boolean(paymentFieldErrors.idNumber)}
                        aria-describedby={
                          paymentFieldErrors.idNumber ? 'su-idnum-error' : undefined
                        }
                        className={paymentInputClass(
                          Boolean(paymentFieldErrors.idNumber),
                        )}
                      />
                      {paymentFieldErrors.idNumber && (
                        <p
                          id="su-idnum-error"
                          className="mt-1.5 text-sm text-red-600"
                          role="alert"
                        >
                          {paymentFieldErrors.idNumber}
                        </p>
                      )}
                    </div>
                  </div>
                  <div className="space-y-2">
                    <label className="block text-sm font-medium text-gray-700">
                      Número de tarjeta
                    </label>
                    <div className="flex h-12 items-center px-3 border border-gray-300 rounded-lg bg-white box-border">
                      <CardNumber placeholder="1234 1234 1234 1234" />
                    </div>
                  </div>
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Vencimiento
                      </label>
                      <div className="flex h-12 items-center px-3 border border-gray-300 rounded-lg bg-white box-border">
                        <ExpirationDate placeholder="MM/AA" />
                      </div>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Código de seguridad
                      </label>
                      <div className="flex h-12 items-center px-3 border border-gray-300 rounded-lg bg-white box-border">
                        <SecurityCode placeholder="123" />
                      </div>
                    </div>
                  </div>
                </>
              ) : (
                <p className="text-sm text-amber-800 bg-amber-50 border border-amber-100 rounded-lg px-3 py-2">
                  Configurá{' '}
                  <code className="text-xs">
                    NEXT_PUBLIC_MERCADOPAGO_PUBLIC_KEY
                  </code>{' '}
                  o usá{' '}
                  <code className="text-xs">
                    NEXT_PUBLIC_SIGNUP_USE_MOCK_PAYMENT=true
                  </code>{' '}
                  con el servidor en modo mock.
                </p>
              )}
            </>
          )}

          {useMockPayment && (
            <div className="space-y-2 rounded-lg border border-dashed border-amber-200 bg-amber-50/50 px-4 py-5">
              <p className="text-sm font-medium text-amber-900">
                Modo desarrollo
              </p>
              <p className="text-xs text-amber-800">
                Pago simulado (sin Mercado Pago). Activá también{' '}
                <code className="text-xs">SIGNUP_ALLOW_MOCK_PAYMENT=true</code> en
                el backend.
              </p>
            </div>
          )}
          </div>
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
