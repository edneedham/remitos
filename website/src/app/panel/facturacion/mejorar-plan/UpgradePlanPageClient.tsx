'use client';

import dynamic from 'next/dynamic';
import Image from 'next/image';
import Link from 'next/link';
import { useEffect, useRef, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Loader2 } from 'lucide-react';
import { initMercadoPago } from '@mercadopago/sdk-react';
import { getApiBaseUrl } from '../../../lib/apiUrl';
import type { PlanPricingResponse } from '../../../lib/planPricing';
import { PLAN_CATALOG } from '../../../lib/planCatalog';
import {
  canAccessWebManagement,
  clearWebSession,
  fetchProfile,
  fetchWithWebAuth,
  hasWebSession,
  postWithWebAuth,
  refreshWebSession,
} from '../../../lib/webAuth';
import {
  deriveBillingPresentation,
  formatDateTime,
  formatPlanLabel,
} from '../../lib/billingPresentation';
import { computeUpgradeProrationDueMinor } from '../../lib/prorationPreview';
import { subscriptionTier } from '../../lib/selfServePlan';
import type { Entitlement } from '../../lib/entitlementTypes';
import type { PlanCatalogLimitsResponse } from '../../lib/planCatalogLimits';
import { formatInvoiceMoney } from '../../lib/invoiceFormat';
import { BILLING_LEGAL_NOTICE_AR } from '../../../lib/billingLegalNotice';
import { UpgradePlanBodySkeleton } from '../../components/PanelSkeletons';

const CardPayment = dynamic(
  () => import('@mercadopago/sdk-react').then((m) => m.CardPayment),
  {
    ssr: false,
    loading: () => (
      <p className="text-sm text-gray-600">Cargando formulario de pago…</p>
    ),
  },
);

const mpPublicKey =
  typeof process !== 'undefined'
    ? (process.env.NEXT_PUBLIC_MERCADOPAGO_PUBLIC_KEY ?? '').trim()
    : '';

const useMockPayment =
  typeof process !== 'undefined' &&
  process.env.NEXT_PUBLIC_SIGNUP_USE_MOCK_PAYMENT === 'true';

/** Fallback until GET /auth/me/plan-catalog-limits loads — matches billing.PlanLimitsByID. */
const PYME_LIMITS_FALLBACK = {
  warehouses: 2,
  users: 3,
  documentsMonthly: 500,
} as const;

function pctRemainingLabel(fraction: number): string {
  return `${Math.round(fraction * 100)} %`;
}

export default function UpgradePlanPageClient() {
  const router = useRouter();
  const [entitlementLoading, setEntitlementLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [entitlement, setEntitlement] = useState<Entitlement | null>(null);
  const [pricingPyme, setPricingPyme] = useState<PlanPricingResponse | null>(
    null,
  );
  const [pricingEmpresa, setPricingEmpresa] =
    useState<PlanPricingResponse | null>(null);
  const [pricingError, setPricingError] = useState<string | null>(null);
  const [catalogLimits, setCatalogLimits] =
    useState<PlanCatalogLimitsResponse | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);
  const [payerEmail, setPayerEmail] = useState('');
  const [paymentRecovery, setPaymentRecovery] = useState<{
    amountMinor: number;
  } | null>(null);
  const mpInitRef = useRef(false);

  useEffect(() => {
    if (!mpPublicKey || mpInitRef.current || useMockPayment) return;
    initMercadoPago(mpPublicKey);
    mpInitRef.current = true;
  }, []);

  type PlanChangeOptions = {
    confirmDowngrade?: boolean;
    card_token?: string;
    use_mock_payment?: boolean;
    /** Proration total (minor ARS) — needed to show Mercado Pago brick after HTTP 402 */
    upgradeAmountMinor?: number;
  };

  /** Returns true when the plan change succeeded (upgrade or downgrade). */
  async function submitPlanChange(
    planId: 'pyme' | 'empresa',
    options: PlanChangeOptions = {},
  ): Promise<boolean> {
    setSubmitting(true);
    setActionError(null);
    setActionSuccess(null);

    const body: Record<string, unknown> = {
      plan_id: planId,
      ...(options.confirmDowngrade ? { confirm_downgrade: true } : {}),
      ...(options.card_token ? { card_token: options.card_token } : {}),
      ...(options.use_mock_payment ? { use_mock_payment: true } : {}),
    };

    const res = await postWithWebAuth('/auth/me/plan/change', body);
    const data = (await res.json().catch(() => ({}))) as {
      message?: string;
      pending_plan?: string;
    };

    if (res.ok) {
      setPaymentRecovery(null);
      setActionSuccess(data.message || 'Plan actualizado correctamente.');
      const entRes = await fetchWithWebAuth('/auth/me/entitlement');
      if (entRes.ok) {
        setEntitlement((await entRes.json()) as Entitlement);
      }
      setSubmitting(false);
      window.setTimeout(() => {
        router.push('/panel/facturacion');
      }, 1500);
      return true;
    }

    setSubmitting(false);

    const fallbackMsg =
      'No se pudo aplicar el cambio de plan. Probá de nuevo más tarde.';
    const msg = (data.message || '').trim() || fallbackMsg;

    if (res.status === 402) {
      setActionError(msg);
      if (
        planId === 'empresa' &&
        typeof options.upgradeAmountMinor === 'number' &&
        options.upgradeAmountMinor > 0
      ) {
        setPaymentRecovery({ amountMinor: options.upgradeAmountMinor });
      }
      return false;
    }

    setPaymentRecovery(null);
    setActionError(msg);
    return false;
  }

  useEffect(() => {
    if (!hasWebSession()) {
      router.replace('/ingresar');
      return;
    }

    let cancelled = false;

    async function load() {
      const api = getApiBaseUrl();
      if (!api) {
        setError(
          'Falta configurar NEXT_PUBLIC_API_URL (URL del servidor de la API).',
        );
        setEntitlementLoading(false);
        return;
      }

      await refreshWebSession();

      const profile = await fetchProfile();
      if (cancelled) return;
      if (!profile || !canAccessWebManagement(profile.role)) {
        clearWebSession();
        router.replace('/ingresar');
        return;
      }

      const email = profile.email?.trim() || profile.username?.trim() || '';
      setPayerEmail(email);

      const res = await fetchWithWebAuth('/auth/me/entitlement');
      if (cancelled) return;
      if (res.status === 401) {
        clearWebSession();
        router.replace('/ingresar');
        return;
      }
      if (!res.ok) {
        const body = (await res.json().catch(() => ({}))) as {
          message?: string;
        };
        setError(
          body.message ||
            'No se pudieron obtener los datos de tu cuenta. Probá de nuevo más tarde.',
        );
        setEntitlementLoading(false);
        return;
      }

      const data = (await res.json()) as Entitlement;
      setEntitlement(data);
      setEntitlementLoading(false);
    }

    void load();
    return () => {
      cancelled = true;
    };
  }, [router]);

  useEffect(() => {
    if (!entitlement) return;

    let cancelled = false;

    async function loadLimits() {
      const res = await fetchWithWebAuth('/auth/me/plan-catalog-limits');
      if (cancelled || !res.ok) return;
      const data = (await res.json()) as PlanCatalogLimitsResponse;
      setCatalogLimits(data);
    }

    void loadLimits();
    return () => {
      cancelled = true;
    };
  }, [entitlement]);

  useEffect(() => {
    if (!entitlement) return;

    let cancelled = false;

    async function loadPricing() {
      setPricingError(null);
      const [r1, r2] = await Promise.all([
        fetchWithWebAuth('/auth/me/plan-pricing?plan_id=pyme'),
        fetchWithWebAuth('/auth/me/plan-pricing?plan_id=empresa'),
      ]);
      if (cancelled) return;

      if (!r1.ok || !r2.ok) {
        const body = (await r1.json().catch(() => ({}))) as {
          message?: string;
        };
        setPricingError(
          body.message ||
            'No se pudieron obtener los importes en pesos. Probá más tarde o revisá la cotización MEP en el servidor.',
        );
        setPricingPyme(null);
        setPricingEmpresa(null);
        return;
      }

      setPricingPyme((await r1.json()) as PlanPricingResponse);
      setPricingEmpresa((await r2.json()) as PlanPricingResponse);
    }

    void loadPricing();
    return () => {
      cancelled = true;
    };
  }, [entitlement]);

  if (entitlementLoading && !error) {
    return (
      <div className="bg-gray-50 px-4 pb-14 pt-8">
        <div className="mx-auto max-w-[92rem] space-y-8">
          <header className="space-y-2">
            <h1 className="text-3xl font-bold tracking-tight text-gray-900">
              Cambiar de plan
            </h1>
            <p className="text-base leading-relaxed text-gray-600">
              Revisá cómo calculamos el ajuste cuando pasás a un plan superior en
              medio de un período de facturación ya abonado.
            </p>
          </header>
          <UpgradePlanBodySkeleton />
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-gray-50 px-4 pb-14 pt-8">
        <div className="mx-auto max-w-[92rem] space-y-6">
          <div
            className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800"
            role="alert"
          >
            {error}
          </div>
          <p>
            <Link
              href="/panel/facturacion"
              className="text-sm font-semibold text-blue-700 underline"
            >
              Volver a Facturación
            </Link>
          </p>
        </div>
      </div>
    );
  }

  if (!entitlement) {
    return null;
  }

  const now = Date.now();
  const billing = deriveBillingPresentation(entitlement, now);
  const tier = subscriptionTier(
    entitlement?.subscription_plan,
    entitlement?.documents_monthly_limit,
  );

  const empresaCatalog = PLAN_CATALOG.find((p) => p.id === 'empresa');
  const pymeCatalog = PLAN_CATALOG.find((p) => p.id === 'pyme');

  const expiresRaw = entitlement?.subscription_expires_at;
  const expiresMs = expiresRaw ? Date.parse(expiresRaw) : Number.NaN;
  const hasPeriodEnd =
    Number.isFinite(expiresMs) && expiresMs > now && Boolean(expiresRaw);

  const proration =
    billing.hasActivePaymentPeriod &&
    hasPeriodEnd &&
    pricingPyme &&
    pricingEmpresa
      ? computeUpgradeProrationDueMinor({
          nowMs: now,
          subscriptionExpiresMs: expiresMs,
          currentMonthlyMinor: pricingPyme.amount_minor,
          newMonthlyMinor: pricingEmpresa.amount_minor,
        })
      : null;

  const showProrationPanel =
    (tier === 'pyme' || tier === 'other') &&
    billing.hasActivePaymentPeriod &&
    hasPeriodEnd &&
    proration &&
    pricingPyme &&
    pricingEmpresa;

  const legalNotice =
    pricingEmpresa?.legal_notice_ar ??
    pricingPyme?.legal_notice_ar ??
    BILLING_LEGAL_NOTICE_AR;

  const pymeResolvedLimits = {
    warehouses:
      catalogLimits?.plans?.pyme?.max_warehouses ?? PYME_LIMITS_FALLBACK.warehouses,
    users: catalogLimits?.plans?.pyme?.max_users ?? PYME_LIMITS_FALLBACK.users,
    documentsMonthly:
      catalogLimits?.plans?.pyme?.documents_monthly_limit ??
      PYME_LIMITS_FALLBACK.documentsMonthly,
  };

  return (
    <div className="bg-gray-50 px-4 pb-14 pt-8">
      <div className="mx-auto max-w-[92rem] space-y-8">
        <header className="space-y-2">
          <h1 className="text-3xl font-bold tracking-tight text-gray-900">
            Cambiar de plan
          </h1>
          <p className="text-base leading-relaxed text-gray-600">
            Revisá cómo calculamos el ajuste cuando pasás a un plan superior en
            medio de un período de facturación ya abonado.
          </p>
        </header>

        {tier === 'corporativo' ? (
          <section
            className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm"
            aria-labelledby="corp-heading"
          >
            <h2
              id="corp-heading"
              className="text-lg font-semibold text-gray-900"
            >
              Plan Corporativo
            </h2>
            <p className="mt-2 text-sm leading-relaxed text-gray-600">
              Ya estás en la categoría comercial que coordina ventas. Para más
              volumen o condiciones a medida, escribinos y lo vemos con tu
              cuenta.
            </p>
            <Link
              href="/contacto"
              className="mt-4 inline-flex rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-blue-700"
            >
              Contactar
            </Link>
          </section>
        ) : null}

        {tier === 'empresa' ? (
          <section
            className="rounded-xl border border-amber-200 bg-amber-50 p-6 shadow-sm"
            aria-labelledby="emp-up-heading"
          >
            <h2
              id="emp-up-heading"
              className="text-lg font-semibold text-amber-950"
            >
              Pasar a Corporativo
            </h2>
            <p className="mt-2 text-sm leading-relaxed text-amber-950/90">
              El plan Corporativo tiene precio y límites a medida. Coordinamos el
              cambio por ventas o soporte.
            </p>
            <Link
              href="/contacto"
              className="mt-4 inline-flex rounded-lg bg-amber-900 px-4 py-2.5 text-sm font-semibold text-white hover:bg-amber-950"
            >
              Hablar con ventas
            </Link>
          </section>
        ) : null}

        {(tier === 'trial' || (!billing.hasActivePaymentPeriod && tier !== 'empresa' && tier !== 'corporativo')) &&
        !showProrationPanel ? (
          <section
            className="rounded-xl border border-blue-200 bg-blue-50 p-6 shadow-sm"
            role="status"
          >
            <h2 className="text-lg font-semibold text-blue-950">
              Activá un plan de pago primero
            </h2>
            <p className="mt-2 text-sm leading-relaxed text-blue-900/90">
              El prorrateo entre planes aplica cuando ya tenés un período pago
              vigente. Si todavía estás en prueba o sin suscripción activa,
              elegí un plan y un medio de pago para comenzar.
            </p>
            <Link
              href="/panel/activar-suscripcion"
              className="mt-4 inline-flex rounded-lg bg-blue-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-blue-800"
            >
              Ir a activar suscripción
            </Link>
          </section>
        ) : null}

        {showProrationPanel && proration ? (
          <section
            className="space-y-6 rounded-xl border border-gray-200 bg-white p-6 shadow-sm"
            aria-labelledby="proration-heading"
          >
            <div>
              <h2
                id="proration-heading"
                className="text-lg font-semibold text-gray-900"
              >
                Confirmación — PyME → Empresa
              </h2>
              <p className="mt-2 text-sm text-gray-600">
                Plan actual:{' '}
                <span className="font-medium text-gray-900">
                  {formatPlanLabel(entitlement?.subscription_plan)}
                </span>
                {tier === 'other' ? (
                  <span className="text-amber-800">
                    {' '}
                    (ajuste estimado como base PyME para el cálculo)
                  </span>
                ) : null}
              </p>
            </div>

            <dl className="grid gap-3 text-sm">
              <div className="flex flex-wrap justify-between gap-2 border-b border-gray-100 pb-3">
                <dt className="text-gray-600">Vencimiento del período pago actual</dt>
                <dd className="font-medium text-gray-900">
                  {expiresRaw ? formatDateTime(expiresRaw) : '—'}
                </dd>
              </div>
              <div className="flex flex-wrap justify-between gap-2 border-b border-gray-100 pb-3">
                <dt className="text-gray-600">Tiempo restante del período</dt>
                <dd className="font-medium text-gray-900">
                  {pctRemainingLabel(proration.fractionRemaining)} del ciclo
                  (aprox.)
                </dd>
              </div>
              <div className="flex flex-wrap justify-between gap-2 border-b border-gray-100 pb-3">
                <dt className="text-gray-600">
                  {pymeCatalog?.name ?? 'PyME'} — mes completo (referencia)
                </dt>
                <dd className="font-medium tabular-nums text-gray-900">
                  {formatInvoiceMoney(pricingPyme!.amount_minor, 'ARS')}
                </dd>
              </div>
              <div className="flex flex-wrap justify-between gap-2 border-b border-gray-100 pb-3">
                <dt className="text-gray-600">
                  {empresaCatalog?.name ?? 'Empresa'} — próximo mes completo
                </dt>
                <dd className="font-medium tabular-nums text-gray-900">
                  {formatInvoiceMoney(pricingEmpresa!.amount_minor, 'ARS')}
                </dd>
              </div>
              <div className="flex flex-wrap justify-between gap-2 border-b border-gray-100 pb-3">
                <dt className="text-gray-600">
                  Valor del tiempo restante — plan actual (referencia)
                </dt>
                <dd className="font-medium tabular-nums text-gray-900">
                  {formatInvoiceMoney(
                    proration.currentPlanRemainingValueMinor,
                    'ARS',
                  )}
                </dd>
              </div>
              <div className="flex flex-wrap justify-between gap-2 border-b border-gray-100 pb-3">
                <dt className="text-gray-600">
                  Valor del tiempo restante — plan nuevo (Empresa)
                </dt>
                <dd className="font-medium tabular-nums text-gray-900">
                  {formatInvoiceMoney(
                    proration.newPlanRemainingValueMinor,
                    'ARS',
                  )}
                </dd>
              </div>
              <div className="flex flex-wrap justify-between gap-2 pt-1">
                <dt className="text-base font-semibold text-gray-900">
                  Diferencia a regularizar ahora (prorrateada)
                </dt>
                <dd className="text-base font-semibold tabular-nums text-blue-800">
                  {formatInvoiceMoney(proration.dueNowMinor, 'ARS')}
                </dd>
              </div>
            </dl>

            <div className="rounded-lg bg-gray-50 px-4 py-3 text-sm text-gray-700">
              <p className="font-medium text-gray-900">
                Después del cambio: próximo período completo
              </p>
              <p className="mt-1">
                Se factura como{' '}
                <span className="font-semibold">
                  {empresaCatalog?.name ?? 'Empresa'}
                </span>{' '}
                al precio de lista mensual vigente al momento del cobro (hoy:{' '}
                {formatInvoiceMoney(pricingEmpresa!.amount_minor, 'ARS')} + IVA
                según corresponda).
              </p>
            </div>

            <p className="text-xs leading-relaxed text-gray-500">{legalNotice}</p>

            <p className="text-sm text-gray-600">
              Confirmá el cambio para cobrar el ajuste prorrateado con la
              tarjeta registrada y aplicar el plan Empresa por el resto del
              período actual. El próximo período se factura al precio mensual
              completo.
            </p>

            {actionError ? (
              <div
                className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800"
                role="alert"
                aria-live="polite"
              >
                {actionError}
              </div>
            ) : null}
            {actionSuccess ? (
              <div
                className="rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800"
                role="status"
              >
                {actionSuccess}
              </div>
            ) : null}

            {paymentRecovery ? (
              <div
                className="space-y-3 rounded-lg border border-amber-300 bg-amber-50/80 px-4 py-4 text-sm text-amber-950"
                role="region"
                aria-labelledby="upgrade-payment-recovery-heading"
              >
                <h3
                  id="upgrade-payment-recovery-heading"
                  className="font-semibold text-amber-950"
                >
                  Cobro del ajuste no procesado (402)
                </h3>
                <p className="leading-relaxed opacity-95">
                  El cobro prorrateado con la tarjeta guardada no se pudo completar.
                  Podés intentar con{' '}
                  <span className="font-medium">otra tarjeta</span> para el mismo
                  importe ({formatInvoiceMoney(paymentRecovery.amountMinor, 'ARS')}
                  ).
                </p>
                {!useMockPayment && !mpPublicKey ? (
                  <p className="text-red-800" role="alert">
                    Falta configurar{' '}
                    <code className="rounded bg-white/80 px-1">
                      NEXT_PUBLIC_MERCADOPAGO_PUBLIC_KEY
                    </code>{' '}
                    en el sitio para cargar tarjetas desde el navegador.
                  </p>
                ) : null}
                {!useMockPayment && mpPublicKey && !payerEmail ? (
                  <p className="text-red-800" role="alert">
                    No encontramos un email en tu cuenta para Mercado Pago.
                    Contactá soporte o actualizá tu perfil.
                  </p>
                ) : null}
                {useMockPayment ? (
                  <button
                    type="button"
                    disabled={submitting}
                    onClick={() =>
                      submitPlanChange('empresa', {
                        use_mock_payment: true,
                        upgradeAmountMinor: proration.dueNowMinor,
                      })
                    }
                    className="inline-flex items-center gap-2 rounded-lg bg-amber-900 px-4 py-2.5 text-sm font-semibold text-white hover:bg-amber-950 disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    {submitting ? (
                      <Loader2 className="h-4 w-4 animate-spin" aria-hidden />
                    ) : null}
                    Reintentar con pago simulado (desarrollo)
                  </button>
                ) : null}
                {!useMockPayment &&
                mpPublicKey &&
                payerEmail &&
                paymentRecovery.amountMinor > 0 ? (
                  <div className="rounded-xl border border-amber-200 bg-white p-4 shadow-sm">
                    <div className="mb-4 flex justify-center border-b border-gray-100 pb-4">
                      <Image
                        src="/brands/MercadoPagoLogo.svg"
                        alt="Mercado Pago"
                        width={156}
                        height={63}
                        className="h-10 w-auto max-w-[min(100%,14rem)]"
                        unoptimized
                      />
                    </div>
                    <CardPayment
                      key={`upgrade-recovery-${paymentRecovery.amountMinor}-${tier}`}
                      initialization={{
                        amount: paymentRecovery.amountMinor / 100,
                        payer: { email: payerEmail },
                      }}
                      locale="es-AR"
                      onSubmit={async (cardData: { token?: string }) => {
                        const token = cardData.token?.trim();
                        if (!token) {
                          setActionError(
                            'No recibimos el token de la tarjeta. Probá de nuevo.',
                          );
                          throw new Error('missing token');
                        }
                        const ok = await submitPlanChange('empresa', {
                          card_token: token,
                          upgradeAmountMinor: proration.dueNowMinor,
                        });
                        if (!ok) {
                          throw new Error(
                            'El cobro no se completó. Revisá la tarjeta o probá otra.',
                          );
                        }
                      }}
                    />
                  </div>
                ) : null}
                <button
                  type="button"
                  className="text-sm font-medium text-amber-900 underline underline-offset-2 hover:text-amber-950"
                  onClick={() => {
                    setPaymentRecovery(null);
                    setActionError(null);
                  }}
                >
                  Ocultar opciones de pago
                </button>
              </div>
            ) : null}

            <div className="flex flex-wrap gap-3">
              <button
                type="button"
                onClick={() =>
                  submitPlanChange('empresa', {
                    upgradeAmountMinor: proration.dueNowMinor,
                  })
                }
                disabled={submitting}
                className="inline-flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-blue-300"
              >
                {submitting ? (
                  <Loader2 className="h-4 w-4 animate-spin" aria-hidden />
                ) : null}
                Confirmar cambio a Empresa
              </button>
              <Link
                href="/panel/facturacion"
                className="inline-flex items-center justify-center rounded-lg border border-gray-300 bg-white px-4 py-2.5 text-sm font-semibold text-gray-700 hover:bg-gray-50"
              >
                Cancelar
              </Link>
            </div>
          </section>
        ) : null}

        {tier === 'empresa' && billing.hasActivePaymentPeriod ? (
          <DowngradePanel
            entitlement={entitlement}
            pricingPyme={pricingPyme}
            pymeResolvedLimits={pymeResolvedLimits}
            submitting={submitting}
            actionError={actionError}
            actionSuccess={actionSuccess}
            onConfirm={() =>
              submitPlanChange('pyme', { confirmDowngrade: true })
            }
          />
        ) : null}

        {billing.hasActivePaymentPeriod &&
        (tier === 'pyme' || tier === 'other') &&
        !showProrationPanel &&
        !pricingError ? (
          <section
            className="rounded-xl border border-amber-200 bg-amber-50 p-6 text-sm text-amber-950"
            role="status"
          >
            <p className="font-medium">No pudimos armar la vista previa</p>
            <p className="mt-2 leading-relaxed opacity-90">
              Falta una fecha de vencimiento del período pago o los importes en
              pesos. Cuando el período esté registrado y la cotización MEP
              disponible, vas a ver acá el desglose.
            </p>
          </section>
        ) : null}

        {pricingError ? (
          <div
            className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800"
            role="alert"
          >
            {pricingError}
          </div>
        ) : null}
      </div>
    </div>
  );
}

function DowngradePanel({
  entitlement,
  pricingPyme,
  pymeResolvedLimits,
  submitting,
  actionError,
  actionSuccess,
  onConfirm,
}: {
  entitlement: Entitlement | null;
  pricingPyme: PlanPricingResponse | null;
  pymeResolvedLimits: {
    warehouses: number;
    users: number;
    documentsMonthly: number;
  };
  submitting: boolean;
  actionError: string | null;
  actionSuccess: string | null;
  onConfirm: () => void;
}) {
  const pendingPlan = entitlement?.pending_plan;
  const warehouseCount = entitlement?.warehouse_count ?? 0;
  const userCount = entitlement?.user_count ?? 0;
  const docsMTD = entitlement?.documents_usage_mtd ?? 0;

  const overItems: string[] = [];
  if (warehouseCount > pymeResolvedLimits.warehouses) {
    overItems.push(
      `Tenés ${warehouseCount} depósitos (PyME permite ${pymeResolvedLimits.warehouses}).`,
    );
  }
  if (userCount > pymeResolvedLimits.users) {
    overItems.push(
      `Tenés ${userCount} usuarios (PyME permite ${pymeResolvedLimits.users}).`,
    );
  }
  if (docsMTD > pymeResolvedLimits.documentsMonthly) {
    overItems.push(
      `Procesaste ${docsMTD} documentos este mes (PyME permite ${pymeResolvedLimits.documentsMonthly}/mes).`,
    );
  }
  const blocked = overItems.length > 0;

  return (
    <section
      className="space-y-4 rounded-xl border border-gray-200 bg-white p-6 shadow-sm"
      aria-labelledby="downgrade-heading"
    >
      <div>
        <h2
          id="downgrade-heading"
          className="text-lg font-semibold text-gray-900"
        >
          Bajar a PyME
        </h2>
        <p className="mt-2 text-sm text-gray-600">
          Si te alcanza con menos volumen, podés programar el cambio para que se
          aplique en el próximo período. No reembolsamos lo ya cobrado del
          período actual; vas a seguir usando Empresa hasta el vencimiento.
        </p>
      </div>

      {pendingPlan ? (
        <div
          className="rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-900"
          role="status"
        >
          Ya programaste un cambio a{' '}
          <span className="font-semibold">{pendingPlan.toUpperCase()}</span>{' '}
          para el próximo período.
        </div>
      ) : null}

      <ul className="grid gap-2 text-sm">
        <li className="flex items-center justify-between gap-3 rounded-lg bg-gray-50 px-3 py-2">
          <span className="text-gray-700">Depósitos activos</span>
          <span
            className={`font-medium tabular-nums ${
              warehouseCount > pymeResolvedLimits.warehouses
                ? 'text-red-700'
                : 'text-gray-900'
            }`}
          >
            {warehouseCount} / {pymeResolvedLimits.warehouses}
          </span>
        </li>
        <li className="flex items-center justify-between gap-3 rounded-lg bg-gray-50 px-3 py-2">
          <span className="text-gray-700">Usuarios</span>
          <span
            className={`font-medium tabular-nums ${
              userCount > pymeResolvedLimits.users
                ? 'text-red-700'
                : 'text-gray-900'
            }`}
          >
            {userCount} / {pymeResolvedLimits.users}
          </span>
        </li>
        <li className="flex items-center justify-between gap-3 rounded-lg bg-gray-50 px-3 py-2">
          <span className="text-gray-700">Documentos este mes</span>
          <span
            className={`font-medium tabular-nums ${
              docsMTD > pymeResolvedLimits.documentsMonthly
                ? 'text-red-700'
                : 'text-gray-900'
            }`}
          >
            {docsMTD} / {pymeResolvedLimits.documentsMonthly}
          </span>
        </li>
      </ul>

      {blocked ? (
        <div
          className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900"
          role="status"
        >
          <p className="font-medium">Antes de bajar de plan, ajustá:</p>
          <ul className="mt-1 list-inside list-disc space-y-1">
            {overItems.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </div>
      ) : null}

      {pricingPyme ? (
        <p className="text-sm text-gray-600">
          A partir del próximo período se factura como{' '}
          <span className="font-semibold">PyME</span> al precio mensual vigente
          (hoy: PyME mensual completo).
        </p>
      ) : null}

      {actionError ? (
        <div
          className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800"
          role="alert"
        >
          {actionError}
        </div>
      ) : null}
      {actionSuccess ? (
        <div
          className="rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800"
          role="status"
        >
          {actionSuccess}
        </div>
      ) : null}

      <div className="flex flex-wrap gap-3">
        <button
          type="button"
          onClick={onConfirm}
          disabled={submitting || blocked || Boolean(pendingPlan)}
          className="inline-flex items-center gap-2 rounded-lg bg-gray-900 px-4 py-2.5 text-sm font-semibold text-white hover:bg-black disabled:cursor-not-allowed disabled:bg-gray-300"
        >
          {submitting ? (
            <Loader2 className="h-4 w-4 animate-spin" aria-hidden />
          ) : null}
          Programar para el próximo período
        </button>
        <Link
          href="/panel/facturacion"
          className="inline-flex items-center justify-center rounded-lg border border-gray-300 bg-white px-4 py-2.5 text-sm font-semibold text-gray-700 hover:bg-gray-50"
        >
          Cancelar
        </Link>
      </div>
    </section>
  );
}
