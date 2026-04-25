import { render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const mockReplace = vi.fn();
const mockFetchWithWebAuth = vi.fn();
const mockHasWebSession = vi.fn();
const mockRefreshWebSession = vi.fn();
const mockGetWebAccessToken = vi.fn();
const mockGetWebRefreshToken = vi.fn();
const mockGetApiBaseUrl = vi.fn();
const mockIsLikelyMobileDevice = vi.fn();
const mockGetPublicSiteOrigin = vi.fn();

vi.mock('next/navigation', () => ({
  useRouter: () => ({
    replace: mockReplace,
  }),
}));

vi.mock('../lib/webAuth', () => ({
  fetchWithWebAuth: (...args: unknown[]) => mockFetchWithWebAuth(...args),
  hasWebSession: () => mockHasWebSession(),
  refreshWebSession: () => mockRefreshWebSession(),
  getWebAccessToken: () => mockGetWebAccessToken(),
  getWebRefreshToken: () => mockGetWebRefreshToken(),
  clearWebSession: vi.fn(),
}));

vi.mock('../lib/apiUrl', () => ({
  getApiBaseUrl: () => mockGetApiBaseUrl(),
}));

vi.mock('../lib/mobileDevice', () => ({
  isLikelyMobileDevice: () => mockIsLikelyMobileDevice(),
}));

vi.mock('../lib/siteUrl', () => ({
  getPublicSiteOrigin: () => mockGetPublicSiteOrigin(),
}));

async function renderDownloadPageClient() {
  const mod = await import('./DownloadPageClient');
  return render(<mod.default />);
}

// Session-transfer UI is not on this branch yet; unskip when merged.
describe.skip('DownloadPageClient desktop QR transfer', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockHasWebSession.mockReturnValue(true);
    mockRefreshWebSession.mockResolvedValue(true);
    mockGetWebAccessToken.mockReturnValue('access-token');
    mockGetWebRefreshToken.mockReturnValue('refresh-token');
    mockGetApiBaseUrl.mockReturnValue('http://localhost:8080');
    mockIsLikelyMobileDevice.mockReturnValue(false);
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

  it('shows transfer QR URL on desktop when entitlement allows download', async () => {
    const fetchMock = vi.fn(async () => ({
      ok: true,
      status: 200,
      json: async () => ({ token: 'random-token' }),
    }));
    vi.stubGlobal('fetch', fetchMock);

    await renderDownloadPageClient();

    expect(
      await screen.findByText('Escaneá este QR desde tu teléfono Android'),
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
