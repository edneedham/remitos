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
import PricingPlansSection from './ui/components/website/PricingPlansSection';

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
                    <h1 className="mb-8 text-3xl font-bold leading-tight sm:text-4xl lg:text-5xl xl:text-6xl">
                      Herramientas de logística para manejar tu negocio.
                    </h1>
                    <p className="mb-8 text-lg font-medium leading-snug text-gray-600 sm:text-xl lg:text-2xl xl:text-3xl">
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

          <PricingPlansSection />

          <section
            className="border-b border-gray-200 bg-white py-20 px-4 sm:px-6 lg:px-8"
            aria-labelledby="faq-heading"
          >
            <div className="mx-auto w-full max-w-[80vw]">
              <div className="mx-auto mb-12 max-w-content-prose text-center">
                <h2
                  id="faq-heading"
                  className="mb-4 text-3xl font-bold text-gray-900 sm:text-4xl lg:text-5xl"
                >
                  Preguntas frecuentes
                </h2>
              </div>

              <div className="mx-auto max-w-3xl space-y-4">
                <article className="rounded-xl border border-gray-200 bg-gray-50 p-5 sm:p-6">
                  <h3 className="text-base font-semibold text-gray-900 sm:text-lg lg:text-xl">
                    ¿Funciona en Android?
                  </h3>
                  <p className="mt-2 text-sm leading-relaxed text-gray-600 sm:text-base lg:text-lg">
                    Sí. En Punto está disponible para dispositivos Android.
                  </p>
                </article>
                <article className="rounded-xl border border-gray-200 bg-gray-50 p-5 sm:p-6">
                  <h3 className="text-base font-semibold text-gray-900 sm:text-lg lg:text-xl">
                    ¿Cuánto tarda la implementación?
                  </h3>
                  <p className="mt-2 text-sm leading-relaxed text-gray-600 sm:text-base lg:text-lg">
                    Podés empezar el mismo día. El onboarding está pensado para que
                    tu equipo cargue sus primeros remitos en minutos.
                  </p>
                </article>
                <article className="rounded-xl border border-gray-200 bg-gray-50 p-5 sm:p-6">
                  <h3 className="text-base font-semibold text-gray-900 sm:text-lg lg:text-xl">
                    ¿Cómo pruebo la plataforma?
                  </h3>
                  <p className="mt-2 text-sm leading-relaxed text-gray-600 sm:text-base lg:text-lg">
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
              <h2 className="mb-6 text-3xl font-bold text-white sm:text-4xl lg:text-5xl">
                ¿Listo para probarlo en tu operación?
              </h2>
              <p className="mb-8 text-lg leading-snug text-gray-300 sm:text-xl lg:text-2xl">
                Empezá hoy con 7 días gratis. Disponible para Android y con soporte
                para tu equipo desde el primer día.
              </p>
              <div className="flex flex-col items-center justify-center gap-4 sm:flex-row">
                <Link
                  href="/signup"
                  className="inline-flex items-center rounded-lg bg-white px-8 py-4 text-base font-semibold text-gray-900 transition-colors duration-200 hover:bg-gray-100 sm:text-lg"
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
