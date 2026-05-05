import { Suspense } from 'react';
import LoadingSpinner from '../ui/components/shared/LoadingSpinner';
import LoginForm from './LoginForm';

export default function LoginPage() {
  return (
    <div className="min-h-[60vh] bg-gray-50">
      <Suspense
        fallback={
          <div className="flex min-h-[40vh] items-center justify-center">
            <LoadingSpinner size="lg" />
          </div>
        }
      >
        <LoginForm />
      </Suspense>
    </div>
  );
}
