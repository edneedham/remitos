-- Dev fixtures: deterministic users/companies for local manual + automated testing.
-- Idempotent via fixed UUID primary keys.
--
-- Password for every seeded login: LocalSeed123!
-- Bcrypt hash generated with golang.org/x/crypto/bcrypt (DefaultCost).

-- Roles (migrations normally insert these; keep idempotent for fresh DBs)
INSERT INTO roles (name) VALUES
  ('company_owner'),
  ('warehouse_admin'),
  ('operator'),
  ('read_only')
ON CONFLICT (name) DO NOTHING;

-- Shared bcrypt hash for password: LocalSeed123!
-- (Plain literal so this migration is self-contained.)

-- ---------------------------------------------------------------------------
-- Company A — active trial (download allowed while trial_ends_at > now)
-- Company code: SEEDTRIAL
-- ---------------------------------------------------------------------------
INSERT INTO companies (
  id, code, name, created_at, updated_at,
  status, is_verified, subscription_plan,
  subscription_expires_at, trial_ends_at,
  max_warehouses, max_users, documents_monthly_limit,
  mp_customer_id, mp_card_id,
  archived_at
) VALUES (
  'a1000000-0000-4000-8000-000000000001',
  'SEEDTRIAL',
  'Seed Co — Trial',
  NOW(), NOW(),
  'active', true, 'trial',
  NULL, NOW() + INTERVAL '30 days',
  2, 5, 1000,
  'stub_mp_customer', 'stub_mp_card',
  NULL
)
ON CONFLICT (id) DO UPDATE SET
  code = EXCLUDED.code,
  name = EXCLUDED.name,
  status = EXCLUDED.status,
  is_verified = EXCLUDED.is_verified,
  subscription_plan = EXCLUDED.subscription_plan,
  subscription_expires_at = EXCLUDED.subscription_expires_at,
  trial_ends_at = EXCLUDED.trial_ends_at,
  max_warehouses = EXCLUDED.max_warehouses,
  max_users = EXCLUDED.max_users,
  documents_monthly_limit = EXCLUDED.documents_monthly_limit,
  mp_customer_id = EXCLUDED.mp_customer_id,
  mp_card_id = EXCLUDED.mp_card_id,
  archived_at = EXCLUDED.archived_at,
  updated_at = NOW();

INSERT INTO warehouses (id, company_id, name, address, created_at, updated_at)
VALUES (
  'a2000000-0000-4000-8000-000000000001',
  'a1000000-0000-4000-8000-000000000001',
  'Depósito Central',
  'Seed address',
  NOW(), NOW()
)
ON CONFLICT (id) DO UPDATE SET
  company_id = EXCLUDED.company_id,
  name = EXCLUDED.name,
  address = EXCLUDED.address,
  updated_at = NOW();

DO $$
DECLARE
  rid_owner uuid;
  rid_admin uuid;
  rid_operator uuid;
  rid_readonly uuid;
  seed_hash text := '$2a$10$KB81/FzuiHMDYmEsqqHvUOSfmx3aRi4xr6FGtQKOxotMJpi.TszMq';
BEGIN
  SELECT id INTO rid_owner FROM roles WHERE name = 'company_owner';
  SELECT id INTO rid_admin FROM roles WHERE name = 'warehouse_admin';
  SELECT id INTO rid_operator FROM roles WHERE name = 'operator';
  SELECT id INTO rid_readonly FROM roles WHERE name = 'read_only';

  INSERT INTO users (
    id, company_id, warehouse_id, email, username, password_hash,
    role, role_id, status, is_verified, created_at, updated_at
  ) VALUES
    (
      'a3000000-0000-4000-8000-000000000001',
      'a1000000-0000-4000-8000-000000000001',
      'a2000000-0000-4000-8000-000000000001',
      'seed-trial-owner@local.test',
      'trial_owner',
      seed_hash,
      'company_owner', rid_owner,
      'active', true,
      NOW(), NOW()
    ),
    (
      'a3000000-0000-4000-8000-000000000002',
      'a1000000-0000-4000-8000-000000000001',
      'a2000000-0000-4000-8000-000000000001',
      'seed-trial-admin@local.test',
      'trial_wh_admin',
      seed_hash,
      'warehouse_admin', rid_admin,
      'active', true,
      NOW(), NOW()
    ),
    (
      'a3000000-0000-4000-8000-000000000003',
      'a1000000-0000-4000-8000-000000000001',
      'a2000000-0000-4000-8000-000000000001',
      'seed-trial-operator@local.test',
      'trial_operator',
      seed_hash,
      'operator', rid_operator,
      'active', true,
      NOW(), NOW()
    ),
    (
      'a3000000-0000-4000-8000-000000000004',
      'a1000000-0000-4000-8000-000000000001',
      'a2000000-0000-4000-8000-000000000001',
      'seed-trial-readonly@local.test',
      'trial_readonly',
      seed_hash,
      'read_only', rid_readonly,
      'active', true,
      NOW(), NOW()
    )
  ON CONFLICT (id) DO UPDATE SET
    company_id = EXCLUDED.company_id,
    warehouse_id = EXCLUDED.warehouse_id,
    email = EXCLUDED.email,
    username = EXCLUDED.username,
    password_hash = EXCLUDED.password_hash,
    role = EXCLUDED.role,
    role_id = EXCLUDED.role_id,
    status = EXCLUDED.status,
    is_verified = EXCLUDED.is_verified,
    updated_at = NOW();
END $$;

INSERT INTO subscriptions (id, user_id, device_id, status, device_connected, features, created_at, updated_at)
VALUES
  (
    'a4000000-0000-4000-8000-000000000001',
    'a3000000-0000-4000-8000-000000000001',
    NULL,
    'trialing', false,
    '{"offlineMode": true, "connectedMode": true, "premiumFeatures": true}'::jsonb,
    NOW(), NOW()
  ),
  (
    'a4000000-0000-4000-8000-000000000002',
    'a3000000-0000-4000-8000-000000000002',
    NULL,
    'active', false,
    '{"offlineMode": true, "connectedMode": true, "premiumFeatures": true}'::jsonb,
    NOW(), NOW()
  ),
  (
    'a4000000-0000-4000-8000-000000000003',
    'a3000000-0000-4000-8000-000000000003',
    NULL,
    'active', false,
    '{"offlineMode": true, "connectedMode": true, "premiumFeatures": false}'::jsonb,
    NOW(), NOW()
  ),
  (
    'a4000000-0000-4000-8000-000000000004',
    'a3000000-0000-4000-8000-000000000004',
    NULL,
    'active', false,
    '{"offlineMode": true, "connectedMode": true, "premiumFeatures": false}'::jsonb,
    NOW(), NOW()
  )
ON CONFLICT (id) DO UPDATE SET
  user_id = EXCLUDED.user_id,
  device_id = EXCLUDED.device_id,
  status = EXCLUDED.status,
  device_connected = EXCLUDED.device_connected,
  features = EXCLUDED.features,
  updated_at = NOW();

-- ---------------------------------------------------------------------------
-- Company B — paid plan (download allowed; NULL expiry => always entitled)
-- Company code: SEEDPAID
-- ---------------------------------------------------------------------------
INSERT INTO companies (
  id, code, name, created_at, updated_at,
  status, is_verified, subscription_plan,
  subscription_expires_at, trial_ends_at,
  max_warehouses, max_users, documents_monthly_limit,
  mp_customer_id, mp_card_id,
  archived_at
) VALUES (
  'b1000000-0000-4000-8000-000000000001',
  'SEEDPAID',
  'Seed Co — Paid',
  NOW(), NOW(),
  'active', true, 'premium',
  NULL, NULL,
  3, 10, 5000,
  'stub_mp_customer', 'stub_mp_card',
  NULL
)
ON CONFLICT (id) DO UPDATE SET
  code = EXCLUDED.code,
  name = EXCLUDED.name,
  status = EXCLUDED.status,
  is_verified = EXCLUDED.is_verified,
  subscription_plan = EXCLUDED.subscription_plan,
  subscription_expires_at = EXCLUDED.subscription_expires_at,
  trial_ends_at = EXCLUDED.trial_ends_at,
  max_warehouses = EXCLUDED.max_warehouses,
  max_users = EXCLUDED.max_users,
  documents_monthly_limit = EXCLUDED.documents_monthly_limit,
  mp_customer_id = EXCLUDED.mp_customer_id,
  mp_card_id = EXCLUDED.mp_card_id,
  archived_at = EXCLUDED.archived_at,
  updated_at = NOW();

INSERT INTO warehouses (id, company_id, name, address, created_at, updated_at)
VALUES (
  'b2000000-0000-4000-8000-000000000001',
  'b1000000-0000-4000-8000-000000000001',
  'Depósito Central',
  'Seed address',
  NOW(), NOW()
)
ON CONFLICT (id) DO UPDATE SET
  company_id = EXCLUDED.company_id,
  name = EXCLUDED.name,
  address = EXCLUDED.address,
  updated_at = NOW();

DO $$
DECLARE
  rid_owner uuid;
  seed_hash text := '$2a$10$KB81/FzuiHMDYmEsqqHvUOSfmx3aRi4xr6FGtQKOxotMJpi.TszMq';
BEGIN
  SELECT id INTO rid_owner FROM roles WHERE name = 'company_owner';

  INSERT INTO users (
    id, company_id, warehouse_id, email, username, password_hash,
    role, role_id, status, is_verified, created_at, updated_at
  ) VALUES (
    'b3000000-0000-4000-8000-000000000001',
    'b1000000-0000-4000-8000-000000000001',
    'b2000000-0000-4000-8000-000000000001',
    'seed-paid-owner@local.test',
    'paid_owner',
    seed_hash,
    'company_owner', rid_owner,
    'active', true,
    NOW(), NOW()
  )
  ON CONFLICT (id) DO UPDATE SET
    company_id = EXCLUDED.company_id,
    warehouse_id = EXCLUDED.warehouse_id,
    email = EXCLUDED.email,
    username = EXCLUDED.username,
    password_hash = EXCLUDED.password_hash,
    role = EXCLUDED.role,
    role_id = EXCLUDED.role_id,
    status = EXCLUDED.status,
    is_verified = EXCLUDED.is_verified,
    updated_at = NOW();
END $$;

INSERT INTO subscriptions (id, user_id, device_id, status, device_connected, features, created_at, updated_at)
VALUES (
  'b4000000-0000-4000-8000-000000000001',
  'b3000000-0000-4000-8000-000000000001',
  NULL,
  'active', false,
  '{"offlineMode": true, "connectedMode": true, "premiumFeatures": true}'::jsonb,
  NOW(), NOW()
)
ON CONFLICT (id) DO UPDATE SET
  user_id = EXCLUDED.user_id,
  device_id = EXCLUDED.device_id,
  status = EXCLUDED.status,
  device_connected = EXCLUDED.device_connected,
  features = EXCLUDED.features,
  updated_at = NOW();

-- ---------------------------------------------------------------------------
-- Company C — free plan (no download entitlement)
-- Company code: SEEDFREE
-- ---------------------------------------------------------------------------
INSERT INTO companies (
  id, code, name, created_at, updated_at,
  status, is_verified, subscription_plan,
  subscription_expires_at, trial_ends_at,
  max_warehouses, max_users, documents_monthly_limit,
  mp_customer_id, mp_card_id,
  archived_at
) VALUES (
  'c1000000-0000-4000-8000-000000000001',
  'SEEDFREE',
  'Seed Co — Free',
  NOW(), NOW(),
  'active', true, 'free',
  NULL, NULL,
  1, 2, 120,
  NULL, NULL,
  NULL
)
ON CONFLICT (id) DO UPDATE SET
  code = EXCLUDED.code,
  name = EXCLUDED.name,
  status = EXCLUDED.status,
  is_verified = EXCLUDED.is_verified,
  subscription_plan = EXCLUDED.subscription_plan,
  subscription_expires_at = EXCLUDED.subscription_expires_at,
  trial_ends_at = EXCLUDED.trial_ends_at,
  max_warehouses = EXCLUDED.max_warehouses,
  max_users = EXCLUDED.max_users,
  documents_monthly_limit = EXCLUDED.documents_monthly_limit,
  mp_customer_id = EXCLUDED.mp_customer_id,
  mp_card_id = EXCLUDED.mp_card_id,
  archived_at = EXCLUDED.archived_at,
  updated_at = NOW();

INSERT INTO warehouses (id, company_id, name, address, created_at, updated_at)
VALUES (
  'c2000000-0000-4000-8000-000000000001',
  'c1000000-0000-4000-8000-000000000001',
  'Depósito Central',
  'Seed address',
  NOW(), NOW()
)
ON CONFLICT (id) DO UPDATE SET
  company_id = EXCLUDED.company_id,
  name = EXCLUDED.name,
  address = EXCLUDED.address,
  updated_at = NOW();

DO $$
DECLARE
  rid_owner uuid;
  seed_hash text := '$2a$10$KB81/FzuiHMDYmEsqqHvUOSfmx3aRi4xr6FGtQKOxotMJpi.TszMq';
BEGIN
  SELECT id INTO rid_owner FROM roles WHERE name = 'company_owner';

  INSERT INTO users (
    id, company_id, warehouse_id, email, username, password_hash,
    role, role_id, status, is_verified, created_at, updated_at
  ) VALUES (
    'c3000000-0000-4000-8000-000000000001',
    'c1000000-0000-4000-8000-000000000001',
    'c2000000-0000-4000-8000-000000000001',
    'seed-free-owner@local.test',
    'free_owner',
    seed_hash,
    'company_owner', rid_owner,
    'active', true,
    NOW(), NOW()
  )
  ON CONFLICT (id) DO UPDATE SET
    company_id = EXCLUDED.company_id,
    warehouse_id = EXCLUDED.warehouse_id,
    email = EXCLUDED.email,
    username = EXCLUDED.username,
    password_hash = EXCLUDED.password_hash,
    role = EXCLUDED.role,
    role_id = EXCLUDED.role_id,
    status = EXCLUDED.status,
    is_verified = EXCLUDED.is_verified,
    updated_at = NOW();
END $$;

INSERT INTO subscriptions (id, user_id, device_id, status, device_connected, features, created_at, updated_at)
VALUES (
  'c4000000-0000-4000-8000-000000000001',
  'c3000000-0000-4000-8000-000000000001',
  NULL,
  'active', false,
  '{"offlineMode": true, "connectedMode": true, "premiumFeatures": false}'::jsonb,
  NOW(), NOW()
)
ON CONFLICT (id) DO UPDATE SET
  user_id = EXCLUDED.user_id,
  device_id = EXCLUDED.device_id,
  status = EXCLUDED.status,
  device_connected = EXCLUDED.device_connected,
  features = EXCLUDED.features,
  updated_at = NOW();

-- ---------------------------------------------------------------------------
-- Company D — expired trial (download denied)
-- Company code: SEEDEXPIRED
-- ---------------------------------------------------------------------------
INSERT INTO companies (
  id, code, name, created_at, updated_at,
  status, is_verified, subscription_plan,
  subscription_expires_at, trial_ends_at,
  max_warehouses, max_users, documents_monthly_limit,
  mp_customer_id, mp_card_id,
  archived_at
) VALUES (
  'd1000000-0000-4000-8000-000000000001',
  'SEEDEXPIRED',
  'Seed Co — Expired trial',
  NOW(), NOW(),
  'active', true, 'trial',
  NULL, NOW() - INTERVAL '1 day',
  1, 2, 1000,
  'stub_mp_customer', 'stub_mp_card',
  NULL
)
ON CONFLICT (id) DO UPDATE SET
  code = EXCLUDED.code,
  name = EXCLUDED.name,
  status = EXCLUDED.status,
  is_verified = EXCLUDED.is_verified,
  subscription_plan = EXCLUDED.subscription_plan,
  subscription_expires_at = EXCLUDED.subscription_expires_at,
  trial_ends_at = EXCLUDED.trial_ends_at,
  max_warehouses = EXCLUDED.max_warehouses,
  max_users = EXCLUDED.max_users,
  documents_monthly_limit = EXCLUDED.documents_monthly_limit,
  mp_customer_id = EXCLUDED.mp_customer_id,
  mp_card_id = EXCLUDED.mp_card_id,
  archived_at = EXCLUDED.archived_at,
  updated_at = NOW();

INSERT INTO warehouses (id, company_id, name, address, created_at, updated_at)
VALUES (
  'd2000000-0000-4000-8000-000000000001',
  'd1000000-0000-4000-8000-000000000001',
  'Depósito Central',
  'Seed address',
  NOW(), NOW()
)
ON CONFLICT (id) DO UPDATE SET
  company_id = EXCLUDED.company_id,
  name = EXCLUDED.name,
  address = EXCLUDED.address,
  updated_at = NOW();

DO $$
DECLARE
  rid_owner uuid;
  seed_hash text := '$2a$10$KB81/FzuiHMDYmEsqqHvUOSfmx3aRi4xr6FGtQKOxotMJpi.TszMq';
BEGIN
  SELECT id INTO rid_owner FROM roles WHERE name = 'company_owner';

  INSERT INTO users (
    id, company_id, warehouse_id, email, username, password_hash,
    role, role_id, status, is_verified, created_at, updated_at
  ) VALUES (
    'd3000000-0000-4000-8000-000000000001',
    'd1000000-0000-4000-8000-000000000001',
    'd2000000-0000-4000-8000-000000000001',
    'seed-expired-owner@local.test',
    'expired_owner',
    seed_hash,
    'company_owner', rid_owner,
    'active', true,
    NOW(), NOW()
  )
  ON CONFLICT (id) DO UPDATE SET
    company_id = EXCLUDED.company_id,
    warehouse_id = EXCLUDED.warehouse_id,
    email = EXCLUDED.email,
    username = EXCLUDED.username,
    password_hash = EXCLUDED.password_hash,
    role = EXCLUDED.role,
    role_id = EXCLUDED.role_id,
    status = EXCLUDED.status,
    is_verified = EXCLUDED.is_verified,
    updated_at = NOW();
END $$;

INSERT INTO subscriptions (id, user_id, device_id, status, device_connected, features, created_at, updated_at)
VALUES (
  'd4000000-0000-4000-8000-000000000001',
  'd3000000-0000-4000-8000-000000000001',
  NULL,
  'canceled', false,
  '{"offlineMode": true, "connectedMode": true, "premiumFeatures": true}'::jsonb,
  NOW(), NOW()
)
ON CONFLICT (id) DO UPDATE SET
  user_id = EXCLUDED.user_id,
  device_id = EXCLUDED.device_id,
  status = EXCLUDED.status,
  device_connected = EXCLUDED.device_connected,
  features = EXCLUDED.features,
  updated_at = NOW();

-- ---------------------------------------------------------------------------
-- Company E — archived company (download denied)
-- Company code: SEEDARCH
-- ---------------------------------------------------------------------------
INSERT INTO companies (
  id, code, name, created_at, updated_at,
  status, is_verified, subscription_plan,
  subscription_expires_at, trial_ends_at,
  max_warehouses, max_users, documents_monthly_limit,
  mp_customer_id, mp_card_id,
  archived_at
) VALUES (
  'e1000000-0000-4000-8000-000000000001',
  'SEEDARCH',
  'Seed Co — Archived',
  NOW(), NOW(),
  'active', true, 'premium',
  NULL, NULL,
  1, 2, 3000,
  NULL, NULL,
  NOW()
)
ON CONFLICT (id) DO UPDATE SET
  code = EXCLUDED.code,
  name = EXCLUDED.name,
  status = EXCLUDED.status,
  is_verified = EXCLUDED.is_verified,
  subscription_plan = EXCLUDED.subscription_plan,
  subscription_expires_at = EXCLUDED.subscription_expires_at,
  trial_ends_at = EXCLUDED.trial_ends_at,
  max_warehouses = EXCLUDED.max_warehouses,
  max_users = EXCLUDED.max_users,
  documents_monthly_limit = EXCLUDED.documents_monthly_limit,
  mp_customer_id = EXCLUDED.mp_customer_id,
  mp_card_id = EXCLUDED.mp_card_id,
  archived_at = EXCLUDED.archived_at,
  updated_at = NOW();

INSERT INTO warehouses (id, company_id, name, address, created_at, updated_at)
VALUES (
  'e2000000-0000-4000-8000-000000000001',
  'e1000000-0000-4000-8000-000000000001',
  'Depósito Central',
  'Seed address',
  NOW(), NOW()
)
ON CONFLICT (id) DO UPDATE SET
  company_id = EXCLUDED.company_id,
  name = EXCLUDED.name,
  address = EXCLUDED.address,
  updated_at = NOW();

DO $$
DECLARE
  rid_owner uuid;
  seed_hash text := '$2a$10$KB81/FzuiHMDYmEsqqHvUOSfmx3aRi4xr6FGtQKOxotMJpi.TszMq';
BEGIN
  SELECT id INTO rid_owner FROM roles WHERE name = 'company_owner';

  INSERT INTO users (
    id, company_id, warehouse_id, email, username, password_hash,
    role, role_id, status, is_verified, created_at, updated_at
  ) VALUES (
    'e3000000-0000-4000-8000-000000000001',
    'e1000000-0000-4000-8000-000000000001',
    'e2000000-0000-4000-8000-000000000001',
    'seed-archived-owner@local.test',
    'archived_owner',
    seed_hash,
    'company_owner', rid_owner,
    'active', true,
    NOW(), NOW()
  )
  ON CONFLICT (id) DO UPDATE SET
    company_id = EXCLUDED.company_id,
    warehouse_id = EXCLUDED.warehouse_id,
    email = EXCLUDED.email,
    username = EXCLUDED.username,
    password_hash = EXCLUDED.password_hash,
    role = EXCLUDED.role,
    role_id = EXCLUDED.role_id,
    status = EXCLUDED.status,
    is_verified = EXCLUDED.is_verified,
    updated_at = NOW();
END $$;

INSERT INTO subscriptions (id, user_id, device_id, status, device_connected, features, created_at, updated_at)
VALUES (
  'e4000000-0000-4000-8000-000000000001',
  'e3000000-0000-4000-8000-000000000001',
  NULL,
  'active', false,
  '{"offlineMode": true, "connectedMode": true, "premiumFeatures": true}'::jsonb,
  NOW(), NOW()
)
ON CONFLICT (id) DO UPDATE SET
  user_id = EXCLUDED.user_id,
  device_id = EXCLUDED.device_id,
  status = EXCLUDED.status,
  device_connected = EXCLUDED.device_connected,
  features = EXCLUDED.features,
  updated_at = NOW();

-- ---------------------------------------------------------------------------
-- Billing invoices fixtures (for dashboard/billing UI)
-- Plan Premium: ARS 80.000,00 / mes → amount_minor 8000000 (centavos)
-- ---------------------------------------------------------------------------
INSERT INTO billing_invoices (
  id, company_id, amount_minor, currency, status, description, issued_at, mp_payment_id, created_at
) VALUES
  -- SEEDTRIAL: mostly pending/paid during trial lifecycle
  ('f1000000-0000-4000-8000-000000000001', 'a1000000-0000-4000-8000-000000000001', 0,      'ARS', 'paid',    'Alta de prueba gratis (7 días)',                   NOW() - INTERVAL '28 days', 'mp_trial_init_001',       NOW() - INTERVAL '28 days'),
  ('f1000000-0000-4000-8000-000000000002', 'a1000000-0000-4000-8000-000000000001', 8000000, 'ARS', 'pending', 'Suscripción mensual Premium (próximo ciclo)',      NOW() - INTERVAL '2 days',  'mp_trial_pending_002',    NOW() - INTERVAL '2 days'),
  ('f1000000-0000-4000-8000-000000000003', 'a1000000-0000-4000-8000-000000000001', 120000, 'ARS', 'void',    'Ajuste prorrateo descartado',                       NOW() - INTERVAL '1 day',   'mp_trial_void_003',       NOW() - INTERVAL '1 day'),

  -- SEEDPAID: regular successful billing history
  ('f2000000-0000-4000-8000-000000000001', 'b1000000-0000-4000-8000-000000000001', 8000000, 'ARS', 'paid',    'Plan Premium mensual',                              NOW() - INTERVAL '92 days', 'mp_paid_202601_001',      NOW() - INTERVAL '92 days'),
  ('f2000000-0000-4000-8000-000000000002', 'b1000000-0000-4000-8000-000000000001', 8000000, 'ARS', 'paid',    'Plan Premium mensual',                              NOW() - INTERVAL '62 days', 'mp_paid_202602_002',      NOW() - INTERVAL '62 days'),
  ('f2000000-0000-4000-8000-000000000003', 'b1000000-0000-4000-8000-000000000001', 8000000, 'ARS', 'paid',    'Plan Premium mensual',                              NOW() - INTERVAL '32 days', 'mp_paid_202603_003',      NOW() - INTERVAL '32 days'),
  ('f2000000-0000-4000-8000-000000000004', 'b1000000-0000-4000-8000-000000000001', 8000000, 'ARS', 'paid',    'Plan Premium mensual',                              NOW() - INTERVAL '2 days',  'mp_paid_202604_004',      NOW() - INTERVAL '2 days'),

  -- SEEDFREE: no invoice yet (kept intentionally empty)

  -- SEEDEXPIRED: charge attempt failed after trial expiry
  ('f4000000-0000-4000-8000-000000000001', 'd1000000-0000-4000-8000-000000000001', 8000000, 'ARS', 'pending', 'Cobro automático post-prueba (reintento pendiente)', NOW() - INTERVAL '3 days',  'mp_expired_pending_001',  NOW() - INTERVAL '3 days'),
  ('f4000000-0000-4000-8000-000000000002', 'd1000000-0000-4000-8000-000000000001', 8000000, 'ARS', 'void',    'Intento de cobro anulado por rechazo del emisor',   NOW() - INTERVAL '2 days',  'mp_expired_void_002',     NOW() - INTERVAL '2 days'),

  -- SEEDARCH: historical payments before archival
  ('f5000000-0000-4000-8000-000000000001', 'e1000000-0000-4000-8000-000000000001', 8000000, 'ARS', 'paid',    'Plan Premium mensual',                              NOW() - INTERVAL '120 days','mp_arch_paid_001',        NOW() - INTERVAL '120 days'),
  ('f5000000-0000-4000-8000-000000000002', 'e1000000-0000-4000-8000-000000000001', 8000000, 'ARS', 'paid',    'Plan Premium mensual',                              NOW() - INTERVAL '90 days', 'mp_arch_paid_002',        NOW() - INTERVAL '90 days')
ON CONFLICT (id) DO UPDATE SET
  company_id = EXCLUDED.company_id,
  amount_minor = EXCLUDED.amount_minor,
  currency = EXCLUDED.currency,
  status = EXCLUDED.status,
  description = EXCLUDED.description,
  issued_at = EXCLUDED.issued_at,
  mp_payment_id = EXCLUDED.mp_payment_id;

-- ---------------------------------------------------------------------------
-- Registered devices (phones used to scan remitos in the app)
-- Every company that has inbound_notes seeded below must have at least one device
-- so dashboard “Dispositivos” matches the story that scans come from phones.
-- ---------------------------------------------------------------------------
INSERT INTO devices (
  id, company_id, warehouse_id,
  device_uuid, platform, model, os_version, app_version,
  status, approved_by, approved_at,
  registered_at, last_seen_at, name
) VALUES
  (
    'a8000000-0000-4000-8000-000000000001',
    'a1000000-0000-4000-8000-000000000001',
    'a2000000-0000-4000-8000-000000000001',
    'seed-trial-scan-phone-001',
    'android',
    'Samsung Galaxy A54',
    '14',
    '1.4.0',
    'active',
    'a3000000-0000-4000-8000-000000000001',
    NOW() - INTERVAL '90 days',
    NOW() - INTERVAL '90 days',
    NOW() - INTERVAL '45 minutes',
    'Teléfono depósito (seed)'
  ),
  (
    'b8000000-0000-4000-8000-000000000001',
    'b1000000-0000-4000-8000-000000000001',
    'b2000000-0000-4000-8000-000000000001',
    'seed-paid-scan-phone-001',
    'android',
    'Motorola Edge 40',
    '14',
    '1.4.0',
    'active',
    'b3000000-0000-4000-8000-000000000001',
    NOW() - INTERVAL '180 days',
    NOW() - INTERVAL '180 days',
    NOW() - INTERVAL '20 minutes',
    'Teléfono depósito (seed)'
  )
ON CONFLICT (company_id, device_uuid) DO UPDATE SET
  warehouse_id = EXCLUDED.warehouse_id,
  model = EXCLUDED.model,
  os_version = EXCLUDED.os_version,
  app_version = EXCLUDED.app_version,
  status = EXCLUDED.status,
  approved_by = EXCLUDED.approved_by,
  approved_at = EXCLUDED.approved_at,
  last_seen_at = EXCLUDED.last_seen_at,
  name = EXCLUDED.name;

-- ---------------------------------------------------------------------------
-- Dashboard data fixtures (local-only)
-- Populates remitos + reparto tables with realistic operational activity.
-- ---------------------------------------------------------------------------

-- Trial company inbound notes (~940 docs MTD; curved ramp toward month-end, ~1000 limit)
-- created_at uses a power curve so cumulative usage rises slowly early and steepens toward "today".
INSERT INTO inbound_notes (
  id, cloud_id, company_id, warehouse_id,
  remito_num_cliente, remito_num_interno, cant_bultos_total,
  cuit_remitente, nombre_remitente, apellido_remitente,
  nombre_destinatario, apellido_destinatario, direccion_destinatario, telefono_destinatario,
  status, created_at, updated_at
) 
SELECT
  ('a5000000-0000-4000-8000-' || LPAD(gs.n::text, 12, '0'))::uuid AS id,
  ('a5100000-0000-4000-8000-' || LPAD(gs.n::text, 12, '0'))::uuid AS cloud_id,
  'a1000000-0000-4000-8000-000000000001'::uuid AS company_id,
  'a2000000-0000-4000-8000-000000000001'::uuid AS warehouse_id,
  'CLI-' || TO_CHAR(700000 + gs.n, 'FM000000') AS remito_num_cliente,
  'INT-' || TO_CHAR(950000 + gs.n, 'FM000000') AS remito_num_interno,
  1 + (gs.n % 18) AS cant_bultos_total,
  CASE gs.n % 8
    WHEN 0 THEN '30-71588432-1'
    WHEN 1 THEN '30-69211333-9'
    WHEN 2 THEN '30-70999221-5'
    WHEN 3 THEN '30-70111777-3'
    WHEN 4 THEN '30-68800333-8'
    WHEN 5 THEN '30-72644119-2'
    WHEN 6 THEN '30-69880551-0'
    ELSE '30-73002999-6'
  END AS cuit_remitente,
  CASE gs.n % 8
    WHEN 0 THEN 'Alimentos'
    WHEN 1 THEN 'Ferro'
    WHEN 2 THEN 'Distribuidora'
    WHEN 3 THEN 'Laboratorio'
    WHEN 4 THEN 'Mayorista'
    WHEN 5 THEN 'Textil'
    WHEN 6 THEN 'Electro'
    ELSE 'Farmacia'
  END AS nombre_remitente,
  CASE gs.n % 8
    WHEN 0 THEN 'Patagonia'
    WHEN 1 THEN 'Norte'
    WHEN 2 THEN 'Andina'
    WHEN 3 THEN 'Rivadavia'
    WHEN 4 THEN 'Metropolitana'
    WHEN 5 THEN 'Pampeana'
    WHEN 6 THEN 'Cuyo'
    ELSE 'Litoral'
  END AS apellido_remitente,
  CASE gs.n % 10
    WHEN 0 THEN 'Sofia'
    WHEN 1 THEN 'Mateo'
    WHEN 2 THEN 'Valentina'
    WHEN 3 THEN 'Joaquin'
    WHEN 4 THEN 'Camila'
    WHEN 5 THEN 'Thiago'
    WHEN 6 THEN 'Julieta'
    WHEN 7 THEN 'Benjamin'
    WHEN 8 THEN 'Lola'
    ELSE 'Franco'
  END AS nombre_destinatario,
  CASE gs.n % 10
    WHEN 0 THEN 'Quiroga'
    WHEN 1 THEN 'Mendez'
    WHEN 2 THEN 'Soria'
    WHEN 3 THEN 'Paz'
    WHEN 4 THEN 'Ledesma'
    WHEN 5 THEN 'Funes'
    WHEN 6 THEN 'Ruiz'
    WHEN 7 THEN 'Campos'
    WHEN 8 THEN 'Arias'
    ELSE 'Ponce'
  END AS apellido_destinatario,
  CASE gs.n % 7
    WHEN 0 THEN 'Av. Corrientes 1450, CABA'
    WHEN 1 THEN 'Bv. Oroño 932, Rosario'
    WHEN 2 THEN 'Av. Colón 1211, Córdoba'
    WHEN 3 THEN 'Godoy Cruz 245, Mendoza'
    WHEN 4 THEN 'Pellegrini 86, Mar del Plata'
    WHEN 5 THEN 'San Martín 1540, Neuquén'
    ELSE 'Independencia 776, Tucumán'
  END AS direccion_destinatario,
  '11-5' || LPAD((100000 + gs.n)::text, 6, '0') AS telefono_destinatario,
  CASE
    WHEN gs.n % 12 = 0 THEN 'Incidencia'
    WHEN gs.n % 9 = 0 THEN 'Devuelta'
    WHEN gs.n % 6 = 0 THEN 'Entregada'
    WHEN gs.n % 4 = 0 THEN 'EnRuta'
    ELSE 'Activa'
  END AS status,
  (
    date_trunc('month', NOW() AT TIME ZONE 'UTC')
    + (
        floor(
          power((gs.n - 1)::numeric / NULLIF(940 - 1, 0), 2.28)
          * GREATEST(EXTRACT(DAY FROM (NOW() AT TIME ZONE 'UTC'))::int - 1, 0)
        )::int || ' days'
      )::interval
    + make_interval(secs => ((gs.n * 7919) % 82800)::int)
  ) AS created_at,
  (
    date_trunc('month', NOW() AT TIME ZONE 'UTC')
    + (
        floor(
          power((gs.n - 1)::numeric / NULLIF(940 - 1, 0), 2.28)
          * GREATEST(EXTRACT(DAY FROM (NOW() AT TIME ZONE 'UTC'))::int - 1, 0)
        )::int || ' days'
      )::interval
    + make_interval(secs => (((gs.n * 7919) % 82800) + 900)::int)
  ) AS updated_at
FROM generate_series(1, 940) AS gs(n)
ON CONFLICT (id) DO UPDATE SET
  warehouse_id = EXCLUDED.warehouse_id,
  remito_num_cliente = EXCLUDED.remito_num_cliente,
  remito_num_interno = EXCLUDED.remito_num_interno,
  cant_bultos_total = EXCLUDED.cant_bultos_total,
  cuit_remitente = EXCLUDED.cuit_remitente,
  nombre_remitente = EXCLUDED.nombre_remitente,
  apellido_remitente = EXCLUDED.apellido_remitente,
  nombre_destinatario = EXCLUDED.nombre_destinatario,
  apellido_destinatario = EXCLUDED.apellido_destinatario,
  direccion_destinatario = EXCLUDED.direccion_destinatario,
  telefono_destinatario = EXCLUDED.telefono_destinatario,
  status = EXCLUDED.status,
  updated_at = EXCLUDED.updated_at;

-- Trial company outbound lists (daily operations with mixed states)
INSERT INTO outbound_lists (
  id, cloud_id, company_id, warehouse_id, list_number, issue_date,
  driver_nombre, driver_apellido, checklist_signature_path, checklist_signed_at,
  status, created_at, updated_at
)
SELECT
  ('a6000000-0000-4000-8000-' || LPAD(gs.n::text, 12, '0'))::uuid AS id,
  ('a6100000-0000-4000-8000-' || LPAD(gs.n::text, 12, '0'))::uuid AS cloud_id,
  'a1000000-0000-4000-8000-000000000001'::uuid AS company_id,
  'a2000000-0000-4000-8000-000000000001'::uuid AS warehouse_id,
  3000 + gs.n AS list_number,
  date_trunc('day', NOW() AT TIME ZONE 'UTC') - ((gs.n % 22) || ' days')::interval + INTERVAL '07:30' AS issue_date,
  CASE gs.n % 6
    WHEN 0 THEN 'Martin'
    WHEN 1 THEN 'Lorena'
    WHEN 2 THEN 'Ruben'
    WHEN 3 THEN 'Ariel'
    WHEN 4 THEN 'Carolina'
    ELSE 'Sergio'
  END AS driver_nombre,
  CASE gs.n % 6
    WHEN 0 THEN 'Perez'
    WHEN 1 THEN 'Gimenez'
    WHEN 2 THEN 'Rojas'
    WHEN 3 THEN 'Lopez'
    WHEN 4 THEN 'Cabrera'
    ELSE 'Moyano'
  END AS driver_apellido,
  '/seed/signatures/lista-' || (3000 + gs.n)::text || '.png' AS checklist_signature_path,
  date_trunc('day', NOW() AT TIME ZONE 'UTC') - ((gs.n % 22) || ' days')::interval + INTERVAL '08:15' AS checklist_signed_at,
  CASE
    WHEN gs.n <= 4 THEN 'Abierta'
    WHEN gs.n <= 10 THEN 'EnRuta'
    ELSE 'Completada'
  END AS status,
  date_trunc('day', NOW() AT TIME ZONE 'UTC') - ((gs.n % 22) || ' days')::interval + INTERVAL '07:10' AS created_at,
  date_trunc('day', NOW() AT TIME ZONE 'UTC') - ((gs.n % 22) || ' days')::interval + INTERVAL '19:30' AS updated_at
FROM generate_series(1, 24) AS gs(n)
ON CONFLICT (id) DO UPDATE SET
  list_number = EXCLUDED.list_number,
  issue_date = EXCLUDED.issue_date,
  driver_nombre = EXCLUDED.driver_nombre,
  driver_apellido = EXCLUDED.driver_apellido,
  checklist_signature_path = EXCLUDED.checklist_signature_path,
  checklist_signed_at = EXCLUDED.checklist_signed_at,
  status = EXCLUDED.status,
  updated_at = EXCLUDED.updated_at;

-- Trial company outbound lines (paired to remitos with delivery outcomes)
INSERT INTO outbound_lines (
  id, cloud_id, outbound_list_id, inbound_note_id,
  delivery_number, recipient_nombre, recipient_apellido, recipient_direccion, recipient_telefono,
  package_qty, allocated_package_ids, status, delivered_qty, returned_qty, missing_qty,
  created_at, updated_at
)
SELECT
  ('a7000000-0000-4000-8000-' || LPAD(gs.n::text, 12, '0'))::uuid AS id,
  ('a7100000-0000-4000-8000-' || LPAD(gs.n::text, 12, '0'))::uuid AS cloud_id,
  ('a6000000-0000-4000-8000-' || LPAD((((gs.n - 1) % 24) + 1)::text, 12, '0'))::uuid AS outbound_list_id,
  ('a5000000-0000-4000-8000-' || LPAD(gs.n::text, 12, '0'))::uuid AS inbound_note_id,
  'DEL-' || TO_CHAR(90000 + gs.n, 'FM00000') AS delivery_number,
  CASE gs.n % 8
    WHEN 0 THEN 'Sofia'
    WHEN 1 THEN 'Mateo'
    WHEN 2 THEN 'Valentina'
    WHEN 3 THEN 'Joaquin'
    WHEN 4 THEN 'Camila'
    WHEN 5 THEN 'Thiago'
    WHEN 6 THEN 'Julieta'
    ELSE 'Franco'
  END AS recipient_nombre,
  CASE gs.n % 8
    WHEN 0 THEN 'Quiroga'
    WHEN 1 THEN 'Mendez'
    WHEN 2 THEN 'Soria'
    WHEN 3 THEN 'Paz'
    WHEN 4 THEN 'Ledesma'
    WHEN 5 THEN 'Funes'
    WHEN 6 THEN 'Ruiz'
    ELSE 'Campos'
  END AS recipient_apellido,
  CASE gs.n % 6
    WHEN 0 THEN 'Av. Corrientes 1450, CABA'
    WHEN 1 THEN 'Bv. Oroño 932, Rosario'
    WHEN 2 THEN 'Av. Colón 1211, Córdoba'
    WHEN 3 THEN 'Godoy Cruz 245, Mendoza'
    WHEN 4 THEN 'Pellegrini 86, Mar del Plata'
    ELSE 'San Martín 1540, Neuquén'
  END AS recipient_direccion,
  '11-6' || LPAD((200000 + gs.n)::text, 6, '0') AS recipient_telefono,
  1 + (gs.n % 8) AS package_qty,
  'PKG' || gs.n::text || '-01,PKG' || gs.n::text || '-02' AS allocated_package_ids,
  CASE
    WHEN gs.n % 11 = 0 THEN 'Incidencia'
    WHEN gs.n % 5 = 0 THEN 'Entregada'
    WHEN gs.n % 3 = 0 THEN 'EnRuta'
    ELSE 'EnDeposito'
  END AS status,
  CASE WHEN gs.n % 5 = 0 THEN 1 + (gs.n % 6) ELSE 0 END AS delivered_qty,
  CASE WHEN gs.n % 11 = 0 THEN 1 ELSE 0 END AS returned_qty,
  CASE WHEN gs.n % 17 = 0 THEN 1 ELSE 0 END AS missing_qty,
  date_trunc('month', NOW() AT TIME ZONE 'UTC') + ((gs.n - 1) * INTERVAL '3 hours') + INTERVAL '10 hours' AS created_at,
  date_trunc('month', NOW() AT TIME ZONE 'UTC') + ((gs.n - 1) * INTERVAL '3 hours') + INTERVAL '12 hours' AS updated_at
FROM generate_series(1, 120) AS gs(n)
ON CONFLICT (id) DO UPDATE SET
  outbound_list_id = EXCLUDED.outbound_list_id,
  inbound_note_id = EXCLUDED.inbound_note_id,
  delivery_number = EXCLUDED.delivery_number,
  recipient_nombre = EXCLUDED.recipient_nombre,
  recipient_apellido = EXCLUDED.recipient_apellido,
  recipient_direccion = EXCLUDED.recipient_direccion,
  recipient_telefono = EXCLUDED.recipient_telefono,
  package_qty = EXCLUDED.package_qty,
  allocated_package_ids = EXCLUDED.allocated_package_ids,
  status = EXCLUDED.status,
  delivered_qty = EXCLUDED.delivered_qty,
  returned_qty = EXCLUDED.returned_qty,
  missing_qty = EXCLUDED.missing_qty,
  updated_at = EXCLUDED.updated_at;

-- ---------------------------------------------------------------------------
-- Paid company dashboard fixtures (steady-state operations)
-- ---------------------------------------------------------------------------

-- Paid company inbound notes (~4879 docs MTD ≈ 5000 plan limit)
-- Calendar day 1 of the month: 0 docs (cumulative chart starts at 0).
-- Activity is packed into the last ~25 days up to today (~4879/25 ≈ 195/day) with a mild
-- end-weighted curve so the cumulative line looks curved, not flat.
INSERT INTO inbound_notes (
  id, cloud_id, company_id, warehouse_id,
  remito_num_cliente, remito_num_interno, cant_bultos_total,
  cuit_remitente, nombre_remitente, apellido_remitente,
  nombre_destinatario, apellido_destinatario, direccion_destinatario, telefono_destinatario,
  status, created_at, updated_at
)
WITH paid_bounds AS (
  SELECT
    date_trunc('month', NOW() AT TIME ZONE 'UTC') AS ms,
    GREATEST(EXTRACT(DAY FROM (NOW() AT TIME ZONE 'UTC'))::int, 2) AS dom_end,
    LEAST(
      GREATEST(2, EXTRACT(DAY FROM (NOW() AT TIME ZONE 'UTC'))::int - 24),
      GREATEST(EXTRACT(DAY FROM (NOW() AT TIME ZONE 'UTC'))::int, 2)
    ) AS dom_start
)
SELECT
  ('b5000000-0000-4000-8000-' || LPAD(gs.n::text, 12, '0'))::uuid AS id,
  ('b5100000-0000-4000-8000-' || LPAD(gs.n::text, 12, '0'))::uuid AS cloud_id,
  'b1000000-0000-4000-8000-000000000001'::uuid AS company_id,
  'b2000000-0000-4000-8000-000000000001'::uuid AS warehouse_id,
  'CLI-' || TO_CHAR(810000 + gs.n, 'FM000000') AS remito_num_cliente,
  'INT-' || TO_CHAR(980000 + gs.n, 'FM000000') AS remito_num_interno,
  2 + (gs.n % 14) AS cant_bultos_total,
  CASE gs.n % 5
    WHEN 0 THEN '30-70200123-4'
    WHEN 1 THEN '30-71588990-1'
    WHEN 2 THEN '30-68811456-0'
    WHEN 3 THEN '30-72900110-2'
    ELSE '30-70199220-8'
  END AS cuit_remitente,
  CASE gs.n % 5
    WHEN 0 THEN 'Distribuidora'
    WHEN 1 THEN 'Mayorista'
    WHEN 2 THEN 'Laboratorio'
    WHEN 3 THEN 'Textil'
    ELSE 'Tecnored'
  END AS nombre_remitente,
  CASE gs.n % 5
    WHEN 0 THEN 'Sur'
    WHEN 1 THEN 'Central'
    WHEN 2 THEN 'Andes'
    WHEN 3 THEN 'Rivadavia'
    ELSE 'Litoral'
  END AS apellido_remitente,
  CASE WHEN gs.n % 7 = 0 THEN 'Entregada' WHEN gs.n % 4 = 0 THEN 'EnRuta' ELSE 'Activa' END AS status,
  CASE gs.n % 6
    WHEN 0 THEN 'Lucia'
    WHEN 1 THEN 'Tomas'
    WHEN 2 THEN 'Candela'
    WHEN 3 THEN 'Renzo'
    WHEN 4 THEN 'Bianca'
    ELSE 'Bruno'
  END AS nombre_destinatario,
  CASE gs.n % 6
    WHEN 0 THEN 'Arias'
    WHEN 1 THEN 'Funes'
    WHEN 2 THEN 'Molina'
    WHEN 3 THEN 'Paz'
    WHEN 4 THEN 'Campos'
    ELSE 'Ruiz'
  END AS apellido_destinatario,
  CASE gs.n % 4
    WHEN 0 THEN 'Av. Santa Fe 2100, CABA'
    WHEN 1 THEN 'Av. Pellegrini 1700, Rosario'
    WHEN 2 THEN 'Av. Colón 2500, Córdoba'
    ELSE 'San Martín 1320, Mendoza'
  END AS direccion_destinatario,
  '11-7' || LPAD((300000 + gs.n)::text, 6, '0') AS telefono_destinatario,
  (
    b.ms
    + (
        (b.dom_start + dom_slot.idx - 1) || ' days'
      )::interval
    + make_interval(secs => ((gs.n * 6143) % 82800)::int)
  ) AS created_at,
  (
    b.ms
    + (
        (b.dom_start + dom_slot.idx - 1) || ' days'
      )::interval
    + make_interval(secs => (((gs.n * 6143) % 82800) + 900)::int)
  ) AS updated_at
FROM generate_series(1, 4879) AS gs(n)
CROSS JOIN paid_bounds AS b
CROSS JOIN LATERAL (
  SELECT GREATEST(b.dom_end - b.dom_start + 1, 1) AS num_active_days
) AS nd
CROSS JOIN LATERAL (
  SELECT
    CASE
      WHEN nd.num_active_days <= 1 THEN 0
      ELSE LEAST(
        GREATEST(
          floor(
            (1::numeric - power(
              1::numeric - (gs.n - 1)::numeric / NULLIF(4879 - 1, 0),
              2.15
            )) * (nd.num_active_days - 1)
          )::int,
          0
        ),
        nd.num_active_days - 1
      )
    END AS idx
) AS dom_slot
ON CONFLICT (id) DO UPDATE SET
  warehouse_id = EXCLUDED.warehouse_id,
  remito_num_cliente = EXCLUDED.remito_num_cliente,
  remito_num_interno = EXCLUDED.remito_num_interno,
  cant_bultos_total = EXCLUDED.cant_bultos_total,
  cuit_remitente = EXCLUDED.cuit_remitente,
  nombre_remitente = EXCLUDED.nombre_remitente,
  apellido_remitente = EXCLUDED.apellido_remitente,
  nombre_destinatario = EXCLUDED.nombre_destinatario,
  apellido_destinatario = EXCLUDED.apellido_destinatario,
  direccion_destinatario = EXCLUDED.direccion_destinatario,
  telefono_destinatario = EXCLUDED.telefono_destinatario,
  status = EXCLUDED.status,
  updated_at = EXCLUDED.updated_at;

-- Paid company outbound lists
INSERT INTO outbound_lists (
  id, cloud_id, company_id, warehouse_id, list_number, issue_date,
  driver_nombre, driver_apellido, checklist_signature_path, checklist_signed_at,
  status, created_at, updated_at
)
SELECT
  ('b6000000-0000-4000-8000-' || LPAD(gs.n::text, 12, '0'))::uuid AS id,
  ('b6100000-0000-4000-8000-' || LPAD(gs.n::text, 12, '0'))::uuid AS cloud_id,
  'b1000000-0000-4000-8000-000000000001'::uuid AS company_id,
  'b2000000-0000-4000-8000-000000000001'::uuid AS warehouse_id,
  5000 + gs.n AS list_number,
  date_trunc('day', NOW() AT TIME ZONE 'UTC') - ((gs.n % 18) || ' days')::interval + INTERVAL '08:00' AS issue_date,
  CASE gs.n % 4 WHEN 0 THEN 'Diego' WHEN 1 THEN 'Marina' WHEN 2 THEN 'Pablo' ELSE 'Noelia' END AS driver_nombre,
  CASE gs.n % 4 WHEN 0 THEN 'Rios' WHEN 1 THEN 'Lopez' WHEN 2 THEN 'Mendez' ELSE 'Acuña' END AS driver_apellido,
  '/seed/signatures/paid-lista-' || (5000 + gs.n)::text || '.png' AS checklist_signature_path,
  date_trunc('day', NOW() AT TIME ZONE 'UTC') - ((gs.n % 18) || ' days')::interval + INTERVAL '08:25' AS checklist_signed_at,
  CASE WHEN gs.n <= 3 THEN 'Abierta' WHEN gs.n <= 8 THEN 'EnRuta' ELSE 'Completada' END AS status,
  date_trunc('day', NOW() AT TIME ZONE 'UTC') - ((gs.n % 18) || ' days')::interval + INTERVAL '07:30' AS created_at,
  date_trunc('day', NOW() AT TIME ZONE 'UTC') - ((gs.n % 18) || ' days')::interval + INTERVAL '18:40' AS updated_at
FROM generate_series(1, 18) AS gs(n)
ON CONFLICT (id) DO UPDATE SET
  list_number = EXCLUDED.list_number,
  issue_date = EXCLUDED.issue_date,
  driver_nombre = EXCLUDED.driver_nombre,
  driver_apellido = EXCLUDED.driver_apellido,
  checklist_signature_path = EXCLUDED.checklist_signature_path,
  checklist_signed_at = EXCLUDED.checklist_signed_at,
  status = EXCLUDED.status,
  updated_at = EXCLUDED.updated_at;

-- Paid company outbound lines
INSERT INTO outbound_lines (
  id, cloud_id, outbound_list_id, inbound_note_id,
  delivery_number, recipient_nombre, recipient_apellido, recipient_direccion, recipient_telefono,
  package_qty, allocated_package_ids, status, delivered_qty, returned_qty, missing_qty,
  created_at, updated_at
)
SELECT
  ('b7000000-0000-4000-8000-' || LPAD(gs.n::text, 12, '0'))::uuid AS id,
  ('b7100000-0000-4000-8000-' || LPAD(gs.n::text, 12, '0'))::uuid AS cloud_id,
  ('b6000000-0000-4000-8000-' || LPAD((((gs.n - 1) % 18) + 1)::text, 12, '0'))::uuid AS outbound_list_id,
  ('b5000000-0000-4000-8000-' || LPAD(gs.n::text, 12, '0'))::uuid AS inbound_note_id,
  'DEL-P-' || TO_CHAR(95000 + gs.n, 'FM00000') AS delivery_number,
  CASE gs.n % 5 WHEN 0 THEN 'Valeria' WHEN 1 THEN 'Nicolas' WHEN 2 THEN 'Agustina' WHEN 3 THEN 'Ezequiel' ELSE 'Martina' END AS recipient_nombre,
  CASE gs.n % 5 WHEN 0 THEN 'Gomez' WHEN 1 THEN 'Ponce' WHEN 2 THEN 'Arias' WHEN 3 THEN 'Molina' ELSE 'Ruiz' END AS recipient_apellido,
  CASE gs.n % 4
    WHEN 0 THEN 'Av. Santa Fe 2100, CABA'
    WHEN 1 THEN 'Av. Pellegrini 1700, Rosario'
    WHEN 2 THEN 'Av. Colón 2500, Córdoba'
    ELSE 'San Martín 1320, Mendoza'
  END AS recipient_direccion,
  '11-8' || LPAD((400000 + gs.n)::text, 6, '0') AS recipient_telefono,
  1 + (gs.n % 6) AS package_qty,
  'PD' || gs.n::text || '-01,PD' || gs.n::text || '-02' AS allocated_package_ids,
  CASE WHEN gs.n % 9 = 0 THEN 'Incidencia' WHEN gs.n % 4 = 0 THEN 'Entregada' WHEN gs.n % 3 = 0 THEN 'EnRuta' ELSE 'EnDeposito' END AS status,
  CASE WHEN gs.n % 4 = 0 THEN 1 + (gs.n % 5) ELSE 0 END AS delivered_qty,
  CASE WHEN gs.n % 9 = 0 THEN 1 ELSE 0 END AS returned_qty,
  CASE WHEN gs.n % 16 = 0 THEN 1 ELSE 0 END AS missing_qty,
  date_trunc('month', NOW() AT TIME ZONE 'UTC') + ((gs.n - 1) * INTERVAL '4 hours') + INTERVAL '11 hours' AS created_at,
  date_trunc('month', NOW() AT TIME ZONE 'UTC') + ((gs.n - 1) * INTERVAL '4 hours') + INTERVAL '13 hours' AS updated_at
FROM generate_series(1, 96) AS gs(n)
ON CONFLICT (id) DO UPDATE SET
  outbound_list_id = EXCLUDED.outbound_list_id,
  inbound_note_id = EXCLUDED.inbound_note_id,
  delivery_number = EXCLUDED.delivery_number,
  recipient_nombre = EXCLUDED.recipient_nombre,
  recipient_apellido = EXCLUDED.recipient_apellido,
  recipient_direccion = EXCLUDED.recipient_direccion,
  recipient_telefono = EXCLUDED.recipient_telefono,
  package_qty = EXCLUDED.package_qty,
  allocated_package_ids = EXCLUDED.allocated_package_ids,
  status = EXCLUDED.status,
  delivered_qty = EXCLUDED.delivered_qty,
  returned_qty = EXCLUDED.returned_qty,
  missing_qty = EXCLUDED.missing_qty,
  updated_at = EXCLUDED.updated_at;
