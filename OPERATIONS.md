# Operations & infrastructure

Companion to **`FAILURE-CHECK.md` §5** and **`To-Prod.md`**. Covers **migrations**, **health checks**, **alerting expectations**, and **production safety flags**.

---

## 1. Database migrations

| Item | Detail |
|------|--------|
| **Migration files** | `backend/db/migrations/` (golang-migrate SQL, versioned `*.up.sql`). |
| **When they run** | Automatically on **every API process start** (`runMigrations` in `backend/main.go`). If `migrate.Up()` fails, the server **exits** and the new revision does not serve traffic. |
| **Who runs them** | The **deployed container** (Cloud Run or local `go run`) against the DB configured in env (`DB_*`). No separate “migration job” is required for normal deploys. |
| **Neon branches** | Point the API at a branch connection string; the next deploy applies pending migrations to that branch (same mechanism). |
| **Rollback stance** | **Forward-fix** is default: ship a new migration to correct issues. **Automated `migrate Down`** is not wired in production. If you must revert schema, **restore a Neon backup** (or PITR) and redeploy a **previous container image** that matches that schema—coordinate with care. |
| **Manual / CI** | To run migrations without starting the full server (e.g. emergency), use `migrate` CLI with the same `file://db/migrations` source and your Postgres URL from repo root: `backend/` module directory context matters for `file://` paths—prefer running from `backend/` as in integration tests. |

---

## 2. Health and webhook endpoints

Use these for **uptime monitors**, **load balancer health checks**, and **post-deploy smoke** (see also **To-Prod.md §9**).

| Endpoint | Purpose |
|----------|---------|
| **`GET /health`** | **Liveness**: returns **200** `ok` if the process is listening. Does **not** verify the database. |
| **`GET /health/ready`** | **Readiness**: returns **200** `ok` if Postgres responds to a pool ping within ~2s; **503** `db unavailable` if not. Prefer this for “is the API usable?” alerts. |
| **`GET /webhooks/mercadopago`** | **Mercado Pago** URL validation (must return **200** when configuring the webhook in the MP dashboard). |

---

## 3. Alerting and observability (recommended)

| Signal | Suggestion |
|--------|------------|
| **API availability** | Synthetic check every 1–5 min on **`GET /health/ready`** (not only `/health`). Alert on non-200 or timeout. |
| **Webhook reachability** | After deploy or MP config change: **`GET /webhooks/mercadopago`** → 200 (see To-Prod §9). |
| **DB connectivity** | **`/health/ready`** failing while **`/health`** passes indicates DB/Neon issues. |
| **Background jobs** | Trial onboarding nudges, subscription renewal sweep/reminders, trial-ending notices log errors via `logger`. Use **Cloud Logging** (or equivalent) alerts on **error** level spikes or specific messages (`trial onboarding nudge`, renewal, webhook handler). |
| **Renewal outcomes** | See **`RENEWAL_FAILURE_RUNBOOK.md`** for billing-specific follow-up. |

Long-term: Mercado Pago **`x-signature`** verification for webhooks (see To-Prod §9 note).

---

## 4. Production safety flags (never enable in production)

Confirm in **Cloud Run / API env** and **Vercel** before go-live and after any env audit:

| Variable | Production requirement |
|----------|-------------------------|
| **`SEED_LOCAL_DEV_USERS`** | **Unset** or **`false`**. When `true`, applies `db/seed/local_dev_users.sql` (see **`LOCAL_DEV_ACCOUNTS.md`**). |
| **`SIGNUP_ALLOW_MOCK_PAYMENT`** | **`false`** (API). |
| **`BILLING_STUB_AUTO_CHARGE`** | **`false`** (API). |
| **`NEXT_PUBLIC_SIGNUP_USE_MOCK_PAYMENT`** | **`false`** or omit (Vercel). |

Reference: **`To-Prod.md`** §3 and §4, **`backend/config/config.go`**.

---

## 5. AFIP / ARCA (factura electrónica)

- **Secrets**: Client certificate + private key (PEM). Local: `AFIP_CERT_PEM` / `AFIP_KEY_PEM` or `*_PATH`. Production: **GCP Secret Manager** via `AFIP_CERT_SECRET_NAME` and `AFIP_KEY_SECRET_NAME`. Rotate before the cert expires and **restart** API instances so the in-process cert cache refreshes (WSAA ticket cache also lives in Postgres: `afip_tickets`).
- **Automatic retries**: After Mercado Pago settles a row, emission runs asynchronously. A **10-minute** sweep (`StartBillingFacturaEmitLoop`) picks up paid invoices with no `factura_emitted_at` and `factura_attempts < 10`. Manual retry (same secret as renewal): `POST /internal/billing/invoices/{invoice_id}/emit-factura` with header **`X-Billing-Secret`**.
- **Operational query** (alerts / support): count invoices stuck without CAE for more than a day:

  `SELECT COUNT(*) FROM billing_invoices WHERE status = 'paid' AND factura_emitted_at IS NULL AND issued_at < NOW() - INTERVAL '24 hours' AND factura_attempts < 10;`

- **AFIP or padrón down**: Rows accumulate `factura_last_error` and `billing_invoice_factura_attempts` audit lines; the emitter falls back to stored `companies.condicion_iva` when padrón is unavailable (see `internal/billing/factura_emitter.go`).
- **Padron cache (factura emission only)**: When `AFIP_PADRON_CACHE_HOURS` \> 0 (default **168**), if `companies.padron_synced_at` is within that window and `condicion_iva` is set, WSFE emission **skips** `getPersona` and uses stored receptor metadata (razón social / estado / domicilio remain whatever was last synced—signup, manual verify, or a prior emit refresh). Set **`AFIP_PADRON_CACHE_HOURS=0`** to call AFIP padron on every factura attempt.

---

## 6. CI / deploy

The **`backend.yml`** workflow builds, tests, and deploys Cloud Run on pushes to `main` under `backend/**`. After deploy, run the **To-Prod §9** smoke checks (health, webhook GET, optional login). Optionally add a step that **`curl`s `/health/ready`** against the public URL using a GitHub secret (not committed).

---

## Revision

Update when migration strategy, health routes, or critical env vars change.
