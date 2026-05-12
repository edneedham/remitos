'use client';

import { forwardRef, useId, useState } from 'react';
import type { InputHTMLAttributes } from 'react';
import { Eye, EyeOff } from 'lucide-react';

/* Shared text input for auth forms and the panel. Renders a stacked label +
 * input + (optional) inline error message; reproduces the visual the auth
 * forms were inlining ad-hoc (rounded-lg, gray border, brand-blue focus ring,
 * red border on error). */
type BaseProps = Omit<InputHTMLAttributes<HTMLInputElement>, 'type' | 'id'> & {
  id?: string;
  label: string;
  error?: string;
  /** Defaults to `text`. Use `password` to opt into the eye-toggle. */
  type?: InputHTMLAttributes<HTMLInputElement>['type'];
  /** Extra `className` merged onto the input element (e.g. `font-mono uppercase`). */
  inputClassName?: string;
};

const TextField = forwardRef<HTMLInputElement, BaseProps>(function TextField(
  { id, label, error, type = 'text', inputClassName, className, ...inputProps },
  ref,
) {
  const generatedId = useId();
  const fieldId = id ?? generatedId;
  const errorId = error ? `${fieldId}-error` : undefined;
  const isPassword = type === 'password';
  const [reveal, setReveal] = useState(false);
  const renderedType = isPassword ? (reveal ? 'text' : 'password') : type;

  const base =
    'w-full rounded-lg border px-4 py-3 focus:ring-2 disabled:opacity-50';
  const borderClasses = error
    ? 'border-red-400 focus:border-red-500 focus:ring-red-500'
    : 'border-gray-300 focus:border-blue-500 focus:ring-blue-500';
  const inputCls = [
    base,
    borderClasses,
    isPassword ? 'pr-12' : '',
    inputClassName ?? '',
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <div className={className}>
      <label
        htmlFor={fieldId}
        className="mb-1 block text-sm font-medium text-gray-700"
      >
        {label}
      </label>
      <div className={isPassword ? 'relative' : undefined}>
        <input
          {...inputProps}
          ref={ref}
          id={fieldId}
          type={renderedType}
          aria-invalid={error ? true : inputProps['aria-invalid']}
          aria-describedby={errorId ?? inputProps['aria-describedby']}
          className={inputCls}
        />
        {isPassword ? (
          <button
            type="button"
            onClick={() => setReveal((prev) => !prev)}
            aria-label={reveal ? 'Ocultar contraseña' : 'Mostrar contraseña'}
            aria-pressed={reveal}
            className="absolute inset-y-0 right-2 my-auto flex h-9 w-9 items-center justify-center rounded-md text-blue-600 transition-colors hover:bg-blue-50 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500/60"
          >
            {reveal ? (
              <EyeOff className="h-5 w-5" aria-hidden />
            ) : (
              <Eye className="h-5 w-5" aria-hidden />
            )}
          </button>
        ) : null}
      </div>
      {error ? (
        <p id={errorId} className="mt-1 text-sm text-red-600" role="alert">
          {error}
        </p>
      ) : null}
    </div>
  );
});

export default TextField;
