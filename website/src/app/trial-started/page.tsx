import { redirect } from 'next/navigation';

/** Alias for legacy links and emails; canonical route is `/prueba-iniciada`. */
export default function TrialStartedRedirectPage() {
  redirect('/prueba-iniciada');
}
