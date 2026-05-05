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
    <aside className="flex flex-col justify-start bg-[var(--color-brand)] px-8 pb-10 pt-8 text-white sm:pb-12 lg:px-10 lg:pb-12 lg:pt-10 xl:px-12">
      <div className="w-full space-y-10">
        <header className="space-y-4">
          <p className="text-xs font-semibold uppercase tracking-wider text-white/80">
            Período de prueba
          </p>
          <h2 className="text-3xl font-bold leading-tight sm:text-4xl">
            7 días gratis
          </h2>
          <p className="text-base leading-relaxed text-white/90">
            Probá En Punto con{' '}
            <strong className="font-semibold text-white">1 empresa</strong>, hasta{' '}
            <strong className="font-semibold text-white">2 depósitos</strong>,{' '}
            <strong className="font-semibold text-white">1 dispositivo por depósito</strong>{' '}
            y hasta <strong className="font-semibold text-white">500 documentos</strong>{' '}
            durante 7 días gratis.
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
