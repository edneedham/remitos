'use client';

import { Check } from 'lucide-react';

const benefits = [
  'Escanear remitos y extraer datos con menos carga manual',
  'Armar listas de reparto y asignar bultos al chofer',
  'Generar un PDF claro para la ruta y la firma en destino',
  'Exportar información para contabilidad u otras herramientas',
];

/**
 * Left column for desktop signup: brand blue background, trial + product benefits.
 */
export default function SignupMarketingAside() {
  return (
    <aside className="flex flex-col justify-center bg-[var(--color-brand)] px-8 py-10 text-white sm:py-12 lg:px-10 lg:py-12 xl:px-12">
      <div className="mx-auto max-w-md space-y-10">
        <header className="space-y-4">
          <p className="text-xs font-semibold uppercase tracking-wider text-white/80">
            Período de prueba
          </p>
          <h2 className="text-3xl font-bold leading-tight sm:text-4xl">
            7 días gratis
          </h2>
          <p className="text-base leading-relaxed text-white/90">
            Probá En Punto con{' '}
            <strong className="font-semibold text-white">1 empresa</strong>,{' '}
            <strong className="font-semibold text-white">1 depósito</strong> y hasta{' '}
            <strong className="font-semibold text-white">2 usuarios</strong>. Podés
            cargar una tarjeta para el cobro después de la prueba —{' '}
            <span className="font-medium text-white">no se cobra durante los 7 días</span>.
          </p>
        </header>

        <section className="space-y-4 border-t border-white/20 pt-10">
          <h3 className="text-lg font-semibold tracking-tight">
            Lo que vas a poder hacer
          </h3>
          <ul className="space-y-3">
            {benefits.map((line) => (
              <li
                key={line}
                className="flex items-start gap-3 text-sm leading-snug text-white/95"
              >
                <span className="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-white/15">
                  <Check className="h-4 w-4 text-white" aria-hidden />
                </span>
                <span>{line}</span>
              </li>
            ))}
          </ul>
        </section>
      </div>
    </aside>
  );
}
