# Local dev accounts (seeded fixtures)

These accounts are created from `local_dev_users.sql` **only when** the API is started with:

```bash
SEED_LOCAL_DEV_USERS=true
```

All accounts share the same password:

- **Password:** `LocalSeed123!`

## How to sign in (web)

Use the **company code** as `company_code`, and either the **email** or **username** as `username` (depending on the row).

## Accounts by scenario

### Active trial + multiple roles (good default for manual testing)

- **Company code:** `SEEDTRIAL`
- **Owner (company_owner)**
  - **Email:** `seed-trial-owner@local.test`
- **Warehouse admin**
  - **Username:** `trial_wh_admin`
- **Operator**
  - **Username:** `trial_operator`
- **Read-only**
  - **Username:** `trial_readonly`

### Paid plan (entitled like a paid customer)

- **Company code:** `SEEDPAID`
- **Owner**
  - **Email:** `seed-paid-owner@local.test`

### Free plan (not entitled to APK download)

- **Company code:** `SEEDFREE`
- **Owner**
  - **Email:** `seed-free-owner@local.test`

### Expired trial (not entitled)

- **Company code:** `SEEDEXPIRED`
- **Owner**
  - **Email:** `seed-expired-owner@local.test`

### Archived company (not entitled)

- **Company code:** `SEEDARCH`
- **Owner**
  - **Email:** `seed-archived-owner@local.test`
