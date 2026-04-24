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
  max_warehouses, max_users,
  mp_customer_id, mp_card_id,
  archived_at
) VALUES (
  'a1000000-0000-4000-8000-000000000001',
  'SEEDTRIAL',
  'Seed Co — Trial',
  NOW(), NOW(),
  'active', true, 'trial',
  NULL, NOW() + INTERVAL '30 days',
  2, 5,
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
  max_warehouses, max_users,
  mp_customer_id, mp_card_id,
  archived_at
) VALUES (
  'b1000000-0000-4000-8000-000000000001',
  'SEEDPAID',
  'Seed Co — Paid',
  NOW(), NOW(),
  'active', true, 'premium',
  NULL, NULL,
  3, 10,
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
  max_warehouses, max_users,
  mp_customer_id, mp_card_id,
  archived_at
) VALUES (
  'c1000000-0000-4000-8000-000000000001',
  'SEEDFREE',
  'Seed Co — Free',
  NOW(), NOW(),
  'active', true, 'free',
  NULL, NULL,
  1, 2,
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
  max_warehouses, max_users,
  mp_customer_id, mp_card_id,
  archived_at
) VALUES (
  'd1000000-0000-4000-8000-000000000001',
  'SEEDEXPIRED',
  'Seed Co — Expired trial',
  NOW(), NOW(),
  'active', true, 'trial',
  NULL, NOW() - INTERVAL '1 day',
  1, 2,
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
  max_warehouses, max_users,
  mp_customer_id, mp_card_id,
  archived_at
) VALUES (
  'e1000000-0000-4000-8000-000000000001',
  'SEEDARCH',
  'Seed Co — Archived',
  NOW(), NOW(),
  'active', true, 'premium',
  NULL, NULL,
  1, 2,
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
