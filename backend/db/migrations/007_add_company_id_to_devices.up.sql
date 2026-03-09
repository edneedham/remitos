-- Add company_id to devices table
ALTER TABLE devices ADD COLUMN company_id UUID REFERENCES companies(id);

CREATE INDEX IF NOT EXISTS idx_devices_company_id ON devices(company_id);
