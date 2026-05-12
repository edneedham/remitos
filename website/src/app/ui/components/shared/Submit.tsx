'use client';

import { useFormStatus } from 'react-dom';
import Button from './Button';

interface SubmitAreaProps {
  buttonText: string;
  submittingText?: string;
  isSubmitting?: boolean;
  disabled?: boolean;
}

export default function SubmitArea({
  buttonText,
  submittingText,
  isSubmitting: isSubmittingProp,
  disabled: disabledProp,
}: SubmitAreaProps) {
  const { pending } = useFormStatus();
  const isSubmitting = isSubmittingProp ?? pending;
  const disabled = disabledProp ?? pending;

  return (
    <div className="mt-6 mb-8">
      <Button
        type="submit"
        variant="primary"
        isLoading={isSubmitting}
        disabled={disabled}
        loadingLabel={submittingText ? <span>{submittingText}</span> : undefined}
      >
        {buttonText}
      </Button>
    </div>
  );
}
