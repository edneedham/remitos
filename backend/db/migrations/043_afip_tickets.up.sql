-- Cache of AFIP/ARCA WSAA "ticket de acceso" (TA) per service.
-- AFIP rate-limits new TA requests per service: we MUST reuse Token + Sign until expiration_time.
-- One row per service ("wsfe", "ws_sr_padron_a5", "ws_sr_constancia_inscripcion", etc.).

CREATE TABLE IF NOT EXISTS afip_tickets (
    service           TEXT PRIMARY KEY,
    token             TEXT NOT NULL,
    sign              TEXT NOT NULL,
    generation_time   TIMESTAMPTZ NOT NULL,
    expiration_time   TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_afip_tickets_expires ON afip_tickets(expiration_time);
