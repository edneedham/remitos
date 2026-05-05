import type { Metadata } from 'next';
import PricingPlansSection from '../ui/components/website/PricingPlansSection';

export const metadata: Metadata = {
  title: 'Precios | En Punto',
  description:
    'Planes PyME, Empresa y Corporativo para la suscripción de En Punto.',
};

export default function PricingPage() {
  return <PricingPlansSection showCtas />;
}
