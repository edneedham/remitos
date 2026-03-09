-- Revert user unique constraints
ALTER TABLE users DROP COLUMN IF EXISTS external_id;
