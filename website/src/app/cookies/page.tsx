import Link from 'next/link';

export default async function Cookies() {
  return (
    <div className="flex flex-col min-h-screen">
      <main className="grow">
        {/* Hero Section */}
        <section className="py-20 px-8 bg-white">
          <div className="max-w-6xl mx-auto">
            <h1 className="text-4xl font-bold text-blue-600 mb-8 text-left">
              Política de Cookies
            </h1>

            <p className="text-l text-gray-600 mb-8 mx-auto text-left">
              Última modificación: 18 de septiembre de 2025
            </p>
          </div>
        </section>

        {/* Introduction Section */}
        <section className="py-20 px-8 bg-gray-50">
          <div className="max-w-6xl mx-auto">
            <p className="text-gray-600 mb-6 leading-relaxed">
              El presente documento es un anexo de los Términos y condiciones de
              los servicios provistos por ROASAL S.A.S. o las aplicaciones
              registradas como &quot;En Punto&quot;, marca registrada propiedad
              de la primera, de ahora en más denominada &quot;La compañía&quot;.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              ROASAL S.A.S. utiliza cookies en sus sitios web y aplicaciones
              móviles (colectivamente, los &quot;Servicios en Línea&quot;) para
              fines esenciales, en cumplimiento con la Ley 25.326 de Protección
              de Datos Personales y regulaciones aplicables en Argentina.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Estas cookies son requeridas para fines esenciales como la gestión
              de sesiones de autenticación. Específicamente, se utiliza una
              cookie llamada &apos;auth-token&apos; para mantener la sesión del
              usuario segura y permitir el acceso a funciones personalizadas
              después de iniciar sesión. ROASAL S.A.S. no utiliza cookies de
              seguimiento, publicitarias de terceros, píxeles de rastreo, ni
              otras tecnologías de seguimiento en este momento. Esta política se
              actualizará si se introducen cambios, asegurando el consentimiento
              donde sea requerido acorde las regulaciones de la República
              Argentina o el país de prestación del servicio.
            </p>
          </div>
        </section>

        {/* Section 1: Cookies Overview */}
        <section className="py-20 px-8 bg-white">
          <div className="max-w-6xl mx-auto">
            <h2 className="text-3xl font-bold text-gray-800 mb-8">
              1. ¿Qué son las Cookies?
            </h2>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Las cookies son pequeños archivos de texto que se almacenan en el
              navegador del usuario o dispositivo por sitios web y aplicaciones,
              que se utilizan para recordar al navegador o dispositivo durante y
              a través de visitas a sitios web.
            </p>

            <h3 className="text-2xl font-semibold text-gray-800 mb-4">
              Cookies que utilizamos
            </h3>

            <p className="text-gray-600 mb-6 leading-relaxed">
              En Punto utiliza únicamente cookies esenciales necesarias para el
              funcionamiento del servicio:
            </p>

            <ul className="list-disc list-inside text-gray-600 mb-6 leading-relaxed space-y-2">
              <li>
                <strong>auth-token:</strong> Cookie de autenticación que
                mantiene la sesión del usuario segura y permite el acceso a
                funciones personalizadas después de iniciar sesión.
              </li>
            </ul>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Estas cookies son estrictamente necesarias para proporcionar los
              servicios que el usuario ha solicitado y no pueden ser
              desactivadas sin afectar la funcionalidad del servicio.
            </p>
          </div>
        </section>

        {/* Section 2: No Tracking or Advertising */}
        <section className="py-20 px-8 bg-gray-50">
          <div className="max-w-6xl mx-auto">
            <h2 className="text-3xl font-bold text-gray-800 mb-8">
              2. Publicidad y Seguimiento
            </h2>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Actualmente, En Punto no utiliza:
            </p>

            <ul className="list-disc list-inside text-gray-600 mb-6 leading-relaxed space-y-2">
              <li>Cookies de publicidad personalizada</li>
              <li>Cookies de seguimiento de terceros</li>
              <li>Etiquetas de píxel o beacons</li>
              <li>SDKs de terceros para recopilación de datos</li>
              <li>Herramientas de análisis de terceros</li>
            </ul>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Si en el futuro se implementan tales funciones, esta política será
              actualizada antes de su implementación y se proporcionarán
              opciones para optar por no participar donde sea requerido por la
              ley.
            </p>
          </div>
        </section>

        {/* Section 3: User Choices */}
        <section className="py-20 px-8 bg-white">
          <div className="max-w-6xl mx-auto">
            <h2 className="text-3xl font-bold text-gray-800 mb-8">
              3. Opciones del usuario
            </h2>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Dado que las cookies que utilizamos son estrictamente necesarias
              para el funcionamiento de los Servicios en Línea, si el usuario
              elige rechazar o eliminar estas cookies, no podrá acceder a las
              funciones autenticadas del servicio.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              La mayoría de los navegadores web están configurados para aceptar
              cookies por defecto. Si el usuario lo prefiere, puede configurar
              su navegador para eliminar o rechazar cookies. Para hacerlo, el
              usuario debe seguir las instrucciones proporcionadas por su
              navegador, que generalmente se encuentran dentro del menú
              &apos;Ayuda&apos; o &apos;Preferencias&apos;.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Para obtener más información sobre cookies y sus derechos bajo la
              Ley 25.326, incluyendo cómo ejercer derechos de acceso y
              oposición, contáctese con ROASAL S.A.S. o visite el sitio de la
              Agencia de Acceso a la Información Pública (AAIP).
            </p>
          </div>
        </section>

        {/* Final CTA Section */}
        <section className="py-20 px-8 bg-gray-100">
          <div className="max-w-6xl mx-auto">
            <h2 className="text-4xl font-bold text-gray-900 mb-6 text-center">
              ¿Preguntas?
            </h2>

            <p className="text-xl text-gray-600 mb-8 text-center">
              Si tiene alguna pregunta sobre esta Política de Cookies,
              contáctenos.
            </p>

            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <Link
                href="/contact"
                className="inline-block bg-blue-600 text-white px-8 py-4 rounded-lg text-lg font-semibold hover:bg-blue-700 transition-colors"
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
