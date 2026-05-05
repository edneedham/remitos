import { Suspense } from 'react';
import LoadingSpinner from '../ui/components/shared/LoadingSpinner';
import ForgotPasswordForm from './ForgotPasswordForm';

export default function ForgotPasswordPage() {
  return (
    <div className="min-h-[60vh] bg-gray-50">
      <Suspense
        fallback={
          <div className="flex min-h-[40vh] items-center justify-center">
            <LoadingSpinner size="lg" />
          </div>
        }
      >
        <ForgotPasswordForm />
      </Suspense>
    </div>
  );
}
