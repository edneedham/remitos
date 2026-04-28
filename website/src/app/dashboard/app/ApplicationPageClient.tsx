'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Loader2 } from 'lucide-react';
import { QRCodeSVG } from 'qrcode.react';
import { getApiBaseUrl } from '../../lib/apiUrl';
import { isLikelyMobileDevice } from '../../lib/mobileDevice';
import { getPublicSiteOrigin } from '../../lib/siteUrl';
import {
  clearWebSession,
  fetchWithWebAuth,
  getWebAccessToken,
  getWebRefreshToken,
  hasWebSession,
  refreshWebSession,
} from '../../lib/webAuth';
import type { Entitlement } from '../lib/entitlementTypes';

export default function ApplicationPageClient() {
  const router = useRouter();
  const [ready, setReady] = useState(false);
  const [entitlement, setEntitlement] = useState<Entitlement | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [downloadBusy, setDownloadBusy] = useState(false);

  const [isMobile, setIsMobile] = useState<boolean | null>(null);
  const [transferBusy, setTransferBusy] = useState(false);
  const [transferError, setTransferError] = useState<string | null>(null);
  const [transferUrl, setTransferUrl] = useState('');
  const didAutoStartTransfer = useRef(false);

  useEffect(() => {
    setIsMobile(isLikelyMobileDevice());
  }, []);

  useEffect(() => {
    if (!hasWebSession()) {
      router.replace('/login');
      return;
    }

    let cancelled = false;

    async function load() {
      const api = getApiBaseUrl();
      if (!api) {
        setLoadError(
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
        setLoadError(
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

  const handleStartTransfer = useCallback(async () => {
    setTransferError(null);
    setTransferBusy(true);
    try {
      const api = getApiBaseUrl();
      if (!api) {
        setTransferError(
          'Falta configurar NEXT_PUBLIC_API_URL (URL del servidor de la API).',
        );
        return;
      }

      await refreshWebSession();
      const accessToken = getWebAccessToken();
      const refreshToken = getWebRefreshToken();
      if (!accessToken || !refreshToken) {
        clearWebSession();
        router.replace('/login');
        return;
      }

      const res = await fetch(`${api}/auth/transfer/start`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${accessToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ refresh_token: refreshToken }),
      });

      const body = (await res.json().catch(() => ({}))) as {
        token?: string;
        message?: string;
      };

      if (res.status === 401) {
        clearWebSession();
        router.replace('/login');
        return;
      }

      if (!res.ok || !body.token) {
        setTransferError(
          body.message ||
            'No se pudo generar el QR de transferencia. Volvé a intentarlo.',
        );
        return;
      }

      const origin = getPublicSiteOrigin();
      if (!origin) {
        setTransferError('No se pudo determinar la URL pública del sitio.');
        return;
      }

      setTransferUrl(
        `${origin}/transfer?token=${encodeURIComponent(body.token)}`,
      );
    } finally {
      setTransferBusy(false);
    }
  }, [router]);

  useEffect(() => {
    if (
      !ready ||
      entitlement?.can_download_app !== true ||
      isMobile !== false ||
      didAutoStartTransfer.current
    ) {
      return;
    }
    didAutoStartTransfer.current = true;
    void handleStartTransfer();
  }, [ready, entitlement?.can_download_app, isMobile, handleStartTransfer]);

  async function handleDownload() {
    setActionError(null);
    setDownloadBusy(true);
    try {
      const res = await fetchWithWebAuth('/auth/downloads/android');
      const body = (await res.json().catch(() => ({}))) as {
        signed_url?: string;
        filename?: string;
        message?: string;
      };

      if (res.status === 403) {
        setActionError(
          body.message ||
            'Tu plan actual no permite descargar la aplicación en este momento.',
        );
        return;
      }

      if (res.status === 503 || res.status === 500) {
        setActionError(
          body.message ||
            'La descarga no está disponible temporalmente. Contactanos si el problema continúa.',
        );
        return;
      }

      if (!res.ok || !body.signed_url) {
        setActionError(
          body.message ||
            'No se pudo iniciar la descarga. Volvé a iniciar sesión e intentá de nuevo.',
        );
        return;
      }

      window.location.assign(body.signed_url);
    } finally {
      setDownloadBusy(false);
    }
  }

  if (!ready && !loadError) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center bg-gray-50">
        <Loader2 className="h-8 w-8 animate-spin text-blue-600" aria-hidden />
      </div>
    );
  }

  const canDownload = entitlement?.can_download_app === true;

  return (
    <div className="bg-gray-50 px-4 pb-12 pt-6">
      <div className="mx-auto max-w-7xl space-y-8 text-left">
        <h1 className="text-2xl font-bold tracking-tight text-gray-900">
          Aplicación
        </h1>

        {loadError ? (
          <div
            className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800"
            role="alert"
          >
            {loadError}
          </div>
        ) : null}

        {actionError ? (
          <div
            className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800"
            role="alert"
          >
            {actionError}
          </div>
        ) : null}

        <div className="max-w-xl space-y-8">
          <div className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
            {!canDownload ? (
              <div className="space-y-4 text-sm text-gray-600">
                <p>
                  Plan:{' '}
                  <span className="font-medium text-gray-900">
                    {entitlement?.subscription_plan ?? '—'}
                  </span>
                </p>
                <p>La descarga no está disponible con el plan actual.</p>
                <Link
                  href="/dashboard"
                  className="inline-block font-semibold text-blue-600 hover:text-blue-700 hover:underline"
                >
                  Volver al panel
                </Link>
              </div>
            ) : isMobile === null ? (
              <div className="flex items-center gap-2 py-10 text-sm text-gray-600">
                <Loader2 className="h-5 w-5 animate-spin text-blue-600" aria-hidden />
                Preparando…
              </div>
            ) : isMobile ? (
              <button
                type="button"
                disabled={downloadBusy}
                onClick={() => void handleDownload()}
                className="inline-flex items-center justify-center rounded-lg bg-blue-600 px-6 py-3 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-60"
              >
                {downloadBusy ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" aria-hidden />
                    Preparando…
                  </>
                ) : (
                  'Descargar APK'
                )}
              </button>
            ) : (
              <div className="space-y-4 text-center">
                <p className="sr-only">
                  Código QR para abrir la sesión en el teléfono
                </p>
                {transferError ? (
                  <p className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
                    {transferError}
                  </p>
                ) : null}
                <div className="mx-auto flex w-fit flex-col items-start rounded-lg border border-gray-100 p-2">
                  {transferUrl ? (
                    <QRCodeSVG value={transferUrl} size={220} level="M" includeMargin={false} />
                  ) : (
                    <div className="flex h-[220px] w-[220px] items-center gap-2 text-sm text-gray-600">
                      <Loader2 className="h-4 w-4 animate-spin text-blue-600" aria-hidden />
                      Generando QR…
                    </div>
                  )}
                </div>
                <button
                  type="button"
                  disabled={transferBusy}
                  onClick={() => void handleStartTransfer()}
                  className="mx-auto inline-flex items-center justify-center rounded-lg border border-gray-300 bg-white px-5 py-2.5 text-sm font-semibold text-gray-800 hover:bg-gray-50 disabled:opacity-60"
                >
                  Generar nuevo QR
                </button>
              </div>
            )}
          </div>

          {canDownload ? (
            <section aria-labelledby="install-mobile-heading">
              <h2
                id="install-mobile-heading"
                className="text-sm font-semibold text-gray-900"
              >
                Instalación en el teléfono (Android)
              </h2>
              <ol className="mt-3 list-decimal space-y-2 pl-5 text-sm leading-relaxed text-gray-600">
                <li>
                  Abrí el archivo APK descargado y, si Android lo pide, permití
                  instalar desde esta fuente (seguridad / fuentes desconocidas).
                </li>
                <li>
                  Abrí En Punto e iniciá sesión con el mismo código de empresa y
                  usuario que usás en la web.
                </li>
              </ol>
            </section>
          ) : null}
        </div>
      </div>
    </div>
  );
}
