-- Seed data for demo purposes
-- Run this after migrations to create test data

-- Create demo company
INSERT INTO companies (id, name, code, status, is_verified, created_at, updated_at)
VALUES 
    ('11111111-1111-1111-1111-111111111111', 'Demo Company', 'DEMO', 'active', true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Create demo warehouse
INSERT INTO warehouses (id, company_id, name, address, created_at, updated_at)
VALUES 
    ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'Main Warehouse', '123 Demo Street, Buenos Aires', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Create demo roles if they don't exist
INSERT INTO roles (id, name) 
SELECT gen_random_uuid(), 'company_owner' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'company_owner')
ON CONFLICT DO NOTHING;

INSERT INTO roles (id, name) 
SELECT gen_random_uuid(), 'warehouse_admin' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'warehouse_admin')
ON CONFLICT DO NOTHING;

INSERT INTO roles (id, name) 
SELECT gen_random_uuid(), 'operator' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'operator')
ON CONFLICT DO NOTHING;

-- Get role IDs
DO $$
DECLARE
    company_owner_role_id UUID;
    warehouse_admin_role_id UUID;
    operator_role_id UUID;
BEGIN
    SELECT id INTO company_owner_role_id FROM roles WHERE name = 'company_owner';
    SELECT id INTO warehouse_admin_role_id FROM roles WHERE name = 'warehouse_admin';
    SELECT id INTO operator_role_id FROM roles WHERE name = 'operator';

    -- Create demo company owner (password: demo1234)
    -- Password hash for "demo1234"
    INSERT INTO users (id, company_id, email, username, password_hash, role, role_id, status, is_verified, created_at, updated_at)
    VALUES 
        ('33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111', 'admin@demo.com', 'admin', '$2a$10$1ea7Bq6C7zd880Vqu8r/EOl.vv.eN8RfkkfTmapq95.zWfhcLDy9a', company_owner_role_id, company_owner_role_id, 'active', true, NOW(), NOW())
    ON CONFLICT (id) DO NOTHING;

    -- Create demo warehouse admin (password: demo1234)
    INSERT INTO users (id, company_id, email, username, password_hash, role, role_id, status, is_verified, created_at, updated_at)
    VALUES 
        ('44444444-4444-4444-4444-444444444444', '11111111-1111-1111-1111-111111111111', 'warehouse@demo.com', 'warehouse', '$2a$10$1ea7Bq6C7zd880Vqu8r/EOl.vv.eN8RfkkfTmapq95.zWfhcLDy9a', warehouse_admin_role_id, warehouse_admin_role_id, 'active', true, NOW(), NOW())
    ON CONFLICT (id) DO NOTHING;

    -- Create demo operator (password: demo1234)
    INSERT INTO users (id, company_id, email, username, password_hash, role, role_id, status, is_verified, created_at, updated_at)
    VALUES 
        ('55555555-5555-5555-5555-555555555555', '11111111-1111-1111-1111-111111111111', 'operator@demo.com', 'operator', '$2a$10$1ea7Bq6C7zd880Vqu8r/EOl.vv.eN8RfkkfTmapq95.zWfhcLDy9a', operator_role_id, operator_role_id, 'active', true, NOW(), NOW())
    ON CONFLICT (id) DO NOTHING;
END $$;

-- Create demo device
INSERT INTO devices (id, company_id, warehouse_id, name, fingerprint, status, last_seen_at, created_at)
VALUES 
    ('66666666-6666-6666-6666-666666666666', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 'Demo Device', 'demo-device-fingerprint', 'active', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;
