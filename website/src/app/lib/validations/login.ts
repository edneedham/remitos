/**
 * Client-side validation for POST /auth/login — aligned with backend
 * `LoginRequest` (server/internal/models/login.go) and NormalizeLoginRequest.
 */

import { validateCompanyCode } from './signupTrial';

export type LoginFormField = 'company_code' | 'username' | 'password';

export type LoginFormErrors = Partial<Record<LoginFormField, string>>;

export type LoginFormValues = {
  companyCode: string;
  username: string;
  password: string;
};

export function validateLoginCompanyCode(value: string): string | null {
  return validateCompanyCode(value);
}

export function validateLoginUsername(value: string): string | null {
  const s = value.trim();
  if (!s) return 'Ingresá tu correo o usuario.';
  return null;
}

/** Password is not trimmed on the server; only require non-empty. */
export function validateLoginPassword(value: string): string | null {
  if (!value) return 'Ingresá tu contraseña.';
  return null;
}

export function validateLoginFormField(
  field: LoginFormField,
  values: LoginFormValues,
): string | null {
  switch (field) {
    case 'company_code':
      return validateLoginCompanyCode(values.companyCode);
    case 'username':
      return validateLoginUsername(values.username);
    case 'password':
      return validateLoginPassword(values.password);
    default:
      return null;
  }
}

export function validateLoginForm(values: LoginFormValues): LoginFormErrors {
  const errors: LoginFormErrors = {};

  const companyErr = validateLoginCompanyCode(values.companyCode);
  if (companyErr) errors.company_code = companyErr;

  const usernameErr = validateLoginUsername(values.username);
  if (usernameErr) errors.username = usernameErr;

  const passwordErr = validateLoginPassword(values.password);
  if (passwordErr) errors.password = passwordErr;

  return errors;
}
