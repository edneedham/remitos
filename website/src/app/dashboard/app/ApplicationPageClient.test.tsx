import { render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const mockReplace = vi.fn();
const mockFetchWithWebAuth = vi.fn();
const mockHasWebSession = vi.fn();
const mockRefreshWebSession = vi.fn();
const mockGetApiBaseUrl = vi.fn();
const mockGetWebAccessToken = vi.fn();
const mockGetWebRefreshToken = vi.fn();
const mockIsLikelyMobileDevice = vi.fn();
const mockGetPublicSiteOrigin = vi.fn();
const mockFetchWebProfile = vi.fn();
const mockCanAccessWebManagement = vi.fn();

vi.mock('next/navigation', () => ({
  useRouter: () => ({
    replace: mockReplace,
  }),
}));

vi.mock('../../lib/webAuth', () => ({
  fetchWithWebAuth: (...args: unknown[]) => mockFetchWithWebAuth(...args),
  hasWebSession: () => mockHasWebSession(),
  refreshWebSession: () => mockRefreshWebSession(),
  getWebAccessToken: () => mockGetWebAccessToken(),
  getWebRefreshToken: () => mockGetWebRefreshToken(),
  fetchProfile: () => mockFetchWebProfile(),
  canAccessWebManagement: (...args: unknown[]) =>
    mockCanAccessWebManagement(...args),
  clearWebSession: vi.fn(),
}));

vi.mock('../../lib/apiUrl', () => ({
  getApiBaseUrl: () => mockGetApiBaseUrl(),
}));

vi.mock('../../lib/mobileDevice', () => ({
  isLikelyMobileDevice: () => mockIsLikelyMobileDevice(),
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
    mockHasWebSession.mockReturnValue(true);
    mockRefreshWebSession.mockResolvedValue(true);
    mockGetApiBaseUrl.mockReturnValue('http://localhost:8080');
    mockFetchWebProfile.mockResolvedValue({ role: 'admin' });
    mockCanAccessWebManagement.mockReturnValue(true);
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
    mockIsLikelyMobileDevice.mockReturnValue(true);

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
    mockHasWebSession.mockReturnValue(true);
    mockRefreshWebSession.mockResolvedValue(true);
    mockGetWebAccessToken.mockReturnValue('access-token');
    mockGetWebRefreshToken.mockReturnValue('refresh-token');
    mockGetApiBaseUrl.mockReturnValue('http://localhost:8080');
    mockIsLikelyMobileDevice.mockReturnValue(false);
    mockGetPublicSiteOrigin.mockReturnValue('https://enpunto.com.ar');
    mockFetchWebProfile.mockResolvedValue({ role: 'admin' });
    mockCanAccessWebManagement.mockReturnValue(true);
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
