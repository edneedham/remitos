-- Composite indexes for tenant-scoped sync and delta queries:
-- WHERE company_id = $1 AND updated_at > $2 (and ORDER BY updated_at)

CREATE INDEX IF NOT EXISTS idx_inbound_notes_company_updated_at
	ON inbound_notes (company_id, updated_at);

CREATE INDEX IF NOT EXISTS idx_outbound_lists_company_updated_at
	ON outbound_lists (company_id, updated_at);
