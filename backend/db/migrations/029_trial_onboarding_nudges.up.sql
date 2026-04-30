-- Anchor for lifecycle emails (+10m / +24h / +72h after plan selection). Nullable for legacy rows.
ALTER TABLE companies
  ADD COLUMN IF NOT EXISTS trial_activation_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS onboarding_nudge_setup_sent_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS onboarding_nudge_day1_sent_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS onboarding_nudge_day3_sent_at TIMESTAMPTZ;

COMMENT ON COLUMN companies.trial_activation_at IS 'Set when the customer chooses PyME/Empresa plan after signup; used for onboarding reminder emails';
COMMENT ON COLUMN companies.onboarding_nudge_setup_sent_at IS 'First reminder (+~10m): download Android app';
COMMENT ON COLUMN companies.onboarding_nudge_day1_sent_at IS 'Second reminder (+~24h)';
COMMENT ON COLUMN companies.onboarding_nudge_day3_sent_at IS 'Third reminder (+~72h)';
