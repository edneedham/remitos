'use client';

import { useEffect, useState } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import { fetchWithWebAuth, hasWebSession } from '../lib/webAuth';
import { needsActivateSubscription } from './lib/activateSubscriptionGate';
import type { Entitlement } from './lib/entitlementTypes';

export default function ActivateSubscriptionGate({
  children,
}: {
  children: React.ReactNode;
}) {
  const pathname = usePathname();
  const router = useRouter();
  const [ready, setReady] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function run() {
      if (
        pathname?.startsWith('/dashboard/activate-subscription') ||
        pathname?.startsWith('/dashboard/billing')
      ) {
        if (!cancelled) setReady(true);
        return;
      }
      if (!hasWebSession()) {
        if (!cancelled) setReady(true);
        return;
      }

      const res = await fetchWithWebAuth('/auth/me/entitlement');
      if (cancelled) return;
      if (!res.ok) {
        setReady(true);
        return;
      }
      const ent = (await res.json()) as Entitlement;
      if (needsActivateSubscription(ent)) {
        router.replace('/dashboard/activate-subscription');
        return;
      }
      setReady(true);
    }

    void run();
    return () => {
      cancelled = true;
    };
  }, [pathname, router]);

  if (
    !ready &&
    pathname &&
    !pathname.startsWith('/dashboard/activate-subscription') &&
    !pathname.startsWith('/dashboard/billing')
  ) {
    return (
      <div className="flex min-h-[40vh] items-center justify-center p-8 text-sm text-gray-600">
        Cargando…
      </div>
    );
  }

  return <>{children}</>;
}
