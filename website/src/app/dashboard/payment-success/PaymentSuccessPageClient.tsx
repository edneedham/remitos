'use client';

import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { useEffect } from 'react';
import { CheckCircle2 } from 'lucide-react';
import { getPlanById } from '../../lib/planCatalog';
import {
  hasWebSession,
  refreshWebSession,
} from '../../lib/webAuth';

type PaymentSuccessContexto = 'activacion' | 'renovacion' | 'otro';

function normalizeContexto(raw: string | null): PaymentSuccessContexto {
  if (raw === 'activacion' || raw === 'renovacion') return raw;
  return 'otro';
}

export default function PaymentSuccessPageClient() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const contexto = normalizeContexto(searchParams.get('contexto'));
  const planId = searchParams.get('plan')?.trim().toLowerCase() ?? '';
  const planMeta =
    planId === 'pyme' || planId === 'empresa'
      ? getPlanById(planId)
      : null;

  useEffect(() => {
    if (!hasWebSession()) {
      router.replace('/login');
      return;
    }
    void refreshWebSession();
  }, [router]);

  let heading = 'Pago confirmado';
  let body: string;

  switch (contexto) {
    case 'activacion':
      heading = 'Suscripción activada';
      body =
        'El pago se registró correctamente y tu suscripción ya está activa. Podés seguir usando el panel y la aplicación según tu plan.';
      break;
    case 'renovacion':
      heading = 'Renovación registrada';
      body =
        'El pago se registró correctamente y tu período de suscripción fue actualizado.';
      break;
    default:
      body =
        'El pago se registró correctamente. Si tenés dudas, revisá Facturación o contactá soporte.';
  }

  return (
    <div className="mx-auto max-w-lg px-4 py-12 sm:px-6 lg:px-8">
      <div className="rounded-2xl border border-green-200 bg-white p-8 text-center shadow-sm">
        <div className="flex justify-center">
          <CheckCircle2
            className="h-16 w-16 text-green-600"
            aria-hidden
            strokeWidth={1.75}
          />
        </div>
        <h1 className="mt-6 text-2xl font-bold tracking-tight text-gray-900">
          {heading}
        </h1>
        <p className="mt-3 text-sm leading-relaxed text-gray-600">{body}</p>
        {planMeta ? (
          <p className="mt-4 rounded-lg bg-gray-50 px-4 py-3 text-sm text-gray-800">
            Plan:{' '}
            <span className="font-semibold text-gray-900">{planMeta.name}</span>
            <span className="block text-xs font-normal text-gray-600">
              {planMeta.monthlyPriceLabel} / mes + IVA · listado en USD, cobro en
              ARS según tipo de cambio al facturar.
            </span>
          </p>
        ) : null}
        <div className="mt-8 flex flex-col gap-3 sm:flex-row sm:justify-center">
          <Link
            href="/dashboard"
            className="inline-flex items-center justify-center rounded-lg bg-blue-600 px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-blue-700"
          >
            Ir al panel
          </Link>
          <Link
            href="/dashboard/billing"
            className="inline-flex items-center justify-center rounded-lg border border-gray-300 bg-white px-5 py-2.5 text-sm font-semibold text-gray-800 transition-colors hover:bg-gray-50"
          >
            Ver facturación
          </Link>
        </div>
      </div>
    </div>
  );
}
