import type { Metadata } from 'next';
import TrialStartedClient from './TrialStartedClient';

export const metadata: Metadata = {
  title: 'Prueba activa | En Punto',
  description:
    'Descargá la app, iniciá sesión y escaneá tu primer remito con En Punto.',
};

export default function TrialStartedPage() {
  return <TrialStartedClient />;
}
