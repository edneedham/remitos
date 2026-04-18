/** Rules aligned with backend `SignupTrialRequest` (see `backend/internal/models/user.go`). */

const COMPANY_CODE_RE = /^[A-Za-z0-9_-]+$/;

// Practical email check; backend uses go-playground/validator `email` tag.
const EMAIL_RE =
  /^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$/;

export type SignupTrialAccountField =
  | 'companyName'
  | 'companyCode'
  | 'email'
  | 'password'
  | 'passwordConfirm';

export type SignupTrialAccountErrors = Partial<
  Record<SignupTrialAccountField, string>
>;

export type SignupTrialAccountValues = {
  companyName: string;
  companyCode: string;
  email: string;
  password: string;
  passwordConfirm: string;
};

export function validateSignupTrialAccountField(
  field: SignupTrialAccountField,
  values: SignupTrialAccountValues,
): string | null {
  switch (field) {
    case 'companyName':
      return validateCompanyName(values.companyName);
    case 'companyCode':
      return validateCompanyCode(values.companyCode);
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
  if (s.length < 2) return 'El nombre debe tener al menos 2 caracteres.';
  if (s.length > 200) return 'El nombre no puede superar los 200 caracteres.';
  return null;
}

export function validateCompanyCode(value: string): string | null {
  const s = value.trim().toUpperCase();
  if (!s) return 'Ingresá el código de empresa.';
  if (s.length < 2) return 'El código debe tener al menos 2 caracteres.';
  if (s.length > 32) return 'El código no puede superar los 32 caracteres.';
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
  if (value.length < 8) return 'La contraseña debe tener al menos 8 caracteres.';
  if (value.length > 72) return 'La contraseña no puede superar los 72 caracteres.';
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

export function validateSignupTrialAccount(
  params: SignupTrialAccountValues,
): SignupTrialAccountErrors {
  const errors: SignupTrialAccountErrors = {};

  const companyNameErr = validateCompanyName(params.companyName);
  if (companyNameErr) errors.companyName = companyNameErr;

  const companyCodeErr = validateCompanyCode(params.companyCode);
  if (companyCodeErr) errors.companyCode = companyCodeErr;

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

export function validateCardholderName(value: string): string | null {
  const s = value.trim();
  if (!s) return 'Ingresá el nombre del titular de la tarjeta.';
  return null;
}

export function validateIdNumber(value: string): string | null {
  const s = value.trim();
  if (!s) return 'Ingresá el número de documento.';
  return null;
}
