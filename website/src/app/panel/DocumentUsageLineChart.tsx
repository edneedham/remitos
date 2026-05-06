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

export type UsageChartDatum = {
  date: string;
  cumulative: number | null;
  limitLine: number | undefined;
};

type Props = {
  chartData: UsageChartDatum[];
  monthAxisLabel: string;
  yMax: number;
  yTicks: number[];
  limitValue: number | null;
};

function formatDay(d: string) {
  const x = new Date(`${d}T12:00:00.000Z`);
  return x.toLocaleDateString('es-AR', { day: '2-digit', timeZone: 'UTC' });
}

/** Split out so the parent can lazy-load recharts on the dashboard. */
export default function DocumentUsageLineChart({
  chartData,
  monthAxisLabel,
  yMax,
  yTicks,
  limitValue,
}: Props) {
  return (
    <ResponsiveContainer width="100%" height={260}>
      <LineChart
        data={chartData}
        margin={{ top: 8, right: 12, left: 8, bottom: 18 }}
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
          interval="equidistantPreserveStart"
          minTickGap={14}
          stroke="#9ca3af"
          height={64}
          tickMargin={10}
          label={{
            value: monthAxisLabel,
            position: 'insideBottom',
            dy: 16,
            style: {
              textAnchor: 'middle',
              fill: '#6b7280',
              fontSize: 12,
              textTransform: 'capitalize',
            },
          }}
        />
        <YAxis
          domain={[0, yMax]}
          ticks={yTicks}
          allowDecimals={false}
          tick={{ fontSize: 11 }}
          tickFormatter={(value) =>
            typeof value === 'number'
              ? value.toLocaleString('es-AR')
              : String(value)
          }
          stroke="#9ca3af"
          width={56}
          tickMargin={10}
          label={{
            value: 'Documentos',
            angle: -90,
            position: 'insideLeft',
            dx: -4,
            style: {
              textAnchor: 'middle',
              fill: '#6b7280',
              fontSize: 12,
            },
          }}
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
            if (name === 'limitLine') {
              const v = typeof value === 'number' ? value : Number(value);
              return [v, 'Límite del plan'];
            }
            if (value == null || value === '') {
              return ['—', 'Documentos'];
            }
            const v = typeof value === 'number' ? value : Number(value);
            return [Number.isFinite(v) ? v : '—', 'Documentos'];
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
          connectNulls={false}
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
  );
}
