'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { getApiBaseUrl } from '../../lib/apiUrl';
import { parseWebProfile } from '../../lib/apiSchemas/panelApi';
import {
  canAccessWebManagement,
  clearWebSession,
  fetchProfile,
  hasWebSession,
  refreshWebSession,
  type WebProfile,
} from '../../lib/webAuth';
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
  /** Re-run session refresh + profile load (e.g. window focus). Returns true if still ready. */
  refresh: () => Promise<boolean>;
};

/**
 * Shared panel gate: API URL, session hint/tokens, refresh, profile, and web-management role.
 * Redirects to /ingresar when unauthenticated or forbidden.
 */
export function usePanelBootstrap(): UsePanelBootstrapResult {
  const routerRef = useRouterRef();
  const [status, setStatus] = useState<PanelBootstrapStatus>('loading');
  const [profile, setProfile] = useState<WebProfile | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const mounted = useRef(true);

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

  const runBootstrap = useCallback(async (): Promise<{
    status: PanelBootstrapStatus;
    profile: WebProfile | null;
    error: string | null;
  }> => {
    const api = getApiBaseUrl();
    if (!api) {
      return {
        status: 'config_error',
        profile: null,
        error: CONFIG_ERROR_MSG,
      };
    }
    if (!hasWebSession()) {
      return { status: 'unauthenticated', profile: null, error: null };
    }

    await refreshWebSession();
    const raw = await fetchProfile();
    const p = raw ? parseWebProfile(raw) : null;
    if (!p || !canAccessWebManagement(p.role)) {
      return { status: 'forbidden', profile: null, error: null };
    }
    return { status: 'ready', profile: p, error: null };
  }, []);

  const applyResult = useCallback(
    (r: {
      status: PanelBootstrapStatus;
      profile: WebProfile | null;
      error: string | null;
    }) => {
      if (!mounted.current) return;
      setStatus(r.status);
      setProfile(r.profile);
      setErrorMessage(r.error);
    },
    [],
  );

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
  }, [runBootstrap, applyResult]);

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
  }, [runBootstrap, applyResult]);

  return { status, profile, errorMessage, refresh };
}
