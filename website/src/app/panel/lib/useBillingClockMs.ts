'use client';

import { useLayoutEffect, useState } from 'react';

/** Millisecond clock for billing presentation (interval refresh; avoids `Date.now()` in render). */
export function useBillingClockMs(intervalMs = 60_000): number {
  const [ms, setMs] = useState(() => Date.now());
  useLayoutEffect(() => {
    const id = window.setInterval(() => setMs(Date.now()), intervalMs);
    return () => window.clearInterval(id);
  }, [intervalMs]);
  return ms;
}
