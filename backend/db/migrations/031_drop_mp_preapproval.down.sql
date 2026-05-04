ALTER TABLE companies ADD COLUMN IF NOT EXISTS mp_preapproval_id VARCHAR(255);

CREATE UNIQUE INDEX IF NOT EXISTS idx_companies_mp_preapproval_id
	ON companies (mp_preapproval_id)
	WHERE mp_preapproval_id IS NOT NULL;

COMMENT ON COLUMN companies.mp_preapproval_id IS 'Mercado Pago preapproval (subscription) id; renewals come from webhooks';
