ALTER TABLE companies ADD COLUMN IF NOT EXISTS documents_monthly_limit INT;

COMMENT ON COLUMN companies.documents_monthly_limit IS 'Max inbound documents per calendar month (UTC); NULL = unlimited';
