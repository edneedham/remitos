import type { Metadata } from 'next';
import SignupGate from './SignupGate';

export const metadata: Metadata = {
  title: 'Registro | En Punto',
  description: 'Probá 7 días gratis con En Punto.',
};

export default function SignupPage() {
  return <SignupGate />;
}
