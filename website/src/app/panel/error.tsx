'use client';

import Link from 'next/link';
import { useEffect } from 'react';

export default function PanelError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <div className="mx-auto max-w-lg px-4 py-12">
      <div className="rounded-2xl border border-red-100 bg-white p-8 text-center shadow-sm">
        <h1 className="text-lg font-bold text-gray-900">Error en el panel</h1>
        <p className="mt-2 text-sm text-gray-600">
          No pudimos mostrar esta sección. Probá de nuevo o volvé al panel.
        </p>
        <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:justify-center">
          <button
            type="button"
            onClick={() => reset()}
            className="rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-blue-700"
          >
            Reintentar
          </button>
          <Link
            href="/panel"
            className="rounded-lg border border-gray-300 bg-white px-4 py-2.5 text-sm font-semibold text-gray-800 hover:bg-gray-50"
          >
            Ir al panel
          </Link>
        </div>
      </div>
    </div>
  );
}
