-- Billing invoices / comprobantes for web account (Mercado Pago or manual rows).

CREATE TABLE IF NOT EXISTS billing_invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    amount_minor BIGINT NOT NULL,
    currency VARCHAR(8) NOT NULL DEFAULT 'ARS',
    status VARCHAR(32) NOT NULL DEFAULT 'paid'
        CHECK (status IN ('paid', 'pending', 'void')),
    description TEXT,
    issued_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    mp_payment_id VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_billing_invoices_company_id ON billing_invoices(company_id);
CREATE INDEX IF NOT EXISTS idx_billing_invoices_issued_at ON billing_invoices(issued_at DESC);
