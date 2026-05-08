ALTER TABLE companies
    DROP COLUMN IF EXISTS cuit_verified_at,
    DROP COLUMN IF EXISTS razon_social,
    DROP COLUMN IF EXISTS condicion_iva,
    DROP COLUMN IF EXISTS domicilio_fiscal,
    DROP COLUMN IF EXISTS padron_synced_at;
