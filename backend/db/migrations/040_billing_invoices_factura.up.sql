-- AFIP factura electrónica (CAE) metadata for Mercado Pago subscription invoices.

ALTER TABLE billing_invoices
    ADD COLUMN IF NOT EXISTS factura_tipo SMALLINT,
    ADD COLUMN IF NOT EXISTS factura_pto_vta INTEGER,
    ADD COLUMN IF NOT EXISTS factura_numero BIGINT,
    ADD COLUMN IF NOT EXISTS factura_cae VARCHAR(20),
    ADD COLUMN IF NOT EXISTS factura_cae_vto DATE,
    ADD COLUMN IF NOT EXISTS factura_emitted_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS factura_request_id UUID,
    ADD COLUMN IF NOT EXISTS factura_last_error TEXT,
    ADD COLUMN IF NOT EXISTS factura_attempts INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN billing_invoices.factura_tipo IS 'AFIP comprobante type (1=A, 6=B, 11=C, …).';
COMMENT ON COLUMN billing_invoices.factura_request_id IS 'Idempotency id for FECAESolicitar retries.';
COMMENT ON COLUMN billing_invoices.factura_attempts IS 'How many emission attempts; capped in worker.';
