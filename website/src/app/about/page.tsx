import Link from 'next/link';
import { Truck } from 'lucide-react';
import Image from 'next/image';

export default async function About() {
  return (
    <div className="flex flex-col min-h-screen">
      <main className="grow">
        {/* Hero Section */}
        <section className="py-20 px-8 bg-white">
          <div className="max-w-6xl mx-auto">
            <h1 className="text-4xl font-bold text-blue-600 mb-12 text-center">
              Empresas y conductores, ahora mas cerca
            </h1>

            <p className="text-xl text-gray-600 mb-8 max-w-3xl mx-auto leading-relaxed text-center">
              Enpunto nació de una necesidad real. Cansados de la informalidad,
              la incertidumbre y las herramientas obsoletas, decidimos construir
              la plataforma que siempre quisimos usar: un mercado de transporte
              transparente, eficiente y basado en la confianza.
            </p>

            <div className="text-center">
              <Link
                href="/signup"
                className="inline-block bg-blue-600 text-white px-8 py-4 rounded-lg text-lg font-semibold hover:bg-blue-600 transition-colors"
              >
                Crear Cuenta Gratis
              </Link>
            </div>
          </div>
        </section>

        {/* Problem Section */}
        <section className="py-20 px-8 bg-gray-50">
          <div className="max-w-6xl mx-auto">
            <h2 className="text-4xl font-bold text-gray-800 mb-12 text-center">
              Hecho por y para profesionales del sector
            </h2>

            <div className="grid md:grid-cols-2 gap-12">
              {/* Customers Column */}
              <div className="text-center">
                <div className="w-16 h-16 mx-auto mb-6 bg-blue-100 rounded-lg flex items-center justify-center">
                  <svg
                    className="w-8 h-8 text-blue-600"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"
                    />
                  </svg>
                </div>

                <h3 className="text-2xl font-bold text-gray-800 mb-4">
                  Para Clientes
                </h3>

                <p className="text-gray-600 leading-relaxed">
                  Encontrar un transportista confiable para tu envío no debería
                  ser un juego de azar. Las interminables llamadas telefónicas,
                  la incertidumbre de los precios y la falta de un proceso de
                  verificación confiable desperdician tiempo y dinero.
                  Construimos Enpunto para darte acceso a una red de
                  transportistas aprobados y profesionales al alcance de tu
                  mano.
                </p>
              </div>

              {/* Vendors Column */}
              <div className="text-center">
                <div className="w-16 h-16 mx-auto mb-6 bg-green-100 rounded-lg flex items-center justify-center">
                  <Truck className="w-8 h-8 text-green-600" />
                </div>

                <h3 className="text-2xl font-bold text-gray-800 mb-4">
                  Para Transportistas
                </h3>

                <p className="text-gray-600 leading-relaxed">
                  Tu tiempo es valioso. Buscar trabajo legítimo a través de
                  grupos informales y esperar los pagos no debería ser tu
                  trabajo principal. En Punto trae solicitudes de servicio
                  verificadas y bien definidas directamente a ti, permitiéndote
                  enfocarte en lo que mejor sabes hacer: conducir. Construye tu
                  reputación y haz crecer tu negocio en una plataforma que
                  valora tu profesionalismo.
                </p>
              </div>
            </div>
          </div>
        </section>

        {/* Founders Section */}
        <section className="py-20 px-8 bg-white">
          <div className="max-w-6xl mx-auto">
            <h2 className="text-4xl font-bold text-gray-800 mb-12 text-center">
              Un equipo con experiencia real
            </h2>

            <div className="grid md:grid-cols-2 gap-12">
              {/* Co-Founder 1 */}
              <div className="text-center p-6 bg-white rounded-lg shadow-md">
                <div className="w-48 h-48 mx-auto mb-6 bg-gray-200 rounded-full flex items-center justify-center overflow-hidden">
                  <Image
                    src="/Me.jpg"
                    alt="Edward Needham"
                    width={192}
                    height={192}
                    className="rounded-full object-cover scale-110"
                  />
                </div>

                <h3 className="text-2xl font-bold text-gray-800 mb-2">
                  Edward Needham
                </h3>
                <p className="text-blue-600 font-semibold mb-4">
                  Co-Fundador y Líder de Tecnología
                </p>

                <p className="text-gray-600 leading-relaxed mb-4">
                  Con pasión por construir software robusto y escalable, soy
                  responsable de toda la arquitectura técnica de En Punto. Mi
                  objetivo es crear una plataforma fluida, intuitiva y segura
                  que empodere a nuestros usuarios y simplifique sus operaciones
                  diarias.
                </p>
                <div className="flex items-center justify-center">
                  <Link
                    href="https://x.com/needhame"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-blue-600 hover:text-blue-700 font-semibold p-2"
                  >
                    <Image
                      src="/logo-black.png"
                      width="20"
                      height="20"
                      alt="X logo in black"
                    />
                  </Link>
                  <Link
                    href="https://linkedin.com/in/edward-needham/"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-blue-600 hover:text-blue-700 font-semibold p-2"
                  >
                    <Image
                      src="/InBug-Black.png"
                      width="24"
                      height="24"
                      alt="Linkedin logo in black"
                    />
                  </Link>
                </div>
              </div>

              {/* Co-Founder 2 */}
              <div className="text-center p-6 bg-white rounded-lg shadow-md">
                <div className="w-48 h-48 mx-auto mb-6 bg-gray-200 rounded-full flex items-center justify-center">
                  <Image
                    src="/Romina.jpg"
                    alt="Romina Guzman"
                    width={192}
                    height={192}
                    className="rounded-full object-cover"
                  />
                </div>

                <h3 className="text-2xl font-bold text-gray-800 mb-2">
                  Romina Guzman
                </h3>
                <p className="text-blue-600 font-semibold mb-4">
                  Co-Fundadora y Líder de Operaciones
                </p>

                <p className="text-gray-600 leading-relaxed mb-4">
                  Habiendo trabajado directamente en la industria logística y de
                  transporte durante años, vi las frustraciones diarias de
                  primera mano. Traigo el conocimiento operativo del mundo real
                  que asegura que En Punto resuelva los problemas que realmente
                  importan a nuestros clientes y transportistas. Soy responsable
                  de nuestro proceso de verificación y la construcción de
                  nuestra comunidad.
                </p>

                <div className="flex items-center justify-center">
                  <Link
                    href="https://www.linkedin.com/in/rominaguzmanar/"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-blue-600 hover:text-blue-700 font-semibold p-2"
                  >
                    <Image
                      src="/InBug-Black.png"
                      width="24"
                      height="24"
                      alt="Linkedin logo in black"
                    />
                  </Link>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Values Section */}
        <section className="py-20 px-8 bg-gray-50">
          <div className="max-w-6xl mx-auto">
            <h2 className="text-4xl font-bold text-gray-800 mb-12 text-center">
              Nuestra Promesa: Confianza, Eficiencia y Transparencia
            </h2>

            <div className="grid md:grid-cols-3 gap-8">
              {/* Trust */}
              <div className="text-center">
                <div className="w-16 h-16 mx-auto mb-6 bg-blue-100 rounded-lg flex items-center justify-center">
                  <svg
                    className="w-8 h-8 text-blue-600"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"
                    />
                  </svg>
                </div>

                <h3 className="text-xl font-bold text-gray-800 mb-4">
                  Confianza
                </h3>

                <p className="text-gray-600">
                  Cada transportista y cliente en nuestra plataforma es revisado
                  manualmente por nuestro equipo. Estamos construyendo una
                  comunidad de profesionales en la que puedes confiar.
                </p>
              </div>

              {/* Efficiency */}
              <div className="text-center">
                <div className="w-16 h-16 mx-auto mb-6 bg-green-100 rounded-lg flex items-center justify-center">
                  <svg
                    className="w-8 h-8 text-green-600"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M13 10V3L4 14h7v7l9-11h-7z"
                    />
                  </svg>
                </div>

                <h3 className="text-xl font-bold text-gray-800 mb-4">
                  Eficiencia
                </h3>

                <p className="text-gray-600">
                  Nuestro motor de emparejamiento inteligente conecta clientes
                  con los transportistas correctos basándose en sus capacidades
                  reales, ahorrando tiempo a todos. Publica un servicio o
                  encuentra trabajo en minutos, no en horas.
                </p>
              </div>

              {/* Transparency */}
              <div className="text-center">
                <div className="w-16 h-16 mx-auto mb-6 bg-purple-100 rounded-lg flex items-center justify-center">
                  <svg
                    className="w-8 h-8 text-purple-600"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"
                    />
                  </svg>
                </div>

                <h3 className="text-xl font-bold text-gray-800 mb-4">
                  Transparencia
                </h3>

                <p className="text-gray-600">
                  Desde requisitos claros del trabajo hasta un sistema de
                  calificación justo y abierto, nuestro objetivo es eliminar la
                  incertidumbre. Sabrás qué esperar en cada paso del camino.
                </p>
              </div>
            </div>
          </div>
        </section>

        {/* Final CTA Section */}
        <section className="py-20 px-8 bg-gray-100">
          <div className="max-w-6xl mx-auto">
            <h2 className="text-4xl font-bold text-gray-900 mb-6 text-center">
              ¿Listo para transformar tu logística?
            </h2>

            <p className="text-xl text-gray-600 mb-8 text-center">
              Únete a la nueva generación de transporte en Argentina.
            </p>

            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <Link
                href="/signup"
                className="inline-block bg-blue-600 text-white px-8 py-4 rounded-lg text-lg font-semibold hover:bg-blue-600 transition-colors"
              >
                Empezar - Es Gratis
              </Link>

              <Link
                href="/contact"
                className="inline-block border-2 border-blue-600 text-blue-600 px-8 py-4 rounded-lg text-lg font-semibold hover:bg-blue-600 hover:text-white transition-colors"
              >
                Contactar
              </Link>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}
