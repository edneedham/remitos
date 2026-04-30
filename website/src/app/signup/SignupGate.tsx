'use client';

import { useEffect, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Loader2 } from 'lucide-react';
import { QRCodeSVG } from 'qrcode.react';
import { getApiBaseUrl } from '../lib/apiUrl';
import { getPublicSiteOrigin } from '../lib/siteUrl';
import { getWebAccessToken } from '../lib/webAuth';
import { trackTrialOnboardingEvent } from '../lib/trialOnboardingAnalytics';
import LoadingSpinner from '../ui/components/shared/LoadingSpinner';
import SignupMarketingAside from './SignupMarketingAside';
import SignupPlanSelector from './SignupPlanSelector';
import SignupTrialForm from './SignupTrialForm';

export default function SignupGate() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [ready, setReady] = useState(false);
  const [mobileUrl, setMobileUrl] = useState('');
  const [showPlanStep, setShowPlanStep] = useState(false);
  const [applyingPreselectedPlan, setApplyingPreselectedPlan] = useState(false);

  const preselectedPlanId = searchParams.get('plan');
  const hasPreselectedPlan =
    preselectedPlanId === 'pyme' || preselectedPlanId === 'empresa';

  useEffect(() => {
    const origin = getPublicSiteOrigin();
    setMobileUrl(`${origin}/signup`);
    setReady(true);
  }, []);

  if (!ready) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center bg-gray-50 px-4">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  return (
    <div className="bg-gray-50 px-4 py-10 lg:py-14">
      <div className="mx-auto w-fit min-w-0 max-w-full">
        {applyingPreselectedPlan ? (
          <div className="flex min-h-[18rem] w-full max-w-2xl items-center justify-center rounded-2xl border border-gray-200 bg-white px-6 py-10 text-center shadow-sm">
            <div className="space-y-3">
              <div className="flex justify-center">
                <Loader2 className="h-7 w-7 animate-spin text-blue-600" aria-hidden />
              </div>
              <p className="text-base font-semibold text-gray-900">Configurando tu plan…</p>
              <p className="text-sm text-gray-600">
                Estamos guardando tu selección para llevarte al panel.
              </p>
            </div>
          </div>
        ) : showPlanStep ? (
          <div className="flex justify-center">
            <SignupPlanSelector
              onSelectPlan={async (plan) => {
                if (plan.id === 'corporativo') {
                  router.push('/contact');
                  return Promise.resolve();
                }

                const api = getApiBaseUrl();
                const token = getWebAccessToken();
                if (!api || !token) {
                  throw new Error('missing auth session');
                }

                const res = await fetch(`${api}/auth/me/plan`, {
                  method: 'POST',
                  headers: {
                    'Content-Type': 'application/json',
                    Authorization: `Bearer ${token}`,
                  },
                  body: JSON.stringify({
                    plan_id: plan.id,
                    plan_name: plan.name,
                    monthly_price: plan.monthlyPrice,
                    billing_cycle: plan.customPricing ? 'custom' : 'monthly',
                    trial_days: 7,
                  }),
                });
                if (!res.ok) {
                  throw new Error('failed to save selected plan');
                }

                trackTrialOnboardingEvent('trial_started', {
                  source: 'signup_plan_selected',
                });
                router.push('/trial-started');
              }}
            />
          </div>
        ) : (
          <div className="mx-auto flex w-fit min-w-0 max-w-full flex-col items-center gap-8 lg:flex-row lg:items-start lg:gap-10 xl:gap-12 signup-step-enter">
            <div className="w-full max-w-[360px] shrink-0 lg:sticky lg:top-8 lg:z-10 lg:max-h-[calc(100vh-4rem)] lg:overflow-y-auto">
              <div className="overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-lg ring-1 ring-black/5">
                <SignupMarketingAside />
              </div>
            </div>

            <section className="flex min-h-0 min-w-0 w-full max-w-md shrink-0 flex-col rounded-2xl border border-gray-200 bg-white px-5 py-8 shadow-sm sm:px-6 lg:py-10">
              <div className="mb-0 flex shrink-0 flex-col gap-6 sm:flex-row sm:items-start sm:justify-between sm:gap-8">
                <header className="min-w-0 flex-1 space-y-3 sm:max-w-md">
                  <h1 className="text-2xl font-bold tracking-tight text-gray-900 sm:text-3xl">
                    Probá 7 días gratis
                  </h1>
                  <p className="text-sm leading-relaxed text-gray-600 break-words">
                    Completá los datos de empresa y cuenta para empezar tu prueba.
                  </p>
                </header>

                <div
                  className="flex shrink-0 flex-col items-center justify-center rounded-xl px-5 py-4 shadow-lg"
                  aria-label="Código QR para abrir el registro en el celular"
                >
                  <p className="mb-3 text-center text-xs font-semibold uppercase tracking-wide text-gray-500">
                    ¿Preferís el celular?
                  </p>
                  <div className="relative h-[140px] w-[140px]">
                    <QRCodeSVG
                      value={mobileUrl}
                      size={140}
                      level="H"
                      includeMargin={false}
                    />
                    <div
                      className="absolute inset-0 flex items-center justify-center"
                      aria-hidden
                    >
                      <div className="flex h-8 w-8 items-center justify-center rounded-full bg-white">
                        <img
                          src="/enpunto-simple.svg"
                          alt=""
                          className="h-5 w-5"
                          aria-hidden
                        />
                      </div>
                    </div>
                  </div>
                  <p className="mt-3 max-w-[11rem] text-center text-xs text-gray-500">
                    Escaneá para el mismo registro en tu teléfono.
                  </p>
                </div>
              </div>

              <div className="min-h-0 min-w-0 flex-1">
                <SignupTrialForm
                  variant="embedded"
                  onSignupSuccess={async () => {
                    if (!hasPreselectedPlan) {
                      setShowPlanStep(true);
                      return;
                    }

                    const selectedPlan = {
                      id: preselectedPlanId,
                      name: preselectedPlanId === 'pyme' ? 'PyME' : 'Empresa',
                      monthlyPrice:
                        preselectedPlanId === 'pyme' ? 'USD 29' : 'USD 59',
                      customPricing: false,
                    };

                    const api = getApiBaseUrl();
                    const token = getWebAccessToken();
                    if (!api || !token) {
                      setShowPlanStep(true);
                      return;
                    }

                    setApplyingPreselectedPlan(true);
                    const res = await fetch(`${api}/auth/me/plan`, {
                      method: 'POST',
                      headers: {
                        'Content-Type': 'application/json',
                        Authorization: `Bearer ${token}`,
                      },
                      body: JSON.stringify({
                        plan_id: selectedPlan.id,
                        plan_name: selectedPlan.name,
                        monthly_price: selectedPlan.monthlyPrice,
                        billing_cycle: 'monthly',
                        trial_days: 7,
                      }),
                    });

                    if (!res.ok) {
                      setApplyingPreselectedPlan(false);
                      setShowPlanStep(true);
                      return;
                    }

                    trackTrialOnboardingEvent('trial_started', {
                      source: 'signup_plan_preselected',
                    });
                    router.push('/trial-started');
                  }}
                />
              </div>
            </section>
          </div>
        )}
      </div>
    </div>
  );
}
