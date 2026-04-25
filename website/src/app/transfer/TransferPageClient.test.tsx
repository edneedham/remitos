import { render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const mockReplace = vi.fn();
const mockRefresh = vi.fn();
const mockSaveWebSession = vi.fn();
const mockGetApiBaseUrl = vi.fn();

vi.mock('next/navigation', () => ({
  useRouter: () => ({
    replace: mockReplace,
    refresh: mockRefresh,
  }),
}));

vi.mock('../lib/apiUrl', () => ({
  getApiBaseUrl: () => mockGetApiBaseUrl(),
}));

vi.mock('../lib/webAuth', () => ({
  saveWebSession: (...args: unknown[]) => mockSaveWebSession(...args),
}));

async function renderTransferClient(token: string) {
  const mod = await import('./TransferPageClient');
  return render(<mod.default token={token} />);
}

describe('TransferPageClient', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockGetApiBaseUrl.mockReturnValue('http://localhost:8080');
    vi.stubGlobal('fetch', vi.fn());
  });

  it('shows an error when token is missing', async () => {
    await renderTransferClient('');
    expect(
      await screen.findByText('El enlace de transferencia no es válido.'),
    ).toBeInTheDocument();
  });

  it('claims transfer, saves session and redirects', async () => {
    const fetchMock = vi.fn(async () => ({
      ok: true,
      json: async () => ({
        token: 'access-token',
        refresh_token: 'refresh-token',
      }),
    }));
    vi.stubGlobal('fetch', fetchMock);

    await renderTransferClient('ABC123');

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
    expect(mockSaveWebSession).toHaveBeenCalledWith(
      'access-token',
      'refresh-token',
    );
    expect(mockReplace).toHaveBeenCalledWith('/download');
    expect(mockRefresh).toHaveBeenCalledTimes(1);
  });

  it('shows backend message when transfer claim fails', async () => {
    const fetchMock = vi.fn(async () => ({
      ok: false,
      json: async () => ({
        message: 'Token de transferencia inválido o expirado',
      }),
    }));
    vi.stubGlobal('fetch', fetchMock);

    await renderTransferClient('ABC123');

    expect(
      await screen.findByText('Token de transferencia inválido o expirado'),
    ).toBeInTheDocument();
    expect(mockSaveWebSession).not.toHaveBeenCalled();
  });

  it('shows api-url error when NEXT_PUBLIC_API_URL is missing', async () => {
    mockGetApiBaseUrl.mockReturnValue('');

    await renderTransferClient('ABC123');

    expect(
      await screen.findByText(
        'Falta configurar NEXT_PUBLIC_API_URL (URL del servidor de la API).',
      ),
    ).toBeInTheDocument();
    expect(mockSaveWebSession).not.toHaveBeenCalled();
  });
});
