'use client';

import { forwardRef, useId } from 'react';
import type { TextareaHTMLAttributes } from 'react';

type BaseProps = Omit<TextareaHTMLAttributes<HTMLTextAreaElement>, 'id'> & {
  id?: string;
  label: string;
  error?: string;
  /** Extra `className` merged onto the textarea element. */
  textareaClassName?: string;
};

const TextAreaField = forwardRef<HTMLTextAreaElement, BaseProps>(
  function TextAreaField(
    {
      id,
      label,
      error,
      rows = 6,
      textareaClassName,
      className,
      ...textareaProps
    },
    ref,
  ) {
    const generatedId = useId();
    const fieldId = id ?? generatedId;
    const errorId = error ? `${fieldId}-error` : undefined;

    const base =
      'w-full resize-none rounded-lg border px-4 py-3 focus:ring-2 disabled:opacity-50';
    const borderClasses = error
      ? 'border-red-400 focus:border-red-500 focus:ring-red-500'
      : 'border-gray-300 focus:border-blue-500 focus:ring-blue-500';
    const areaCls = [base, borderClasses, textareaClassName ?? '']
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
        <textarea
          {...textareaProps}
          ref={ref}
          id={fieldId}
          rows={rows}
          aria-invalid={error ? true : textareaProps['aria-invalid']}
          aria-describedby={errorId ?? textareaProps['aria-describedby']}
          className={areaCls}
        />
        {error ? (
          <p id={errorId} className="mt-1 text-sm text-red-600" role="alert">
            {error}
          </p>
        ) : null}
      </div>
    );
  },
);

export default TextAreaField;
