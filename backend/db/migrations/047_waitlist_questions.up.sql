ALTER TABLE waitlist_entries
    ADD COLUMN delivery_notes_per_day_band TEXT,
    ADD COLUMN processing_mode TEXT,
    ADD COLUMN digital_application TEXT,
    ADD COLUMN warehouse_count INTEGER;
