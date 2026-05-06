# Failure check: Remitos (6-month risk register)

This document captures **plausible failure modes** for the Remitos stack as the product and customer base grow. It is not a postmortem; it is a **checklist** for engineering, support, and ops to revisit before scale forces the issues.

**Stack (summary):** Android app (offline-first, Room, optional cloud sync) · Go API (PostgreSQL) · Next.js site (Vercel) · Billing (USD catalog, ARS charges via MEP + Mercado Pago).

---

## 1. Billing and renewals (revenue and trust)

**What goes wrong**

- **Catalog drift:** USD list prices and plan labels must stay aligned between the backend (e.g. `internal/billing/pricing.go`) and the website (e.g. `website/src/app/lib/planCatalog.ts`). Drift produces “the site said X, I was charged Y” support load.
- **FX and disputes:** Charges depend on live MEP (e.g. bolsa/dolarapi) with fallback env rates. Without **persisting FX and effective date per invoice**, reconciling a charge months later is painful and error-prone.
- **Renewal reliability:** Recurring charges depend on Mercado Pago behavior, saved cards, idempotency, and webhooks. Issuer and regional rules differ; automatic renewal sweeps are opt-in and must be proven in production before wide enablement.
- **Operational gaps:** Manual renewal triggers, stub modes in dev, and multiple env flags mean production misconfiguration can look like “billing is broken” for a subset of customers.

**Checks**

- [x] Single source of truth for catalog USD prices (or automated contract test that Go and web catalogs match) (`backend/internal/billing/pricing_catalog_contract_test.go`).
- [x] `billing_invoices` (or equivalent) stores `ars_per_usd`, `fx_source`, `fx_effective_date`, and snapshot fields needed for support and accounting (see `billing-doc.md` recommendations; migration `038_billing_invoice_fx_snapshot`, renewal path populates on catalog-derived charges).
- [x] Runbook for failed renewal: retry policy, dunning, grace period, and how `subscription_expires_at` interacts with app entitlement (`RENEWAL_FAILURE_RUNBOOK.md`).
- [ ] Webhook and secret rotation tested after each API/deploy change.

**References:** `billing-doc.md`, `To-Prod.md`, `backend/internal/billing/`, `backend/internal/payments/mercadopago/`.

---

## 2. Offline ↔ cloud sync (data trust)

**What goes wrong**

- **Multi-device / offline latency:** Users expect the phone to be authoritative. Timestamp-based full sync and server upserts can **mask conflicts** until two people or two devices diverge; resolution may be “last write wins” or silent skips in edge cases (e.g. tenant or `cloud_id` boundaries).
- **Entitlement vs sync:** If web billing says “no download” or API gates uploads while the app still has local data, users perceive **data loss** or “the app broke” when sync is partial.
- **Support narrative:** Without clear in-app messaging for “sync failed / device revoked / user suspended,” support cannot explain state.

**Checks**

- [x] Documented conflict policy (what wins, what is merged, what is rejected) and how it is reflected in UI (`SYNC_CONFLICT_POLICY.md`).
- [ ] Scenarios tested: long offline period, second device login, revoked device, suspended user.
- [ ] Server-side limits (document caps, company rules) explicitly aligned with billing and product copy.

**References:** `SYNC_CONFLICT_POLICY.md`, `android/.../SyncManager.kt`, `android/.../SyncService.kt`, `backend/internal/handlers/sync.go`, `backend/internal/repository/sync.go`.

---

## 3. Feature flags and release coordination

**What goes wrong**

- **Cross-surface mismatch:** Offline vs backend modes and flags (`FeatureFlags`, `enableCloudSync`, `enableImageUpload`, `enableBackendOcr`) can diverge across **app build**, **API deployment**, and **website** config. Symptoms: intermittent 403s, uploads that never complete, or OCR paths that differ by environment.
- **“Works on my build”:** QA and production differ by `BACKEND_BASE_URL`, signing, and flags—bugs reproduce only for customers.

**Checks**

- [ ] Matrix of flags × surfaces (Android, API, web) for each release.
- [ ] Smoke test from a **production-like** build against staging/prod API before wide rollout.
- [ ] Changelog or release notes mention flag changes.

**References:** `README.md` (feature flags), `android/.../FeatureFlags.kt`, `RemitosApplication.kt`.

---

## 4. Activation and funnel (growth vs reality)

**What goes wrong**

- **Instrumented funnel without product parity:** Plans (e.g. `TRIAL_ONBOARDING_ACTIVATION_PLAN.md`) assume flows such as iOS or desktop; if the shipped product is Android-first, **metrics mislead** investment.
- **Drop-off misattribution:** Failure to download, log in, or complete first scan is often **environmental** (network, device policy, email delivery), not copy.

**Checks**

- [ ] Funnel events verified end-to-end in production with a real trial account.
- [ ] Platform CTAs match **shipping** clients; placeholders removed or gated.
- [ ] Deep links and “send link to phone” (if implemented) monitored for failure rates.

**References:** `TRIAL_ONBOARDING_ACTIVATION_PLAN.md`, website dashboard and signup flows.

---

## 5. Operations and infrastructure

**What goes wrong**

- **Migration drift:** Postgres migrations (`backend/db/migrations/`) applied differently across Neon branches or environments → subtle runtime errors or partial schema.
- **Secrets and webhooks:** Rotating API keys, JWT secrets, Mercado Pago tokens, or billing secrets without a checklist breaks **payments or auth** silently for some routes only.
- **Multi-provider blast radius:** Neon + Vercel + GCP (e.g. Cloud Run) + GCS + email increases **mean time to diagnose** when one leg fails.

**Checks**

- [ ] One documented migration path for production (who runs migrations, from where, rollback stance).
- [ ] Health checks and alerting on API, DB connectivity, webhook receipt, and renewal job outcomes.
- [ ] `SEED_LOCAL_DEV_USERS` and mock payment flags confirmed **never** enabled in production (`To-Prod.md`).

**References:** `To-Prod.md`, `backend/main.go` (migrations, seed), `.github/workflows/`.

---

## 6. Summary: highest leverage before scale

| Area              | If ignored for six months                         |
|-------------------|---------------------------------------------------|
| Billing + FX      | Support and finance become the bottleneck.       |
| Sync semantics    | Field teams lose trust in the product.            |
| Flags + releases  | Unreproducible bugs and slow releases.            |
| Funnel vs reality | Wrong product bets from misleading metrics.       |
| Ops + migrations  | Avoidable outages and payment incidents.          |

---

## Revision

Update this file when major architecture, billing, or sync behavior changes. Last reviewed: **2026-05-04** (initial draft).
