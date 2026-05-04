-- Remove Mercado Pago Suscripciones / preapproval id; renewals are merchant-owned + Payments API only.

DROP INDEX IF EXISTS idx_companies_mp_preapproval_id;
ALTER TABLE companies DROP COLUMN IF EXISTS mp_preapproval_id;
