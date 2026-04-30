'use client';

import Link from 'next/link';
import { Check } from 'lucide-react';
import type { TrialChecklistModel } from '../lib/trialOnboardingChecklist';

type Props = {
  model: TrialChecklistModel;
};

export default function TrialOnboardingChecklist({ model }: Props) {
  return (
    <section
      className="rounded-xl border border-blue-200 bg-gradient-to-b from-blue-50/80 to-white p-5 shadow-sm"
      aria-labelledby="trial-checklist-heading"
    >
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div className="min-w-0">
          <h2
            id="trial-checklist-heading"
            className="text-base font-semibold text-gray-900"
          >
            Seguí con la app Android
          </h2>
          <p className="mt-1 text-sm text-gray-600">
            Completá estos pasos para procesar tu primer remito. Este recordatorio
            desaparece cuando ya hay documentos sincronizados desde la app.
          </p>
        </div>
        <div className="flex shrink-0 flex-col items-stretch gap-2 sm:items-end">
          <span className="inline-flex items-center justify-center rounded-full bg-blue-100 px-3 py-1 text-xs font-semibold tabular-nums text-blue-900">
            {model.completedCount} de {model.total} listos
          </span>
          <Link
            href="/trial-started"
            className="text-center text-sm font-semibold text-blue-600 hover:text-blue-700 hover:underline sm:text-right"
          >
            Reanudar guía de inicio
          </Link>
        </div>
      </div>

      <ol className="mt-5 space-y-3">
        {model.steps.map((step, index) => (
          <li
            key={step.id}
            className={`flex gap-3 rounded-lg border p-3 ${
              step.done
                ? 'border-emerald-200 bg-emerald-50/60'
                : 'border-gray-200 bg-white'
            }`}
          >
            <div className="flex h-8 w-8 shrink-0 items-center justify-center">
              {step.done ? (
                <Check
                  className="h-6 w-6 text-emerald-600"
                  strokeWidth={2.5}
                  aria-hidden
                />
              ) : (
                <span
                  className="flex h-8 w-8 items-center justify-center rounded-full border-2 border-gray-300 text-xs font-bold text-gray-600"
                  aria-hidden
                >
                  {index + 1}
                </span>
              )}
            </div>
            <div className="min-w-0 flex-1 pt-0.5">
              <p className="font-medium text-gray-900">{step.title}</p>
              <p className="mt-1 text-sm leading-relaxed text-gray-600">
                {step.description}
              </p>
              {step.id === 'install' && !step.done ? (
                <Link
                  href="/dashboard/app"
                  className="mt-2 inline-block text-sm font-semibold text-blue-600 hover:text-blue-700 hover:underline"
                >
                  Ir a descargar la app
                </Link>
              ) : null}
              {step.id === 'login' && !step.done && model.steps[0]?.done ? (
                <p className="mt-2 text-xs text-gray-500">
                  Cuando abras la app e inicies sesión, vas a registrar el
                  dispositivo y este paso se marca solo.
                </p>
              ) : null}
            </div>
          </li>
        ))}
      </ol>
    </section>
  );
}
