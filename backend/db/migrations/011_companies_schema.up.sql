-- Update companies table with new schema
ALTER TABLE companies ADD COLUMN IF NOT EXISTS cuit VARCHAR(20);
ALTER TABLE companies ADD COLUMN IF NOT EXISTS status TEXT NOT NULL DEFAULT 'active';
ALTER TABLE companies ADD COLUMN IF NOT EXISTS is_verified BOOLEAN DEFAULT FALSE;
ALTER TABLE companies ADD COLUMN IF NOT EXISTS subscription_plan TEXT DEFAULT 'free';
ALTER TABLE companies ADD COLUMN IF NOT EXISTS subscription_expires_at TIMESTAMP;
ALTER TABLE companies ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP;

-- Set NOT NULL on created_at if not already set
ALTER TABLE companies ALTER COLUMN created_at SET NOT NULL;

-- Add index on status for filtering
CREATE INDEX IF NOT EXISTS idx_companies_status ON companies(status);
