import type { Metadata } from 'next';
import { Suspense } from 'react';
import SignupGate from './SignupGate';

export const metadata: Metadata = {
  title: 'Registro | En Punto',
  description: 'Probá 7 días gratis con En Punto.',
};

export default function SignupPage() {
  return (
    <Suspense
      fallback={
        <div className="flex min-h-[60vh] items-center justify-center bg-gray-50 px-4">
          <p className="text-gray-600">Cargando…</p>
        </div>
      }
    >
      <SignupGate />
    </Suspense>
  );
}
