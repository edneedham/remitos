'use client';

import Link from 'next/link';
import { useState } from 'react';
import Button from '../components/shared/Button';
import StatusBanner from '../components/shared/StatusBanner';
import TextAreaField from '../components/shared/TextAreaField';
import TextField from '../components/shared/TextField';
import {
  ContactFormSchema,
  type ContactFieldErrors,
} from '../../lib/validations/contacto';

type ContactField = keyof ContactFieldErrors;

const CONTACT_FIELD_ORDER: ContactField[] = ['name', 'email', 'message'];

const CONTACT_FIELD_DOM_ID: Record<ContactField, string> = {
  name: 'contact-name',
  email: 'contact-email',
  message: 'contact-message',
};

function scrollAndFocusById(elementId: string) {
  const el = document.getElementById(elementId);
  if (!el) return;
  el.scrollIntoView({ behavior: 'smooth', block: 'center' });
  window.requestAnimationFrame(() => {
    if (el instanceof HTMLElement && typeof el.focus === 'function') {
      const tag = el.tagName.toLowerCase();
      if (
        tag === 'input' ||
        tag === 'select' ||
        tag === 'textarea' ||
        tag === 'button'
      ) {
        el.focus({ preventScroll: true });
      }
    }
  });
}

function scrollFirstContactError(errs: ContactFieldErrors) {
  for (const key of CONTACT_FIELD_ORDER) {
    if (errs[key]) {
      scrollAndFocusById(CONTACT_FIELD_DOM_ID[key]);
      break;
    }
  }
}

function mapApiFieldErrors(
  raw: Record<string, string[] | undefined> | undefined,
): ContactFieldErrors {
  if (!raw) return {};
  const pick = (k: ContactField) =>
    Array.isArray(raw[k]) && raw[k]![0] ? raw[k]![0] : undefined;
  return {
    name: pick('name'),
    email: pick('email'),
    message: pick('message'),
  };
}

export default function ContactForm() {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [message, setMessage] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const [formSuccess, setFormSuccess] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<ContactFieldErrors>({});
  const [loading, setLoading] = useState(false);

  const clearFieldError = (field: ContactField) => {
    setFieldErrors((prev) => {
      if (!prev[field]) return prev;
      const next = { ...prev };
      delete next[field];
      return next;
    });
  };

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setFormError(null);
    setFormSuccess(null);

    const payload = {
      name: name.trim(),
      email: email.trim(),
      message: message.trim(),
    };

    const clientParsed = ContactFormSchema.safeParse(payload);
    if (!clientParsed.success) {
      const flat = clientParsed.error.flatten().fieldErrors;
      const next: ContactFieldErrors = {
        name: flat.name?.[0],
        email: flat.email?.[0],
        message: flat.message?.[0],
      };
      setFieldErrors(next);
      setFormError('Revisá los datos del formulario.');
      scrollFirstContactError(next);
      return;
    }

    setFieldErrors({});
    setLoading(true);
    try {
      const response = await fetch('/api/contacto', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(clientParsed.data),
      });

      const data = (await response.json().catch(() => ({}))) as {
        success?: boolean;
        message?: string;
        errors?: Record<string, string[] | undefined>;
      };

      if (!response.ok) {
        const apiErrs = mapApiFieldErrors(data.errors);
        setFieldErrors(apiErrs);
        setFormError(
          typeof data.message === 'string' && data.message
            ? data.message
            : 'No se pudo enviar el mensaje. Probá de nuevo.',
        );
        scrollFirstContactError(apiErrs);
        return;
      }

      setFormSuccess(
        typeof data.message === 'string' && data.message
          ? data.message
          : '¡Gracias! Recibimos tu mensaje y te vamos a contactar a la brevedad.',
      );
      setName('');
      setEmail('');
      setMessage('');
    } catch {
      setFormError('Error de red. Verificá la conexión e intentá de nuevo.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-[60vh] bg-gray-50 px-4 py-10 lg:py-14">
      <div className="mx-auto w-full max-w-md">
        <section className="flex flex-col rounded-2xl border border-gray-200 bg-white px-5 py-7 shadow-sm sm:px-6 sm:py-8 lg:py-10">
          <header className="space-y-2 text-left sm:space-y-3">
            <h1 className="text-[1.625rem] font-bold leading-snug tracking-tight text-gray-900 sm:text-3xl sm:leading-tight">
              Contáctanos
            </h1>
            <p className="text-base leading-relaxed text-gray-600 sm:text-sm sm:leading-relaxed">
              Si tenés una consulta o comentario, escribinos y te respondemos a
              la brevedad.
            </p>
          </header>

          <form
            onSubmit={(e) => void handleSubmit(e)}
            className="mt-6 space-y-5 sm:mt-8"
            noValidate
          >
            {formSuccess && (
              <StatusBanner variant="success">{formSuccess}</StatusBanner>
            )}
            {formError && !formSuccess && (
              <StatusBanner variant="error">{formError}</StatusBanner>
            )}

            <TextField
              id={CONTACT_FIELD_DOM_ID.name}
              label="Nombre completo"
              value={name}
              onChange={(ev) => {
                setName(ev.target.value);
                clearFieldError('name');
                if (formError) setFormError(null);
                if (formSuccess) setFormSuccess(null);
              }}
              autoComplete="name"
              aria-required
              error={fieldErrors.name}
              placeholder="Tu nombre"
            />

            <TextField
              id={CONTACT_FIELD_DOM_ID.email}
              label="Correo electrónico"
              type="email"
              value={email}
              onChange={(ev) => {
                setEmail(ev.target.value);
                clearFieldError('email');
                if (formError) setFormError(null);
                if (formSuccess) setFormSuccess(null);
              }}
              autoComplete="email"
              aria-required
              error={fieldErrors.email}
              placeholder="ejemplo@correo.com"
            />

            <TextAreaField
              id={CONTACT_FIELD_DOM_ID.message}
              label="Mensaje"
              value={message}
              onChange={(ev) => {
                setMessage(ev.target.value);
                clearFieldError('message');
                if (formError) setFormError(null);
                if (formSuccess) setFormSuccess(null);
              }}
              aria-required
              error={fieldErrors.message}
              rows={6}
              placeholder="Contanos en qué podemos ayudarte…"
            />

            <Button type="submit" isLoading={loading} variant="primary">
              Enviar mensaje
            </Button>
          </form>

          <p className="mt-6 border-t border-gray-100 pt-6 text-center text-sm text-gray-600 sm:mt-8 sm:pt-8">
            <Link
              href="/"
              className="font-medium text-blue-600 hover:text-blue-700 hover:underline"
            >
              Ir al inicio
            </Link>
          </p>
        </section>
      </div>
    </div>
  );
}
