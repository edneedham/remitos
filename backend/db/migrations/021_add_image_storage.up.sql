-- Migration 021: Add image storage support for remitos
-- Stores metadata for images uploaded to Google Cloud Storage

-- Images table to store GCS metadata
CREATE TABLE IF NOT EXISTS images (
    id UUID PRIMARY KEY,
    gcs_path VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL DEFAULT 'image/jpeg',
    file_size BIGINT NOT NULL,
    entity_type VARCHAR(50) NOT NULL CHECK (entity_type IN ('inbound_note', 'outbound_list', 'scan')),
    entity_id BIGINT NOT NULL,
    warehouse_id UUID REFERENCES warehouses(id),
    uploaded_by UUID REFERENCES users(id),
    uploaded_at TIMESTAMP NOT NULL DEFAULT NOW(),
    storage_class VARCHAR(20) DEFAULT 'STANDARD',
    
    -- Unique constraint to prevent duplicate uploads for same entity
    CONSTRAINT unique_entity_image UNIQUE (entity_type, entity_id)
);

-- Indexes for common queries
CREATE INDEX IF NOT EXISTS idx_images_entity ON images(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_images_warehouse ON images(warehouse_id);
CREATE INDEX IF NOT EXISTS idx_images_uploaded_at ON images(uploaded_at);
CREATE INDEX IF NOT EXISTS idx_images_uploaded_by ON images(uploaded_by);

-- Note: Signed URLs are generated on-demand and not stored
-- They have short expiration (7 days) and are regenerated when needed
