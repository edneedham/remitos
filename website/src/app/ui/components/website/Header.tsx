'use client';

import Link from 'next/link';
import Image from 'next/image';
import { usePathname } from 'next/navigation';
import HeaderAuthNav from './HeaderAuthNav';

export default function Header() {
  const pathname = usePathname();
  if (pathname.startsWith('/dashboard')) {
    return null;
  }

  return (
    <header className="relative z-10 bg-white shadow-sm">
      <div className="mx-auto w-full max-w-[80vw] px-4 sm:px-6 lg:px-8">
        <div className="flex h-24 items-center justify-between gap-3">
          <div className="flex shrink-0 items-center gap-10">
            <Link
              href="/"
              className="flex items-center rounded-md text-xl font-bold text-blue-600 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-500/60 focus-visible:ring-offset-2"
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
            <Link
              href="/pricing"
              className="text-sm font-semibold text-gray-700 transition-colors hover:text-gray-900"
            >
              Precios
            </Link>
          </div>
          <HeaderAuthNav />
        </div>
      </div>
    </header>
  );
}
