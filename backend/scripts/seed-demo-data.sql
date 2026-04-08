-- Seed script for LOGSUR company demo data
-- Run this against your Neon PostgreSQL database

-- Insert LOGSUR company
INSERT INTO companies (id, name, code, status, is_verified, subscription_plan, created_at, updated_at)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'Logística del Sur S.A.',
    'LOGSUR',
    'active',
    true,
    'premium',
    NOW(),
    NOW()
)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    code = EXCLUDED.code,
    status = EXCLUDED.status;

-- Insert warehouse for LOGSUR
INSERT INTO warehouses (id, company_id, name, address, created_at, updated_at)
VALUES (
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    'Depósito Central',
    'Av. Corrientes 1234, Buenos Aires',
    NOW(),
    NOW()
)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    address = EXCLUDED.address;

-- Get role IDs
DO $$
DECLARE
    owner_role_id UUID;
    admin_role_id UUID;
    operator_role_id UUID;
BEGIN
    -- Get role IDs
    SELECT id INTO owner_role_id FROM roles WHERE name = 'company_owner';
    SELECT id INTO admin_role_id FROM roles WHERE name = 'warehouse_admin';
    SELECT id INTO operator_role_id FROM roles WHERE name = 'operator';

    -- Insert admin user (company_owner)
    INSERT INTO users (id, company_id, warehouse_id, email, username, password_hash, role, role_id, status, is_verified, created_at, updated_at)
    VALUES (
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        '11111111-1111-1111-1111-111111111111',
        '22222222-2222-2222-2222-222222222222',
        'admin@logsur.com',
        'admin',
        '$2a$10$8poL3PM1HC4NFaJpFSbfneBbrjSDOCG.M0g1x4B11v5xE4n6KcEWu',
        'company_owner',
        owner_role_id,
        'active',
        true,
        NOW(),
        NOW()
    )
    ON CONFLICT (id) DO UPDATE SET
        username = EXCLUDED.username,
        password_hash = EXCLUDED.password_hash,
        status = EXCLUDED.status;

    -- Insert warehouse admin
    INSERT INTO users (id, company_id, warehouse_id, email, username, password_hash, role, role_id, status, is_verified, created_at, updated_at)
    VALUES (
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
        '11111111-1111-1111-1111-111111111111',
        '22222222-2222-2222-2222-222222222222',
        'jefe@logsur.com',
        'jefedeposito',
        '$2a$10$8poL3PM1HC4NFaJpFSbfneBbrjSDOCG.M0g1x4B11v5xE4n6KcEWu',
        'warehouse_admin',
        admin_role_id,
        'active',
        true,
        NOW(),
        NOW()
    )
    ON CONFLICT (id) DO UPDATE SET
        username = EXCLUDED.username,
        password_hash = EXCLUDED.password_hash,
        status = EXCLUDED.status;

    -- Insert operators
    INSERT INTO users (id, company_id, warehouse_id, email, username, password_hash, role, role_id, status, is_verified, created_at, updated_at)
    VALUES 
        ('cccccccc-cccc-cccc-cccc-cccccccccccc', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 'miguel@logsur.com', 'm.gomez', '$2a$10$8poL3PM1HC4NFaJpFSbfneBbrjSDOCG.M0g1x4B11v5xE4n6KcEWu', 'operator', operator_role_id, 'active', true, NOW(), NOW()),
        ('dddddddd-dddd-dddd-dddd-dddddddddddd', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 'juan@logsur.com', 'j.perez', '$2a$10$8poL3PM1HC4NFaJpFSbfneBbrjSDOCG.M0g1x4B11v5xE4n6KcEWu', 'operator', operator_role_id, 'active', true, NOW(), NOW()),
        ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 'lucia@logsur.com', 'l.rodriguez', '$2a$10$8poL3PM1HC4NFaJpFSbfneBbrjSDOCG.M0g1x4B11v5xE4n6KcEWu', 'operator', operator_role_id, 'inactive', true, NOW(), NOW())
    ON CONFLICT (id) DO UPDATE SET
        username = EXCLUDED.username,
        password_hash = EXCLUDED.password_hash,
        status = EXCLUDED.status;
END $$;

-- Create subscriptions for all users
INSERT INTO subscriptions (id, user_id, status, device_connected, offline_mode, connected_mode, premium_features, created_at, updated_at)
SELECT 
    uuid_generate_v4(),
    id,
    'trialing',
    false,
    true,
    true,
    true,
    NOW(),
    NOW()
FROM users 
WHERE company_id = '11111111-1111-1111-1111-111111111111'
ON CONFLICT (user_id) DO NOTHING;

-- Verify creation
SELECT u.username, u.role, u.status, c.name as company_name, w.name as warehouse_name
FROM users u
JOIN companies c ON u.company_id = c.id
JOIN warehouses w ON u.warehouse_id = w.id
WHERE c.code = 'LOGSUR';
