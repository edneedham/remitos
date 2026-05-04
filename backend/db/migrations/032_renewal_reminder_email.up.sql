-- One renewal-notice email per billing period (matched to subscription_expires_at).
ALTER TABLE companies
  ADD COLUMN IF NOT EXISTS renewal_reminder_sent_for_expires_at TIMESTAMPTZ;

COMMENT ON COLUMN companies.renewal_reminder_sent_for_expires_at IS 'Last subscription_expires_at we emailed an upcoming renewal notice for; resend when expiry advances';
