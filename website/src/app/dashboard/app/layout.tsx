import type { Metadata } from 'next';
import type { ReactNode } from 'react';

export const metadata: Metadata = {
  title: 'Aplicación | En Punto',
  description: 'Descarga APK y transferencia de sesión al teléfono.',
};

export default function ApplicationLayout({ children }: { children: ReactNode }) {
  return children;
}
