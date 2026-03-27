'use client';

import { useFormStatus } from 'react-dom';
import LoadingSpinner from './LoadingSpinner';

interface SubmitAreaProps {
  buttonText: string;
  submittingText?: string;
  isSubmitting?: boolean;
  disabled?: boolean;
}

export default function SubmitArea({
  buttonText,
  submittingText = 'Procesando...',
  isSubmitting: isSubmittingProp,
  disabled: disabledProp,
}: SubmitAreaProps) {
  const { pending } = useFormStatus();
  const isSubmitting = isSubmittingProp ?? pending;
  const disabled = disabledProp ?? pending;

  const content = isSubmitting ? (
    <>
      <LoadingSpinner />
      <span className="ml-2">{submittingText}</span>
    </>
  ) : (
    buttonText
  );

  return (
    <button
      type="submit"
      disabled={disabled}
      className="w-full px-4 py-2 mt-6 flex justify-center items-center bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:bg-gray-400 disabled:cursor-not-allowed min-h-[42px] mb-8"
    >
      {content}
    </button>
  );
}
