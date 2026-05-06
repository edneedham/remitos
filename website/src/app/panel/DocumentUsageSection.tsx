'use client';

import dynamic from 'next/dynamic';
import type { UsageChartDatum } from './DocumentUsageLineChart';

const DocumentUsageLineChart = dynamic(
  () => import('./DocumentUsageLineChart'),
  {
    ssr: false,
    loading: () => (
      <div
        className="flex h-[260px] w-full items-center justify-center rounded-lg bg-gray-50 text-sm text-gray-500"
        role="status"
        aria-live="polite"
      >
        Cargando gráfico…
      </div>
    ),
  },
);

export type DocumentUsageSeriesPoint = {
  date: string;
  cumulative: number;
};

export type WarehouseDocumentUsageRow = {
  warehouse_id: string;
  name: string;
  count: number;
};

type Props = {
  mtd: number;
  limit?: number | null;
  series: DocumentUsageSeriesPoint[];
  warehouseRows: WarehouseDocumentUsageRow[];
};

const WAREHOUSE_ROW_BAR_FILLS = [
  'bg-blue-500',
  'bg-emerald-500',
  'bg-violet-500',
  'bg-amber-500',
  'bg-rose-500',
  'bg-cyan-600',
] as const;

function utcYmd(d: Date): string {
  const y = d.getUTCFullYear();
  const m = String(d.getUTCMonth() + 1).padStart(2, '0');
  const day = String(d.getUTCDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

function daysInUtcMonth(year: number, monthIndex0: number): number {
  return new Date(Date.UTC(year, monthIndex0 + 1, 0)).getUTCDate();
}

/** One row per calendar day in the month; cumulative forward-filled through today, null on future days. */
function buildMonthChartData(
  series: DocumentUsageSeriesPoint[],
  limitValue: number | null,
): UsageChartDatum[] {
  const now = new Date();
  const year = now.getUTCFullYear();
  const monthIndex = now.getUTCMonth();
  const monthPrefix = `${year}-${String(monthIndex + 1).padStart(2, '0')}-`;
  const inMonth = series.filter((p) => p.date.startsWith(monthPrefix));
  const sorted = [...inMonth].sort((a, b) => a.date.localeCompare(b.date));
  const lastDay = daysInUtcMonth(year, monthIndex);
  const todayStr = utcYmd(now);

  const byDate = new Map(
    sorted.map((p) => [p.date, Number(p.cumulative)]),
  );

  const out: UsageChartDatum[] = [];

  let lastCumulative = 0;
  for (let day = 1; day <= lastDay; day++) {
    const mm = String(monthIndex + 1).padStart(2, '0');
    const dd = String(day).padStart(2, '0');
    const dateStr = `${year}-${mm}-${dd}`;
    const isFuture = dateStr > todayStr;

    if (isFuture) {
      out.push({
        date: dateStr,
        cumulative: null,
        limitLine: limitValue != null ? limitValue : undefined,
      });
      continue;
    }

    if (byDate.has(dateStr)) {
      lastCumulative = byDate.get(dateStr)!;
    }

    out.push({
      date: dateStr,
      cumulative: lastCumulative,
      limitLine: limitValue != null ? limitValue : undefined,
    });
  }

  return out;
}

export default function DocumentUsageSection({
  mtd,
  limit,
  series,
  warehouseRows,
}: Props) {
  const limitValue = limit != null && Number.isFinite(limit) ? limit : null;
  const limitDisplay = limitValue != null ? limitValue : '—';

  const chartData = buildMonthChartData(series, limitValue);

  const yTickCount = 6;
  const seriesMax = Math.max(
    ...chartData.map((d) => (d.cumulative != null ? d.cumulative : 0)),
    0,
    1,
  );
  const maxSeriesValue = seriesMax;
  const yMax = limitValue != null && limitValue > 0 ? limitValue : maxSeriesValue;
  const yStep =
    limitValue != null && limitValue > 0
      ? limitValue / (yTickCount - 1)
      : Math.max(1, Math.ceil(maxSeriesValue / (yTickCount - 1)));
  const yTicks = Array.from({ length: yTickCount }, (_, index) => {
    const tickValue = index * yStep;
    return limitValue != null && limitValue > 0
      ? Number(tickValue.toFixed(2))
      : tickValue;
  });

  const monthAxisLabel =
    chartData.length > 0
      ? new Date(`${chartData[0].date}T12:00:00.000Z`).toLocaleDateString(
          'es-AR',
          {
            month: 'long',
            year: 'numeric',
            timeZone: 'UTC',
          },
        )
      : '';

  const sortedWarehouses = [...warehouseRows].sort((a, b) => {
    if (b.count !== a.count) return b.count - a.count;
    return (a.name || '').localeCompare(b.name || '', 'es');
  });

  const warehouseDenominator =
    mtd > 0 ? mtd : sortedWarehouses.reduce((s, r) => s + r.count, 0);

  return (
    <section
      className="grid min-h-0 grid-cols-1 gap-4 lg:grid-cols-2 lg:gap-6 lg:items-stretch"
      aria-label="Uso de documentos y desglose por depósito"
    >
      <div className="flex min-h-[26rem] flex-col rounded-xl border border-gray-200 bg-white p-5 shadow-sm lg:h-full lg:min-h-0">
        <div className="flex flex-col gap-1 sm:flex-row sm:items-baseline sm:justify-between">
          <h2
            id="document-usage-heading"
            className="text-base font-semibold text-gray-900"
          >
            Uso de documentos
          </h2>
          <p className="shrink-0 tabular-nums text-sm font-semibold text-gray-700 sm:text-base">
            {mtd} / {limitDisplay}
          </p>
        </div>

        <div
          className="mt-4 flex flex-wrap gap-x-8 gap-y-2 text-xs text-gray-600"
          aria-label="Leyenda del gráfico"
        >
          <span className="inline-flex items-center gap-2">
            <span
              className="inline-block h-0.5 w-10 rounded-full bg-blue-600"
              aria-hidden
            />
            Documentos (mes en curso)
          </span>
          {limitValue != null ? (
            <span className="inline-flex items-center gap-2">
              <span
                className="inline-block w-10 border-t-2 border-dashed border-slate-500"
                aria-hidden
              />
              Límite del plan
            </span>
          ) : (
            <span className="text-gray-500">Sin límite mensual configurado</span>
          )}
        </div>

        <div className="mt-4 min-h-[260px] flex-1 w-full min-w-0">
          <DocumentUsageLineChart
            chartData={chartData}
            monthAxisLabel={monthAxisLabel}
            yMax={yMax}
            yTicks={yTicks}
            limitValue={limitValue}
          />
        </div>
      </div>

      <div className="flex min-h-[26rem] flex-col rounded-xl border border-gray-200 bg-white p-5 shadow-sm lg:h-full lg:min-h-0">
        <div className="flex flex-col gap-1 sm:flex-row sm:items-baseline sm:justify-between">
          <h2
            id="document-warehouse-heading"
            className="text-base font-semibold text-gray-900"
          >
            Por depósito
          </h2>
          <p className="shrink-0 text-xs text-gray-500 sm:text-sm">
            Mes en curso (UTC)
          </p>
        </div>
        <p className="mt-2 text-xs leading-snug text-gray-600">
          Documentos entrantes sincronizados por depósito en el mes calendario.
        </p>

        <div className="mt-4 flex min-h-0 flex-1 flex-col overflow-hidden">
          {sortedWarehouses.length === 0 ? (
            <p className="mt-2 flex flex-1 items-center justify-center text-center text-sm text-gray-500">
              No hay depósitos configurados para esta empresa.
            </p>
          ) : (
            <ul className="min-h-0 flex-1 space-y-4 overflow-y-auto pr-1">
              {sortedWarehouses.map((row, index) => {
                const pct =
                  warehouseDenominator > 0
                    ? Math.round((row.count / warehouseDenominator) * 1000) /
                      10
                    : 0;
                const fillClass =
                  WAREHOUSE_ROW_BAR_FILLS[
                    index % WAREHOUSE_ROW_BAR_FILLS.length
                  ];
                return (
                  <li key={`${row.warehouse_id}-${index}`}>
                    <div className="flex items-center justify-between gap-3 text-sm">
                      <span className="truncate font-medium text-gray-900">
                        {row.name?.trim() ? row.name : 'Sin nombre'}
                      </span>
                      <span className="shrink-0 tabular-nums text-gray-700">
                        {row.count}
                        {warehouseDenominator > 0 ? (
                          <span className="text-gray-400"> ({pct}%)</span>
                        ) : null}
                      </span>
                    </div>
                    <div
                      className="mt-2 h-2 overflow-hidden rounded-full bg-gray-100"
                      role="presentation"
                    >
                      <div
                        className={`h-full rounded-full transition-[width] duration-300 ease-out ${fillClass}`}
                        style={{
                          width:
                            warehouseDenominator > 0
                              ? `${(row.count / warehouseDenominator) * 100}%`
                              : '0%',
                        }}
                        aria-hidden
                      />
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      </div>
    </section>
  );
}
