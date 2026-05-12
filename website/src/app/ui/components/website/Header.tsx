'use client';

import Link from 'next/link';
import Image from 'next/image';
import { Menu, X } from 'lucide-react';
import { useEffect, useState } from 'react';
import { usePathname } from 'next/navigation';
import HeaderAuthNav from './HeaderAuthNav';
import { hasWebSession } from '../../../lib/webAuth';

export default function Header() {
  const pathname = usePathname();
  const [siteMenuOpen, setSiteMenuOpen] = useState(false);
  const [guest, setGuest] = useState(true);

  const loginActive = pathname === '/ingresar';
  const signupActive =
    pathname === '/registro' || pathname.startsWith('/registro/');

  useEffect(() => {
    queueMicrotask(() => setGuest(!hasWebSession()));
  }, [pathname]);

  useEffect(() => {
    queueMicrotask(() => setSiteMenuOpen(false));
  }, [pathname]);

  useEffect(() => {
    if (!siteMenuOpen) return;
    const prev = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    function onKey(event: KeyboardEvent) {
      if (event.key === 'Escape') setSiteMenuOpen(false);
    }
    document.addEventListener('keydown', onKey);
    return () => {
      document.body.style.overflow = prev;
      document.removeEventListener('keydown', onKey);
    };
  }, [siteMenuOpen]);

  if (pathname.startsWith('/panel')) {
    return null;
  }

  return (
    <>
      <header className="relative z-10 w-full min-w-0 shrink-0 border-b border-gray-100 bg-white">
        <div className="w-full min-w-0 max-w-none px-6 sm:px-6 lg:mx-auto lg:max-w-[80rem] lg:px-8">
          <div className="flex min-h-[4.5rem] flex-row items-center justify-between gap-3 py-4 sm:gap-4 lg:h-24 lg:py-0">
            <div className="flex min-w-0 flex-1 items-center gap-4 sm:gap-8">
              <Link
                href="/"
                className="flex shrink-0 items-center rounded-md text-xl font-bold text-blue-600 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500/60 focus-visible:ring-offset-2"
                prefetch={false}
              >
                <Image
                  src="/enpunto-new.svg"
                  alt="En Punto"
                  width={140}
                  height={25}
                  className="h-8 w-auto max-w-[148px] sm:h-10 sm:max-w-[180px] lg:h-11 lg:max-w-[200px]"
                  priority
                  unoptimized
                />
              </Link>

              <Link
                href="/precios"
                className="hidden text-sm font-medium text-gray-600 transition-colors hover:text-gray-900 lg:inline"
              >
                Precios
              </Link>
            </div>

            <div className="flex shrink-0 items-center gap-3">
              <HeaderAuthNav />
              {guest ? (
                <button
                  type="button"
                  className="-m-1.5 rounded-lg p-1.5 text-gray-600 transition-colors hover:bg-gray-50 hover:text-gray-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500/60 lg:hidden"
                  aria-expanded={siteMenuOpen}
                  aria-haspopup="dialog"
                  aria-controls="header-full-menu"
                  onClick={() => setSiteMenuOpen((open) => !open)}
                >
                  <Menu className="h-6 w-6 shrink-0" aria-hidden />
                  <span className="sr-only">Abrir menú</span>
                </button>
              ) : null}
            </div>
          </div>
        </div>
      </header>

      {/* Guests only: full-screen menu (Rocket-style) — links on top, Entrar / Registro fixed at bottom */}
      {siteMenuOpen && guest ? (
        <div
          className="fixed inset-0 z-[200] flex flex-col bg-white lg:hidden"
          role="dialog"
          aria-modal="true"
          aria-labelledby="header-full-menu-title"
          id="header-full-menu"
        >
          {/* Keep this row geometry identical to the live header row to avoid visual jumps */}
          <div className="flex min-h-[4.5rem] shrink-0 items-center justify-between border-b border-gray-100 px-6 py-4 sm:px-6 sm:py-4">
            <Link
              href="/"
              className="flex shrink-0 items-center rounded-md focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500/60 focus-visible:ring-offset-2"
              onClick={() => setSiteMenuOpen(false)}
            >
              <Image
                src="/enpunto-new.svg"
                alt="En Punto"
                width={140}
                height={25}
                className="h-8 w-auto max-w-[148px] sm:h-10 sm:max-w-[180px]"
                priority
                unoptimized
              />
            </Link>
            <button
              type="button"
              className="-m-1.5 rounded-lg p-1.5 text-gray-500 hover:bg-gray-100 hover:text-gray-900 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500/60"
              onClick={() => setSiteMenuOpen(false)}
              aria-label="Cerrar menú"
            >
              <X className="h-6 w-6 shrink-0" aria-hidden />
            </button>
          </div>

          <nav
            className="min-h-0 flex-1 overflow-y-auto px-6 py-6"
            aria-labelledby="header-full-menu-title"
          >
            <p id="header-full-menu-title" className="sr-only">
              Navegación del sitio
            </p>
            <Link
              href="/precios"
              className="block rounded-lg py-4 text-lg font-medium text-gray-900 hover:bg-gray-50"
              onClick={() => setSiteMenuOpen(false)}
            >
              Precios
            </Link>
          </nav>

          <div className="shrink-0 bg-white px-4 pt-2 sm:px-6 pb-[max(4.5rem,calc(env(safe-area-inset-bottom)+3.5rem))]">
            <div className="mx-auto flex w-full max-w-[min(17rem,100%)] flex-col gap-4">
              <Link
                href="/ingresar"
                aria-label="Iniciar sesión"
                className={`flex min-h-12 w-full items-center justify-center rounded-full border border-gray-200 bg-white py-3 text-center text-base font-semibold transition-colors ${
                  loginActive
                    ? 'border-blue-600 text-blue-700'
                    : 'text-gray-900 hover:bg-gray-50'
                }`}
                onClick={() => setSiteMenuOpen(false)}
              >
                Iniciar sesión
              </Link>
              <Link
                href="/registro"
                className={`flex min-h-12 w-full items-center justify-center rounded-full py-3 text-center text-base font-semibold text-white shadow-sm transition-colors ${
                  signupActive ? 'bg-blue-700 hover:bg-blue-800' : 'bg-blue-600 hover:bg-blue-700'
                }`}
                onClick={() => setSiteMenuOpen(false)}
              >
                Registro
              </Link>
            </div>
          </div>
        </div>
      ) : null}
    </>
  );
}
