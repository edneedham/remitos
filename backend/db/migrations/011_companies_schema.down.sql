-- Revert companies table changes
ALTER TABLE companies DROP COLUMN IF EXISTS cuit;
ALTER TABLE companies DROP COLUMN IF EXISTS status;
ALTER TABLE companies DROP COLUMN IF EXISTS is_verified;
ALTER TABLE companies DROP COLUMN IF EXISTS subscription_plan;
ALTER TABLE companies DROP COLUMN IF EXISTS subscription_expires_at;
ALTER TABLE companies DROP COLUMN IF EXISTS archived_at;
DROP INDEX IF EXISTS idx_companies_status;
