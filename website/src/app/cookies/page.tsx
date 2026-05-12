import Link from 'next/link';

export default async function Cookies() {
  return (
    <div className="min-h-screen bg-gray-50">
      <main className="mx-auto max-w-content-prose px-4 py-12 sm:px-6 sm:py-16 lg:py-20">
        <article className="rounded-2xl bg-white px-5 py-8 shadow-sm sm:px-8 sm:py-10">
          <header className="border-b border-gray-100 pb-8">
            <h1 className="text-3xl font-bold tracking-tight text-gray-900 sm:text-4xl">
              Política de Cookies
            </h1>
            <p className="mt-3 text-sm text-gray-500">
              Última modificación: 12 de mayo de 2026
            </p>
          </header>

          <div className="pt-8 text-sm leading-relaxed text-gray-600 sm:text-base">
            <p className="mb-6">
              Este documento complementa los Términos y Condiciones de los
              servicios provistos por ROASAL S.A.S. a través de la plataforma
              &quot;En Punto&quot;. Se aplica al{' '}
              <strong className="font-medium text-gray-800">
                sitio web y panel web
              </strong>{' '}
              (navegador). La aplicación móvil Android tiene su propio paquete y
              configuración de permisos; acá describimos tecnologías del sitio.
            </p>

            <p className="mb-6">
              En cumplimiento de la Ley 25.326 de Protección de Datos Personales
              y normativa aplicable en Argentina, informamos qué tecnologías de
              almacenamiento usamos y con qué fines.
            </p>

            <h2 className="mt-10 text-xl font-semibold text-gray-900 first:mt-0">
              1. ¿Qué son las cookies y tecnologías similares?
            </h2>
            <p className="mb-4 mt-3">
              Las cookies son archivos pequeños que el sitio guarda en el
              navegador. El{' '}
              <strong className="font-medium text-gray-800">
                almacenamiento local
              </strong>{' '}
              (<code className="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-800">localStorage</code> y{' '}
              <code className="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-800">sessionStorage</code>) cumple funciones parecidas pero vive en el
              dispositivo sin enviarse automáticamente al servidor en cada
              pedido.
            </p>

            <h2 className="mt-10 text-xl font-semibold text-gray-900">
              2. Sesión en el panel web (cookies y modo alternativo)
            </h2>
            <p className="mb-4 mt-3">
              Para mantener iniciada la sesión después de que ingresás al panel,
              usamos una de estas configuraciones (según cómo esté desplegado el
              sitio):
            </p>
            <ul className="mb-6 list-disc space-y-2 pl-5">
              <li>
                <strong className="font-medium text-gray-800">
                  Modo recomendado (cookies HttpOnly)
                </strong>
                : el servidor establece cookies llamadas{' '}
                <code className="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-800">
                  enpunto_access
                </code>{' '}
                y{' '}
                <code className="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-800">
                  enpunto_refresh
                </code>{' '}
                (tokens de sesión; no son legibles por JavaScript de la página).
                Además puede establecerse la cookie{' '}
                <code className="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-800">
                  enpunto_web_hint
                </code>
                , un marcador no secreto para que la aplicación web detecte que
                hay sesión activa.
              </li>
              <li>
                <strong className="font-medium text-gray-800">
                  Modo alternativo
                </strong>{' '}
                (si la cookie-session está desactivada en la configuración del
                sitio): los mismos tokens se guardan en{' '}
                <code className="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-800">
                  sessionStorage
                </code>{' '}
                bajo las claves{' '}
                <code className="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-800">
                  enpunto_web_access_token
                </code>{' '}
                y{' '}
                <code className="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-800">
                  enpunto_web_refresh_token
                </code>
                . No son cookies HTTP, pero cumplen el mismo fin esencial de
                sesión.
              </li>
            </ul>
            <p className="mb-6">
              Sin una de estas opciones activa no podés usar las funciones del
              panel que requieren inicio de sesión.
            </p>

            <h2 className="mt-10 text-xl font-semibold text-gray-900">
              3. Almacenamiento local del panel (sin publicidad)
            </h2>
            <p className="mb-4 mt-3">
              Para funciones básicas de la experiencia (checklist de prueba,
              recordatorios de interfaz y eventos internos de uso), el panel
              puede guardar datos en{' '}
              <code className="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-800">
                localStorage
              </code>{' '}
              bajo claves como:
            </p>
            <ul className="mb-6 list-disc space-y-2 pl-5">
              <li>
                <code className="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-800">
                  remitos_checklist_download_page_visited
                </code>{' '}
                — recordar que visitaste la página de descarga de la app.
              </li>
              <li>
                <code className="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-800">
                  remitos_first_scan_completed_analytics_sent
                </code>{' '}
                — evitar duplicar un evento interno asociado al primer uso
                exitoso del escaneo.
              </li>
              <li>
                <code className="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-800">
                  remitos_trial_success_screen_viewed_at
                </code>{' '}
                — marcar que viste la pantalla de bienvenida tras el registro.
              </li>
              <li>
                <code className="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-800">
                  enpunto_panel_mobile_desktop_hint_dismissed
                </code>{' '}
                — recordar que cerraste un aviso de uso entre móvil y escritorio.
              </li>
            </ul>
            <p className="mb-6">
              No usamos esas claves para publicidad personalizada ni para
              perfilar comportamiento en sitios de terceros.
            </p>

            <h2 className="mt-10 text-xl font-semibold text-gray-900">
              4. Pagos con tarjeta (Mercado Pago)
            </h2>
            <p className="mb-6 mt-3">
              Cuando cargás o actualizás un medio de pago con tarjeta en el
              panel, podés interactuar con componentes de{' '}
              <strong className="font-medium text-gray-800">Mercado Pago</strong>
              . Esos componentes pueden usar cookies o almacenamiento propios
              según la política de Mercado Pago; ROASAL S.A.S. no controla esas
              tecnologías. Te recomendamos revisar la{' '}
              <a
                href="https://www.mercadopago.com.ar/privacidad"
                className="font-medium text-blue-600 hover:text-blue-700 hover:underline"
                target="_blank"
                rel="noopener noreferrer"
              >
                privacidad de Mercado Pago
              </a>
              .
            </p>

            <h2 className="mt-10 text-xl font-semibold text-gray-900">
              5. Publicidad y seguimiento de terceros
            </h2>
            <p className="mb-4 mt-3">
              En Punto no utiliza, por cuenta propia en el sitio web descrito
              acá:
            </p>
            <ul className="mb-6 list-disc space-y-2 pl-5">
              <li>cookies de publicidad personalizada;</li>
              <li>cookies de medición de audiencia de terceros;</li>
              <li>píxeles o beacons de remarketing;</li>
              <li>SDKs de terceros instalados en este front para recolección de
                datos de navegación.</li>
            </ul>
            <p className="mb-6">
              Si en el futuro incorporáramos tecnologías distintas, actualizaremos
              esta política con anticipación razonable y, cuando la ley lo exija,
              pediremos consentimiento u ofreceremos opciones adicionales.
            </p>

            <h2 className="mt-10 text-xl font-semibold text-gray-900">
              6. Tus opciones
            </h2>
            <p className="mb-4 mt-3">
              Podés configurar el navegador para borrar cookies o datos del
              sitio. Si eliminás las cookies o el almacenamiento de sesión
              descritos arriba, es probable que tengas que iniciar sesión de
              nuevo o que se pierdan preferencias locales del panel (por ejemplo
              avisos ya cerrados).
            </p>
            <p className="mb-6">
              Para más información sobre datos personales y derechos (acceso,
              rectificación, etc.), podés consultar nuestra{' '}
              <Link
                href="/privacidad"
                className="font-medium text-blue-600 hover:text-blue-700 hover:underline"
              >
                política de privacidad
              </Link>{' '}
              y el sitio de la{' '}
              <a
                href="https://www.argentina.gob.ar/aaip"
                className="font-medium text-blue-600 hover:text-blue-700 hover:underline"
                target="_blank"
                rel="noopener noreferrer"
              >
                Agencia de Acceso a la Información Pública (AAIP)
              </a>
              .
            </p>

            <div className="mt-10 border-t border-gray-100 pt-8 text-center">
              <p className="mb-4 text-gray-600">
                ¿Dudas sobre esta política? Escribinos.
              </p>
              <Link
                href="/contacto"
                className="inline-flex items-center justify-center rounded-lg bg-blue-600 px-6 py-3 text-sm font-semibold text-white transition-colors hover:bg-blue-700"
              >
                Contacto
              </Link>
            </div>
          </div>
        </article>
      </main>
    </div>
  );
}
