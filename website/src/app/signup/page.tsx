import type { Metadata } from 'next';
import SignupGate from './SignupGate';

export const metadata: Metadata = {
  title: 'Registro | En Punto',
  description: 'Creá tu cuenta en En Punto.',
};

export default function SignupPage() {
  return <SignupGate />;
}
