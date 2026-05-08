import { describe, expect, it, vi, beforeEach } from 'vitest';
import {
  fetchPanelNotificationsList,
  markPanelNotificationRead,
  isUnread,
} from './fetchPanelNotifications';
import type { PanelNotification } from './panelNotificationsTypes';

vi.mock('../lib/webAuth', () => ({
  fetchWithWebAuth: vi.fn(),
  patchWithWebAuth: vi.fn(),
  hasWebSession: vi.fn(),
}));

import {
  fetchWithWebAuth,
  patchWithWebAuth,
  hasWebSession,
} from '../lib/webAuth';

describe('fetchPanelNotificationsList', () => {
  beforeEach(() => {
    vi.mocked(hasWebSession).mockReturnValue(true);
    vi.mocked(fetchWithWebAuth).mockReset();
  });

  it('returns null when session missing', async () => {
    vi.mocked(hasWebSession).mockReturnValue(false);
    const out = await fetchPanelNotificationsList();
    expect(out).toBeNull();
    expect(fetchWithWebAuth).not.toHaveBeenCalled();
  });

  it('parses successful response', async () => {
    vi.mocked(fetchWithWebAuth).mockResolvedValue(
      new Response(
        JSON.stringify({
          notifications: [
            {
              id: 'n1',
              kind: 'signup_welcome',
              title: 'Hi',
              created_at: '2026-01-01T00:00:00Z',
            },
          ],
          unread_count: 1,
        }),
        { status: 200 },
      ),
    );

    const out = await fetchPanelNotificationsList();
    expect(out?.unread_count).toBe(1);
    expect(out?.notifications).toHaveLength(1);
    expect(out?.notifications[0].kind).toBe('signup_welcome');
    expect(fetchWithWebAuth).toHaveBeenCalledWith('/auth/me/notifications');
  });

  it('returns null on HTTP error', async () => {
    vi.mocked(fetchWithWebAuth).mockResolvedValue(new Response('', { status: 500 }));
    const out = await fetchPanelNotificationsList();
    expect(out).toBeNull();
  });
});

describe('markPanelNotificationRead', () => {
  beforeEach(() => {
    vi.mocked(hasWebSession).mockReturnValue(true);
    vi.mocked(patchWithWebAuth).mockReset();
  });

  it('returns false without session', async () => {
    vi.mocked(hasWebSession).mockReturnValue(false);
    const ok = await markPanelNotificationRead('x');
    expect(ok).toBe(false);
    expect(patchWithWebAuth).not.toHaveBeenCalled();
  });

  it('returns true on 204', async () => {
    vi.mocked(patchWithWebAuth).mockResolvedValue(new Response(null, { status: 204 }));
    const ok = await markPanelNotificationRead('abc-id');
    expect(ok).toBe(true);
    expect(patchWithWebAuth).toHaveBeenCalledWith(
      '/auth/me/notifications/abc-id/read',
      null,
    );
  });
});

describe('isUnread', () => {
  it('detects read_at', () => {
    const row = {
      id: '1',
      kind: 'k',
      title: 't',
      read_at: null,
      created_at: '2026-01-01T00:00:00Z',
    } as PanelNotification;
    expect(isUnread(row)).toBe(true);
    expect(isUnread({ ...row, read_at: '2026-01-02T00:00:00Z' })).toBe(false);
  });
});
