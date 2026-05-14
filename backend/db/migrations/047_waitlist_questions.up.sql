-- Idempotent: safe if some columns already exist (partial applies / hotfix DDL).
ALTER TABLE waitlist_entries ADD COLUMN IF NOT EXISTS delivery_notes_per_day_band TEXT;
ALTER TABLE waitlist_entries ADD COLUMN IF NOT EXISTS processing_mode TEXT;
ALTER TABLE waitlist_entries ADD COLUMN IF NOT EXISTS digital_application TEXT;
ALTER TABLE waitlist_entries ADD COLUMN IF NOT EXISTS warehouse_count INTEGER;
