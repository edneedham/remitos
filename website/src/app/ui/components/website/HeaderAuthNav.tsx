'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useEffect, useRef, useState } from 'react';
import { LogOut } from 'lucide-react';
import {
  fetchProfile,
  hasWebSession,
  logoutWebSession,
  type WebProfile,
} from '../../../lib/webAuth';

/**
 * Right-hand auth controls for the site header and account dashboard top bar.
 */
export default function HeaderAuthNav() {
  const pathname = usePathname();
  const router = useRouter();
  const [session, setSession] = useState(false);
  const [profile, setProfile] = useState<WebProfile | null>(null);
  const [accountMenuOpen, setAccountMenuOpen] = useState(false);
  const accountMenuRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    queueMicrotask(() => setSession(hasWebSession()));
  }, [pathname]);

  useEffect(() => {
    if (!session) {
      queueMicrotask(() => {
        setProfile(null);
        setAccountMenuOpen(false);
      });
      return;
    }

    let cancelled = false;
    async function loadProfile() {
      const nextProfile = await fetchProfile();
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
    pathname === '/registro' || pathname.startsWith('/registro/');
  const loginActive = pathname === '/ingresar';
  const dashboardHomeActive = pathname === '/panel';
  const billingNavActive = pathname.startsWith('/panel/facturacion');
  const applicationNavActive = pathname.startsWith('/panel/aplicacion');
  const changePasswordNavActive = pathname.startsWith(
    '/panel/cambiar-clave',
  );
  /** Sidebar replaces these links on desktop; hide them in the dropdown only there. */
  const navLinksMobileOnlyInDropdown =
    pathname.startsWith('/panel');
  const isPanelRoute = pathname.startsWith('/panel');

  async function handleLogout() {
    await logoutWebSession();
    setAccountMenuOpen(false);
    setSession(false);
    router.push('/');
    router.refresh();
  }

  return (
    <nav
      className={`flex items-center gap-2 sm:gap-4 ${
        session
          ? 'w-auto max-w-full shrink-0 sm:min-w-0'
          : 'w-auto shrink-0 justify-end'
      }`}
      aria-label="Principal"
    >
      {session ? (
        <>
          <div
            className={`relative ${isPanelRoute ? 'hidden md:block' : ''}`}
            ref={accountMenuRef}
          >
          <button
            type="button"
            onClick={() => setAccountMenuOpen((open) => !open)}
            className="inline-flex max-w-[15rem] items-center gap-2 rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm font-semibold text-gray-800 hover:bg-gray-50 sm:max-w-[17rem]"
            aria-haspopup="menu"
            aria-expanded={accountMenuOpen}
            aria-label="Abrir menú de empresa"
          >
            <span className="max-w-[11rem] truncate sm:max-w-[13rem]">
              {profile?.company_name || '—'}
            </span>
            <svg
              className={`h-4 w-4 shrink-0 text-gray-500 transition-transform ${accountMenuOpen ? 'rotate-180' : ''}`}
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
              <div
                className={
                  navLinksMobileOnlyInDropdown ? 'md:hidden' : undefined
                }
              >
                <Link
                  href="/panel"
                  className={`block w-full rounded-md px-3 py-2 text-left text-sm font-semibold ${
                    dashboardHomeActive
                      ? 'text-blue-700'
                      : 'text-gray-700 hover:bg-gray-50'
                  }`}
                  onClick={() => setAccountMenuOpen(false)}
                >
                  Panel
                </Link>
                <Link
                  href="/panel/facturacion"
                  className={`block w-full rounded-md px-3 py-2 text-left text-sm font-semibold ${
                    billingNavActive
                      ? 'text-blue-700'
                      : 'text-gray-700 hover:bg-gray-50'
                  }`}
                  onClick={() => setAccountMenuOpen(false)}
                >
                  Facturación
                </Link>
                <Link
                  href="/panel/aplicacion"
                  className={`block w-full rounded-md px-3 py-2 text-left text-sm font-semibold ${
                    applicationNavActive
                      ? 'text-blue-700'
                      : 'text-gray-700 hover:bg-gray-50'
                  }`}
                  onClick={() => setAccountMenuOpen(false)}
                >
                  Aplicación
                </Link>
                <Link
                  href="/panel/cambiar-clave"
                  className={`block w-full rounded-md px-3 py-2 text-left text-sm font-semibold ${
                    changePasswordNavActive
                      ? 'text-blue-700'
                      : 'text-gray-700 hover:bg-gray-50'
                  }`}
                  onClick={() => setAccountMenuOpen(false)}
                >
                  Contraseña
                </Link>
              </div>
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
        </>
      ) : (
        <div className="hidden items-center gap-2 sm:gap-4 lg:flex">
          {/* Desktop: plain login + pill signup; mobile uses full-screen menu in Header */}
          <Link
            href="/ingresar"
            aria-label="Iniciar sesión"
            className={`whitespace-nowrap px-1 py-2 text-sm font-medium transition-colors sm:px-2 ${
              loginActive
                ? 'text-blue-700'
                : 'text-gray-700 hover:text-gray-900'
            }`}
          >
            Iniciar sesión
          </Link>
          <Link
            href="/registro"
            className={`inline-flex shrink-0 items-center rounded-full px-4 py-2 text-sm font-semibold shadow-sm transition-colors sm:px-5 sm:py-2.5 ${
              signupActive
                ? 'bg-blue-700 text-white hover:bg-blue-800'
                : 'bg-blue-600 text-white hover:bg-blue-700'
            }`}
          >
            Registro
          </Link>
        </div>
      )}
    </nav>
  );
}
