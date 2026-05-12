import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import Button from './Button';
import StatusBanner from './StatusBanner';
import TextField from './TextField';

describe('shared/Button', () => {
  it('renders a primary button with brand classes by default', () => {
    render(<Button>Iniciar sesión</Button>);
    const button = screen.getByRole('button', { name: /iniciar sesión/i });
    expect(button.className).toContain('bg-blue-600');
    expect(button).not.toBeDisabled();
  });

  it('disables itself and shows a spinner when isLoading is true', () => {
    render(<Button isLoading>Enviar</Button>);
    const button = screen.getByRole('button');
    expect(button).toBeDisabled();
    expect(button.textContent).toBe('');
  });

  it('applies disabled tokens when disabled', () => {
    render(<Button disabled>Guardar</Button>);
    const button = screen.getByRole('button', { name: /guardar/i });
    expect(button).toBeDisabled();
    expect(button.className).toContain('disabled:bg-(--color-button-disabled-bg)');
  });
});

describe('shared/TextField', () => {
  it('renders label, input, and error message wired with aria attributes', () => {
    render(
      <TextField
        id="example"
        label="Correo o usuario"
        error="Campo requerido"
      />,
    );
    const input = screen.getByLabelText('Correo o usuario') as HTMLInputElement;
    expect(input).toBeTruthy();
    expect(input.getAttribute('aria-invalid')).toBe('true');
    expect(screen.getByRole('alert').textContent).toBe('Campo requerido');
  });

  it('renders a password visibility toggle when type is password', async () => {
    const user = userEvent.setup();
    render(<TextField id="pw" label="Contraseña" type="password" />);
    const input = screen.getByLabelText('Contraseña') as HTMLInputElement;
    expect(input.type).toBe('password');
    const toggle = screen.getByRole('button', { name: /mostrar contraseña/i });
    await user.click(toggle);
    expect(input.type).toBe('text');
    expect(
      screen.getByRole('button', { name: /ocultar contraseña/i }),
    ).toBeTruthy();
  });
});

describe('shared/StatusBanner', () => {
  it('renders with role=alert for the error variant', () => {
    render(<StatusBanner variant="error">No pudimos validar tus datos.</StatusBanner>);
    const banner = screen.getByRole('alert');
    expect(banner.textContent).toContain('No pudimos validar');
    expect(banner.className).toContain('bg-(--color-error-subtle)');
  });

  it('renders with role=status for the success variant', () => {
    render(<StatusBanner variant="success">Cambios guardados.</StatusBanner>);
    const banner = screen.getByRole('status');
    expect(banner.textContent).toContain('Cambios guardados');
    expect(banner.className).toContain('bg-(--color-success-subtle)');
  });
});
