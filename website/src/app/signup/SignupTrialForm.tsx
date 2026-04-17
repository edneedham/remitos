'use client';

import { useCallback, useEffect, useState } from 'react';
import {
  CardNumber,
  createCardToken,
  ExpirationDate,
  getIdentificationTypes,
  initMercadoPago,
  SecurityCode,
} from '@mercadopago/sdk-react';
import { ArrowRight, Loader2 } from 'lucide-react';
import { getApiBaseUrl } from '../lib/apiUrl';

type IdType = { id: string; name: string };

const useMockPayment =
  process.env.NEXT_PUBLIC_SIGNUP_USE_MOCK_PAYMENT === 'true';

export default function SignupTrialForm() {
  const publicKey = process.env.NEXT_PUBLIC_MERCADOPAGO_PUBLIC_KEY ?? '';
  const [mpReady, setMpReady] = useState(false);
  const [idTypes, setIdTypes] = useState<IdType[]>([]);

  const [companyName, setCompanyName] = useState('');
  const [companyCode, setCompanyCode] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [cardholderName, setCardholderName] = useState('');
  const [idType, setIdType] = useState('DNI');
  const [idNumber, setIdNumber] = useState('');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState<{
    trialEndsAt: string;
    companyCode: string;
  } | null>(null);

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

  const submit = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault();
      setError(null);
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
      publicKey,
    ],
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

  return (
    <form
      onSubmit={submit}
      className="rounded-xl bg-white border border-gray-200 p-6 shadow-sm space-y-5 text-left"
    >
      <div className="space-y-1">
        <h2 className="text-lg font-semibold text-gray-900">
          Datos de la empresa
        </h2>
        <p className="text-sm text-gray-500">
          Incluye 7 días de prueba: 1 depósito y hasta 2 usuarios. La tarjeta no
          se cobra durante la prueba.
        </p>
      </div>

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

      {!useMockPayment && (
        <>
          <div className="border-t border-gray-100 pt-4 space-y-3">
            <h3 className="text-sm font-semibold text-gray-900">
              Tarjeta (Mercado Pago)
            </h3>
            <p className="text-xs text-gray-500">
              Los datos se envían directamente a Mercado Pago; no guardamos el
              número de tarjeta. Se guarda solo para cobrar después del período
              de prueba.
            </p>
            {mpReady && publicKey ? (
              <>
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
                    required
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
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
                      className="w-full px-3 py-3 border border-gray-300 rounded-lg"
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
                      required
                      className="w-full px-4 py-3 border border-gray-300 rounded-lg"
                    />
                  </div>
                </div>
                <div className="space-y-2">
                  <label className="block text-sm font-medium text-gray-700">
                    Número de tarjeta
                  </label>
                  <div className="px-3 py-2 border border-gray-300 rounded-lg bg-white">
                    <CardNumber placeholder="1234 1234 1234 1234" />
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Vencimiento
                    </label>
                    <div className="px-3 py-2 border border-gray-300 rounded-lg bg-white">
                      <ExpirationDate placeholder="MM/AA" />
                    </div>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Código de seguridad
                    </label>
                    <div className="px-3 py-2 border border-gray-300 rounded-lg bg-white">
                      <SecurityCode placeholder="123" />
                    </div>
                  </div>
                </div>
              </>
            ) : (
              <p className="text-sm text-amber-800 bg-amber-50 border border-amber-100 rounded-lg px-3 py-2">
                Configurá{' '}
                <code className="text-xs">NEXT_PUBLIC_MERCADOPAGO_PUBLIC_KEY</code>{' '}
                o usá{' '}
                <code className="text-xs">
                  NEXT_PUBLIC_SIGNUP_USE_MOCK_PAYMENT=true
                </code>{' '}
                con el servidor en modo mock.
              </p>
            )}
          </div>
        </>
      )}

      {useMockPayment && (
        <p className="text-xs text-amber-800 bg-amber-50 border border-amber-100 rounded-lg px-3 py-2">
          Modo desarrollo: pago simulado (sin Mercado Pago). Activá también{' '}
          <code className="text-xs">SIGNUP_ALLOW_MOCK_PAYMENT=true</code> en el
          backend.
        </p>
      )}

      {error && (
        <p className="text-sm text-red-700 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
          {error}
        </p>
      )}

      <button
        type="submit"
        disabled={
          loading || (!useMockPayment && (!publicKey || !mpReady))
        }
        className="w-full inline-flex items-center justify-center px-4 py-3 bg-blue-600 text-white font-semibold rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50"
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
    </form>
  );
}
