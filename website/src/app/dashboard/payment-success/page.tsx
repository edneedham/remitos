import type { Metadata } from 'next';
import { Suspense } from 'react';
import LoadingSpinner from '../../ui/components/shared/LoadingSpinner';
import PaymentSuccessPageClient from './PaymentSuccessPageClient';

export const metadata: Metadata = {
  title: 'Pago confirmado | En Punto',
  description: 'Confirmación de pago registrado correctamente.',
};

export default function PaymentSuccessPage() {
  return (
    <Suspense
      fallback={
        <div className="flex min-h-[40vh] items-center justify-center px-4">
          <LoadingSpinner size="lg" />
        </div>
      }
    >
      <PaymentSuccessPageClient />
    </Suspense>
  );
}
