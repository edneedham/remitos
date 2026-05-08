ALTER TABLE billing_invoices
    DROP COLUMN IF EXISTS factura_tipo,
    DROP COLUMN IF EXISTS factura_pto_vta,
    DROP COLUMN IF EXISTS factura_numero,
    DROP COLUMN IF EXISTS factura_cae,
    DROP COLUMN IF EXISTS factura_cae_vto,
    DROP COLUMN IF EXISTS factura_emitted_at,
    DROP COLUMN IF EXISTS factura_request_id,
    DROP COLUMN IF EXISTS factura_last_error,
    DROP COLUMN IF EXISTS factura_attempts;
