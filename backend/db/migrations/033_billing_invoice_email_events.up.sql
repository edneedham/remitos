ALTER TABLE billing_invoices
  ADD COLUMN IF NOT EXISTS receipt_email_sent_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS renewal_failure_notice_sent_at TIMESTAMPTZ;

COMMENT ON COLUMN billing_invoices.receipt_email_sent_at IS 'When we emailed a payment receipt for this row (idempotent with mp_payment_id)';
COMMENT ON COLUMN billing_invoices.renewal_failure_notice_sent_at IS 'When we emailed about a failed automatic renewal charge for this pending invoice';
