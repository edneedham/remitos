import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Descargar aplicación | En Punto',
  description:
    'Descargá la aplicación Android En Punto para usar en depósito (planes de prueba o pagos activos).',
};

export default function DownloadLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return children;
}
