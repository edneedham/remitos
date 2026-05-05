import type { Metadata } from 'next';
import type { ReactNode } from 'react';

export const metadata: Metadata = {
  title: 'Dispositivos | En Punto',
  description: 'Listado y revocación de dispositivos registrados.',
};

export default function DispositivosLayout({
  children,
}: {
  children: ReactNode;
}) {
  return children;
}
