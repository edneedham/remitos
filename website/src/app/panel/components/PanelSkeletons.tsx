/**
 * Loading placeholders for /panel routes (Next.js loading.tsx boundaries).
 * Styled to match DashboardShell content area: bg-gray-50, max-w-[92rem].
 */

import type { ReactNode } from 'react';

const pulse = 'animate-pulse bg-gray-200';

/** Default list rows for `loading.tsx` on narrow list routes (dispositivos uses grouped variant). */
export const PANEL_LIST_ROUTE_ROWS = 8;

function PanelOuter({ children }: { children: ReactNode }) {
  return (
    <div className="bg-gray-50 px-4 pb-12 pt-6" aria-busy="true">
      <div className="mx-auto max-w-[92rem] space-y-8">{children}</div>
    </div>
  );
}

/** Dashboard home: KPI row only (progressive load before entitlement arrives). */
export function DashboardStatCardsSkeleton() {
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
      {Array.from({ length: 5 }).map((_, i) => (
        <div
          key={i}
          className="h-36 rounded-xl border border-gray-100 bg-white p-5 shadow-sm"
        >
          <div className="flex animate-pulse gap-3">
            <div className="h-12 w-12 shrink-0 rounded-xl bg-gray-200" />
            <div className="flex min-w-0 flex-1 flex-col gap-2 pt-0.5">
              <div className="h-3 w-24 rounded bg-gray-200" />
              <div className="h-9 w-16 rounded bg-gray-200" />
              <div className="h-3 w-full max-w-[11rem] rounded bg-gray-200" />
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

/** Document usage + warehouse breakdown while metrics load. */
export function DashboardDocumentUsageSkeleton() {
  return (
    <div
      className="grid min-h-[28rem] grid-cols-1 gap-4 lg:grid-cols-2 lg:items-stretch lg:gap-6"
      aria-busy="true"
    >
      <div className="flex min-h-[26rem] flex-col rounded-xl border border-gray-100 bg-white p-5 shadow-sm">
        <div className="h-5 w-44 animate-pulse rounded bg-gray-200" />
        <div className="mt-4 h-4 w-28 animate-pulse rounded bg-gray-200" />
        <div className="mt-4 min-h-[260px] flex-1 animate-pulse rounded-lg bg-gray-100" />
      </div>
      <div className="flex min-h-[26rem] flex-col rounded-xl border border-gray-100 bg-white p-5 shadow-sm">
        <div className="h-5 w-36 animate-pulse rounded bg-gray-200" />
        <div className="mt-4 space-y-4">
          {Array.from({ length: 5 }).map((_, i) => (
            <div
              key={i}
              className="h-10 animate-pulse rounded-lg bg-gray-100"
            />
          ))}
        </div>
      </div>
    </div>
  );
}

/** Invoice table block on dashboard home. */
export function DashboardInvoicesSkeleton() {
  return (
    <section
      className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm"
      aria-busy="true"
    >
      <div className="flex flex-col gap-1 sm:flex-row sm:items-baseline sm:justify-between">
        <div className="h-6 w-28 animate-pulse rounded bg-gray-200" />
        <div className="h-4 w-64 max-w-full animate-pulse rounded bg-gray-200" />
      </div>
      <div className="mt-4 overflow-hidden rounded-lg border border-gray-200">
        <div className="h-12 animate-pulse bg-gray-100" />
        {Array.from({ length: 4 }).map((_, i) => (
          <div
            key={i}
            className="h-14 animate-pulse border-b border-gray-50 bg-white last:border-0"
          />
        ))}
      </div>
    </section>
  );
}

/** Facturación page: summary + plan grid while entitlement loads. */
export function BillingMainSkeleton() {
  return (
    <div className="space-y-8" aria-busy="true">
      <div className="rounded-xl border border-gray-100 bg-white p-6 shadow-sm">
        <div className="h-7 w-64 animate-pulse rounded bg-gray-200" />
        <div className="mt-4 h-4 w-full max-w-2xl animate-pulse rounded bg-gray-200" />
        <div className="mt-8 space-y-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <div
              key={i}
              className="flex justify-between gap-4 border-b border-gray-50 py-2 last:border-0"
            >
              <div className="h-4 w-40 animate-pulse rounded bg-gray-200" />
              <div className="h-4 w-32 animate-pulse rounded bg-gray-200" />
            </div>
          ))}
        </div>
        <div className="mt-8 grid gap-4 lg:grid-cols-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <div
              key={i}
              className="h-40 animate-pulse rounded-lg border border-gray-100 bg-gray-50"
            />
          ))}
        </div>
      </div>
    </div>
  );
}

/** Comprobantes table only (facturación) while invoices load. */
export function BillingComprobantesSkeleton() {
  return (
    <div className="mt-8 border-t border-gray-100 pt-6" aria-busy="true">
      <div className="h-5 w-52 animate-pulse rounded bg-gray-200" />
      <div className="mt-2 h-4 w-72 max-w-full animate-pulse rounded bg-gray-200" />
      <div className="mt-4 overflow-hidden rounded-lg border border-gray-200">
        <div className="h-12 animate-pulse bg-gray-100" />
        {Array.from({ length: 5 }).map((_, i) => (
          <div
            key={i}
            className="h-14 animate-pulse border-b border-gray-50 bg-white last:border-0"
          />
        ))}
      </div>
    </div>
  );
}

/** Main dashboard: stat cards + wide content blocks (charts). */
export function PanelDashboardSkeleton() {
  return (
    <PanelOuter>
      <div className="h-24 rounded-xl bg-gray-100" />
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
        {Array.from({ length: 5 }).map((_, i) => (
          <div
            key={i}
            className={`h-36 rounded-xl border border-gray-100 bg-white p-5 shadow-sm ${pulse}`}
          />
        ))}
      </div>
      <div className="grid min-h-[28rem] grid-cols-1 gap-4 lg:grid-cols-2 lg:gap-6">
        <div className={`min-h-[26rem] rounded-xl border border-gray-100 bg-white p-5 shadow-sm ${pulse}`} />
        <div className={`min-h-[26rem] rounded-xl border border-gray-100 bg-white p-5 shadow-sm ${pulse}`} />
      </div>
    </PanelOuter>
  );
}

/** Facturación-style: summary strip + table. */
export function PanelBillingSkeleton() {
  return (
    <PanelOuter>
      <div className="space-y-2">
        <div className={`h-9 w-48 rounded-md ${pulse}`} />
        <div className={`h-5 w-96 max-w-full rounded-md ${pulse}`} />
      </div>
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <div className={`h-40 rounded-xl border border-gray-100 bg-white p-5 shadow-sm ${pulse}`} />
        <div className={`h-40 rounded-xl border border-gray-100 bg-white p-5 shadow-sm ${pulse}`} />
      </div>
      <div className="overflow-hidden rounded-xl border border-gray-100 bg-white shadow-sm">
        <div className={`h-12 border-b border-gray-100 ${pulse}`} />
        {Array.from({ length: 6 }).map((_, i) => (
          <div
            key={i}
            className={`h-14 border-b border-gray-50 last:border-0 ${pulse}`}
          />
        ))}
      </div>
    </PanelOuter>
  );
}

/**
 * Narrow list routes (depósitos, operadores): matches max-w-5xl shell + back link, title,
 * intro lines, and warehouse-style list rows (same density as live pages).
 */
export function PanelListSkeleton({
  rows = PANEL_LIST_ROUTE_ROWS,
}: {
  rows?: number;
}) {
  return (
    <PanelOuter>
      <div className="mx-auto max-w-5xl space-y-6">
        <header className="space-y-2">
          <div className={`h-4 w-28 rounded ${pulse}`} />
          <div className={`h-9 w-56 max-w-full rounded-md ${pulse}`} />
          <div className="space-y-2 pt-0.5">
            <div className={`h-5 w-full max-w-2xl rounded ${pulse}`} />
            <div
              className={`h-5 w-full max-w-xl rounded bg-gray-100 ${pulse}`}
            />
          </div>
        </header>
        <section className="overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
          {Array.from({ length: rows }).map((_, i) => (
            <div
              key={i}
              className="flex flex-wrap items-center justify-between gap-3 border-b border-gray-50 px-5 py-4 last:border-0"
            >
              <div className="min-w-0 flex-1 space-y-2">
                <div className={`h-5 w-48 max-w-full rounded ${pulse}`} />
                <div
                  className={`h-4 w-full max-w-md rounded bg-gray-100 ${pulse}`}
                />
              </div>
              <div className="flex shrink-0 gap-2">
                <div className={`h-9 w-20 rounded-lg ${pulse}`} />
                <div className={`h-9 w-24 rounded-lg ${pulse}`} />
              </div>
            </div>
          ))}
        </section>
      </div>
    </PanelOuter>
  );
}

/** Dispositivos route loading: grouped sections like the live page. */
export function PanelDevicesRouteSkeleton() {
  return (
    <PanelOuter>
      <div className="mx-auto max-w-5xl space-y-6">
        <header className="space-y-2">
          <div className={`h-4 w-28 rounded ${pulse}`} />
          <div className={`h-9 w-56 max-w-full rounded-md ${pulse}`} />
          <div className="space-y-2 pt-0.5">
            <div className={`h-5 w-full max-w-2xl rounded ${pulse}`} />
            <div
              className={`h-5 w-full max-w-xl rounded bg-gray-100 ${pulse}`}
            />
          </div>
        </header>
        <DevicesGroupedListSkeleton />
      </div>
    </PanelOuter>
  );
}

/** Aplicación route loading: wide title + compact column like ApplicationPageClient. */
export function PanelApplicationRouteSkeleton() {
  return (
    <PanelOuter>
      <div className="mx-auto max-w-[92rem] space-y-8 text-left">
        <div className={`h-8 w-44 max-w-full rounded-md ${pulse}`} />
        <ApplicationContentSkeleton />
      </div>
    </PanelOuter>
  );
}

/** Cambiar de plan route loading: matches UpgradePlanPageClient shell. */
export function PanelUpgradePlanRouteSkeleton() {
  return (
    <PanelOuter>
      <header className="space-y-2">
        <div className={`h-4 w-36 rounded ${pulse}`} />
        <div className={`h-9 w-72 max-w-full rounded-md ${pulse}`} />
        <div className={`h-5 w-full max-w-2xl rounded bg-gray-100 ${pulse}`} />
      </header>
      <UpgradePlanBodySkeleton />
    </PanelOuter>
  );
}

/** Activar suscripción route loading: title + plan/payment blocks (parent provides max-w-xl). */
export function PanelActivateSubscriptionRouteSkeleton() {
  return (
    <PanelOuter>
      <div className="mx-auto max-w-xl space-y-8">
        <div className="space-y-3">
          <div className={`h-8 w-64 max-w-full rounded-md ${pulse}`} />
          <div className={`h-4 w-full max-w-lg rounded bg-gray-100 ${pulse}`} />
        </div>
        <ActivateSubscriptionBodySkeleton />
      </div>
    </PanelOuter>
  );
}

/** Success / confirmation pages (compact). */
export function PanelCompactSkeleton() {
  return (
    <PanelOuter>
      <div className="mx-auto flex max-w-lg flex-col items-center space-y-6 py-8">
        <div className={`h-14 w-14 shrink-0 rounded-full ${pulse}`} />
        <div className={`h-8 w-56 rounded-md ${pulse}`} />
        <div className={`h-4 w-full max-w-sm rounded ${pulse}`} />
        <div className={`h-11 w-full max-w-xs rounded-lg ${pulse}`} />
      </div>
    </PanelOuter>
  );
}

/** Forms (cambiar contraseña): matches max-w-lg title + intro + card. */
export function PanelFormSkeleton() {
  return (
    <PanelOuter>
      <div className="mx-auto max-w-lg space-y-6">
        <div className="space-y-3">
          <div className={`h-8 w-56 max-w-full rounded-md ${pulse}`} />
          <div className={`h-4 w-full max-w-md rounded bg-gray-100 ${pulse}`} />
        </div>
        <div className="space-y-6 rounded-xl border border-gray-100 bg-white p-6 shadow-sm">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="space-y-2">
              <div className={`h-4 w-28 rounded ${pulse}`} />
              <div className={`h-11 w-full rounded-lg ${pulse}`} />
            </div>
          ))}
          <div className={`h-11 w-full max-w-xs rounded-lg ${pulse}`} />
        </div>
      </div>
    </PanelOuter>
  );
}

/** Dispositivos: grouped blocks like warehouse sections + device rows. */
export function DevicesGroupedListSkeleton() {
  return (
    <div className="space-y-6" aria-busy="true">
      {[0, 1].map((section) => (
        <section
          key={section}
          className="rounded-xl border border-gray-200 bg-white shadow-sm"
        >
          <header className="border-b border-gray-100 px-5 py-3">
            <div className="h-5 w-44 animate-pulse rounded bg-gray-200" />
            <div className="mt-2 h-3 w-32 animate-pulse rounded bg-gray-200" />
          </header>
          <ul className="divide-y divide-gray-100">
            {[0, 1].map((row) => (
              <li key={row} className="px-5 py-4">
                <div className="h-4 w-48 max-w-full animate-pulse rounded bg-gray-200" />
                <div className="mt-2 h-3 w-full max-w-md animate-pulse rounded bg-gray-200" />
                <div className="mt-2 h-3 w-56 animate-pulse rounded bg-gray-200" />
              </li>
            ))}
          </ul>
        </section>
      ))}
    </div>
  );
}

/** Depósitos: botón + lista mientras cargan datos. */
export function WarehousesBodySkeleton() {
  return (
    <div className="space-y-6" aria-busy="true">
      <div className="flex flex-wrap items-center gap-3">
        <div className="h-10 w-44 animate-pulse rounded-lg bg-gray-200" />
        <div className="h-4 w-48 max-w-full animate-pulse rounded bg-gray-200" />
      </div>
      <section className="rounded-xl border border-gray-200 bg-white shadow-sm">
        {Array.from({ length: 5 }).map((_, i) => (
          <div
            key={i}
            className="flex flex-wrap items-center justify-between gap-3 border-b border-gray-50 px-5 py-4 last:border-0"
          >
            <div className="min-w-0 flex-1 space-y-2">
              <div className="h-5 w-48 max-w-full animate-pulse rounded bg-gray-200" />
              <div className="h-4 w-full max-w-sm animate-pulse rounded bg-gray-200" />
            </div>
            <div className="flex shrink-0 gap-2">
              <div className="h-9 w-20 animate-pulse rounded-lg bg-gray-200" />
              <div className="h-9 w-24 animate-pulse rounded-lg bg-gray-200" />
            </div>
          </div>
        ))}
      </section>
    </div>
  );
}

/** Operadores: filas de lista (el formulario puede mostrarse antes). */
export function OperadoresListSkeleton() {
  return (
    <section
      className="rounded-xl border border-gray-200 bg-white shadow-sm"
      aria-busy="true"
      aria-label="Cargando operadores"
    >
      {Array.from({ length: 4 }).map((_, i) => (
        <div
          key={i}
          className="space-y-3 border-b border-gray-100 px-5 py-4 last:border-0"
        >
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div className="space-y-2">
              <div className="h-4 w-56 animate-pulse rounded bg-gray-200" />
              <div className="h-3 w-32 animate-pulse rounded bg-gray-200" />
            </div>
            <div className="h-8 w-24 animate-pulse rounded-lg bg-gray-200" />
          </div>
          <div className="flex flex-wrap items-end gap-2">
            <div className="h-9 min-w-[200px] flex-1 animate-pulse rounded-lg bg-gray-100" />
            <div className="h-9 w-36 animate-pulse rounded-lg bg-gray-200" />
          </div>
        </div>
      ))}
    </section>
  );
}

/** Subtítulo de página que depende del entitlement (max depósitos / usuarios). */
export function PanelEntitlementIntroSkeleton() {
  return (
    <div className="space-y-2" aria-busy="true">
      <div className="h-5 w-full max-w-2xl animate-pulse rounded bg-gray-200" />
      <div className="h-5 w-full max-w-xl animate-pulse rounded bg-gray-100" />
    </div>
  );
}

/** Aplicación: columna principal antes de entitlement. */
export function ApplicationContentSkeleton() {
  return (
    <div className="max-w-xl space-y-6" aria-busy="true">
      <div className="h-32 w-full animate-pulse rounded-xl bg-gray-100" />
      <div className="h-4 w-40 animate-pulse rounded bg-gray-200" />
      <div className="h-11 w-48 animate-pulse rounded-lg bg-gray-200" />
    </div>
  );
}

/** Cambiar de plan: bloques de contenido bajo el header. */
export function UpgradePlanBodySkeleton() {
  return (
    <div className="space-y-8" aria-busy="true">
      <div className="rounded-xl border border-gray-100 bg-white p-6 shadow-sm">
        <div className="h-6 w-72 animate-pulse rounded bg-gray-200" />
        <div className="mt-4 h-4 w-full max-w-2xl animate-pulse rounded bg-gray-200" />
        <div className="mt-6 grid gap-4 md:grid-cols-2">
          <div className="h-36 animate-pulse rounded-lg border border-gray-100 bg-gray-50" />
          <div className="h-36 animate-pulse rounded-lg border border-gray-100 bg-gray-50" />
        </div>
        <div className="mt-8 h-12 w-full max-w-xs animate-pulse rounded-lg bg-gray-200" />
      </div>
      <div className="h-24 animate-pulse rounded-xl bg-gray-100" />
    </div>
  );
}

/** Activar suscripción: plan + área de pago. */
export function ActivateSubscriptionBodySkeleton() {
  return (
    <div className="space-y-8" aria-busy="true">
      <div className="space-y-3 rounded-xl border border-gray-100 bg-white p-5 shadow-sm">
        <div className="h-4 w-24 animate-pulse rounded bg-gray-200" />
        <div className="grid gap-2">
          {[0, 1].map((i) => (
            <div
              key={i}
              className="h-14 animate-pulse rounded-lg border border-gray-100 bg-gray-50"
            />
          ))}
        </div>
      </div>
      <div className="space-y-4 rounded-xl border border-gray-100 bg-white p-6 shadow-sm">
        <div className="h-5 w-48 animate-pulse rounded bg-gray-200" />
        <div className="h-24 animate-pulse rounded-lg bg-gray-100" />
        <div className="h-11 w-full max-w-xs animate-pulse rounded-lg bg-gray-200" />
      </div>
    </div>
  );
}

/** Operadores: bootstrap antes de conocer perfil (formulario + lista genéricos). */
export function OperadoresBootstrapSkeleton() {
  return (
    <div className="space-y-6" aria-busy="true">
      <div className="space-y-4 rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
        <div className="h-5 w-40 animate-pulse rounded bg-gray-200" />
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div className="h-10 animate-pulse rounded-lg bg-gray-100" />
          <div className="h-10 animate-pulse rounded-lg bg-gray-100" />
        </div>
        <div className="h-10 w-40 animate-pulse rounded-lg bg-gray-200" />
      </div>
      <OperadoresListSkeleton />
    </div>
  );
}
