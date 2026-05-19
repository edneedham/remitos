'use client';

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { Bell } from 'lucide-react';
import {
  fetchPanelNotificationsList,
  fetchPanelNotificationsUnreadBadge,
  markPanelNotificationRead,
  isUnread,
} from './fetchPanelNotifications';
import type { PanelNotification } from './panelNotificationsTypes';
import {
  panelNotificationKindUi,
  PANEL_NOTIFICATION_TONE_WRAP,
} from './panelNotificationKindUi';

type PanelNotificationsContextValue = {
  notifications: PanelNotification[];
  unreadCount: number;
  open: boolean;
  setOpen: (next: boolean | ((prev: boolean) => boolean)) => void;
  loading: boolean;
  refresh: () => Promise<void>;
  toggleOpen: () => void;
  onPickNotification: (n: PanelNotification) => Promise<void>;
};

const PanelNotificationsContext =
  createContext<PanelNotificationsContextValue | null>(null);

export function PanelNotificationsProvider({
  children,
}: {
  children: React.ReactNode;
}) {
  const [notifications, setNotifications] = useState<PanelNotification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);

  const refreshUnreadBadge = useCallback(async () => {
    try {
      const count = await fetchPanelNotificationsUnreadBadge();
      if (count !== null) {
        setUnreadCount(count);
      }
    } catch {
      /* ignore */
    }
  }, []);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      const data = await fetchPanelNotificationsList({ limit: 40 });
      if (data) {
        setNotifications(data.notifications);
        setUnreadCount(data.unread_count);
      }
    } finally {
      setLoading(false);
    }
  }, []);

  /** Bell badge: one small request on mount + when the window regains focus (no full list until the bell opens). */
  useEffect(() => {
    queueMicrotask(() => {
      void refreshUnreadBadge();
    });
  }, [refreshUnreadBadge]);

  useEffect(() => {
    function onFocus() {
      void refreshUnreadBadge();
    }
    window.addEventListener('focus', onFocus);
    return () => window.removeEventListener('focus', onFocus);
  }, [refreshUnreadBadge]);

  useEffect(() => {
    function onVisibilityChange() {
      if (document.visibilityState === 'visible') {
        void refreshUnreadBadge();
      }
    }
    document.addEventListener('visibilitychange', onVisibilityChange);
    return () =>
      document.removeEventListener('visibilitychange', onVisibilityChange);
  }, [refreshUnreadBadge]);

  /** Full list when the user opens the panel (lazy load). */
  useEffect(() => {
    if (!open) return;
    queueMicrotask(() => {
      void refresh();
    });
  }, [open, refresh]);

  const toggleOpen = useCallback(() => {
    setOpen((o) => !o);
  }, []);

  const onPickNotification = useCallback(
    async (n: PanelNotification) => {
      if (isUnread(n)) {
        const ok = await markPanelNotificationRead(n.id);
        if (ok) {
          setNotifications((prev) =>
            prev.map((x) =>
              x.id === n.id ? { ...x, read_at: new Date().toISOString() } : x,
            ),
          );
          setUnreadCount((c) => Math.max(0, c - 1));
        }
      }
      setOpen(false);
    },
    [],
  );

  useEffect(() => {
    if (!open) return;

    function onKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') setOpen(false);
    }
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [open]);

  useEffect(() => {
    if (!open) return;

    function onPointerDown(event: MouseEvent) {
      const target = event.target as HTMLElement | null;
      if (target?.closest?.('[data-notification-ui]')) return;
      setOpen(false);
    }

    document.addEventListener('mousedown', onPointerDown);
    return () => document.removeEventListener('mousedown', onPointerDown);
  }, [open]);

  const value = useMemo(
    () => ({
      notifications,
      unreadCount,
      open,
      setOpen,
      loading,
      refresh,
      toggleOpen,
      onPickNotification,
    }),
    [
      notifications,
      unreadCount,
      open,
      loading,
      refresh,
      toggleOpen,
      onPickNotification,
    ],
  );

  return (
    <PanelNotificationsContext.Provider value={value}>
      {children}
      {open ? (
          <div
            data-notification-ui
            className="fixed right-4 top-[5.5rem] z-[70] w-[min(22rem,calc(100vw-2rem))] rounded-xl border border-gray-200 bg-white shadow-xl ring-1 ring-black/5 sm:right-6"
            role="dialog"
            aria-label="Notificaciones"
          >
            <div className="border-b border-gray-100 px-4 py-3">
              <p className="text-sm font-semibold text-gray-900">
                Notificaciones
              </p>
            </div>
            <div className="max-h-[min(24rem,calc(100vh-8rem))] overflow-y-auto">
              {loading && notifications.length === 0 ? (
                <p className="px-4 py-8 text-center text-sm text-gray-500">
                  Cargando…
                </p>
              ) : notifications.length === 0 ? (
                <p className="px-4 py-8 text-center text-sm text-gray-500">
                  No hay notificaciones.
                </p>
              ) : (
                <ul className="divide-y divide-gray-100">
                  {notifications.map((n) => (
                    <li key={n.id}>
                      {n.action_url ? (
                        <a
                          href={n.action_url}
                          className={`block px-4 py-3 text-left transition-colors hover:bg-gray-50 ${
                            isUnread(n) ? 'bg-blue-50/50' : ''
                          }`}
                          onClick={() => void onPickNotification(n)}
                        >
                          <NotificationRowBody n={n} />
                        </a>
                      ) : (
                        <button
                          type="button"
                          className={`flex w-full px-4 py-3 text-left transition-colors hover:bg-gray-50 ${
                            isUnread(n) ? 'bg-blue-50/50' : ''
                          }`}
                          onClick={() => void onPickNotification(n)}
                        >
                          <NotificationRowBody n={n} />
                        </button>
                      )}
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        ) : null}
    </PanelNotificationsContext.Provider>
  );
}

function NotificationRowBody({ n }: { n: PanelNotification }) {
  const { Icon, tone } = panelNotificationKindUi(n.kind);
  const toneWrap = PANEL_NOTIFICATION_TONE_WRAP[tone];

  return (
    <span className="flex w-full gap-3">
      <span
        className={`mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-lg ${toneWrap}`}
        aria-hidden
      >
        <Icon className="h-5 w-5" strokeWidth={2} />
      </span>
      <span className="min-w-0 flex-1">
        <span className="block text-sm font-semibold text-gray-900">
          {n.title}
        </span>
        {n.body ? (
          <span className="mt-0.5 block text-sm leading-snug text-gray-600">
            {n.body}
          </span>
        ) : null}
      </span>
    </span>
  );
}

function usePanelNotifications() {
  const ctx = useContext(PanelNotificationsContext);
  if (!ctx) {
    throw new Error(
      'PanelNotificationBell must be used within PanelNotificationsProvider',
    );
  }
  return ctx;
}

export function PanelNotificationBell({ className }: { className?: string }) {
  const { unreadCount, open, toggleOpen } = usePanelNotifications();

  return (
    <button
      type="button"
      data-notification-ui
      className={`relative inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-lg text-gray-700 transition-colors hover:bg-gray-100 ${className ?? ''}`}
      aria-expanded={open}
      aria-haspopup="dialog"
      aria-label="Notificaciones"
      onClick={() => toggleOpen()}
    >
      <Bell className="h-6 w-6" aria-hidden strokeWidth={2} />
      {unreadCount > 0 ? (
        <span
          className="absolute right-1.5 top-1.5 h-2 w-2 rounded-full bg-blue-600 ring-2 ring-white"
          aria-hidden
        />
      ) : null}
    </button>
  );
}
