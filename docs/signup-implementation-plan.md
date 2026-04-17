# Signup & trial billing — implementation plan

This plan completes the self-serve signup flow beyond what already exists: `POST /auth/signup/trial`, migration `023` (company trial + Mercado Pago ids), website `SignupTrialForm`, and Mercado Pago card tokenization (no charge at signup).

## Current baseline (done)

- Backend: trial company creation, warehouse, owner user, subscription `trialing`, MP customer + saved card ids stored on `companies`.
- Website: form + MP secure fields + optional mock mode for local dev.
- CORS + env vars documented in `website/.env.example` and `backend/config`.

---

## Phase 1 — Environment & deployment wiring

**Goal:** Signup works end-to-end in staging and production with real Mercado Pago (test + live keys as appropriate).

**Tasks:**

- [ ] **API (Cloud Run / server):** Set `MERCADOPAGO_ACCESS_TOKEN`, `CORS_ALLOWED_ORIGINS` (comma-separated origins for the marketing site), DB vars. Ensure `SIGNUP_ALLOW_MOCK_PAYMENT` is **unset** or `false` in production.
- [ ] **Website (Vercel / static host):** Set `NEXT_PUBLIC_API_URL`, `NEXT_PUBLIC_MERCADOPAGO_PUBLIC_KEY` (TEST vs APP key per environment). Ensure `NEXT_PUBLIC_SIGNUP_USE_MOCK_PAYMENT` is **off** in production.
- [ ] **Smoke test:** One full signup with a MP test card; confirm `companies.mp_*` populated and user can log in via existing auth paths.

---

## Phase 2 — Enforce trial entitlements (1 warehouse, 2 users)

**Goal:** Database fields `max_warehouses`, `max_users`, `trial_ends_at` are **enforced** in APIs that create users or warehouses.

**Tasks:**

- [ ] **Load company** (or minimal billing fields) in handlers that create:
  - additional **users** (e.g. admin/invite flows),
  - additional **warehouses**.
- [ ] **Rules:**
  - If `trial_ends_at` is set and `now > trial_ends_at`, reject or require paid state (coordinate with Phase 3).
  - Count active users / warehouses for the company; block when count would exceed `max_users` / `max_warehouses` (treat `NULL` limits as “unlimited” for legacy companies).
- [ ] **Tests:** Repository or handler tests for limit checks; document behavior for legacy tenants with `NULL` limits.

**Likely touchpoints:** `backend/internal/handlers/admin.go` (or user/warehouse creation), `internal/repository/company.go` (helpers: `CountUsers`, `CountWarehouses`, `GetCompanyBilling`).

---

## Phase 3 — Post-trial billing (Mercado Pago)

**Goal:** After the 7-day trial, charge the saved card (or transition to a subscription model) without storing raw card data.

**Tasks:**

- [ ] **Product decision:** One-off charge vs recurring subscription (Mercado Pago subscriptions / preapproval APIs differ by region—confirm Argentina docs).
- [ ] **Scheduler or job:** Cloud Scheduler → HTTPS endpoint or Cloud Run Job that:
  - selects companies where `trial_ends_at < now` and `subscription_plan = 'trial'` (or equivalent flag) and not yet billed;
  - creates a payment using stored `mp_customer_id` + `mp_card_id` (or MP’s recommended “saved card” charge path);
  - on success: set `subscription_plan`, `subscription_expires_at` / clear trial fields as designed;
  - on failure: mark past-due, notify (email — Phase 5), optional retry policy.
- [ ] **Webhooks:** Handle Mercado Pago payment notifications to reconcile state idempotently.
- [ ] **Secrets:** Webhook signing secret in config; no card data in logs.

---

## Phase 4 — Website & API hardening

**Goal:** Safer, clearer signup for production traffic.

**Tasks:**

- [ ] **Copy / legal:** Checkbox or link to Terms + Privacy; store acceptance timestamp if required (new column or audit table).
- [ ] **Abuse:** Rate limit `POST /auth/signup/trial` (per IP / per email domain)—middleware or edge (Cloud Run + Redis, or API gateway).
- [ ] **Errors:** Map MP and validation errors to user-friendly Spanish messages (already partially done).
- [ ] **Success path:** Optionally redirect to app download or login instructions with `company_code` (UX polish).

---

## Phase 5 — Notifications & support

**Tasks:**

- [ ] **Email:** Welcome email with company code; trial ending reminder; payment failed / card update flow (depends on MP capabilities).
- [ ] **Admin visibility:** Optional internal dashboard or query for trial companies expiring in N days.

---

## Phase 6 — Android (optional)

**Goal:** Align mobile with self-serve signup if desired.

**Tasks:**

- [ ] **Deep link** from app to `https://<site>/signup` or `/signup/m`.
- [ ] **Or** embedded WebView / Chrome Custom Tab for signup only (avoid duplicating MP Bricks in native unless necessary).

---

## Phase 7 — Observability

**Tasks:**

- [ ] Structured logs/metrics: signup success/failure, MP attach-card failures, post-trial job outcomes.
- [ ] Alerts on error rate spikes on `/auth/signup/trial` and on billing job failures.

---

## Suggested order

1. Phase 1 (env + smoke test)  
2. Phase 2 (limits) — unblocks fair use before billing goes live  
3. Phase 3 (billing + webhooks)  
4. Phases 4–7 in parallel where possible  

---

## References (code)

- Signup handler: `backend/internal/handlers/signup_trial.go`
- Mercado Pago client: `backend/internal/payments/mercadopago/client.go`
- Company trial columns: `backend/db/migrations/023_company_trial_and_mercado_pago.up.sql`
- Website form: `website/src/app/signup/SignupTrialForm.tsx`
