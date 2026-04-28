-- Normalize legacy users.role='admin' to the canonical role name company_owner.
-- Also aligns role_id with roles.name='company_owner' when available.

UPDATE users
SET
    role = 'company_owner',
    role_id = COALESCE(
        (SELECT id FROM roles WHERE name = 'company_owner' LIMIT 1),
        role_id
    ),
    updated_at = NOW()
WHERE role = 'admin';
