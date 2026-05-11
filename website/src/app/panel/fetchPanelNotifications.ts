import {
  fetchWithWebAuth,
  patchWithWebAuth,
  hasWebSession,
} from '../lib/webAuth';
import type {
  PanelNotification,
  PanelNotificationsResponse,
} from './panelNotificationsTypes';

export async function fetchPanelNotificationsList(params?: {
  limit?: number;
  unread_only?: boolean;
}): Promise<PanelNotificationsResponse | null> {
  if (!hasWebSession()) return null;
  const search = new URLSearchParams();
  if (params?.limit != null) search.set('limit', String(params.limit));
  if (params?.unread_only) search.set('unread_only', 'true');
  const q = search.toString();
  const path = `/auth/me/notifications${q ? `?${q}` : ''}`;
  const res = await fetchWithWebAuth(path);
  if (!res.ok) return null;
  const body = (await res.json().catch(() => null)) as PanelNotificationsResponse | null;
  if (!body || !Array.isArray(body.notifications)) return null;
  return body;
}

export async function markPanelNotificationRead(id: string): Promise<boolean> {
  if (!hasWebSession()) return false;
  const res = await patchWithWebAuth(
    `/auth/me/notifications/${encodeURIComponent(id)}/read`,
    null,
  );
  return res.ok;
}

export function isUnread(n: PanelNotification): boolean {
  return !n.read_at;
}
