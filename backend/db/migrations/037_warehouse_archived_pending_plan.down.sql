DROP INDEX IF EXISTS idx_warehouses_company_id_active;
ALTER TABLE warehouses DROP COLUMN IF EXISTS archived_at;
ALTER TABLE companies DROP COLUMN IF EXISTS pending_plan;
