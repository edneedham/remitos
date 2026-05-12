import { render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const mockReplace = vi.fn();
const mockFetchWithWebAuth = vi.fn();
const mockPostWithWebAuth = vi.fn();
const mockGetApiBaseUrl = vi.fn();
const mockGetWebAccessToken = vi.fn();
const mockGetWebRefreshToken = vi.fn();
const mockDetectDevicePlatform = vi.fn();
const mockGetPublicSiteOrigin = vi.fn();

const { mockUsePanelBootstrap } = vi.hoisted(() => {
  const ent = { can_download_app: true, subscription_plan: 'trial' };
  return {
    mockUsePanelBootstrap: vi.fn(() => ({
      status: 'ready' as const,
      profile: {
        id: 'u1',
        username: 'admin',
        email: 'a@b.com',
        company_id: 'c1',
        company_name: 'Acme',
        company_code: 'ACME',
        role: 'admin',
      },
      errorMessage: null,
      entitlement: ent,
      refresh: vi.fn(),
    })),
  };
});

vi.mock('next/navigation', () => ({
  useRouter: () => ({
    replace: mockReplace,
  }),
}));

vi.mock('../../lib/webAuth', () => ({
  fetchWithWebAuth: (...args: unknown[]) => mockFetchWithWebAuth(...args),
  postWithWebAuth: (...args: unknown[]) => mockPostWithWebAuth(...args),
  isWebCookieSession: () => false,
  refreshWebSession: () => Promise.resolve(true),
  getWebAccessToken: () => mockGetWebAccessToken(),
  getWebRefreshToken: () => mockGetWebRefreshToken(),
  clearWebSession: vi.fn(),
}));

vi.mock('../lib/usePanelBootstrap', () => ({
  usePanelBootstrap: () => mockUsePanelBootstrap(),
}));

vi.mock('../../lib/apiUrl', () => ({
  getApiBaseUrl: () => mockGetApiBaseUrl(),
}));

vi.mock('../../lib/mobileDevice', () => ({
  detectDevicePlatform: () => mockDetectDevicePlatform(),
  isLikelyMobileDevice: () => mockDetectDevicePlatform() !== 'desktop',
}));

vi.mock('../../lib/siteUrl', () => ({
  getPublicSiteOrigin: () => mockGetPublicSiteOrigin(),
}));

async function renderApplicationPageClient() {
  const mod = await import('./ApplicationPageClient');
  return render(<mod.default />);
}

describe('ApplicationPageClient', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockGetApiBaseUrl.mockReturnValue('http://localhost:8080');
    mockFetchWithWebAuth.mockImplementation(async () => {
      return new Response(
        JSON.stringify({
          can_download_app: true,
          subscription_plan: 'trial',
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      );
    });
  });

  it('shows title and APK download on mobile', async () => {
    mockDetectDevicePlatform.mockReturnValue('android');

    await renderApplicationPageClient();

    expect(await screen.findByRole('heading', { name: 'Aplicación', level: 1 })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /descargar apk/i })).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Instalación en el teléfono (Android)' }),
    ).toBeInTheDocument();
  });
});

describe('ApplicationPageClient desktop QR transfer', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockGetWebAccessToken.mockReturnValue('access-token');
    mockGetWebRefreshToken.mockReturnValue('refresh-token');
    mockPostWithWebAuth.mockImplementation(
      async (path: string, body: unknown) => {
        return fetch(`http://localhost:8080${path}`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(body),
        });
      },
    );
    mockGetApiBaseUrl.mockReturnValue('http://localhost:8080');
    mockDetectDevicePlatform.mockReturnValue('desktop');
    mockGetPublicSiteOrigin.mockReturnValue('https://enpunto.com.ar');
    mockFetchWithWebAuth.mockImplementation(async () =>
      new Response(
        JSON.stringify({
          can_download_app: true,
          subscription_plan: 'trial',
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      ),
    );
  });

  it('starts transfer QR on desktop when entitlement allows download', async () => {
    const fetchMock = vi.fn(async () => ({
      ok: true,
      status: 200,
      json: async () => ({ token: 'random-token' }),
    }));
    vi.stubGlobal('fetch', fetchMock);

    await renderApplicationPageClient();

    expect(
      await screen.findByRole('button', { name: /generar nuevo qr/i }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Instalación en el teléfono (Android)' }),
    ).toBeInTheDocument();
    await waitFor(() =>
      expect(fetchMock).toHaveBeenCalledWith(
        'http://localhost:8080/auth/transfer/start',
        expect.objectContaining({
          method: 'POST',
        }),
      ),
    );
  });
});

describe('ApplicationPageClient iOS guard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockGetApiBaseUrl.mockReturnValue('http://localhost:8080');
    mockDetectDevicePlatform.mockReturnValue('ios');
    mockFetchWithWebAuth.mockImplementation(async () =>
      new Response(
        JSON.stringify({
          can_download_app: true,
          subscription_plan: 'trial',
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      ),
    );
  });

  it('does not show APK download action on iOS', async () => {
    await renderApplicationPageClient();

    expect(
      await screen.findByText(/está disponible para android/i),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: /descargar apk/i }),
    ).not.toBeInTheDocument();
  });
});
