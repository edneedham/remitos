'use client';

import Link from 'next/link';
import Image from 'next/image';
import { usePathname } from 'next/navigation';
import HeaderAuthNav from '../ui/components/website/HeaderAuthNav';

function SidebarNavLink({
  href,
  end,
  children,
}: {
  href: string;
  /** When true, only an exact pathname match is active (no prefix match). */
  end?: boolean;
  children: React.ReactNode;
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
      className={`rounded-lg px-3 py-2 text-sm font-semibold transition-colors ${
        active
          ? 'bg-blue-50 text-blue-700'
          : 'text-gray-700 hover:bg-gray-50'
      }`}
    >
      {children}
    </Link>
  );
}

export default function DashboardShell({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
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
        <nav className="flex flex-col gap-1 p-4">
          <SidebarNavLink href="/dashboard" end>
            Panel
          </SidebarNavLink>
          <SidebarNavLink href="/dashboard/billing">Facturación</SidebarNavLink>
          <SidebarNavLink href="/dashboard/app">Aplicación</SidebarNavLink>
        </nav>
      </aside>

      <div className="flex min-h-screen w-full min-w-0 flex-1 flex-col md:pl-64">
        <header className="sticky top-0 z-30 h-24 shrink-0 border-b border-gray-200 bg-white shadow-sm">
          <div className="mx-auto h-full w-full max-w-[80vw] px-4 sm:px-6 md:max-w-[calc(80vw-16rem)] lg:px-8">
            <div className="flex h-full items-center justify-between gap-3">
              <div className="flex min-w-0 flex-1 items-center md:hidden">
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
              </div>
              <div className="flex shrink-0 items-center justify-end md:ml-auto md:flex-1">
                <HeaderAuthNav />
              </div>
            </div>
          </div>
        </header>
        <div className="flex-1">{children}</div>
      </div>
    </div>
  );
}
