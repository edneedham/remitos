/** Rules aligned with backend `SignupRequest` (see `backend/internal/models/user.go`). */

import { SIGNUP_LIMITS } from './signupLimits';

const COMPANY_CODE_RE = /^[A-Za-z0-9_-]+$/;

// Practical email check; backend uses go-playground/validator `email` tag.
const EMAIL_RE =
  /^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$/;

export type SignupAccountField =
  | 'companyName'
  | 'companyCode'
  | 'companyCuit'
  | 'email'
  | 'password'
  | 'passwordConfirm';

/** JSON keys from the API `fields` object → form field keys */
export const SIGNUP_API_FIELD_MAP: Record<string, SignupAccountField> = {
  email: 'email',
  password: 'password',
  company_name: 'companyName',
  company_code: 'companyCode',
  company_cuit: 'companyCuit',
};

export type SignupAccountErrors = Partial<
  Record<SignupAccountField, string>
>;

export type SignupAccountValues = {
  companyName: string;
  companyCode: string;
  companyCuit: string;
  email: string;
  password: string;
  passwordConfirm: string;
};

function digitsOnlyCUIT(s: string): string {
  return s.replace(/\D/g, '');
}

/** Argentine CUIT/CUIL verifier digit (weights mod 11). */
function cuitChecksumValid(digits: string): boolean {
  if (digits.length !== 11 || /\D/.test(digits)) return false;
  const mult = [5, 4, 3, 2, 7, 6, 5, 4, 3, 2];
  let sum = 0;
  for (let i = 0; i < 10; i++) {
    sum += parseInt(digits[i]!, 10) * mult[i]!;
  }
  let dv = 11 - (sum % 11);
  if (dv === 11) dv = 0;
  if (dv === 10) dv = 9;
  return dv === parseInt(digits[10]!, 10);
}

/** Required; must be exactly 11 digits (hyphens optional) with valid verifier digit. */
export function validateCompanyCUIT(value: string): string | null {
  const d = digitsOnlyCUIT(value.trim());
  if (d.length === 0) {
    return 'Ingresá el CUIT de la empresa.';
  }
  if (d.length !== 11) {
    return 'El CUIT debe tener 11 dígitos (podés usar guiones o no).';
  }
  if (!cuitChecksumValid(d)) {
    return 'El dígito verificador del CUIT no es válido.';
  }
  return null;
}

export function validateSignupAccountField(
  field: SignupAccountField,
  values: SignupAccountValues,
): string | null {
  switch (field) {
    case 'companyName':
      return validateCompanyName(values.companyName);
    case 'companyCode':
      return validateCompanyCode(values.companyCode);
    case 'companyCuit':
      return validateCompanyCUIT(values.companyCuit);
    case 'email':
      return validateSignupEmail(values.email);
    case 'password':
      return validateSignupPassword(values.password);
    case 'passwordConfirm':
      return validatePasswordConfirm(values.password, values.passwordConfirm);
    default:
      return null;
  }
}

export function validateCompanyName(value: string): string | null {
  const s = value.trim();
  if (!s) return 'Ingresá el nombre de la empresa.';
  if (s.length < SIGNUP_LIMITS.companyNameMin) {
    return `El nombre debe tener al menos ${SIGNUP_LIMITS.companyNameMin} caracteres.`;
  }
  if (s.length > SIGNUP_LIMITS.companyNameMax) {
    return `El nombre no puede superar los ${SIGNUP_LIMITS.companyNameMax} caracteres.`;
  }
  return null;
}

export function validateCompanyCode(value: string): string | null {
  const s = value.trim().toUpperCase();
  if (!s) return 'Ingresá el código de empresa.';
  if (s.length < SIGNUP_LIMITS.companyCodeMin) {
    return `El código debe tener al menos ${SIGNUP_LIMITS.companyCodeMin} caracteres.`;
  }
  if (s.length > SIGNUP_LIMITS.companyCodeMax) {
    return `El código no puede superar los ${SIGNUP_LIMITS.companyCodeMax} caracteres.`;
  }
  if (!COMPANY_CODE_RE.test(s)) {
    return 'Usá solo letras, números, guiones o guiones bajos.';
  }
  return null;
}

export function validateSignupEmail(value: string): string | null {
  const s = value.trim();
  if (!s) return 'Ingresá tu correo electrónico.';
  if (!EMAIL_RE.test(s)) return 'Ingresá un correo electrónico válido.';
  return null;
}

export function validateSignupPassword(value: string): string | null {
  if (!value) return 'Ingresá una contraseña.';
  if (value.length < SIGNUP_LIMITS.passwordMin) {
    return `La contraseña debe tener al menos ${SIGNUP_LIMITS.passwordMin} caracteres.`;
  }
  if (value.length > SIGNUP_LIMITS.passwordMax) {
    return `La contraseña no puede superar los ${SIGNUP_LIMITS.passwordMax} caracteres.`;
  }
  return null;
}

export function validatePasswordConfirm(
  password: string,
  confirm: string,
): string | null {
  if (!confirm) return 'Confirmá la contraseña.';
  if (password !== confirm) return 'Las contraseñas no coinciden.';
  return null;
}

export function validateSignupAccount(
  params: SignupAccountValues,
): SignupAccountErrors {
  const errors: SignupAccountErrors = {};

  const companyNameErr = validateCompanyName(params.companyName);
  if (companyNameErr) errors.companyName = companyNameErr;

  const companyCodeErr = validateCompanyCode(params.companyCode);
  if (companyCodeErr) errors.companyCode = companyCodeErr;

  const companyCuitErr = validateCompanyCUIT(params.companyCuit);
  if (companyCuitErr) errors.companyCuit = companyCuitErr;

  const emailErr = validateSignupEmail(params.email);
  if (emailErr) errors.email = emailErr;

  const passwordErr = validateSignupPassword(params.password);
  if (passwordErr) errors.password = passwordErr;

  const confirmErr = validatePasswordConfirm(
    params.password,
    params.passwordConfirm,
  );
  if (confirmErr) errors.passwordConfirm = confirmErr;

  return errors;
}
