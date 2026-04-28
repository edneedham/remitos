'use client';

import { useEffect, useState } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import { isLikelyMobileDevice } from '../../../lib/mobileDevice';
import { getPublicSiteOrigin } from '../../../lib/siteUrl';

export default function HeroQrOverlay() {
  const [visible, setVisible] = useState(false);
  const [qrUrl, setQrUrl] = useState('');

  useEffect(() => {
    if (isLikelyMobileDevice()) {
      setVisible(false);
      return;
    }
    setQrUrl(`${getPublicSiteOrigin()}/signup/m`);
    setVisible(true);
  }, []);

  if (!visible || !qrUrl) {
    return null;
  }

  return (
    <div
      className="pointer-events-auto absolute bottom-[10%] right-3 z-10 w-[min(38%,168px)] max-w-[168px] rounded-2xl border border-gray-200/90 bg-white p-2.5 shadow-[0_12px_40px_-8px_rgba(0,0,0,0.35)] ring-2 ring-white sm:bottom-[12%] lg:-right-5 lg:bottom-[14%]"
      role="region"
      aria-label="Registro móvil con código QR"
    >
      <div className="flex flex-col items-center gap-2">
        <div className="relative h-24 w-24">
          <QRCodeSVG
            value={qrUrl}
            size={96}
            // Logo overlay needs higher resilience; H keeps scans reliable.
            level="H"
            includeMargin={false}
          />
          <div
            className="absolute inset-0 flex items-center justify-center"
            aria-hidden
          >
            <div className="flex h-6 w-6 items-center justify-center rounded-full bg-white">
              <img
                src="/enpunto-simple.svg"
                alt=""
                className="h-4 w-4"
                aria-hidden
              />
            </div>
          </div>
        </div>
        <p className="text-center text-[11px] font-semibold leading-snug text-gray-900 sm:text-xs">
          Escaneá y probalo gratis por 7 días.
        </p>
      </div>
    </div>
  );
}
