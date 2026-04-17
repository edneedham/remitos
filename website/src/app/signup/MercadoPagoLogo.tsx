'use client';

import { useState } from 'react';

/**
 * Add your official asset under `public/brands/` per Mercado Pago brand rules.
 * Prefer **SVG** (sharp at any size, small file); use **PNG** if MP only ships raster.
 * Files tried in order: `mercadopago-logo.svg`, then `mercadopago-logo.png`.
 */
export default function MercadoPagoLogo() {
  const [phase, setPhase] = useState(0);
  const sources = [
    '/brands/MercadoPagoLogo.svg',
    '/brands/mercadopago-logo.svg',
    '/brands/mercadopago-logo.png',
  ];

  if (phase >= sources.length) {
    return (
      <p className="text-center text-xl font-medium tracking-tight text-gray-600">
        Mercado Pago
      </p>
    );
  }

  return (
    // eslint-disable-next-line @next/next/no-img-element -- optional user assets; graceful fallback
    <img
      src={sources[phase]}
      alt="Mercado Pago"
      className="mx-auto block h-[54px] w-auto max-w-[330px] object-contain object-center"
      width={222}
      height={60}
      onError={() => setPhase((p) => p + 1)}
    />
  );
}
