/**
 * Loading placeholders for /panel routes (Next.js loading.tsx boundaries).
 * Styled to match DashboardShell content area: bg-gray-50, max-w-[92rem].
 */

import type { ReactNode } from 'react';

const pulse = 'animate-pulse bg-gray-200';

function PanelOuter({ children }: { children: ReactNode }) {
  return (
    <div className="bg-gray-50 px-4 pb-12 pt-6" aria-busy="true">
      <div className="mx-auto max-w-[92rem] space-y-8">{children}</div>
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

/** Lists (depósitos, dispositivos, operadores, aplicación). */
export function PanelListSkeleton({ rows = 6 }: { rows?: number }) {
  return (
    <PanelOuter>
      <div className={`h-9 w-56 rounded-md ${pulse}`} />
      <div className={`h-5 w-full max-w-xl rounded-md ${pulse}`} />
      <div className="overflow-hidden rounded-xl border border-gray-100 bg-white shadow-sm">
        {Array.from({ length: rows }).map((_, i) => (
          <div
            key={i}
            className="flex items-center gap-4 border-b border-gray-50 px-5 py-4 last:border-0"
          >
            <div className={`h-10 w-10 shrink-0 rounded-full ${pulse}`} />
            <div className="min-w-0 flex-1 space-y-2">
              <div className={`h-4 w-full max-w-md rounded ${pulse}`} />
              <div className={`h-3 w-full max-w-xs rounded ${pulse}`} />
            </div>
            <div className={`h-8 w-20 shrink-0 rounded-md ${pulse}`} />
          </div>
        ))}
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

/** Forms (contraseña, activar suscripción, mejorar plan). */
export function PanelFormSkeleton() {
  return (
    <PanelOuter>
      <div className={`h-9 w-64 rounded-md ${pulse}`} />
      <div className={`h-5 w-full max-w-lg rounded-md ${pulse}`} />
      <div className="mx-auto max-w-lg space-y-6 rounded-xl border border-gray-100 bg-white p-6 shadow-sm">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="space-y-2">
            <div className={`h-4 w-28 rounded ${pulse}`} />
            <div className={`h-11 w-full rounded-lg ${pulse}`} />
          </div>
        ))}
        <div className={`h-11 w-full max-w-xs rounded-lg ${pulse}`} />
      </div>
    </PanelOuter>
  );
}
