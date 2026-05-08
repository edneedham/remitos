'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  BadgeCheck,
  Download,
  ScanLine,
  Smartphone,
  Users,
  Warehouse,
} from 'lucide-react';
import { getApiBaseUrl } from '../lib/apiUrl';
import {
  canAccessWebManagement,
  clearWebSession,
  fetchProfile,
  fetchWithWebAuth,
  hasWebSession,
  refreshWebSession,
} from '../lib/webAuth';
import {
  buildTrialOnboardingChecklist,
  CHECKLIST_DOWNLOAD_PAGE_VISITED_KEY,
} from '../lib/trialOnboardingChecklist';
import DocumentUsageSection from './DocumentUsageSection';
import {
  DashboardDocumentUsageSkeleton,
  DashboardInvoicesSkeleton,
  DashboardStatCardsSkeleton,
} from './components/PanelSkeletons';
import TrialOnboardingChecklist from './TrialOnboardingChecklist';
import {
  deriveBillingPresentation,
  formatPlanLabel,
  shortBillingStatusSummary,
} from './lib/billingPresentation';
import type { BillingInvoiceRow, Entitlement } from './lib/entitlementTypes';
import {
  formatInvoiceDate,
  formatInvoiceMoney,
  invoiceStatusLabel,
} from './lib/invoiceFormat';
import {
  FIRST_SCAN_ANALYTICS_SENT_KEY,
  trackTrialOnboardingEvent,
} from '../lib/trialOnboardingAnalytics';

function maybeEmitFirstScanCompleted(data: Entitlement): void {
  try {
    const done =
      data.first_scan_completed === true ||
      (data.remitos_processed_last_30_days ?? 0) >= 1;
    if (!done) return;
    if (
      typeof window !== 'undefined' &&
      window.localStorage.getItem(FIRST_SCAN_ANALYTICS_SENT_KEY) !== '1'
    ) {
      window.localStorage.setItem(FIRST_SCAN_ANALYTICS_SENT_KEY, '1');
      trackTrialOnboardingEvent('first_scan_completed', {
        first_scan_completed: data.first_scan_completed === true,
        first_scan_completed_at: data.first_scan_completed_at,
        remitos_processed_last_30_days: data.remitos_processed_last_30_days,
      });
    }
  } catch {
    /* ignore localStorage / analytics */
  }
}

export default function DashboardPageClient() {
  const router = useRouter();
  const [entitlement, setEntitlement] = useState<Entitlement | null>(null);
  const [entitlementLoading, setEntitlementLoading] = useState(true);
  const [invoicesLoading, setInvoicesLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [invoices, setInvoices] = useState<BillingInvoiceRow[]>([]);
  const [invoicesError, setInvoicesError] = useState<string | null>(null);
  const [downloadPageVisited, setDownloadPageVisited] = useState(false);
  const [companyName, setCompanyName] = useState<string | null>(null);

  useEffect(() => {
    function readDownloadVisitFlag() {
      try {
        setDownloadPageVisited(
          window.localStorage.getItem(CHECKLIST_DOWNLOAD_PAGE_VISITED_KEY) ===
            '1',
        );
      } catch {
        setDownloadPageVisited(false);
      }
    }

    async function refreshEntitlementOnFocus() {
      if (!hasWebSession()) return;
      const res = await fetchWithWebAuth('/auth/me/entitlement');
      if (res.status === 401) {
        clearWebSession();
        router.replace('/ingresar');
        return;
      }
      if (!res.ok) return;
      const data = (await res.json()) as Entitlement;
      setEntitlement(data);
      maybeEmitFirstScanCompleted(data);
    }

    function onWindowFocus() {
      readDownloadVisitFlag();
      if (entitlement !== null) void refreshEntitlementOnFocus();
    }

    readDownloadVisitFlag();
    window.addEventListener('focus', onWindowFocus);
    window.addEventListener('storage', readDownloadVisitFlag);
    return () => {
      window.removeEventListener('focus', onWindowFocus);
      window.removeEventListener('storage', readDownloadVisitFlag);
    };
  }, [entitlement, router]);

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
        setInvoicesLoading(false);
        return;
      }

      setEntitlementLoading(true);
      setInvoicesLoading(true);
      setError(null);

      await refreshWebSession();

      const profile = await fetchProfile();
      if (cancelled) return;
      if (!profile || !canAccessWebManagement(profile.role)) {
        clearWebSession();
        router.replace('/ingresar');
        return;
      }

      setCompanyName(profile.company_name);

      const [entRes, invRes] = await Promise.all([
        fetchWithWebAuth('/auth/me/entitlement'),
        fetchWithWebAuth('/auth/me/invoices'),
      ]);
      if (cancelled) return;

      if (entRes.status === 401 || invRes.status === 401) {
        clearWebSession();
        router.replace('/ingresar');
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
        maybeEmitFirstScanCompleted(data);
      }
      setEntitlementLoading(false);

      if (!invRes.ok) {
        setInvoicesError(
          'No se pudieron cargar las facturas. Probá de nuevo más tarde.',
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
  }, [router]);

  const now = Date.now();
  const billing = entitlement
    ? deriveBillingPresentation(entitlement, now)
    : null;
  const checklistModel =
    entitlement &&
    buildTrialOnboardingChecklist(entitlement, downloadPageVisited);

  const handleDownloadInvoice = (invoice: BillingInvoiceRow) => {
    const lines = [
      `Factura: ${invoice.id}`,
      `Fecha: ${formatInvoiceDate(invoice.issued_at)}`,
      `Importe: ${formatInvoiceMoney(invoice.amount_minor, invoice.currency)}`,
      `Estado: ${invoiceStatusLabel(invoice.status)}`,
      `Concepto: ${invoice.description?.trim() ? invoice.description : '—'}`,
    ];
    const blob = new Blob([`${lines.join('\n')}\n`], {
      type: 'text/plain;charset=utf-8',
    });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `factura-${invoice.id}.txt`;
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="bg-gray-50 px-4 pb-8 pt-6 md:pb-12">
      <div className="mx-auto max-w-[92rem] space-y-6 md:space-y-8">
        <div className="md:hidden">
          <p className="truncate text-lg font-medium text-gray-900">
            Hola,{' '}
            <span className="font-semibold text-gray-950">
              {companyName ?? '…'}
            </span>
          </p>
        </div>
        {checklistModel ? (
          <div className="hidden md:block">
            <TrialOnboardingChecklist model={checklistModel} />
          </div>
        ) : null}

        {entitlementLoading && !error ? <DashboardStatCardsSkeleton /> : null}

        {entitlement ? (
          <div className="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-3 xl:grid-cols-5">
            <Link
              href="/panel/depositos"
              className="block rounded-xl border border-gray-200 bg-white p-3 shadow-sm transition-colors hover:border-blue-300 hover:bg-blue-50/40 md:p-5"
              aria-labelledby="warehouses-card-heading"
            >
              <div className="flex gap-2.5 md:gap-3">
                <div
                  className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-blue-50 text-blue-600 md:h-12 md:w-12 md:rounded-xl"
                  aria-hidden
                >
                  <Warehouse
                    className="h-4 w-4 md:h-7 md:w-7"
                    strokeWidth={1.75}
                  />
                </div>
                <div className="min-w-0 flex-1 space-y-1.5 md:space-y-2">
                  <h2
                    id="warehouses-card-heading"
                    className="text-[9px] font-semibold uppercase leading-none tracking-wide text-gray-500 md:text-sm md:leading-normal"
                  >
                    Depósitos
                  </h2>
                  <p className="text-lg font-bold tabular-nums leading-none tracking-tight text-gray-900 md:text-3xl md:leading-none">
                    {typeof entitlement.warehouse_count === 'number'
                      ? typeof entitlement.max_warehouses === 'number'
                        ? `${entitlement.warehouse_count} / ${entitlement.max_warehouses}`
                        : entitlement.warehouse_count
                      : '—'}
                  </p>
                  <p className="text-[10px] leading-snug text-gray-600 md:text-xs md:leading-snug">
                    {typeof entitlement.max_warehouses === 'number' ? (
                      <>
                        <span className="md:hidden">vs. límite del plan</span>
                        <span className="hidden md:inline">
                          Usados frente al límite de tu plan.
                        </span>
                      </>
                    ) : (
                      <>
                        <span className="md:hidden">Configurados</span>
                        <span className="hidden md:inline">
                          Depósitos configurados para tu empresa.
                        </span>
                      </>
                    )}
                  </p>
                </div>
              </div>
            </Link>

            <Link
              href="/panel/dispositivos"
              className="block rounded-xl border border-gray-200 bg-white p-3 shadow-sm transition-colors hover:border-blue-300 hover:bg-blue-50/40 md:p-5"
              aria-labelledby="devices-card-heading"
            >
              <div className="flex gap-2.5 md:gap-3">
                <div
                  className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-blue-50 text-blue-600 md:h-12 md:w-12 md:rounded-xl"
                  aria-hidden
                >
                  <Smartphone
                    className="h-4 w-4 md:h-7 md:w-7"
                    strokeWidth={1.75}
                  />
                </div>
                <div className="min-w-0 flex-1 space-y-1.5 md:space-y-2">
                  <h2
                    id="devices-card-heading"
                    className="text-[9px] font-semibold uppercase leading-none tracking-wide text-gray-500 md:text-sm md:leading-normal"
                  >
                    Dispositivos
                  </h2>
                  <p className="text-lg font-bold tabular-nums leading-none tracking-tight text-gray-900 md:text-3xl md:leading-none">
                    {typeof entitlement.device_count === 'number'
                      ? entitlement.device_count
                      : '—'}
                  </p>
                  <p className="text-[10px] leading-snug text-gray-600 md:text-xs md:leading-snug">
                    <span className="md:hidden">En la empresa</span>
                    <span className="hidden md:inline">
                      Registrados para tu empresa en En Punto.
                    </span>
                  </p>
                </div>
              </div>
            </Link>

            <Link
              href="/panel/facturacion"
              className="block rounded-xl border border-gray-200 bg-white p-3 shadow-sm transition-colors hover:border-blue-300 hover:bg-blue-50/40 md:p-5"
              aria-labelledby="users-card-heading"
            >
              <div className="flex gap-2.5 md:gap-3">
                <div
                  className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-blue-50 text-blue-600 md:h-12 md:w-12 md:rounded-xl"
                  aria-hidden
                >
                  <Users className="h-4 w-4 md:h-7 md:w-7" strokeWidth={1.75} />
                </div>
                <div className="min-w-0 flex-1 space-y-1.5 md:space-y-2">
                  <h2
                    id="users-card-heading"
                    className="text-[9px] font-semibold uppercase leading-none tracking-wide text-gray-500 md:text-sm md:leading-normal"
                  >
                    Usuarios
                  </h2>
                  <p className="text-lg font-bold tabular-nums leading-none tracking-tight text-gray-900 md:text-3xl md:leading-none">
                    {typeof entitlement.user_count === 'number'
                      ? typeof entitlement.max_users === 'number'
                        ? `${entitlement.user_count} / ${entitlement.max_users}`
                        : entitlement.user_count
                      : '—'}
                  </p>
                  <p className="text-[10px] leading-snug text-gray-600 md:text-xs md:leading-snug">
                    {typeof entitlement.max_users === 'number' ? (
                      <>
                        <span className="md:hidden">vs. límite del plan</span>
                        <span className="hidden md:inline">
                          Cuentas de la empresa frente al límite del plan.
                        </span>
                      </>
                    ) : (
                      <>
                        <span className="md:hidden">Web y app</span>
                        <span className="hidden md:inline">
                          Cuentas de la empresa con acceso web o app.
                        </span>
                      </>
                    )}
                  </p>
                </div>
              </div>
            </Link>

            <section
              className="rounded-xl border border-gray-200 bg-white p-3 shadow-sm md:p-5"
              aria-labelledby="documents-card-heading"
            >
              <div className="flex gap-2.5 md:gap-3">
                <div
                  className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-blue-50 text-blue-600 md:h-12 md:w-12 md:rounded-xl"
                  aria-hidden
                >
                  <ScanLine className="h-4 w-4 md:h-7 md:w-7" strokeWidth={1.75} />
                </div>
                <div className="min-w-0 flex-1 space-y-1.5 md:space-y-2">
                  <h2
                    id="documents-card-heading"
                    className="text-[9px] font-semibold uppercase leading-none tracking-wide text-gray-500 md:text-sm md:leading-normal"
                  >
                    Documentos
                  </h2>
                  <p className="text-lg font-bold tabular-nums leading-none tracking-tight text-gray-900 md:text-3xl md:leading-none">
                    {typeof entitlement.remitos_processed_last_30_days ===
                    'number'
                      ? entitlement.remitos_processed_last_30_days
                      : '—'}
                  </p>
                  <p className="text-[10px] leading-snug text-gray-600 md:text-xs md:leading-snug">
                    Últimos 30 días, sincronizados desde la app.
                  </p>
                </div>
              </div>
            </section>

            <section
              className="rounded-xl border border-gray-200 bg-white p-3 shadow-sm md:p-5"
              aria-labelledby="plan-card-heading"
            >
              <div className="flex gap-2.5 md:gap-3">
                <div
                  className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-blue-50 text-blue-600 md:h-12 md:w-12 md:rounded-xl"
                  aria-hidden
                >
                  <BadgeCheck className="h-4 w-4 md:h-7 md:w-7" strokeWidth={1.75} />
                </div>
                <div className="min-w-0 flex-1 space-y-1.5 md:space-y-2">
                  <h2
                    id="plan-card-heading"
                    className="text-[9px] font-semibold uppercase leading-none tracking-wide text-gray-500 md:text-sm md:leading-normal"
                    aria-label="Plan actual"
                  >
                    <span className="md:hidden" aria-hidden="true">
                      Plan
                    </span>
                    <span className="hidden md:inline" aria-hidden="true">
                      Plan actual
                    </span>
                  </h2>
                  <p className="truncate text-base font-bold leading-none tracking-tight text-gray-900 md:text-2xl md:leading-tight">
                    {formatPlanLabel(entitlement.subscription_plan)}
                  </p>
                  <p className="text-[10px] leading-snug text-gray-600 md:text-xs md:leading-snug">
                    <span className="md:hidden">
                      {billing?.billingStatusSummary
                        ? shortBillingStatusSummary(billing.billingStatusSummary)
                        : '—'}
                    </span>
                    <span className="hidden md:inline">
                      {billing?.billingStatusSummary ?? '—'}
                    </span>
                  </p>
                </div>
              </div>
            </section>
          </div>
        ) : null}

        {entitlementLoading && !error ? (
          <div className="hidden md:block">
            <DashboardDocumentUsageSkeleton />
          </div>
        ) : null}

        {entitlement ? (
          <div className="hidden md:block">
            <DocumentUsageSection
              mtd={
                typeof entitlement.documents_usage_mtd === 'number'
                  ? entitlement.documents_usage_mtd
                  : 0
              }
              limit={entitlement.documents_monthly_limit ?? null}
              series={(entitlement.documents_usage_series ?? []).map((p) => ({
                date: p.date,
                cumulative: Number(p.cumulative),
              }))}
              warehouseRows={(
                entitlement.documents_usage_by_warehouse_mtd ?? []
              ).map((r) => ({
                warehouse_id: r.warehouse_id,
                name: r.name,
                count: Number(r.count),
              }))}
            />
          </div>
        ) : null}

        {entitlement && invoicesLoading ? (
          <div className="hidden md:block">
            <DashboardInvoicesSkeleton />
          </div>
        ) : null}

        {entitlement && !invoicesLoading ? (
          <section
            className="hidden md:block rounded-xl border border-gray-200 bg-white p-5 shadow-sm"
            aria-labelledby="invoices-heading"
          >
            <div className="flex flex-col gap-1 sm:flex-row sm:items-baseline sm:justify-between">
              <h2 id="invoices-heading" className="text-base font-semibold text-gray-900">
                Facturas
              </h2>
              <p className="text-xs text-gray-500 sm:text-sm">
                Descargá el detalle de cada comprobante.
              </p>
            </div>

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
                Todavía no hay facturas para mostrar.
              </p>
            ) : null}

            {!invoicesError && invoices.length > 0 ? (
              <div className="mt-4 overflow-x-auto rounded-lg border border-gray-200">
                <table className="w-full min-w-[52rem] text-left text-sm">
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
                      <th scope="col" className="px-4 py-3 text-right font-semibold text-gray-700">
                        Descargar
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
                        <td className="max-w-[20rem] px-4 py-3 text-gray-700">
                          {inv.description?.trim() ? inv.description : '—'}
                        </td>
                        <td className="whitespace-nowrap px-4 py-3 text-right">
                          <button
                            type="button"
                            onClick={() => handleDownloadInvoice(inv)}
                            className="inline-flex items-center justify-center gap-2 rounded-lg border border-blue-200 bg-white px-3 py-2 text-xs font-semibold text-blue-700 hover:bg-blue-50"
                            aria-label={`Descargar factura ${inv.id}`}
                          >
                            <Download className="h-4 w-4 text-blue-600" aria-hidden />
                            Descargar
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : null}
          </section>
        ) : null}

        {error && (
          <div
            className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800"
            role="alert"
          >
            {error}
          </div>
        )}

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
      </div>
    </div>
  );
}
