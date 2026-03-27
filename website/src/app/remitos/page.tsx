'use client';

import { useState, useTransition } from 'react';
import { Mail, User, MessageSquare, Smartphone, CheckCircle, Package, Truck } from 'lucide-react';
import AndroidFrame from '../ui/components/website/AndroidFrame';

interface ContactFormState {
  errors?: {
    name?: string[];
    email?: string[];
    message?: string[];
  };
  message?: string;
  success: boolean;
}

function SimpleContactForm() {
  const [state, setState] = useState<ContactFormState>({ success: false });
  const [isPending, startTransition] = useTransition();

  const handleSubmit = async (formData: FormData) => {
    startTransition(async () => {
      try {
        const response = await fetch('/api/contact', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            name: formData.get('name'),
            email: formData.get('email'),
            message: formData.get('message'),
          }),
        });

        const data = await response.json();

        if (!response.ok) {
          setState({
            success: false,
            errors: data.errors,
            message: data.message || 'Error al enviar el mensaje. Por favor, intenta de nuevo.',
          });
          return;
        }

        setState({
          success: true,
          message: '¡Gracias! Hemos recibido tu mensaje y te contactaremos pronto.',
        });

        const form = document.querySelector('form') as HTMLFormElement;
        if (form) {
          form.reset();
        }
      } catch (error) {
        console.error('Error submitting contact form:', error);
        setState({
          success: false,
          message: 'Error al enviar el mensaje. Por favor, intenta de nuevo.',
        });
      }
    });
  };

  return (
    <form action={handleSubmit} className="space-y-6">
      {state?.message && (
        <div
          className={`p-4 rounded-lg ${
            state.success
              ? 'bg-green-50 border border-green-200 text-green-800'
              : 'bg-red-50 border border-red-200 text-red-800'
          }`}
        >
          {state.message}
        </div>
      )}

      <input type="hidden" name="subject" value="Inquery about Inlog app" />

      <div className="space-y-2">
        <label htmlFor="name" className="flex items-center gap-2 text-sm font-medium text-gray-700">
          <User className="h-4 w-4 text-gray-500" />
          Nombre completo
        </label>
        <input
          type="text"
          id="name"
          name="name"
          placeholder="Ingrese su nombre completo"
          className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors placeholder-gray-400"
          required
        />
      </div>

      <div className="space-y-2">
        <label htmlFor="email" className="flex items-center gap-2 text-sm font-medium text-gray-700">
          <Mail className="h-4 w-4 text-gray-500" />
          Correo electrónico
        </label>
        <input
          type="email"
          id="email"
          name="email"
          placeholder="ejemplo@correo.com"
          className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors placeholder-gray-400"
          required
        />
      </div>

      <div className="space-y-2">
        <label htmlFor="message" className="flex items-center gap-2 text-sm font-medium text-gray-700">
          <MessageSquare className="h-4 w-4 text-gray-500" />
          Mensaje
        </label>
        <textarea
          id="message"
          name="message"
          rows={4}
          placeholder="¿Estás interesado en la app de remitos? Cuéntanos sobre tus necesidades..."
          className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors placeholder-gray-400 resize-none"
          defaultValue="Estoy interesado en la app de remitos. Por favor, contactenme para más información."
          required
        />
      </div>

      <button
        type="submit"
        disabled={isPending}
        className="w-full px-4 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-600 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:bg-gray-400 disabled:cursor-not-allowed font-medium"
      >
        {isPending ? 'Enviando...' : 'Enviar Mensaje'}
      </button>
    </form>
  );
}

export default function RemitosPage() {
  return (
    <div className="flex flex-col min-h-screen">
      <main className="grow">
        {/* Hero Section */}
        <section className="py-16 px-8 bg-white border-b border-gray-200">
          <div className="max-w-6xl mx-auto">
            <div className="grid md:grid-cols-2 gap-12 items-center">
              <div>
                <div className="inline-flex items-center justify-center w-20 h-20 bg-gray-100 rounded-full mb-6">
                  <Smartphone className="w-10 h-10 text-emerald-700" />
                </div>
                <h1 className="text-4xl sm:text-5xl font-bold text-gray-900 mb-4">
                  App de Repartos
                </h1>
                <p className="text-xl text-gray-600 mb-8">
                  Aplicación Android para empresas de logística. Escanea remitos, 
                  gestiona repartos y haz seguimiento de tus entregas.
                </p>
              </div>
              <div className="flex justify-center">
                <AndroidFrame />
              </div>
            </div>
          </div>
        </section>

        {/* Benefits Section */}
        <section className="py-16 px-8 bg-gray-50">
          <div className="max-w-4xl mx-auto">
            <h2 className="text-3xl font-bold text-gray-900 mb-12 text-center">
              ¿Cómo te ayuda?
            </h2>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
              <div className="flex items-start">
                <div className="flex-shrink-0 w-12 h-12 bg-gray-200 rounded-lg flex items-center justify-center mr-4">
                  <Package className="w-6 h-6 text-emerald-700" />
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 mb-2">
                    Escanea remitos automáticamente
                  </h3>
                  <p className="text-gray-600">
                    Fotografía el remito y la app extrae los datos: CUIT, 
                    dirección, cantidad de bultos y más.
                  </p>
                </div>
              </div>

              <div className="flex items-start">
                <div className="flex-shrink-0 w-12 h-12 bg-gray-200 rounded-lg flex items-center justify-center mr-4">
                  <Truck className="w-6 h-6 text-emerald-700" />
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 mb-2">
                    Organiza tus repartos
                  </h3>
                  <p className="text-gray-600">
                    Crea listas de reparto, asigna remitos y controla 
                    qué falta entregar.
                  </p>
                </div>
              </div>

              <div className="flex items-start">
                <div className="flex-shrink-0 w-12 h-12 bg-gray-200 rounded-lg flex items-center justify-center mr-4">
                  <CheckCircle className="w-6 h-6 text-emerald-700" />
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 mb-2">
                    Controla cada entrega
                  </h3>
                  <p className="text-gray-600">
                    Sabés en qué estado está cada envío: en depósito, 
                    en tránsito, entregado o devuelto.
                  </p>
                </div>
              </div>

              <div className="flex items-start">
                <div className="flex-shrink-0 w-12 h-12 bg-gray-200 rounded-lg flex items-center justify-center mr-4">
                  <CheckCircle className="w-6 h-6 text-emerald-700" />
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 mb-2">
                    Sincronización en la nube
                  </h3>
                  <p className="text-gray-600">
                    Tus datos se sincronizan automáticamente. 
                    Accedé desde cualquier dispositivo.
                  </p>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Contact Section */}
        <section className="py-16 px-8 bg-gray-50">
          <div className="max-w-2xl mx-auto">
            <div className="text-center mb-8">
              <h2 className="text-3xl font-bold text-gray-800 mb-4">
                ¿Te interesa?
              </h2>
              <p className="text-gray-600">
                Contactanos para probar la app o discutir una solución personalizada.
              </p>
            </div>

            <div className="bg-white rounded-xl shadow-lg p-8">
              <SimpleContactForm />
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}
