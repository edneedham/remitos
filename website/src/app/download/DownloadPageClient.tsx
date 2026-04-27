'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Loader2 } from 'lucide-react';
import {
  clearWebSession,
  fetchWithWebAuth,
  hasWebSession,
  refreshWebSession,
} from '../lib/webAuth';
import { getApiBaseUrl } from '../lib/apiUrl';

type Entitlement = {
  can_download_app: boolean;
  subscription_plan?: string;
  trial_ends_at?: string;
  subscription_expires_at?: string;
  company_status?: string;
  archived_at?: string;
};

export default function DownloadPageClient() {
  const router = useRouter();
  const [ready, setReady] = useState(false);
  const [entitlement, setEntitlement] = useState<Entitlement | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [downloadBusy, setDownloadBusy] = useState(false);

  useEffect(() => {
    if (!hasWebSession()) {
      router.replace('/login');
      return;
    }

    let cancelled = false;

    async function load() {
      const api = getApiBaseUrl();
      if (!api) {
        setError(
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
        setError(
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

  async function handleDownload() {
    setError(null);
    setDownloadBusy(true);
    try {
      const res = await fetchWithWebAuth('/auth/downloads/android');
      const body = (await res.json().catch(() => ({}))) as {
        signed_url?: string;
        filename?: string;
        message?: string;
      };

      if (res.status === 403) {
        setError(
          body.message ||
            'Tu plan actual no permite descargar la aplicación en este momento.',
        );
        return;
      }

      if (res.status === 503 || res.status === 500) {
        setError(
          body.message ||
            'La descarga no está disponible temporalmente. Contactanos si el problema continúa.',
        );
        return;
      }

      if (!res.ok || !body.signed_url) {
        setError(
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

  if (!ready && !error) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center bg-gray-50">
        <Loader2 className="h-8 w-8 animate-spin text-blue-600" aria-hidden />
      </div>
    );
  }

  const canDownload = entitlement?.can_download_app === true;

  return (
    <div className="bg-gray-50 px-4 py-12">
      <div className="mx-auto max-w-2xl space-y-8">
        <header className="space-y-2">
          <h1 className="text-3xl font-bold tracking-tight text-gray-900">
            Descargar aplicación Android
          </h1>
          <p className="text-base leading-relaxed text-gray-600">
            Instalá En Punto en el teléfono del depósito sin usar Google Play.
            Necesitás un plan de prueba activo o una suscripción paga.
          </p>
        </header>

        {error && (
          <div
            className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800"
            role="alert"
          >
            {error}
          </div>
        )}

        <section className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
          {!canDownload ? (
            <>
              <h2 className="text-lg font-semibold text-gray-900">
                Descarga no disponible con tu plan actual
              </h2>
              <p className="mt-2 text-sm leading-relaxed text-gray-600">
                Cuando tu período de prueba esté activo o tu suscripción esté al
                día, vas a poder descargar el archivo APK desde esta página.
              </p>
              <p className="mt-4 text-sm text-gray-600">
                Plan actual:{' '}
                <span className="font-medium text-gray-900">
                  {entitlement?.subscription_plan ?? '—'}
                </span>
              </p>
              <Link
                href="/account"
                className="mt-6 inline-block text-sm font-semibold text-blue-600 hover:text-blue-700 hover:underline"
              >
                Ir a mi cuenta
              </Link>
            </>
          ) : (
            <>
              <h2 className="text-lg font-semibold text-gray-900">
                Descargar APK
              </h2>
              <p className="mt-2 text-sm leading-relaxed text-gray-600">
                El enlace es temporal y seguro. Si expira, tocá el botón de nuevo.
              </p>
              <button
                type="button"
                disabled={downloadBusy}
                onClick={() => void handleDownload()}
                className="mt-6 inline-flex items-center justify-center rounded-lg bg-blue-600 px-5 py-2.5 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-60"
              >
                {downloadBusy ? (
                  <>
                    <Loader2
                      className="mr-2 h-4 w-4 animate-spin"
                      aria-hidden
                    />
                    Preparando descarga…
                  </>
                ) : (
                  'Descargar APK'
                )}
              </button>
            </>
          )}
        </section>

        <section className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
          <h2 className="text-lg font-semibold text-gray-900">
            Instalación (Android)
          </h2>
          <ol className="mt-3 list-decimal space-y-2 pl-5 text-sm text-gray-600">
            <li>
              Abrí el archivo descargado y permití la instalación desde esta
              fuente si el sistema lo pide (ajustes de seguridad / fuentes
              desconocidas).
            </li>
            <li>
              Iniciá sesión en la app con el mismo código de empresa y usuario
              que en la web.
            </li>
          </ol>
        </section>

      </div>
    </div>
  );
}
