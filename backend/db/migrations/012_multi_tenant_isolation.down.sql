-- Revert multi-tenant isolation changes
ALTER TABLE user_warehouses DROP COLUMN IF EXISTS company_id;
ALTER TABLE documents DROP COLUMN IF EXISTS company_id;
ALTER TABLE ocr_results DROP COLUMN IF EXISTS company_id;

DROP INDEX IF EXISTS idx_user_warehouses_company_id;
DROP INDEX IF EXISTS idx_documents_company_id;
DROP INDEX IF EXISTS idx_ocr_results_company_id;
