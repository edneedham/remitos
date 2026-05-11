'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { Loader2, Pencil, Plus, Trash2 } from 'lucide-react';
import {
  PanelEntitlementIntroSkeleton,
  WarehousesBodySkeleton,
} from '../components/PanelSkeletons';
import {
  clearWebSession,
  deleteWithWebAuth,
  fetchWithWebAuth,
  patchWithWebAuth,
  postWithWebAuth,
} from '../../lib/webAuth';
import type { Entitlement } from '../lib/entitlementTypes';
import { usePanelBootstrap } from '../lib/usePanelBootstrap';
import { useRouterRef } from '../lib/useRouterRef';

type Warehouse = {
  id: string;
  company_id: string;
  name: string;
  address?: string;
  created_at: string;
  updated_at: string;
};

type FormState = {
  mode: 'closed' | 'create' | 'edit';
  id?: string;
  name: string;
  address: string;
};

const emptyForm: FormState = { mode: 'closed', name: '', address: '' };

export default function WarehousesPageClient() {
  const routerRef = useRouterRef();
  const { status, errorMessage: configError } = usePanelBootstrap();
  const [warehousesLoading, setWarehousesLoading] = useState(true);
  const [entitlementLoading, setEntitlementLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [entitlement, setEntitlement] = useState<Entitlement | null>(null);
  const [form, setForm] = useState<FormState>(emptyForm);
  const [submitting, setSubmitting] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (status === 'config_error') {
      setLoadError(configError);
      setWarehousesLoading(false);
      setEntitlementLoading(false);
      return;
    }
    if (status !== 'ready') {
      return;
    }

    let cancelled = false;

    async function load() {
      const [whRes, entRes] = await Promise.all([
        fetchWithWebAuth('/warehouses'),
        fetchWithWebAuth('/auth/me/entitlement'),
      ]);
      if (cancelled) return;

      if (whRes.status === 401 || entRes.status === 401) {
        clearWebSession();
        routerRef.current.replace('/ingresar');
        return;
      }

      setEntitlementLoading(false);
      if (entRes.ok) {
        setEntitlement((await entRes.json()) as Entitlement);
      }

      if (!whRes.ok) {
        setLoadError(
          'No se pudieron cargar los depósitos. Probá de nuevo más tarde.',
        );
        setWarehousesLoading(false);
        return;
      }
      const list = (await whRes.json()) as Warehouse[];
      setWarehouses(Array.isArray(list) ? list : []);

      setWarehousesLoading(false);
    }

    setWarehousesLoading(true);
    setEntitlementLoading(true);
    setLoadError(null);
    void load();
    return () => {
      cancelled = true;
    };
  }, [status, configError, routerRef]);

  const refreshList = async () => {
    const res = await fetchWithWebAuth('/warehouses');
    if (res.ok) {
      const list = (await res.json()) as Warehouse[];
      setWarehouses(Array.isArray(list) ? list : []);
    }
    const entRes = await fetchWithWebAuth('/auth/me/entitlement');
    if (entRes.ok) {
      setEntitlement((await entRes.json()) as Entitlement);
    }
  };

  const startCreate = () => {
    setActionError(null);
    setFieldErrors({});
    setForm({ mode: 'create', name: '', address: '' });
  };

  const startEdit = (warehouse: Warehouse) => {
    setActionError(null);
    setFieldErrors({});
    setForm({
      mode: 'edit',
      id: warehouse.id,
      name: warehouse.name,
      address: warehouse.address ?? '',
    });
  };

  const cancelForm = () => setForm(emptyForm);

  const onSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setSubmitting(true);
    setActionError(null);
    setFieldErrors({});

    const body = {
      name: form.name.trim(),
      address: form.address.trim(),
    };

    const res =
      form.mode === 'create'
        ? await postWithWebAuth('/warehouses', body)
        : await patchWithWebAuth(`/warehouses/${form.id}`, body);

    setSubmitting(false);

    if (res.ok) {
      setForm(emptyForm);
      await refreshList();
      return;
    }

    const data = (await res.json().catch(() => ({}))) as {
      message?: string;
      fields?: Record<string, string>;
    };
    setActionError(
      data.message ||
        'No se pudo guardar el depósito. Probá de nuevo más tarde.',
    );
    if (data.fields) setFieldErrors(data.fields);
  };

  const onArchive = async (warehouse: Warehouse) => {
    const ok = window.confirm(
      `¿Archivar el depósito "${warehouse.name}"? No se podrá usar para registrar dispositivos ni recibir remitos.`,
    );
    if (!ok) return;
    setActionError(null);
    const res = await deleteWithWebAuth(`/warehouses/${warehouse.id}`);
    if (res.ok) {
      await refreshList();
      return;
    }
    const data = (await res.json().catch(() => ({}))) as { message?: string };
    setActionError(
      data.message || 'No se pudo archivar el depósito.',
    );
  };

  const max = entitlement?.max_warehouses ?? null;
  const count = warehouses.length;
  const atLimit = typeof max === 'number' && count >= max;

  return (
    <div className="bg-gray-50 px-4 pb-12 pt-6">
      <div className="mx-auto max-w-5xl space-y-6">
        <header className="space-y-2">
          <h1 className="text-3xl font-bold tracking-tight text-gray-900">
            Depósitos
          </h1>
          {entitlementLoading && !loadError ? (
            <PanelEntitlementIntroSkeleton />
          ) : (
            <p className="text-base leading-relaxed text-gray-600">
              Cada depósito agrupa los dispositivos y remitos de una sucursal o
              ubicación. Podés tener{' '}
              {typeof max === 'number' ? (
                <>
                  hasta <span className="font-semibold">{max}</span>{' '}
                  depósito{max === 1 ? '' : 's'} en tu plan actual ({count} en
                  uso).
                </>
              ) : (
                <>
                  la cantidad que necesites en tu plan actual ({count} en uso).
                </>
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

        {actionError ? (
          <div
            className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800"
            role="alert"
          >
            {actionError}
          </div>
        ) : null}

        {form.mode === 'closed' && warehousesLoading && !loadError ? (
          <WarehousesBodySkeleton />
        ) : (
          <>
            {form.mode === 'closed' ? (
              <div className="flex flex-wrap items-center gap-3">
                <button
                  type="button"
                  onClick={startCreate}
                  disabled={atLimit}
                  className="inline-flex items-center justify-center gap-1.5 rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-gray-300"
                >
                  <Plus className="h-4 w-4" aria-hidden />
                  Agregar depósito
                </button>
                {atLimit ? (
                  <p className="text-sm text-amber-800">
                    Alcanzaste el límite de tu plan.{' '}
                    <Link
                      href="/panel/facturacion/mejorar-plan"
                      className="font-semibold underline"
                    >
                      Mejorar plan
                    </Link>{' '}
                    para agregar más.
                  </p>
                ) : null}
              </div>
            ) : (
              <form
                onSubmit={onSubmit}
                className="space-y-4 rounded-xl border border-gray-200 bg-white p-5 shadow-sm"
                aria-labelledby="warehouse-form-heading"
              >
                <h2
                  id="warehouse-form-heading"
                  className="text-base font-semibold text-gray-900"
                >
                  {form.mode === 'create' ? 'Nuevo depósito' : 'Editar depósito'}
                </h2>
                <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                  <label className="block text-sm">
                    <span className="font-medium text-gray-800">Nombre</span>
                    <input
                      type="text"
                      required
                      maxLength={100}
                      value={form.name}
                      onChange={(e) =>
                        setForm((prev) => ({ ...prev, name: e.target.value }))
                      }
                      className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
                    />
                    {fieldErrors.name ? (
                      <p className="mt-1 text-xs text-red-700">
                        {fieldErrors.name}
                      </p>
                    ) : null}
                  </label>
                  <label className="block text-sm">
                    <span className="font-medium text-gray-800">
                      Dirección (opcional)
                    </span>
                    <input
                      type="text"
                      maxLength={500}
                      value={form.address}
                      onChange={(e) =>
                        setForm((prev) => ({ ...prev, address: e.target.value }))
                      }
                      className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
                    />
                    {fieldErrors.address ? (
                      <p className="mt-1 text-xs text-red-700">
                        {fieldErrors.address}
                      </p>
                    ) : null}
                  </label>
                </div>
                <div className="flex flex-wrap gap-3">
                  <button
                    type="submit"
                    disabled={submitting}
                    className="inline-flex items-center justify-center rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-blue-300"
                  >
                    {submitting ? (
                      <Loader2 className="h-4 w-4 animate-spin" aria-hidden />
                    ) : form.mode === 'create' ? (
                      'Crear depósito'
                    ) : (
                      'Guardar cambios'
                    )}
                  </button>
                  <button
                    type="button"
                    onClick={cancelForm}
                    disabled={submitting}
                    className="inline-flex items-center justify-center rounded-lg border border-gray-300 bg-white px-4 py-2.5 text-sm font-semibold text-gray-700 hover:bg-gray-50"
                  >
                    Cancelar
                  </button>
                </div>
              </form>
            )}

            <section
              className="rounded-xl border border-gray-200 bg-white shadow-sm"
              aria-label="Listado de depósitos"
            >
              {warehousesLoading && !loadError ? (
                <div className="p-6" aria-busy="true">
                  <div className="space-y-4">
                    {Array.from({ length: 5 }).map((_, i) => (
                      <div
                        key={i}
                        className="flex flex-wrap items-center justify-between gap-3 border-b border-gray-50 pb-4 last:border-0"
                      >
                        <div className="min-w-0 flex-1 space-y-2">
                          <div className="h-5 w-48 max-w-full animate-pulse rounded bg-gray-200" />
                          <div className="h-4 w-full max-w-sm animate-pulse rounded bg-gray-200" />
                        </div>
                        <div className="flex shrink-0 gap-2">
                          <div className="h-9 w-20 animate-pulse rounded-lg bg-gray-200" />
                          <div className="h-9 w-24 animate-pulse rounded-lg bg-gray-200" />
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ) : null}

              {!warehousesLoading && warehouses.length === 0 ? (
                <p className="p-6 text-sm text-gray-600">
                  Aún no tenés depósitos. Crea el primero para registrar
                  dispositivos y empezar a cargar remitos.
                </p>
              ) : !warehousesLoading ? (
                <ul className="divide-y divide-gray-100">
                  {warehouses.map((warehouse) => (
                    <li
                      key={warehouse.id}
                      className="flex flex-wrap items-center justify-between gap-3 px-5 py-4"
                    >
                      <div className="min-w-0">
                        <p className="truncate text-base font-semibold text-gray-900">
                          {warehouse.name}
                        </p>
                        {warehouse.address ? (
                          <p className="mt-0.5 truncate text-sm text-gray-600">
                            {warehouse.address}
                          </p>
                        ) : null}
                      </div>
                      <div className="flex shrink-0 gap-2">
                        <button
                          type="button"
                          onClick={() => startEdit(warehouse)}
                          className="inline-flex items-center gap-1.5 rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm font-medium text-gray-800 hover:bg-gray-50"
                        >
                          <Pencil className="h-4 w-4" aria-hidden />
                          Editar
                        </button>
                        <button
                          type="button"
                          onClick={() => onArchive(warehouse)}
                          className="inline-flex items-center gap-1.5 rounded-lg border border-red-300 bg-white px-3 py-2 text-sm font-medium text-red-700 hover:bg-red-50"
                        >
                          <Trash2 className="h-4 w-4" aria-hidden />
                          Archivar
                        </button>
                      </div>
                    </li>
                  ))}
                </ul>
              ) : null}
            </section>
          </>
        )}
      </div>
    </div>
  );
}
