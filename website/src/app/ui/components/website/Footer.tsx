'use client';

import Link from 'next/link';
import Image from 'next/image';
import { usePathname } from 'next/navigation';

export default function Footer() {
  const pathname = usePathname();
  if (pathname.startsWith('/panel')) {
    return null;
  }

  return (
    <footer className="border-t border-gray-200 bg-white p-6">
      <div className="mx-auto flex w-full max-w-[80rem] flex-col items-center justify-between space-y-4 px-4 sm:px-6 md:flex-row md:space-y-0 lg:px-8">
        <p className="text-sm text-gray-600">
          © {new Date().getFullYear()} En Punto. Todos los derechos reservados.
        </p>

        <div className="flex flex-col md:flex-row space-y-2 md:space-y-0 md:space-x-4 text-sm">
          <Link
            href="/contacto"
            className="text-gray-600 underline-offset-4 transition-colors hover:text-gray-900 hover:underline"
          >
            Contacto
          </Link>
          <Link
            href="/privacidad"
            className="text-gray-600 underline-offset-4 transition-colors hover:text-gray-900 hover:underline"
          >
            Política de privacidad
          </Link>
          <Link
            href="/terminos"
            className="text-gray-600 underline-offset-4 transition-colors hover:text-gray-900 hover:underline"
          >
            Términos y condiciones
          </Link>
          <Link
            href="/cookies"
            className="text-gray-600 underline-offset-4 transition-colors hover:text-gray-900 hover:underline"
          >
            Cookies
          </Link>
        </div>

        <div className="flex items-center gap-3">
            <Link
              href="https://www.x.com/enpuntoapp"
              aria-label="X/Twitter"
              className="inline-flex size-6 items-center justify-center opacity-80 transition-opacity hover:opacity-100"
            >
              <Image
                src="/logo-black.png"
                width={20}
                height={20}
                className="size-5 object-contain"
                alt="X logo"
              />
            </Link>
            <Link
              href="https://www.linkedin.com/company/roasal/"
              aria-label="LinkedIn"
              className="inline-flex size-6 items-center justify-center opacity-80 transition-opacity hover:opacity-100"
            >
              <Image
                src="/InBug-Black.png"
                width={20}
                height={20}
                className="size-5 object-contain"
                alt="LinkedIn logo"
              />
            </Link>
          </div>
      </div>

      <p className="mx-auto mt-4 max-w-[80rem] border-t border-gray-200 px-4 pt-4 text-center text-[11px] leading-snug text-gray-500 sm:px-6 lg:px-8">
        Android y el logotipo de Android, Google Drive y el logotipo de Google Drive son marcas
        comerciales de Google LLC.
      </p>
    </footer>
  );
}
