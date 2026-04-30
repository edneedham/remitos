'use client';

import dynamic from 'next/dynamic';
import Link from 'next/link';
import { useEffect, useRef, useState } from 'react';
import { useRouter } from 'next/navigation';
import { initMercadoPago } from '@mercadopago/sdk-react';
import { getApiBaseUrl } from '../../lib/apiUrl';
import { BILLING_LEGAL_NOTICE_AR } from '../../lib/billingLegalNotice';
import { PLAN_CATALOG } from '../../lib/planCatalog';
import {
  canAccessWebManagement,
  clearWebSession,
  fetchProfile,
  fetchWithWebAuth,
  hasWebSession,
  postWithWebAuth,
} from '../../lib/webAuth';
import { needsActivateSubscription } from '../lib/activateSubscriptionGate';
import type { Entitlement } from '../lib/entitlementTypes';

const CardPayment = dynamic(
  () => import('@mercadopago/sdk-react').then((m) => m.CardPayment),
  {
    ssr: false,
    loading: () => (
      <p className="text-sm text-gray-600">Cargando formulario de pago…</p>
    ),
  },
);

const SELECTABLE_PLANS = PLAN_CATALOG.filter((p) => p.id !== 'corporativo');

const publicKey =
  typeof process !== 'undefined'
    ? (process.env.NEXT_PUBLIC_MERCADOPAGO_PUBLIC_KEY ?? '').trim()
    : '';

const useMockPayment =
  typeof process !== 'undefined' &&
  process.env.NEXT_PUBLIC_SIGNUP_USE_MOCK_PAYMENT === 'true';

type PlanChoice = 'pyme' | 'empresa';

type PlanPricingResponse = {
  plan_id: string;
  currency: string;
  amount_minor: number;
  monthly_list_usd: number;
  ars_per_usd: number;
  fx_source: string;
  fx_effective_date?: string;
  legal_notice_ar: string;
};

export default function ActivateSubscriptionPageClient() {
  const router = useRouter();
  const [ready, setReady] = useState(false);
  const [planId, setPlanId] = useState<PlanChoice>('pyme');
  const [payerEmail, setPayerEmail] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [brickAmountArs, setBrickAmountArs] = useState<number | null>(null);
  const [pricingError, setPricingError] = useState<string | null>(null);
  const [pricingMeta, setPricingMeta] = useState<PlanPricingResponse | null>(
    null,
  );
  const mpInitRef = useRef(false);

  useEffect(() => {
    if (!publicKey || mpInitRef.current || useMockPayment) return;
    initMercadoPago(publicKey);
    mpInitRef.current = true;
  }, []);

  useEffect(() => {
    if (!ready || useMockPayment || !publicKey) {
      setBrickAmountArs(null);
      setPricingError(null);
      setPricingMeta(null);
      return;
    }
    let cancelled = false;
    setBrickAmountArs(null);
    setPricingError(null);
    setPricingMeta(null);
    (async () => {
      const res = await fetchWithWebAuth(
        `/auth/me/plan-pricing?plan_id=${encodeURIComponent(planId)}`,
      );
      if (cancelled) return;
      if (!res.ok) {
        const body = (await res.json().catch(() => ({}))) as { message?: string };
        setPricingError(
          body.message ||
            'No se pudo obtener el importe en pesos. Revisá la configuración del servidor.',
        );
        return;
      }
      const data = (await res.json()) as PlanPricingResponse;
      if (cancelled) return;
      setBrickAmountArs(data.amount_minor / 100);
      setPricingMeta(data);
    })();
    return () => {
      cancelled = true;
    };
  }, [planId, ready, useMockPayment, publicKey]);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      if (!hasWebSession()) {
        router.replace('/login');
        return;
      }
      const api = getApiBaseUrl();
      if (!api) {
        setError(
          'Falta configurar NEXT_PUBLIC_API_URL (URL del servidor de la API).',
        );
        setReady(true);
        return;
      }

      const profile = await fetchProfile();
      if (cancelled) return;
      if (!profile || !canAccessWebManagement(profile.role)) {
        clearWebSession();
        router.replace('/login');
        return;
      }
      const email = profile.email?.trim() || profile.username?.trim() || '';
      setPayerEmail(email);

      const res = await fetchWithWebAuth('/auth/me/entitlement');
      if (cancelled) return;
      if (!res.ok) {
        setError('No se pudieron cargar los datos de la cuenta.');
        setReady(true);
        return;
      }
      const ent = (await res.json()) as Entitlement;
      if (!needsActivateSubscription(ent)) {
        router.replace('/dashboard');
        return;
      }

      setReady(true);
    }

    void load();
    return () => {
      cancelled = true;
    };
  }, [router]);

  async function submitMock() {
    setError(null);
    setSubmitting(true);
    try {
      const res = await postWithWebAuth('/auth/me/activate-subscription', {
        plan_id: planId,
        use_mock_payment: true,
      });
      const data = (await res.json().catch(() => ({}))) as {
        message?: string;
      };
      if (!res.ok) {
        throw new Error(
          data.message || 'No se pudo activar la suscripción (simulación).',
        );
      }
      router.replace('/dashboard');
      router.refresh();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Error al activar.');
    } finally {
      setSubmitting(false);
    }
  }

  if (!ready) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-16 text-center text-sm text-gray-600">
        Cargando…
      </div>
    );
  }

  const showMpBrick =
    !useMockPayment && Boolean(publicKey) && brickAmountArs != null;
  const showMpConfigError = !useMockPayment && !publicKey;
  const showMpPricingLoading =
    !useMockPayment && Boolean(publicKey) && brickAmountArs == null && !pricingError;

  return (
    <div className="mx-auto max-w-xl px-4 py-10 sm:px-6 lg:px-8">
      <h1 className="text-2xl font-bold tracking-tight text-gray-900">
        Activar suscripción
      </h1>
      <p className="mt-3 text-sm leading-relaxed text-gray-600">
        Tu prueba terminó o el acceso pago no está vigente. Elegí un plan y
        cargá un medio de pago para seguir usando la aplicación y el panel.
      </p>

      <fieldset className="mt-8 space-y-3">
        <legend className="text-sm font-semibold text-gray-900">Plan</legend>
        <div className="space-y-2">
          {SELECTABLE_PLANS.map((p) => (
            <label
              key={p.id}
              className={`flex cursor-pointer items-start gap-3 rounded-lg border p-4 ${
                planId === p.id
                  ? 'border-blue-500 bg-blue-50/60'
                  : 'border-gray-200 bg-white'
              }`}
            >
              <input
                type="radio"
                name="plan"
                value={p.id}
                checked={planId === p.id}
                onChange={() => setPlanId(p.id as PlanChoice)}
                className="mt-1"
              />
              <span>
                <span className="font-semibold text-gray-900">{p.name}</span>
                <span className="block text-sm text-gray-600">
                  {p.monthlyPriceLabel} · {p.description}
                </span>
              </span>
            </label>
          ))}
        </div>
      </fieldset>

      <p className="mt-6 text-sm leading-relaxed text-gray-600">
        {pricingMeta?.legal_notice_ar ?? BILLING_LEGAL_NOTICE_AR}
      </p>
      {pricingMeta && !pricingError && !useMockPayment ? (
        <p className="mt-2 text-xs text-gray-500">
          Cotización aplicada:{' '}
          <span className="font-medium text-gray-700">
            1 USD ={' '}
            {pricingMeta.ars_per_usd.toLocaleString('es-AR', {
              maximumFractionDigits: 2,
            })}{' '}
            ARS
          </span>
          {pricingMeta.fx_effective_date
            ? ` · Fecha referencia: ${pricingMeta.fx_effective_date}`
            : null}
          {pricingMeta.fx_source ? ` · Fuente: ${pricingMeta.fx_source}` : null}
        </p>
      ) : null}

      <p className="mt-6 text-sm text-gray-600">
        ¿Necesitás plan{' '}
        <span className="font-medium text-gray-800">Corporativo</span>? Escribinos
        a{' '}
        <a className="text-blue-700 underline" href="mailto:soporte@enpunto.app">
          soporte@enpunto.app
        </a>
        .
      </p>

      <div className="mt-8 rounded-xl border border-gray-200 bg-white p-4 shadow-sm sm:p-6">
        {showMpConfigError ? (
          <p className="text-sm text-red-700" role="alert">
            Falta configurar{' '}
            <code className="rounded bg-gray-100 px-1">NEXT_PUBLIC_MERCADOPAGO_PUBLIC_KEY</code>{' '}
            en el sitio web, o usá el modo simulado en desarrollo (
            <code className="rounded bg-gray-100 px-1">
              NEXT_PUBLIC_SIGNUP_USE_MOCK_PAYMENT=true
            </code>
            ) con la API en{' '}
            <code className="rounded bg-gray-100 px-1">
              SIGNUP_ALLOW_MOCK_PAYMENT=true
            </code>
            .
          </p>
        ) : null}

        {pricingError ? (
          <p className="mt-3 text-sm text-red-700" role="alert">
            {pricingError}
          </p>
        ) : null}

        {showMpPricingLoading ? (
          <p className="mt-3 text-sm text-gray-600">Calculando importe en pesos…</p>
        ) : null}

        {useMockPayment ? (
          <div className="space-y-4">
            <p className="text-sm text-gray-600">
              Modo desarrollo: se simula el guardado de la tarjeta (sin cobro
              real).
            </p>
            <button
              type="button"
              disabled={submitting}
              onClick={() => void submitMock()}
              className="inline-flex w-full justify-center rounded-lg bg-blue-600 px-4 py-3 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
            >
              {submitting ? 'Activando…' : 'Activar suscripción (simulado)'}
            </button>
          </div>
        ) : null}

        {showMpBrick && payerEmail ? (
          <div className="mt-2 space-y-2">
            <CardPayment
              key={`${planId}-${brickAmountArs}`}
              initialization={{
                amount: brickAmountArs ?? 0,
                payer: { email: payerEmail },
              }}
              locale="es-AR"
              onSubmit={async (data) => {
                setError(null);
                setSubmitting(true);
                try {
                  const res = await postWithWebAuth(
                    '/auth/me/activate-subscription',
                    {
                      plan_id: planId,
                      card_token: data.token,
                    },
                  );
                  const body = (await res.json().catch(() => ({}))) as {
                    message?: string;
                  };
                  if (!res.ok) {
                    const msg =
                      body.message ||
                      'No se pudo activar la suscripción. Revisá la tarjeta.';
                    setError(msg);
                    throw new Error(msg);
                  }
                  router.replace('/dashboard');
                  router.refresh();
                } catch (e) {
                  const msg =
                    e instanceof Error
                      ? e.message
                      : 'No se pudo completar el pago.';
                  setError((prev) => prev ?? msg);
                  throw e instanceof Error
                    ? e
                    : new Error(msg);
                } finally {
                  setSubmitting(false);
                }
              }}
            />
            {submitting ? (
              <p className="text-xs text-gray-500">Procesando…</p>
            ) : null}
          </div>
        ) : null}

        {showMpBrick && !payerEmail ? (
          <p className="text-sm text-amber-800" role="status">
            Tu usuario no tiene email cargado; agregalo en el perfil o contactá
            soporte para activar la cuenta.
          </p>
        ) : null}
      </div>

      {error ? (
        <div
          className="mt-6 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800"
          role="alert"
        >
          {error}
        </div>
      ) : null}

      <p className="mt-8 text-center text-sm text-gray-500">
        <Link href="/dashboard/billing" className="text-blue-700 underline">
          Ver facturación
        </Link>
        {' · '}
        <Link href="/dashboard" className="text-blue-700 underline">
          Volver al panel
        </Link>
      </p>
    </div>
  );
}
