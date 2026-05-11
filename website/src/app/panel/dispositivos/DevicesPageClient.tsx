'use client';

import Link from 'next/link';
import { useEffect, useMemo, useState } from 'react';
import { Loader2, Smartphone } from 'lucide-react';
import { DevicesGroupedListSkeleton } from '../components/PanelSkeletons';
import {
  clearWebSession,
  fetchWithWebAuth,
  patchWithWebAuth,
} from '../../lib/webAuth';
import { usePanelBootstrap } from '../lib/usePanelBootstrap';
import { useRouterRef } from '../lib/useRouterRef';

type DeviceStatus = 'active' | 'revoked' | 'pending' | string;

type Device = {
  id: string;
  company_id: string;
  warehouse_id: string;
  warehouse_name: string;
  device_uuid: string;
  platform: string;
  model?: string;
  os_version?: string;
  app_version?: string;
  status: DeviceStatus;
  registered_at: string;
  last_seen_at?: string;
};

function formatDateTime(value?: string): string {
  if (!value) return '—';
  const ms = Date.parse(value);
  if (!Number.isFinite(ms)) return '—';
  return new Date(ms).toLocaleString('es-AR', {
    dateStyle: 'short',
    timeStyle: 'short',
  });
}

function statusBadge(status: DeviceStatus): {
  label: string;
  className: string;
} {
  if (status === 'active') {
    return {
      label: 'Activo',
      className: 'bg-emerald-100 text-emerald-800',
    };
  }
  if (status === 'revoked') {
    return {
      label: 'Revocado',
      className: 'bg-red-100 text-red-800',
    };
  }
  return {
    label: status || '—',
    className: 'bg-gray-100 text-gray-800',
  };
}

export default function DevicesPageClient() {
  const routerRef = useRouterRef();
  const { status, errorMessage: configError } = usePanelBootstrap();
  const [devicesLoading, setDevicesLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [devices, setDevices] = useState<Device[]>([]);
  const [busyId, setBusyId] = useState<string | null>(null);

  useEffect(() => {
    if (status === 'config_error') {
      setLoadError(configError);
      setDevicesLoading(false);
      return;
    }
    if (status !== 'ready') {
      return;
    }

    let cancelled = false;

    async function load() {
      const res = await fetchWithWebAuth('/devices');
      if (cancelled) return;
      if (res.status === 401) {
        clearWebSession();
        routerRef.current.replace('/ingresar');
        return;
      }
      if (!res.ok) {
        setLoadError(
          'No se pudieron cargar los dispositivos. Probá de nuevo más tarde.',
        );
        setDevicesLoading(false);
        return;
      }
      const list = (await res.json()) as Device[];
      setDevices(Array.isArray(list) ? list : []);
      setDevicesLoading(false);
    }

    setDevicesLoading(true);
    setLoadError(null);
    void load();
    return () => {
      cancelled = true;
    };
  }, [status, configError, routerRef]);

  const refreshDevices = async () => {
    const res = await fetchWithWebAuth('/devices');
    if (res.ok) {
      const list = (await res.json()) as Device[];
      setDevices(Array.isArray(list) ? list : []);
    }
  };

  const onAction = async (
    device: Device,
    action: 'revoke' | 'reactivate',
  ) => {
    if (action === 'revoke') {
      const ok = window.confirm(
        `¿Revocar el dispositivo "${device.model ?? device.platform}" del depósito ${device.warehouse_name || '—'}? El operador será desconectado en su próxima sincronización.`,
      );
      if (!ok) return;
    }
    setBusyId(device.id);
    setActionError(null);
    const res = await patchWithWebAuth(`/devices/${device.id}/${action}`, {});
    setBusyId(null);
    if (res.ok) {
      await refreshDevices();
      return;
    }
    const data = (await res.json().catch(() => ({}))) as { message?: string };
    setActionError(
      data.message ||
        (action === 'revoke'
          ? 'No se pudo revocar el dispositivo.'
          : 'No se pudo reactivar el dispositivo.'),
    );
  };

  const grouped = useMemo(() => {
    const map = new Map<
      string,
      { warehouseName: string; warehouseId: string; devices: Device[] }
    >();
    for (const device of devices) {
      const key = device.warehouse_id || '__unassigned';
      const existing = map.get(key);
      if (existing) {
        existing.devices.push(device);
      } else {
        map.set(key, {
          warehouseId: device.warehouse_id,
          warehouseName: device.warehouse_name || 'Depósito archivado',
          devices: [device],
        });
      }
    }
    return Array.from(map.values()).sort((a, b) =>
      a.warehouseName.localeCompare(b.warehouseName, 'es'),
    );
  }, [devices]);

  return (
    <div className="bg-gray-50 px-4 pb-12 pt-6">
      <div className="mx-auto max-w-5xl space-y-6">
        <header className="space-y-2">
          <h1 className="text-3xl font-bold tracking-tight text-gray-900">
            Dispositivos
          </h1>
          <p className="text-base leading-relaxed text-gray-600">
            Cada operador registra su teléfono desde la app En Punto eligiendo
            un depósito. Para evitar accesos indebidos, podés revocar
            dispositivos perdidos o que ya no usás. El consumo real de tu plan
            se mide por documentos sincronizados, no por la cantidad de
            dispositivos.
          </p>
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

        {devicesLoading && !loadError ? (
          <DevicesGroupedListSkeleton />
        ) : null}

        {!devicesLoading && devices.length === 0 ? (
          <section className="rounded-xl border border-gray-200 bg-white p-6 text-sm text-gray-600 shadow-sm">
            <div className="flex items-center gap-3">
              <Smartphone className="h-6 w-6 text-orange-600" aria-hidden />
              <p>
                Aún no hay dispositivos registrados. Descargá la app desde{' '}
                <Link
                  href="/panel/aplicacion"
                  className="font-semibold text-blue-700 underline"
                >
                  Aplicación
                </Link>{' '}
                e iniciá sesión en el teléfono para registrar el primero.
              </p>
            </div>
          </section>
        ) : !devicesLoading ? (
          <div className="space-y-6">
            {grouped.map((group) => (
              <section
                key={group.warehouseId || group.warehouseName}
                className="rounded-xl border border-gray-200 bg-white shadow-sm"
                aria-label={`Depósito ${group.warehouseName}`}
              >
                <header className="border-b border-gray-100 px-5 py-3">
                  <h2 className="text-base font-semibold text-gray-900">
                    {group.warehouseName}
                  </h2>
                  <p className="mt-0.5 text-xs text-gray-500">
                    {group.devices.length} dispositivo
                    {group.devices.length === 1 ? '' : 's'}
                  </p>
                </header>
                <ul className="divide-y divide-gray-100">
                  {group.devices.map((device) => {
                    const badge = statusBadge(device.status);
                    const busy = busyId === device.id;
                    return (
                      <li
                        key={device.id}
                        className="flex flex-wrap items-center justify-between gap-3 px-5 py-4"
                      >
                        <div className="min-w-0">
                          <div className="flex flex-wrap items-center gap-2">
                            <p className="text-sm font-semibold text-gray-900">
                              {device.model ?? device.platform}
                            </p>
                            <span
                              className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-semibold ${badge.className}`}
                            >
                              {badge.label}
                            </span>
                          </div>
                          <p className="mt-0.5 text-xs text-gray-500">
                            {device.platform}
                            {device.os_version ? ` · ${device.os_version}` : ''}
                            {device.app_version
                              ? ` · App ${device.app_version}`
                              : ''}
                          </p>
                          <p className="mt-0.5 text-xs text-gray-500">
                            Registrado: {formatDateTime(device.registered_at)}
                            {' · '}Última actividad:{' '}
                            {formatDateTime(device.last_seen_at)}
                          </p>
                        </div>
                        <div className="flex shrink-0 gap-2">
                          {device.status === 'revoked' ? (
                            <button
                              type="button"
                              disabled={busy}
                              onClick={() => onAction(device, 'reactivate')}
                              className="inline-flex items-center justify-center rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm font-medium text-gray-800 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-60"
                            >
                              {busy ? (
                                <Loader2
                                  className="h-4 w-4 animate-spin"
                                  aria-hidden
                                />
                              ) : (
                                'Reactivar'
                              )}
                            </button>
                          ) : (
                            <button
                              type="button"
                              disabled={busy}
                              onClick={() => onAction(device, 'revoke')}
                              className="inline-flex items-center justify-center rounded-lg border border-red-300 bg-white px-3 py-2 text-sm font-medium text-red-700 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-60"
                            >
                              {busy ? (
                                <Loader2
                                  className="h-4 w-4 animate-spin"
                                  aria-hidden
                                />
                              ) : (
                                'Revocar'
                              )}
                            </button>
                          )}
                        </div>
                      </li>
                    );
                  })}
                </ul>
              </section>
            ))}
          </div>
        ) : null}
      </div>
    </div>
  );
}
