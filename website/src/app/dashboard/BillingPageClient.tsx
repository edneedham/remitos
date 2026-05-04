'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Loader2 } from 'lucide-react';
import Link from 'next/link';
import { getApiBaseUrl } from '../lib/apiUrl';
import {
  canAccessWebManagement,
  clearWebSession,
  fetchProfile,
  fetchWithWebAuth,
  hasWebSession,
  refreshWebSession,
} from '../lib/webAuth';
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

export default function BillingPageClient() {
  const router = useRouter();
  const [ready, setReady] = useState(false);
  const [entitlement, setEntitlement] = useState<Entitlement | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [invoices, setInvoices] = useState<BillingInvoiceRow[]>([]);
  const [invoicesError, setInvoicesError] = useState<string | null>(null);

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

      const invRes = await fetchWithWebAuth('/auth/me/invoices');
      if (cancelled) return;
      if (invRes.status === 401) {
        clearWebSession();
        router.replace('/login');
        return;
      }
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

      setReady(true);
    }

    void load();
    return () => {
      cancelled = true;
    };
  }, [router]);

  if (!ready && !error) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center bg-gray-50">
        <Loader2 className="h-8 w-8 animate-spin text-blue-600" aria-hidden />
      </div>
    );
  }

  const now = Date.now();
  const billing = deriveBillingPresentation(entitlement, now);
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
    billing.hasActivePaymentPeriod,
  );

  return (
    <div className="bg-gray-50 px-4 pb-12 pt-6">
      <div className="mx-auto max-w-7xl space-y-8">
        <header className="mx-auto max-w-2xl space-y-2 xl:mx-0">
          <h1 className="text-3xl font-bold tracking-tight text-gray-900">
            Facturación
          </h1>
          <p className="text-base leading-relaxed text-gray-600">
            Plan, estado de suscripción y comprobantes de pago de tu empresa.
          </p>
          <p className="mt-3 text-sm leading-relaxed text-gray-500">
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

        {entitlement && billing.isArchived ? (
          <div
            className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900"
            role="status"
          >
            Esta empresa está archivada. La descarga de la app y el acceso
            completo pueden no estar disponibles.
          </div>
        ) : null}

        {entitlement && !billing.isArchived && billing.companyBillingInactive ? (
          <div
            className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900"
            role="status"
          >
            El estado de la empresa no es &quot;activo&quot;; revisá la cuenta o
            contactá soporte si necesitás reactivarla.
          </div>
        ) : null}

        {entitlement &&
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
              href="/dashboard/activate-subscription"
              className="mt-2 inline-block font-semibold text-blue-800 underline"
            >
              Ir a activar suscripción
            </Link>
          </div>
        ) : null}

        {entitlement &&
        usageUpgrade.type === 'href' &&
        !billing.isArchived &&
        !billing.companyBillingInactive ? (
          <div
            className="mx-auto max-w-2xl rounded-xl border border-amber-200 bg-amber-50 p-5 shadow-sm xl:mx-0 xl:max-w-none"
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
              className="mt-4 inline-flex rounded-lg bg-amber-900 px-4 py-2.5 text-sm font-semibold text-white hover:bg-amber-950"
            >
              {usageUpgrade.label}
            </Link>
          </div>
        ) : null}

        {entitlement ? (
          <section
            className="mx-auto max-w-2xl rounded-xl border border-gray-200 bg-white p-6 shadow-sm xl:mx-0 xl:max-w-none"
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

            <dl className="mt-6 space-y-3 text-sm">
              <div className="flex flex-wrap items-center justify-between gap-4">
                <dt className="text-gray-600">Plan</dt>
                <dd className="text-right font-medium text-gray-900">
                  {formatPlanLabel(entitlement.subscription_plan)}
                </dd>
              </div>
              <div className="flex flex-wrap items-center justify-between gap-4">
                <dt className="text-gray-600">Estado de facturación</dt>
                <dd className="text-right font-medium text-gray-900">
                  {billing.billingStatusSummary}
                </dd>
              </div>
              <div className="flex flex-wrap items-center justify-between gap-4">
                <dt className="text-gray-600">Próximo hito</dt>
                <dd className="max-w-[min(100%,20rem)] text-right font-medium text-gray-900">
                  {billing.nextBillingMilestone}
                </dd>
              </div>
              <div className="flex flex-wrap items-center justify-between gap-4">
                <dt className="text-gray-600">Estado pago (acceso a app)</dt>
                <dd className="font-medium text-gray-900">
                  {billing.isPaid ? 'Sí' : 'No'}
                </dd>
              </div>
              <div className="flex flex-wrap items-center justify-between gap-4">
                <dt className="text-gray-600">Período pago</dt>
                <dd className="max-w-[min(100%,20rem)] text-right font-medium text-gray-900">
                  {billing.hasActivePaymentPeriod
                    ? entitlement.subscription_expires_at
                      ? `Hasta ${formatDateTime(entitlement.subscription_expires_at)}`
                      : 'Activo (sin vencimiento)'
                    : 'No activo'}
                </dd>
              </div>
              <div className="flex flex-wrap items-center justify-between gap-4">
                <dt className="text-gray-600">En prueba</dt>
                <dd className="font-medium text-gray-900">
                  {billing.hasActiveTrial ? 'Sí' : 'No'}
                </dd>
              </div>
              <div className="flex flex-wrap items-center justify-between gap-4">
                <dt className="text-gray-600">Período de prueba</dt>
                <dd className="max-w-[min(100%,20rem)] text-right font-medium text-gray-900">
                  {billing.hasActiveTrial
                    ? `Hasta ${formatDateTime(entitlement?.trial_ends_at)}`
                    : 'No activo'}
                </dd>
              </div>
            </dl>

            <div className="mt-8 border-t border-gray-100 pt-6">
              <h3 className="text-sm font-semibold text-gray-900">
                Tu plan actual
              </h3>
              <div className="mt-4 grid gap-4 lg:grid-cols-3">
                <article className="rounded-lg border border-gray-200 bg-gray-50 p-4 lg:col-span-1">
                  <p className="text-xs font-semibold uppercase tracking-wide text-gray-500">
                    Plan
                  </p>
                  <p className="mt-2 text-lg font-semibold text-gray-900">
                    {currentPlanName}
                  </p>
                  <p className="mt-1 text-sm text-gray-600">
                    {currentPlanPrice} / mes + IVA
                  </p>
                  <p className="mt-2 text-xs text-gray-500">
                    Excedentes: {currentPlanOverage}
                  </p>
                  <Link
                    href="/dashboard/billing/upgrade"
                    className="mt-4 inline-flex items-center rounded-lg bg-blue-600 px-3 py-2 text-sm font-semibold text-white transition-colors hover:bg-blue-700"
                  >
                    Mejorar plan
                  </Link>
                </article>

                <article className="rounded-lg border border-gray-200 bg-white p-4 lg:col-span-1">
                  <p className="text-xs font-semibold uppercase tracking-wide text-gray-500">
                    Uso actual
                  </p>
                  <p className="mt-2 text-2xl font-bold text-gray-900">
                    {docsUsed.toLocaleString('es-AR')}
                  </p>
                  <p className="mt-1 text-sm text-gray-600">
                    documentos en el mes
                  </p>
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
                </article>

                <article className="rounded-lg border border-gray-200 bg-white p-4 lg:col-span-1">
                  <p className="text-xs font-semibold uppercase tracking-wide text-gray-500">
                    Proyección
                  </p>
                  <p className="mt-2 text-2xl font-bold text-gray-900">
                    {projectedMonthEndDocs.toLocaleString('es-AR')}
                  </p>
                  <p className="mt-1 text-sm text-gray-600">
                    documentos estimados al cierre
                  </p>
                  <p className="mt-2 text-xs text-gray-500">
                    {projectedOverage && projectedOverage > 0
                      ? `Excedente estimado: ${projectedOverage.toLocaleString('es-AR')} documentos`
                      : 'Sin excedente estimado al ritmo actual'}
                  </p>
                </article>
              </div>
              <div className="mt-4 rounded-lg border border-gray-100 bg-gray-50 p-4">
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
              {!invoicesError && invoices.length === 0 ? (
                <p className="mt-4 rounded-lg border border-dashed border-gray-200 bg-gray-50 px-4 py-6 text-center text-sm text-gray-600">
                  Todavía no hay comprobantes para mostrar. Cuando se registren
                  pagos, van a aparecer acá.
                </p>
              ) : null}
              {!invoicesError && invoices.length > 0 ? (
                <div className="mt-4 overflow-x-auto rounded-lg border border-gray-200">
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
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : null}
            </div>

            <div className="mt-8 border-t border-gray-100 pt-6">
              <h3 className="text-sm font-semibold text-gray-900">
                Medio de pago
              </h3>
              <p className="mt-2 text-sm text-gray-600">
                La gestión del medio de pago (tarjeta u otros medios) desde esta
                web estará disponible próximamente. Para cambios o consultas
                sobre tu suscripción, contactá a soporte.
              </p>
            </div>
          </section>
        ) : null}
      </div>
    </div>
  );
}
