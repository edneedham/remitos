DROP INDEX IF EXISTS idx_billing_invoices_mp_payment_id_unique;
DROP INDEX IF EXISTS idx_companies_mp_preapproval_id;
ALTER TABLE companies DROP COLUMN IF EXISTS mp_preapproval_id;
