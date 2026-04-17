'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { QRCodeSVG } from 'qrcode.react';
import { isLikelyMobileDevice } from '../lib/mobileDevice';
import { getPublicSiteOrigin } from '../lib/siteUrl';
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
    <div className="min-h-[70vh] bg-gray-50 px-4 py-16">
      <div className="max-w-5xl mx-auto">
        <div className="text-center mb-10">
          <h1 className="text-3xl sm:text-4xl font-bold text-gray-900 mb-3">
            Creá tu cuenta
          </h1>
          <p className="text-lg text-gray-600 max-w-2xl mx-auto">
            Probá gratis 7 días con 1 depósito y hasta 2 usuarios. Cargá una
            tarjeta para el cobro posterior a la prueba (no se cobra durante los
            7 días).
          </p>
        </div>

        <div className="grid gap-12 lg:grid-cols-2 lg:items-start">
          <SignupTrialForm />

          <div className="text-center lg:text-left">
            <h2 className="text-lg font-semibold text-gray-900 mb-2">
              Preferís el celular?
            </h2>
            <p className="text-gray-600 mb-6 text-sm sm:text-base">
              Escaneá el código para abrir el registro en tu teléfono o copiá el
              enlace.
            </p>
            <div className="inline-block rounded-2xl bg-white p-6 shadow-sm ring-1 ring-gray-200">
              <QRCodeSVG value={mobileUrl} size={220} level="M" includeMargin />
            </div>
            <p className="mt-6 text-sm text-gray-500 break-all">{mobileUrl}</p>
          </div>
        </div>
      </div>
    </div>
  );
}
