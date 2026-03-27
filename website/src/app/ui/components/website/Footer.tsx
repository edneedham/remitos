import Link from 'next/link';
import Image from 'next/image';

export default function Footer() {
  return (
    <footer className="bg-gray-800 text-white p-6">
      <div className="container mx-auto flex flex-col md:flex-row justify-between items-center space-y-4 md:space-y-0">
        <p className="text-sm text-gray-400">
          © {new Date().getFullYear()} En Punto. Todos los derechos reservados.
        </p>

        <div className="flex flex-col md:flex-row space-y-2 md:space-y-0 md:space-x-4 text-sm">
          <Link
            href="/privacy"
            className="text-gray-300 hover:text-white transition-colors"
          >
            Política de privacidad
          </Link>
          <Link
            href="/terms"
            className="text-gray-300 hover:text-white transition-colors"
          >
            Términos y condiciones
          </Link>
          <Link
            href="/cookies"
            className="text-gray-300 hover:text-white transition-colors"
          >
            Cookies
          </Link>
          <Link
            href="/licenses"
            className="text-gray-300 hover:text-white transition-colors"
          >
            Licencias
          </Link>
        </div>

        <div className="flex flex-col md:flex-row items-center space-y-2 md:space-y-0 md:space-x-4">
          <div className="flex space-x-3">
            <Link
              href="https://www.x.com/enpuntoapp"
              aria-label="X/Twitter"
              className="text-gray-400 hover:text-white transition-colors"
            >
              <Image
                src="/logo-white.png"
                width="20"
                height="20"
                alt="X logo in white"
              />
            </Link>
            <Link
              href="https://www.linkedin.com/company/roasal/"
              aria-label="LinkedIn"
              className="text-gray-400 hover:text-white transition-colors"
            >
              <Image
                src="/InBug-White.png"
                width="24"
                height="24"
                alt="Linkedin logo in white"
              />
            </Link>
          </div>
        </div>
      </div>
    </footer>
  );
}
