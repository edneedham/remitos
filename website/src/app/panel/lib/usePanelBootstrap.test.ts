import { createElement, type ReactNode } from 'react';
import { renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const mockReplace = vi.fn();

vi.mock('next/navigation', () => ({
  useRouter: () => ({ replace: mockReplace }),
}));

const mockRefreshWebSession = vi.fn();
const mockFetchProfile = vi.fn();
const mockHasWebSession = vi.fn();
const mockGetApiBaseUrl = vi.fn();
const mockFetchWithWebAuth = vi.fn();

vi.mock('../../lib/apiUrl', () => ({
  getApiBaseUrl: () => mockGetApiBaseUrl(),
}));

vi.mock('../../lib/webAuth', () => ({
  hasWebSession: () => mockHasWebSession(),
  refreshWebSession: () => mockRefreshWebSession(),
  fetchProfile: () => mockFetchProfile(),
  fetchWithWebAuth: (path: string) => mockFetchWithWebAuth(path),
  canAccessWebManagement: (role: string) =>
    role === 'company_owner' || role === 'admin',
  clearWebSession: vi.fn(),
}));

import { PanelBootstrapProvider } from './PanelBootstrapContext';
import { usePanelBootstrap } from './usePanelBootstrap';

function bootstrapWrapper({ children }: { children: ReactNode }) {
  return createElement(PanelBootstrapProvider, null, children);
}

describe('usePanelBootstrap', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockGetApiBaseUrl.mockReturnValue('http://localhost:8080');
    mockHasWebSession.mockReturnValue(true);
    mockRefreshWebSession.mockResolvedValue(undefined);
    mockFetchProfile.mockResolvedValue({
      id: 'u1',
      username: 'u',
      company_id: 'c1',
      company_name: 'Co',
      company_code: 'CO',
      role: 'company_owner',
    });
    mockFetchWithWebAuth.mockImplementation(async (path: string) => {
      if (path === '/auth/me/entitlement') {
        return new Response(JSON.stringify({ can_download_app: true }), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        });
      }
      return new Response(null, { status: 404 });
    });
  });

  it('reaches ready with a valid profile and entitlement', async () => {
    const { result } = renderHook(() => usePanelBootstrap(), {
      wrapper: bootstrapWrapper,
    });
    await waitFor(() => {
      expect(result.current.status).toBe('ready');
    });
    expect(result.current.profile?.username).toBe('u');
    expect(result.current.errorMessage).toBeNull();
    expect(result.current.entitlement?.can_download_app).toBe(true);
    expect(mockRefreshWebSession).not.toHaveBeenCalled();
  });

  it('sets config_error when API URL is missing', async () => {
    mockGetApiBaseUrl.mockReturnValue('');
    const { result } = renderHook(() => usePanelBootstrap(), {
      wrapper: bootstrapWrapper,
    });
    await waitFor(() => {
      expect(result.current.status).toBe('config_error');
    });
    expect(result.current.errorMessage).toContain('NEXT_PUBLIC_API_URL');
  });

  it('sets forbidden when profile fails Zod validation', async () => {
    mockFetchProfile.mockResolvedValue({ id: 'only-id' });
    const { result } = renderHook(() => usePanelBootstrap(), {
      wrapper: bootstrapWrapper,
    });
    await waitFor(() => {
      expect(result.current.status).toBe('forbidden');
    });
  });
});
