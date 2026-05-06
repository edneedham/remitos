# Performance audit report

**Branch:** `performance`  
**Date:** 2026-05-06  
**Scope:** Backend (Go), PostgreSQL usage, marketing website (Next.js), Android app sync path — focused on latency, throughput, and scalability risks.

---

## Executive summary

The highest-impact issues are **database round-trip amplification** on the sync API: upserts run **one statement per row** (and often **two** lookups per inbound note), and fetching outbound lists for sync runs **one extra query per list** (N+1). These paths dominate cost as tenants grow document volume. Secondary concerns include **no explicit connection pool tuning**, **per-request device validation** when `X-Device-ID` is present, and **large JSON sync payloads** without pagination or compression.

The billing renewal job and Mercado Pago HTTP clients use reasonable timeouts; the Next.js app uses some code-splitting (`dynamic`) for heavy payment UI. No automated load tests or profiling hooks were found in-repo.

---

## Severity legend

| Level | Meaning |
| ----- | ------- |
| **P0** | Likely to dominate latency or cost at moderate scale |
| **P1** | Important under growth or bursty traffic |
| **P2** | Optimization or hygiene; measure before heavy investment |

---

## Backend API (Go)

### P0 — Sync upserts: per-row queries

`UpsertInboundNotes`, `UpsertOutboundLists`, `UpsertStatusHistory`, and `UpsertEditHistory` iterate collections and issue **separate** `QueryRow` / `Exec` calls per item.

**Why it matters:** A sync with hundreds of inbound notes or lines generates hundreds to thousands of round trips. Pool contention and total request duration grow linearly with batch size.

**Direction:** Batch with `pgx.Batch`, multi-row `INSERT ... ON CONFLICT`, `COPY` into a staging table then merge, or chunk large payloads server-side with explicit limits.

**Reference:** `backend/internal/repository/sync.go` (`UpsertInboundNotes`, `UpsertOutboundLists`, etc.).

### P0 — `GetOutboundListsSince`: N+1 queries for lines

For each outbound list returned, the code loads lines in a separate query (`getLinesForList` per list cloud ID).

**Why it matters:** If `last_sync` is far in the past or many lists changed, response time scales with **number of lists**, not a single scan.

**Direction:** Single query joining `outbound_lists` and `outbound_lines`, then group lines by list `cloud_id` in memory (or use JSON aggregation in SQL if preferred).

**Reference:** `GetOutboundListsSince` and `getLinesForList` in `backend/internal/repository/sync.go`.

### P1 — Sync handler logging at info on every request

Each sync logs user, company, and counts at **info** level.

**Why it matters:** High-volume sync generates expensive log I/O and storage in production; cardinality fields (user_id) can stress log pipelines.

**Direction:** Demote to debug with sampling, or log aggregates periodically.

**Reference:** `backend/internal/handlers/sync.go` (`Sync`).

### P1 — Auth middleware: DB hit when device header is set

Authenticated requests with `X-Device-ID` trigger `DeviceRepo.GetByID` for validation.

**Why it matters:** Every mobile API call pays an extra PostgreSQL round trip unless cached.

**Direction:** Short-lived cache keyed by device ID + token fingerprint, or embed device scope in JWT when safe to do so (with clear revocation semantics).

**Reference:** `backend/internal/middleware/auth.go`.

### P2 — Database pool configuration

`db.Connect` builds a pool with `pgxpool.NewWithConfig` but **does not** set `MaxConns`, `MinConns`, `MaxConnLifetime`, or statement timeouts in application code.

**Why it matters:** Defaults may be OK on Cloud Run / small instances; under load, tuning avoids exhaustion or idle connection churn.

**Direction:** Align pool size with instance CPU/RAM and Postgres `max_connections`; consider `statement_timeout` at the pool or role level for accidental long queries.

**Reference:** `backend/db/connection.go`.

### P2 — Jobs: sequential renewal sweep

`runBillingRenewalSweep` processes up to 200 company IDs **sequentially**.

**Why it matters:** Tick duration grows with candidates × Mercado Pago latency; slow payments delay the tail.

**Direction:** Bounded worker pool with rate limiting to respect MP API limits.

**Reference:** `backend/internal/jobs/billing_renewal_sweep.go`.

---

## PostgreSQL

### Indexes

Existing migrations define single-column indexes on `company_id`, `cloud_id`, and `updated_at` for sync tables (`022_sync_tables.up.sql`). Queries such as `WHERE company_id = $1 AND updated_at > $2` may benefit from a **composite** index `(company_id, updated_at)` on `inbound_notes` and `outbound_lists` so the planner can satisfy filters and ordering from one structure.

**Recommendation:** Validate with `EXPLAIN (ANALYZE, BUFFERS)` on production-like data; add composite indexes if sequential scans or large bitmap merges appear.

### P1 — Monthly usage query on every limited sync

When `DocumentsMonthlyLimit` is set and the request includes inbound notes, the handler calls `InboundNotesMTDCumulativeSeries`, which aggregates by day for the month.

**Why it matters:** Extra aggregation work on hot path; acceptable at small scale but redundant if only **projected new count** vs limit is needed.

**Direction:** Cheaper limit checks (e.g., month-to-date count + delta only) if business rules allow.

**Reference:** `backend/internal/handlers/sync.go`, `InboundNotesMTDCumulativeSeries` in `backend/internal/repository/sync.go`.

---

## Website (`website/` — Next.js)

### P2 — API proxy via rewrites

`next.config.ts` rewrites `/api/v1/*` to the backend. That keeps the browser same-origin but does **not** inherently add caching.

**Direction:** For read-heavy public endpoints, consider CDN caching headers on the API or static generation where applicable.

### P2 — Bundle / runtime

Heavy flows (e.g. card UI) use `next/dynamic` in some panel pages — good for initial load. Broader use of dynamic imports for rarely visited routes can further reduce main-bundle cost if profiling shows regressions.

---

## Android client

### P1 — Building sync payload: per-list line fetch

`SyncService.performFullSync` loads unsynced lists and, for each list, loads lines (pattern similar to server-side N+1 when assembling outbound DTOs).

**Why it matters:** Large unsynced batches increase DB time on device before the HTTP call.

**Direction:** Single DAO query returning lists with lines (JOIN), or Room `@Relation` / raw query — mirror the server-side “fetch in one go” approach.

**Reference:** `android/.../SyncService.kt` (loop over `unsyncedLists` with `getLinesForListSync`).

### P2 — Coroutine scope in `SyncManager`

`syncIfNeeded` uses `CoroutineScope(Dispatchers.IO).launch` without structured concurrency tied to lifecycle/worker; worth auditing for duplicate concurrent syncs under rapid triggers (guarded partly by `_isSyncing`).

---

## External HTTP / integrations

- **Mercado Pago client:** `http.Client` timeout **30s** — reasonable for API calls (`backend/internal/payments/mercadopago/client.go`).
- **Billing FX:** separate client with **20s** timeout (`backend/main.go`). Good practice.

---

## Observability & validation gaps

| Gap | Why it matters |
| --- | --- |
| No documented load test or k6/Locust scripts in-repo | Regressions on sync batch size go unnoticed |
| Request logging includes duration (`middleware/logger.go`) | Good baseline; pair with **slow-query logging** in Postgres or app-level thresholds for sync routes |

**Recommendation:** Add synthetic sync benchmarks (small/medium/large payloads), enable Postgres `log_min_duration_statement` in staging, and track p95 sync latency + pool stats.

---

## Prioritized recommendations

1. **Batch or pipeline sync writes** (inbound/outbound/history) to collapse round trips — **P0**.
2. **Rewrite `GetOutboundListsSince`** to avoid per-list line queries — **P0**.
3. **Composite indexes** `(company_id, updated_at)` if `EXPLAIN` warrants — **P1**.
4. **Reduce sync info logging** or sample — **P1**.
5. **Android:** fetch lines for outbound sync in one query — **P1**.
6. **Tune pgxpool** and consider **statement timeouts** — **P2**.
7. **Parallelize renewal sweep** with a worker cap — **P2**.

---

## What looks healthy

- Mercado Pago and FX HTTP clients use explicit timeouts.
- Sync-related tables already have sensible indexes from migrations (starting point for composite tuning).
- Next.js payment flows use dynamic import in several panel pages.
- `CountInboundNotesByCloudIDs` uses `ANY($2::text[])` — single query for existence counts.

---

*This audit is based on static code review; runtime measurements should confirm priorities before large refactors.*
