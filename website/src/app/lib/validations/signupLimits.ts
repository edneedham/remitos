/**
 * Canonical limits for signup forms — keep in sync with
 * `SignupRequest` in backend/internal/models/user.go.
 */
export const SIGNUP_LIMITS = {
  companyNameMin: 2,
  companyNameMax: 200,
  companyCodeMin: 2,
  companyCodeMax: 32,
  passwordMin: 8,
  passwordMax: 72,
} as const;
