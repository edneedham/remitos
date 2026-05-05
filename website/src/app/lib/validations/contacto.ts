import { z } from 'zod';

export const ContactFormSchema = z.object({
  name: z.string().min(2, { message: 'Por favor, ingresa tu nombre.' }),
  email: z
    .string()
    .email({ message: 'Por favor, ingresa un correo electrónico válido.' }),
  message: z
    .string()
    .min(10, { message: 'Tu mensaje debe tener al menos 10 caracteres.' }),
});

export type ContactFormState = {
  errors?: {
    name?: string[];
    email?: string[];
    message?: string[];
  };
  message?: string;
  success: boolean;
};
