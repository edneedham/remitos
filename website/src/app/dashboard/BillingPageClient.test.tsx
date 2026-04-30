import { render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const mockReplace = vi.fn();
const mockFetchWithWebAuth = vi.fn();
const mockHasWebSession = vi.fn();
const mockRefreshWebSession = vi.fn();
const mockGetApiBaseUrl = vi.fn();
const mockFetchProfile = vi.fn();
const mockCanAccessWebManagement = vi.fn();

vi.mock('next/navigation', () => ({
  useRouter: () => ({
    replace: mockReplace,
  }),
}));

vi.mock('../lib/webAuth', () => ({
  fetchWithWebAuth: (...args: unknown[]) => mockFetchWithWebAuth(...args),
  hasWebSession: () => mockHasWebSession(),
  refreshWebSession: () => mockRefreshWebSession(),
  fetchProfile: () => mockFetchProfile(),
  canAccessWebManagement: (...args: unknown[]) =>
    mockCanAccessWebManagement(...args),
  clearWebSession: vi.fn(),
}));

vi.mock('../lib/apiUrl', () => ({
  getApiBaseUrl: () => mockGetApiBaseUrl(),
}));

async function renderBillingPageClient() {
  const mod = await import('./BillingPageClient');
  return render(<mod.default />);
}

describe('BillingPageClient', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockHasWebSession.mockReturnValue(true);
    mockRefreshWebSession.mockResolvedValue(true);
    mockFetchProfile.mockResolvedValue({
      id: '11111111-1111-1111-1111-111111111111',
      username: 'owner',
      company_id: '22222222-2222-2222-2222-222222222222',
      company_name: 'Acme',
      company_code: 'ACME',
      role: 'admin',
    });
    mockCanAccessWebManagement.mockReturnValue(true);
    mockGetApiBaseUrl.mockReturnValue('http://localhost:8080');
    mockFetchWithWebAuth.mockImplementation(async (path: unknown) => {
      if (path === '/auth/me/invoices') {
        return new Response(
          JSON.stringify([
            {
              id: '33333333-3333-3333-3333-333333333333',
              amount_minor: 10050,
              currency: 'ARS',
              status: 'paid',
              description: 'Suscripción mensual',
              issued_at: '2025-06-01T14:30:00.000Z',
            },
          ]),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        );
      }
      return new Response(
        JSON.stringify({
          can_download_app: true,
          subscription_plan: 'trial',
          trial_ends_at: new Date(Date.now() + 86400000).toISOString(),
          warehouse_count: 3,
          device_count: 2,
          remitos_processed_last_30_days: 12,
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      );
    });
  });

  it('shows billing summary and invoice table after loads', async () => {
    await renderBillingPageClient();

    expect(await screen.findByRole('heading', { name: 'Facturación', level: 1 })).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Resumen de facturación' }),
    ).toBeInTheDocument();
    expect(screen.getByText('Tu plan actual')).toBeInTheDocument();
    expect(screen.getByText('Uso actual')).toBeInTheDocument();
    expect(screen.getByText('Proyección')).toBeInTheDocument();
    expect(
      screen.getByRole('link', { name: 'Mejorar plan' }),
    ).toHaveAttribute('href', '/pricing');
    expect(
      screen.getByRole('heading', { name: 'Comprobantes de pago' }),
    ).toBeInTheDocument();
    expect(screen.getByText('Suscripción mensual')).toBeInTheDocument();
    expect(screen.getByText('Pagado')).toBeInTheDocument();
    expect(screen.getAllByText('En período de prueba').length).toBeGreaterThanOrEqual(
      1,
    );
  });

  it('redirects to login when session is missing', async () => {
    mockHasWebSession.mockReturnValue(false);
    await renderBillingPageClient();
    await waitFor(() => expect(mockReplace).toHaveBeenCalledWith('/login'));
  });
});
