import Link from 'next/link';

export default async function Terms() {
  return (
    <div className="flex flex-col min-h-screen">
      <main className="grow">
        {/* Hero Section */}
        <section className="py-20 px-8 bg-white">
          <div className="max-w-6xl mx-auto">
            <h1 className="text-4xl font-bold text-blue-600 mb-8 text-left">
              Términos y Condiciones de Uso
            </h1>

            <p className="text-l text-gray-600 mb-8 mx-auto text-left">
              Última modificación: 18 de septiembre de 2025
            </p>
          </div>
        </section>

        {/* Introduction Section */}
        <section className="py-20 px-8 bg-gray-50">
          <div className="max-w-6xl mx-auto">
            <h2 className="text-3xl font-bold text-gray-800 mb-8">
              Introducción
            </h2>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Estos &quot;Términos y Condiciones de Uso&quot; rigen el acceso e
              interacción de cualquier individuo, persona física o entidad
              jurídica (de ahora en más denominado &quot;Usuario&quot;) para con
              el software conocido como &quot;En Punto&quot;, incluidas las
              aplicaciones móviles, sitios web, contenido, productos y servicios
              (colectivamente denominados de ahora en más como los
              &quot;Servicios&quot;) proporcionados por ROASAL S.A.S.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              &quot;En Punto Logística&quot; es una marca logística registrada
              de la firma ROASAL S.A.S. CUIT 30-71793629-5., con domicilio legal
              en Pinar del Río 3631, Córdoba, Argentina, de ahora en más
              denominada como &quot;la compañía&quot;.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              <strong>
                LEA ESTOS TÉRMINOS CUIDADOSAMENTE ANTES DE ACCEDER Y UTILIZAR
                LOS SERVICIOS.
              </strong>
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              En estos Términos, las palabras &quot;incluyendo&quot; e
              &quot;incluye&quot; significan &quot;incluyendo, pero no limitado
              a&quot;.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Al acceder a la plataforma y/o hacer uso de de los Servicios, el
              Usuario confirma estar de acuerdo y vinculado a estos Términos y
              Condiciones, que establecen una relación contractual entre el
              Usuario y la Compañía. Estos Términos sustituyen cualquier otro
              acuerdo relacionado a los Servicios que hubiera entre el Usuario y
              la Compañía, excepto que existieran contratos escritos,
              certificados y legalizados previos a la existencia de este
              documento; en cuyo caso prevalecerá el primero en entrar en
              vigencia.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              La compañía, por medio de &quot;En Punto&quot;, presta servicios
              de networking, coordinación de cargas terrestres, bolsa de
              trabajo, administración y procesamiento de pagos para terceras
              partes con fines de crear una red de logística dinámica y
              eficiente a nivel Nacional.
            </p>
          </div>
        </section>

        {/* Section 1: Definiciones Generales */}
        <section className="py-20 px-8 bg-white">
          <div className="max-w-6xl mx-auto">
            <h2 className="text-3xl font-bold text-gray-800 mb-8">
              1. Definiciones Generales
            </h2>

            <p className="text-gray-600 mb-6 leading-relaxed">
              A los fines de estos Términos y Condiciones de Uso, se entenderán
              por:
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              <strong>Usuario:</strong> Toda persona humana o jurídica que
              acceda, interactúe, utilice o se registre en cualquiera de las
              plataformas, sistemas, aplicaciones, interfaces, canales de
              comunicación oficiales, sitios web, software o servicios de
              titularidad de ROASAL S.A.S., incluyendo la plataforma denominada
              &quot;En Punto&quot;. La condición de Usuario no genera relación
              laboral, societaria, ni vínculo de dependencia alguno con la
              compañía.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              <strong>Administrador:</strong> Persona física autorizada por una
              persona jurídica usuaria para gestionar su perfil, acceso y
              operaciones dentro de la plataforma. Es responsable por la
              veracidad y legalidad de las acciones realizadas en nombre de la
              organización representada.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              <strong>Transportista:</strong> Individuo o entidad registrada que
              ofrece vehículos motorizados habilitados para el transporte de
              Bienes quienes no pertencen a esta firma.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              <strong>Conductor:</strong> Persona física que opera vehículos
              para realizar servicios de transporte en nombre propio o de un
              transportista.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              <strong>Cliente y/o Dador de carga:</strong> Usuario que contrata
              servicios de transporte, en carácter de remitente, consignante o
              consignatario de la mercadería. Esta categoría excluye a los
              consumidores finales en los terminus de la Ley 24.240.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              <strong>Despachador:</strong> Entidad o persona física que actúa
              como coordinador logístico de cargas, asignando vehículos, rutas u
              operadores a los fines del cumplimiento de traslados.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              <strong>Transacción:</strong> Todo acuerdo comercial celebrado
              entre usuarios de la plataforma, de forma directa o indirecta, que
              derive en la prestación efectiva de un servicio logístico o
              cualquier otra operación vinculada.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              <strong>Interacción:</strong> Toda acción realizada entre usuarios
              dentro del entorno de la plataforma, ya sea mediante mensajería,
              publicación, búsqueda, contacto, cotización, calificación u otras
              funcionalidades disponibles.
            </p>
          </div>
        </section>

        {/* Section 2: Condiciones del servicio */}
        <section className="py-20 px-8 bg-gray-50">
          <div className="max-w-6xl mx-auto">
            <h2 className="text-3xl font-bold text-gray-800 mb-8">
              2. Condiciones del servicio
            </h2>

            <h3 className="text-2xl font-semibold text-gray-800 mb-4">
              2.1 Licencia
            </h3>
            <p className="text-gray-600 mb-6 leading-relaxed">
              Sujeto a su cumplimiento de estos Términos y Condiciones, La
              Compañía otorga una licencia limitada de uso al Usuario. Esta
              licencia no es exclusiva, no sublicenciable, y tampoco es
              transferible. La licencia puede ser revocada sin previo aviso a
              discreción de los administradores de ROASAL S.A.S. Cualquier
              derecho no otorgado expresamente en este documento está reservado
              para la Compañía y sus afiliadas, según corresponda.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Esta licencia limitada de uso permite al Usuario acceder a los
              Servicios de ROASAL S.A.S., registrarse en la bolsa de trabajo,
              aplicar a ofertas, postear servicios vacantes e interactuar con
              ellos, y hacer uso de la plataforma web para la coordinación de
              actividades logísticas.
            </p>

            <h3 className="text-2xl font-semibold text-gray-800 mb-4">
              2.2 Obligaciones
            </h3>
            <p className="text-gray-600 mb-6 leading-relaxed">
              Es obligación del Usuario mantener en confidencialidad todas las
              credenciales para acceder y/o usar los Servicios, permitiendo que
              sólo personal autorizado a actuar en su nombre y representación
              acceda y/o use los Servicios relacionados a su cuenta. El Usuario
              se asume responsable de toda interacción o actividad realizada a
              su nombre dentro de las plataformas de la Compañía.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              El Usuario declara al hacer uso de los Servicios que se encuentra
              debidamente habilitado en materia tributaria y legal para la
              prestación de actividades económicas, así como declara poseer has
              habilitaciones pertinentes de transporte, sanitarias o de
              cualquier rubro requerido para interactuar con la bolsa de trabajo
              o Servicios específicos publicados en la misma.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Para poder utilizar correctamente los servicios ofrecidos por la
              plataforma, el Usuario deberá garantizar que cuenta con toda la
              documentación técnica, legal y operativa necesaria según el tipo
              de actividad que desee realizar.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              ROASAL S.A.S. podrá, en cualquier momento y sin previo aviso,
              solicitar la presentación o validación de dicha documentación. El
              incumplimiento de este requisito puede derivar en la suspensión
              temporal, limitación o baja definitiva de la cuenta.
            </p>

            <h3 className="text-2xl font-semibold text-gray-800 mb-4">
              2.3 Limitaciones
            </h3>
            <p className="text-gray-600 mb-6 leading-relaxed">
              La Compañía se reserva el derecho a cesar la prestación de
              Servicios total o parcialmente, en cualquier momento y por
              cualquier razón, y sin necesidad de previo aviso. Esto incluye
              pero no se limita a:
            </p>

            <ul className="list-disc pl-6 text-gray-600 mb-6 leading-relaxed space-y-2">
              <li>
                2.3.1. Al cese de operaciones total y cierre del sitio web;
              </li>
              <li>2.3.2. A la baja o eliminación de cuentas de usuario;</li>
              <li>
                2.3.3. Al cierre o bloqueo de cuentas de usuario específicas,
                privando el acceso al sistema.
              </li>
            </ul>

            <p className="text-gray-600 mb-6 leading-relaxed">
              ROASAL S.A.S. se reserva además el derecho a modificar los
              Términos y Condiciones en cualquier momento para adecuarlo a
              requerimientos de sistema y la normativa legal vigente. Las
              enmiendas serán efectivas tras la publicación de dichos Términos
              actualizados por parte de la compañía en esta ubicación. La
              continuación del uso de los servicios de la compañía por parte del
              Usuario después de la actualización notificada de los Términos y
              Condiciones constituyen en sí consentimiento de ambas partes en
              relación a las actualizaciones.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Consulte el Acuerdo de Privacidad de ROASAL S.A.S. ubicado{' '}
              <Link href="/privacy" className="text-blue-600 hover:underline">
                aquí
              </Link>{' '}
              para obtener información sobre nuestra recopilación y uso de
              información personal en conexión con los Servicios, en
              cumplimiento con la Ley 25.326 de Protección de Datos Personales
              de Argentina.
            </p>
          </div>
        </section>

        {/* Section 3: Restricciones */}
        <section className="py-20 px-8 bg-white">
          <div className="max-w-6xl mx-auto">
            <h2 className="text-3xl font-bold text-gray-800 mb-8">
              3. Restricciones
            </h2>

            <p className="text-gray-600 mb-6 leading-relaxed">
              El usuario no tiene derecho de ejecutar ni instar a terceros a
              realizar las siguientes acciones:
            </p>

            <ul className="list-none text-gray-600 mb-6 leading-relaxed space-y-4">
              <li>
                (i) eliminar cualquier aviso de derechos de autor, marca
                comercial u otros avisos propietarios de cualquier elemento,
                sección o producción relacionada a los Servicios;
              </li>
              <li>
                (ii) reproducir, modificar, preparar trabajos derivados basados
                en, distribuir, licenciar, arrendar, vender, revender,
                transferir, mostrar públicamente, realizar públicamente,
                transmitir, transmitir en streaming, difundir o explotar de otra
                manera los Servicios excepto existiere un acuerdo legal entre
                ROASAL S.A.S. y el usuario específico para tales fines;
              </li>
              <li>
                (iii) descompilar, desensamblar, realizar ingeniería inversa o
                intentar derivar el código fuente o la tecnología subyacente,
                metodologías o algoritmos de los Servicios, excepto según lo
                permitido por la ley aplicable;
              </li>
              <li>
                (iv) enlazar, reflejar o enmarcar cualquier porción de los
                Servicios;
              </li>
              <li>
                (v) causar o lanzar cualquier programa o script con el propósito
                de scraping, indexación, encuestas u otra minería de datos de
                cualquier porción de los Servicios, o sobrecargar o dificultar
                indebidamente la operación y/o funcionalidad de cualquier
                aspecto de los Servicios; o
              </li>
              <li>
                (vi) intentar obtener acceso no autorizado o perjudicar
                cualquier aspecto de los Servicios o sus sistemas o redes
                relacionados.
              </li>
            </ul>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Cualquier violación de estos puntos será considerádo una grave
              infracción, además de causar perjuicio a ROASAL S.A.S., quien se
              reserva el derecho a requerir acciones legales y penalizaciones.
              Cualquier violación que perjudique, además, la gestión de datos
              confidenciales será tratada como una violación a las leyes y
              estatutos provinciales, requiriendo la propicia intervención
              judicial.
            </p>
          </div>
        </section>

        {/* Section 4: Propiedad */}
        <section className="py-20 px-8 bg-gray-50">
          <div className="max-w-6xl mx-auto">
            <h2 className="text-3xl font-bold text-gray-800 mb-8">
              4. Propiedad
            </h2>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Los Servicios y todos los derechos sobre los mismos son propiedad
              de ROASAL S.A.S. y sus licenciantes, incluida la marca registrada
              &quot;En Punto Logística&quot;. El uso de los Servicios no otorgan
              al Usuario ningún derecho:
            </p>

            <ul className="list-none text-gray-600 mb-6 leading-relaxed space-y-2">
              <li>
                (i) sobre el software o las prestaciones de Servicios excepto
                por la licencia de uso limitada otorgada anteriormente; o
              </li>
              <li>
                (ii) de usar o referenciar de cualquier manera los nombres de
                compañía, logotipos, nombres de productos y servicios, marcas
                comerciales o marcas de servicio de En Punto o de sus
                licenciantes.
              </li>
            </ul>

            <p className="text-gray-600 mb-6 leading-relaxed">
              La compañía informa al Usuario que toda información ingresada en
              sus entornos virtuales o medios de comunicación puede ser
              almacenada acorde al Acuerdo de Privacidad y Confidencialidad, que
              forma parte de este acuerdo de Términos y Condiciones. Acceder a
              los servicios y/o proporcionar datos personales supone la
              aceptación sin reservas de los términos y condiciones aquí
              establecidos.
            </p>
          </div>
        </section>

        {/* Section 5: RESPONSABILIDAD */}
        <section className="py-20 px-8 bg-white">
          <div className="max-w-6xl mx-auto">
            <h2 className="text-3xl font-bold text-gray-800 mb-8">
              5. RESPONSABILIDAD
            </h2>

            <p className="text-gray-600 mb-6 leading-relaxed">
              ROASAL S.A.S. Actúa exclusivamente como intermediario entre
              dadores de carga, transportistas y/o conductores, brindando una
              plataforma digital para que las partes puedan contactarse, acordar
              condiciones comerciales y eventualmente celebrar contratos de
              transporte entre ellas. Su responsabilidad y alcance se limita al
              servicio de la pltaforma que se oferta como intermediario
              tecnologico dirigido a la relación &quot;business to
              business&quot;.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              La empresa no participa, interviene ni tiene injerencia en la
              ejecución del servicio de transporte, en la negociación de
              tarifas, condiciones logísticas, seguros, ni en el cumplimiento de
              las obligaciones asumidas por los usuarios.
            </p>

            <h3 className="text-2xl font-semibold text-gray-800 mb-4">
              5.1. Descargo de responsabilidad
            </h3>
            <p className="text-gray-600 mb-6 leading-relaxed">
              Los servicios se proporcionan &quot;tal cual&quot; y &quot;según
              disponibilidad&quot;. ROASAL S.A.S. No se hace responsable de:
            </p>

            <ul className="list-disc pl-6 text-gray-600 mb-6 leading-relaxed space-y-2">
              <li>
                Inconvenientes surgidos por la cancelación de cargas pactadas
                entre diferentes partes dentro del software.
              </li>
              <li>
                Siniestros, conflictos legales o retrasos derivados del
                transporte;
              </li>
              <li>Daños a la mercadería o pérdida de la misma</li>
              <li>
                Tarifas acordadas fuera de los límites del software y/o los
                servicios.
              </li>
              <li>
                Plazos de pago acordados fuera de los límites del software y/o
                los servicios.
              </li>
              <li>
                Cualquier acuerdo, transacción o estipulación convenida fuera
                del entorno de la plataforma
              </li>
            </ul>

            <p className="text-gray-600 mb-6 leading-relaxed">
              ROASAL S.A.S. Declara que no tiene responsabilidad, obligación ni
              incidencia sobre las mercancías transportadas o a transportar; la
              compañía confirma no tener acceso a la mercadería o bienes a
              transportar y/o ser transportada excepto explícita indicación de
              lo contrario.
            </p>

            <h3 className="text-2xl font-semibold text-gray-800 mb-4">
              5.2 ALCANCE
            </h3>
            <p className="text-gray-600 mb-6 leading-relaxed">
              La compañía se exime y deslinda de cualquier responsabilidad
              solidaria para con choferes, dadores de carga, transportistas o
              clientes en caso de inconvenientes, siniestros, o disputas
              legales, civiles o sociales de ningún tipo.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              ROASAL S.A.S. renuncia a todas las representaciones expresas,
              implícitas o estatutarias, no establecidas expresamente en estos
              términos. La compañía no hace ninguna representación ,o garantía
              con respecto a la confiabilidad, puntualidad, calidad, adecuación
              o disponibilidad de los servicios, o que los servicios serán
              ininterrumpidos o libres de errores. Usted acepta que todo el
              riesgo que surja de su acceso o uso de los servicios permanece
              únicamente con usted, en la medida máxima permitida por la ley
              aplicable.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              ROASAL S.A.S. como organización privada independiente, no asume
              responsabilidad sobre la calidad tributaria, legal o social de sus
              usuarios. La compañía asume que todos los usuarios son personas
              fisicas o jurídicas registradas, habilitadas, y en uso total de
              sus facultades a la hora de acceder a sus servicios, y por tanto
              los usuarios son completa e intransferiblemente responsables de
              sus acciones.
            </p>

            <h3 className="text-2xl font-semibold text-gray-800 mb-4">
              5.3 Limitación de responsabilidad
            </h3>
            <p className="text-gray-600 mb-6 leading-relaxed">
              La compañía no será responsable por daños indirectos,
              incidentales, especiales, ejemplares, punitivos o consecuentes,
              incluyendo pero no limitado a pérdidas de ganancias, pérdidas de
              datos contractuales, lesiones personales o daños a la propiedad
              relacionados que resulten de cualquier uso de los servicios o
              estén relacionados en cualquier modo con los mismos, incluso si
              ROASAL S.A.S. ha advertido al usuario de la posibilidad de tales
              daños. La compañía no será responsable por cualquier daño,
              responsabilidad o pérdida que surja de:
            </p>

            <ul className="list-none text-gray-600 mb-6 leading-relaxed space-y-2">
              <li>
                5.3.1. Su uso o confianza en los servicios o su incapacidad para
                acceder o usar los servicios; o
              </li>
              <li>
                5.3.2. Cualquier transacción, vínculo comercial o relación
                contractual que se genere entre usuarios, proveedores o
                terceros, celebrada fuera del entorno de la plataforma de ROASAL
                S.A.S., incluso cuando el contacto inicial entre las partes haya
                sido facilitado por medio de la misma. En todos los casos,
                ROASAL S.A.S. no asume responsabilidad alguna por daños,
                pérdidas o incumplimientos derivados de dichas relaciones, aun
                cuando hubiere advertido sobre la posibilidad de tales
                contingencias.
              </li>
            </ul>

            <p className="text-gray-600 mb-6 leading-relaxed">
              ROASAL S.A.S. no será responsable por retraso o fallos en la
              prestación de servicios de usuarios que presten actividades por
              medio de la bolsa de trabajo de la aplicación, lo que incluye pero
              no se limita a conductores, choferes, dadores de carga y clientes.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              La compañía no controla, respalda ni asume responsabilidad por
              ningún contenido de usuario o de terceros disponible en la
              plataforma en punto (incluyendo información relacionada a la bolsa
              de trabajo proporcionada por usuarios tipo cliente). La compañía
              no puede y norepresenta ni garantiza que los servicios o
              servidores estén libres de virus u otros componentes dañinos.
            </p>

            <h3 className="text-2xl font-semibold text-gray-800 mb-4">
              5.4 Alcance bajo la ley 24.240 – deber de información
            </h3>
            <p className="text-gray-600 mb-6 leading-relaxed">
              En los excepcionales y/o extraordinarios casos en que los
              servicios de la plataforma sean utilizados por consumidores
              finales, conforme lo previsto por la ley 24.240, ROASAL S.A.S.
              declara que:
            </p>

            <ul className="list-disc pl-6 text-gray-600 mb-6 leading-relaxed space-y-2">
              <li>
                No ofrece ni garantiza servicios de transporte ni logística;
              </li>
              <li>
                No representa a los transportistas, dadores de carga ni
                conductores;
              </li>
              <li>
                La función de la plataforma es exclusivamente de intermediación
                y facilitación tecnológica.
              </li>
            </ul>

            <p className="text-gray-600 mb-6 leading-relaxed">
              No obstante, ROASAL S.A.S. se compromete a actuar de buena fe,
              brindar información clara sobre sus funciones, y ofrecer canales
              adecuados de atención para la resolución de consultas o conflictos
              que pudieran surgir en el uso de la plataforma.
            </p>
          </div>
        </section>

        {/* Section 6: Propiedad Intelectual */}
        <section className="py-20 px-8 bg-gray-50">
          <div className="max-w-6xl mx-auto">
            <h2 className="text-3xl font-bold text-gray-800 mb-8">
              6. Propiedad Intelectual
            </h2>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Todo contenido que el Usuario cargue en la plataforma (como fotos,
              calificaciones, comentarios o documentos) seguirá siendo de
              propiedad del usuario, pero al hacerlo, otorga a ROASAL S.A.S. una
              licencia gratuita, no exclusiva y por el tiempo de conservación de
              la inforación y datos proporcionados para reproducir, mostrar o
              adaptar dicho contenido dentro del funcionamiento de los
              Servicios, siempre en el marco legal vigente.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              La Compañía se compromete a no utilizar estos contenidos fuera del
              ámbito de la plataforma ni con fines que pudieran afectar la
              reputación o privacidad del Usuario sin consentimiento expreso.
            </p>
          </div>
        </section>

        {/* Section 7: Indemnización */}
        <section className="py-20 px-8 bg-white">
          <div className="max-w-6xl mx-auto">
            <h2 className="text-3xl font-bold text-gray-800 mb-8">
              7. Indemnización
            </h2>

            <p className="text-gray-600 mb-6 leading-relaxed">
              El Usuario acepta indemnizar y mantener indemne a ROASAL S.A.S.,
              sus afiliadas y sus respectivos oficiales, directores, empleados y
              agentes de reclamaciones, demandas, pérdidas, responsabilidades y
              gastos (incluyendo honorarios de abogados), que surjan de o en
              conexión con:
            </p>

            <ul className="list-none text-gray-600 mb-6 leading-relaxed space-y-4">
              <li>
                7.1. El uso indebido negligente o doloso de la plataforma o los
                servicios ofrecidos a través de la misma, incluyendo, sin
                limitación, el uso contrario a los fines propios del sistema,
                los presentes Términos y Condiciones, la ley aplicable o el
                orden público.
              </li>
              <li>
                7.2. El incumplimiento o violación o desconocimiento de
                cualquiera de las disposiciones de estos Términos y Condiciones.
              </li>
              <li>
                7.3 La comisión de actos ilícitos por parte del Usuario,
                incluyendo sin limitarse a:
                <ul className="list-disc pl-6 mt-2 space-y-2">
                  <li>
                    El uso fraudulento del sistema para contactar, pactar o
                    sustraer mercadería;
                  </li>
                  <li>
                    La utilización de la plataforma con intenciones maliciosas o
                    para fines distintos a los previstos;
                  </li>
                  <li>
                    El acceso no autorizado, intento de ingeniería inversa,
                    manipulación, alteración, desmantelamiento, vulneración o
                    copia del código fuente, infraestructura tecnológica,
                    arquitectura de base de datos o cualquier componente
                    funcional o estructural del software;
                  </li>
                  <li>
                    La afectación de la seguridad, confidencialidad, integridad
                    o disponibilidad de la información procesada o transmitida a
                    través de los sistemas de ROASAL S.A.S.
                  </li>
                </ul>
              </li>
              <li>
                7.4. Cualquier conflicto legal, contractual, administrativo o de
                cualquier otra naturaleza que tenga origen en relaciones
                comerciales o contractuales celebradas entre Usuarios fuera del
                entorno de la plataforma, aun cuando el primer contacto se haya
                producido por medio de ella.
              </li>
            </ul>

            <p className="text-gray-600 mb-6 leading-relaxed">
              El Usuario reconoce y acepta que cualquier acto de esta naturaleza
              podrá dar lugar a la interposición de acciones civiles y/o penales
              correspondientes, conforme la legislación vigente en la República
              Argentina, sin perjuicio de las acciones de resarcimiento que
              pudieran corresponder en favor de ROASAL S.A.S.
            </p>
          </div>
        </section>

        {/* Section 8: Ley Aplicable */}
        <section className="py-20 px-8 bg-gray-50">
          <div className="max-w-6xl mx-auto">
            <h2 className="text-3xl font-bold text-gray-800 mb-8">
              8. Ley Aplicable y Política de resolución de conflictos
            </h2>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Cualquier conflicto, controversia o reclamo que surja en relación
              con el uso de la plataforma, la interpretación, validez,
              cumplimiento o incumplimiento de los presentes Términos y
              Condiciones, será resuelto conforme a los procedimientos
              judiciales previstos por la legislación vigente en la Provincia de
              Córdoba, República Argentina, con expresa renuncia a cualquier
              otro fuero o jurisdicción que pudiera corresponder.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              A todos los efectos legales, las partes fijan como domicilio el
              correspondiente al domicilio legal de la Compañía, ubicado en
              calle Pinar del Río 3631, ciudad de Córdoba, provincia de Córdoba,
              Argentina.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              La vía judicial será el mecanismo aplicable ante cualquier
              desacuerdo entre la Compañía y el Usuario, sin perjuicio de que
              las partes puedan optar voluntariamente por instancias previas de
              conciliación o mediación en los términos de la ley local.
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              El presente contrato se rige por ley 24.653, legislación
              complementaria y Código Civil y Comercial de la Nación.
            </p>
          </div>
        </section>

        {/* Section 9: Notificaciones */}
        <section className="py-20 px-8 bg-white">
          <div className="max-w-6xl mx-auto">
            <h2 className="text-3xl font-bold text-gray-800 mb-8">
              9. Notificaciones
            </h2>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Todas las notificaciones que ROASAL S.A.S. deba realizar al
              Usuario en relación con el uso de la plataforma, modificaciones
              contractuales o cuestiones operativas, se considerarán válidamente
              realizadas si se envían a través de los medios de contacto
              registrados por el Usuario (correo electrónico, notificaciones
              dentro de la app o mensajes al teléfono informado).
            </p>

            <p className="text-gray-600 mb-6 leading-relaxed">
              Es responsabilidad del Usuario mantener sus datos de contacto
              actualizados. El uso continuado de la plataforma luego de recibir
              una notificación será interpretado como aceptación de la misma,
              salvo que se indique lo contrario por vía expresa.
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
              Si tiene alguna pregunta sobre estos Términos, contáctenos.
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
