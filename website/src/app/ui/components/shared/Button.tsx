'use client';

import { forwardRef } from 'react';
import type { ButtonHTMLAttributes, ReactNode } from 'react';
import { Loader2 } from 'lucide-react';

type Variant = 'primary' | 'secondary' | 'destructive';

type CommonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  /** Shows a spinner instead of the label when true. Also disables the button. */
  isLoading?: boolean;
  /** Optional fully-controlled label override for when isLoading is true. */
  loadingLabel?: ReactNode;
};

type ButtonProps = CommonProps & {
  variant?: Variant;
};

const BASE =
  'inline-flex w-full items-center justify-center rounded-lg px-4 py-3 text-base font-semibold transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 disabled:cursor-not-allowed';

const VARIANT_CLASSES: Record<Variant, string> = {
  primary:
    'bg-blue-600 text-white hover:bg-blue-700 active:bg-blue-800 focus-visible:ring-blue-500/60 disabled:bg-(--color-button-disabled-bg) disabled:text-(--color-button-disabled-content) disabled:hover:bg-(--color-button-disabled-bg)',
  secondary:
    'border border-gray-300 bg-white text-gray-900 hover:bg-gray-50 active:bg-gray-100 focus-visible:ring-blue-500/60 disabled:bg-(--color-button-disabled-bg) disabled:text-(--color-button-disabled-content) disabled:border-(--color-button-disabled-bg)',
  destructive:
    'bg-red-600 text-white hover:bg-red-700 active:bg-red-700 focus-visible:ring-red-500/60 disabled:bg-(--color-button-disabled-bg) disabled:text-(--color-button-disabled-content)',
};

const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  {
    variant = 'primary',
    isLoading = false,
    loadingLabel,
    className,
    disabled,
    children,
    type,
    ...rest
  },
  ref,
) {
  const classes = [BASE, VARIANT_CLASSES[variant], className ?? '']
    .filter(Boolean)
    .join(' ');
  return (
    <button
      ref={ref}
      type={type ?? 'button'}
      className={classes}
      disabled={Boolean(disabled) || isLoading}
      {...rest}
    >
      {isLoading ? (
        loadingLabel ?? <Loader2 className="h-5 w-5 animate-spin" aria-hidden />
      ) : (
        children
      )}
    </button>
  );
});

export default Button;
