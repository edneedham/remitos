import { render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const mockReplace = vi.fn();
const mockFetchWithWebAuth = vi.fn();
const mockHasWebSession = vi.fn();
const mockRefreshWebSession = vi.fn();
const mockGetApiBaseUrl = vi.fn();

vi.mock('next/navigation', () => ({
  useRouter: () => ({
    replace: mockReplace,
  }),
}));

vi.mock('../lib/webAuth', () => ({
  fetchWithWebAuth: (...args: unknown[]) => mockFetchWithWebAuth(...args),
  hasWebSession: () => mockHasWebSession(),
  refreshWebSession: () => mockRefreshWebSession(),
  clearWebSession: vi.fn(),
}));

vi.mock('../lib/apiUrl', () => ({
  getApiBaseUrl: () => mockGetApiBaseUrl(),
}));

async function renderDashboardPageClient() {
  const mod = await import('./DashboardPageClient');
  return render(<mod.default />);
}

describe('DashboardPageClient', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockHasWebSession.mockReturnValue(true);
    mockRefreshWebSession.mockResolvedValue(true);
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
          documents_monthly_limit: 3000,
          documents_usage_mtd: 2347,
          documents_usage_series: [
            { date: '2026-04-01', cumulative: 100 },
            { date: '2026-04-15', cumulative: 1200 },
            { date: '2026-04-26', cumulative: 2347 },
          ],
          documents_usage_by_warehouse_mtd: [
            {
              warehouse_id: '11111111-1111-1111-1111-111111111111',
              name: 'Central',
              count: 1800,
            },
            {
              warehouse_id: '22222222-2222-2222-2222-222222222222',
              name: 'Norte',
              count: 547,
            },
          ],
          warehouse_usage_last_30_days: [],
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      );
    });
  });

  it('shows KPIs, charts, and invoices after entitlement loads', async () => {
    await renderDashboardPageClient();

    expect(screen.queryByRole('heading', { name: 'Facturación' })).not.toBeInTheDocument();
    expect(
      screen.queryByRole('heading', { name: 'Comprobantes de pago' }),
    ).not.toBeInTheDocument();

    expect(await screen.findByRole('heading', { name: 'Depósitos' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Dispositivos' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Documentos' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Plan actual' })).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Uso de documentos' }),
    ).toBeInTheDocument();
    expect(screen.getByText('2347 / 3000')).toBeInTheDocument();
    expect(screen.getByText('Documentos (mes en curso)')).toBeInTheDocument();
    expect(screen.getByText('Límite del plan')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
    expect(screen.getByText('12')).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Por depósito' }),
    ).toBeInTheDocument();
    expect(screen.getByText('Central')).toBeInTheDocument();
    expect(screen.getByText('Norte')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Facturas' })).toBeInTheDocument();
    expect(screen.getByText('Suscripción mensual')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Descargar factura/i })).toBeInTheDocument();

    expect(
      screen.queryByRole('heading', { name: 'Administración de cuenta' }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole('heading', { name: 'App en el depósito' }),
    ).not.toBeInTheDocument();
    expect(screen.getAllByText('En período de prueba').length).toBeGreaterThanOrEqual(1);
  });

  it('redirects to login when session is missing', async () => {
    mockHasWebSession.mockReturnValue(false);
    await renderDashboardPageClient();
    await waitFor(() => expect(mockReplace).toHaveBeenCalledWith('/login'));
  });

  it('fetches invoices for the dashboard invoice section', async () => {
    await renderDashboardPageClient();
    await screen.findByRole('heading', { name: 'Depósitos' });
    expect(mockFetchWithWebAuth).toHaveBeenCalledWith('/auth/me/entitlement');
    expect(mockFetchWithWebAuth).toHaveBeenCalledWith('/auth/me/invoices');
  });
});
