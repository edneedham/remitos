'use client';

import { useRouter } from 'next/navigation';
import { useLayoutEffect, useRef } from 'react';

/**
 * Stable ref to next/navigation router so effects do not re-run when the router
 * object identity changes between renders (e.g. test doubles returning a fresh object).
 */
export function useRouterRef() {
  const router = useRouter();
  const ref = useRef(router);
  useLayoutEffect(() => {
    ref.current = router;
  }, [router]);
  return ref;
}
