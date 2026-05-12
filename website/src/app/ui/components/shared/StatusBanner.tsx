'use client';

import { AlertCircle, CheckCircle2, Info, AlertTriangle } from 'lucide-react';
import type { ReactNode } from 'react';

export type StatusVariant = 'info' | 'success' | 'warning' | 'error';

interface StatusBannerProps {
  variant: StatusVariant;
  title?: ReactNode;
  children: ReactNode;
  className?: string;
}

const VARIANT_STYLES: Record<
  StatusVariant,
  { wrapper: string; icon: typeof AlertCircle; iconClass: string }
> = {
  info: {
    wrapper:
      'border-(--color-info-subtle) bg-(--color-info-subtle) text-(--color-info-on)',
    icon: Info,
    iconClass: 'text-(--color-info)',
  },
  success: {
    wrapper:
      'border-(--color-success-subtle) bg-(--color-success-subtle) text-(--color-success-on)',
    icon: CheckCircle2,
    iconClass: 'text-(--color-success)',
  },
  warning: {
    wrapper:
      'border-(--color-warning-subtle) bg-(--color-warning-subtle) text-(--color-warning-on)',
    icon: AlertTriangle,
    iconClass: 'text-(--color-warning)',
  },
  error: {
    wrapper:
      'border-(--color-error-subtle) bg-(--color-error-subtle) text-(--color-error-on)',
    icon: AlertCircle,
    iconClass: 'text-(--color-error)',
  },
};

export default function StatusBanner({
  variant,
  title,
  children,
  className,
}: StatusBannerProps) {
  const config = VARIANT_STYLES[variant];
  const Icon = config.icon;
  const role = variant === 'error' ? 'alert' : 'status';
  const wrapperCls = [
    'flex items-start gap-3 rounded-lg border px-4 py-3 text-sm',
    config.wrapper,
    className ?? '',
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <div role={role} aria-live="polite" className={wrapperCls}>
      <Icon
        className={`mt-0.5 h-5 w-5 shrink-0 ${config.iconClass}`}
        aria-hidden
      />
      <div className="min-w-0 space-y-1">
        {title ? <p className="font-semibold">{title}</p> : null}
        <div className="leading-relaxed">{children}</div>
      </div>
    </div>
  );
}
