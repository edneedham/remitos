'use client';

import { useState, useTransition } from 'react';
import { Mail, User, MessageSquare } from 'lucide-react';
import { ContactFormState } from '../../lib/validations/contact';
import SubmitArea from '../components/shared/Submit';

export default function ContactForm() {
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
            message:
              data.message ||
              'Error al enviar el mensaje. Por favor, intenta de nuevo.',
          });
          return;
        }

        setState({
          success: true,
          message:
            data.message ||
            '¡Gracias! Hemos recibido tu mensaje y te contactaremos pronto.',
        });

        // Reset form on success
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
    <form
      action={handleSubmit}
      className="w-full bg-white rounded-lg shadow-lg border border-gray-200"
    >
      {/* Header */}
      <div className="px-6 py-5 border-b border-gray-200">
        <div className="flex items-center gap-3">
          <div className="p-2 bg-blue-100 rounded-lg">
            <Mail className="h-6 w-6 text-blue-600" />
          </div>
          <div>
            <h2 className="text-2xl font-bold text-gray-900">
              Envíanos un mensaje
            </h2>
            <p className="text-sm text-gray-600">
              Complete el formulario y nos pondremos en contacto contigo
            </p>
          </div>
        </div>
      </div>

      {/* Form Content */}
      <div className="p-8 space-y-6">
        {/* Success/Error Messages */}
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

        {/* Name Field */}
        <div className="space-y-2">
          <label
            htmlFor="name"
            className="flex items-center gap-2 text-sm font-medium text-gray-700"
          >
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
            aria-describedby="name-error"
          />
          {state?.errors?.name && (
            <div id="name-error" className="mt-2 text-sm text-red-500">
              {state.errors.name.map((error, index) => (
                <p key={index}>{error}</p>
              ))}
            </div>
          )}
        </div>

        {/* Email Field */}
        <div className="space-y-2">
          <label
            htmlFor="email"
            className="flex items-center gap-2 text-sm font-medium text-gray-700"
          >
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
            aria-describedby="email-error"
          />
          {state?.errors?.email && (
            <div id="email-error" className="mt-2 text-sm text-red-500">
              {state.errors.email.map((error, index) => (
                <p key={index}>{error}</p>
              ))}
            </div>
          )}
        </div>

        {/* Message Field */}
        <div className="space-y-2">
          <label
            htmlFor="message"
            className="flex items-center gap-2 text-sm font-medium text-gray-700"
          >
            <MessageSquare className="h-4 w-4 text-gray-500" />
            Mensaje
          </label>
          <textarea
            id="message"
            name="message"
            rows={6}
            placeholder="Cuéntenos sobre sus necesidades logísticas, consultas o cómo podemos ayudarle..."
            className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors placeholder-gray-400 resize-none"
            required
            aria-describedby="message-error"
          />
          {state?.errors?.message && (
            <div id="message-error" className="mt-2 text-sm text-red-500">
              {state.errors.message.map((error, index) => (
                <p key={index}>{error}</p>
              ))}
            </div>
          )}
        </div>

        {/* Submit Button */}
        <div className="pt-6">
          <SubmitArea
            buttonText="Enviar Mensaje"
            submittingText="Enviando mensaje..."
            isSubmitting={isPending}
          />
        </div>
      </div>
    </form>
  );
}
