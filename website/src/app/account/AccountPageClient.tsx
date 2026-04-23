'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Loader2 } from 'lucide-react';
import { hasWebSession, logoutWebSession } from '../lib/webAuth';

export default function AccountPageClient() {
  const router = useRouter();
  const [ready, setReady] = useState(false);

  useEffect(() => {
    if (!hasWebSession()) {
      router.replace('/login');
      return;
    }
    setReady(true);
  }, [router]);

  async function handleLogout() {
    await logoutWebSession();
    router.push('/');
    router.refresh();
  }

  if (!ready) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center bg-gray-50">
        <Loader2 className="h-8 w-8 animate-spin text-blue-600" aria-hidden />
      </div>
    );
  }

  return (
    <div className="bg-gray-50 px-4 py-12">
      <div className="mx-auto max-w-2xl space-y-8">
        <header className="space-y-2">
          <h1 className="text-3xl font-bold tracking-tight text-gray-900">
            Administración de cuenta
          </h1>
          <p className="text-base leading-relaxed text-gray-600">
            Desde la web podés gestionar tu cuenta, suscripción y datos de la
            empresa. El uso diario en depósito (remitos, repartos, escaneo) es en
            la{' '}
            <strong className="font-medium text-gray-800">
              aplicación Android
            </strong>
            : iniciá sesión en el teléfono para habilitar el modo operativo.
          </p>
        </header>

        <div className="grid gap-4 sm:grid-cols-2">
          <section className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
            <h2 className="text-lg font-semibold text-gray-900">
              Cuenta y suscripción
            </h2>
            <p className="mt-2 text-sm text-gray-600">
              Próximamente: facturación, plan y datos de contacto desde esta
              pantalla.
            </p>
          </section>
          <section className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
            <h2 className="text-lg font-semibold text-gray-900">
              App en el depósito
            </h2>
            <p className="mt-2 text-sm text-gray-600">
              Iniciá sesión en la aplicación móvil con el mismo código de empresa
              y usuario para operar en depósito (remitos, repartos, escaneo).
            </p>
            <Link
              href="/download"
              className="mt-4 inline-block text-sm font-semibold text-blue-600 hover:text-blue-700 hover:underline"
            >
              Descargar la aplicación Android
            </Link>
          </section>
        </div>

        <div className="flex flex-wrap items-center gap-4 border-t border-gray-200 pt-8">
          <button
            type="button"
            onClick={() => void handleLogout()}
            className="rounded-lg border border-gray-300 bg-white px-4 py-2 text-sm font-semibold text-gray-800 hover:bg-gray-50"
          >
            Cerrar sesión en el sitio
          </button>
          <Link
            href="/"
            className="text-sm font-medium text-blue-600 hover:text-blue-700 hover:underline"
          >
            Volver al inicio
          </Link>
        </div>
      </div>
    </div>
  );
}
