-- Drop existing devices table and recreate with new schema
DROP TABLE IF EXISTS devices CASCADE;

CREATE TABLE devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    warehouse_id UUID NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT,

    -- Device identity
    device_uuid TEXT NOT NULL,               -- Generated on device install
    platform TEXT NOT NULL,                  -- android, ios (future)
    model TEXT,
    os_version TEXT,
    app_version TEXT,

    -- Security state
    status TEXT NOT NULL DEFAULT 'pending',  -- pending, active, revoked
    approved_by UUID REFERENCES users(id),
    approved_at TIMESTAMP,

    -- Metadata
    registered_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_seen_at TIMESTAMP,

    -- Prevent duplicate registration
    UNIQUE(company_id, device_uuid)
);

CREATE INDEX IF NOT EXISTS idx_devices_company_id ON devices(company_id);
CREATE INDEX IF NOT EXISTS idx_devices_warehouse_id ON devices(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_devices_device_uuid ON devices(device_uuid);
CREATE INDEX IF NOT EXISTS idx_devices_status ON devices(status);
