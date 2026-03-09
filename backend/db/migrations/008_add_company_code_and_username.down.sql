-- Remove company code and username columns
ALTER TABLE users DROP COLUMN IF EXISTS warehouse_id;
ALTER TABLE users DROP COLUMN IF EXISTS username;
ALTER TABLE companies DROP COLUMN IF EXISTS code;
