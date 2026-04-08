-- ============================================================================
-- QUICK SETUP: Create essential tables if they don't exist
-- Run this first if you get "relation does not exist" errors
-- ============================================================================

-- 1. Create roles table (essential for user creation)
CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Insert roles
INSERT INTO roles (name) VALUES 
    ('company_owner'),
    ('warehouse_admin'),
    ('operator'),
    ('read_only')
ON CONFLICT (name) DO NOTHING;

-- 2. Create companies table
CREATE TABLE IF NOT EXISTS companies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) UNIQUE,
    cuit VARCHAR(20),
    status VARCHAR(50) DEFAULT 'active',
    is_verified BOOLEAN DEFAULT false,
    subscription_plan VARCHAR(50) DEFAULT 'free',
    subscription_expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    archived_at TIMESTAMP
);

-- 3. Create warehouses table
CREATE TABLE IF NOT EXISTS warehouses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id),
    name VARCHAR(255) NOT NULL,
    address VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_warehouses_company_id ON warehouses(company_id);

-- 4. Create users table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID REFERENCES companies(id),
    warehouse_id UUID REFERENCES warehouses(id),
    email VARCHAR(255) UNIQUE,
    username VARCHAR(255),
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50),
    role_id UUID REFERENCES roles(id),
    status VARCHAR(50) DEFAULT 'active',
    is_verified BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_company_id ON users(company_id);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- 5. Create subscriptions table
CREATE TABLE IF NOT EXISTS subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id),
    status VARCHAR(50) DEFAULT 'trialing',
    device_connected BOOLEAN DEFAULT false,
    offline_mode BOOLEAN DEFAULT true,
    connected_mode BOOLEAN DEFAULT true,
    premium_features BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. Create refresh_tokens table
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    token_hash VARCHAR(255) NOT NULL,
    device_name VARCHAR(255),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- 7. Create devices table
CREATE TABLE IF NOT EXISTS devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID REFERENCES companies(id),
    warehouse_id UUID REFERENCES warehouses(id),
    device_uuid VARCHAR(255) UNIQUE,
    platform VARCHAR(50),
    model VARCHAR(255),
    status VARCHAR(50) DEFAULT 'active',
    name VARCHAR(255),
    fingerprint VARCHAR(500),
    registered_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 8. Create inbound_notes table (for remitos)
CREATE TABLE IF NOT EXISTS inbound_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id),
    warehouse_id UUID REFERENCES warehouses(id),
    remito_number VARCHAR(100),
    client_number VARCHAR(100),
    client_name VARCHAR(255),
    origin VARCHAR(255),
    package_qty INTEGER DEFAULT 0,
    scanned_qty INTEGER DEFAULT 0,
    status VARCHAR(50) DEFAULT 'pending',
    ocr_confidence FLOAT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    received_at TIMESTAMP,
    UNIQUE(remito_number, company_id)
);

CREATE INDEX IF NOT EXISTS idx_inbound_notes_company_id ON inbound_notes(company_id);
CREATE INDEX IF NOT EXISTS idx_inbound_notes_status ON inbound_notes(status);

-- 9. Create outbound_lists table (for repartos)
CREATE TABLE IF NOT EXISTS outbound_lists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id),
    warehouse_id UUID REFERENCES warehouses(id),
    list_number INTEGER NOT NULL,
    driver_name VARCHAR(255),
    driver_lastname VARCHAR(255),
    vehicle_plate VARCHAR(50),
    status VARCHAR(50) DEFAULT 'pending',
    issue_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(list_number, company_id)
);

CREATE INDEX IF NOT EXISTS idx_outbound_lists_company_id ON outbound_lists(company_id);
CREATE INDEX IF NOT EXISTS idx_outbound_lists_status ON outbound_lists(status);

-- 10. Create outbound_lines table
CREATE TABLE IF NOT EXISTS outbound_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    outbound_list_id UUID NOT NULL REFERENCES outbound_lists(id),
    inbound_note_id UUID REFERENCES inbound_notes(id),
    delivery_number VARCHAR(100),
    recipient_name VARCHAR(255),
    recipient_lastname VARCHAR(255),
    recipient_address VARCHAR(500),
    recipient_phone VARCHAR(50),
    package_qty INTEGER DEFAULT 0,
    status VARCHAR(50) DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_outbound_lines_outbound_list_id ON outbound_lines(outbound_list_id);

-- ============================================================================
-- SEED DATA
-- ============================================================================

-- Step 1: Create LOGSUR company
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

-- Step 2: Create warehouse
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

-- Step 3: Create demo users
DO $$
DECLARE
    v_company_id UUID := '11111111-1111-1111-1111-111111111111';
    v_warehouse_id UUID := '22222222-2222-2222-2222-222222222222';
    v_owner_role_id UUID;
    v_admin_role_id UUID;
    v_operator_role_id UUID;
BEGIN
    -- Get role IDs
    SELECT id INTO v_owner_role_id FROM roles WHERE name = 'company_owner';
    SELECT id INTO v_admin_role_id FROM roles WHERE name = 'warehouse_admin';
    SELECT id INTO v_operator_role_id FROM roles WHERE name = 'operator';

    -- Create admin user (company_owner)
    INSERT INTO users (id, company_id, warehouse_id, email, username, password_hash, role, role_id, status, is_verified, created_at, updated_at)
    VALUES (
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        v_company_id,
        v_warehouse_id,
        'admin@logsur.com',
        'admin',
        '$2a$10$8poL3PM1HC4NFaJpFSbfneBbrjSDOCG.M0g1x4B11v5xE4n6KcEWu',
        'company_owner',
        v_owner_role_id,
        'active',
        true,
        NOW(),
        NOW()
    )
    ON CONFLICT (id) DO UPDATE SET
        username = EXCLUDED.username,
        password_hash = EXCLUDED.password_hash,
        status = EXCLUDED.status;

    -- Create warehouse admin
    INSERT INTO users (id, company_id, warehouse_id, email, username, password_hash, role, role_id, status, is_verified, created_at, updated_at)
    VALUES (
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
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
    ON CONFLICT (id) DO UPDATE SET
        username = EXCLUDED.username,
        password_hash = EXCLUDED.password_hash,
        status = EXCLUDED.status;

    -- Create operators
    INSERT INTO users (id, company_id, warehouse_id, email, username, password_hash, role, role_id, status, is_verified, created_at, updated_at)
    VALUES 
        ('cccccccc-cccc-cccc-cccc-cccccccccccc', v_company_id, v_warehouse_id, 'miguel@logsur.com', 'm.gomez', '$2a$10$8poL3PM1HC4NFaJpFSbfneBbrjSDOCG.M0g1x4B11v5xE4n6KcEWu', 'operator', v_operator_role_id, 'active', true, NOW(), NOW()),
        ('dddddddd-dddd-dddd-dddd-dddddddddddd', v_company_id, v_warehouse_id, 'juan@logsur.com', 'j.perez', '$2a$10$8poL3PM1HC4NFaJpFSbfneBbrjSDOCG.M0g1x4B11v5xE4n6KcEWu', 'operator', v_operator_role_id, 'active', true, NOW(), NOW()),
        ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', v_company_id, v_warehouse_id, 'lucia@logsur.com', 'l.rodriguez', '$2a$10$8poL3PM1HC4NFaJpFSbfneBbrjSDOCG.M0g1x4B11v5xE4n6KcEWu', 'operator', v_operator_role_id, 'inactive', true, NOW(), NOW())
    ON CONFLICT (id) DO UPDATE SET
        username = EXCLUDED.username,
        password_hash = EXCLUDED.password_hash,
        status = EXCLUDED.status;
END $$;

-- Step 4: Create subscriptions
INSERT INTO subscriptions (id, user_id, status, device_connected, offline_mode, connected_mode, premium_features, created_at, updated_at)
SELECT 
    gen_random_uuid(),
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

-- Step 5: Create demo device
INSERT INTO devices (id, company_id, warehouse_id, device_uuid, platform, model, status, name, fingerprint, registered_at, created_at)
VALUES (
    gen_random_uuid(),
    '11111111-1111-1111-1111-111111111111',
    '22222222-2222-2222-2222-222222222222',
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
-- VERIFICATION
-- ============================================================================
SELECT 'ROLES CREATED' as status, COUNT(*) as count FROM roles;
SELECT 'COMPANY' as entity, code, name FROM companies WHERE code = 'LOGSUR';
SELECT 'USERS' as entity, username, role, status FROM users WHERE company_id = '11111111-1111-1111-1111-111111111111' ORDER BY role;
