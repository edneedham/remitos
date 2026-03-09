-- Update users table with new schema
ALTER TABLE users ADD COLUMN IF NOT EXISTS status TEXT NOT NULL DEFAULT 'active';
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_verified BOOLEAN DEFAULT FALSE;

-- Make email nullable for warehouse operators
ALTER TABLE users ALTER COLUMN email DROP NOT NULL;

-- Drop old indexes
DROP INDEX IF EXISTS idx_users_email;
DROP INDEX IF EXISTS idx_users_username;
DROP INDEX IF EXISTS idx_users_email_company;
DROP INDEX IF EXISTS idx_users_username_company;

-- Add new composite unique constraints (allow nulls)
ALTER TABLE users ADD CONSTRAINT unique_company_email UNIQUE (company_id, email);
ALTER TABLE users ADD CONSTRAINT unique_company_username UNIQUE (company_id, username);

-- Add index on status
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);
