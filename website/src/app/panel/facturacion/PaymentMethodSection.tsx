'use client';

import dynamic from 'next/dynamic';
import Image from 'next/image';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Loader2 } from 'lucide-react';
import { initMercadoPago } from '@mercadopago/sdk-react';
import { postWithWebAuth } from '../../lib/webAuth';

const CardPayment = dynamic(
  () => import('@mercadopago/sdk-react').then((m) => m.CardPayment),
  {
    ssr: false,
    loading: () => (
      <p className="text-sm text-gray-600">Cargando formulario de pago…</p>
    ),
  },
);

const mpPublicKey =
  typeof process !== 'undefined'
    ? (process.env.NEXT_PUBLIC_MERCADOPAGO_PUBLIC_KEY ?? '').trim()
    : '';

const useMockPayment =
  typeof process !== 'undefined' &&
  process.env.NEXT_PUBLIC_SIGNUP_USE_MOCK_PAYMENT === 'true';

/**
 * Flat renewal-style tokenization amount for Mercado Pago Bricks (whole pesos).
 * Listing USD prices use fractional amounts; updating card on file uses a nominal ARS floor.
 */
const CARD_UPDATE_NOMINAL_ARS = 100;

function useDesktopPaymentUi(): boolean {
  const [desktop, setDesktop] = useState(true);

  useEffect(() => {
    if (typeof window === 'undefined') return;
    const mq = window.matchMedia('(min-width: 768px)');
    const sync = () => setDesktop(mq.matches);
    sync();
    mq.addEventListener('change', sync);
    return () => mq.removeEventListener('change', sync);
  }, []);

  return desktop;
}

export default function PaymentMethodSection({
  canManage,
  payerEmail,
}: {
  canManage: boolean;
  payerEmail: string;
}) {
  const desktopPaymentUi = useDesktopPaymentUi();
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const mpInitRef = useRef(false);

  useEffect(() => {
    if (!desktopPaymentUi || !mpPublicKey || mpInitRef.current || useMockPayment)
      return;
    initMercadoPago(mpPublicKey);
    mpInitRef.current = true;
  }, [desktopPaymentUi]);

  const cardInitialization = useMemo(
    () => ({
      amount: CARD_UPDATE_NOMINAL_ARS,
      payer: { email: payerEmail.trim() },
    }),
    [payerEmail],
  );

  const handleCardSubmit = useCallback(
    async (data: { token?: string }) => {
      const token = data.token?.trim();
      if (!token) {
        setError('No recibimos el token de la tarjeta.');
        throw new Error('missing token');
      }
      setError(null);
      setMessage(null);
      setSubmitting(true);
      try {
        const res = await postWithWebAuth('/auth/me/payment-method', {
          card_token: token,
        });
        const body = (await res.json().catch(() => ({}))) as {
          message?: string;
        };
        if (!res.ok) {
          const msg =
            body.message ||
            'No pudimos guardar la tarjeta. Probá de nuevo.';
          setError(msg);
          throw new Error(msg);
        }
        setMessage(body.message || 'Medio de pago actualizado.');
      } finally {
        setSubmitting(false);
      }
    },
    [],
  );

  async function submitMock() {
    setError(null);
    setMessage(null);
    setSubmitting(true);
    try {
      const res = await postWithWebAuth('/auth/me/payment-method', {
        use_mock_payment: true,
      });
      const body = (await res.json().catch(() => ({}))) as { message?: string };
      if (!res.ok) {
        setError(body.message || 'No se pudo actualizar el medio de pago.');
        return;
      }
      setMessage(body.message || 'Medio de pago actualizado.');
    } finally {
      setSubmitting(false);
    }
  }

  if (!canManage) {
    return (
      <section
        className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm"
        aria-labelledby="payment-method-heading"
      >
        <h2
          id="payment-method-heading"
          className="text-lg font-semibold text-gray-900"
        >
          Medio de pago
        </h2>
        <p className="mt-2 text-sm text-gray-600">
          Tu rol solo permite ver facturación. Pedile al titular de la cuenta que actualice la
          tarjeta si hace falta.
        </p>
      </section>
    );
  }

  if (!desktopPaymentUi) {
    return null;
  }

  const showBrick =
    !useMockPayment && Boolean(mpPublicKey) && payerEmail.trim().length > 0;

  return (
    <section
      className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm"
      aria-labelledby="payment-method-heading"
    >
      <h2
        id="payment-method-heading"
        className="text-lg font-semibold text-gray-900"
      >
        Medio de pago (Mercado Pago)
      </h2>
      <p className="mt-2 text-sm text-gray-600">
        Reemplazá la tarjeta guardada para renovaciones y cobros prorrateados, sin cambiar de plan.
      </p>

      {message ? (
        <p
          className="mt-4 rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-900"
          role="status"
        >
          {message}
        </p>
      ) : null}
      {error ? (
        <p
          className="mt-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800"
          role="alert"
        >
          {error}
        </p>
      ) : null}

      {!useMockPayment && !mpPublicKey ? (
        <p className="mt-4 text-sm text-amber-800" role="alert">
          Falta configurar{' '}
          <code className="rounded bg-gray-100 px-1 text-xs">
            NEXT_PUBLIC_MERCADOPAGO_PUBLIC_KEY
          </code>{' '}
          en el sitio.
        </p>
      ) : null}

      {!useMockPayment && mpPublicKey && !payerEmail.trim() ? (
        <p className="mt-4 text-sm text-amber-800" role="alert">
          No hay email en tu cuenta para Mercado Pago. Contactá soporte.
        </p>
      ) : null}

      {useMockPayment ? (
        <div className="mt-6 space-y-3">
          <p className="text-sm text-gray-600">
            Modo desarrollo: simula guardar tarjeta sin cobro real.
          </p>
          <button
            type="button"
            disabled={submitting}
            onClick={() => void submitMock()}
            className="inline-flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {submitting ? (
              <Loader2 className="h-4 w-4 animate-spin" aria-hidden />
            ) : null}
            Actualizar medio (simulado)
          </button>
        </div>
      ) : null}

      {showBrick ? (
        <div className="mt-6 rounded-xl border border-gray-100 bg-gray-50 p-4">
          <div className="mb-4 flex justify-center border-b border-gray-200 pb-4">
            <Image
              src="/brands/MercadoPagoLogo.svg"
              alt="Mercado Pago"
              width={156}
              height={63}
              className="h-10 w-auto max-w-[min(100%,14rem)]"
              unoptimized
            />
          </div>
          <CardPayment
            key={`card-update-${payerEmail}`}
            initialization={cardInitialization}
            locale="es-AR"
            onSubmit={handleCardSubmit}
          />
        </div>
      ) : null}
    </section>
  );
}
