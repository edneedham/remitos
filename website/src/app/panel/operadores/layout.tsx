import type { Metadata } from 'next';
import type { ReactNode } from 'react';

export const metadata: Metadata = {
  title: 'Operadores | En Punto',
  description: 'Administrá los operadores que usan la app en tus depósitos.',
};

export default function OperadoresLayout({ children }: { children: ReactNode }) {
  return children;
}
