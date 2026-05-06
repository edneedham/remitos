ALTER TABLE billing_invoices
    ADD COLUMN IF NOT EXISTS usd_list_amount NUMERIC(12,2),
    ADD COLUMN IF NOT EXISTS ars_per_usd NUMERIC(12,6),
    ADD COLUMN IF NOT EXISTS fx_source TEXT,
    ADD COLUMN IF NOT EXISTS fx_effective_date DATE;

COMMENT ON COLUMN billing_invoices.usd_list_amount IS 'Catalog USD list amount captured at invoice issuance (major units).';
COMMENT ON COLUMN billing_invoices.ars_per_usd IS 'Applied ARS per 1 USD used for conversion at issuance time.';
COMMENT ON COLUMN billing_invoices.fx_source IS 'FX quote source identifier used at issuance (for support/accounting traceability).';
COMMENT ON COLUMN billing_invoices.fx_effective_date IS 'Effective date of the FX quote used to compute this invoice.';
