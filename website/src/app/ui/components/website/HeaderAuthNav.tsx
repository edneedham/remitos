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
    pathname === '/signup' || pathname.startsWith('/signup/');
  const loginActive = pathname === '/login';
  const dashboardHomeActive = pathname === '/dashboard';
  const billingNavActive = pathname.startsWith('/dashboard/billing');
  const applicationNavActive = pathname.startsWith('/dashboard/app');
  /** Sidebar replaces these links on desktop; hide them in the dropdown only there. */
  const navLinksMobileOnlyInDropdown =
    pathname.startsWith('/dashboard');

  async function handleLogout() {
    await logoutWebSession();
    setAccountMenuOpen(false);
    setSession(false);
    router.push('/');
    router.refresh();
  }

  return (
    <nav
      className="flex shrink-0 items-center gap-2 sm:gap-3"
      aria-label="Principal"
    >
      {session ? (
        <div className="relative" ref={accountMenuRef}>
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
                  href="/dashboard"
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
                  href="/dashboard/billing"
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
                  href="/dashboard/app"
                  className={`block w-full rounded-md px-3 py-2 text-left text-sm font-semibold ${
                    applicationNavActive
                      ? 'text-blue-700'
                      : 'text-gray-700 hover:bg-gray-50'
                  }`}
                  onClick={() => setAccountMenuOpen(false)}
                >
                  Aplicación
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
  );
}
