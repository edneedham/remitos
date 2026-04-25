'use client';

import { useLayoutEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { hasWebSession } from './lib/webAuth';

/** When a web session exists, sends the user to /account instead of the marketing home. */
export default function HomeGate({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const [showMarketing, setShowMarketing] = useState(true);

  useLayoutEffect(() => {
    if (hasWebSession()) {
      setShowMarketing(false);
      router.replace('/account');
    }
  }, [router]);

  if (!showMarketing) {
    return null;
  }

  return <>{children}</>;
}
