'use client';

import { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import {
  canManageBillingSubscriptions,
  clearWebSession,
  fetchWithWebAuth,
  type WebProfile,
} from '../lib/webAuth';
import {
  BillingComprobantesSkeleton,
  BillingMainSkeleton,
} from './components/PanelSkeletons';
import { PanelKeyValueList, PanelStatTile } from './components/PanelShared';
import PaymentMethodSection from './facturacion/PaymentMethodSection';
import { needsActivateSubscription } from './lib/activateSubscriptionGate';
import {
  deriveBillingPresentation,
  formatDateTime,
  formatPlanLabel,
} from './lib/billingPresentation';
import type { BillingInvoiceRow, Entitlement } from './lib/entitlementTypes';
import { BILLING_LEGAL_NOTICE_AR } from '../lib/billingLegalNotice';
import { getPlanById } from '../lib/planCatalog';
import {
  formatInvoiceDate,
  formatInvoiceMoney,
  invoiceStatusLabel,
} from './lib/invoiceFormat';
import {
  resolveUsageUpgradeAction,
  subscriptionTier,
} from './lib/selfServePlan';
import { useBillingClockMs } from './lib/useBillingClockMs';
import { usePanelBootstrap } from './lib/usePanelBootstrap';
import { useRouterRef } from './lib/useRouterRef';

export default function BillingPageClient() {
  const routerRef = useRouterRef();
  const now = useBillingClockMs();
  const { status, profile, errorMessage: configError } = usePanelBootstrap();
  const [entitlementLoading, setEntitlementLoading] = useState(true);
  const [invoicesLoading, setInvoicesLoading] = useState(true);
  const [entitlement, setEntitlement] = useState<Entitlement | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [invoices, setInvoices] = useState<BillingInvoiceRow[]>([]);
  const [invoicesError, setInvoicesError] = useState<string | null>(null);
  const [nextBillingEstimateMinor, setNextBillingEstimateMinor] = useState<number | null>(null);
  const [nextBillingEstimateCurrency, setNextBillingEstimateCurrency] = useState<string>('ARS');

  const downloadFacturaPdf = useCallback(
    async (invoiceId: string) => {
      try {
        const res = await fetchWithWebAuth(
          `/auth/me/invoices/${invoiceId}/factura.pdf`,
        );
        if (res.status === 401) {
          clearWebSession();
          routerRef.current.replace('/ingresar');
          return;
        }
        if (!res.ok) {
          window.alert(
            'El PDF todavía no está disponible o hubo un error al descargarlo.',
          );
          return;
        }
        const blob = await res.blob();
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `remitos-factura-${invoiceId}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
      } catch {
        window.alert('Error al descargar el PDF.');
      }
    },
    [routerRef],
  );

  useEffect(() => {
    if (status === 'config_error') {
      queueMicrotask(() => {
        setError(configError);
        setEntitlementLoading(false);
        setInvoicesLoading(false);
      });
      return;
    }
    if (status !== 'ready' || !profile) {
      return;
    }

    let cancelled = false;

    async function load() {
      setEntitlementLoading(true);
      setInvoicesLoading(true);
      setError(null);

      const [entRes, invRes] = await Promise.all([
        fetchWithWebAuth('/auth/me/entitlement'),
        fetchWithWebAuth('/auth/me/invoices'),
      ]);
      if (cancelled) return;

      if (entRes.status === 401 || invRes.status === 401) {
        clearWebSession();
        routerRef.current.replace('/ingresar');
        return;
      }

      if (!entRes.ok) {
        const body = (await entRes.json().catch(() => ({}))) as {
          message?: string;
        };
        setError(
          body.message ||
            'No se pudieron obtener los datos de tu cuenta. Probá de nuevo más tarde.',
        );
        setEntitlement(null);
      } else {
        const data = (await entRes.json()) as Entitlement;
        setEntitlement(data);

        const normalizedPlan = (data.subscription_plan ?? '')
          .toLowerCase()
          .trim();
        const expiresAtMs = data.subscription_expires_at
          ? Date.parse(data.subscription_expires_at)
          : Number.NaN;
        const inThreeDayWindow =
          Number.isFinite(expiresAtMs) &&
          expiresAtMs > Date.now() &&
          expiresAtMs - Date.now() <= 3 * 24 * 60 * 60 * 1000;
        const supportsPricingPreview =
          normalizedPlan === 'pyme' || normalizedPlan === 'empresa';

        void (async () => {
          if (inThreeDayWindow && supportsPricingPreview) {
            const pricingRes = await fetchWithWebAuth(
              `/auth/me/plan-pricing?plan_id=${normalizedPlan}`,
            );
            if (cancelled) return;
            if (pricingRes.ok) {
              const pricing = (await pricingRes.json()) as {
                amount_minor?: number;
                currency?: string;
              };
              if (
                typeof pricing.amount_minor === 'number' &&
                Number.isFinite(pricing.amount_minor)
              ) {
                setNextBillingEstimateMinor(pricing.amount_minor);
                setNextBillingEstimateCurrency(
                  (pricing.currency ?? 'ARS').toUpperCase(),
                );
              } else {
                setNextBillingEstimateMinor(null);
              }
            } else {
              setNextBillingEstimateMinor(null);
            }
          } else {
            if (!cancelled) setNextBillingEstimateMinor(null);
          }
        })();
      }
      setEntitlementLoading(false);

      if (!invRes.ok) {
        setInvoicesError(
          'No se pudieron cargar los comprobantes. Probá de nuevo más tarde.',
        );
        setInvoices([]);
      } else {
        const raw = (await invRes.json()) as unknown;
        setInvoicesError(null);
        setInvoices(Array.isArray(raw) ? (raw as BillingInvoiceRow[]) : []);
      }
      setInvoicesLoading(false);
    }

    void load();
    return () => {
      cancelled = true;
    };
  }, [status, configError, profile, routerRef]);

  const billing = entitlement
    ? deriveBillingPresentation(entitlement, now)
    : null;
  const planCatalogEntry = getPlanById(entitlement?.subscription_plan);
  const currentPlanName = planCatalogEntry?.name ?? formatPlanLabel(entitlement?.subscription_plan);
  const currentPlanPrice = planCatalogEntry?.monthlyPriceLabel ?? 'A definir';
  const currentPlanIncludes = planCatalogEntry?.perks ?? ['Sin detalle disponible'];
  const currentPlanOverage = planCatalogEntry?.overageLabel ?? 'A confirmar';
  const docsUsed = entitlement?.documents_usage_mtd ?? 0;
  const docsLimit = entitlement?.documents_monthly_limit;
  const docsRemaining =
    typeof docsLimit === 'number' ? Math.max(docsLimit - docsUsed, 0) : null;
  const utilizationPct =
    typeof docsLimit === 'number' && docsLimit > 0
      ? Math.min((docsUsed / docsLimit) * 100, 100)
      : null;
  const nowDate = new Date();
  const dayOfMonth = nowDate.getDate();
  const daysInMonth = new Date(
    nowDate.getFullYear(),
    nowDate.getMonth() + 1,
    0,
  ).getDate();
  const projectedMonthEndDocs =
    dayOfMonth > 0 ? Math.round((docsUsed / dayOfMonth) * daysInMonth) : docsUsed;
  const projectedOverage =
    typeof docsLimit === 'number'
      ? Math.max(projectedMonthEndDocs - docsLimit, 0)
      : null;

  const subscriptionTierId = subscriptionTier(
    entitlement?.subscription_plan,
    entitlement?.documents_monthly_limit,
  );
  const usageUpgrade = resolveUsageUpgradeAction(
    projectedOverage,
    subscriptionTierId,
    billing?.hasActivePaymentPeriod ?? false,
  );
  const paymentEndMs = entitlement?.subscription_expires_at
    ? Date.parse(entitlement.subscription_expires_at)
    : Number.NaN;
  const showNextBillingEstimate =
    billing?.hasActivePaymentPeriod &&
    Number.isFinite(paymentEndMs) &&
    paymentEndMs > now &&
    paymentEndMs - now <= 3 * 24 * 60 * 60 * 1000 &&
    nextBillingEstimateMinor !== null;

  const summaryItems =
    entitlement && billing
      ? [
          {
            label: 'Plan',
            value: formatPlanLabel(entitlement.subscription_plan),
          },
          {
            label: 'Estado de facturación',
            value: billing.billingStatusSummary,
          },
          {
            label: 'Próximo hito',
            value: billing.nextBillingMilestone,
          },
          ...(showNextBillingEstimate
            ? [
                {
                  label: 'Próximo cobro estimado',
                  value: formatInvoiceMoney(
                    nextBillingEstimateMinor,
                    nextBillingEstimateCurrency,
                  ),
                },
              ]
            : []),
          {
            label: 'Estado pago (acceso a app)',
            value: billing.isPaid ? 'Sí' : 'No',
          },
          {
            label: 'Período pago',
            value: billing.hasActivePaymentPeriod
              ? entitlement.subscription_expires_at
                ? `Hasta ${formatDateTime(entitlement.subscription_expires_at)}`
                : 'Activo (sin vencimiento)'
              : 'No activo',
          },
          {
            label: 'En prueba',
            value: billing.hasActiveTrial ? 'Sí' : 'No',
          },
          {
            label: 'Período de prueba',
            value: billing.hasActiveTrial
              ? `Hasta ${formatDateTime(entitlement?.trial_ends_at)}`
              : 'No activo',
          },
        ]
      : [];

  return (
    <div className="bg-gray-50 px-4 pb-12 pt-6">
      <div className="mx-auto max-w-[92rem] space-y-8">
        <header className="space-y-2">
          <h1 className="text-3xl font-bold tracking-tight text-gray-900">
            Facturación
          </h1>
          <p className="text-base leading-relaxed text-gray-600">
            Plan, estado de suscripción y comprobantes de pago de tu empresa.
          </p>
          <p className="mt-3 hidden text-sm leading-relaxed text-gray-500 md:block">
            {BILLING_LEGAL_NOTICE_AR}
          </p>
        </header>

        {error && (
          <div
            className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800"
            role="alert"
          >
            {error}
          </div>
        )}

        {entitlementLoading && !error ? <BillingMainSkeleton /> : null}

        {entitlement && billing?.isArchived ? (
          <div
            className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900"
            role="status"
          >
            Esta empresa está archivada. La descarga de la app y el acceso
            completo pueden no estar disponibles.
          </div>
        ) : null}

        {entitlement &&
        billing &&
        !billing.isArchived &&
        billing.companyBillingInactive ? (
          <div
            className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900"
            role="status"
          >
            El estado de la empresa no es &quot;activo&quot;; revisá la cuenta o
            contactá soporte si necesitás reactivarla.
          </div>
        ) : null}

        {entitlement &&
        billing &&
        !billing.isArchived &&
        !billing.companyBillingInactive &&
        needsActivateSubscription(entitlement) ? (
          <div
            className="rounded-lg border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-900"
            role="status"
          >
            <p className="font-medium">Activá tu suscripción para seguir usando la app</p>
            <p className="mt-1 text-blue-800">
              La prueba terminó o el período pago venció. Cargá un medio de pago y
              elegí un plan.
            </p>
            <Link
              href="/panel/activar-suscripcion"
              className="mt-2 inline-block font-semibold text-blue-800 underline"
            >
              Ir a activar suscripción
            </Link>
          </div>
        ) : null}

        {entitlement?.pending_plan &&
        entitlement.pending_plan.trim() !== '' &&
        billing &&
        billing.hasActivePaymentPeriod &&
        !billing.isArchived &&
        !billing.companyBillingInactive ? (
          <div
            className="rounded-xl border border-indigo-200 bg-indigo-50 px-4 py-4 text-sm text-indigo-950 shadow-sm"
            role="status"
          >
            <p className="font-semibold">Cambio de plan programado</p>
            <p className="mt-2 leading-relaxed opacity-95">
              Pasarás al plan{' '}
              <span className="font-semibold uppercase">
                {entitlement.pending_plan.trim()}
              </span>{' '}
              al comenzar el próximo período de facturación
              {entitlement.subscription_expires_at ? (
                <>
                  {' '}
                  (después del{' '}
                  <span className="font-medium">
                    {formatDateTime(entitlement.subscription_expires_at)}
                  </span>
                  ).
                </>
              ) : (
                '.'
              )}{' '}
              Seguís con el plan actual hasta esa fecha.
            </p>
            <Link
              href="/panel/facturacion/mejorar-plan"
              className="hidden md:inline-block font-semibold text-indigo-900 underline"
            >
              Ver o modificar en Cambiar de plan
            </Link>
          </div>
        ) : null}

        {entitlement &&
        billing &&
        usageUpgrade.type === 'href' &&
        !billing.isArchived &&
        !billing.companyBillingInactive ? (
          <div
            className="rounded-xl border border-amber-200 bg-amber-50 p-5 shadow-sm"
            role="region"
            aria-labelledby="usage-upgrade-heading"
          >
            <h2
              id="usage-upgrade-heading"
              className="text-base font-semibold text-amber-950"
            >
              Tu proyección supera el cupo del plan
            </h2>
            <p className="mt-2 text-sm leading-relaxed text-amber-950/90">
              Al ritmo actual, estimamos{' '}
              <span className="font-semibold tabular-nums">
                {projectedMonthEndDocs.toLocaleString('es-AR')}
              </span>{' '}
              documentos este mes
              {typeof docsLimit === 'number'
                ? ` (cupo: ${docsLimit.toLocaleString('es-AR')})`
                : ''}
              . Pasar al siguiente plan aumenta el límite mensual y puede
              reducir el costo por excedentes.
            </p>
            <Link
              href={usageUpgrade.href}
              className="hidden md:inline-flex rounded-lg bg-amber-900 px-4 py-2.5 text-sm font-semibold text-white hover:bg-amber-950"
            >
              {usageUpgrade.label}
            </Link>
          </div>
        ) : null}

        {entitlement && billing ? (
          <section
            className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm"
            aria-labelledby="billing-heading"
          >
            <h2
              id="billing-heading"
              className="text-lg font-semibold text-gray-900"
            >
              Resumen de facturación
            </h2>
            <p className="mt-2 text-sm text-gray-600">
              Estado del plan y próximos hitos. Los comprobantes de pago aparecen
              abajo cuando estén disponibles.
            </p>

            <PanelKeyValueList items={summaryItems} className="mt-6" />
            {showNextBillingEstimate ? (
              <p className="mt-4 text-xs leading-relaxed text-gray-500">
                Este importe es una estimación calculada 3 días antes del vencimiento usando la
                cotización vigente. Al vencer, el sistema intenta cobrar automáticamente mediante
                Mercado Pago.
              </p>
            ) : null}

            <div className="mt-8 hidden border-t border-gray-100 pt-6 md:block">
              <h3 className="text-sm font-semibold text-gray-900">
                Tu plan actual
              </h3>
              <div className="mt-4 grid gap-4 lg:grid-cols-3">
                <PanelStatTile
                  label="Plan"
                  value={currentPlanName}
                  description={`${currentPlanPrice} / mes + IVA`}
                  tone="muted"
                  className="hidden md:block lg:col-span-1"
                >
                  <p className="mt-2 text-xs text-gray-500">
                    Excedentes: {currentPlanOverage}
                  </p>
                  <Link
                    href="/panel/facturacion/mejorar-plan"
                    className="mt-4 hidden md:inline-flex items-center rounded-lg bg-blue-600 px-3 py-2 text-sm font-semibold text-white transition-colors hover:bg-blue-700"
                  >
                    Mejorar plan
                  </Link>
                </PanelStatTile>

                <PanelStatTile
                  label="Uso actual"
                  value={docsUsed.toLocaleString('es-AR')}
                  description="documentos en el mes"
                  className="lg:col-span-1"
                >
                  <p className="mt-2 text-xs text-gray-500">
                    {typeof docsLimit === 'number'
                      ? `${docsRemaining?.toLocaleString('es-AR')} restantes de ${docsLimit.toLocaleString('es-AR')}`
                      : 'Sin límite mensual configurado'}
                  </p>
                  {typeof utilizationPct === 'number' && (
                    <div className="mt-3 h-2 w-full overflow-hidden rounded-full bg-gray-200">
                      <div
                        className="h-full bg-blue-600"
                        style={{ width: `${utilizationPct}%` }}
                      />
                    </div>
                  )}
                </PanelStatTile>

                <PanelStatTile
                  label="Proyección"
                  value={projectedMonthEndDocs.toLocaleString('es-AR')}
                  description="documentos estimados al cierre"
                  className="hidden md:block lg:col-span-1"
                >
                  <p className="mt-2 text-xs text-gray-500">
                    {projectedOverage && projectedOverage > 0
                      ? `Excedente estimado: ${projectedOverage.toLocaleString('es-AR')} documentos`
                      : 'Sin excedente estimado al ritmo actual'}
                  </p>
                </PanelStatTile>
              </div>
              <div className="mt-4 hidden rounded-lg border border-gray-100 bg-gray-50 p-4 md:block">
                <p className="text-xs font-semibold uppercase tracking-wide text-gray-500">
                  Incluye
                </p>
                <ul className="mt-2 grid gap-2 text-sm text-gray-700 sm:grid-cols-2">
                  {currentPlanIncludes.map((item) => (
                    <li key={item} className="rounded bg-white px-3 py-2">
                      {item}
                    </li>
                  ))}
                </ul>
              </div>
            </div>

            <div className="mt-8 border-t border-gray-100 pt-6">
              <h3 className="text-sm font-semibold text-gray-900">
                Comprobantes de pago
              </h3>
              <p className="mt-1 text-sm text-gray-600">
                Facturas y comprobantes registrados para tu empresa.
              </p>
              {invoicesError ? (
                <p
                  className="mt-3 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900"
                  role="alert"
                >
                  {invoicesError}
                </p>
              ) : null}
              {invoicesLoading && !invoicesError ? (
                <BillingComprobantesSkeleton />
              ) : null}
              {!invoicesLoading &&
              !invoicesError &&
              invoices.length === 0 ? (
                <p className="mt-4 rounded-lg border border-dashed border-gray-200 bg-gray-50 px-4 py-6 text-center text-sm text-gray-600">
                  Todavía no hay comprobantes para mostrar. Cuando se registren
                  pagos, van a aparecer acá.
                </p>
              ) : null}
              {!invoicesLoading &&
              !invoicesError &&
              invoices.length > 0 ? (
                <>
                  <div className="mt-4 space-y-2 md:hidden">
                    {invoices.map((inv) => (
                      <article
                        key={inv.id}
                        className="rounded-lg border border-gray-200 bg-white px-3 py-2.5"
                      >
                        <div className="flex items-center justify-between gap-3">
                          <p className="text-xs font-medium text-gray-500">
                            {formatInvoiceDate(inv.issued_at)}
                          </p>
                          <p className="text-sm font-semibold tabular-nums text-gray-900">
                            {formatInvoiceMoney(inv.amount_minor, inv.currency)}
                          </p>
                        </div>
                        <p className="mt-1 text-sm text-gray-800">
                          {inv.description?.trim() ? inv.description : '—'}
                        </p>
                        <p className="mt-1 text-xs font-medium text-gray-600">
                          {invoiceStatusLabel(inv.status)}
                        </p>
                        {inv.factura_pending ? (
                          <p className="mt-1 text-xs font-medium text-amber-800">
                            Factura AFIP: pendiente CAE
                          </p>
                        ) : null}
                        {inv.factura_cae ? (
                          <p className="mt-1 text-xs text-gray-600">
                            CAE {inv.factura_cae}
                          </p>
                        ) : null}
                        {inv.factura_cae && inv.factura_emitted_at ? (
                          <button
                            type="button"
                            className="mt-1 text-xs font-medium text-blue-600 underline"
                            onClick={() => void downloadFacturaPdf(inv.id)}
                          >
                            Descargar PDF
                          </button>
                        ) : null}
                      </article>
                    ))}
                  </div>
                  <div className="mt-4 hidden overflow-x-auto rounded-lg border border-gray-200 md:block">
                  <table className="w-full min-w-[36rem] text-left text-sm">
                    <thead className="border-b border-gray-200 bg-gray-50">
                      <tr>
                        <th scope="col" className="px-4 py-3 font-semibold text-gray-700">
                          Fecha
                        </th>
                        <th scope="col" className="px-4 py-3 font-semibold text-gray-700">
                          Importe
                        </th>
                        <th scope="col" className="px-4 py-3 font-semibold text-gray-700">
                          Estado
                        </th>
                        <th scope="col" className="px-4 py-3 font-semibold text-gray-700">
                          Concepto
                        </th>
                        <th scope="col" className="px-4 py-3 font-semibold text-gray-700">
                          Factura AFIP
                        </th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-100 bg-white">
                      {invoices.map((inv) => (
                        <tr key={inv.id}>
                          <td className="whitespace-nowrap px-4 py-3 text-gray-900">
                            {formatInvoiceDate(inv.issued_at)}
                          </td>
                          <td className="whitespace-nowrap px-4 py-3 tabular-nums text-gray-900">
                            {formatInvoiceMoney(inv.amount_minor, inv.currency)}
                          </td>
                          <td className="whitespace-nowrap px-4 py-3 text-gray-900">
                            {invoiceStatusLabel(inv.status)}
                          </td>
                          <td className="max-w-[14rem] px-4 py-3 text-gray-700">
                            {inv.description?.trim()
                              ? inv.description
                              : '—'}
                          </td>
                          <td className="whitespace-nowrap px-4 py-3 text-gray-900">
                            {inv.factura_pending ? (
                              <span className="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-900">
                                Pendiente CAE
                              </span>
                            ) : null}
                            {inv.factura_cae ? (
                              <span className="block text-xs text-gray-600">
                                CAE {inv.factura_cae}
                              </span>
                            ) : null}
                            {inv.factura_cae && inv.factura_emitted_at ? (
                              <button
                                type="button"
                                className="mt-1 text-xs font-medium text-blue-600 underline"
                                onClick={() => void downloadFacturaPdf(inv.id)}
                              >
                                Descargar PDF
                              </button>
                            ) : null}
                            {!inv.factura_pending &&
                            !inv.factura_cae &&
                            inv.status === 'paid' ? (
                              <span className="text-xs text-gray-500">—</span>
                            ) : null}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                </>
              ) : null}
            </div>

            <div className="mt-8 pt-6 md:border-t md:border-gray-100">
              <PaymentMethodSection
                canManage={
                  profile ? canManageBillingSubscriptions(profile.role) : false
                }
                payerEmail={
                  profile?.email?.trim() || profile?.username?.trim() || ''
                }
              />
            </div>
          </section>
        ) : null}
      </div>
    </div>
  );
}
