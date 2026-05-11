'use client';

import Link from 'next/link';
import Image from 'next/image';
import { useEffect, useState } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import type { LucideIcon } from 'lucide-react';
import {
  AppWindow,
  Check,
  CreditCard,
  KeyRound,
  LayoutDashboard,
  LogOut,
  Menu,
  Monitor,
  Smartphone,
  UserCog,
  Warehouse,
  X,
} from 'lucide-react';
import HeaderAuthNav from '../ui/components/website/HeaderAuthNav';
import {
  canManageOperators,
  fetchProfile,
  hasWebSession,
  logoutWebSession,
} from '../lib/webAuth';
import ActivateSubscriptionGate from './ActivateSubscriptionGate';
import {
  PanelNotificationsProvider,
  PanelNotificationBell,
} from './PanelNotificationsContext';

const PANEL_MOBILE_DESKTOP_HINT_STORAGE_KEY =
  'enpunto_panel_mobile_desktop_hint_dismissed';

function PanelMobileDesktopHint() {
  const [dismissed, setDismissed] = useState<boolean | null>(null);
  const [acknowledging, setAcknowledging] = useState(false);
  const [closingAck, setClosingAck] = useState(false);

  useEffect(() => {
    try {
      setDismissed(
        window.localStorage.getItem(PANEL_MOBILE_DESKTOP_HINT_STORAGE_KEY) ===
          '1',
      );
    } catch {
      setDismissed(false);
    }
  }, []);

  if (dismissed !== false) {
    return null;
  }

  return (
    <div
      className="border-b border-blue-100 bg-blue-50 px-4 py-3 md:hidden"
      role="status"
    >
      <div className="mx-auto flex max-w-[92rem] flex-col gap-3 sm:flex-row sm:items-center sm:justify-between sm:gap-4">
        <p className="flex items-start gap-2.5 text-sm leading-snug text-blue-950 sm:items-center">
          <Monitor
            className="mt-0.5 h-5 w-5 shrink-0 text-blue-800 sm:mt-0"
            aria-hidden
            strokeWidth={2}
          />
          <span>
            Para ver todos los detalles completos, abrí esta página desde tu PC.
          </span>
        </p>
        <button
          type="button"
          onClick={() => {
            if (acknowledging) return;
            setAcknowledging(true);
            try {
              window.localStorage.setItem(
                PANEL_MOBILE_DESKTOP_HINT_STORAGE_KEY,
                '1',
              );
            } catch {
              /* ignore quota / private mode */
            }
            window.setTimeout(() => {
              setClosingAck(true);
            }, 220);
            window.setTimeout(() => {
              setDismissed(true);
            }, 520);
          }}
          className={`shrink-0 rounded-lg border border-blue-200 bg-white px-3 py-2 text-sm font-semibold text-blue-900 transition-all duration-300 ${
            closingAck
              ? 'pointer-events-none scale-95 opacity-0'
              : 'hover:bg-blue-100/80'
          }`}
          aria-label={
            acknowledging ? 'Confirmado, ocultando aviso' : 'No volver a mostrar'
          }
        >
          {acknowledging ? (
            <span className="inline-flex items-center justify-center">
              <Check className="h-4 w-4" aria-hidden />
            </span>
          ) : (
            'No volver a mostrar'
          )}
        </button>
      </div>
    </div>
  );
}

function SidebarNavLink({
  href,
  end,
  icon: Icon,
  children,
  onNavigate,
}: {
  href: string;
  /** When true, only an exact pathname match is active (no prefix match). */
  end?: boolean;
  icon: LucideIcon;
  children: React.ReactNode;
  /** Called after navigation (e.g. close mobile drawer). */
  onNavigate?: () => void;
}) {
  const pathname = usePathname();
  const active =
    href === '/'
      ? pathname === '/'
      : end
        ? pathname === href
        : pathname === href || pathname.startsWith(`${href}/`);

  return (
    <Link
      href={href}
      prefetch={href === '/' ? false : undefined}
      onClick={() => onNavigate?.()}
      className={`flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-semibold transition-colors ${
        active
          ? 'bg-blue-50 text-blue-700'
          : 'text-gray-700 hover:bg-gray-50'
      }`}
    >
      <Icon className="h-4 w-4 shrink-0" aria-hidden />
      {children}
    </Link>
  );
}

function PanelSidebarLinks({
  showOperadoresNav,
  onNavigate,
  className,
}: {
  showOperadoresNav: boolean;
  onNavigate?: () => void;
  className?: string;
}) {
  return (
    <nav className={className}>
      <SidebarNavLink href="/panel" end icon={LayoutDashboard} onNavigate={onNavigate}>
        Panel
      </SidebarNavLink>
      <SidebarNavLink href="/panel/depositos" icon={Warehouse} onNavigate={onNavigate}>
        Depósitos
      </SidebarNavLink>
      <SidebarNavLink href="/panel/dispositivos" icon={Smartphone} onNavigate={onNavigate}>
        Dispositivos
      </SidebarNavLink>
      {showOperadoresNav ? (
        <SidebarNavLink href="/panel/operadores" icon={UserCog} onNavigate={onNavigate}>
          Operadores
        </SidebarNavLink>
      ) : null}
      <SidebarNavLink href="/panel/facturacion" icon={CreditCard} onNavigate={onNavigate}>
        Facturación
      </SidebarNavLink>
      <SidebarNavLink href="/panel/aplicacion" icon={AppWindow} onNavigate={onNavigate}>
        Aplicación
      </SidebarNavLink>
      <SidebarNavLink href="/panel/cambiar-clave" end icon={KeyRound} onNavigate={onNavigate}>
        Contraseña
      </SidebarNavLink>
    </nav>
  );
}

export default function DashboardShell({
  children,
}: {
  children: React.ReactNode;
}) {
  const [showOperadoresNav, setShowOperadoresNav] = useState(false);
  const [mobileNavOpen, setMobileNavOpen] = useState(false);
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => {
    setMobileNavOpen(false);
  }, [pathname]);

  async function handleDrawerLogout() {
    await logoutWebSession();
    setMobileNavOpen(false);
    router.push('/');
    router.refresh();
  }

  useEffect(() => {
    let cancelled = false;
    if (!hasWebSession()) return;
    void (async () => {
      const p = await fetchProfile();
      if (cancelled || !p) return;
      setShowOperadoresNav(canManageOperators(p.role));
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!mobileNavOpen) return;
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setMobileNavOpen(false);
    };
    window.addEventListener('keydown', onKeyDown);
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      window.removeEventListener('keydown', onKeyDown);
      document.body.style.overflow = prevOverflow;
    };
  }, [mobileNavOpen]);

  return (
    <PanelNotificationsProvider>
    <div className="flex min-h-screen bg-gray-50">
      <aside
        className="fixed inset-y-0 left-0 z-40 hidden w-64 flex-col border-r border-gray-200 bg-white md:flex"
        aria-label="Navegación del panel"
      >
        <div className="flex h-24 shrink-0 items-center border-b border-gray-100 px-6">
          <Link href="/" className="flex items-center" prefetch={false}>
            <Image
              src="/enpunto-new.svg"
              alt="En Punto"
              width={140}
              height={25}
              className="h-10 w-auto max-w-[160px]"
              priority
              unoptimized
            />
          </Link>
        </div>
        <PanelSidebarLinks
          showOperadoresNav={showOperadoresNav}
          className="flex flex-col gap-1 p-4"
        />
      </aside>

      <div className="flex min-h-screen w-full min-w-0 flex-1 flex-col md:pl-64">
        <header className="sticky top-0 z-[60] h-24 shrink-0 border-b border-gray-200 bg-white shadow-sm">
          <div className="mx-auto h-full w-full max-w-[90vw] px-4 sm:px-6 md:max-w-[calc(90vw-16rem)] lg:px-8">
            <div className="flex h-full w-full items-center gap-3">
              <button
                type="button"
                className="relative z-10 flex h-10 w-10 shrink-0 items-center justify-center rounded-lg text-gray-700 transition-colors hover:bg-gray-100 md:hidden"
                aria-expanded={mobileNavOpen}
                aria-controls="panel-mobile-drawer"
                aria-label={mobileNavOpen ? 'Cerrar menú de navegación' : 'Abrir menú de navegación'}
                onClick={() => setMobileNavOpen((o) => !o)}
              >
                {mobileNavOpen ? (
                  <X className="h-6 w-6" aria-hidden strokeWidth={2} />
                ) : (
                  <Menu className="h-6 w-6" aria-hidden strokeWidth={2} />
                )}
              </button>
              <div className="flex min-w-0 flex-1 items-center justify-start gap-3 md:hidden">
                <Link href="/" className="flex shrink-0 items-center" prefetch={false}>
                  <Image
                    src="/enpunto-new.svg"
                    alt="En Punto"
                    width={140}
                    height={25}
                    className="h-9 w-auto max-w-[140px]"
                    unoptimized
                  />
                </Link>
                <PanelNotificationBell className="shrink-0" />
              </div>
              <div className="relative z-10 ml-auto flex shrink-0 items-center gap-2 md:flex-1 md:justify-end md:gap-3">
                <div className="hidden items-center gap-3 md:flex">
                  <PanelNotificationBell className="hidden shrink-0 md:inline-flex" />
                  <div
                    className="hidden h-8 w-px shrink-0 bg-gray-200 md:block"
                    aria-hidden
                  />
                </div>
                <HeaderAuthNav />
              </div>
            </div>
          </div>
        </header>

        {/* Mobile nav drawer (same links as desktop sidebar); sits below sticky header */}
        <div className="md:hidden">
          <div
            className={`fixed inset-0 z-[45] bg-black/40 transition-opacity duration-300 ease-out motion-reduce:transition-none ${
              mobileNavOpen ? 'opacity-100' : 'pointer-events-none opacity-0'
            }`}
            aria-hidden={!mobileNavOpen}
            onClick={() => setMobileNavOpen(false)}
          />
          <div
            id="panel-mobile-drawer"
            role="dialog"
            aria-modal="true"
            aria-label="Menú de navegación del panel"
            className={`fixed bottom-0 left-0 top-24 z-[55] flex w-[min(18rem,85vw)] flex-col border-r border-gray-200 bg-white shadow-xl transition-transform duration-300 ease-out motion-reduce:transition-none ${
              mobileNavOpen ? 'translate-x-0' : '-translate-x-full pointer-events-none'
            }`}
          >
            <div className="flex min-h-0 flex-1 flex-col">
              <div className="min-h-0 flex-1 overflow-y-auto">
                <PanelSidebarLinks
                  showOperadoresNav={showOperadoresNav}
                  onNavigate={() => setMobileNavOpen(false)}
                  className="flex flex-col gap-1 px-4 pb-4 pt-4"
                />
              </div>
              <div className="shrink-0 border-t border-gray-100 p-4 pt-3">
                <button
                  type="button"
                  onClick={() => void handleDrawerLogout()}
                  className="flex w-full items-center justify-center gap-2 rounded-lg border border-gray-300 bg-white px-3 py-2.5 text-sm font-semibold text-gray-800 hover:bg-gray-50"
                >
                  <LogOut className="h-4 w-4 shrink-0" aria-hidden strokeWidth={2} />
                  Cerrar sesión
                </button>
              </div>
            </div>
          </div>
        </div>
        <div className="flex-1">
          <PanelMobileDesktopHint />
          <ActivateSubscriptionGate>{children}</ActivateSubscriptionGate>
        </div>
      </div>
    </div>
    </PanelNotificationsProvider>
  );
}
