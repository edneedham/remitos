'use client';

import { useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Loader2 } from 'lucide-react';
import { getApiBaseUrl } from '../lib/apiUrl';
import { saveWebSession } from '../lib/webAuth';

type TransferClaimResponse = {
  token?: string;
  refresh_token?: string;
  message?: string;
};

export default function TransferPageClient({ token }: { token: string }) {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const normalizedToken = useMemo(() => token.trim(), [token]);

  useEffect(() => {
    let cancelled = false;

    async function claimTransfer() {
      if (!normalizedToken) {
        setError('El enlace de transferencia no es válido.');
        return;
      }

      const api = getApiBaseUrl();
      if (!api) {
        setError('Falta configurar NEXT_PUBLIC_API_URL (URL del servidor de la API).');
        return;
      }

      try {
        const res = await fetch(`${api}/auth/transfer/claim`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ token: normalizedToken }),
        });
        if (cancelled) return;

        const body = (await res.json().catch(() => ({}))) as TransferClaimResponse;
        if (!res.ok || !body.token || !body.refresh_token) {
          setError(
            body.message ||
              'No se pudo completar la transferencia de sesión. Volvé a escanear el código QR.',
          );
          return;
        }

        saveWebSession(body.token, body.refresh_token);
        router.replace('/dashboard/app');
        router.refresh();
      } catch {
        if (!cancelled) {
          setError('Error de red. Volvé a intentar la transferencia desde el QR.');
        }
      }
    }

    void claimTransfer();
    return () => {
      cancelled = true;
    };
  }, [normalizedToken, router]);

  return (
    <div className="flex min-h-[60vh] items-center justify-center bg-gray-50 px-4 py-12">
      <div className="w-full max-w-md rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
        <h1 className="text-xl font-semibold text-gray-900">Transferencia de sesión</h1>
        {error ? (
          <p className="mt-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">
            {error}
          </p>
        ) : (
          <div className="mt-4 flex min-h-[7rem] flex-col items-center justify-center gap-3 text-center text-sm text-gray-600">
            <Loader2 className="h-4 w-4 animate-spin text-blue-600" aria-hidden />
            <span>Validando enlace y preparando la sesión en este dispositivo...</span>
          </div>
        )}
      </div>
    </div>
  );
}
