ALTER TABLE public.waitlist_entries
    ADD COLUMN IF NOT EXISTS product_updates_opt_in boolean NOT NULL DEFAULT false;
