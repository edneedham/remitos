-- Legacy users.role was CHECK (admin|operator). Canonical roles (company_owner, etc.) are stored
-- in role / role_id; the old check blocks valid rows.
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
