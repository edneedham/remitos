ALTER TABLE companies
  ADD COLUMN IF NOT EXISTS trial_end_notice_sent_for_trial_ends_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS subscription_lapse_notice_sent_for_expires_at TIMESTAMPTZ;

COMMENT ON COLUMN companies.trial_end_notice_sent_for_trial_ends_at IS 'trial_ends_at value we already emailed a trial-ending reminder for';
COMMENT ON COLUMN companies.subscription_lapse_notice_sent_for_expires_at IS 'subscription_expires_at we emailed a lapse notice for';
