-- Soft-delete (archive) for warehouses so we never hard-delete a row that still
-- has devices, users, or remitos referencing it.
ALTER TABLE warehouses
    ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP NULL;

CREATE INDEX IF NOT EXISTS idx_warehouses_company_id_active
    ON warehouses (company_id)
    WHERE archived_at IS NULL;

-- pending_plan stores a downgrade scheduled to take effect at the next renewal.
ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS pending_plan TEXT NULL;

COMMENT ON COLUMN companies.pending_plan IS
    'When set, the company is scheduled to switch to this plan at the next subscription renewal (downgrade only).';
