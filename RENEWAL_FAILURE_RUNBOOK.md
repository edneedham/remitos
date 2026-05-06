# Renewal failure runbook

This runbook defines how to operate failed subscription renewals in production.

Scope:
- Paid plans (`pyme`, `empresa`, and other paid aliases in code)
- Automatic renewals driven by `StartBillingRenewalSweep`
- Manual retries via `POST /internal/billing/trigger-renewal`

## Current policy (as implemented)

- Retry policy:
  - Automatic renewal sweep runs on `BILLING_RENEWAL_POLL_MINUTES` (default: 60).
  - Each sweep attempts renewal for companies where `subscription_expires_at <= NOW()` and paid-plan/card criteria match.
  - Manual retry is always available through the internal renewal endpoint.
- Dunning:
  - One renewal-failure email per failed pending invoice row (`renewal_failure_notice_sent_at` idempotency).
  - Separate lapse notice email once per expired paid period (`subscription_lapse_notice_sent_for_expires_at` idempotency).
- Grace period:
  - A fixed 3-day grace window is applied after paid-period expiry.
  - Entitlement remains active while `now < subscription_expires_at + 72h`.

## Entitlement semantics and `subscription_expires_at`

`CompanyHasAppDownloadAccess(now, company)` grants access when:
- trial is active (`trial_ends_at > now`), or
- plan is paid and:
  - `subscription_expires_at` is `NULL` (treated as active), or
  - `subscription_expires_at + 72h > now`.

Access is denied when:
- company is archived, or status is not active, or
- paid plan has passed grace: `subscription_expires_at + 72h <= now`, and trial is not active.

Operational implication:
- A failed renewal attempt does not extend entitlement.
- Entitlement returns as soon as a later successful renewal extends `subscription_expires_at`.
- Renewal extensions are anchored to the prior `subscription_expires_at` boundary (when present), so
  late-paid days are not free days in the next cycle.

## Detection and triage

1. Confirm sweep is enabled:
   - `BILLING_AUTOMATIC_RENEWAL_ENABLED=true`
   - `BILLING_RENEWAL_SECRET` configured (for manual trigger fallback)
2. Check logs:
   - `billing renewal sweep: run failed`
   - `renewal failure email:*`
3. Inspect DB:
   - `companies.subscription_expires_at`, `subscription_plan`, `mp_customer_id`, `mp_card_id`
   - `billing_invoices` newest rows for company (`pending` vs `paid`, `mp_payment_id`, email flags)
4. Verify payment method is valid (card updated in panel if needed).

### Quick SQL checks

Use these snippets during incident triage (replace `<company_id>`):

- Canonical script in repo: `backend/db/ops/renewal_failure_triage.sql`
- Example:
  - `psql "$DATABASE_URL" -v company_id="'<uuid>'" -f backend/db/ops/renewal_failure_triage.sql`

```sql
-- Company billing state and entitlement boundary
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
WHERE id = '<company_id>'::uuid;
```

```sql
-- Most recent invoices and email flags
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
WHERE company_id = '<company_id>'::uuid
ORDER BY issued_at DESC, created_at DESC
LIMIT 20;
```

```sql
-- Due paid companies waiting for renewal sweep
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
```

## Recovery procedure

1. Confirm company is eligible:
   - active company
   - paid plan
   - non-empty MP customer/card ids
2. Trigger manual retry:
   - Endpoint: `POST /internal/billing/trigger-renewal`
   - Header: `X-Billing-Secret: <secret>`
   - Body example:
     - `company_id`: target company UUID
     - `amount_minor`: `0` (derive from catalog USD x current MEP path)
     - `currency`: `ARS`
     - `description`: `Suscripción mensual`
     - `extend_months`: `1`
3. Verify success:
   - response `charged=true`
   - invoice marked `paid` with `mp_payment_id`
   - `companies.subscription_expires_at` extended by one month

If still failing:
- ask customer to refresh payment method
- retry after payment method update
- escalate with MP payment/rejection context (issuer rejection, auth, token/card invalid, etc.)

## Alerts and SLO guardrails

Use these thresholds for paging/urgent response:

- Renewal sweep failures:
  - page if `billing renewal sweep: run failed` appears continuously for 3+ sweeps, or
  - page if `due_companies` backlog grows for 2 consecutive hours.
- Payment approval health:
  - investigate if approval rate for renewal charges drops below 85% for the last 24h.
- Notification reliability:
  - investigate if failure/lapse emails cannot be sent for 30+ minutes.

## Ownership and escalation

- Primary owner: backend/on-call engineer for billing.
- Secondary owner: product/support lead for customer communications.
- External escalation: Mercado Pago support with:
  - `mp_payment_id`,
  - rejection reason from API/logs,
  - timestamp and environment,
  - affected `company_id` and invoice id.

## Post-incident checklist

- Confirm all due companies were retried successfully or are tracked for follow-up.
- Confirm `subscription_expires_at` reflects expected anchored-cycle behavior.
- Confirm duplicate dunning was not sent (`*_sent_*` idempotency fields).
- Add timeline and root cause summary to internal notes.
- Create follow-up tickets for any systemic issue (issuer pattern, webhook drift, retries, observability gaps).

## Customer communications timeline

- T-3 days before expiry: renewal reminder email job (estimate and upcoming renewal date).
- On failed automatic charge: renewal failure email (per pending invoice).
- After paid period lapses: lapse notice email (once per expired period).

## Safety notes

- Keep `SEED_LOCAL_DEV_USERS` and mock payment modes disabled in production.
- Prefer `amount_minor: 0` for retries unless finance/support explicitly needs a fixed overridden ARS amount for a one-off correction.
