# Feature flags × surfaces (release coordination)

Use this matrix before tagging a release so **Android**, **Go API**, and **website** stay aligned. Symptom when misaligned: **403**, uploads stuck, OCR falling back unexpectedly, or panel pointing at the wrong API.

**Primary references:** `android/app/src/main/java/com/remitos/app/data/FeatureFlags.kt`, `android/app/src/main/java/com/remitos/app/RemitosApplication.kt`, `android/app/build.gradle.kts` (`BACKEND_BASE_URL`), `website/.env.example` (`NEXT_PUBLIC_API_URL`), `backend/main.go` / handlers.

---

## Matrix

| User-facing capability | Android (`FeatureFlags`) | Wired at startup | Go API | Website |
|------------------------|--------------------------|------------------|--------|---------|
| Call backend at all | `backendBaseUrl` from `BuildConfig.BACKEND_BASE_URL` | `RemitosApplication.onCreate` → `configureBackendMode(baseUrl)` | Server deployed and reachable | `NEXT_PUBLIC_API_URL` must target the **same** API host as the app build under test |
| Low-confidence OCR → server processing | `enableBackendOcr` | Set **true** together with backend mode (same hook) | `POST` scan/OCR path; Cloud Vision / credentials configured where required | — |
| Upload scan images | `enableImageUpload` | Set **true** with backend mode | Image handler + **GCS** (if missing, uploads fail at runtime) | — |
| Cloud sync (push/pull) | `enableCloudSync` | Set **true** with backend mode | Auth JWT, sync handler, billing/limits | Panel uses API for entitlement, billing, devices |

**Note:** Production app startup uses **`configureBackendMode(url)`**, which turns **on** `enableBackendOcr`, `enableImageUpload`, and `enableCloudSync` whenever a base URL is present. Granular **`configure(...)`** exists for tests or special builds; document any build flavor that diverges.

**Background uploads:** `FeatureFlags.syncIntervalMinutes` drives the periodic image-upload worker, clamped to **at least 15 minutes** (WorkManager minimum). User context init on cold start runs on **`RemitosApplication.applicationScope`** (not blocking `onCreate`).

| Build / env surface | What to verify |
|---------------------|----------------|
| Android **debug** | Default `BACKEND_BASE_URL` is emulator loopback (`10.0.2.2:8080`). Physical device → LAN IP or `adb reverse`. |
| Android **release** | `BACKEND_BASE_URL` in `build.gradle.kts` points to intended API (e.g. Cloud Run URL). |
| API | Migrations applied; secrets match clients (JWT issuer/audience as applicable); optional GCS/Vision only if you rely on those paths. |
| Website (Vercel/local) | `NEXT_PUBLIC_API_URL` matches the API you mean customers and QA to hit. |

---

## Release smoke checklist (production-like app ↔ real API)

Run **before** wide rollout or store submission. Use a **release** or **staging-equivalent** APK/IPA build where `BACKEND_BASE_URL` matches the API under test (often staging).

1. **Install** the production-like build on a device or emulator with network access to that API.
2. **Login** with a known good operator (trial or test company).
3. **Cloud sync:** open dashboard with sync enabled; confirm sync completes or surfaces a **clear** error (not silent failure). Optional: airplane mode off/on and confirm retry behavior.
4. **Inbound path:** scan or save one ingreso; confirm it appears locally and (if entitled) sync-related state updates.
5. **Website:** open panel login against the **same** API URL as configured in `NEXT_PUBLIC_API_URL`; confirm dashboard loads entitlement/billing without “missing API URL” errors.

Record failures in the issue tracker; do not ship until login + sync + one write path work end-to-end.

---

## Changelog discipline

When a release changes any of the following, add a bullet under that version in **`CHANGELOG.md`**:

- `FeatureFlags` defaults or `configure*` behavior in `RemitosApplication`
- `BACKEND_BASE_URL` per build type (debug/release) or new build flavors
- Website requirement or semantics for `NEXT_PUBLIC_API_URL`
- New optional API dependencies for OCR/images (e.g. GCS bucket, Vision)

---

## Revision

Introduced for **FAILURE-CHECK.md §3**. Update when adding new flags or env-driven behavior.
