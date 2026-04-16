'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { QRCodeSVG } from 'qrcode.react';
import { isLikelyMobileDevice } from '../lib/mobileDevice';
import { getPublicSiteOrigin } from '../lib/siteUrl';

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
      <div className="max-w-lg mx-auto text-center">
        <h1 className="text-3xl sm:text-4xl font-bold text-gray-900 mb-4">
          Registrate desde tu celular
        </h1>
        <p className="text-lg text-gray-600 mb-10">
          Escaneá este código QR con la cámara de tu teléfono para abrir la
          versión móvil del registro.
        </p>

        <div className="inline-block rounded-2xl bg-white p-6 shadow-sm ring-1 ring-gray-200">
          <QRCodeSVG value={mobileUrl} size={240} level="M" includeMargin />
        </div>

        <p className="mt-8 text-sm text-gray-500 break-all">{mobileUrl}</p>
        <p className="mt-4 text-sm text-gray-500">
          También podés copiar el enlace y abrirlo en el navegador del celular.
        </p>
      </div>
    </div>
  );
}
