import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Mi cuenta | En Punto',
  description:
    'Administrá tu cuenta En Punto en la web: suscripción y datos de la empresa.',
};

export default function AccountLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
