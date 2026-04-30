'use client';

import { useState } from 'react';
import {
  Building,
  Building2,
  Check,
  ShieldCheck,
  Sparkles,
  Warehouse,
} from 'lucide-react';
import { BILLING_LEGAL_NOTICE_AR } from '../lib/billingLegalNotice';
import { PLAN_CATALOG } from '../lib/planCatalog';

export type SignupPlan = {
  id: string;
  name: string;
  monthlyPrice: string;
  description: string;
  perks: string[];
  featured?: boolean;
  customPricing?: boolean;
  Icon: typeof Warehouse;
};

const ICON_BY_PLAN_ID: Record<SignupPlan['id'], typeof Warehouse> = {
  pyme: Warehouse,
  empresa: Building2,
  corporativo: Building,
};

const PLANS: SignupPlan[] = PLAN_CATALOG.map((plan) => ({
  id: plan.id,
  name: plan.name,
  monthlyPrice: plan.monthlyPriceLabel,
  description: plan.description,
  perks: [...plan.perks, `Excedentes: ${plan.overageLabel}`],
  featured: plan.featured,
  customPricing: plan.customPricing,
  Icon: ICON_BY_PLAN_ID[plan.id],
}));

export default function SignupPlanSelector({
  onSelectPlan,
}: {
  onSelectPlan: (plan: SignupPlan) => Promise<void>;
}) {
  const [selectingPlanId, setSelectingPlanId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function handleSelectPlan(plan: SignupPlan) {
    setError(null);
    setSelectingPlanId(plan.id);
    try {
      await onSelectPlan(plan);
    } catch {
      setError('No pudimos guardar el plan. Probá de nuevo.');
      setSelectingPlanId(null);
    }
  }

  return (
    <section
      className="w-full max-w-5xl rounded-2xl border border-gray-200 bg-white p-6 shadow-sm signup-step-enter sm:p-8"
      aria-label="Seleccionar plan"
    >
      <header className="mb-8 text-center">
        <p className="inline-flex items-center gap-2 rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold uppercase tracking-wide text-blue-700">
          <Sparkles className="h-3.5 w-3.5" aria-hidden />
          Paso 2 de 2
        </p>
        <h2 className="mt-4 text-2xl font-bold tracking-tight text-gray-900 sm:text-3xl">
          Elegí tu plan
        </h2>
        <p className="mt-2 text-sm text-gray-600 sm:text-base">
          Tu cuenta ya fue creada. Todos los planes incluyen 7 días de prueba gratis.
        </p>
      </header>

      <div className="grid gap-4 md:grid-cols-3">
        {PLANS.map((plan) => (
          <article
            key={plan.id}
            className={`relative flex h-full flex-col rounded-xl border p-5 ${
              plan.featured
                ? 'border-blue-500 bg-blue-50/30 ring-1 ring-blue-200'
                : 'border-gray-200 bg-white'
            }`}
          >
            {plan.featured && (
              <span className="absolute left-1/2 -top-3 inline-flex w-fit -translate-x-1/2 rounded-full border border-blue-300 bg-white px-2.5 py-1 text-xs font-semibold text-blue-700">
                Recomendado
              </span>
            )}

            <div className="mb-1 flex items-center gap-2">
              <plan.Icon
                className={`text-gray-700 ${
                  plan.id === 'pyme'
                    ? 'h-4 w-4'
                    : plan.id === 'empresa'
                      ? 'h-5 w-5'
                      : 'h-6 w-6'
                }`}
                aria-hidden
              />
              <h3 className="text-lg font-semibold text-gray-900">{plan.name}</h3>
            </div>
            <p className="mt-1 text-2xl font-bold text-gray-900">{plan.monthlyPrice}</p>
            <p className="mt-1 text-xs text-gray-500">
              {plan.customPricing
                ? 'Precio personalizado'
                : 'USD / mes + IVA'}
            </p>
            <p className="mt-4 text-sm text-gray-600">{plan.description}</p>

            <ul className="mt-4 space-y-2 text-sm text-gray-700">
              {plan.perks.map((perk) => (
                <li key={perk} className="flex items-start gap-2">
                  <Check className="mt-0.5 h-4 w-4 shrink-0 text-green-600" aria-hidden />
                  <span>{perk}</span>
                </li>
              ))}
            </ul>

            <button
              type="button"
              onClick={() => {
                void handleSelectPlan(plan);
              }}
              disabled={Boolean(selectingPlanId)}
              className={`mt-6 inline-flex w-full items-center justify-center rounded-lg px-4 py-2.5 text-sm font-semibold transition-colors ${
                plan.featured
                  ? 'bg-blue-600 text-white hover:bg-blue-700'
                  : 'bg-gray-900 text-white hover:bg-black'
              } disabled:pointer-events-none disabled:opacity-60`}
            >
              {selectingPlanId === plan.id
                ? 'Guardando...'
                : plan.customPricing
                  ? 'Hablar con ventas'
                  : 'Comenzar prueba gratis'}
            </button>
          </article>
        ))}
      </div>

      {error && (
        <p className="mt-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-center text-sm text-red-700">
          {error}
        </p>
      )}

      <div className="mt-6 flex items-center justify-center gap-2 rounded-xl border border-blue-200 bg-blue-50 px-4 py-3 text-sm font-medium text-blue-800">
        <ShieldCheck className="h-4 w-4 shrink-0" aria-hidden />
        <span>Sin permanencia. Cancela cuando quieras.</span>
      </div>
      <p className="mx-auto mt-3 max-w-2xl text-center text-xs leading-relaxed text-gray-500">
        {BILLING_LEGAL_NOTICE_AR}
      </p>
    </section>
  );
}
