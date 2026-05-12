'use client';

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import { getApiBaseUrl } from '../../lib/apiUrl';
import { parseWebProfile } from '../../lib/apiSchemas/panelApi';
import {
  canAccessWebManagement,
  clearWebSession,
  fetchProfile,
  fetchWithWebAuth,
  hasWebSession,
  type WebProfile,
} from '../../lib/webAuth';
import type { Entitlement } from './entitlementTypes';
import { useRouterRef } from './useRouterRef';

export type PanelBootstrapStatus =
  | 'loading'
  | 'ready'
  | 'unauthenticated'
  | 'forbidden'
  | 'config_error';

const CONFIG_ERROR_MSG =
  'Falta configurar NEXT_PUBLIC_API_URL (URL del servidor de la API).';

export type UsePanelBootstrapResult = {
  status: PanelBootstrapStatus;
  profile: WebProfile | null;
  errorMessage: string | null;
  /** Entitlement from the same bootstrap round-trip as profile (may be null if the request failed). */
  entitlement: Entitlement | null;
  /** Re-run session refresh, profile, and entitlement (e.g. window focus). Returns true if still ready. */
  refresh: () => Promise<boolean>;
};

type BootstrapRunResult = {
  status: PanelBootstrapStatus;
  profile: WebProfile | null;
  error: string | null;
  entitlement: Entitlement | null;
};

const PanelBootstrapContext = createContext<UsePanelBootstrapResult | null>(
  null,
);

async function runBootstrap(): Promise<BootstrapRunResult> {
  const api = getApiBaseUrl();
  if (!api) {
    return {
      status: 'config_error',
      profile: null,
      error: CONFIG_ERROR_MSG,
      entitlement: null,
    };
  }
  if (!hasWebSession()) {
    return {
      status: 'unauthenticated',
      profile: null,
      error: null,
      entitlement: null,
    };
  }

  /** No proactive refresh: `fetchWithWebAuth` / `fetchProfile` refresh on 401 (see single-flight `refreshWebSession`). */
  const [raw, entRes] = await Promise.all([
    fetchProfile(),
    fetchWithWebAuth('/auth/me/entitlement'),
  ]);

  let entitlement: Entitlement | null = null;
  if (entRes.ok) {
    try {
      entitlement = (await entRes.json()) as Entitlement;
    } catch {
      entitlement = null;
    }
  }

  const p = raw ? parseWebProfile(raw) : null;
  if (!p || !canAccessWebManagement(p.role)) {
    return {
      status: 'forbidden',
      profile: null,
      error: null,
      entitlement: null,
    };
  }
  return {
    status: 'ready',
    profile: p,
    error: null,
    entitlement,
  };
}

export function PanelBootstrapProvider({ children }: { children: ReactNode }) {
  const routerRef = useRouterRef();
  const [status, setStatus] = useState<PanelBootstrapStatus>('loading');
  const [profile, setProfile] = useState<WebProfile | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [entitlement, setEntitlement] = useState<Entitlement | null>(null);
  const mounted = useRef(true);

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  const applyResult = useCallback((r: BootstrapRunResult) => {
    if (!mounted.current) return;
    setStatus(r.status);
    setProfile(r.profile);
    setErrorMessage(r.error);
    setEntitlement(r.entitlement);
  }, []);

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      const r = await runBootstrap();
      if (cancelled) return;
      applyResult(r);
    })();
    return () => {
      cancelled = true;
    };
  }, [applyResult]);

  useEffect(() => {
    if (status === 'unauthenticated') {
      routerRef.current.replace('/ingresar');
      return;
    }
    if (status === 'forbidden') {
      clearWebSession();
      routerRef.current.replace('/ingresar');
    }
  }, [status, routerRef]);

  const refresh = useCallback(async () => {
    const r = await runBootstrap();
    applyResult(r);
    return r.status === 'ready';
  }, [applyResult]);

  const value: UsePanelBootstrapResult = {
    status,
    profile,
    errorMessage,
    entitlement,
    refresh,
  };

  return (
    <PanelBootstrapContext.Provider value={value}>
      {children}
    </PanelBootstrapContext.Provider>
  );
}

/**
 * Panel session + profile + entitlement (single bootstrap per shell mount).
 * Must be used under {@link PanelBootstrapProvider}.
 */
export function usePanelBootstrap(): UsePanelBootstrapResult {
  const ctx = useContext(PanelBootstrapContext);
  if (!ctx) {
    throw new Error(
      'usePanelBootstrap must be used within PanelBootstrapProvider',
    );
  }
  return ctx;
}
