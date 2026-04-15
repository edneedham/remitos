-- Migration 022: Add sync tracking columns and create sync tables
-- These tables support bidirectional sync between Android devices and the server

-- Add sync tracking columns to inbound_notes
ALTER TABLE documents ADD COLUMN IF NOT EXISTS cloud_id UUID UNIQUE DEFAULT gen_random_uuid();
ALTER TABLE documents ADD COLUMN IF NOT EXISTS company_id UUID REFERENCES companies(id) ON DELETE CASCADE;

-- Add cloud_id columns to support sync
-- Note: inbound_notes and outbound_lists tables will be created by this migration
-- with cloud_id and company_id columns for multi-tenant sync support

CREATE TABLE IF NOT EXISTS inbound_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cloud_id UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    warehouse_id UUID REFERENCES warehouses(id) ON DELETE SET NULL,

    remito_num_cliente VARCHAR(100) NOT NULL,
    remito_num_interno VARCHAR(100),
    cant_bultos_total INTEGER NOT NULL DEFAULT 0,

    cuit_remitente VARCHAR(50),
    nombre_remitente VARCHAR(255),
    apellido_remitente VARCHAR(255),
    nombre_destinatario VARCHAR(255),
    apellido_destinatario VARCHAR(255),
    direccion_destinatario VARCHAR(500),
    telefono_destinatario VARCHAR(50),

    status VARCHAR(50) NOT NULL DEFAULT 'Activa',

    image_gcs_path VARCHAR(500),
    image_url VARCHAR(1000),

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_inbound_notes_company_id ON inbound_notes(company_id);
CREATE INDEX IF NOT EXISTS idx_inbound_notes_cloud_id ON inbound_notes(cloud_id);
CREATE INDEX IF NOT EXISTS idx_inbound_notes_status ON inbound_notes(status);
CREATE INDEX IF NOT EXISTS idx_inbound_notes_updated_at ON inbound_notes(updated_at);

-- Outbound lists with sync support
CREATE TABLE IF NOT EXISTS outbound_lists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cloud_id UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    warehouse_id UUID REFERENCES warehouses(id) ON DELETE SET NULL,

    list_number BIGINT NOT NULL,
    issue_date TIMESTAMP NOT NULL DEFAULT NOW(),

    driver_nombre VARCHAR(255),
    driver_apellido VARCHAR(255),

    checklist_signature_path VARCHAR(500),
    checklist_signed_at TIMESTAMP,

    status VARCHAR(50) NOT NULL DEFAULT 'Abierta',

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_outbound_lists_company_id ON outbound_lists(company_id);
CREATE INDEX IF NOT EXISTS idx_outbound_lists_cloud_id ON outbound_lists(cloud_id);
CREATE INDEX IF NOT EXISTS idx_outbound_lists_status ON outbound_lists(status);
CREATE INDEX IF NOT EXISTS idx_outbound_lists_updated_at ON outbound_lists(updated_at);

-- Outbound lines with sync support
CREATE TABLE IF NOT EXISTS outbound_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cloud_id UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
    outbound_list_id UUID NOT NULL REFERENCES outbound_lists(id) ON DELETE CASCADE,

    inbound_note_id UUID REFERENCES inbound_notes(id) ON DELETE SET NULL,

    delivery_number VARCHAR(100),
    recipient_nombre VARCHAR(255),
    recipient_apellido VARCHAR(255),
    recipient_direccion VARCHAR(500),
    recipient_telefono VARCHAR(50),

    package_qty INTEGER NOT NULL DEFAULT 0,
    allocated_package_ids TEXT,

    status VARCHAR(50) NOT NULL DEFAULT 'EnDeposito',

    delivered_qty INTEGER NOT NULL DEFAULT 0,
    returned_qty INTEGER NOT NULL DEFAULT 0,
    missing_qty INTEGER NOT NULL DEFAULT 0,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_outbound_lines_outbound_list_id ON outbound_lines(outbound_list_id);
CREATE INDEX IF NOT EXISTS idx_outbound_lines_cloud_id ON outbound_lines(cloud_id);
CREATE INDEX IF NOT EXISTS idx_outbound_lines_status ON outbound_lines(status);

-- Status history with cloud_id for sync deduplication
CREATE TABLE IF NOT EXISTS outbound_line_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cloud_id UUID UNIQUE DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    outbound_line_id UUID NOT NULL REFERENCES outbound_lines(id) ON DELETE CASCADE,

    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_outbound_line_status_history_line_id ON outbound_line_status_history(outbound_line_id);
CREATE INDEX IF NOT EXISTS idx_outbound_line_status_history_cloud_id ON outbound_line_status_history(cloud_id);

-- Edit history with cloud_id for sync deduplication
CREATE TABLE IF NOT EXISTS outbound_line_edit_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cloud_id UUID UNIQUE DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    outbound_line_id UUID NOT NULL REFERENCES outbound_lines(id) ON DELETE CASCADE,

    field_name VARCHAR(100) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_outbound_line_edit_history_line_id ON outbound_line_edit_history(outbound_line_id);
CREATE INDEX IF NOT EXISTS idx_outbound_line_edit_history_cloud_id ON outbound_line_edit_history(cloud_id);

-- Sync metadata table for tracking per-device sync state
CREATE TABLE IF NOT EXISTS sync_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    last_sync_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_sync_timestamp BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    UNIQUE(device_id, company_id)
);

CREATE INDEX IF NOT EXISTS idx_sync_metadata_device_id ON sync_metadata(device_id);
CREATE INDEX IF NOT EXISTS idx_sync_metadata_company_id ON sync_metadata(company_id);