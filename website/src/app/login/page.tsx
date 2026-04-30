import { Suspense } from 'react';
import LoginForm from './LoginForm';

export default function LoginPage() {
  return (
    <div className="min-h-[60vh] bg-gray-50">
      <Suspense
        fallback={
          <div className="flex min-h-[40vh] items-center justify-center text-sm text-gray-600">
            Cargando…
          </div>
        }
      >
        <LoginForm />
      </Suspense>
    </div>
  );
}
