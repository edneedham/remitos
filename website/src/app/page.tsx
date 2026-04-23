import {
  ArrowRight,
} from 'lucide-react';
import Image from 'next/image';
import Link from 'next/link';
import HeroSignupRow from './ui/components/website/HeroSignupRow';
import HeroQrOverlay from './ui/components/website/HeroQrOverlay';

export default function Home() {
  return (
    <div className="flex flex-col min-h-screen">
      <main className="grow">
        {/* Hero Section */}
        <div className="border-b border-gray-200 bg-white">
          <section className="py-20 px-4 sm:px-6 lg:px-8">
            <div className="mx-auto mt-16 w-full max-w-[80vw] rounded-3xl bg-blue-50 px-6 pt-8 pb-0 sm:px-8">
              <div className="flex flex-col items-center gap-12 lg:min-h-[460px] lg:flex-row lg:items-stretch">
                <div className="flex w-full flex-1 flex-col justify-center lg:max-w-xl lg:pr-6">
                  <h1 className="text-3xl sm:text-4xl lg:text-5xl font-bold mb-8 leading-tight">
                    Herramientas de logística para manejar tu negocio.
                  </h1>
                  <p className="text-lg sm:text-xl lg:text-2xl font-medium text-gray-600 mb-8 leading-snug">
                    Reducí la carga manual de datos, generá hojas de reparto y
                    exportá la info para usarla en otras plataformas.
                  </p>
                  <div className="mb-10 lg:mb-14">
                    <HeroSignupRow />
                  </div>
                </div>
                <div className="relative flex w-full max-w-[420px] shrink-0 flex-col justify-end lg:mx-0">
                  <div className="h-[460px] overflow-hidden rounded-t-3xl">
                    <Image
                      src="/screenshots/dashboard.png"
                      alt="Dashboard de la app de repartos"
                      width={400}
                      height={850}
                      className="h-auto w-full max-w-[420px]"
                      priority
                    />
                  </div>
                  <HeroQrOverlay />
                </div>
              </div>
            </div>
          </section>
        </div>

        {/* Workflow: scan → reparto → PDF → export */}
        <section
          className="border-b border-gray-200 bg-gray-50 py-20 px-4 sm:px-6 lg:px-8"
          aria-labelledby="workflow-heading"
        >
          <div className="mx-auto w-full max-w-[80vw]">
            <div className="text-center max-w-content-prose mx-auto mb-14">
              <h2
                id="workflow-heading"
                className="text-3xl sm:text-4xl font-bold text-gray-900 mb-4"
              >
                Del remito al PDF y a tus planillas
              </h2>
              <p className="text-lg text-gray-600">
                Un flujo continuo: capturás datos, armás el reparto, entregás un
                PDF al chofer y exportás la información para contabilidad u otras
                herramientas.
              </p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-12 xl:gap-8">
              <div className="flex flex-col items-center text-center">
                <Image
                  src="/screenshots/scan.png"
                  alt="Pantalla de escaneo y captura de remito"
                  width={320}
                  height={680}
                  className="mb-4 h-auto w-full max-w-[260px]"
                  priority
                />
                <h3 className="text-lg font-semibold text-gray-900 mb-1">
                  1. Escaneá el remito
                </h3>
                <p className="text-sm text-gray-600 max-w-xs">
                  Capturá la imagen y extraé los datos que necesitás para el
                  ingreso.
                </p>
              </div>

              <div className="flex flex-col items-center text-center">
                <Image
                  src="/screenshots/reparto-form.png"
                  alt="Formulario de armado de lista de reparto"
                  width={320}
                  height={680}
                  className="mb-4 h-auto w-full max-w-[260px]"
                />
                <h3 className="text-lg font-semibold text-gray-900 mb-1">
                  2. Armá la lista de reparto
                </h3>
                <p className="text-sm text-gray-600 max-w-xs">
                  Organizá qué sale en cada salida y asigná bultos al chofer.
                </p>
              </div>

              <div className="flex flex-col items-center text-center">
                <Image
                  src="/screenshots/reparto-pdf.png"
                  alt="Vista previa del PDF de hoja de reparto"
                  width={320}
                  height={680}
                  className="mb-4 h-auto w-full max-w-[260px]"
                />
                <h3 className="text-lg font-semibold text-gray-900 mb-1">
                  3. PDF para el chofer
                </h3>
                <p className="text-sm text-gray-600 max-w-xs">
                  Generá un documento claro para la ruta y la firma en destino.
                </p>
              </div>

              <div className="flex flex-col items-center text-center">
                <Image
                  src="/screenshots/exportar-csv.png"
                  alt="Opciones para exportar datos a CSV"
                  width={320}
                  height={680}
                  className="mb-4 h-auto w-full max-w-[260px]"
                />
                <h3 className="text-lg font-semibold text-gray-900 mb-1">
                  4. Exportá para el depósito
                </h3>
                <p className="text-sm text-gray-600 max-w-xs">
                  Llevá los datos a Excel, tu contador o el sistema que uses.
                </p>
              </div>
            </div>
          </div>
        </section>

        {/* CTA Section */}
        <section className="bg-gray-900 py-20 px-4 sm:px-6 lg:px-8">
          <div className="mx-auto w-full max-w-[80vw] text-center">
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
