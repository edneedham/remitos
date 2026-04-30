import { redirect } from 'next/navigation';

/**
 * Legacy URL: mobile signup now uses the same two-step flow as /signup.
 */
export default function MobileSignupRedirect() {
  redirect('/signup');
}
