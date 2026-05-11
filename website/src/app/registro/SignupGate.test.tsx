import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const mockPush = vi.fn();
const mockReplace = vi.fn();
const mockSearchParamGet = vi.fn();
const mockGetApiBaseUrl = vi.fn();
const mockHasWebSession = vi.fn();
const mockPostWithWebAuth = vi.fn();

vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: mockPush,
    replace: mockReplace,
  }),
  useSearchParams: () => ({
    get: mockSearchParamGet,
  }),
}));

vi.mock('../lib/mobileDevice', () => ({
  isLikelyMobileDevice: () => false,
}));

vi.mock('../lib/siteUrl', () => ({
  getPublicSiteOrigin: () => 'https://enpunto.test',
}));

vi.mock('../lib/apiUrl', () => ({
  getApiBaseUrl: () => mockGetApiBaseUrl(),
}));

vi.mock('../lib/webAuth', () => ({
  hasWebSession: () => mockHasWebSession(),
  postWithWebAuth: (...args: unknown[]) => mockPostWithWebAuth(...args),
}));

vi.mock('qrcode.react', () => ({
  QRCodeSVG: () => <div data-testid="signup-qr" />,
}));

vi.mock('./SignupMarketingAside', () => ({
  default: () => <div data-testid="signup-marketing-aside" />,
}));

vi.mock('./SignupPlanSelector', () => ({
  default: () => <div data-testid="signup-plan-selector">Plan selector</div>,
}));

vi.mock('./SignupForm', () => ({
  default: ({ onSignupSuccess }: { onSignupSuccess?: () => Promise<void> | void }) => (
    <button
      type="button"
      onClick={() => {
        if (onSignupSuccess) void onSignupSuccess();
      }}
    >
      Completar registro
    </button>
  ),
}));

async function renderSignupGate() {
  const mod = await import('./SignupGate');
  return render(<mod.default />);
}

describe('SignupGate', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockSearchParamGet.mockReturnValue(null);
    mockGetApiBaseUrl.mockReturnValue('http://localhost:8080');
    mockHasWebSession.mockReturnValue(true);
    mockPostWithWebAuth.mockResolvedValue(new Response(null, { status: 200 }));
  });

  it('auto-applies preselected plan and redirects to trial onboarding', async () => {
    mockSearchParamGet.mockImplementation((k: string) =>
      k === 'plan' ? 'pyme' : null,
    );

    await renderSignupGate();
    fireEvent.click(
      await screen.findByRole('button', { name: 'Completar registro' }),
    );

    await waitFor(() => expect(mockPostWithWebAuth).toHaveBeenCalledTimes(1));
    expect(mockPostWithWebAuth).toHaveBeenCalledWith(
      '/auth/me/plan',
      expect.objectContaining({
        plan_id: 'pyme',
      }),
    );
    expect(mockPush).toHaveBeenCalledWith('/prueba-iniciada');
  });

  it('falls back to plan selector when preselected apply fails', async () => {
    mockSearchParamGet.mockImplementation((k: string) =>
      k === 'plan' ? 'empresa' : null,
    );
    mockPostWithWebAuth.mockResolvedValue(new Response(null, { status: 500 }));

    await renderSignupGate();
    fireEvent.click(
      await screen.findByRole('button', { name: 'Completar registro' }),
    );

    await waitFor(() =>
      expect(screen.getByTestId('signup-plan-selector')).toBeInTheDocument(),
    );
    expect(mockPush).not.toHaveBeenCalledWith('/prueba-iniciada');
  });

  it('shows plan selector when no preselected plan is present', async () => {
    await renderSignupGate();
    fireEvent.click(
      await screen.findByRole('button', { name: 'Completar registro' }),
    );

    await waitFor(() =>
      expect(screen.getByTestId('signup-plan-selector')).toBeInTheDocument(),
    );
  });
});
