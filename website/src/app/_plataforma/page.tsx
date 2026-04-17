import {
  Truck,
  Package,
  FilePlus,
  Users,
  CheckCircle,
  Shield,
  Target,
  Award,
  ArrowRight,
} from 'lucide-react';
import Image from 'next/image';
import Link from 'next/link';

export default async function PlataformaPage() {
  return (
    <div className="flex flex-col min-h-screen">
      <main className="grow">
        {/* Coming Soon Banner */}
        <div className="bg-amber-500 text-white py-3 px-4 text-center font-semibold">
          Próximamente - Estamos trabajando en algo increíble
        </div>

        {/* Hero Section */}
        <section
          id="home"
          className="flex flex-col md:flex-row items-center justify-center min-h-[70vh] bg-white p-8 md:p-12 lg:p-16 border-b border-gray-200"
        >
          {/* Content */}
          <div className="relative z-10 w-full max-w-content mx-auto flex flex-col md:flex-row items-center">
            {/* Left Column: Text Content */}
            <div className="w-full md:w-1/2 text-center md:text-left mb-10 md:mb-0 md:pr-10 lg:pr-16">
              <h1 className="text-4xl sm:text-5xl lg:text-6xl font-bold text-gray-800 mb-6 leading-tight">
                La Forma Inteligente de Mover Carga en Argentina
              </h1>
              <p className="text-lg sm:text-xl text-gray-600 mb-8 leading-relaxed">
                Conectamos empresas con transportistas profesionales y
                verificados. Publica un servicio en minutos, recibe ofertas
                competitivas y gestiona tu logística con total confianza.
              </p>

              {/* Primary CTA */}
              <div className="flex flex-col sm:flex-row gap-4 mb-6">
                <Link
                  href="/contact?subject=plataforma"
                  className="inline-flex items-center justify-center px-8 py-4 bg-blue-600 text-white font-semibold rounded-lg hover:bg-blue-600 transition-colors duration-200 text-lg shadow-lg hover:shadow-xl"
                >
                  Contactanos para Recibir Actualizaciones
                  <ArrowRight className="ml-2 h-5 w-5" />
                </Link>
              </div>
            </div>

            {/* Right Column: Visual */}
            <div className="w-full md:w-1/2 flex justify-center md:justify-end">
              <div className="relative">
                <Image
                  src="/argentina.svg"
                  alt="Mapa de Argentina"
                  width={500}
                  height={820}
                  className="max-w-full h-auto"
                  priority
                />
              </div>
            </div>
          </div>
        </section>

        {/* Who is this for? Section */}
        <section className="py-20 px-8 bg-white">
          <div className="max-w-content mx-auto">
            <h2 className="text-4xl font-bold text-gray-800 mb-12 text-center">
              Una Plataforma, Dos Soluciones
            </h2>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-12">
              {/* Left Card - For Customers */}
              <div className="bg-white border border-gray-200 p-8 rounded-lg shadow-sm hover:shadow-md transition-shadow duration-300">
                <div className="text-blue-600 mb-6 flex justify-center">
                  <Package size={64} />
                </div>
                <h3 className="text-2xl font-bold text-gray-900 mb-6 text-center">
                  Para Empresas que Envían
                </h3>
                <ul className="space-y-4 mb-8">
                  <li className="flex items-start">
                    <CheckCircle
                      className="text-blue-600 mt-1 mr-3 flex-shrink-0"
                      size={20}
                    />
                    <span className="text-gray-700">
                      Acceso a una red de transportistas verificados
                    </span>
                  </li>
                  <li className="flex items-start">
                    <CheckCircle
                      className="text-blue-600 mt-1 mr-3 flex-shrink-0"
                      size={20}
                    />
                    <span className="text-gray-700">
                      Recibe múltiples ofertas y elige la mejor
                    </span>
                  </li>
                  <li className="flex items-start">
                    <CheckCircle
                      className="text-blue-600 mt-1 mr-3 flex-shrink-0"
                      size={20}
                    />
                    <span className="text-gray-700">
                      Centraliza toda tu logística en un solo lugar
                    </span>
                  </li>
                </ul>
              </div>

              {/* Right Card - For Vendors */}
              <div className="bg-white border border-gray-200 p-8 rounded-lg shadow-sm hover:shadow-md transition-shadow duration-300">
                <div className="text-emerald-700 mb-6 flex justify-center">
                  <Truck size={64} />
                </div>
                <h3 className="text-2xl font-bold text-gray-900 mb-6 text-center">
                  Para Transportistas Profesionales
                </h3>
                <ul className="space-y-4 mb-8">
                  <li className="flex items-start">
                    <CheckCircle
                      className="text-emerald-700 mt-1 mr-3 flex-shrink-0"
                      size={20}
                    />
                    <span className="text-gray-700">
                      Encuentra nuevos trabajos compatibles con tu flota
                    </span>
                  </li>
                  <li className="flex items-start">
                    <CheckCircle
                      className="text-emerald-700 mt-1 mr-3 flex-shrink-0"
                      size={20}
                    />
                    <span className="text-gray-700">
                      Construye tu reputación y obtén más clientes
                    </span>
                  </li>
                  <li className="flex items-start">
                    <CheckCircle
                      className="text-emerald-700 mt-1 mr-3 flex-shrink-0"
                      size={20}
                    />
                    <span className="text-gray-700">
                      Sin intermediarios. Negocia y coordina directamente
                    </span>
                  </li>
                </ul>
              </div>
            </div>

            {/* Single CTA Button */}
            <div className="text-center mt-10">
              <Link
                href="/contact?subject=plataforma"
                className="inline-flex items-center px-8 py-4 bg-blue-600 text-white font-semibold rounded-lg hover:bg-blue-700 transition-colors duration-200 text-lg"
              >
                Contactanos
                <ArrowRight className="ml-2 h-5 w-5" />
              </Link>
            </div>
          </div>
        </section>

        {/* How it Works Section */}
        <section className="py-20 px-8 bg-gray-50">
          <div className="max-w-content mx-auto">
            <h2 className="text-4xl font-bold text-gray-800 mb-12 text-center">
              Logística Simplificada en Tres Pasos
            </h2>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
              {/* Step 1 */}
              <div className="text-center">
                <div className="bg-blue-600 text-white w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-6 text-2xl font-bold">
                  1
                </div>
                <div className="text-blue-600 mb-4 flex justify-center">
                  <FilePlus size={48} />
                </div>
                <h3 className="text-xl font-semibold text-gray-800 mb-4">
                  Publica
                </h3>
                <p className="text-gray-600">
                  El cliente publica los detalles de su servicio, desde los
                  requisitos de carga hasta el destino. Nuestra plataforma lo
                  hace simple y claro.
                </p>
              </div>

              {/* Step 2 */}
              <div className="text-center">
                <div className="bg-blue-600 text-white w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-6 text-2xl font-bold">
                  2
                </div>
                <div className="text-blue-600 mb-4 flex justify-center">
                  <Users size={48} />
                </div>
                <h3 className="text-xl font-semibold text-gray-800 mb-4">
                  Elige
                </h3>
                <p className="text-gray-600">
                  Los vendedores verificados hacen ofertas. El cliente revisa
                  sus perfiles, calificaciones y precios, y elige al mejor
                  profesional para el trabajo.
                </p>
              </div>

              {/* Step 3 */}
              <div className="text-center">
                <div className="bg-blue-600 text-white w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-6 text-2xl font-bold">
                  3
                </div>
                <div className="text-blue-600 mb-4 flex justify-center">
                  <CheckCircle size={48} />
                </div>
                <h3 className="text-xl font-semibold text-gray-800 mb-4">
                  Transporta
                </h3>
                <p className="text-gray-600">
                  El vendedor elegido completa el servicio. Ambas partes
                  coordinan y confirman la finalización a través de la
                  plataforma, construyendo un historial verificable de
                  confianza.
                </p>
              </div>
            </div>
          </div>
        </section>

        {/* Professionals Section */}
        <section className="py-20 px-8 bg-gray-50">
          <div className="max-w-content mx-auto">
            <h2 className="text-4xl font-bold text-gray-800 mb-6 text-center">
              Para los Profesionales que Mueven Argentina
            </h2>
            <p className="text-xl text-gray-600 mb-12 text-center max-w-content-prose mx-auto">
              En Punto está diseñado para empresas y transportistas que valoran
              la confianza, la eficiencia y el trabajo bien hecho.
            </p>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-12">
              {/* Left Card - For Businesses */}
              <div className="bg-white rounded-2xl shadow-lg overflow-hidden hover:shadow-xl transition-shadow duration-300">
                <div className="relative h-64">
                  <Image
                    src="/inventory-management.jpg"
                    alt="Almacén moderno con vista aérea"
                    fill
                    className="object-cover"
                  />
                </div>
                <div className="p-8">
                  <h3 className="text-2xl font-bold text-gray-800 mb-6 text-center">
                    Para Empresas y Emprendedores
                  </h3>
                  <h4 className="text-xl font-semibold text-gray-800 mb-4 text-center">
                    Encuentra el Transporte Adecuado, Siempre
                  </h4>
                  <p className="text-gray-700 leading-relaxed">
                    Ya sea que envíes mercancías a un cliente local o a través
                    del país, tu logística debe ser confiable. En Punto te
                    conecta con una red curada de transportistas verificados
                    cuyas capacidades coinciden exactamente con tus necesidades.
                    Deja de lado las interminables llamadas telefónicas y
                    gestiona tus envíos con confianza.
                  </p>
                </div>
              </div>

              {/* Right Card - For Vendors */}
              <div className="bg-white rounded-2xl shadow-lg overflow-hidden hover:shadow-xl transition-shadow duration-300">
                <div className="relative h-64">
                  <Image
                    src="/man-leaning-window-full.jpg"
                    alt="Camión Scania en carretera abierta"
                    fill
                    className="object-cover"
                  />
                </div>
                <div className="p-8">
                  <h3 className="text-2xl font-bold text-gray-800 mb-6 text-center">
                    Para Transportistas y Flotas
                  </h3>
                  <h4 className="text-xl font-semibold text-gray-800 mb-4 text-center">
                    Convierte tu Flota en una Oportunidad
                  </h4>
                  <p className="text-gray-700 leading-relaxed">
                    Tus vehículos son tu negocio. En Punto te brinda un flujo
                    constante de solicitudes de servicio legítimas y bien
                    definidas que coinciden con las capacidades de tu flota.
                    Construye tu reputación profesional con cada trabajo
                    completado, comunícate directamente con los clientes y haz
                    crecer tu negocio en una plataforma diseñada para
                    profesionales.
                  </p>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Features Section */}
        <section className="py-20 px-8 bg-gray-50">
          <div className="max-w-content mx-auto">
            <h2 className="text-4xl font-bold text-gray-800 mb-12 text-center">
              Más que un simple listado de trabajos
            </h2>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
              {/* Feature 1 */}
              <div className="bg-white p-8 rounded-xl shadow-md hover:shadow-lg transition-shadow duration-300">
                <div className="text-blue-600 mb-6 flex justify-center">
                  <Shield size={48} />
                </div>
                <h3 className="text-xl font-semibold text-gray-800 mb-4 text-center">
                  Verificación y Confianza
                </h3>
                <p className="text-gray-600 text-center">
                  Nuestro proceso de aprobación manual asegura que solo trabajes
                  con profesionales serios.
                </p>
              </div>

              {/* Feature 2 */}
              <div className="bg-white p-8 rounded-xl shadow-md hover:shadow-lg transition-shadow duration-300">
                <div className="text-blue-600 mb-6 flex justify-center">
                  <Target size={48} />
                </div>
                <h3 className="text-xl font-semibold text-gray-800 mb-4 text-center">
                  Matching Inteligente
                </h3>
                <p className="text-gray-600 text-center">
                  Nuestra tecnología te muestra solo las oportunidades y los
                  transportistas que son compatibles, ahorrándote tiempo.
                </p>
              </div>

              {/* Feature 3 */}
              <div className="bg-white p-8 rounded-xl shadow-md hover:shadow-lg transition-shadow duration-300">
                <div className="text-blue-600 mb-6 flex justify-center">
                  <Award size={48} />
                </div>
                <h3 className="text-xl font-semibold text-gray-800 mb-4 text-center">
                  Reputación Digital
                </h3>
                <p className="text-gray-600 text-center">
                  Cada servicio completado construye tu historial. Una buena
                  reputación en En Punto es tu mejor carta de presentación.
                </p>
              </div>
            </div>
          </div>
        </section>

        {/* Final CTA Section */}
        <section className="py-20 px-8 bg-blue-600">
          <div className="max-w-content-narrow mx-auto text-center">
            <h2 className="text-4xl font-bold text-white mb-6">
              ¿Listo para empezar?
            </h2>
            <p className="text-xl text-blue-100 mb-8">
              Regístrate para recibir actualizaciones cuando lancemos
            </p>
            <Link
              href="/contact?subject=plataforma"
              className="inline-flex items-center px-8 py-4 bg-white text-blue-600 font-semibold rounded-lg hover:bg-gray-100 transition-colors duration-200 text-lg shadow-lg hover:shadow-xl"
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
