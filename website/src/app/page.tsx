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
        <div className="border-b border-gray-200">
          <section className="w-full max-w-content-wide mx-auto py-20 px-8 bg-white">
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
                  <HeroSignupRow />
                </div>
                <div className="relative shrink-0 w-full max-w-[340px] lg:mt-28">
                  <div className="h-[380px] overflow-hidden rounded-t-3xl">
                    <Image
                      src="/screenshots/dashboard.png"
                      alt="Dashboard de la app de repartos"
                      width={320}
                      height={680}
                      className="w-[340px] h-auto max-w-none"
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
          className="py-20 px-8 bg-gray-50 border-b border-gray-200"
          aria-labelledby="workflow-heading"
        >
          <div className="max-w-content-wide mx-auto">
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
        <section className="py-20 px-8 bg-gray-900">
          <div className="max-w-content-narrow mx-auto text-center">
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
