-- Add role to user_warehouses for warehouse-level access control
ALTER TABLE user_warehouses ADD COLUMN IF NOT EXISTS role VARCHAR(50) NOT NULL DEFAULT 'operator';
ALTER TABLE user_warehouses ADD COLUMN IF NOT EXISTS company_id UUID NOT NULL REFERENCES companies(id);

-- Backfill company_id from users
UPDATE user_warehouses 
SET company_id = (SELECT company_id FROM users WHERE users.id = user_warehouses.user_id)
WHERE company_id IS NULL;

-- Add indexes
CREATE INDEX IF NOT EXISTS idx_user_warehouses_role ON user_warehouses(role);
