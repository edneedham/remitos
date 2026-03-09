-- Revert users table changes
ALTER TABLE users DROP COLUMN IF EXISTS status;
ALTER TABLE users DROP COLUMN IF EXISTS is_verified;
ALTER TABLE users ALTER COLUMN email SET NOT NULL;

ALTER TABLE users DROP CONSTRAINT IF EXISTS unique_company_email;
ALTER TABLE users DROP CONSTRAINT IF EXISTS unique_company_username;

DROP INDEX IF EXISTS idx_users_status;

-- Restore old indexes
CREATE UNIQUE INDEX idx_users_email ON users(email);
CREATE UNIQUE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email_company ON users(email, company_id);
CREATE INDEX idx_users_username_company ON users(username, company_id);
