-- Migration 018: Complete schema update to match new design
-- This brings all tables in line with the simplified schema

-- =====================
-- COMPANIES (already exists, verify schema)
-- =====================
-- companies table should have: id, name, code, created_at, updated_at
-- Already has: id, name, code (from 008), created_at, updated_at
-- Add if missing
ALTER TABLE companies ADD COLUMN IF NOT EXISTS code VARCHAR(50) UNIQUE;
ALTER TABLE companies ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE companies ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT NOW();

-- =====================
-- WAREHOUSES (add location)
-- =====================
ALTER TABLE warehouses ADD COLUMN IF NOT EXISTS location VARCHAR(500);

-- =====================
-- USERS (simplified RBAC)
-- =====================
-- Drop old role column and constraints, use role_id from roles table instead
ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'active';
ALTER TABLE users ADD COLUMN IF NOT EXISTS external_id VARCHAR(50) UNIQUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT NOW();

-- =====================
-- DEVICES (simplified)
-- =====================
-- Update devices to match new schema
ALTER TABLE devices ADD COLUMN IF NOT EXISTS name VARCHAR(255);
ALTER TABLE devices ADD COLUMN IF NOT EXISTS last_seen_at TIMESTAMP;
ALTER TABLE devices ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();

-- =====================
-- DOCUMENTS (new table - replace old one if exists, or create new)
-- =====================
-- First drop the old documents table if it exists (it has different schema)
DROP TABLE IF EXISTS documents CASCADE;

CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    warehouse_id UUID NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT,
    
    type VARCHAR(50) NOT NULL CHECK (type IN ('incoming_remito', 'outgoing_remito')),
    supplier_name VARCHAR(255),
    document_number VARCHAR(100),
    ocr_confidence DOUBLE PRECISION,
    ocr_engine_used VARCHAR(20) CHECK (ocr_engine_used IN ('local', 'cloud')),
    status VARCHAR(20) NOT NULL DEFAULT 'draft' CHECK (status IN ('draft', 'verifying', 'verified', 'finalized')),
    
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    verified_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_documents_company_id ON documents(company_id);
CREATE INDEX IF NOT EXISTS idx_documents_warehouse_id ON documents(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_documents_status ON documents(status);
CREATE INDEX IF NOT EXISTS idx_documents_type ON documents(type);

-- =====================
-- DOCUMENT_ITEMS (new table)
-- =====================
CREATE TABLE IF NOT EXISTS document_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    
    description TEXT NOT NULL,
    expected_quantity INTEGER NOT NULL DEFAULT 0,
    received_quantity INTEGER NOT NULL DEFAULT 0,
    unit VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_document_items_document_id ON document_items(document_id);

-- =====================
-- SCANNED_CODES (new table)
-- =====================
CREATE TABLE IF NOT EXISTS scanned_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    document_item_id UUID REFERENCES document_items(id) ON DELETE SET NULL,
    
    raw_value TEXT NOT NULL,
    parsed_gtin VARCHAR(50),
    parsed_sscc VARCHAR(50),
    parsed_batch VARCHAR(50),
    parsed_expiry DATE,
    matched BOOLEAN NOT NULL DEFAULT FALSE,
    
    scanned_by UUID REFERENCES users(id),
    device_id UUID REFERENCES devices(id),
    scanned_at_local TIMESTAMP,
    scanned_at_server TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_scanned_codes_document_id ON scanned_codes(document_id);
CREATE INDEX IF NOT EXISTS idx_scanned_codes_document_item_id ON scanned_codes(document_item_id);
CREATE INDEX IF NOT EXISTS idx_scanned_codes_raw_value ON scanned_codes(raw_value);

-- =====================
-- AUDIT_LOGS (new table - replace user_events with this)
-- =====================
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    warehouse_id UUID REFERENCES warehouses(id) ON DELETE SET NULL,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    device_id UUID REFERENCES devices(id) ON DELETE SET NULL,
    
    action_type VARCHAR(100) NOT NULL,
    target_id UUID,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_company_id ON audit_logs(company_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_warehouse_id ON audit_logs(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action_type ON audit_logs(action_type);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at);
