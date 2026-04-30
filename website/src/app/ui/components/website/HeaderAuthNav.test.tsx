import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

let mockPathname = '/';
const mockPush = vi.fn();
const mockRefresh = vi.fn();
const mockHasWebSession = vi.fn();
const mockLogoutWebSession = vi.fn();
const mockFetchWebProfile = vi.fn();

vi.mock('next/navigation', () => ({
  usePathname: () => mockPathname,
  useRouter: () => ({
    push: mockPush,
    refresh: mockRefresh,
  }),
}));

vi.mock('../../../lib/webAuth', () => ({
  hasWebSession: () => mockHasWebSession(),
  logoutWebSession: () => mockLogoutWebSession(),
  fetchProfile: () => mockFetchWebProfile(),
}));

async function renderNav() {
  const mod = await import('./HeaderAuthNav');
  return render(<mod.default />);
}

describe('HeaderAuthNav', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockPathname = '/download';
    mockHasWebSession.mockReturnValue(false);
    mockFetchWebProfile.mockResolvedValue(null);
  });

  it('on dashboard, dropdown nav links use md:hidden (sidebar on desktop)', async () => {
    mockPathname = '/dashboard/billing';
    mockHasWebSession.mockReturnValue(true);
    mockFetchWebProfile.mockResolvedValue({
      username: 'owner_user',
      company_name: 'Mi Empresa SA',
      company_code: 'MIESA',
      role: 'company_owner',
    });

    const { container } = await renderNav();
    await waitFor(() => expect(mockFetchWebProfile).toHaveBeenCalled());

    fireEvent.click(screen.getByRole('button', { name: 'Abrir menú de empresa' }));

    const navGroup = container.querySelector('[role="menu"] > div');
    expect(navGroup).toHaveClass('md:hidden');
  });

  it('opens dropdown from company trigger and lists account links', async () => {
    mockHasWebSession.mockReturnValue(true);
    mockFetchWebProfile.mockResolvedValue({
      username: 'owner_user',
      company_name: 'Mi Empresa SA',
      company_code: 'MIESA',
      role: 'company_owner',
    });

    await renderNav();
    await waitFor(() => expect(mockFetchWebProfile).toHaveBeenCalled());

    fireEvent.click(screen.getByRole('button', { name: 'Abrir menú de empresa' }));

    expect(screen.getByRole('menu')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Panel' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Facturación' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Aplicación' })).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Cerrar sesión' }),
    ).toBeInTheDocument();
  });

  it('closes dropdown when clicking outside', async () => {
    mockHasWebSession.mockReturnValue(true);
    mockFetchWebProfile.mockResolvedValue({
      username: 'u',
      company_name: 'Mi Empresa SA',
      company_code: 'X',
      role: 'company_owner',
    });

    await renderNav();
    await waitFor(() => expect(mockFetchWebProfile).toHaveBeenCalled());

    fireEvent.click(screen.getByRole('button', { name: 'Abrir menú de empresa' }));
    expect(screen.getByRole('menu')).toBeInTheDocument();

    fireEvent.mouseDown(document.body);

    await waitFor(() =>
      expect(screen.queryByRole('menu')).not.toBeInTheDocument(),
    );
  });

  it('logs out from dropdown action', async () => {
    mockHasWebSession.mockReturnValue(true);
    mockFetchWebProfile.mockResolvedValue({
      username: 'u',
      company_name: 'Mi Empresa SA',
      company_code: 'X',
      role: 'company_owner',
    });
    mockLogoutWebSession.mockResolvedValue(undefined);

    await renderNav();
    await waitFor(() => expect(mockFetchWebProfile).toHaveBeenCalled());

    fireEvent.click(screen.getByRole('button', { name: 'Abrir menú de empresa' }));
    fireEvent.click(screen.getByRole('button', { name: 'Cerrar sesión' }));

    await waitFor(() => expect(mockLogoutWebSession).toHaveBeenCalledTimes(1));
    expect(mockPush).toHaveBeenCalledWith('/');
    expect(mockRefresh).toHaveBeenCalledTimes(1);
  });

  it('does not fetch profile when session is missing', async () => {
    mockHasWebSession.mockReturnValue(false);

    await renderNav();

    expect(mockFetchWebProfile).not.toHaveBeenCalled();
    expect(
      screen.getByRole('link', { name: 'Iniciar sesión' }),
    ).toBeInTheDocument();
  });

  it('shows placeholder when profile cannot be loaded', async () => {
    mockHasWebSession.mockReturnValue(true);
    mockFetchWebProfile.mockResolvedValue(null);

    await renderNav();

    await waitFor(() => expect(mockFetchWebProfile).toHaveBeenCalled());
    expect(screen.getByText('—')).toBeInTheDocument();
  });
});
