ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS cuit_estado TEXT;

COMMENT ON COLUMN companies.cuit_estado IS 'AFIP padron estado clave (e.g. ACTIVO, INACTIVO).';
