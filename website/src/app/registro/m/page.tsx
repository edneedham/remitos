import { redirect } from 'next/navigation';
import { isWaitlistOnly } from '../../lib/waitlistOnly';

/**
 * Legacy URL: mobile signup now uses the same two-step flow as /registro.
 */
export default function MobileSignupRedirect() {
  redirect(isWaitlistOnly() ? '/lista-de-espera' : '/registro');
}
