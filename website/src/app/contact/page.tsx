// Contact page
import ContactForm from '../ui/forms/contact';

export default function ContactPage() {
  return (
    <div className="min-h-screen bg-gray-50">
      <main className="max-w-4xl mx-auto py-12 px-4">
        <div className="text-center mb-12">
          <h1 className="text-4xl font-bold text-gray-900 mb-4">Contáctanos</h1>
          <p className="text-xl text-gray-600 max-w-2xl mx-auto">
            ¿Tienes alguna pregunta o comentario? Nos encantaría saber de ti.
          </p>
        </div>

        <div className="grid lg:grid-cols-3 gap-8">
          {/* Contact Form */}
          <div className="lg:col-span-2">
            <ContactForm />
          </div>

          {/* Contact Information */}
          <div className="space-y-6">
            <div className="bg-white rounded-lg shadow-lg border border-gray-200 p-6">
              <h3 className="text-lg font-semibold text-gray-900 mb-4">
                Información de Contacto
              </h3>
              <div className="space-y-4 text-sm text-gray-600">
                <div>
                  <p className="font-medium text-gray-900">Email</p>
                  <p>contacto@enpunto.com</p>
                </div>
                <div>
                  <p className="font-medium text-gray-900">
                    Horarios de atención
                  </p>
                  <p>Lunes a Viernes de 9:00 a 18:00</p>
                </div>
                <div>
                  <p className="font-medium text-gray-900">
                    Tiempo de respuesta
                  </p>
                  <p>Menos de 24 horas</p>
                </div>
              </div>
            </div>

            <div className="bg-gray-100 rounded-lg p-6">
              <h4 className="text-md font-semibold text-gray-900 mb-3">
                ¿Por qué contactarnos?
              </h4>
              <ul className="space-y-2 text-sm text-gray-700">
                <li>• Consultas sobre nuestros servicios</li>
                <li>• Soporte técnico</li>
                <li>• Sugerencias y feedback</li>
                <li>• Problemas con tu cuenta</li>
                <li>• Información sobre precios</li>
              </ul>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
