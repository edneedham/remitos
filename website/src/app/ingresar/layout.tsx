import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Iniciar sesión | En Punto',
  description:
    'Iniciá sesión en el sitio para administrar tu cuenta. Usá la app en el depósito para operar.',
};

export default function LoginLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
