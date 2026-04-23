import type { Metadata } from 'next';
import SignupTrialForm from '../SignupTrialForm';

export const metadata: Metadata = {
  title: 'Registro (móvil) | En Punto',
  description: 'Probá 7 días gratis con En Punto desde tu teléfono.',
};

export default function MobileSignupPage() {
  return (
    <div className="min-h-[70vh] bg-gray-50 px-4 py-10">
      <div className="mx-auto w-full max-w-sm">
        <div className="text-center mb-8">
          <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 mb-2">
            Probá 7 días gratis
          </h1>
          <p className="text-gray-600 text-sm sm:text-base">
            Completá los datos para arrancar con En Punto.
          </p>
        </div>
        <div className="rounded-xl bg-white border border-gray-200 p-6 shadow-sm sm:p-8">
          <SignupTrialForm variant="embedded" />
        </div>
      </div>
    </div>
  );
}
