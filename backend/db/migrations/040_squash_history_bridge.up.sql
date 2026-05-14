-- Squash bridge: production DBs may still be at schema_migrations.version 39 from the
-- pre-squash migration line. golang-migrate requires consecutive version files; this repo
-- jumps from 000001 (v1) to 046+. Versions 40–45 are intentional no-ops so migrate can reach
-- the waitlist migrations. Before deploying, confirm this database already has any DDL
-- that old migrations 040–045 would have applied (or that those versions never shipped).
SELECT 1;
