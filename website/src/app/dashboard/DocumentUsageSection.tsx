'use client';

import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';

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

export default function DocumentUsageSection({
  mtd,
  limit,
  series,
  warehouseRows,
}: Props) {
  const limitValue = limit != null && Number.isFinite(limit) ? limit : null;
  const limitDisplay = limitValue != null ? limitValue : '—';

  const chartData = series.map((p) => ({
    date: p.date,
    cumulative: Number(p.cumulative),
    limitLine: limitValue != null ? limitValue : undefined,
  }));

  const maxY = Math.max(
    ...chartData.map((d) => d.cumulative),
    limitValue ?? 0,
    1,
  );
  const yMax = Math.ceil(maxY * 1.05);

  const formatDay = (d: string) => {
    const x = new Date(`${d}T12:00:00.000Z`);
    return x.toLocaleDateString('es-AR', { day: 'numeric', month: 'short' });
  };

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
          {chartData.length === 0 ? (
            <p className="flex h-full min-h-[260px] items-center justify-center text-sm text-gray-500">
              No hay datos de uso para este mes.
            </p>
          ) : (
            <ResponsiveContainer width="100%" height={260}>
              <LineChart
                data={chartData}
                margin={{ top: 8, right: 12, left: 0, bottom: 0 }}
              >
                <CartesianGrid
                  strokeDasharray="3 3"
                  vertical={false}
                  stroke="#e5e7eb"
                />
                <XAxis
                  dataKey="date"
                  tickFormatter={formatDay}
                  tick={{ fontSize: 11 }}
                  stroke="#9ca3af"
                />
                <YAxis
                  domain={[0, yMax]}
                  allowDecimals={false}
                  tick={{ fontSize: 11 }}
                  stroke="#9ca3af"
                  width={44}
                />
                <Tooltip
                  contentStyle={{
                    borderRadius: '8px',
                    border: '1px solid #e5e7eb',
                  }}
                  labelFormatter={(label) =>
                    typeof label === 'string' ? formatDay(label) : String(label)
                  }
                  formatter={(value, name) => {
                    const v = typeof value === 'number' ? value : Number(value);
                    if (name === 'limitLine') {
                      return [v, 'Límite del plan'];
                    }
                    return [v, 'Documentos'];
                  }}
                />
                <Line
                  type="monotone"
                  dataKey="cumulative"
                  name="Documentos"
                  stroke="#2563eb"
                  strokeWidth={2}
                  dot={false}
                  activeDot={{ r: 4 }}
                  isAnimationActive={false}
                />
                {limitValue != null ? (
                  <Line
                    type="monotone"
                    dataKey="limitLine"
                    name="Límite del plan"
                    stroke="#64748b"
                    strokeWidth={2}
                    strokeDasharray="6 4"
                    dot={false}
                    isAnimationActive={false}
                  />
                ) : null}
              </LineChart>
            </ResponsiveContainer>
          )}
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
