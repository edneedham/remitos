ALTER TABLE waitlist_entries
    DROP COLUMN IF EXISTS delivery_notes_per_day_band,
    DROP COLUMN IF EXISTS processing_mode,
    DROP COLUMN IF EXISTS digital_application,
    DROP COLUMN IF EXISTS warehouse_count;
