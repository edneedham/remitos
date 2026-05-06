-- Renewal failure triage helpers
-- Usage examples:
--   psql "$DATABASE_URL" -v company_id="'<uuid>'" -f backend/db/ops/renewal_failure_triage.sql
--   psql "$DATABASE_URL" -v company_id="'a1000000-0000-4000-8000-000000000001'" -f backend/db/ops/renewal_failure_triage.sql
--
-- Notes:
-- - Pass company_id including single quotes via psql -v (see examples above).
-- - This script is read-only.

\echo '=== Company billing state and entitlement boundary ==='
SELECT
  id,
  name,
  subscription_plan,
  subscription_expires_at,
  (subscription_expires_at + INTERVAL '72 hours') AS grace_ends_at,
  status,
  archived_at,
  mp_customer_id,
  mp_card_id
FROM companies
WHERE id = :company_id::uuid;

\echo '=== Recent billing invoices + email flags ==='
SELECT
  id,
  status,
  amount_minor,
  currency,
  issued_at,
  mp_payment_id,
  renewal_failure_notice_sent_at,
  receipt_email_sent_at
FROM billing_invoices
WHERE company_id = :company_id::uuid
ORDER BY issued_at DESC, created_at DESC
LIMIT 20;

\echo '=== Due paid companies waiting for renewal sweep ==='
SELECT COUNT(*) AS due_companies
FROM companies
WHERE archived_at IS NULL
  AND (status = '' OR status = 'active')
  AND subscription_expires_at IS NOT NULL
  AND subscription_expires_at <= NOW()
  AND LOWER(COALESCE(subscription_plan, '')) IN (
    'premium', 'paid', 'subscriber', 'standard', 'pyme', 'empresa', 'corporativo'
  )
  AND COALESCE(TRIM(mp_customer_id), '') NOT IN ('', 'stub_mp_customer')
  AND COALESCE(TRIM(mp_card_id), '') != '';
