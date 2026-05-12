'use client';

import { useEffect } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import { PanelDashboardSkeleton } from './components/PanelSkeletons';
import { needsActivateSubscription } from './lib/activateSubscriptionGate';
import { usePanelBootstrap } from './lib/usePanelBootstrap';

export default function ActivateSubscriptionGate({
  children,
}: {
  children: React.ReactNode;
}) {
  const pathname = usePathname() ?? '';
  const router = useRouter();
  const { status, entitlement } = usePanelBootstrap();

  const pathExempt =
    pathname.startsWith('/panel/activar-suscripcion') ||
    pathname.startsWith('/panel/facturacion');

  useEffect(() => {
    if (
      pathname.startsWith('/panel/activar-suscripcion') ||
      pathname.startsWith('/panel/facturacion')
    ) {
      return;
    }
    if (status !== 'ready') return;
    if (needsActivateSubscription(entitlement)) {
      router.replace('/panel/activar-suscripcion');
    }
  }, [pathname, router, status, entitlement]);

  if (pathExempt) {
    return <>{children}</>;
  }

  if (status === 'loading') {
    return (
      <>
        <span className="sr-only">Cargando panel…</span>
        <PanelDashboardSkeleton />
      </>
    );
  }

  return <>{children}</>;
}
