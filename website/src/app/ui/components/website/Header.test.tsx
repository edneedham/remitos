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

vi.mock('next/link', () => ({
  default: ({ href, children, ...props }: any) => (
    <a href={href} {...props}>
      {children}
    </a>
  ),
}));

vi.mock('next/image', () => ({
  default: (props: any) => <img alt={props.alt} />,
}));

vi.mock('../../../lib/webAuth', () => ({
  hasWebSession: () => mockHasWebSession(),
  logoutWebSession: () => mockLogoutWebSession(),
  fetchWebProfile: () => mockFetchWebProfile(),
}));

async function renderHeader() {
  const mod = await import('./Header');
  return render(<mod.default />);
}

describe('Header account menu', () => {
  beforeEach(() => {
    vi.resetModules();
    vi.clearAllMocks();
    mockPathname = '/';
    mockHasWebSession.mockReturnValue(false);
    mockFetchWebProfile.mockResolvedValue(null);
  });

  it('shows company code trigger on /account and hides account links', async () => {
    mockPathname = '/account';
    mockHasWebSession.mockReturnValue(true);
    mockFetchWebProfile.mockResolvedValue({
      username: 'owner_user',
      company_name: 'Mi Empresa SA',
      company_code: 'MIESA',
    });

    await renderHeader();

    await waitFor(() =>
      expect(mockFetchWebProfile).toHaveBeenCalledTimes(1),
    );
    expect(await screen.findByText('Mi Empresa SA')).toBeInTheDocument();
    expect(
      screen.queryByRole('link', { name: 'Mi cuenta' }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole('link', { name: 'Descargar app' }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: 'Salir' }),
    ).not.toBeInTheDocument();
  });

  it('opens dropdown from company code chevron trigger', async () => {
    mockPathname = '/account';
    mockHasWebSession.mockReturnValue(true);
    mockFetchWebProfile.mockResolvedValue({
      username: 'owner_user',
      company_name: 'Mi Empresa SA',
      company_code: 'MIESA',
    });

    await renderHeader();
    await waitFor(() =>
      expect(mockFetchWebProfile).toHaveBeenCalledTimes(1),
    );
    fireEvent.click(screen.getByRole('button', { name: 'Abrir menú de empresa' }));

    expect(screen.getByRole('menu')).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Abrir menú de empresa' }),
    ).toHaveTextContent('Mi Empresa SA');
    expect(screen.getByRole('link', { name: 'Descargar app' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Mi cuenta' })).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Cerrar sesión' }),
    ).toBeInTheDocument();
  });

  it('closes dropdown when clicking outside', async () => {
    mockPathname = '/account';
    mockHasWebSession.mockReturnValue(true);
    mockFetchWebProfile.mockResolvedValue({
      username: 'owner_user',
      company_name: 'Mi Empresa SA',
      company_code: 'MIESA',
    });

    await renderHeader();
    await waitFor(() =>
      expect(mockFetchWebProfile).toHaveBeenCalledTimes(1),
    );

    fireEvent.click(screen.getByRole('button', { name: 'Abrir menú de empresa' }));
    expect(screen.getByRole('menu')).toBeInTheDocument();

    fireEvent.mouseDown(document.body);

    await waitFor(() =>
      expect(screen.queryByRole('menu')).not.toBeInTheDocument(),
    );
  });

  it('logs out from dropdown action', async () => {
    mockPathname = '/account';
    mockHasWebSession.mockReturnValue(true);
    mockFetchWebProfile.mockResolvedValue({
      username: 'owner_user',
      company_name: 'Mi Empresa SA',
      company_code: 'MIESA',
    });
    mockLogoutWebSession.mockResolvedValue(undefined);

    await renderHeader();
    await waitFor(() =>
      expect(mockFetchWebProfile).toHaveBeenCalledTimes(1),
    );

    fireEvent.click(screen.getByRole('button', { name: 'Abrir menú de empresa' }));
    fireEvent.click(screen.getByRole('button', { name: 'Cerrar sesión' }));

    await waitFor(() =>
      expect(mockLogoutWebSession).toHaveBeenCalledTimes(1),
    );
    expect(mockPush).toHaveBeenCalledWith('/');
    expect(mockRefresh).toHaveBeenCalledTimes(1);
  });

  it('fetches profile on non-account routes and shows account menu trigger', async () => {
    mockPathname = '/download';
    mockHasWebSession.mockReturnValue(true);
    mockFetchWebProfile.mockResolvedValue({
      username: 'u',
      company_name: 'Co',
      company_code: 'X',
    });

    await renderHeader();

    await waitFor(() =>
      expect(mockFetchWebProfile).toHaveBeenCalledTimes(1),
    );
    expect(
      screen.getByRole('button', { name: 'Abrir menú de empresa' }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole('link', { name: 'Descargar app' }),
    ).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Mi cuenta' })).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Abrir menú de empresa' }));
    expect(screen.getByRole('link', { name: 'Descargar app' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Mi cuenta' })).toBeInTheDocument();
  });

  it('does not fetch profile and keeps public nav when session is missing', async () => {
    mockPathname = '/account';
    mockHasWebSession.mockReturnValue(false);

    await renderHeader();

    expect(mockFetchWebProfile).not.toHaveBeenCalled();
    expect(
      screen.queryByRole('button', { name: 'Abrir menú de empresa' }),
    ).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Iniciar sesión' })).toBeInTheDocument();
  });

  it('shows placeholder when profile cannot be loaded', async () => {
    mockPathname = '/account/settings';
    mockHasWebSession.mockReturnValue(true);
    mockFetchWebProfile.mockResolvedValue(null);

    await renderHeader();

    await waitFor(() =>
      expect(mockFetchWebProfile).toHaveBeenCalledTimes(1),
    );
    expect(screen.getByText('—')).toBeInTheDocument();
  });
});
