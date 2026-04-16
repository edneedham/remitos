'use client';

import { useState } from 'react';
import { ArrowRight } from 'lucide-react';

export default function MobileSignupForm() {
  const [submitted, setSubmitted] = useState(false);

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setSubmitted(true);
  };

  if (submitted) {
    return (
      <div className="rounded-xl bg-white border border-gray-200 p-8 text-center shadow-sm">
        <h2 className="text-xl font-semibold text-gray-900 mb-2">
          Gracias por tu interés
        </h2>
        <p className="text-gray-600">
          Estamos finalizando el registro online. Te vamos a avisar cuando
          puedas completar la cuenta.
        </p>
      </div>
    );
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="rounded-xl bg-white border border-gray-200 p-6 shadow-sm space-y-5"
    >
      <div>
        <label
          htmlFor="signup-email"
          className="block text-sm font-medium text-gray-700 mb-1"
        >
          Correo electrónico
        </label>
        <input
          id="signup-email"
          name="email"
          type="email"
          required
          autoComplete="email"
          placeholder="vos@ejemplo.com"
          className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        />
      </div>
      <div>
        <label
          htmlFor="signup-name"
          className="block text-sm font-medium text-gray-700 mb-1"
        >
          Nombre (opcional)
        </label>
        <input
          id="signup-name"
          name="name"
          type="text"
          autoComplete="name"
          placeholder="Tu nombre"
          className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        />
      </div>
      <button
        type="submit"
        className="w-full inline-flex items-center justify-center px-4 py-3 bg-blue-600 text-white font-semibold rounded-lg hover:bg-blue-700 transition-colors"
      >
        Continuar
        <ArrowRight className="ml-2 h-4 w-4" />
      </button>
    </form>
  );
}
