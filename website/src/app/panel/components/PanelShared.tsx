'use client';

import type { ReactNode } from 'react';

type KeyValueItem = {
  label: string;
  value: ReactNode;
};

export function PanelKeyValueList({
  items,
  className = '',
}: {
  items: KeyValueItem[];
  className?: string;
}) {
  return (
    <dl className={`space-y-2.5 text-sm ${className}`.trim()}>
      {items.map((item) => (
        <div
          key={item.label}
          className="rounded-lg border border-gray-100 bg-gray-50 px-3 py-2.5 md:flex md:items-center md:justify-between md:gap-4 md:border-0 md:bg-transparent md:px-0 md:py-0"
        >
          <dt className="text-xs font-medium uppercase tracking-wide text-gray-500 md:text-sm md:normal-case md:tracking-normal md:text-gray-600">
            {item.label}
          </dt>
          <dd className="mt-1 text-sm font-semibold text-gray-900 md:mt-0 md:max-w-[min(100%,20rem)] md:text-right md:font-medium">
            {item.value}
          </dd>
        </div>
      ))}
    </dl>
  );
}

export function PanelStatTile({
  label,
  value,
  description,
  children,
  tone = 'default',
  className = '',
}: {
  label: string;
  value: ReactNode;
  description?: ReactNode;
  children?: ReactNode;
  tone?: 'default' | 'muted';
  className?: string;
}) {
  const toneClasses =
    tone === 'muted'
      ? 'border-gray-200 bg-gray-50'
      : 'border-gray-200 bg-white';

  return (
    <article className={`rounded-lg border p-4 ${toneClasses} ${className}`.trim()}>
      <p className="text-[11px] font-semibold uppercase tracking-wide text-gray-500">
        {label}
      </p>
      <p className="mt-2 text-2xl font-bold text-gray-900">{value}</p>
      {description ? <p className="mt-1 text-sm text-gray-600">{description}</p> : null}
      {children}
    </article>
  );
}
