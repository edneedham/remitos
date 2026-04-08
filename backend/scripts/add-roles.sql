-- Add missing roles to the database
-- Run this in your Neon SQL editor

INSERT INTO roles (id, name, description, created_at, updated_at)
VALUES 
    (gen_random_uuid(), 'operator', 'Standard operator with basic permissions', NOW(), NOW()),
    (gen_random_uuid(), 'warehouse_admin', 'Warehouse administrator', NOW(), NOW()),
    (gen_random_uuid(), 'company_owner', 'Company owner with full permissions', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- Verify roles
SELECT * FROM roles;
