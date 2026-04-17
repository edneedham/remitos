'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import Image from 'next/image';

export default function Header() {
  const pathname = usePathname();

  const isActive = (path: string) => {
    return pathname === path
      ? 'text-blue-600 font-bold'
      : 'text-gray-600 hover:text-blue-500';
  };

  return (
    <header className="bg-white shadow-sm relative z-10">
      <div className="max-w-content-wide mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16">
          <div className="shrink-0 flex items-center">
            <Link
              href="/"
              className="text-xl font-bold text-blue-600 flex items-center"
              prefetch={false}
            >
              <Image
                src="/enpunto-new.svg"
                alt="En Punto"
                width={140}
                height={25}
                className="h-8 w-auto max-w-[180px] sm:h-9"
                priority
                unoptimized
              />
            </Link>
          </div>
          <div className="hidden sm:ml-6 sm:flex sm:items-center flex-1 justify-end">
            <nav className="flex space-x-8">
              <Link
                href="/contact"
                className={`inline-flex items-center px-1 pt-1 border-b-2 ${
                  pathname === '/contact'
                    ? 'border-blue-500'
                    : 'border-transparent'
                } ${isActive('/contact')}`}
              >
                Contacto
              </Link>
            </nav>
          </div>
        </div>
      </div>
    </header>
  );
}
