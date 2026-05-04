ALTER TABLE companies
  DROP COLUMN IF EXISTS subscription_lapse_notice_sent_for_expires_at,
  DROP COLUMN IF EXISTS trial_end_notice_sent_for_trial_ends_at;
