import type { Metadata } from 'next';
import { Suspense } from 'react';
import LoadingSpinner from '../ui/components/shared/LoadingSpinner';
import WaitlistForm from './WaitlistForm';

export const metadata: Metadata = {
  title: 'Lista de espera | En Punto',
  description:
    'Dejanos tu correo y te avisamos cuando abramos el registro a En Punto.',
};

export default function WaitlistPage() {
  return (
    <div className="min-h-svh bg-white md:min-h-0 md:bg-gray-50 md:px-4 md:py-12 lg:py-16">
      <div className="w-full max-md:max-w-none px-3 pb-6 pt-4 sm:px-4 md:mx-auto md:max-w-xl md:rounded-2xl md:border md:border-gray-200 md:bg-white md:px-8 md:py-10 md:shadow-sm lg:max-w-3xl lg:px-10 lg:py-12 xl:max-w-4xl">
        <Suspense
          fallback={
            <div className="flex justify-center py-10">
              <LoadingSpinner size="md" />
            </div>
          }
        >
          <WaitlistForm />
        </Suspense>
      </div>
    </div>
  );
}
