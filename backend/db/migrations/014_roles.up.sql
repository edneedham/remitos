-- Create roles table
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Seed with default roles
INSERT INTO roles (name) VALUES 
    ('company_owner'),
    ('warehouse_admin'),
    ('operator'),
    ('read_only');

-- Add role_id to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS role_id UUID REFERENCES roles(id);

-- Keep role column for backward compatibility (deprecated, use role_id)
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(50);

-- Create index for role lookups
CREATE INDEX IF NOT EXISTS idx_users_role_id ON users(role_id);
