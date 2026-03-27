import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';
import { ContactFormSchema } from '../../lib/validations/contact';

const FORMSPREE_FORM_ID = process.env.FORMSPREE_FORM_ID;

export async function POST(request: NextRequest) {
  if (!FORMSPREE_FORM_ID) {
    return NextResponse.json(
      { success: false, message: 'Form not configured' },
      { status: 500 }
    );
  }

  try {
    const body = await request.json();

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

    return NextResponse.json({
      success: true,
      message: '¡Gracias! Hemos recibido tu mensaje y te contactaremos pronto.',
    });
  } catch (error) {
    console.error('/api/contact error:', error);
    return NextResponse.json(
      {
        success: false,
        message: 'Error al enviar el mensaje. Por favor, intenta de nuevo.',
      },
      { status: 500 },
    );
  }
}
