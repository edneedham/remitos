import {
  ArrowRight,
} from 'lucide-react';
import Image from 'next/image';
import Link from 'next/link';
import HomeGate from './HomeGate';
import HeroSignupRow from './ui/components/website/HeroSignupRow';
import HeroQrOverlay from './ui/components/website/HeroQrOverlay';
import BenefitsSection from './ui/components/website/BenefitsSection';
import HowItWorksSection from './ui/components/website/HowItWorksSection';

export default function Home() {
  return (
    <HomeGate>
      <div className="flex flex-col min-h-screen">
        <main className="grow">
          {/* Hero Section */}
          <div className="border-b border-gray-200 bg-white">
            <section className="py-20 px-4 sm:px-6 lg:px-8">
              <div className="mx-auto mt-16 w-full max-w-[70vw] rounded-3xl bg-blue-50 px-8 pt-8 pb-0 sm:px-10 lg:px-12">
                <div className="flex flex-col items-center gap-10 lg:min-h-[460px] lg:flex-row lg:items-stretch lg:justify-between lg:gap-8">
                  <div className="flex w-full flex-1 flex-col justify-center lg:max-w-xl lg:pr-2">
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
                  <div className="relative flex w-full max-w-[420px] shrink-0 flex-col justify-end lg:ml-auto">
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

          <BenefitsSection />

          <HowItWorksSection />

          <section
            className="border-b border-gray-200 bg-gray-50 py-20 px-4 sm:px-6 lg:px-8"
            aria-labelledby="who-is-it-for-heading"
          >
            <div className="mx-auto w-full max-w-[80vw]">
              <div className="mx-auto mb-12 max-w-content-prose text-center">
                <h2
                  id="who-is-it-for-heading"
                  className="mb-4 text-3xl font-bold text-gray-900 sm:text-4xl"
                >
                  Para cada rol de la operación
                </h2>
                <p className="text-lg text-gray-600">
                  Cada equipo ve lo que necesita para ejecutar mejor, sin perder
                  contexto entre áreas.
                </p>
              </div>

              <div className="grid grid-cols-1 gap-6 md:grid-cols-3">
                <article className="rounded-2xl border border-gray-200 bg-white p-6">
                  <h3 className="mb-2 text-lg font-semibold text-gray-900">
                    Depósito
                  </h3>
                  <p className="text-sm leading-relaxed text-gray-600">
                    Prepará salidas con datos claros y evitá rehacer armado por
                    información incompleta.
                  </p>
                </article>
                <article className="rounded-2xl border border-gray-200 bg-white p-6">
                  <h3 className="mb-2 text-lg font-semibold text-gray-900">
                    Administración
                  </h3>
                  <p className="text-sm leading-relaxed text-gray-600">
                    Exportá en CSV y cerrá más rápido sin volver a pasar datos a
                    otras herramientas.
                  </p>
                </article>
                <article className="rounded-2xl border border-gray-200 bg-white p-6">
                  <h3 className="mb-2 text-lg font-semibold text-gray-900">
                    Chofer
                  </h3>
                  <p className="text-sm leading-relaxed text-gray-600">
                    Recibí una hoja de ruta PDF ordenada para salir con menos dudas
                    y menos llamadas.
                  </p>
                </article>
              </div>
            </div>
          </section>

          <section
            className="border-b border-gray-200 bg-white py-20 px-4 sm:px-6 lg:px-8"
            aria-labelledby="faq-heading"
          >
            <div className="mx-auto w-full max-w-[80vw]">
              <div className="mx-auto mb-12 max-w-content-prose text-center">
                <h2
                  id="faq-heading"
                  className="mb-4 text-3xl font-bold text-gray-900 sm:text-4xl"
                >
                  Preguntas frecuentes
                </h2>
              </div>

              <div className="mx-auto max-w-3xl space-y-4">
                <article className="rounded-xl border border-gray-200 bg-gray-50 p-5">
                  <h3 className="text-base font-semibold text-gray-900">
                    ¿Funciona en Android?
                  </h3>
                  <p className="mt-2 text-sm leading-relaxed text-gray-600">
                    Sí. En Punto está disponible para dispositivos Android.
                  </p>
                </article>
                <article className="rounded-xl border border-gray-200 bg-gray-50 p-5">
                  <h3 className="text-base font-semibold text-gray-900">
                    ¿Cuánto tarda la implementación?
                  </h3>
                  <p className="mt-2 text-sm leading-relaxed text-gray-600">
                    Podés empezar el mismo día. El onboarding está pensado para que
                    tu equipo cargue sus primeros remitos en minutos.
                  </p>
                </article>
                <article className="rounded-xl border border-gray-200 bg-gray-50 p-5">
                  <h3 className="text-base font-semibold text-gray-900">
                    ¿Cómo pruebo la plataforma?
                  </h3>
                  <p className="mt-2 text-sm leading-relaxed text-gray-600">
                    Tenés una prueba gratis de 7 días para validar el flujo completo
                    con tu propia operación.
                  </p>
                </article>
              </div>
            </div>
          </section>

          {/* CTA Section */}
          <section className="bg-gray-900 py-20 px-4 sm:px-6 lg:px-8">
            <div className="mx-auto w-full max-w-[80vw] text-center">
              <h2 className="text-3xl font-bold text-white mb-6">
                ¿Listo para probarlo en tu operación?
              </h2>
              <p className="text-xl text-gray-300 mb-8">
                Empezá hoy con 7 días gratis. Disponible para Android y con soporte
                para tu equipo desde el primer día.
              </p>
              <div className="flex flex-col items-center justify-center gap-4 sm:flex-row">
                <Link
                  href="/signup"
                  className="inline-flex items-center px-8 py-4 bg-white text-gray-900 font-semibold rounded-lg hover:bg-gray-100 transition-colors duration-200 text-lg"
                >
                  Probar en{' '}
                  <Image
                    src="/brands/android-head_flat.svg"
                    alt=""
                    width={152}
                    height={89}
                    className="mx-1 inline-block h-[1.15em] w-auto shrink-0 align-[-0.12em]"
                    unoptimized
                    aria-hidden
                  />
                  Android
                  <ArrowRight className="ml-2 h-5 w-5 shrink-0" />
                </Link>
              </div>
            </div>
          </section>
        </main>
      </div>
    </HomeGate>
  );
}
