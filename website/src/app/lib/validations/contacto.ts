import { z } from 'zod';

export const ContactFormSchema = z.object({
  name: z.string().min(2, { message: 'El nombre debe tener al menos 2 caracteres.' }),
  email: z
    .string()
    .email({ message: 'Ingresá un correo electrónico válido.' }),
  message: z
    .string()
    .min(10, { message: 'El mensaje debe tener al menos 10 caracteres.' }),
});

export type ContactFieldErrors = {
  name?: string;
  email?: string;
  message?: string;
};
