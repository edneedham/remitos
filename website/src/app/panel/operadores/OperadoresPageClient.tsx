'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Loader2 } from 'lucide-react';
import {
  OperadoresBootstrapSkeleton,
  OperadoresListSkeleton,
  PanelEntitlementIntroSkeleton,
} from '../components/PanelSkeletons';
import { getApiBaseUrl } from '../../lib/apiUrl';
import {
  canAccessWebManagement,
  canManageOperators,
  clearWebSession,
  fetchProfile,
  fetchWithWebAuth,
  hasWebSession,
  postWithWebAuth,
  putWithWebAuth,
  refreshWebSession,
  type WebProfile,
} from '../../lib/webAuth';
import type { Entitlement } from '../lib/entitlementTypes';

type Operator = {
  id: string;
  email?: string;
  username?: string;
  role: string;
  status: string;
  created_at: string;
};

export default function OperadoresPageClient() {
  const router = useRouter();
  const [profileResolved, setProfileResolved] = useState(false);
  const [entitlementLoading, setEntitlementLoading] = useState(true);
  const [operatorsLoading, setOperatorsLoading] = useState(false);
  const [profile, setProfile] = useState<WebProfile | null>(null);
  const [entitlement, setEntitlement] = useState<Entitlement | null>(null);
  const [operators, setOperators] = useState<Operator[]>([]);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [formUsername, setFormUsername] = useState('');
  const [formPassword, setFormPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [pwdById, setPwdById] = useState<Record<string, string>>({});
  const [busyId, setBusyId] = useState<string | null>(null);

  const canManage = profile ? canManageOperators(profile.role) : false;

  useEffect(() => {
    if (!hasWebSession()) {
      router.replace('/ingresar');
      return;
    }

    let cancelled = false;

    async function load() {
      const api = getApiBaseUrl();
      if (!api) {
        setLoadError(
          'Falta configurar NEXT_PUBLIC_API_URL (URL del servidor de la API).',
        );
        setProfileResolved(true);
        setEntitlementLoading(false);
        return;
      }

      await refreshWebSession();
      const userProfile = await fetchProfile();
      if (cancelled) return;
      if (!userProfile || !canAccessWebManagement(userProfile.role)) {
        clearWebSession();
        router.replace('/ingresar');
        return;
      }
      setProfile(userProfile);
      setProfileResolved(true);

      const entRes = await fetchWithWebAuth('/auth/me/entitlement');
      if (cancelled) return;
      setEntitlementLoading(false);
      if (entRes.ok) {
        setEntitlement((await entRes.json()) as Entitlement);
      }

      if (!canManageOperators(userProfile.role)) {
        return;
      }

      setOperatorsLoading(true);
      const res = await fetchWithWebAuth('/admin/operadores');
      if (cancelled) return;
      if (res.status === 401) {
        clearWebSession();
        router.replace('/ingresar');
        return;
      }
      if (!res.ok) {
        setLoadError(
          'No se pudieron cargar los operadores. Probá de nuevo más tarde.',
        );
        setOperatorsLoading(false);
        return;
      }
      const list = (await res.json()) as Operator[];
      setOperators(Array.isArray(list) ? list : []);
      setOperatorsLoading(false);
    }

    void load();
    return () => {
      cancelled = true;
    };
  }, [router]);

  const refreshOperators = async () => {
    const res = await fetchWithWebAuth('/admin/operadores');
    if (res.ok) {
      const list = (await res.json()) as Operator[];
      setOperators(Array.isArray(list) ? list : []);
    }
    const entRes = await fetchWithWebAuth('/auth/me/entitlement');
    if (entRes.ok) {
      setEntitlement((await entRes.json()) as Entitlement);
    }
  };

  const onCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    const username = formUsername.trim();
    if (username.length < 3) {
      setActionError('El nombre de usuario debe tener al menos 3 caracteres.');
      return;
    }
    setSubmitting(true);
    setActionError(null);
    const body: { username: string; password: string } = {
      username,
      password: formPassword,
    };
    const res = await postWithWebAuth('/admin/operadores', body);
    setSubmitting(false);
    if (res.ok) {
      setFormUsername('');
      setFormPassword('');
      await refreshOperators();
      return;
    }
    const data = (await res.json().catch(() => ({}))) as { message?: string };
    setActionError(data.message || 'No se pudo crear el operador.');
  };

  const toggleStatus = async (op: Operator) => {
    const next = op.status === 'active' ? 'suspended' : 'active';
    const ok = window.confirm(
      next === 'suspended'
        ? `¿Suspender a ${op.username ?? op.email ?? 'este operador'}?`
        : `¿Reactivar a ${op.username ?? op.email ?? 'este operador'}?`,
    );
    if (!ok) return;
    setBusyId(op.id);
    setActionError(null);
    const res = await putWithWebAuth(`/admin/operadores/${op.id}/status`, {
      status: next,
    });
    setBusyId(null);
    if (res.ok) {
      await refreshOperators();
      return;
    }
    const data = (await res.json().catch(() => ({}))) as { message?: string };
    setActionError(data.message || 'No se pudo actualizar el estado.');
  };

  const savePassword = async (op: Operator) => {
    const pwd = (pwdById[op.id] ?? '').trim();
    if (pwd.length < 8) {
      setActionError('La contraseña debe tener al menos 8 caracteres.');
      return;
    }
    setBusyId(op.id);
    setActionError(null);
    const res = await putWithWebAuth(`/admin/operadores/${op.id}/password`, {
      password: pwd,
    });
    setBusyId(null);
    if (res.ok) {
      setPwdById((prev) => ({ ...prev, [op.id]: '' }));
      return;
    }
    const data = (await res.json().catch(() => ({}))) as { message?: string };
    setActionError(data.message || 'No se pudo cambiar la contraseña.');
  };

  const maxUsers = entitlement?.max_users;
  const userCount = entitlement?.user_count;
  const atUserCap =
    typeof maxUsers === 'number' &&
    typeof userCount === 'number' &&
    userCount >= maxUsers;

  return (
    <div className="bg-gray-50 px-4 pb-12 pt-6">
      <div className="mx-auto max-w-5xl space-y-6">
        <header className="space-y-2">
          <h1 className="text-3xl font-bold tracking-tight text-gray-900">
            Operadores
          </h1>
          {!profileResolved || (entitlementLoading && !loadError) ? (
            <PanelEntitlementIntroSkeleton />
          ) : (
            <p className="text-base leading-relaxed text-gray-600">
              Los operadores inician sesión en la app En Punto en cada depósito.{' '}
              {typeof maxUsers === 'number' ? (
                <>
                  Tu plan permite hasta{' '}
                  <span className="font-semibold">{maxUsers}</span> usuarios en
                  total (
                  {typeof userCount === 'number' ? userCount : '—'} en uso,
                  incluye titular y operadores).
                </>
              ) : (
                <>Gestioná cuentas de operadores para tu empresa.</>
              )}
            </p>
          )}
        </header>

        {loadError ? (
          <div
            className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800"
            role="alert"
          >
            {loadError}
          </div>
        ) : null}

        {!profileResolved && !loadError ? (
          <OperadoresBootstrapSkeleton />
        ) : null}

        {!canManage && profile && profileResolved ? (
          <div
            className="rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-950"
            role="status"
          >
            Tu rol no puede administrar operadores. Solo titulares y administradores de depósito
            pueden crear o editar operadores.
          </div>
        ) : null}

        {canManage && profileResolved ? (
          <>
            {actionError ? (
              <div
                className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800"
                role="alert"
              >
                {actionError}
              </div>
            ) : null}

            <div className="flex flex-wrap items-center gap-3">
              <p className="text-sm text-gray-600">
                {atUserCap ? (
                  <>
                    Alcanzaste el cupo de usuarios.{' '}
                    <Link
                      href="/panel/facturacion/mejorar-plan"
                      className="font-semibold text-blue-700 underline"
                    >
                      Mejorar plan
                    </Link>{' '}
                    o suspendé un operador existente.
                  </>
                ) : null}
              </p>
            </div>

            <form
              onSubmit={onCreate}
              className="space-y-4 rounded-xl border border-gray-200 bg-white p-5 shadow-sm"
              aria-labelledby="new-operator-heading"
            >
              <h2
                id="new-operator-heading"
                className="text-base font-semibold text-gray-900"
              >
                Nuevo operador
              </h2>
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <label className="block text-sm">
                  <span className="font-medium text-gray-800">Usuario</span>
                  <input
                    type="text"
                    required
                    minLength={3}
                    value={formUsername}
                    onChange={(e) => setFormUsername(e.target.value)}
                    className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
                    autoCapitalize="none"
                    autoComplete="off"
                  />
                </label>
                <label className="block text-sm">
                  <span className="font-medium text-gray-800">Contraseña inicial</span>
                  <input
                    type="password"
                    required
                    minLength={8}
                    value={formPassword}
                    onChange={(e) => setFormPassword(e.target.value)}
                    className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
                    autoComplete="new-password"
                  />
                </label>
              </div>
              <button
                type="submit"
                disabled={submitting || atUserCap}
                className="inline-flex items-center justify-center rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-gray-300"
              >
                {submitting ? (
                  <Loader2 className="h-4 w-4 animate-spin" aria-hidden />
                ) : (
                  'Crear operador'
                )}
              </button>
            </form>

            {operatorsLoading && !loadError ? (
              <OperadoresListSkeleton />
            ) : (
              <section
                className="rounded-xl border border-gray-200 bg-white shadow-sm"
                aria-label="Lista de operadores"
              >
                {operators.length === 0 ? (
                  <p className="p-6 text-sm text-gray-600">
                    Todavía no hay operadores. Creá uno para que puedan iniciar sesión en la app.
                  </p>
                ) : (
                  <ul className="divide-y divide-gray-100">
                    {operators.map((op) => {
                      const busy = busyId === op.id;
                      return (
                        <li
                          key={op.id}
                          className="space-y-3 px-5 py-4 text-sm"
                        >
                          <div className="flex flex-wrap items-start justify-between gap-3">
                            <div>
                              <p className="font-semibold text-gray-900">
                                {op.username ?? op.email ?? 'Sin usuario'}
                              </p>
                              <p className="text-xs text-gray-500">
                                Estado:{' '}
                                <span className="font-medium text-gray-700">
                                  {op.status === 'active'
                                    ? 'Activo'
                                    : 'Suspendido'}
                                </span>
                              </p>
                            </div>
                            <button
                              type="button"
                              disabled={busy}
                              onClick={() => void toggleStatus(op)}
                              className="shrink-0 rounded-lg border border-gray-300 bg-white px-3 py-1.5 text-xs font-medium text-gray-800 hover:bg-gray-50 disabled:opacity-50"
                            >
                              {busy ? (
                                <Loader2
                                  className="h-3 w-3 animate-spin"
                                  aria-hidden
                                />
                              ) : op.status === 'active' ? (
                                'Suspender'
                              ) : (
                                'Reactivar'
                              )}
                            </button>
                          </div>
                          <div className="flex flex-wrap items-end gap-2">
                            <label className="min-w-[200px] flex-1 text-xs">
                              <span className="text-gray-600">
                                Nueva contraseña
                              </span>
                              <input
                                type="password"
                                minLength={8}
                                value={pwdById[op.id] ?? ''}
                                onChange={(e) =>
                                  setPwdById((prev) => ({
                                    ...prev,
                                    [op.id]: e.target.value,
                                  }))
                                }
                                className="mt-1 w-full rounded border border-gray-300 px-2 py-1.5 text-sm"
                                autoComplete="new-password"
                              />
                            </label>
                            <button
                              type="button"
                              disabled={busy}
                              onClick={() => void savePassword(op)}
                              className="rounded-lg bg-gray-100 px-3 py-2 text-xs font-semibold text-gray-800 hover:bg-gray-200 disabled:opacity-50"
                            >
                              Guardar contraseña
                            </button>
                          </div>
                        </li>
                      );
                    })}
                  </ul>
                )}
              </section>
            )}
          </>
        ) : null}
      </div>
    </div>
  );
}
