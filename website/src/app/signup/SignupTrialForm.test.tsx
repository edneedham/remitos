import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const routerPush = vi.fn();
const routerRefresh = vi.fn();

vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: routerPush,
    refresh: routerRefresh,
  }),
}));

vi.mock('@mercadopago/sdk-react', () => ({
  initMercadoPago: vi.fn(),
  getIdentificationTypes: vi.fn(async () => [{ id: 'DNI', name: 'DNI' }]),
  createCardToken: vi.fn(async () => ({ id: 'tok_test' })),
  CardNumber: () => <div data-testid="card-number-field" />,
  ExpirationDate: () => <div data-testid="expiration-date-field" />,
  SecurityCode: () => <div data-testid="security-code-field" />,
}));

async function renderSignupTrialForm() {
  const mod = await import('./SignupTrialForm');
  return render(<mod.default />);
}

async function fillAccountSection(user: ReturnType<typeof userEvent.setup>) {
  fireEvent.change(screen.getByLabelText('Nombre de la empresa'), {
    target: { value: 'Mi Empresa SA' },
  });
  fireEvent.change(screen.getByLabelText('Código de empresa'), {
    target: { value: 'MIESA' },
  });
  fireEvent.change(screen.getByLabelText('Correo electrónico'), {
    target: { value: 'owner@example.com' },
  });
  fireEvent.change(screen.getByLabelText('Contraseña'), {
    target: { value: 'password123' },
  });
  fireEvent.change(screen.getByLabelText('Confirmar contraseña'), {
    target: { value: 'password123' },
  });
  await user.tab();
}

describe('SignupTrialForm', () => {
  beforeEach(() => {
    vi.resetModules();
    vi.unstubAllEnvs();
    vi.clearAllMocks();
    routerPush.mockReset();
    routerRefresh.mockReset();
    vi.stubEnv('NEXT_PUBLIC_MERCADOPAGO_PUBLIC_KEY', 'TEST-public-key');
    vi.stubEnv('NEXT_PUBLIC_API_URL', 'http://localhost:8080');
    vi.stubEnv('NEXT_PUBLIC_SIGNUP_USE_MOCK_PAYMENT', 'false');
    vi.stubGlobal('fetch', vi.fn());
    Object.defineProperty(window.HTMLElement.prototype, 'scrollIntoView', {
      value: vi.fn(),
      writable: true,
    });
  });

  it('blocks submit and shows payment error when billing details are missing (real mode)', async () => {
    const user = userEvent.setup();
    await renderSignupTrialForm();
    await fillAccountSection(user);

    await user.click(screen.getByRole('button', { name: /mostrar datos de pago/i }));
    await user.click(screen.getByRole('button', { name: /crear cuenta/i }));

    expect(
      await screen.findByText('Completá los datos de pago.'),
    ).toBeInTheDocument();
  });

  it('blocks submit in mock mode until cardholder and document are filled', async () => {
    vi.stubEnv('NEXT_PUBLIC_SIGNUP_USE_MOCK_PAYMENT', 'true');
    const user = userEvent.setup();
    await renderSignupTrialForm();
    await fillAccountSection(user);

    await user.click(screen.getByRole('button', { name: /mostrar datos de pago/i }));
    await user.click(screen.getByRole('button', { name: /crear cuenta/i }));
    expect(
      await screen.findByText('Completá los datos de pago.'),
    ).toBeInTheDocument();
  });

  it('submits tokenized card in real mode (not mock token)', async () => {
    const user = userEvent.setup();
    const sdk = await import('@mercadopago/sdk-react');
    const fetchMock = vi.fn(async () => ({
      ok: true,
      json: async () => ({
        trial_ends_at: '',
        company_code: 'MIESA',
        token: 'access-token',
        refresh_token: 'refresh-token',
      }),
    }));
    vi.stubGlobal('fetch', fetchMock);

    await renderSignupTrialForm();
    await fillAccountSection(user);

    await user.click(screen.getByRole('button', { name: /mostrar datos de pago/i }));
    await user.type(screen.getByLabelText('Nombre del titular'), 'Owner Example');
    await user.type(screen.getByLabelText('Número'), '12345678');

    await user.click(screen.getByRole('button', { name: /crear cuenta/i }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
    expect(sdk.createCardToken).toHaveBeenCalledTimes(1);

    const [, req] = fetchMock.mock.calls[0] as [string, RequestInit];
    const payload = JSON.parse(String(req.body)) as { card_token?: string };
    expect(payload.card_token).toBe('tok_test');
    expect(payload.card_token).not.toBe('mock_card_token');
    expect(routerPush).toHaveBeenCalledWith('/dashboard');
  });
});
