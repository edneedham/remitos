import { render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

let mockPathname = '/';
const mockPush = vi.fn();
const mockRefresh = vi.fn();
const mockHasWebSession = vi.fn();
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
  logoutWebSession: () => vi.fn(),
  fetchWebProfile: () => mockFetchWebProfile(),
}));

async function renderHeader() {
  const mod = await import('./Header');
  return render(<mod.default />);
}

describe('Header', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockPathname = '/';
    mockHasWebSession.mockReturnValue(false);
    mockFetchWebProfile.mockResolvedValue(null);
  });

  it('renders nothing on /dashboard (dashboard supplies its own chrome)', async () => {
    mockPathname = '/dashboard';

    const { container } = await renderHeader();

    expect(container.firstChild).toBeNull();
  });

  it('renders nothing for nested dashboard routes', async () => {
    mockPathname = '/dashboard/billing';

    const { container } = await renderHeader();

    expect(container.firstChild).toBeNull();
  });

  it('fetches profile on non-account routes and shows account menu trigger', async () => {
    mockPathname = '/download';
    mockHasWebSession.mockReturnValue(true);
    mockFetchWebProfile.mockResolvedValue({
      username: 'u',
      company_name: 'Co',
      company_code: 'X',
      role: 'company_owner',
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
    expect(screen.queryByRole('link', { name: 'Panel' })).not.toBeInTheDocument();
  });
});
