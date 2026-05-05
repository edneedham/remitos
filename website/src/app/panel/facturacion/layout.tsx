import type { Metadata } from 'next';
import type { ReactNode } from 'react';

export const metadata: Metadata = {
  title: 'Facturación | En Punto',
  description: 'Facturación, plan y comprobantes de tu empresa.',
};

export default function BillingLayout({ children }: { children: ReactNode }) {
  return children;
}
