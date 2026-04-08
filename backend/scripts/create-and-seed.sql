-- ============================================================================
-- CREATE BASE TABLES (if they don't exist from migrations)
-- This ensures the seed script can run even if migrations failed
-- ============================================================================

-- Create roles table
CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Seed roles
INSERT INTO roles (name) VALUES 
    ('company_owner'),
    ('warehouse_admin'),
    ('operator'),
    ('read_only')
ON CONFLICT (name) DO NOTHING;

-- Check if companies table exists and create minimal structure if needed
DO $$
BEGIN
    -- Create companies table if it doesn't exist
    CREATE TABLE IF NOT EXISTS companies (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        name VARCHAR(255) NOT NULL,
        code VARCHAR(50) UNIQUE,
        cuit VARCHAR(20),
        status VARCHAR(50) DEFAULT 'active',
        is_verified BOOLEAN DEFAULT false,
        subscription_plan VARCHAR(50) DEFAULT 'free',
        subscription_expires_at TIMESTAMP,
        created_at TIMESTAMP DEFAULT NOW(),
        updated_at TIMESTAMP DEFAULT NOW(),
        archived_at TIMESTAMP
    );
    
    -- Create warehouses table if it doesn't exist
    CREATE TABLE IF NOT EXISTS warehouses (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        company_id UUID NOT NULL,
        name VARCHAR(255) NOT NULL,
        address VARCHAR(500),
        created_at TIMESTAMP DEFAULT NOW(),
        updated_at TIMESTAMP DEFAULT NOW()
    );
    
    -- Create users table if it doesn't exist
    CREATE TABLE IF NOT EXISTS users (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        company_id UUID,
        warehouse_id UUID,
        email VARCHAR(255) UNIQUE,
        username VARCHAR(255),
        password_hash VARCHAR(255) NOT NULL,
        role VARCHAR(50),
        role_id UUID REFERENCES roles(id),
        status VARCHAR(50) DEFAULT 'active',
        is_verified BOOLEAN DEFAULT false,
        created_at TIMESTAMP DEFAULT NOW(),
        updated_at TIMESTAMP DEFAULT NOW()
    );
    
    -- Create subscriptions table if it doesn't exist
    CREATE TABLE IF NOT EXISTS subscriptions (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        user_id UUID NOT NULL UNIQUE REFERENCES users(id),
        status VARCHAR(50) DEFAULT 'trialing',
        device_connected BOOLEAN DEFAULT false,
        offline_mode BOOLEAN DEFAULT true,
        connected_mode BOOLEAN DEFAULT true,
        premium_features BOOLEAN DEFAULT true,
        created_at TIMESTAMP DEFAULT NOW(),
        updated_at TIMESTAMP DEFAULT NOW()
    );
    
    -- Create inbound_notes table if it doesn't exist
    CREATE TABLE IF NOT EXISTS inbound_notes (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        company_id UUID NOT NULL,
        warehouse_id UUID,
        remito_number VARCHAR(100),
        client_number VARCHAR(100),
        client_name VARCHAR(255),
        origin VARCHAR(255),
        package_qty INTEGER DEFAULT 0,
        scanned_qty INTEGER DEFAULT 0,
        status VARCHAR(50) DEFAULT 'pending',
        ocr_confidence FLOAT,
        created_at TIMESTAMP DEFAULT NOW(),
        updated_at TIMESTAMP DEFAULT NOW(),
        received_at TIMESTAMP,
        UNIQUE(remito_number, company_id)
    );
    
    -- Create outbound_lists table if it doesn't exist
    CREATE TABLE IF NOT EXISTS outbound_lists (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        company_id UUID NOT NULL,
        warehouse_id UUID,
        list_number INTEGER NOT NULL,
        driver_name VARCHAR(255),
        driver_lastname VARCHAR(255),
        vehicle_plate VARCHAR(50),
        status VARCHAR(50) DEFAULT 'pending',
        issue_date DATE,
        created_at TIMESTAMP DEFAULT NOW(),
        updated_at TIMESTAMP DEFAULT NOW(),
        UNIQUE(list_number, company_id)
    );
    
    -- Create outbound_lines table if it doesn't exist
    CREATE TABLE IF NOT EXISTS outbound_lines (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        outbound_list_id UUID NOT NULL,
        inbound_note_id UUID,
        delivery_number VARCHAR(100),
        recipient_name VARCHAR(255),
        recipient_lastname VARCHAR(255),
        recipient_address VARCHAR(500),
        recipient_phone VARCHAR(50),
        package_qty INTEGER DEFAULT 0,
        status VARCHAR(50) DEFAULT 'pending',
        created_at TIMESTAMP DEFAULT NOW(),
        updated_at TIMESTAMP DEFAULT NOW()
    );
    
    -- Create devices table if it doesn't exist
    CREATE TABLE IF NOT EXISTS devices (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        company_id UUID,
        warehouse_id UUID,
        device_uuid VARCHAR(255) UNIQUE,
        platform VARCHAR(50),
        model VARCHAR(255),
        status VARCHAR(50) DEFAULT 'active',
        name VARCHAR(255),
        fingerprint VARCHAR(500),
        registered_at TIMESTAMP,
        created_at TIMESTAMP DEFAULT NOW()
    );
    
EXCEPTION 
    WHEN others THEN
        RAISE NOTICE 'Table creation failed (may already exist): %', SQLERRM;
END $$;

-- Now run the main seed script
\i seed-production-complete.sql
