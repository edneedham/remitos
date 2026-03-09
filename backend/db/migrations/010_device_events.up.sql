-- Create device_events table (append-only audit log)
CREATE TABLE device_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    warehouse_id UUID NOT NULL REFERENCES warehouses(id),

    event_type TEXT NOT NULL,   -- registered, approved, revoked, reassigned
    performed_by UUID REFERENCES users(id),

    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Append-only: no delete allowed
-- No indexes needed for audit log (append-only, sequential writes)
-- Optional: index if querying by device or company becomes common
CREATE INDEX IF NOT EXISTS idx_device_events_device_id ON device_events(device_id);
CREATE INDEX IF NOT EXISTS idx_device_events_company_id ON device_events(company_id);
CREATE INDEX IF NOT EXISTS idx_device_events_event_type ON device_events(event_type);
CREATE INDEX IF NOT EXISTS idx_device_events_created_at ON device_events(created_at);
