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
    vi.stubEnv('NEXT_PUBLIC_API_URL', 'http://localhost:8080');
    vi.stubGlobal('fetch', vi.fn());
    Object.defineProperty(window.HTMLElement.prototype, 'scrollIntoView', {
      value: vi.fn(),
      writable: true,
    });
  });

  it('submits signup with account fields only', async () => {
    const user = userEvent.setup();
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

    await user.click(screen.getByRole('button', { name: /crear cuenta/i }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
    const [, req] = fetchMock.mock.calls[0] as [string, RequestInit];
    const payload = JSON.parse(String(req.body)) as { card_token?: string };
    expect(payload.card_token).toBeUndefined();
    expect(routerPush).toHaveBeenCalledWith('/dashboard');
  });

  it('shows validation errors when required account fields are missing', async () => {
    const user = userEvent.setup();
    await renderSignupTrialForm();
    await user.click(screen.getByRole('button', { name: /crear cuenta/i }));

    expect(
      await screen.findByText('Ingresá el nombre de la empresa.'),
    ).toBeInTheDocument();
    expect(screen.getByText('Ingresá el código de empresa.')).toBeInTheDocument();
    expect(screen.getByText('Ingresá tu correo electrónico.')).toBeInTheDocument();
  });
});
