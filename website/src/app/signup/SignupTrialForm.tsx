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
import MercadoPagoLogo from './MercadoPagoLogo';

type IdType = { id: string; name: string };

const useMockPayment =
  process.env.NEXT_PUBLIC_SIGNUP_USE_MOCK_PAYMENT === 'true';

export type SignupTrialFormVariant = 'card' | 'embedded';

const ACCOUNT_FIELD_IDS = [
  'su-company',
  'su-code',
  'su-email',
  'su-password',
  'su-password-confirm',
] as const;

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
  const [done, setDone] = useState<{
    trialEndsAt: string;
    companyCode: string;
  } | null>(null);

  const [paymentSectionOpen, setPaymentSectionOpen] = useState(false);
  const paymentSectionRef = useRef<HTMLDivElement>(null);

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

  const accountSectionValid = useMemo(() => {
    const code = companyCode.trim();
    const em = email.trim();
    return (
      companyName.trim().length >= 2 &&
      code.length >= 2 &&
      code.length <= 32 &&
      /^[A-Za-z0-9_-]+$/.test(code) &&
      /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(em) &&
      password.length >= 8 &&
      password === passwordConfirm
    );
  }, [companyName, companyCode, email, password, passwordConfirm]);

  const paymentSectionValid = useMemo(() => {
    if (useMockPayment) return true;
    if (!publicKey || !mpReady) return false;
    return (
      cardholderName.trim().length > 0 && idNumber.trim().length > 0
    );
  }, [useMockPayment, publicKey, mpReady, cardholderName, idNumber]);

  const canSubmit = useMemo(
    () =>
      !loading &&
      accountSectionValid &&
      paymentSectionValid &&
      (useMockPayment || (Boolean(publicKey) && mpReady)),
    [
      accountSectionValid,
      loading,
      mpReady,
      paymentSectionValid,
      publicKey,
      useMockPayment,
    ],
  );

  const validateAccountFields = useCallback(() => {
    setError(null);
    for (const fieldId of ACCOUNT_FIELD_IDS) {
      const el = document.getElementById(fieldId) as HTMLInputElement | null;
      if (el && !el.checkValidity()) {
        el.reportValidity();
        return false;
      }
    }
    if (password !== passwordConfirm) {
      setError('Las contraseñas no coinciden.');
      return false;
    }
    return true;
  }, [password, passwordConfirm]);

  const togglePaymentSection = useCallback(() => {
    setPaymentSectionOpen((open) => !open);
  }, []);

  const submit = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault();
      setError(null);
      if (!validateAccountFields()) {
        return;
      }
      if (!paymentSectionValid) {
        setError('Completá los datos de pago.');
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
      password,
      passwordConfirm,
      paymentSectionValid,
      publicKey,
      validateAccountFields,
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
              onChange={(e) => setCompanyName(e.target.value)}
              required
              minLength={2}
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
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
              onChange={(e) => setCompanyCode(e.target.value.toUpperCase())}
              required
              minLength={2}
              maxLength={32}
              pattern="[A-Za-z0-9_-]+"
              title="Letras, números, guiones"
              className="w-full px-4 py-3 border border-gray-300 rounded-lg font-mono uppercase focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
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
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              autoComplete="email"
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
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
              onChange={(e) => setPassword(e.target.value)}
              required
              minLength={8}
              autoComplete="new-password"
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
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
              onChange={(e) => setPasswordConfirm(e.target.value)}
              required
              minLength={8}
              autoComplete="new-password"
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
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
                      onChange={(e) => setCardholderName(e.target.value)}
                      className="w-full h-12 px-4 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    />
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
                        onChange={(e) => setIdNumber(e.target.value)}
                        className="w-full h-12 px-4 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                      />
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
          <p className="text-sm text-red-700 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
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
