ALTER TABLE billing_invoices
    DROP COLUMN IF EXISTS fx_effective_date,
    DROP COLUMN IF EXISTS fx_source,
    DROP COLUMN IF EXISTS ars_per_usd,
    DROP COLUMN IF EXISTS usd_list_amount;
