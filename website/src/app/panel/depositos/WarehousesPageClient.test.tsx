import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const mockReplace = vi.fn();
const mockPush = vi.fn();
const mockFetchWithWebAuth = vi.fn();
const mockHasWebSession = vi.fn();
const mockRefreshWebSession = vi.fn();
const mockGetApiBaseUrl = vi.fn();
const mockFetchProfile = vi.fn();
const mockCanAccessWebManagement = vi.fn();
const mockPostWithWebAuth = vi.fn();
const mockPatchWithWebAuth = vi.fn();
const mockDeleteWithWebAuth = vi.fn();

vi.mock('next/navigation', () => ({
  useRouter: () => ({ replace: mockReplace, push: mockPush }),
}));

vi.mock('../../lib/webAuth', () => ({
  fetchWithWebAuth: (...args: unknown[]) => mockFetchWithWebAuth(...args),
  postWithWebAuth: (...args: unknown[]) => mockPostWithWebAuth(...args),
  patchWithWebAuth: (...args: unknown[]) => mockPatchWithWebAuth(...args),
  deleteWithWebAuth: (...args: unknown[]) => mockDeleteWithWebAuth(...args),
  hasWebSession: () => mockHasWebSession(),
  refreshWebSession: () => mockRefreshWebSession(),
  fetchProfile: () => mockFetchProfile(),
  canAccessWebManagement: (...args: unknown[]) =>
    mockCanAccessWebManagement(...args),
  clearWebSession: vi.fn(),
}));

vi.mock('../../lib/apiUrl', () => ({
  getApiBaseUrl: () => mockGetApiBaseUrl(),
}));

async function renderClient() {
  const mod = await import('./WarehousesPageClient');
  return render(<mod.default />);
}

describe('WarehousesPageClient', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockHasWebSession.mockReturnValue(true);
    mockRefreshWebSession.mockResolvedValue(true);
    mockFetchProfile.mockResolvedValue({
      id: 'u1',
      username: 'owner',
      company_id: 'c1',
      company_name: 'Acme',
      company_code: 'ACME',
      role: 'company_owner',
    });
    mockCanAccessWebManagement.mockReturnValue(true);
    mockGetApiBaseUrl.mockReturnValue('http://localhost:8080');
    mockFetchWithWebAuth.mockImplementation(async (path: unknown) => {
      if (path === '/warehouses') {
        return new Response(
          JSON.stringify([
            {
              id: 'w1',
              company_id: 'c1',
              name: 'Central',
              address: 'Av. Siempre Viva 123',
              created_at: new Date().toISOString(),
              updated_at: new Date().toISOString(),
            },
            {
              id: 'w2',
              company_id: 'c1',
              name: 'Norte',
              address: '',
              created_at: new Date().toISOString(),
              updated_at: new Date().toISOString(),
            },
          ]),
          { status: 200 },
        );
      }
      return new Response(
        JSON.stringify({
          can_download_app: true,
          subscription_plan: 'pyme',
          warehouse_count: 2,
          max_warehouses: 2,
          documents_usage_mtd: 0,
        }),
        { status: 200 },
      );
    });
  });

  it('renders the list and disables the create button at the cap', async () => {
    await renderClient();
    expect(
      await screen.findByRole('heading', { name: 'Depósitos' }),
    ).toBeInTheDocument();
    expect(screen.getByText('Central')).toBeInTheDocument();
    expect(screen.getByText('Norte')).toBeInTheDocument();
    const addBtn = screen.getByRole('button', { name: /agregar dep/i });
    expect(addBtn).toBeDisabled();
    expect(
      screen.getByText(/alcanzaste el l[íi]mite/i),
    ).toBeInTheDocument();
  });

  it('shows a soft-delete confirmation and calls delete', async () => {
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true);
    mockDeleteWithWebAuth.mockResolvedValue(new Response('{}', { status: 200 }));
    await renderClient();
    await screen.findByText('Central');
    const archiveBtns = screen.getAllByRole('button', { name: /archivar/i });
    await userEvent.click(archiveBtns[0]);
    expect(confirmSpy).toHaveBeenCalled();
    expect(mockDeleteWithWebAuth).toHaveBeenCalledWith('/warehouses/w1');
    confirmSpy.mockRestore();
  });
});
