import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

vi.mock('next/link', () => ({
  default: ({ href, children, ...props }: any) => (
    <a href={href} {...props}>
      {children}
    </a>
  ),
}));

afterEach(() => {
  vi.unstubAllEnvs();
});

async function renderPricingPlans(showCtas = false) {
  const mod = await import('./PricingPlansSection');
  return render(<mod.default showCtas={showCtas} />);
}

describe('PricingPlansSection', () => {
  it('renders signup links with preselected plan query params in full mode', async () => {
    await renderPricingPlans(true);

    const trialButtons = screen.getAllByRole('link', {
      name: /comenzar prueba gratis/i,
    });
    expect(trialButtons).toHaveLength(2);
    expect(trialButtons[0]).toHaveAttribute('href', '/registro?plan=pyme');
    expect(trialButtons[1]).toHaveAttribute('href', '/registro?plan=empresa');
    expect(
      screen.getByRole('link', { name: /hablar con ventas/i }),
    ).toHaveAttribute('href', '/contacto');
  });

  it('uses lista de espera CTAs and copy when waitlist mode is enabled', async () => {
    vi.stubEnv('NEXT_PUBLIC_WAITLIST_ONLY', 'true');
    vi.resetModules();
    const mod = await import('./PricingPlansSection');
    render(<mod.default showCtas={true} />);

    expect(
      screen.getByRole('heading', { name: /planes y precios de referencia/i }),
    ).toBeInTheDocument();

    const waitlistLinks = screen.getAllByRole('link', {
      name: /lista de espera/i,
    });
    expect(waitlistLinks).toHaveLength(2);
    expect(waitlistLinks[0]).toHaveAttribute(
      'href',
      '/lista-de-espera?plan=pyme',
    );
    expect(waitlistLinks[1]).toHaveAttribute(
      'href',
      '/lista-de-espera?plan=empresa',
    );
  });

  it('does not render CTA links in summary mode', async () => {
    await renderPricingPlans(false);

    expect(
      screen.queryByRole('link', { name: /comenzar prueba gratis/i }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole('link', { name: /hablar con ventas/i }),
    ).not.toBeInTheDocument();
  });
});
