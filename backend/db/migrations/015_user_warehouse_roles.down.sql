-- Revert user_warehouses changes
ALTER TABLE user_warehouses DROP COLUMN IF EXISTS role;
ALTER TABLE user_warehouses DROP COLUMN IF EXISTS company_id;
DROP INDEX IF EXISTS idx_user_warehouses_role;
