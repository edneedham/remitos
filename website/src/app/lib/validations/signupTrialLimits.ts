/**
 * Canonical limits for trial signup — keep in sync with
 * `SignupTrialRequest` in backend/internal/models/user.go.
 */
export const SIGNUP_TRIAL_LIMITS = {
  companyNameMin: 2,
  companyNameMax: 200,
  companyCodeMin: 2,
  companyCodeMax: 32,
  passwordMin: 8,
  passwordMax: 72,
} as const;
