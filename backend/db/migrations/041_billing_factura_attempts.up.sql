CREATE TABLE IF NOT EXISTS billing_invoice_factura_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES billing_invoices(id) ON DELETE CASCADE,
    attempted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ok BOOLEAN NOT NULL DEFAULT FALSE,
    error_code TEXT,
    error_msg TEXT,
    request_xml TEXT,
    response_xml TEXT
);

CREATE INDEX IF NOT EXISTS idx_billing_factura_attempts_invoice_id
    ON billing_invoice_factura_attempts(invoice_id, attempted_at DESC);
