-- Remove company_id from devices table
ALTER TABLE devices DROP COLUMN IF EXISTS company_id;

DROP INDEX IF EXISTS idx_devices_company_id;
