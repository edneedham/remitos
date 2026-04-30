import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

vi.mock('next/link', () => ({
  default: ({ href, children, ...props }: any) => (
    <a href={href} {...props}>
      {children}
    </a>
  ),
}));

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
    expect(trialButtons[0]).toHaveAttribute('href', '/signup?plan=pyme');
    expect(trialButtons[1]).toHaveAttribute('href', '/signup?plan=empresa');
    expect(
      screen.getByRole('link', { name: /hablar con ventas/i }),
    ).toHaveAttribute('href', '/contact');
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
