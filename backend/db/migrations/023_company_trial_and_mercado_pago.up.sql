-- Trial entitlements and Mercado Pago references (card on file for post-trial billing).
-- NULL max_* means no cap (legacy companies). Trial signups set explicit limits.

ALTER TABLE companies ADD COLUMN IF NOT EXISTS trial_ends_at TIMESTAMPTZ;
ALTER TABLE companies ADD COLUMN IF NOT EXISTS max_warehouses INT;
ALTER TABLE companies ADD COLUMN IF NOT EXISTS max_users INT;
ALTER TABLE companies ADD COLUMN IF NOT EXISTS mp_customer_id VARCHAR(255);
ALTER TABLE companies ADD COLUMN IF NOT EXISTS mp_card_id VARCHAR(255);

COMMENT ON COLUMN companies.trial_ends_at IS 'End of free trial; no charge before this when trialing';
COMMENT ON COLUMN companies.max_warehouses IS 'Cap for trial/plan; NULL = unlimited (legacy)';
COMMENT ON COLUMN companies.max_users IS 'Cap for trial/plan; NULL = unlimited (legacy)';
COMMENT ON COLUMN companies.mp_customer_id IS 'Mercado Pago customer id';
COMMENT ON COLUMN companies.mp_card_id IS 'Mercado Pago saved card id for charging after trial';
