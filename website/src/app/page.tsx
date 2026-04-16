import {
  ArrowRight,
} from 'lucide-react';
import Image from 'next/image';
import Link from 'next/link';

export default function Home() {
  return (
    <div className="flex flex-col min-h-screen">
      <main className="grow">
        {/* Hero Section */}
        <div className="border-b border-gray-200">
          <section className="w-full max-w-7xl mx-auto py-20 px-8 bg-white">
            <div className="mt-16 rounded-3xl bg-blue-50 px-8 pt-8 pb-0">
              <div className="flex flex-col lg:flex-row items-center lg:items-start gap-12">
                <div className="flex-1 lg:pt-[7%]">
                  <h1 className="text-3xl sm:text-4xl lg:text-5xl font-bold mb-8 leading-tight">
                    Herramientas de logística para manejar tu negocio.
                  </h1>
                  <p className="text-2xl sm:text-3xl lg:text-4xl font-semibold text-gray-600 mb-8 leading-tight">
                    Reducí la carga manual de datos, generá hojas de reparto y
                    exportá la info para usarla en otras plataformas.
                  </p>
                  <div className="flex items-center gap-4">
                    <Link
                      href="/signup"
                      className="inline-flex items-center px-6 py-3 bg-blue-600 text-white font-semibold rounded-lg hover:bg-blue-700 transition-colors duration-200"
                    >
                      Arrancá hoy
                      <ArrowRight className="ml-2 h-4 w-4" />
                    </Link>
                    <Link
                      href="#"
                      className="inline-flex items-center px-6 py-3 bg-white text-gray-900 font-semibold rounded-lg border border-gray-900 hover:bg-gray-50 transition-colors duration-200"
                    >
                      Agendá una demo
                    </Link>
                  </div>
                </div>
                <div className="shrink-0 h-[470px] overflow-hidden rounded-t-3xl shadow-2xl lg:mt-28">
                  <Image
                    src="/screenshots/dashboard.png"
                    alt="Dashboard de la app de repartos"
                    width={320}
                    height={680}
                    className="w-[420px] h-auto max-w-none"
                    priority
                  />
                </div>
              </div>
            </div>
          </section>
        </div>

        {/* CTA Section */}
        <section className="py-20 px-8 bg-gray-900">
          <div className="max-w-4xl mx-auto text-center">
            <h2 className="text-3xl font-bold text-white mb-6">
              ¿Necesitas una solución personalizada?
            </h2>
            <p className="text-xl text-gray-300 mb-8">
              Contáctanos para discutir cómo podemos ayudarte a optimizar tus
              procesos.
            </p>
            <Link
              href="/contact"
              className="inline-flex items-center px-8 py-4 bg-white text-gray-900 font-semibold rounded-lg hover:bg-gray-100 transition-colors duration-200 text-lg"
            >
              Contactanos
              <ArrowRight className="ml-2 h-5 w-5" />
            </Link>
          </div>
        </section>
      </main>
    </div>
  );
}
