import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const mockFetchWithWebAuth = vi.fn();

const { mockUsePanelBootstrap } = vi.hoisted(() => ({
  mockUsePanelBootstrap: vi.fn(() => ({
    status: 'ready' as const,
    profile: {
      id: '11111111-1111-1111-1111-111111111111',
      username: 'owner',
      company_id: '22222222-2222-2222-2222-222222222222',
      company_name: 'Acme',
      company_code: 'ACME',
      role: 'admin',
    },
    errorMessage: null,
    refresh: vi.fn(),
  })),
}));

vi.mock('next/navigation', () => ({
  useRouter: () => ({
    replace: vi.fn(),
  }),
}));

vi.mock('../lib/webAuth', () => ({
  fetchWithWebAuth: (...args: unknown[]) => mockFetchWithWebAuth(...args),
  canManageBillingSubscriptions: () => true,
  clearWebSession: vi.fn(),
}));

vi.mock('./lib/usePanelBootstrap', () => ({
  usePanelBootstrap: () => mockUsePanelBootstrap(),
}));

async function renderBillingPageClient() {
  const mod = await import('./BillingPageClient');
  return render(<mod.default />);
}

describe('BillingPageClient', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUsePanelBootstrap.mockReturnValue({
      status: 'ready' as const,
      profile: {
        id: '11111111-1111-1111-1111-111111111111',
        username: 'owner',
        company_id: '22222222-2222-2222-2222-222222222222',
        company_name: 'Acme',
        company_code: 'ACME',
        role: 'admin',
      },
      errorMessage: null,
      refresh: vi.fn(),
    });
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
    ).toHaveAttribute('href', '/panel/facturacion/mejorar-plan');
    expect(
      screen.getByRole('heading', { name: 'Comprobantes de pago' }),
    ).toBeInTheDocument();
    expect(screen.getAllByText('Suscripción mensual').length).toBeGreaterThanOrEqual(
      1,
    );
    expect(screen.getAllByText('Pagado').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('En período de prueba').length).toBeGreaterThanOrEqual(
      1,
    );
  });

});
