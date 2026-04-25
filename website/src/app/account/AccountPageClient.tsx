'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Loader2 } from 'lucide-react';
import { getApiBaseUrl } from '../lib/apiUrl';
import {
  clearWebSession,
  fetchWithWebAuth,
  hasWebSession,
  refreshWebSession,
} from '../lib/webAuth';

type Entitlement = {
  can_download_app: boolean;
  subscription_plan?: string;
  trial_ends_at?: string;
  subscription_expires_at?: string;
  company_status?: string;
  archived_at?: string;
};

function isPaidPlan(plan: string): boolean {
  switch (plan.toLowerCase().trim()) {
    case 'premium':
    case 'paid':
    case 'subscriber':
    case 'standard':
      return true;
    default:
      return false;
  }
}

export default function AccountPageClient() {
  const router = useRouter();
  const [ready, setReady] = useState(false);
  const [entitlement, setEntitlement] = useState<Entitlement | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!hasWebSession()) {
      router.replace('/login');
      return;
    }

    let cancelled = false;

    async function load() {
      const api = getApiBaseUrl();
      if (!api) {
        setError(
          'Falta configurar NEXT_PUBLIC_API_URL (URL del servidor de la API).',
        );
        setReady(true);
        return;
      }

      await refreshWebSession();

      const res = await fetchWithWebAuth('/auth/me/entitlement');
      if (cancelled) return;

      if (res.status === 401) {
        clearWebSession();
        router.replace('/login');
        return;
      }

      if (!res.ok) {
        const body = (await res.json().catch(() => ({}))) as {
          message?: string;
        };
        setError(
          body.message ||
            'No se pudieron obtener los datos de tu cuenta. Probá de nuevo más tarde.',
        );
        setReady(true);
        return;
      }

      const data = (await res.json()) as Entitlement;
      setEntitlement(data);
      setReady(true);
    }

    void load();
    return () => {
      cancelled = true;
    };
  }, [router]);

  if (!ready && !error) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center bg-gray-50">
        <Loader2 className="h-8 w-8 animate-spin text-blue-600" aria-hidden />
      </div>
    );
  }

  const now = Date.now();
  const plan = (entitlement?.subscription_plan ?? '').toLowerCase().trim();
  const trialEndMs = entitlement?.trial_ends_at
    ? Date.parse(entitlement.trial_ends_at)
    : Number.NaN;
  const paymentEndMs = entitlement?.subscription_expires_at
    ? Date.parse(entitlement.subscription_expires_at)
    : Number.NaN;
  const isArchived = Boolean(entitlement?.archived_at);
  const companyStatus = (entitlement?.company_status ?? 'active')
    .toLowerCase()
    .trim();
  const companyBillingInactive =
    companyStatus !== '' && companyStatus !== 'active';

  const hasActiveTrial =
    !isArchived &&
    !companyBillingInactive &&
    plan === 'trial' &&
    Number.isFinite(trialEndMs) &&
    trialEndMs > now;

  // Match backend billing.CompanyHasAppDownloadAccess: paid plans with no
  // subscription_expires_at are treated as an open-ended active period.
  const hasActivePaymentPeriod =
    !isArchived &&
    !companyBillingInactive &&
    isPaidPlan(plan) &&
    (!entitlement?.subscription_expires_at ||
      (Number.isFinite(paymentEndMs) && paymentEndMs > now));

  const isPaid = hasActivePaymentPeriod;
  const canDownload = entitlement?.can_download_app === true;

  const formatDateTime = (value?: string) => {
    if (!value) return '—';
    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) return '—';
    return parsed.toLocaleString('es-AR', {
      dateStyle: 'long',
      timeStyle: 'short',
    });
  };

  return (
    <div className="bg-gray-50 px-4 py-12">
      <div className="mx-auto max-w-2xl space-y-8">
        <header className="space-y-2">
          <h1 className="text-3xl font-bold tracking-tight text-gray-900">
            Administración de cuenta
          </h1>
          <p className="text-base leading-relaxed text-gray-600">
            Desde la web podés gestionar tu cuenta, suscripción y datos de la
            empresa. El uso diario en depósito (remitos, repartos, escaneo) es en
            la{' '}
            <strong className="font-medium text-gray-800">
              aplicación Android
            </strong>
            : iniciá sesión en el teléfono para habilitar el modo operativo.
          </p>
        </header>

        {error && (
          <div
            className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800"
            role="alert"
          >
            {error}
          </div>
        )}

        {entitlement && isArchived ? (
          <div
            className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900"
            role="status"
          >
            Esta empresa está archivada. La descarga de la app y el acceso
            completo pueden no estar disponibles.
          </div>
        ) : null}

        {entitlement && !isArchived && companyBillingInactive ? (
          <div
            className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900"
            role="status"
          >
            El estado de la empresa no es &quot;activo&quot;; revisá la cuenta o
            contactá soporte si necesitás reactivarla.
          </div>
        ) : null}

        <div className="grid gap-4 sm:grid-cols-2">
          <section className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
            <h2 className="text-lg font-semibold text-gray-900">
              Cuenta y suscripción
            </h2>
            <dl className="mt-4 space-y-3 text-sm">
              <div className="flex items-center justify-between gap-4">
                <dt className="text-gray-600">Estado pago</dt>
                <dd className="font-medium text-gray-900">
                  {isPaid ? 'Sí' : 'No'}
                </dd>
              </div>
              <div className="flex items-center justify-between gap-4">
                <dt className="text-gray-600">Período pago</dt>
                <dd className="text-right font-medium text-gray-900">
                  {hasActivePaymentPeriod
                    ? entitlement?.subscription_expires_at
                      ? `Hasta ${formatDateTime(entitlement.subscription_expires_at)}`
                      : 'Activo (sin vencimiento)'
                    : 'No activo'}
                </dd>
              </div>
              <div className="flex items-center justify-between gap-4">
                <dt className="text-gray-600">En prueba</dt>
                <dd className="font-medium text-gray-900">
                  {hasActiveTrial ? 'Sí' : 'No'}
                </dd>
              </div>
              <div className="flex items-center justify-between gap-4">
                <dt className="text-gray-600">Período de prueba</dt>
                <dd className="text-right font-medium text-gray-900">
                  {hasActiveTrial
                    ? `Hasta ${formatDateTime(entitlement?.trial_ends_at)}`
                    : 'No activo'}
                </dd>
              </div>
            </dl>
          </section>
          <section className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
            <h2 className="text-lg font-semibold text-gray-900">
              App en el depósito
            </h2>
            <p className="mt-2 text-sm text-gray-600">
              Iniciá sesión en la aplicación móvil con el mismo código de empresa
              y usuario para operar en depósito (remitos, repartos, escaneo).
            </p>
            {canDownload ? (
              <Link
                href="/download"
                className="mt-4 inline-block text-sm font-semibold text-blue-600 hover:text-blue-700 hover:underline"
              >
                Descargar la aplicación Android
              </Link>
            ) : (
              <p className="mt-4 text-sm text-gray-500">
                El enlace de descarga se habilita cuando tu cuenta está en prueba
                o con estado pago.
              </p>
            )}
          </section>
        </div>

      </div>
    </div>
  );
}
