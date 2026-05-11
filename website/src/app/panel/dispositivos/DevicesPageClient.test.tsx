import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const mockReplace = vi.fn();
const mockFetchWithWebAuth = vi.fn();
const mockPatchWithWebAuth = vi.fn();

vi.mock('next/navigation', () => ({
  useRouter: () => ({ replace: mockReplace }),
}));

vi.mock('../../lib/webAuth', () => ({
  fetchWithWebAuth: (...args: unknown[]) => mockFetchWithWebAuth(...args),
  patchWithWebAuth: (...args: unknown[]) => mockPatchWithWebAuth(...args),
  clearWebSession: vi.fn(),
}));

vi.mock('../lib/usePanelBootstrap', () => ({
  usePanelBootstrap: () => ({
    status: 'ready' as const,
    profile: {
      id: 'u1',
      username: 'owner',
      company_id: 'c1',
      company_name: 'Acme',
      company_code: 'ACME',
      role: 'company_owner',
    },
    errorMessage: null,
    refresh: vi.fn(),
  }),
}));

async function renderClient() {
  const mod = await import('./DevicesPageClient');
  return render(<mod.default />);
}

describe('DevicesPageClient', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    const devicesPayload = [
      {
        id: 'd1',
        company_id: 'c1',
        warehouse_id: 'w1',
        warehouse_name: 'Central',
        device_uuid: 'u-1',
        platform: 'android',
        model: 'Pixel 6',
        status: 'active',
        registered_at: '2026-04-01T12:00:00Z',
        last_seen_at: '2026-04-30T18:00:00Z',
      },
      {
        id: 'd2',
        company_id: 'c1',
        warehouse_id: 'w1',
        warehouse_name: 'Central',
        device_uuid: 'u-2',
        platform: 'android',
        model: 'Galaxy A52',
        status: 'revoked',
        registered_at: '2026-03-01T12:00:00Z',
      },
    ];
    mockFetchWithWebAuth.mockImplementation(async () =>
      new Response(JSON.stringify(devicesPayload), { status: 200 }),
    );
  });

  it('groups devices by warehouse and shows status badges', async () => {
    await renderClient();
    expect(
      await screen.findByRole('heading', { name: 'Dispositivos' }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Central' }),
    ).toBeInTheDocument();
    expect(screen.getByText('Pixel 6')).toBeInTheDocument();
    expect(screen.getByText('Galaxy A52')).toBeInTheDocument();
    expect(screen.getByText('Activo')).toBeInTheDocument();
    expect(screen.getByText('Revocado')).toBeInTheDocument();
  });

  it('confirms and revokes an active device', async () => {
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true);
    mockPatchWithWebAuth.mockResolvedValue(
      new Response('{}', { status: 200 }),
    );
    await renderClient();
    await screen.findByText('Pixel 6');
    const revokeBtn = screen.getByRole('button', { name: /revocar/i });
    await userEvent.click(revokeBtn);
    expect(confirmSpy).toHaveBeenCalled();
    expect(mockPatchWithWebAuth).toHaveBeenCalledWith(
      '/devices/d1/revoke',
      {},
    );
    confirmSpy.mockRestore();
  });
});
