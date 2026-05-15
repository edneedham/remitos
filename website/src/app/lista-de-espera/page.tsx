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
    <div className="bg-gray-50 px-4 py-12 sm:py-16">
      <div className="mx-auto w-full max-w-md sm:max-w-xl lg:max-w-3xl xl:max-w-4xl rounded-2xl border border-gray-200 bg-white px-5 py-8 shadow-sm sm:px-8 sm:py-10 lg:px-10 lg:py-12">
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
