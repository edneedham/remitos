'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Check, Download, QrCode, ScanLine } from 'lucide-react';
import { QRCodeSVG } from 'qrcode.react';
import { getPublicSiteOrigin } from '../lib/siteUrl';
import { isLikelyMobileDevice } from '../lib/mobileDevice';
import { hasWebSession, refreshWebSession } from '../lib/webAuth';
import LoadingSpinner from '../ui/components/shared/LoadingSpinner';
import {
  markTrialSuccessScreenViewed,
  trackTrialOnboardingEvent,
} from '../lib/trialOnboardingAnalytics';

const STEPS = [
  {
    title: 'Descargá la app En Punto para Android',
    body: 'La app de depósito es solo Android (APK). Instalá y abrí la aplicación en el teléfono — no usamos Google Play todavía.',
  },
  {
    title: 'Iniciá sesión en el teléfono',
    body: 'Usá el mismo correo y contraseña que al registrarte en la web.',
  },
  {
    title: 'Escaneá tu primer remito',
    body: 'En la app, escaneá un remito; los datos se sincronizan con el panel web.',
  },
] as const;

const LOGIN_WITH_RETURN = '/login?next=%2Ftrial-started';

export default function TrialStartedClient() {
  const router = useRouter();
  const [ready, setReady] = useState(false);
  const [copyDone, setCopyDone] = useState(false);
  const [origin, setOrigin] = useState('');
  const [isMobile, setIsMobile] = useState(false);
  const didTrackView = useRef(false);
  const didTrackQr = useRef(false);

  const appPagePath = '/dashboard/app';
  const appPageUrl = origin ? `${origin}${appPagePath}` : appPagePath;

  useEffect(() => {
    if (!hasWebSession()) {
      router.replace(LOGIN_WITH_RETURN);
      return;
    }

    void refreshWebSession().finally(() => {
      setOrigin(getPublicSiteOrigin() || window.location.origin);
      setIsMobile(isLikelyMobileDevice());
      setReady(true);
    });
  }, [router]);

  useEffect(() => {
    if (!ready || didTrackView.current) return;
    didTrackView.current = true;
    markTrialSuccessScreenViewed();
    trackTrialOnboardingEvent('trial_success_screen_viewed', {
      is_mobile: isMobile,
    });
  }, [ready, isMobile]);

  useEffect(() => {
    if (!ready || isMobile || didTrackQr.current) return;
    didTrackQr.current = true;
    trackTrialOnboardingEvent('trial_qr_viewed');
  }, [ready, isMobile]);

  const handleDownloadNav = useCallback(
    (source: 'primary' | 'inline') => {
      trackTrialOnboardingEvent('trial_download_clicked', {
        platform: isMobile ? 'mobile_web' : 'desktop_web',
        source,
      });
    },
    [isMobile],
  );

  const handleCopyLink = useCallback(async () => {
    const text = appPageUrl;
    try {
      await navigator.clipboard.writeText(text);
      setCopyDone(true);
      trackTrialOnboardingEvent('trial_copy_link_clicked', {
        channel: 'clipboard',
      });
      window.setTimeout(() => setCopyDone(false), 2000);
    } catch {
      setCopyDone(false);
    }
  }, [appPageUrl]);

  const handleDashboardFallback = useCallback(() => {
    trackTrialOnboardingEvent('trial_fallback_web_upload_clicked', {
      destination: 'dashboard',
    });
  }, []);

  if (!ready) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center bg-gray-50 px-4">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  return (
    <div className="bg-gray-50 px-4 py-10 lg:py-14">
      <div className="mx-auto w-full max-w-2xl">
        <div className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm sm:p-8">
          <p className="text-xs font-semibold uppercase tracking-wide text-blue-700">
            Comenzá con la app Android
          </p>
          <h1 className="mt-2 text-2xl font-bold tracking-tight text-gray-900 sm:text-3xl">
            Listo: tenés 7 días de prueba. Instalá la app y escaneá un remito.
          </h1>
          <p className="mt-2 text-sm leading-relaxed text-gray-600 sm:text-base">
            Todos los planes arrancan con 7 días de prueba bajo las mismas
            reglas (incluido límite de depósitos, documentos y un dispositivo
            por depósito). En unos 2 minutos podés estar escaneando en
            Android.
          </p>

          <ol className="mt-8 space-y-4">
            {STEPS.map((step, i) => (
              <li
                key={step.title}
                className="flex gap-3 rounded-xl border border-gray-100 bg-gray-50/80 p-4"
              >
                <div
                  className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-blue-600 text-sm font-bold text-white"
                  aria-hidden
                >
                  {i + 1}
                </div>
                <div className="min-w-0">
                  <p className="font-semibold text-gray-900">{step.title}</p>
                  <p className="mt-1 text-sm leading-relaxed text-gray-600">
                    {step.body}
                  </p>
                </div>
              </li>
            ))}
          </ol>

          <div className="mt-8">
            <Link
              href={appPagePath}
              onClick={() => handleDownloadNav('primary')}
              className="inline-flex w-full items-center justify-center gap-2 rounded-lg bg-blue-600 px-5 py-3 text-sm font-semibold text-white hover:bg-blue-700 sm:w-auto"
            >
              <Download className="h-4 w-4 shrink-0" aria-hidden />
              Ir a descargar la app (Android)
            </Link>
          </div>

          {!isMobile && origin ? (
            <div className="mt-8 rounded-xl border border-gray-200 bg-gray-50 p-5">
              <div className="flex flex-col items-center gap-4 sm:flex-row sm:items-start sm:justify-between">
                <div className="text-center sm:text-left">
                  <p className="inline-flex items-center gap-2 text-sm font-semibold text-gray-900">
                    <QrCode className="h-4 w-4 text-gray-500" aria-hidden />
                    Abrí la descarga en tu Android
                  </p>
                  <p className="mt-1 text-sm text-gray-600">
                    Escaneá con el teléfono: abre la misma página de descarga; si
                    hace falta, iniciá sesión con tu usuario de registro.
                  </p>
                </div>
                <div className="flex shrink-0 flex-col items-center rounded-lg bg-white p-2 shadow-sm ring-1 ring-gray-100">
                  <QRCodeSVG
                    value={appPageUrl}
                    size={140}
                    level="M"
                    includeMargin
                  />
                </div>
              </div>
            </div>
          ) : null}

          <div className="mt-6 flex flex-wrap items-center gap-3 border-t border-gray-100 pt-6">
            <button
              type="button"
              onClick={() => void handleCopyLink()}
              className="text-sm font-semibold text-blue-600 hover:text-blue-700 hover:underline"
            >
              {copyDone ? 'Enlace copiado' : 'Copiar enlace de descarga'}
            </button>
            <span className="text-gray-300" aria-hidden>
              ·
            </span>
            <Link
              href="/dashboard"
              onClick={handleDashboardFallback}
              className="text-sm font-semibold text-gray-700 hover:text-gray-900 hover:underline"
            >
              Ir al panel web
            </Link>
          </div>

          <div className="mt-8 rounded-lg border border-emerald-100 bg-emerald-50/80 px-4 py-3 text-sm text-emerald-900">
            <p className="flex items-start gap-2">
              <ScanLine
                className="mt-0.5 h-4 w-4 shrink-0 text-emerald-700"
                aria-hidden
              />
              <span>
                Los remitos que proceses en la app se reflejan en el panel.
                Podés ver el resumen en <strong>Panel</strong> cuando quieras.
              </span>
            </p>
          </div>
        </div>

        <p className="mt-6 text-center text-xs text-gray-500">
          <Check
            className="inline h-3.5 w-3.5 -translate-y-px text-emerald-600"
            aria-hidden
          />{' '}
          Ya iniciaste sesión en esta web. El mismo usuario sirve para la app
          Android.
        </p>
      </div>
    </div>
  );
}
