'use client';

import {
  ArrowRight,
  Building,
  Building2,
  Check,
  ShieldCheck,
  Warehouse,
} from 'lucide-react';
import Link from 'next/link';
import { PLAN_CATALOG } from '../../../lib/planCatalog';

type WebsitePlan = {
  id: string;
  name: string;
  monthlyPrice: string;
  description: string;
  perks: string[];
  featured?: boolean;
  customPricing?: boolean;
  Icon: typeof Warehouse;
};

const ICON_BY_PLAN_ID: Record<WebsitePlan['id'], typeof Warehouse> = {
  pyme: Warehouse,
  empresa: Building2,
  corporativo: Building,
};

const PLANS: WebsitePlan[] = PLAN_CATALOG.map((plan) => ({
  id: plan.id,
  name: plan.name,
  monthlyPrice: plan.monthlyPriceLabel,
  description: plan.description,
  perks: [...plan.perks, `Excedentes: ${plan.overageLabel}`],
  featured: plan.featured,
  customPricing: plan.customPricing,
  Icon: ICON_BY_PLAN_ID[plan.id],
}));

export default function PricingPlansSection({
  showCtas = false,
}: {
  showCtas?: boolean;
}) {
  return (
    <section
      className="border-b border-gray-200 bg-white py-20 px-4 sm:px-6 lg:px-8"
      aria-labelledby="pricing-plans-heading"
    >
      <div className="mx-auto w-full max-w-[80vw]">
        <div className="mx-auto mb-12 max-w-content-prose text-center">
          <h2
            id="pricing-plans-heading"
            className="mb-4 text-3xl font-bold text-gray-900 sm:text-4xl lg:text-5xl"
          >
            Planes para tu suscripción
          </h2>
          <p className="text-lg leading-snug text-gray-600 sm:text-xl lg:text-2xl">
            Todos los planes incluyen 7 días de prueba gratis para empezar.
          </p>
        </div>

        <div className="mx-auto grid max-w-6xl gap-4 md:grid-cols-3">
          {PLANS.map((plan) => (
            <article
              key={plan.id}
              className={`relative flex h-full flex-col rounded-xl border p-5 sm:p-6 ${
                plan.featured
                  ? 'border-blue-500 bg-blue-50/30 ring-1 ring-blue-200'
                  : 'border-gray-200 bg-white'
              }`}
            >
              {plan.featured && (
                <span className="absolute left-1/2 -top-3 inline-flex w-fit -translate-x-1/2 rounded-full border border-blue-300 bg-white px-2.5 py-1 text-xs font-semibold text-blue-700 sm:text-sm">
                  Más Elegido
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
                <h3 className="text-lg font-semibold text-gray-900 sm:text-xl">
                  {plan.name}
                </h3>
              </div>
              <p className="mt-1 text-2xl font-bold text-gray-900 sm:text-3xl">
                {plan.monthlyPrice}
              </p>
              <p className="mt-1 text-xs text-gray-500 sm:text-sm">
                {plan.customPricing
                  ? 'Precio personalizado'
                  : 'USD / mes + IVA'}
              </p>
              <p className="mt-4 text-sm leading-relaxed text-gray-600 sm:text-base">
                {plan.description}
              </p>

              <ul className="mt-4 space-y-2 text-sm leading-snug text-gray-700 sm:text-base">
                {plan.perks.map((perk) => (
                  <li key={perk} className="flex items-start gap-2">
                    <Check className="mt-0.5 h-4 w-4 shrink-0 text-green-600" aria-hidden />
                    <span>{perk}</span>
                  </li>
                ))}
              </ul>

              {showCtas ? (
                <Link
                  href={
                    plan.customPricing
                      ? '/contact'
                      : `/signup?plan=${encodeURIComponent(plan.id)}`
                  }
                  className={`mt-6 inline-flex w-full items-center justify-center gap-1 rounded-lg px-4 py-2.5 text-sm font-semibold transition-colors sm:text-base sm:py-3 ${
                    plan.featured
                      ? 'bg-blue-600 text-white hover:bg-blue-700'
                      : 'bg-gray-900 text-white hover:bg-black'
                  }`}
                >
                  <span>
                    {plan.customPricing
                      ? 'Hablar con ventas'
                      : 'Comenzar prueba gratis'}
                  </span>
                  {!plan.customPricing && <ArrowRight className="h-4 w-4" aria-hidden />}
                </Link>
              ) : null}
            </article>
          ))}
        </div>

        <div className="mx-auto mt-6 flex max-w-6xl items-center justify-center gap-2 rounded-xl border border-blue-200 bg-blue-50 px-4 py-3 text-sm font-medium text-blue-800 sm:text-base sm:px-5 sm:py-3.5">
          <ShieldCheck className="h-4 w-4 shrink-0 sm:h-5 sm:w-5" aria-hidden />
          <span>Sin permanencia. Cancela cuando quieras.</span>
        </div>
        <p className="mx-auto mt-3 max-w-6xl text-center text-xs text-gray-500 sm:text-sm">
          Facturado en pesos argentinos.
        </p>
      </div>
    </section>
  );
}
