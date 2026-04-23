'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import Image from 'next/image';
import { useEffect, useState } from 'react';
import { hasWebSession, logoutWebSession } from '../../../lib/webAuth';

export default function Header() {
  const pathname = usePathname();
  const router = useRouter();
  const [session, setSession] = useState(false);

  useEffect(() => {
    setSession(hasWebSession());
  }, [pathname]);

  const signupActive =
    pathname === '/signup' || pathname.startsWith('/signup/');
  const loginActive = pathname === '/login';
  const accountActive =
    pathname === '/account' || pathname.startsWith('/account/');
  const downloadActive = pathname === '/download';

  async function handleLogout() {
    await logoutWebSession();
    setSession(false);
    router.push('/');
    router.refresh();
  }

  return (
    <header className="relative z-10 bg-white shadow-sm">
      <div className="mx-auto w-full max-w-[80vw] px-4 sm:px-6 lg:px-8">
        <div className="flex h-24 items-center justify-between gap-3">
          <div className="flex shrink-0 items-center">
            <Link
              href="/"
              className="flex items-center text-xl font-bold text-blue-600"
              prefetch={false}
            >
              <Image
                src="/enpunto-new.svg"
                alt="En Punto"
                width={140}
                height={25}
                className="h-10 w-auto max-w-[160px] sm:h-11 sm:max-w-[200px]"
                priority
                unoptimized
              />
            </Link>
          </div>
          <nav
            className="flex shrink-0 items-center gap-2 sm:gap-3"
            aria-label="Principal"
          >
            {session ? (
              <>
                <Link
                  href="/download"
                  className={`inline-flex items-center rounded-lg px-3 py-2 text-sm font-semibold transition-colors sm:px-4 ${
                    downloadActive
                      ? 'text-blue-700'
                      : 'text-gray-800 hover:text-gray-900'
                  }`}
                >
                  Descargar app
                </Link>
                <Link
                  href="/account"
                  className={`inline-flex items-center rounded-lg border px-3 py-2 text-sm font-semibold transition-colors sm:px-4 ${
                    accountActive
                      ? 'border-blue-600 text-blue-700'
                      : 'border-gray-300 bg-white text-gray-800 hover:bg-gray-50'
                  }`}
                >
                  Mi cuenta
                </Link>
                <button
                  type="button"
                  onClick={() => void handleLogout()}
                  className="inline-flex items-center rounded-lg px-3 py-2 text-sm font-semibold text-gray-600 transition-colors hover:text-gray-900 sm:px-4"
                >
                  Salir
                </button>
              </>
            ) : (
              <>
                <Link
                  href="/signup"
                  className={`inline-flex items-center rounded-lg px-3 py-2 text-sm font-semibold transition-colors sm:px-4 ${
                    signupActive
                      ? 'bg-blue-700 text-white'
                      : 'bg-blue-600 text-white hover:bg-blue-700'
                  }`}
                >
                  Registro
                </Link>
                <Link
                  href="/login"
                  className={`inline-flex items-center rounded-lg border px-3 py-2 text-sm font-semibold transition-colors sm:px-4 ${
                    loginActive
                      ? 'border-blue-600 text-blue-700'
                      : 'border-gray-300 bg-white text-gray-800 hover:bg-gray-50'
                  }`}
                >
                  Iniciar sesión
                </Link>
              </>
            )}
          </nav>
        </div>
      </div>
    </header>
  );
}
