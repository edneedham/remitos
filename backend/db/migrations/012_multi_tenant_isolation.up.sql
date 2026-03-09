-- Add company_id to all operational tables for multi-tenant isolation

-- user_warehouses: derive company_id from user
ALTER TABLE user_warehouses ADD COLUMN IF NOT EXISTS company_id UUID NOT NULL REFERENCES companies(id);

-- documents: derive company_id from warehouse (already has warehouse_id)
ALTER TABLE documents ADD COLUMN IF NOT EXISTS company_id UUID NOT NULL REFERENCES companies(id);

-- ocr_results: derive company_id from document (through documents table)
ALTER TABLE ocr_results ADD COLUMN IF NOT EXISTS company_id UUID NOT NULL REFERENCES companies(id);

-- Backfill company_id from existing relationships
UPDATE user_warehouses 
SET company_id = (SELECT company_id FROM users WHERE users.id = user_warehouses.user_id)
WHERE company_id IS NULL;

UPDATE documents 
SET company_id = (SELECT company_id FROM warehouses WHERE warehouses.id = documents.warehouse_id)
WHERE company_id IS NULL;

UPDATE ocr_results
SET company_id = (SELECT company_id FROM documents WHERE documents.id = ocr_results.document_id)
WHERE company_id IS NULL;

-- Add indexes for company-based queries
CREATE INDEX IF NOT EXISTS idx_user_warehouses_company_id ON user_warehouses(company_id);
CREATE INDEX IF NOT EXISTS idx_documents_company_id ON documents(company_id);
CREATE INDEX IF NOT EXISTS idx_ocr_results_company_id ON ocr_results(company_id);
