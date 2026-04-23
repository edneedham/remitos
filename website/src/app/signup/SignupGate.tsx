'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { QRCodeSVG } from 'qrcode.react';
import { isLikelyMobileDevice } from '../lib/mobileDevice';
import { getPublicSiteOrigin } from '../lib/siteUrl';
import SignupMarketingAside from './SignupMarketingAside';
import SignupTrialForm from './SignupTrialForm';

export default function SignupGate() {
  const router = useRouter();
  const [ready, setReady] = useState(false);
  const [mobileUrl, setMobileUrl] = useState('');

  useEffect(() => {
    if (isLikelyMobileDevice()) {
      router.replace('/signup/m');
      return;
    }
    const origin = getPublicSiteOrigin();
    setMobileUrl(`${origin}/signup/m`);
    setReady(true);
  }, [router]);

  if (!ready) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center bg-gray-50 px-4">
        <p className="text-gray-600">Cargando…</p>
      </div>
    );
  }

  return (
    <div className="bg-gray-50 px-4 py-10 lg:py-14">
      <div className="mx-auto flex w-fit min-w-0 max-w-full flex-col items-center gap-8 lg:flex-row lg:items-start lg:gap-10 xl:gap-12">
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
                Completá el registro en dos pasos: empresa y medio de pago para
                después del período de prueba.
              </p>
            </header>

            <div
              className="flex shrink-0 flex-col items-center justify-center rounded-xl px-5 py-4 shadow-lg"
              aria-label="Código QR para abrir el registro en el celular"
            >
              <p className="mb-3 text-center text-xs font-semibold uppercase tracking-wide text-gray-500">
                ¿Preferís el celular?
              </p>
              <QRCodeSVG value={mobileUrl} size={140} level="M" includeMargin />
              <p className="mt-3 max-w-[11rem] text-center text-xs text-gray-500">
                Escaneá para el mismo registro en tu teléfono.
              </p>
            </div>
          </div>

          <div className="min-h-0 min-w-0 flex-1">
            <SignupTrialForm variant="embedded" />
          </div>
        </section>
      </div>
    </div>
  );
}
