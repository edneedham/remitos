import type { Metadata } from 'next';
import DashboardShell from './DashboardShell';

export const metadata: Metadata = {
  title: 'Panel | En Punto',
  description:
    'Panel En Punto: uso de documentos, depósitos y acceso a facturación.',
};

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return <DashboardShell>{children}</DashboardShell>;
}
