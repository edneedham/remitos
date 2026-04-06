import {
  Smartphone,
  ArrowRight,
  CheckCircle,
} from 'lucide-react';
import Link from 'next/link';
import AndroidFrame from './ui/components/website/AndroidFrame';

export default function Home() {
  return (
    <div className="flex flex-col min-h-screen">
      <main className="grow">
        {/* Hero Section */}
        <div className="border-b border-gray-200">
          <section className="w-3/5 mx-auto py-20 px-8 bg-white">
          <div className="mt-16">
            <h1 className="text-3xl sm:text-4xl lg:text-5xl font-light mb-8 leading-tight">
              Herramientas de logística para manejar tu negocio.{' '}
              <span className="opacity-80">
                Reducí la carga manual de datos, generá hojas de reparto y
                exportá la info para usarla en otras plataformas.
              </span>
            </h1>
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
        </section>
        </div>

        {/* Solutions Section */}
        <section className="py-20 px-8 bg-gray-50">
          <div className="max-w-6xl mx-auto">
            <h2 className="text-3xl font-bold text-gray-900 mb-12 text-center">
              Nuestras Soluciones
            </h2>

            <div className="flex items-center justify-center gap-12">
              <div className="max-w-lg">
                {/* Remitos - Inlog App */}
                <div className="bg-white border border-gray-200 p-8 rounded-lg shadow-sm hover:shadow-md transition-shadow duration-300">
                  <div className="text-emerald-700 mb-6 flex justify-center">
                    <Smartphone size={64} />
                  </div>
                  <h3 className="text-2xl font-bold text-gray-900 mb-4 text-center">
                    App de Repartos
                  </h3>
                  <p className="text-gray-700 mb-6 text-center">
                    Aplicación Android para el manejo de remitos y repartos.
                    Escanea con OCR, gestiona listas de reparto y realiza seguimiento
                    de entregas.
                  </p>
                  <ul className="space-y-3 mb-8">
                    <li className="flex items-start">
                      <CheckCircle
                        className="text-emerald-700 mt-1 mr-3 flex-shrink-0"
                        size={20}
                      />
                      <span className="text-gray-700">
                        Escaneo de remitos con OCR
                      </span>
                    </li>
                    <li className="flex items-start">
                      <CheckCircle
                        className="text-emerald-700 mt-1 mr-3 flex-shrink-0"
                        size={20}
                      />
                      <span className="text-gray-700">
                        Gestión de listas de reparto
                      </span>
                    </li>
                    <li className="flex items-start">
                      <CheckCircle
                        className="text-emerald-700 mt-1 mr-3 flex-shrink-0"
                        size={20}
                      />
                      <span className="text-gray-700">100% Offline</span>
                    </li>
                  </ul>
                  <div className="text-center">
                    <Link
                      href="/remitos"
                      className="inline-flex items-center px-6 py-3 bg-emerald-700 text-white font-semibold rounded-lg hover:bg-emerald-800 transition-colors duration-200"
                    >
                      Ver App
                      <ArrowRight className="ml-2 h-4 w-4" />
                    </Link>
                  </div>
                </div>
              </div>
              <div className="shrink-0">
                <AndroidFrame />
              </div>
            </div>
          </div>
        </section>

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
