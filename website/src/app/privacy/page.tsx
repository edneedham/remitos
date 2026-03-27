import Link from 'next/link';

export default async function Privacy() {
  return (
    <div className="flex flex-col min-h-screen">
      <main className="grow">
        {/* Hero Section */}
        <section className="py-20 px-8 bg-white">
          <div className="max-w-6xl mx-auto">
            <h1 className="text-4xl font-bold text-blue-600 mb-8 text-left">
              Acuerdo de Privacidad y Confidencialidad
            </h1>

            <p className="text-l text-gray-600 mb-8 mx-auto text-left">
              Última modificación: 18 de septiembre de 2025
            </p>
          </div>
        </section>

        {/* Section 1: Introducción */}
        <section className="py-20 px-8 bg-gray-50">
          <div className="max-w-6xl mx-auto">
            <h2 className="text-3xl font-bold text-gray-800 mb-8">
              1. Introducción
            </h2>

            <p className="text-gray-600 mb-6 leading-relaxed">
              &quot;En Punto&quot; es una marca logística registrada de la firma
              ROASAL S.A.S. CUIT 30- 71793629-5., con domicilio legal en Pinar
              del Río 3631, Córdoba, Argentina, de ahora en más denominada como
              &quot;la compañía&quot;.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Esta política de privacidad describe cómo ROASAL S.A.S., recoge,
              utiliza y protege su información personal cuando emplea los
              servicios de su propiedad, incluida la plataforma En Punto.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              El Usuario puede utilizar el la el software o plataforma conocidos
              como En Punto, así como interactuar con los diversos medios de
              comunicación oficiales de la compañía para acceder a la bolsa de
              trabajo, aplicar o publicar servicios logísticos, contactar con
              clientes, actualizar su información de perfil, entre otros.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Al suministrar el Usuario su aceptación a este aviso, la compañía
              supone la aceptación sin reservas de los términos aquí
              establecidos. La información suministrada por el usuario será
              usada por la compañía para fines contractuales entre otros fines,
              para proveer de servicios logísticos y propiciar el constante
              proceso de mejora de los servicios disponibles y en el desarrollo
              de nuevos productos.
            </p>
          </div>
        </section>

        {/* Section 2: Consentimiento informado */}
        <section className="py-20 px-8 bg-white">
          <div className="max-w-6xl mx-auto">
            <h2 className="text-3xl font-bold text-gray-800 mb-8">
              2. Consentimiento informado
            </h2>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Al hacer clic en el botón &quot;Acepto&quot; o acción equivalente,
              el Usuario manifiesta de manera libre, expresa e informada su
              consentimiento respecto del tratamiento de sus datos personales
              conforme a los términos establecidos en este Acuerdo de Privacidad
              y Confidencialidad.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Dicho consentimiento comprende la recopilación, almacenamiento,
              uso, procesamiento, cesión, transferencia internacional,
              conservación y, en su caso, supresión de datos, con las
              finalidades previstas en la presente política, en cumplimiento de
              la Ley N.º 25.326 de Protección de Datos Personales.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Asimismo, el Usuario declara haber leído y comprendido
              integralmente este documento, y acepta sin reservas las
              condiciones aquí estipuladas en su propio nombre o en
              representación de la persona jurídica a la que pertenezca,
              asumiendo plena responsabilidad por el uso de la plataforma.
            </p>
          </div>
        </section>

        {/* Section 4: Definición de Usuario */}
        <section className="py-20 px-8 bg-gray-50">
          <div className="max-w-6xl mx-auto">
            <h2 className="text-3xl font-bold text-gray-800 mb-8">
              3. Definición de Usuario
            </h2>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Este aviso aplica a todos los usuarios de las apps, sitios web, u
              otros servicios de ROASAL S.A.S. en cualquier parte del mundo. Se
              considera Usuario a toda persona fisica, jurídica o entidad que
              interactúa con los servicios de ROASAL S.A.S., lo que incluye pero
              no limita correos electrónicos, emails, mensajes, sitio web,
              software de aplicaciones, aplicaciones móviles y otros servicios
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              El usuario puede obtener acceso limitado a diferentes interfaces e
              información de &quot;En Punto&quot; dependiendo de su rol dentro
              de los circuitos logísticos. La empresa reconoce los siguientes
              posibles tipos de usuarios:
            </p>

            <ul className="list-disc pl-6 text-gray-600 mb-6 leading-relaxed space-y-2">
              <li>
                <strong>Transportistas:</strong> individuos o entidades que
                ofrecen vehículos motorizados para transportar bienes
              </li>
              <li>
                <strong>Conductores de transportistas:</strong> individuos que
                operan vehículos motorizados para transportar bienes para o en
                nombre de transportistas
              </li>
              <li>
                <strong>Clientes:</strong> cualquier usuario de los servicios
                que sea identificado como el remitente, consignatario o
                consignador, que posea los bienes que se transportan o que
                requiera algún tipo de servicio de transporte de mercadería
                terrestre.
              </li>
              <li>
                <strong>Despachadores:</strong> entidades logísticas que sirven
                de coordinadores de tráfico, asignando vehículos motorizados y
                conductores de transportistas para transportar bienes.
              </li>
            </ul>

            <p className="text-gray-600 mb-6 leading-relaxed">
              El uso de la aplicación, sea cual fuere el rol asignado, no
              implica ningún nivel de responsabilidad de la compañía para con el
              usuario ni relación de dependencia. Toda actividad comercial está
              sujeta a las leyes aplicables de la República Argentina. Al
              aceptar los términos y condiciones el usuario confirma que es
              total e independientemente responsable en materia jurídica e
              impositiva de sus actividades, sin repetición a la compañía.
            </p>
          </div>
        </section>

        {/* Section 5: Definición de datos */}
        <section className="py-20 px-8 bg-white">
          <div className="max-w-6xl mx-auto">
            <h2 className="text-3xl font-bold text-gray-800 mb-8">
              4. Definición de datos
            </h2>

            <p className="text-gray-600 mb-6 leading-relaxed">
              La información recopilada, almacenada y utilizada por la empresa
              responde a dos tipos de naturaleza:
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              <strong>5.1. Datos personales:</strong> Son información que puede
              utilizarse para identificar al Usuario, ya sea directa o
              indirectamente. Esto incluye pero no se limita a datos como el
              nombre, el apellido o apellidos, la dirección de correo
              electrónico, las tecnologías de seguimiento (como las cookies o
              píxeles de seguimiento), datos bancarios, información tributaria,
              la actividad del usuario y la información sobre su dispositivo.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Los datos personales serán proporcionados libremente por el
              Usuario. Salvo se indique lo contrario, todos los datos
              solicitados por En Punto son obligatorios y la negativa a
              proporcionarlos puede imposibilitar que la compañía pueda proceder
              a la prestación de sus servicios.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              En los casos en los que el software o la bolsa de trabajo indique
              específicamente que ciertos datos personales no son obligatorios,
              tendrá la opción de no comunicar tales datos sin que esto tenga
              consecuencia alguna sobre la disponibilidad o el funcionamiento
              del servicio.
            </p>

            <h3 className="text-2xl font-semibold text-gray-800 mb-4 mt-8">
              ROASAL S.A.S. Recopila la siguiente información:
            </h3>

            <div className="overflow-x-auto mb-6">
              <table className="w-full text-left border-collapse border border-gray-300">
                <thead>
                  <tr className="bg-gray-200">
                    <th className="p-4 border border-gray-300 font-semibold">
                      Categoría de datos
                    </th>
                    <th className="p-4 border border-gray-300 font-semibold">
                      Tipos de datos
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td className="p-4 border border-gray-300 align-top">
                      <strong>a. Información de cuenta.</strong>
                      <br />
                      Recopilamos datos cuando los usuarios crean, actualizan o
                      inician sesión en su cuenta de En Punto.
                    </td>
                    <td className="p-4 border border-gray-300">
                      Dirección; Información de pago y bancaria; Correo
                      electrónico; Nombre y apellido; Nombre de usuario y
                      contraseña; Número de teléfono; Configuraciones y
                      preferencias; Documentos de identificación
                      gubernamentales; Números e imágenes de licencia de
                      conducir; Fecha de nacimiento; Firma; Información del
                      vehículo; Información de seguro; Número de placa de
                      matrícula; Número de identificación del vehículo;
                      Información de registro del vehículo; Documentación
                      respaldatoria de habilitaciones municipales, provinciales,
                      inspecciones, seguros o licencias.
                    </td>
                  </tr>
                  <tr>
                    <td className="p-4 border border-gray-300 align-top">
                      <strong>b. Información sensible de tratamiento:</strong>
                      <br />
                      Recopilamos datos sensible de tratamiento exclusivamente
                      cuando resultan necesarios para cumplir con finalidades
                      específicas y legales.
                    </td>
                    <td className="p-4 border border-gray-300">
                      Fotografías personales de identificación; Información
                      bancaria; Información fiscal y tributaria; Datos
                      biométricos (si se utilizan medios de validación con
                      imagen o firma digital);
                    </td>
                  </tr>
                  <tr>
                    <td className="p-4 border border-gray-300 align-top">
                      <strong>c. Datos demográficos.</strong>
                      <br />
                      Recopilamos datos demográficos a través de encuestas de
                      usuarios. También podemos recibir datos demográficos sobre
                      usuarios de terceros.
                    </td>
                    <td className="p-4 border border-gray-300">
                      Género o género inferido (usando el primer nombre);
                      Certificaciones de pequeñas empresas de transportistas
                      propietarios operadores
                    </td>
                  </tr>
                  <tr>
                    <td className="p-4 border border-gray-300 align-top">
                      <strong>d. Metadatos y metainformación</strong>
                    </td>
                    <td className="p-4 border border-gray-300">
                      Datos de uso, interacción, segmentación, estadísticas,
                      información arrojada por sistemas de navegacion y/o
                      marketing; Información de navegación; Tiempos de actividad
                      e inactividad; Segmentación de uso; Estadísticas
                      operativas; Resultados de sistemas de análisis de
                      comportamiento; Información generada por cookies,
                      etiquetas de píxel u otras tecnologías de seguimiento;
                      Logs del sistema; Datos de interacción con otras cuentas o
                      publicaciones dentro de la plataforma.
                    </td>
                  </tr>
                  <tr>
                    <td className="p-4 border border-gray-300 align-top">
                      <strong>e. Datos de uso.</strong>
                      <br />
                      Es toda la información referida a interacciones del
                      usuario con los Servicios de la Compañía, incluyendo pero
                      no limitado a prestaciones de servicios concretadas como
                      consecuencia de conexiones o interacciones originadas en
                      el entorno virtual de En Punto o cualquier servicio
                      prestado por ROASAL S.A.S. o sus afiliados.
                    </td>
                    <td className="p-4 border border-gray-300">
                      Registros de chat y grabaciones de llamadas;
                      Calificaciones o retroalimentación; Fotos y grabaciones
                      subidas; Registros de servicios prestados; Registros de
                      domicilios visitados; Registros de acuerdos comerciales
                    </td>
                  </tr>
                  <tr>
                    <td className="p-4 border border-gray-300 align-top">
                      <strong>f. Contenido de usuario.</strong>
                      <br />
                      Datos que recopilamos cuando los usuarios: Contactan a En
                      Punto para soporte al cliente u otras consultas. Suben
                      fotos y grabaciones, incluyendo aquellas enviadas para
                      fines de soporte al cliente o para confirmar entrega
                      Proporcionan calificaciones o retroalimentación de
                      instalaciones para fines de soporte al cliente o para
                      confirmar entrega. Proporcionan calificaciones o
                      retroalimentación
                    </td>
                    <td className="p-4 border border-gray-300">
                      Registros de chat y grabaciones de llamadas;
                      Calificaciones o retroalimentación; Fotos y grabaciones
                      subidas
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

            <p className="text-gray-600 mb-6 leading-relaxed">
              La información se recopila automáticamente de la interacción del
              usuario con el software denominado &quot;En Punto&quot; y todos
              los canales de comunicación oficiales de la compañía, lo que
              incluye pero no limita correos electrónicos, emails, mensajes,
              sitio web, software de aplicaciones, aplicaciones móviles y otros
              servicios (los &quot;servicios&quot;). La manipulación de la
              información se realiza en cumplimiento con la Ley 25.326 de
              Protección de Datos Personales y su reglamentación.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              El Usuario asume la responsabilidad respecto de los datos
              personales de terceros que se obtengan, publiquen o compartan a
              través de este sitio web.
            </p>
          </div>
        </section>

        {/* Section 6: Métodos de tratamiento */}
        <section className="py-20 px-8 bg-gray-50">
          <div className="max-w-6xl mx-auto">
            <h2 className="text-3xl font-bold text-gray-800 mb-8">
              5. Métodos de tratamiento
            </h2>

            <p className="text-gray-600 mb-6 leading-relaxed">
              ROASAL S.A.S. se compromete a adoptar las medidas de seguridad
              apropiadas para impedir el acceso no autorizado a los datos
              personales de los usuarios, la revelación, la alteración o la
              destrucción no autorizados de los mismos independientemente de
              dónde se encuentren, o por quién se procesen sus datos personales.
              El almacenamiento de la información se hará mediante plataformas
              de terceros de servicios Cloud. ROASAL S.A.S.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              El tratamiento de datos se realizará mediante ordenadores y/o
              herramientas informáticas, siguiendo procedimientos y modalidades
              organizativas estrictamente relacionadas con las finalidades
              señaladas. ROASAL S.A.S. se compromete a informar a sus usuarios
              sobre las modificaciones futuras mediante sistemas informativos
              disponibles, ya sea mediante notificaciones push, correo
              electrónicos y/o otros medios disponibles.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Esto incluye implementar medidas globales para proteger los datos
              de los usuarios, incluyendo:
            </p>

            <ul className="list-disc pl-6 text-gray-600 mb-6 leading-relaxed space-y-2">
              <li>
                Asegurar los datos de los usuarios en tránsito, incluyendo
                mediante encriptación, y en reposo.
              </li>
              <li>
                Requerir entrenamiento en toda la compañía respecto a privacidad
                y seguridad de datos
              </li>
              <li>
                Implementar políticas y procedimientos internos para limitar el
                acceso a, y uso de, los datos de los usuarios.
              </li>
              <li>
                Mantener la confidencialidad de los datos de los usuarios a el
                acceso gubernamental y de aplicación de la ley a los datos de
                los usuarios, excepto cuando sea requerido por ley, jueces, haya
                amenazas inminentes a la seguridad nacional, o los usuarios
                hayan consentido el acceso por escrito y/o de manera presencial.
              </li>
            </ul>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Además de la compañía, en algunos casos podrán acceder a los datos
              ciertas categorías de datos personas autorizadas o subsidiarias,
              relacionadas con el funcionamiento esencial de este sitio web.
              También podrán acceder a los datos personas o empresas externas a
              la compañía como encargados del tratamiento, procesamiento,
              recolección o oficial de protección de dato o entidades
              autorizadas para garantizar la correcta prestación de los
              servicios ofrecidos por esta compañía
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              El usuario podrá solicitar en cualquier momento la lista
              actualizada de estas personas o empresas contactando por medio de
              los canales oficiales descritos en 2. Contacto, en el presente
              documento.
            </p>
          </div>
        </section>

        {/* Section 7: Modalidad y lugar del tratamiento */}
        <section className="py-20 px-8 bg-white">
          <div className="max-w-6xl mx-auto">
            <h2 className="text-3xl font-bold text-gray-800 mb-8">
              6. Modalidad y lugar del tratamiento de datos personales
            </h2>

            <h3 className="text-2xl font-semibold text-gray-800 mb-4">
              7.1 Lugar
            </h3>
            <p className="text-gray-600 mb-6 leading-relaxed">
              ROASAL S.A.S. opera y procesa datos en Argentina y globalmente.
              Los datos recopilados pueden ser transferidos a países distintos
              del de residencia o del de prestación de servicios de la compañía
              para fines de procesamiento de la información, siempre en
              cumplimiento con la Ley 25.326 y disposiciones de la Agencia de
              Acceso a la Información Pública (AAIP).
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Dependiendo de la localización del Usuario, los datos relacionados
              a su cuenta pueden implicar la transferencia de sus datos a otro
              país diferente del suyo propio.
            </p>

            <h3 className="text-2xl font-semibold text-gray-800 mb-4">
              7.2 Período de conservación
            </h3>
            <p className="text-gray-600 mb-6 leading-relaxed">
              Salvo que se indique lo contrario en el presente documento, los
              datos personales serán tratados y conservados durante el tiempo
              necesario y para la finalidad por la que han sido recogidos y
              podrán conservarse durante más tiempo debido a una obligación
              legal pertinente o sobre la base de su consentimiento.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Cuando la compañía recoja su información personal, la conservará
              durante el tiempo que sea necesario para las finalidades para las
              cuales fueron recogidas. En algunas ocasiones, la compañía
              necesitará conservar la información personal del usuario durante
              más tiempo debido a una obligación legal o basado en su
              consentimiento.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Una vez terminado el período de conservación, los datos personales
              de los usuarios serán eliminados. Por lo tanto, los derechos de
              acceso, supresión, rectificación y de portabilidad de datos no
              podrán ejercitarse una vez haya expirado dicho periodo de
              conservación.
            </p>

            <h3 className="text-2xl font-semibold text-gray-800 mb-4">
              7.3 Información detallada sobre el tratamiento de datos personales
            </h3>
            <p className="text-gray-600 mb-6 leading-relaxed">
              Los datos personales de los usuarios se recogen para posibilitar
              la prestación de los servicios, cumplir las obligaciones legales
              de la compañía, responder a solicitudes de ejecución, proteger
              derechos e intereses (o de los usuarios, la compañía o los de
              terceros), detectar cualquier actividad maliciosa o fraudulenta,
              así como para las finalidades descritas a continuación:
            </p>

            <h4 className="text-xl font-semibold text-gray-800 mb-4">
              Visualizar contenidos de plataformas externas
            </h4>
            <p className="text-gray-600 mb-6 leading-relaxed">
              Este tipo de servicios permiten a los usuarios visualizar
              contenidos alojados en plataformas externas directamente desde las
              páginas del sitio web e interactuar con estos. A menudo dichos
              servicios se denominan widgets -pequeños elementos colocados en un
              sitio web o aplicación-. Proporcionan información específica o
              realizan una función concreta y a menudo permiten la interacción
              con los usuarios. Este tipo de servicios puede recoger datos de
              tráfico web respecto de las páginas en las que estén instalados
              incluso en caso de que usted no los utilice.
            </p>

            <h4 className="text-xl font-semibold text-gray-800 mb-4">
              Formulario de contacto (este sitio web)
            </h4>
            <p className="text-gray-600 mb-6 leading-relaxed">
              Al rellenar el usuario el formulario de contacto con sus datos, el
              usuario autoriza a la compañía a usar estos datos para responder a
              sus solicitudes de información, de presupuestos o a cualquier otro
              tipo de solicitud, según se indica en el encabezamiento del
              formulario.
            </p>
            <p className="text-gray-600 mb-6 leading-relaxed">
              Datos personales que se tratan: dirección de correo electrónico;
              nombre; apellido(s) Categoría de Información personal recogida con
              arreglo a la CCPA: identificadores. Este tratamiento constituye:
              una venta de conformidad con la CCPA, VCDPA, CPA, CTDPA y UCPA.
            </p>

            <h4 className="text-xl font-semibold text-gray-800 mb-4">
              Información adicional - Base jurídica del tratamiento
            </h4>
            <p className="text-gray-600 mb-6 leading-relaxed">
              La compañía puede tratar los datos personales que le conciernen si
              el usuario ha dado su consentimiento o para una o más finalidades
              específicas:
            </p>

            <ul className="list-disc pl-6 text-gray-600 mb-6 leading-relaxed space-y-2">
              <li>
                Si la obtención de datos es necesaria para la ejecución de un
                contrato celebrado con el usuario y/o cualquier otra obligación
                precontractual del mismo;
              </li>
              <li>
                Si el tratamiento es necesario para el cumplimiento de una
                obligación legal a la que la compañía es aplicable;
              </li>
              <li>
                Si el tratamiento está relacionado con una misión realizada en
                interés público o en el ejercicio de poderes públicos que a la
                empresa han sido conferidos;
              </li>
              <li>
                Si el tratamiento es necesario para la satisfacción de intereses
                legítimos perseguidos por ROASAL S.A.S. o por un tercero.
              </li>
            </ul>

            <p className="text-gray-600 mb-6 leading-relaxed">
              La obtención de datos personales por medio de los servicios se
              establece así como un requisito operativo y legal para el
              cumplimiento de los objetivos propios del contrato celebrado entre
              la compañía y el usuario para asegurar la correcta ejecución de
              los servicios.
            </p>

            <h4 className="text-xl font-semibold text-gray-800 mb-4">
              Información sobre este documento: derechos y obligaciones del
              usuario
            </h4>
            <p className="text-gray-600 mb-6 leading-relaxed">
              El usuario puede ejercer sus derechos en relación con los datos
              que sean tratados por ROASAL S.A.S. En particular, tiene derecho a
              hacer lo siguiente, en la medida en que lo permita la ley:
            </p>

            <ul className="list-disc pl-6 text-gray-600 mb-6 leading-relaxed space-y-2">
              <li>
                <strong>Oposición al tratamiento de datos.</strong> Tiene
                derecho a oponerse al tratamiento de datos si dicho tratamiento
                se lleva a cabo con arreglo a una base jurídica distinta del
                consentimiento.
              </li>
              <li>
                <strong>Acceso a sus datos.</strong> Tiene derecho a saber si la
                compañía está tratando sus datos, a obtener información sobre
                ciertos aspectos del tratamiento, así como a obtener una copia
                de los datos objeto del tratamiento.
              </li>
              <li>
                <strong>Verificar y solicitar la rectificación.</strong> Tiene
                derecho a verificar la exactitud de sus datos y a solicitar que
                los mismos se actualicen o corrijan.
              </li>
              <li>
                <strong>
                  Recibir sus datos y transferirlos a otro responsable del
                  tratamiento.
                </strong>{' '}
                Tiene derecho a recibir sus datos en un formato estructurado, de
                uso común y lectura mecánica y, si fuera técnicamente posible, a
                que se transmitan los mismos a otro responsable del tratamiento
                sin ningún impedimento.
              </li>
              <li>
                <strong>Presentar una reclamación.</strong> Tiene derecho a
                presentar una reclamación ante la autoridad competente en
                materia de protección de datos personales.
              </li>
              <li>
                <strong>
                  Averiguar los motivos de las transferencias de datos.
                </strong>{' '}
                Tiene derecho a conocer las bases jurídicas de las
                transferencias de datos al extranjero, inclusive a cualquier
                organización internacional que se rige por el Derecho
                Internacional Público o que esté formada por dos o más países,
                como la ONU.
              </li>
              <li>
                <strong>Conocer las medidas de seguridad:</strong> Tiene derecho
                a conocer las medidas de seguridad que la compañía adopta para
                proteger sus datos.
              </li>
            </ul>

            <p className="text-gray-600 mb-6 leading-relaxed">
              La compañía informará de cualquier rectificación o supresión de
              datos personales o limitación del tratamiento a cada destinatario,
              en su caso, al que se le hayan comunicado los datos personales,
              salvo que sea imposible o exija un esfuerzo desproporcionado.
            </p>

            <h4 className="text-xl font-semibold text-gray-800 mb-4">
              El Usuario se obliga a:
            </h4>

            <p className="text-gray-600 mb-6 leading-relaxed">
              1) Mantener actualizada su información tributaria, bancaria y
              personal en la plataforma, siendo exclusivamente responsable por
              cualquier daño, perjuicio, retraso o incumplimiento contractual
              que pudiera derivarse de la omisión, falsedad o inexactitud de
              dicha información. La Compañía no será responsable, bajo ninguna
              circunstancia, por las consecuencias derivadas de la falta de
              actualización o veracidad de los datos provistos por el Usuario.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              2) Proporcionar información veraz, exacta y completa respecto de
              su identidad y, en caso de corresponder, sobre vehículos,
              mercaderías, personal afectado a las operaciones o cualquier otro
              dato requerido para la utilización de los Servicios. La carga de
              información falsa, engañosa, adulterada o inexacta será
              considerada un incumplimiento grave de los presentes Términos y
              Condiciones.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Cualquier daño o responsabilidad, de carácter civil, penal,
              administrativo o comercial, que se origine como consecuencia del
              incumplimiento de estas obligaciones será atribuible al Usuario.
              La Compañía se reserva el derecho de ejercer las acciones legales
              correspondientes, incluyendo pero no limitándose a reclamos por
              daños y perjuicios, con más costas y honorarios a cargo del
              Usuario y/o sus representantes legales.
            </p>
          </div>
        </section>

        {/* Section 8: Resolución de Conflictos */}
        <section className="py-20 px-8 bg-gray-50">
          <div className="max-w-6xl mx-auto">
            <h2 className="text-3xl font-bold text-gray-800 mb-8">
              7. Resolución de Conflictos en Materia de Protección de Datos
            </h2>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Toda controversia, reclamo o conflicto que se derive del
              tratamiento de datos personales, su recolección, almacenamiento,
              uso, acceso, rectificación o supresión, así como cualquier otro
              aspecto vinculado al presente Aviso de Privacidad, será resuelto
              conforme a lo previsto por la Ley N.º 25.326 de Protección de
              Datos Personales y su normativa reglamentaria.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Sin perjuicio del derecho del Usuario a formular denuncias ante la
              Agencia de Acceso a la Información Pública (AAIP), autoridad de
              aplicación nacional, cualquier disputa legal entre el Usuario y la
              Compañía será dirimida ante los tribunales ordinarios con
              competencia en la ciudad de Córdoba, Provincia de Córdoba,
              República Argentina, con renuncia expresa a cualquier otro fuero o
              jurisdicción.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Las partes fijan como domicilio legal a estos efectos el de la
              Compañía, sito en Pinar del Río 3631, ciudad de Córdoba, provincia
              de Córdoba, Argentina.
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
              Si tiene alguna pregunta sobre este Aviso de Privacidad,
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
