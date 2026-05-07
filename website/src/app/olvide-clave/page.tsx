import { Suspense } from 'react';
import LoadingSpinner from '../ui/components/shared/LoadingSpinner';
import ForgotPasswordForm from './ForgotPasswordForm';

export default function ForgotPasswordPage() {
  return (
    <Suspense
      fallback={
        <div className="flex min-h-[60vh] items-center justify-center bg-gray-50 px-4">
          <LoadingSpinner size="lg" />
        </div>
      }
    >
      <ForgotPasswordForm />
    </Suspense>
  );
}
