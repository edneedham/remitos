'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { Loader2 } from 'lucide-react';
import { QRCodeSVG } from 'qrcode.react';
import { ApplicationContentSkeleton } from '../components/PanelSkeletons';
import { getApiBaseUrl } from '../../lib/apiUrl';
import { detectDevicePlatform, type DevicePlatform } from '../../lib/mobileDevice';
import { getPublicSiteOrigin } from '../../lib/siteUrl';
import {
  clearWebSession,
  fetchWithWebAuth,
  getWebAccessToken,
  getWebRefreshToken,
  postWithWebAuth,
  refreshWebSession,
  isWebCookieSession,
} from '../../lib/webAuth';
import { CHECKLIST_DOWNLOAD_PAGE_VISITED_KEY } from '../../lib/trialOnboardingChecklist';
import type { Entitlement } from '../lib/entitlementTypes';
import { usePanelBootstrap } from '../lib/usePanelBootstrap';
import { useRouterRef } from '../lib/useRouterRef';

export default function ApplicationPageClient() {
  const routerRef = useRouterRef();
  const cookieSession = isWebCookieSession();
  const {
    status,
    errorMessage: configError,
    entitlement: bootstrapEntitlement,
  } = usePanelBootstrap();
  const [entitlementLoading, setEntitlementLoading] = useState(true);
  const [entitlement, setEntitlement] = useState<Entitlement | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [downloadBusy, setDownloadBusy] = useState(false);

  const [devicePlatform, setDevicePlatform] = useState<DevicePlatform | null>(null);
  const [transferBusy, setTransferBusy] = useState(false);
  const [transferError, setTransferError] = useState<string | null>(null);
  const [transferUrl, setTransferUrl] = useState('');
  const didAutoStartTransfer = useRef(false);

  useEffect(() => {
    queueMicrotask(() => setDevicePlatform(detectDevicePlatform()));
  }, []);

  useEffect(() => {
    if (entitlementLoading || loadError) return;
    try {
      window.localStorage.setItem(CHECKLIST_DOWNLOAD_PAGE_VISITED_KEY, '1');
    } catch {
      /* ignore */
    }
  }, [entitlementLoading, loadError]);

  useEffect(() => {
    if (status === 'config_error') {
      queueMicrotask(() => {
        setLoadError(configError);
        setEntitlementLoading(false);
      });
      return;
    }
    if (status !== 'ready') {
      return;
    }

    if (bootstrapEntitlement) {
      queueMicrotask(() => {
        setEntitlement(bootstrapEntitlement);
        setEntitlementLoading(false);
        setLoadError(null);
      });
      return;
    }

    let cancelled = false;

    async function load() {
      const res = await fetchWithWebAuth('/auth/me/entitlement');
      if (cancelled) return;

      if (res.status === 401) {
        clearWebSession();
        routerRef.current.replace('/ingresar');
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
        setEntitlementLoading(false);
        return;
      }

      const data = (await res.json()) as Entitlement;
      setEntitlement(data);
      setEntitlementLoading(false);
    }

    queueMicrotask(() => {
      setEntitlementLoading(true);
      setLoadError(null);
      void load();
    });
    return () => {
      cancelled = true;
    };
  }, [status, configError, routerRef, bootstrapEntitlement]);

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

      if (!cookieSession) {
        const accessToken = getWebAccessToken();
        const refreshToken = getWebRefreshToken();
        if (!accessToken || !refreshToken) {
          clearWebSession();
          routerRef.current.replace('/ingresar');
          return;
        }
      }

      const res = await postWithWebAuth(
        '/auth/transfer/start',
        cookieSession ? {} : { refresh_token: getWebRefreshToken()! },
      );

      const body = (await res.json().catch(() => ({}))) as {
        token?: string;
        message?: string;
      };

      if (res.status === 401) {
        clearWebSession();
        routerRef.current.replace('/ingresar');
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
        `${origin}/transferir?token=${encodeURIComponent(body.token)}`,
      );
    } finally {
      setTransferBusy(false);
    }
  }, [routerRef, cookieSession]);

  useEffect(() => {
    if (
      entitlementLoading ||
      entitlement?.can_download_app !== true ||
      devicePlatform !== 'desktop' ||
      didAutoStartTransfer.current
    ) {
      return;
    }
    didAutoStartTransfer.current = true;
    void handleStartTransfer();
  }, [
    entitlementLoading,
    entitlement?.can_download_app,
    devicePlatform,
    handleStartTransfer,
  ]);

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

  const canDownload = entitlement?.can_download_app === true;

  return (
    <div className="bg-gray-50 px-4 pb-12 pt-6">
      <div className="mx-auto max-w-[92rem] space-y-8 text-left">
        <h1 className="text-3xl font-bold tracking-tight text-gray-900">
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
          {entitlementLoading && !loadError ? (
            <ApplicationContentSkeleton />
          ) : null}

          {!entitlementLoading && !canDownload ? (
            <div className="space-y-4 text-sm text-gray-600">
              <p>
                Plan:{' '}
                <span className="font-medium text-gray-900">
                  {entitlement?.subscription_plan ?? '—'}
                </span>
              </p>
              <p>La descarga no está disponible con el plan actual.</p>
              <Link
                href="/panel"
                className="inline-block font-semibold text-blue-600 hover:text-blue-700 hover:underline"
              >
                Volver al panel
              </Link>
            </div>
          ) : !entitlementLoading && devicePlatform === null ? (
            <div className="flex min-h-[8rem] flex-col items-center justify-center gap-2 py-10 text-sm text-gray-600">
              <Loader2 className="h-5 w-5 animate-spin text-blue-600" aria-hidden />
              <span className="text-center">Preparando…</span>
            </div>
          ) : !entitlementLoading && devicePlatform !== 'desktop' ? (
            devicePlatform === 'ios' ? (
              <div className="space-y-3">
                <p className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
                  La app de En Punto está disponible para Android. Desde iPhone o iPad no se puede instalar APK.
                </p>
                <p className="text-sm text-gray-600">
                  Si necesitás usar la app móvil, abrí esta cuenta desde un dispositivo Android.
                </p>
              </div>
            ) : (
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
            )
          ) : !entitlementLoading ? (
            <div className="space-y-4 text-center">
              <p className="sr-only">
                Código QR para abrir la sesión en el teléfono
              </p>
              {transferError ? (
                <p className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
                  {transferError}
                </p>
              ) : null}
              <div className="mx-auto flex w-fit flex-col items-start">
                {transferUrl ? (
                  <div className="relative h-[220px] w-[220px]">
                    <QRCodeSVG
                      value={transferUrl}
                      size={220}
                      // Logo overlay needs higher resilience; H keeps scans reliable.
                      level="H"
                      includeMargin={false}
                    />
                    <div
                      className="absolute inset-0 flex items-center justify-center"
                      aria-hidden
                    >
                      <div className="flex h-12 w-12 items-center justify-center rounded-full bg-white">
                        {/* eslint-disable-next-line @next/next/no-img-element -- small static SVG inside QR overlay */}
                        <img
                          src="/enpunto-simple.svg"
                          alt=""
                          className="h-7 w-7"
                          aria-hidden
                        />
                      </div>
                    </div>
                  </div>
                ) : (
                  <div className="flex h-[220px] w-[220px] flex-col items-center justify-center gap-2 text-center text-sm text-gray-600">
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
          ) : null}

          {!entitlementLoading && canDownload ? (
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
              {devicePlatform !== 'ios' ? (
                <p className="mt-4 text-xs leading-relaxed text-gray-500">
                  Software de código abierto: la app incluye bibliotecas de
                  terceros. Un listado con nombre y versión de cada componente
                  se entrega en el paquete de instalación, en el archivo{' '}
                  <code className="rounded bg-gray-100 px-1 py-0.5 font-mono text-[0.7rem] text-gray-800">
                    assets/THIRD_PARTY_LICENSES.txt
                  </code>
                  .
                </p>
              ) : null}
            </section>
          ) : null}
        </div>
      </div>
    </div>
  );
}
