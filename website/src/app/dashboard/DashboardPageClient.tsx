'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import {
  BadgeCheck,
  Download,
  Loader2,
  ScanLine,
  Smartphone,
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
import DocumentUsageSection from './DocumentUsageSection';
import {
  deriveBillingPresentation,
  formatPlanLabel,
} from './lib/billingPresentation';
import type { BillingInvoiceRow, Entitlement } from './lib/entitlementTypes';
import {
  formatInvoiceDate,
  formatInvoiceMoney,
  invoiceStatusLabel,
} from './lib/invoiceFormat';

export default function DashboardPageClient() {
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
          'No se pudieron cargar las facturas. Probá de nuevo más tarde.',
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
    <div className="bg-gray-50 px-4 pb-12 pt-6">
      <div className="mx-auto max-w-7xl space-y-8">
        {entitlement ? (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <section
              className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm"
              aria-labelledby="warehouses-card-heading"
            >
              <div className="flex gap-3">
                <div
                  className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-blue-50 text-blue-700"
                  aria-hidden
                >
                  <Warehouse className="h-7 w-7" strokeWidth={1.75} />
                </div>
                <div className="min-w-0 flex-1">
                  <h2
                    id="warehouses-card-heading"
                    className="text-sm font-semibold uppercase tracking-wide text-gray-500"
                  >
                    Depósitos
                  </h2>
                  <p className="mt-1 text-3xl font-bold tabular-nums tracking-tight text-gray-900">
                    {typeof entitlement.warehouse_count === 'number'
                      ? entitlement.warehouse_count
                      : '—'}
                  </p>
                  <p className="mt-2 text-xs leading-snug text-gray-600">
                    Depósitos configurados para tu empresa.
                  </p>
                </div>
              </div>
            </section>

            <section
              className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm"
              aria-labelledby="devices-card-heading"
            >
              <div className="flex gap-3">
                <div
                  className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-orange-50 text-orange-700"
                  aria-hidden
                >
                  <Smartphone className="h-7 w-7" strokeWidth={1.75} />
                </div>
                <div className="min-w-0 flex-1">
                  <h2
                    id="devices-card-heading"
                    className="text-sm font-semibold uppercase tracking-wide text-gray-500"
                  >
                    Dispositivos
                  </h2>
                  <p className="mt-1 text-3xl font-bold tabular-nums tracking-tight text-gray-900">
                    {typeof entitlement.device_count === 'number'
                      ? entitlement.device_count
                      : '—'}
                  </p>
                  <p className="mt-2 text-xs leading-snug text-gray-600">
                    Registrados para tu empresa en En Punto.
                  </p>
                </div>
              </div>
            </section>

            <section
              className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm"
              aria-labelledby="documents-card-heading"
            >
              <div className="flex gap-3">
                <div
                  className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-emerald-50 text-emerald-700"
                  aria-hidden
                >
                  <ScanLine className="h-7 w-7" strokeWidth={1.75} />
                </div>
                <div className="min-w-0 flex-1">
                  <h2
                    id="documents-card-heading"
                    className="text-sm font-semibold uppercase tracking-wide text-gray-500"
                  >
                    Documentos
                  </h2>
                  <p className="mt-1 text-3xl font-bold tabular-nums tracking-tight text-gray-900">
                    {typeof entitlement.remitos_processed_last_30_days ===
                    'number'
                      ? entitlement.remitos_processed_last_30_days
                      : '—'}
                  </p>
                  <p className="mt-2 text-xs leading-snug text-gray-600">
                    Este mes (últimos 30 días), sincronizados desde la app.
                  </p>
                </div>
              </div>
            </section>

            <section
              className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm"
              aria-labelledby="plan-card-heading"
            >
              <div className="flex gap-3">
                <div
                  className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl bg-violet-50 text-violet-700"
                  aria-hidden
                >
                  <BadgeCheck className="h-7 w-7" strokeWidth={1.75} />
                </div>
                <div className="min-w-0 flex-1">
                  <h2
                    id="plan-card-heading"
                    className="text-sm font-semibold uppercase tracking-wide text-gray-500"
                  >
                    Plan actual
                  </h2>
                  <p className="mt-1 truncate text-2xl font-bold tracking-tight text-gray-900">
                    {formatPlanLabel(entitlement.subscription_plan)}
                  </p>
                  <p className="mt-2 text-xs leading-snug text-gray-600">
                    {billing.billingStatusSummary}
                  </p>
                </div>
              </div>
            </section>
          </div>
        ) : null}

        {entitlement ? (
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
            warehouseRows={(entitlement.documents_usage_by_warehouse_mtd ?? []).map(
              (r) => ({
                warehouse_id: r.warehouse_id,
                name: r.name,
                count: Number(r.count),
              }),
            )}
          />
        ) : null}

        {entitlement ? (
          <section
            className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm"
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
                            className="inline-flex items-center justify-center gap-2 rounded-lg border border-gray-300 bg-white px-3 py-2 text-xs font-semibold text-gray-800 hover:bg-gray-50"
                            aria-label={`Descargar factura ${inv.id}`}
                          >
                            <Download className="h-4 w-4" aria-hidden />
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
      </div>
    </div>
  );
}
