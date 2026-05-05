import type { Metadata } from 'next';
import type { ReactNode } from 'react';

export const metadata: Metadata = {
  title: 'Depósitos | En Punto',
  description: 'Administrá los depósitos configurados para tu empresa.',
};

export default function DepositosLayout({ children }: { children: ReactNode }) {
  return children;
}
