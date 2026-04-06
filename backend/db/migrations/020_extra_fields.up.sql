ALTER TABLE documents ADD COLUMN IF NOT EXISTS extra_fields_json JSONB DEFAULT '{}';
