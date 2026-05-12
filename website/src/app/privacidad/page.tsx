import Link from 'next/link';

export default async function Privacy() {
  return (
    <div className="min-h-screen bg-gray-50">
      <main className="mx-auto max-w-content-prose px-4 py-12 sm:px-6 sm:py-16 lg:py-20">
        <article className="rounded-2xl bg-white px-5 py-8 shadow-sm sm:px-8 sm:py-10">
          <header className="border-b border-gray-100 pb-8">
            <h1 className="text-3xl font-bold tracking-tight text-gray-900 sm:text-4xl">
              Acuerdo de Privacidad y Confidencialidad
            </h1>
            <p className="mt-3 text-sm text-gray-500">
              Última modificación: 12 de mayo de 2026
            </p>
          </header>

          <div className="pt-8 text-sm leading-relaxed text-gray-600 sm:text-base">
            <h2 className="text-xl font-semibold text-gray-900">
              1. Introducción
            </h2>
            <p className="mb-4 mt-3">
              &quot;En Punto&quot; es una marca logística registrada de la firma
              ROASAL S.A.S., CUIT 30-71793629-5, con domicilio legal en Pinar
              del Río 3631, Córdoba, Argentina, de ahora en más denominada como
              &quot;la compañía&quot;.
            </p>

            <p className="mb-4">
              Este acuerdo describe cómo ROASAL S.A.S. recopila, utiliza y
              protege la información personal cuando el Usuario emplea los
              servicios de su propiedad, incluida la plataforma En Punto. Los
              servicios digitales incluyen el{' '}
              <strong className="font-medium text-gray-800">
                sitio web público
              </strong>
              , el{' '}
              <strong className="font-medium text-gray-800">panel web</strong>{' '}
              (navegador, con inicio de sesión) y la{' '}
              <strong className="font-medium text-gray-800">
                aplicación móvil Android
              </strong>{' '}
              publicada por la compañía. Las tecnologías de almacenamiento en el
              navegador (cookies,{' '}
              <code className="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-800">
                localStorage
              </code>
              ,{' '}
              <code className="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-800">
                sessionStorage
              </code>
              ) del sitio y del panel se detallan en la{' '}
              <Link
                href="/cookies"
                className="font-medium text-blue-600 hover:text-blue-700 hover:underline"
              >
                Política de Cookies
              </Link>
              . Las reglas generales de uso del servicio figuran en los{' '}
              <Link
                href="/terminos"
                className="font-medium text-blue-600 hover:text-blue-700 hover:underline"
              >
                Términos y Condiciones
              </Link>
              .
            </p>

            <p className="mb-4">
              El Usuario puede utilizar el software o la plataforma conocidos
              como En Punto, así como interactuar con los canales oficiales de
              la compañía para acceder a la bolsa de trabajo, aplicar o publicar
              servicios logísticos, contactar clientes, actualizar su información
              de perfil, entre otros.
            </p>

            <p className="mb-6">
              Al suministrar su aceptación a este aviso, la compañía entiende
              que el Usuario acepta sin reservas lo aquí establecido. La
              información suministrada será usada para fines contractuales y
              operativos, para prestar servicios logísticos y para el proceso de
              mejora de los servicios disponibles y el desarrollo de nuevos
              productos, conforme a la Ley N.º 25.326 de Protección de Datos
              Personales y normativa complementaria.
            </p>

            <h2 className="mt-10 text-xl font-semibold text-gray-900">
              2. Consentimiento informado
            </h2>
            <p className="mb-4 mt-3">
              Al hacer clic en el botón &quot;Acepto&quot; o acción equivalente,
              el Usuario manifiesta de manera libre, expresa e informada su
              consentimiento respecto del tratamiento de sus datos personales
              conforme a los términos establecidos en este acuerdo.
            </p>

            <p className="mb-4">
              Dicho consentimiento comprende la recopilación, almacenamiento,
              uso, procesamiento, cesión, transferencia internacional cuando
              corresponda, conservación y, en su caso, supresión de datos, con
              las finalidades previstas en la presente política.
            </p>

            <p className="mb-6">
              Asimismo, el Usuario declara haber leído y comprendido integralmente
              este documento, y acepta sin reservas las condiciones aquí
              estipuladas en su propio nombre o en representación de la persona
              jurídica a la que pertenezca, asumiendo plena responsabilidad por
              el uso de la plataforma.
            </p>

            <h2 className="mt-10 text-xl font-semibold text-gray-900">
              3. Definición de Usuario
            </h2>
            <p className="mb-4 mt-3">
              Este aviso aplica a quienes usan las aplicaciones, sitios web u
              otros servicios de ROASAL S.A.S. Se considera Usuario a toda
              persona física, jurídica o entidad que interactúa con esos
              servicios, lo que incluye correo electrónico, mensajes, sitio web,
              software, aplicaciones móviles y otros canales habilitados.
            </p>

            <p className="mb-4">
              El usuario puede obtener acceso limitado a distintas interfaces e
              información de &quot;En Punto&quot; según su rol en los circuitos
              logísticos. La empresa reconoce, entre otros, los siguientes tipos
              de usuarios:
            </p>

            <ul className="mb-6 list-disc space-y-2 pl-5">
              <li>
                <strong className="font-medium text-gray-800">
                  Transportistas:
                </strong>{' '}
                individuos o entidades que ofrecen vehículos motorizados para
                transportar bienes.
              </li>
              <li>
                <strong className="font-medium text-gray-800">
                  Conductores de transportistas:
                </strong>{' '}
                individuos que operan vehículos motorizados para transportar
                bienes para o en nombre de transportistas.
              </li>
              <li>
                <strong className="font-medium text-gray-800">Clientes:</strong>{' '}
                usuario identificado como remitente, consignatario o consignador,
                que posee los bienes que se transportan o requiere servicios de
                transporte de mercadería terrestre.
              </li>
              <li>
                <strong className="font-medium text-gray-800">
                  Despachadores:
                </strong>{' '}
                entidades logísticas que coordinan tráfico, asignando vehículos y
                conductores de transportistas para transportar bienes.
              </li>
            </ul>

            <p className="mb-6">
              El uso de la aplicación, cualquiera sea el rol asignado, no implica
              relación laboral ni de dependencia con la compañía. La actividad
              comercial del Usuario queda sujeta a las leyes aplicables de la
              República Argentina. Al aceptar los términos y condiciones, el
              usuario confirma que es total e independientemente responsable en
              materia jurídica e impositiva de sus actividades, sin que ello
              genere obligación adicional para la compañía frente a terceros por
              dichos actos.
            </p>

            <h2 className="mt-10 text-xl font-semibold text-gray-900">
              4. Definición de datos
            </h2>
            <p className="mb-4 mt-3">
              La información recopilada, almacenada y utilizada por la empresa
              responde principalmente a dos tipos de naturaleza:
            </p>

            <h3 className="mt-8 text-lg font-semibold text-gray-900">
              4.1. Datos personales
            </h3>
            <p className="mb-4 mt-3">
              Son información que puede utilizarse para identificar al Usuario,
              de forma directa o indirecta. Incluye, entre otros, nombre y
              apellido, correo electrónico, teléfono, credenciales de acceso,
              datos tributarios y de facturación, datos de pago gestionados por
              proveedores (por ejemplo Mercado Pago), documentación de
              habilitación y del vehículo, y datos técnicos de uso (logs,
              dirección IP, tipo de navegador o dispositivo, marcas de tiempo).
              Las cookies y el almacenamiento local del navegador se describen
              en la{' '}
              <Link
                href="/cookies"
                className="font-medium text-blue-600 hover:text-blue-700 hover:underline"
              >
                Política de Cookies
              </Link>
              .
            </p>

            <p className="mb-4">
              Los datos personales serán proporcionados por el Usuario en la
              medida en que resulte necesario para el servicio. Salvo que se
              indique lo contrario, los datos marcados como obligatorios en los
              formularios o flujos de alta son necesarios para que la compañía
              pueda prestar el servicio; la negativa a proporcionarlos puede
              impedir o limitar la prestación.
            </p>

            <p className="mb-6">
              Cuando el software indique que ciertos datos no son obligatorios,
              el Usuario podrá omitirlos sin que ello afecte la disponibilidad
              del servicio en los términos informados en cada pantalla.
            </p>

            <h3 className="mt-8 text-lg font-semibold text-gray-900">
              4.2. Datos sensibles
            </h3>
            <p className="mb-6 mt-3">
              Solo se recopilan datos calificados como sensibles por la ley
              cuando resultan estrictamente necesarios para finalidades
              específicas y legales (por ejemplo identificación con imagen o
              firma cuando el flujo de verificación lo exija), aplicando las
              garantías previstas por la Ley 25.326.
            </p>

            <h3 className="mt-8 text-lg font-semibold text-gray-900">
              4.3. Categorías de información
            </h3>
            <p className="mb-4 mt-3">
              A modo orientativo, ROASAL S.A.S. puede tratar la siguiente
              información según el rol y los productos utilizados:
            </p>

            <div className="mb-6 overflow-x-auto rounded-lg border border-gray-200">
              <table className="min-w-full border-collapse text-left text-sm">
                <thead>
                  <tr className="border-b border-gray-200 bg-gray-50">
                    <th className="p-3 font-semibold text-gray-900 sm:p-4">
                      Categoría de datos
                    </th>
                    <th className="p-3 font-semibold text-gray-900 sm:p-4">
                      Tipos de datos
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200">
                  <tr>
                    <td className="p-3 align-top sm:p-4">
                      <strong className="text-gray-800">
                        a. Información de cuenta.
                      </strong>{' '}
                      Datos al crear, actualizar o iniciar sesión en En Punto.
                    </td>
                    <td className="p-3 sm:p-4">
                      Domicilio; datos de pago y bancarios; correo electrónico;
                      nombre y apellido; usuario y contraseña; teléfono;
                      preferencias; documentos de identificación; licencia de
                      conducir; fecha de nacimiento; firma; datos del vehículo,
                      seguro, dominio y documentación de habilitaciones e
                      inspecciones cuando corresponda al servicio.
                    </td>
                  </tr>
                  <tr>
                    <td className="p-3 align-top sm:p-4">
                      <strong className="text-gray-800">
                        b. Información sensible de tratamiento.
                      </strong>{' '}
                      Solo cuando sea necesario y con respaldo legal.
                    </td>
                    <td className="p-3 sm:p-4">
                      Imágenes de identificación; información bancaria y
                      fiscal; datos biométricos si el medio de validación lo
                      incorpora.
                    </td>
                  </tr>
                  <tr>
                    <td className="p-3 align-top sm:p-4">
                      <strong className="text-gray-800">
                        c. Datos demográficos o de perfil.
                      </strong>{' '}
                      Encuestas u origen legítimo de terceros cuando aplique.
                    </td>
                    <td className="p-3 sm:p-4">
                      Datos inferidos o declarados de perfil comercial o
                      certificaciones relevantes para el transporte.
                    </td>
                  </tr>
                  <tr>
                    <td className="p-3 align-top sm:p-4">
                      <strong className="text-gray-800">
                        d. Metadatos y registro técnico.
                      </strong>
                    </td>
                    <td className="p-3 sm:p-4">
                      Registros de uso de la plataforma, tiempos de sesión,
                      direcciones IP, identificadores de dispositivo o
                      navegador, logs del sistema y métricas operativas. En el
                      panel web, ciertos hitos de producto pueden registrarse
                      también mediante eventos enviados al{' '}
                      <code className="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-800">
                        dataLayer
                      </code>{' '}
                      del navegador cuando exista una capa de analítica
                      configurada (por ejemplo Google Tag Manager); si no hay
                      scripts de terceros cargados, esos eventos no salen del
                      entorno del navegador salvo lo que la aplicación envíe a
                      los servidores de ROASAL S.A.S. conforme a estos fines. En
                      el sitio y panel no utilizamos, por cuenta propia, cookies
                      de publicidad comportamental descritas en la Política de
                      Cookies.
                    </td>
                  </tr>
                  <tr>
                    <td className="p-3 align-top sm:p-4">
                      <strong className="text-gray-800">e. Datos de uso.</strong>{' '}
                      Interacciones con los Servicios.
                    </td>
                    <td className="p-3 sm:p-4">
                      Mensajes o registros de atención; calificaciones;
                      fotografías o archivos cargados para operación o soporte;
                      registros de servicios y acuerdos gestionados en la
                      plataforma.
                    </td>
                  </tr>
                  <tr>
                    <td className="p-3 align-top sm:p-4">
                      <strong className="text-gray-800">
                        f. Contenido y consultas.
                      </strong>{' '}
                      Cuando el usuario contacta o carga material.
                    </td>
                    <td className="p-3 sm:p-4">
                      Datos del formulario de contacto; contenidos enviados
                      para soporte o confirmación de entregas; calificaciones y
                      comentarios.
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <p className="mb-4">
              La información se recopila de la interacción del usuario con En
              Punto y los canales oficiales de la compañía. El tratamiento se
              realiza en cumplimiento de la Ley 25.326 de Protección de Datos
              Personales y su reglamentación.
            </p>

            <p className="mb-6">
              El Usuario asume la responsabilidad respecto de los datos personales
              de terceros que obtenga, publique o comparta a través del sitio o
              la aplicación.
            </p>

            <h2 className="mt-10 text-xl font-semibold text-gray-900">
              5. Métodos de tratamiento
            </h2>
            <p className="mb-4 mt-3">
              ROASAL S.A.S. adopta medidas de seguridad razonables para impedir
              el acceso no autorizado, la alteración o la destrucción indebida
              de los datos, con independencia del lugar de almacenamiento o del
              encargado que intervenga en el procesamiento. El almacenamiento
              puede realizarse en infraestructura propia o de terceros
              (servicios cloud) que ofrezcan niveles adecuados de seguridad y
              disponibilidad.
            </p>

            <p className="mb-4">
              El tratamiento se realiza mediante sistemas informáticos y
              procedimientos alineados con las finalidades informadas. La
              compañía comunicará cambios relevantes por los medios habilitados
              (por ejemplo correo electrónico, notificaciones en la app o
              avisos en el panel), según corresponda.
            </p>

            <p className="mb-4">
              Entre las medidas pueden incluirse, según el caso:
            </p>

            <ul className="mb-6 list-disc space-y-2 pl-5">
              <li>
                Protección de datos en tránsito (por ejemplo cifrado) y en
                reposo, cuando la arquitectura lo permita.
              </li>
              <li>
                Capacitación interna en privacidad y seguridad de la información.
              </li>
              <li>
                Políticas y procedimientos para limitar el acceso y el uso de
                los datos al personal o proveedores que lo requieran.
              </li>
              <li>
                Gestión de solicitudes de autoridades conforme a la ley:
                salvo obligación legal o orden judicial fundada, la compañía no
                revelará datos personales; cuando proceda, se documentará el
                alcance de la entrega según la normativa aplicable.
              </li>
            </ul>

            <p className="mb-4">
              Además de la compañía, podrán acceder a los datos categorías de
              personas autorizadas, encargados del tratamiento o proveedores
              necesarios para la prestación del servicio (infraestructura,
              medios de pago, comunicaciones, soporte), bajo obligaciones de
              confidencialidad y tratamiento acordes a la Ley 25.326.
            </p>

            <p className="mb-6">
              El Usuario puede solicitar información actualizada sobre
              encargados o categorías de destinatarios razonables a través de la{' '}
              <Link
                href="/contacto"
                className="font-medium text-blue-600 hover:text-blue-700 hover:underline"
              >
                página de contacto
              </Link>
              .
            </p>

            <h2 className="mt-10 text-xl font-semibold text-gray-900">
              6. Modalidad y lugar del tratamiento de datos personales
            </h2>

            <h3 className="mt-8 text-lg font-semibold text-gray-900">
              6.1. Lugar
            </h3>
            <p className="mb-4 mt-3">
              ROASAL S.A.S. opera principalmente desde Argentina. Los datos
              pueden procesarse también en otros países cuando los proveedores
              de infraestructura o servicios así lo requieran, siempre con
              salvaguardas compatibles con la Ley 25.326 y las orientaciones de la{' '}
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

            <p className="mb-6">
              Según la ubicación del Usuario o de los proveedores, puede
              implicarse transferencia internacional de datos; en tal caso se
              aplicarán los mecanismos previstos por la ley argentina.
            </p>

            <h3 className="mt-8 text-lg font-semibold text-gray-900">
              6.2. Período de conservación
            </h3>
            <p className="mb-4 mt-3">
              Salvo indicación distinta, los datos personales se conservarán
              durante el tiempo necesario para las finalidades para las que
              fueron recogidos y, adicionalmente, el que impongan obligaciones
              legales o contractuales.
            </p>

            <p className="mb-4">
              Cuando no exista tal obligación y haya vencido el plazo aplicable,
              los datos serán suprimidos o anonimizados cuando sea técnica y
              legalmente posible. Transcurrido el plazo de conservación, puede
              no ser posible ejercer derechos sobre información ya eliminada de
              forma definitiva.
            </p>

            <h3 className="mt-8 text-lg font-semibold text-gray-900">
              6.3. Fines específicos y actividades
            </h3>
            <p className="mb-4 mt-3">
              Los datos personales se tratan para prestar los servicios,
              cumplir obligaciones legales, responder solicitudes, proteger
              derechos e intereses legítimos, prevenir fraude o abuso y para las
              finalidades que se detallan a continuación.
            </p>

            <h4 className="mt-6 text-base font-semibold text-gray-900">
              Eventos y mediciones en el panel web
            </h4>
            <p className="mb-4 mt-3">
              El panel puede emitir eventos de uso (por ejemplo hitos de
              registro o de prueba) hacia el{' '}
              <code className="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-800">
                dataLayer
              </code>{' '}
              del navegador y guardar marcas en{' '}
              <code className="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-800">
                localStorage
              </code>{' '}
              para no duplicar mediciones, según se describe en la{' '}
              <Link
                href="/cookies"
                className="font-medium text-blue-600 hover:text-blue-700 hover:underline"
              >
                Política de Cookies
              </Link>
              . Si en el entorno no hay herramientas de terceros cargadas, esos
              eventos no implican envío automático a terceros fuera de lo que la
              aplicación transmita a los servidores de ROASAL S.A.S.
            </p>

            <h4 className="mt-6 text-base font-semibold text-gray-900">
              Formulario de contacto (sitio web)
            </h4>
            <p className="mb-4 mt-3">
              Al completar el formulario de contacto, el Usuario autoriza el uso
              de los datos indicados allí para responder consultas o
              solicitudes, según el objeto del formulario. Típicamente se tratan
              nombre, apellido, correo electrónico y el contenido del mensaje.
            </p>

            <p className="mb-6">
              La base jurídica del tratamiento en Argentina incluye, según el
              caso, el consentimiento del titular, la ejecución de medidas
              precontractuales o contractuales a su solicitud, el cumplimiento de
              obligaciones legales aplicables a ROASAL S.A.S. o el interés
              legítimo de la compañía en atender consultas y mejorar el servicio,
              conforme a la Ley 25.326 y normativa complementaria.
            </p>

            <h4 className="mt-6 text-base font-semibold text-gray-900">
              Medios de pago (Mercado Pago u otros proveedores)
            </h4>
            <p className="mb-6 mt-3">
              Cuando el Usuario cargue tarjeta u otros medios a través de
              integraciones con Mercado Pago u otros procesadores, parte del
              tratamiento (incluidos datos de pago sensibles) ocurre bajo la
              responsabilidad de ese proveedor, según sus propias políticas. La
              compañía recibe la información estrictamente necesaria para la
              operación de facturación y cobros en En Punto.
            </p>

            <h4 className="mt-6 text-base font-semibold text-gray-900">
              Derechos del titular de los datos
            </h4>
            <p className="mb-4 mt-3">
              El Usuario puede ejercer los derechos de acceso, rectificación,
              actualización, supresión cuando corresponda, confidencialidad y
              demás previstos por la Ley 25.326, mediante solicitud razonable por
              los canales de contacto. También puede presentar reclamo ante la{' '}
              <a
                href="https://www.argentina.gob.ar/aaip"
                className="font-medium text-blue-600 hover:text-blue-700 hover:underline"
                target="_blank"
                rel="noopener noreferrer"
              >
                AAIP
              </a>
              .
            </p>

            <ul className="mb-6 list-disc space-y-2 pl-5">
              <li>
                <strong className="font-medium text-gray-800">
                  Oposición o limitación.
                </strong>{' '}
                Cuando la ley lo permita, puede oponerse o solicitar la
                limitación del tratamiento.
              </li>
              <li>
                <strong className="font-medium text-gray-800">Acceso.</strong>{' '}
                Solicitar información sobre datos tratados y, cuando
                corresponda, copia simplificada.
              </li>
              <li>
                <strong className="font-medium text-gray-800">
                  Rectificación o actualización.
                </strong>{' '}
                Solicitar corrección de datos inexactos.
              </li>
              <li>
                <strong className="font-medium text-gray-800">
                  Supresión.
                </strong>{' '}
                Solicitar la eliminación cuando no exista obligación legal de
                conservación.
              </li>
            </ul>

            <h4 className="mt-6 text-base font-semibold text-gray-900">
              Obligaciones del Usuario
            </h4>
            <p className="mb-4 mt-3">
              1) Mantener actualizada su información tributaria, bancaria y
              personal en la plataforma, siendo exclusivamente responsable por
              cualquier daño, perjuicio, retraso o incumplimiento que pudiera
              derivarse de la omisión, falsedad o inexactitud de dicha
              información. La compañía no será responsable por las consecuencias
              de la falta de actualización o veracidad de los datos provistos
              por el Usuario.
            </p>

            <p className="mb-4">
              2) Proporcionar información veraz, exacta y completa respecto de
              su identidad y, si corresponde, sobre vehículos, mercaderías,
              personal u otros datos requeridos para el uso de los Servicios. La
              carga de información falsa o engañosa constituye incumplimiento
              grave de los{' '}
              <Link
                href="/terminos"
                className="font-medium text-blue-600 hover:text-blue-700 hover:underline"
              >
                Términos y Condiciones
              </Link>
              .
            </p>

            <p className="mb-6">
              Cualquier daño o responsabilidad que se origine por el
              incumplimiento de estas obligaciones será atribuible al Usuario. La
              compañía se reserva el ejercicio de las acciones legales que
              correspondan.
            </p>

            <h2 className="mt-10 text-xl font-semibold text-gray-900">
              7. Resolución de conflictos en materia de protección de datos
            </h2>
            <p className="mb-4 mt-3">
              Toda controversia, reclamo o conflicto vinculado al tratamiento de
              datos personales conforme al presente acuerdo se interpretará
              según la Ley N.º 25.326 y su normativa reglamentaria.
            </p>

            <p className="mb-4">
              Sin perjuicio del derecho del Usuario a formular denuncias ante la{' '}
              <a
                href="https://www.argentina.gob.ar/aaip"
                className="font-medium text-blue-600 hover:text-blue-700 hover:underline"
                target="_blank"
                rel="noopener noreferrer"
              >
                AAIP
              </a>
              , las disputas patrimoniales o contractuales entre el Usuario y la
              compañía podrán encomendarse a los tribunales competentes según lo
              convenido en los{' '}
              <Link
                href="/terminos"
                className="font-medium text-blue-600 hover:text-blue-700 hover:underline"
              >
                Términos y Condiciones
              </Link>
              .
            </p>

            <p className="mb-6">
              A estos efectos, las partes fijan como domicilio el de la
              compañía, sito en Pinar del Río 3631, ciudad de Córdoba, provincia
              de Córdoba, Argentina.
            </p>

            <div className="mt-10 border-t border-gray-100 pt-8 text-center">
              <p className="mb-4 text-gray-600">
                ¿Dudas sobre privacidad? Escribinos.
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
