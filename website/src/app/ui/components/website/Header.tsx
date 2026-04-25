'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import Image from 'next/image';
import { useEffect, useRef, useState } from 'react';
import { LogOut } from 'lucide-react';
import {
  fetchWebProfile,
  hasWebSession,
  logoutWebSession,
  type WebProfile,
} from '../../../lib/webAuth';

export default function Header() {
  const pathname = usePathname();
  const router = useRouter();
  const [session, setSession] = useState(false);
  const [profile, setProfile] = useState<WebProfile | null>(null);
  const [accountMenuOpen, setAccountMenuOpen] = useState(false);
  const accountMenuRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    setSession(hasWebSession());
  }, [pathname]);

  useEffect(() => {
    if (!session) {
      setProfile(null);
      setAccountMenuOpen(false);
      return;
    }

    let cancelled = false;
    async function loadProfile() {
      const nextProfile = await fetchWebProfile();
      if (cancelled) return;
      setProfile(nextProfile);
      if (!nextProfile && !hasWebSession()) {
        setSession(false);
      }
    }
    void loadProfile();

    return () => {
      cancelled = true;
    };
  }, [pathname, session]);

  useEffect(() => {
    if (!accountMenuOpen) return;

    function handlePointerDown(event: MouseEvent) {
      const menuEl = accountMenuRef.current;
      if (!menuEl) return;
      const target = event.target as Node | null;
      if (target && !menuEl.contains(target)) {
        setAccountMenuOpen(false);
      }
    }

    document.addEventListener('mousedown', handlePointerDown);
    return () => {
      document.removeEventListener('mousedown', handlePointerDown);
    };
  }, [accountMenuOpen]);

  const signupActive =
    pathname === '/signup' || pathname.startsWith('/signup/');
  const loginActive = pathname === '/login';
  const accountActive =
    pathname === '/account' || pathname.startsWith('/account/');
  const downloadActive = pathname === '/download';

  async function handleLogout() {
    await logoutWebSession();
    setAccountMenuOpen(false);
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
              accountActive ? (
                <div className="relative" ref={accountMenuRef}>
                  <button
                    type="button"
                    onClick={() => setAccountMenuOpen((open) => !open)}
                    className="inline-flex items-center gap-2 rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm font-semibold text-gray-800 hover:bg-gray-50 sm:px-4"
                    aria-haspopup="menu"
                    aria-expanded={accountMenuOpen}
                    aria-label="Abrir menú de empresa"
                  >
                    <span>{profile?.company_name || '—'}</span>
                    <svg
                      className={`h-4 w-4 text-gray-500 transition-transform ${accountMenuOpen ? 'rotate-180' : ''}`}
                      viewBox="0 0 20 20"
                      fill="none"
                      aria-hidden="true"
                    >
                      <path
                        d="M5 7.5L10 12.5L15 7.5"
                        stroke="currentColor"
                        strokeWidth="1.5"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      />
                    </svg>
                  </button>
                  {accountMenuOpen ? (
                    <div
                      className="absolute right-0 top-full z-20 mt-2 min-w-52 rounded-lg border border-gray-200 bg-white p-1 shadow-lg"
                      role="menu"
                    >
                      <button
                        type="button"
                        onClick={() => void handleLogout()}
                        className="flex w-full items-center justify-between gap-2 rounded-md px-3 py-2 text-left text-sm font-semibold text-gray-700 hover:bg-gray-50"
                      >
                        <span>Cerrar sesión</span>
                        <LogOut className="h-4 w-4" aria-hidden />
                      </button>
                    </div>
                  ) : null}
                </div>
              ) : (
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
              )
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
