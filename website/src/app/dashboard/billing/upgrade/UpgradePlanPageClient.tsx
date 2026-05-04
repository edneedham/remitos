'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Loader2 } from 'lucide-react';
import { getApiBaseUrl } from '../../../lib/apiUrl';
import type { PlanPricingResponse } from '../../../lib/planPricing';
import { PLAN_CATALOG } from '../../../lib/planCatalog';
import {
  canAccessWebManagement,
  clearWebSession,
  fetchProfile,
  fetchWithWebAuth,
  hasWebSession,
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
import { formatInvoiceMoney } from '../../lib/invoiceFormat';
import { BILLING_LEGAL_NOTICE_AR } from '../../../lib/billingLegalNotice';

function pctRemainingLabel(fraction: number): string {
  return `${Math.round(fraction * 100)} %`;
}

export default function UpgradePlanPageClient() {
  const router = useRouter();
  const [ready, setReady] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [entitlement, setEntitlement] = useState<Entitlement | null>(null);
  const [pricingPyme, setPricingPyme] = useState<PlanPricingResponse | null>(
    null,
  );
  const [pricingEmpresa, setPricingEmpresa] =
    useState<PlanPricingResponse | null>(null);
  const [pricingError, setPricingError] = useState<string | null>(null);

  useEffect(() => {
    if (!hasWebSession()) {
      router.replace('/login');
      return;
    }

    let cancelled = false;

    async function load() {
      const api = getApiBaseUrl();
      if (!api) {
        setError(
          'Falta configurar NEXT_PUBLIC_API_URL (URL del servidor de la API).',
        );
        setReady(true);
        return;
      }

      await refreshWebSession();

      const profile = await fetchProfile();
      if (cancelled) return;
      if (!profile || !canAccessWebManagement(profile.role)) {
        clearWebSession();
        router.replace('/login');
        return;
      }

      const res = await fetchWithWebAuth('/auth/me/entitlement');
      if (cancelled) return;
      if (res.status === 401) {
        clearWebSession();
        router.replace('/login');
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
        setReady(true);
        return;
      }

      const data = (await res.json()) as Entitlement;
      setEntitlement(data);
      setReady(true);
    }

    void load();
    return () => {
      cancelled = true;
    };
  }, [router]);

  useEffect(() => {
    if (!ready || !entitlement) return;

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
  }, [ready, entitlement]);

  if (!ready && !error) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center bg-gray-50">
        <Loader2 className="h-8 w-8 animate-spin text-blue-600" aria-hidden />
      </div>
    );
  }

  if (error) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-12">
        <div
          className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800"
          role="alert"
        >
          {error}
        </div>
        <p className="mt-6">
          <Link
            href="/dashboard/billing"
            className="text-sm font-semibold text-blue-700 underline"
          >
            Volver a Facturación
          </Link>
        </p>
      </div>
    );
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

  return (
    <div className="bg-gray-50 px-4 pb-14 pt-8">
      <div className="mx-auto max-w-2xl space-y-8">
        <header className="space-y-2">
          <p>
            <Link
              href="/dashboard/billing"
              className="text-sm font-semibold text-blue-700 underline"
            >
              ← Facturación
            </Link>
          </p>
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
              href="/contact"
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
              href="/contact"
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
              href="/dashboard/activate-subscription"
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
              El cobro del ajuste prorrateado con tarjeta u otro medio registrado
              se habilitará cuando el cambio de plan esté disponible
              completamente en línea. Mientras tanto, podés solicitar el cambio y
              validar estos importes con el equipo.
            </p>

            <div className="flex flex-wrap gap-3">
              <Link
                href="/contact"
                className="inline-flex rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-blue-700"
              >
                Solicitar cambio a Empresa
              </Link>
            </div>
          </section>
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
