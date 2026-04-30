import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const mockPush = vi.fn();
const mockReplace = vi.fn();
const mockSearchParamGet = vi.fn();
const mockGetApiBaseUrl = vi.fn();
const mockGetWebAccessToken = vi.fn();

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
  getWebAccessToken: () => mockGetWebAccessToken(),
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

vi.mock('./SignupTrialForm', () => ({
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
    mockGetWebAccessToken.mockReturnValue('access-token');
    vi.stubGlobal('fetch', vi.fn());
  });

  it('auto-applies preselected plan and redirects to dashboard', async () => {
    mockSearchParamGet.mockImplementation((k: string) =>
      k === 'plan' ? 'pyme' : null,
    );
    const fetchMock = vi.fn(async () => ({ ok: true }));
    vi.stubGlobal('fetch', fetchMock);

    await renderSignupGate();
    fireEvent.click(screen.getByRole('button', { name: 'Completar registro' }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
    const [url, req] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe('http://localhost:8080/auth/me/plan');
    expect(req.method).toBe('POST');
    expect(req.headers).toEqual(
      expect.objectContaining({
        Authorization: 'Bearer access-token',
      }),
    );
    expect(mockPush).toHaveBeenCalledWith('/dashboard');
  });

  it('falls back to plan selector when preselected apply fails', async () => {
    mockSearchParamGet.mockImplementation((k: string) =>
      k === 'plan' ? 'empresa' : null,
    );
    const fetchMock = vi.fn(async () => ({ ok: false }));
    vi.stubGlobal('fetch', fetchMock);

    await renderSignupGate();
    fireEvent.click(screen.getByRole('button', { name: 'Completar registro' }));

    await waitFor(() =>
      expect(screen.getByTestId('signup-plan-selector')).toBeInTheDocument(),
    );
    expect(mockPush).not.toHaveBeenCalledWith('/dashboard');
  });

  it('shows plan selector when no preselected plan is present', async () => {
    await renderSignupGate();
    fireEvent.click(screen.getByRole('button', { name: 'Completar registro' }));

    await waitFor(() =>
      expect(screen.getByTestId('signup-plan-selector')).toBeInTheDocument(),
    );
  });
});
