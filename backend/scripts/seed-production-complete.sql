-- ============================================================================
-- PRODUCTION DATABASE SEED SCRIPT
-- Run this in Neon SQL Editor to populate the production database
-- ============================================================================

-- Step 1: Ensure all roles exist
-- ============================================================================
INSERT INTO roles (id, name, description, created_at, updated_at)
VALUES 
    (gen_random_uuid(), 'company_owner', 'Company owner with full permissions', NOW(), NOW()),
    (gen_random_uuid(), 'warehouse_admin', 'Warehouse administrator with management access', NOW(), NOW()),
    (gen_random_uuid(), 'operator', 'Standard operator with basic permissions', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- Step 2: Update company code from ADMIN to LOGSUR
-- ============================================================================
UPDATE companies 
SET code = 'LOGSUR', 
    name = 'Logística del Sur S.A.',
    subscription_plan = 'premium',
    is_verified = true,
    status = 'active'
WHERE code = 'ADMIN';

-- Verify the company exists with correct code
SELECT id, code, name, status FROM companies WHERE code = 'LOGSUR';

-- Step 3: Create/update warehouse
-- ============================================================================
INSERT INTO warehouses (id, company_id, name, address, created_at, updated_at)
VALUES (
    '22222222-2222-2222-2222-222222222222',
    (SELECT id FROM companies WHERE code = 'LOGSUR'),
    'Depósito Central',
    'Av. Corrientes 1234, Buenos Aires',
    NOW(),
    NOW()
)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    address = EXCLUDED.address;

-- Step 4: Create/update demo users with proper roles and warehouse assignment
-- ============================================================================
DO $$
DECLARE
    v_company_id UUID;
    v_warehouse_id UUID;
    v_owner_role_id UUID;
    v_admin_role_id UUID;
    v_operator_role_id UUID;
BEGIN
    -- Get company and warehouse IDs
    SELECT id INTO v_company_id FROM companies WHERE code = 'LOGSUR';
    SELECT id INTO v_warehouse_id FROM warehouses WHERE company_id = v_company_id LIMIT 1;
    
    -- Get role IDs
    SELECT id INTO v_owner_role_id FROM roles WHERE name = 'company_owner';
    SELECT id INTO v_admin_role_id FROM roles WHERE name = 'warehouse_admin';
    SELECT id INTO v_operator_role_id FROM roles WHERE name = 'operator';

    -- Update existing admin user
    UPDATE users 
    SET company_id = v_company_id,
        warehouse_id = v_warehouse_id,
        role = 'company_owner',
        role_id = v_owner_role_id,
        status = 'active',
        is_verified = true
    WHERE username = 'admin' AND email = 'admin@logsur.com';

    -- Create warehouse admin
    INSERT INTO users (id, company_id, warehouse_id, email, username, password_hash, role, role_id, status, is_verified, created_at, updated_at)
    VALUES (
        gen_random_uuid(),
        v_company_id,
        v_warehouse_id,
        'jefe@logsur.com',
        'jefedeposito',
        '$2a$10$8poL3PM1HC4NFaJpFSbfneBbrjSDOCG.M0g1x4B11v5xE4n6KcEWu',
        'warehouse_admin',
        v_admin_role_id,
        'active',
        true,
        NOW(),
        NOW()
    )
    ON CONFLICT (email) DO UPDATE SET
        username = EXCLUDED.username,
        role = EXCLUDED.role,
        role_id = EXCLUDED.role_id,
        status = EXCLUDED.status;

    -- Create operators
    INSERT INTO users (id, company_id, warehouse_id, email, username, password_hash, role, role_id, status, is_verified, created_at, updated_at)
    VALUES 
        (gen_random_uuid(), v_company_id, v_warehouse_id, 'miguel@logsur.com', 'm.gomez', '$2a$10$8poL3PM1HC4NFaJpFSbfneBbrjSDOCG.M0g1x4B11v5xE4n6KcEWu', 'operator', v_operator_role_id, 'active', true, NOW(), NOW()),
        (gen_random_uuid(), v_company_id, v_warehouse_id, 'juan@logsur.com', 'j.perez', '$2a$10$8poL3PM1HC4NFaJpFSbfneBbrjSDOCG.M0g1x4B11v5xE4n6KcEWu', 'operator', v_operator_role_id, 'active', true, NOW(), NOW()),
        (gen_random_uuid(), v_company_id, v_warehouse_id, 'lucia@logsur.com', 'l.rodriguez', '$2a$10$8poL3PM1HC4NFaJpFSbfneBbrjSDOCG.M0g1x4B11v5xE4n6KcEWu', 'operator', v_operator_role_id, 'inactive', true, NOW(), NOW())
    ON CONFLICT (email) DO UPDATE SET
        username = EXCLUDED.username,
        role = EXCLUDED.role,
        role_id = EXCLUDED.role_id,
        status = EXCLUDED.status;
END $$;

-- Step 5: Create subscriptions for all LOGSUR users
-- ============================================================================
INSERT INTO subscriptions (id, user_id, status, device_connected, offline_mode, connected_mode, premium_features, created_at, updated_at)
SELECT 
    gen_random_uuid(),
    u.id,
    'trialing',
    false,
    true,
    true,
    true,
    NOW(),
    NOW()
FROM users u
JOIN companies c ON u.company_id = c.id
WHERE c.code = 'LOGSUR'
ON CONFLICT (user_id) DO NOTHING;

-- Step 6: Create demo inbound notes (remitos)
-- ============================================================================
INSERT INTO inbound_notes (id, company_id, warehouse_id, remito_number, client_number, client_name, origin, package_qty, scanned_qty, status, ocr_confidence, created_at, updated_at, received_at)
SELECT 
    gen_random_uuid(),
    c.id,
    w.id,
    'R-' || LPAD((series.n)::text, 6, '0'),
    'CLI-' || LPAD((series.n)::text, 5, '0'),
    CASE series.n % 5
        WHEN 0 THEN 'Ferretería El Martillo S.A.'
        WHEN 1 THEN 'Distribuidora Norte S.R.L.'
        WHEN 2 THEN 'Comercial Sur S.A.'
        WHEN 3 THEN 'Importadora Este S.A.'
        ELSE 'Exportadora Oeste S.R.L.'
    END,
    CASE series.n % 3
        WHEN 0 THEN 'Mendoza'
        WHEN 1 THEN 'Córdoba'
        ELSE 'Rosario'
    END,
    5 + (series.n % 20),
    0,
    'pending',
    0.85 + (series.n % 15)::float / 100,
    NOW() - (series.n || ' hours')::interval,
    NOW() - (series.n || ' hours')::interval,
    NOW() - (series.n || ' hours')::interval
FROM generate_series(1, 10) AS series(n)
CROSS JOIN (SELECT id FROM companies WHERE code = 'LOGSUR') c
CROSS JOIN (SELECT id FROM warehouses WHERE company_id = (SELECT id FROM companies WHERE code = 'LOGSUR') LIMIT 1) w
ON CONFLICT (remito_number, company_id) DO NOTHING;

-- Step 7: Create demo outbound lists (listas de reparto)
-- ============================================================================
INSERT INTO outbound_lists (id, company_id, warehouse_id, list_number, driver_name, driver_lastname, vehicle_plate, status, issue_date, created_at, updated_at)
SELECT 
    gen_random_uuid(),
    c.id,
    w.id,
    series.n,
    CASE series.n % 3
        WHEN 0 THEN 'Juan'
        WHEN 1 THEN 'Pedro'
        ELSE 'Martín'
    END,
    CASE series.n % 3
        WHEN 0 THEN 'González'
        WHEN 1 THEN 'Rodríguez'
        ELSE 'Fernández'
    END,
    'AA' || LPAD((100 + series.n)::text, 3, '0') || 'BB',
    CASE 
        WHEN series.n = 1 THEN 'issued'
        WHEN series.n = 2 THEN 'in_transit'
        WHEN series.n = 3 THEN 'delivered'
        ELSE 'pending'
    END,
    CURRENT_DATE - (series.n || ' days')::interval,
    NOW() - (series.n || ' days')::interval,
    NOW() - (series.n || ' days')::interval
FROM generate_series(1, 5) AS series(n)
CROSS JOIN (SELECT id FROM companies WHERE code = 'LOGSUR') c
CROSS JOIN (SELECT id FROM warehouses WHERE company_id = (SELECT id FROM companies WHERE code = 'LOGSUR') LIMIT 1) w
ON CONFLICT (list_number, company_id) DO NOTHING;

-- Step 8: Add outbound lines connecting to inbound notes
-- ============================================================================
INSERT INTO outbound_lines (id, outbound_list_id, inbound_note_id, delivery_number, recipient_name, recipient_lastname, recipient_address, recipient_phone, package_qty, status, created_at, updated_at)
SELECT 
    gen_random_uuid(),
    ol.id,
    inn.id,
    'ENT-' || LPAD((row_number() over ())::text, 5, '0'),
    'Juan',
    'Pérez',
    'Av. Libertador 1234',
    '11-5555-1234',
    3,
    'pending',
    NOW(),
    NOW()
FROM outbound_lists ol
JOIN inbound_notes inn ON inn.company_id = ol.company_id
WHERE ol.company_id = (SELECT id FROM companies WHERE code = 'LOGSUR')
LIMIT 15
ON CONFLICT DO NOTHING;

-- Step 9: Create demo device registration
-- ============================================================================
INSERT INTO devices (id, company_id, warehouse_id, device_uuid, platform, model, status, name, fingerprint, registered_at, created_at)
VALUES (
    gen_random_uuid(),
    (SELECT id FROM companies WHERE code = 'LOGSUR'),
    (SELECT id FROM warehouses WHERE company_id = (SELECT id FROM companies WHERE code = 'LOGSUR') LIMIT 1),
    'demo-device-mobile-001',
    'android',
    'Samsung Galaxy S23',
    'active',
    'Teléfono Móvil Demo',
    'demo-fingerprint-001',
    NOW(),
    NOW()
)
ON CONFLICT (device_uuid) DO NOTHING;

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================

-- Verify company
SELECT 'COMPANY' as entity, code, name, status FROM companies WHERE code = 'LOGSUR';

-- Verify warehouse  
SELECT 'WAREHOUSE' as entity, name, address FROM warehouses WHERE company_id = (SELECT id FROM companies WHERE code = 'LOGSUR');

-- Verify users
SELECT 'USER' as entity, username, role, status, email
FROM users 
WHERE company_id = (SELECT id FROM companies WHERE code = 'LOGSUR')
ORDER BY role;

-- Verify counts
SELECT 
    'Inbound Notes (Remitos)' as entity, COUNT(*) as count
FROM inbound_notes 
WHERE company_id = (SELECT id FROM companies WHERE code = 'LOGSUR')
UNION ALL
SELECT 
    'Outbound Lists' as entity, COUNT(*) as count
FROM outbound_lists 
WHERE company_id = (SELECT id FROM companies WHERE code = 'LOGSUR')
UNION ALL
SELECT 
    'Outbound Lines' as entity, COUNT(*) as count
FROM outbound_lines ol
JOIN outbound_lists oll ON ol.outbound_list_id = oll.id
WHERE oll.company_id = (SELECT id FROM companies WHERE code = 'LOGSUR');

-- ============================================================================
-- SEEDING COMPLETE!
-- ============================================================================
-- 
-- Credentials for testing:
-- Company Code: LOGSUR
-- Password for all users: demo1234
--
-- Users:
--   - admin (company_owner)
--   - jefedeposito (warehouse_admin)  
--   - m.gomez (operator)
--   - j.perez (operator)
--   - l.rodriguez (operator - inactive)
--
-- Data:
--   - 10 inbound remitos
--   - 5 outbound lists (repartos)
--   - 15 outbound lines (remitos assigned to lists)
-- ============================================================================
