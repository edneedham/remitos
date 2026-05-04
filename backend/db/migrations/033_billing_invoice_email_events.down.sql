ALTER TABLE billing_invoices
  DROP COLUMN IF EXISTS renewal_failure_notice_sent_at,
  DROP COLUMN IF EXISTS receipt_email_sent_at;
