import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';
import {
  allowContactoSubmission,
  getContactoClientIP,
} from '../../lib/contactoRateLimit';
import { ContactFormSchema } from '../../lib/validations/contacto';
import { sendContactAckEmail } from '../../lib/sendContactAck';

const FORMSPREE_FORM_ID = process.env.FORMSPREE_FORM_ID;

/** Cap JSON body for the public contact endpoint (defense in depth). */
const MAX_CONTACT_JSON_BYTES = 64 * 1024;

export async function POST(request: NextRequest) {
  const clientIp = getContactoClientIP(request);
  if (!allowContactoSubmission(clientIp)) {
    return NextResponse.json(
      {
        success: false,
        message:
          'Demasiados intentos desde esta conexión. Probá de nuevo en unos minutos.',
      },
      {
        status: 429,
        headers: { 'Retry-After': '60' },
      }
    );
  }

  if (!FORMSPREE_FORM_ID) {
    return NextResponse.json(
      { success: false, message: 'Form not configured' },
      { status: 500 }
    );
  }

  try {
    const raw = await request.arrayBuffer();
    if (raw.byteLength > MAX_CONTACT_JSON_BYTES) {
      return NextResponse.json(
        {
          success: false,
          message: 'La solicitud es demasiado grande.',
        },
        { status: 413 },
      );
    }

    let body: unknown;
    try {
      body = JSON.parse(new TextDecoder().decode(raw)) as unknown;
    } catch {
      return NextResponse.json(
        { success: false, message: 'Cuerpo JSON inválido.' },
        { status: 400 },
      );
    }

    const validatedFields = ContactFormSchema.safeParse(body);
    if (!validatedFields.success) {
      return NextResponse.json(
        {
          success: false,
          errors: validatedFields.error.flatten().fieldErrors,
          message: 'Por favor, corrige los errores en el formulario.',
        },
        { status: 400 },
      );
    }

    const response = await fetch(
      `https://formspree.io/f/${FORMSPREE_FORM_ID}`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Accept: 'application/json',
        },
        body: JSON.stringify({
          name: validatedFields.data.name,
          email: validatedFields.data.email,
          message: validatedFields.data.message,
        }),
      }
    );

    if (!response.ok) {
      return NextResponse.json(
        { success: false, message: 'Error al enviar el mensaje.' },
        { status: response.status }
      );
    }

    try {
      await sendContactAckEmail(
        validatedFields.data.email,
        validatedFields.data.name
      );
    } catch (ackErr) {
      console.warn('/api/contacto: optional Resend ack failed', ackErr);
    }

    return NextResponse.json({
      success: true,
      message: '¡Gracias! Hemos recibido tu mensaje y te contactaremos pronto.',
    });
  } catch (error) {
    console.error('/api/contacto error:', error);
    return NextResponse.json(
      {
        success: false,
        message: 'Error al enviar el mensaje. Por favor, intenta de nuevo.',
      },
      { status: 500 },
    );
  }
}
