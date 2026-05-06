# Sync and conflict policy (implemented behavior)

This document describes **what the product does today** for offline-first sync between the Android app and the Go API. It is the reference for support and engineering when answering “what wins” questions.

**Primary code:** `android/.../SyncService.kt`, `android/.../SyncManager.kt`, `backend/internal/handlers/sync.go`, `backend/internal/repository/sync.go`.

---

## Model

- Each sync is effectively **upload pending local changes, then download server changes since `last_sync_timestamp`** (`POST /sync`).
- Entities are correlated by **`cloud_id`** (UUID string). The server stores inbound notes, outbound lists, and outbound lines keyed by `cloud_id` within a **company**.
- There is **no per-field version vector or revision clock** for conflict detection. The API returns **`conflicts: []`** always (placeholder).

---

## What wins (within one company)

### Server row identity

- **Inbound notes:** Upsert on `cloud_id`. If the row exists for another `company_id`, the upsert is **skipped** (silent).
- **Outbound lists:** Insert if missing by `cloud_id`, else **UPDATE** for matching `cloud_id` **and** `company_id`.
- **Outbound lines:** Upsert on `cloud_id` with full replacement of scalar fields on conflict.

### Concurrent edits (two devices / offline divergence)

- **Last successful sync that reaches the server wins** for the persisted row: the server applies the **latest upsert payload** it receives. There is **no merge** of divergent field-level edits from two offline writers for the same `cloud_id`.
- Afterward, other devices **pull** `updated_at > last_sync` and **overwrite local rows** that match the same `cloud_id` (Android merges server fields into existing entities—server values typically replace local once applied).

### Push vs pull ordering (same device)

1. Client sends unsynced outbound payloads.
2. Server applies upserts (if uploads allowed).
3. Server returns rows changed **since** `last_sync_timestamp`.

So **server state after step 2 is what gets replicated** to other clients on their next pull.

---

## Upload gating (billing / entitlement)

- Server applies upserts **only if** `billing.CompanyHasAppDownloadAccess` is true for the company at sync time (`backend/internal/handlers/sync.go`).
- If uploads are **blocked**, **no inbound/outbound upserts** run, but the handler **still returns** server-side rows since `last_sync_timestamp` so the device can **receive** updates. The JSON body includes **`uploads_applied`: false** when upserts were skipped (true when they ran); the Android app surfaces a **Snackbar** if local changes were pending while uploads were gated.
- Document limits: if `documents_monthly_limit` is set and would be exceeded by the incoming batch, sync responds **403** with a Spanish limit message.

---

## Document caps (catalog ↔ billing ↔ sync)

Enforced caps for self-serve catalog plans are defined once in **`billing.PlanLimitsByID`** (`backend/internal/billing/pricing.go`). They are copied onto **`companies.documents_monthly_limit`** (and warehouse/user caps) when a company’s plan is applied (signup, upgrades, renewal sweep).

| Surface | Role |
|--------|------|
| Public marketing | `website/src/app/lib/planCatalog.ts` perk strings (must match `PlanLimitsByID`; **`pricing_limits_catalog_contract_test.go`**) |
| Panel / app entitlement | `GET /auth/me` and related handlers expose `documents_monthly_limit`; dashboard usage (`DocumentUsageSection`, billing UI) |
| Sync | `POST /sync` compares MTD inbound note count + projected **new** notes in the batch against `documents_monthly_limit`; overage → **403** with body `Límite de documentos de tu plan alcanzado para este mes.` (`sync.go`) |
| Android | Failed sync surfaces API error text via **`SyncState.Error(message)`** (same handler message for limit errors) |

**Company rules** (warehouses, users) use the same plan caps when provisioning or changing plans; those checks live outside this sync handler (e.g. warehouse and operator endpoints). Corporativo uses **nil** limits in `PlanLimitsByID` (negotiated / manual).

---

## Scenario matrix (failure-check §2)

Support and QA can trace these behaviors to code paths:

| Scenario | Expected behavior | Primary code |
|----------|-------------------|----------------|
| Long offline then reconnect | Pending uploads run on next sync; server applies **last writer wins** for conflicting `cloud_id`; other devices receive updates on **pull** (`updated_at` > `last_sync_timestamp`). | `SyncManager.kt`, `SyncService.kt`, `sync.go`, `repository/sync.go` |
| Second device / same account | Same company: concurrent edits converge on **last successful upsert** to the API; no merge UI. | Same as § concurrent edits above |
| Device revoked | Before posting `/sync`, **`getUserStatus`**: `deviceStatus == "revoked"` → **`SyncState.DeviceRevoked`** → blocking dialog and re-registration flow (no merge of uploads). | `SyncManager.kt` (lines ~76–88), `DashboardScreen.kt`, `AuthInterceptor.kt` |
| User suspended | **`userStatus != "active"`** → **`SyncState.UserSuspended`** → blocking dialog and logout-style handling. | `SyncManager.kt`, `DashboardScreen.kt` |
| Monthly document cap exceeded | If the batch would exceed the cap, the **entire** `POST /sync` returns **403** (Spanish limit message) and no sync response body; a later sync with fewer new documents or after MTD window moves can succeed. | `sync.go` |

**Automated checks:** plan tier limits vs handler (`auth_change_plan_test.go`); USD catalog vs web (`pricing_catalog_contract_test.go`); marketing perk lines vs `PlanLimitsByID` (`pricing_limits_catalog_contract_test.go`).

---

## History rows

- Status history and edit history use insert paths with **`ON CONFLICT DO NOTHING`** where applicable; duplicates may be dropped **without** surfacing a conflict to the client.

---

## Android UI behavior

| State | User-visible behavior |
|--------|------------------------|
| Syncing | Modal with status text (`SyncModal`). |
| Error | `SyncState.Error(message)` — error text from HTTP/body. |
| User suspended | Blocking dialog → session cleared → logout flow (`DashboardScreen`). |
| Device revoked | Blocking dialog → session/users cleared → device flow (`DashboardScreen`). |

There is **no dedicated “merge conflict” screen**; resolution is **implicit** via server row replacement on next successful sync.

---

## Operational implications

- Treat **`cloud_id`** as the stable join key; offline-first users should avoid destructive local-only forks of the same logical remito without a single `cloud_id`.
- For **multi-device** teams, assume **last writer to sync** defines server truth; train operators accordingly until richer conflict UX exists.

---

## Future improvements (not implemented)

- Structured **`conflicts`** payload and client resolution UI.
- **`updated_at`-based** conditional writes or revision checks to reject stale pushes.
- Webhook-style notifications when sync repeatedly fails for a tenant.
