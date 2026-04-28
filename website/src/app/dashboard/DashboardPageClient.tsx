'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { BadgeCheck, Loader2, ScanLine, Smartphone, Warehouse } from 'lucide-react';
import { getApiBaseUrl } from '../lib/apiUrl';
import {
  canAccessWebManagement,
  clearWebSession,
  fetchWebProfile,
  fetchWithWebAuth,
  hasWebSession,
  refreshWebSession,
} from '../lib/webAuth';
import DocumentUsageSection from './DocumentUsageSection';
import {
  deriveBillingPresentation,
  formatPlanLabel,
} from './lib/billingPresentation';
import type { Entitlement } from './lib/entitlementTypes';

export default function DashboardPageClient() {
  const router = useRouter();
  const [ready, setReady] = useState(false);
  const [entitlement, setEntitlement] = useState<Entitlement | null>(null);
  const [error, setError] = useState<string | null>(null);

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

      const profile = await fetchWebProfile();
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

  if (!ready && !error) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center bg-gray-50">
        <Loader2 className="h-8 w-8 animate-spin text-blue-600" aria-hidden />
      </div>
    );
  }

  const now = Date.now();
  const billing = deriveBillingPresentation(entitlement, now);

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
