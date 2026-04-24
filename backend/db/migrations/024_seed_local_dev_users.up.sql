-- NOTE:
-- Dev fixture seed SQL used to live in this migration, but that meant every environment
-- would get test accounts just by running migrations.
--
-- Fixtures are now gated behind SEED_LOCAL_DEV_USERS=true and executed from main.go using:
--   db/seed/local_dev_users.sql
--
-- This migration remains as a no-op version anchor for databases that already applied 024
-- when it contained data.

SELECT 1;
