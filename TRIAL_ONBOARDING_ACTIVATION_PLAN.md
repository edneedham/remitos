# Trial Onboarding Activation Plan

## Goal

Reduce time-to-first-value after signup by guiding users from trial activation to their first scanned document.

Primary success metric: users complete activation within 10 minutes of `trial_started`. **Authoritative completion** is `GET /auth/me/entitlement` → `first_scan_completed` (true when the company has at least one **synced** inbound note on the server) and optional `first_scan_completed_at`.

## Scope and Principles

- Focus on one user outcome: first successful scan.
- Keep onboarding linear and explicit.
- Use web + app touchpoints with consistent language.
- Instrument every step so we can optimize by drop-off point.

## Phase 1: Dedicated Success Screen (Start Here)

### Objective

Replace the generic post-signup redirect with a focused success screen that directs users to download the app, log in, and perform the first scan.

### Entry Condition

User successfully completes signup and trial activation (`trial_started`).

### UX Requirements

- Show a dedicated success page immediately after trial starts.
- Display a 3-step sequence:
  1. Download the app
  2. Log in with your credentials
  3. Scan your first document
- Keep one clear primary CTA: open **Android APK** download (`/panel/aplicacion`).
- **Shipping product is Android-only** for depot scanning (no iOS App Store / desktop client). Marketing must not imply unavailable platforms.
- Show a **QR code** on desktop to open the same download page on the phone.
- Secondary: **Copy download link** (clipboard) as the practical “send to phone” channel until SMS/WhatsApp exists.
- Keep copy outcome-based (Spanish UI live on `/prueba-iniciada`).

### Functional Requirements

- Route users to success screen immediately after successful trial creation.
- Persist a boolean or timestamp marker that success screen was displayed.
- If user revisits before first scan, allow redisplay via dashboard checklist item.
- Deep links should prefill account email in login flow where technically possible.
- Fallback: **Panel web** (`/panel`) for account management; scanning remains on the Android app once installed.

### Event Tracking (Required)

Track at minimum:

- `trial_started`
- `trial_success_screen_viewed`
- `trial_download_clicked` (with platform)
- `trial_qr_viewed`
- `trial_copy_link_clicked` (channel `clipboard`; replaces generic “send link” until SMS/WhatsApp)
- `trial_fallback_web_upload_clicked`

### Acceptance Criteria

- 100% of newly activated trial users land on success screen.
- Success screen shows Android + QR + copy-link; no non-shipping platform CTAs.
- Event tracking is emitted for all primary and secondary actions.
- Users can continue to product without dead-end states.

## Phase 2: Dashboard Onboarding Checklist

### Objective

Keep activation momentum after first session with visible progress.

### Requirements

- Persistent checklist on dashboard until first scan is complete:
  - Install app
  - Log in
  - First scan
- Mark steps using **server state**: `device_count`, `GET /auth/me/entitlement` (`first_scan_completed`), plus local flag for “visited download page”.
- Include "Resume setup" action that links back to success flow.
- Dismiss checklist automatically on `first_scan_completed`.

## Phase 3: Lifecycle Nudges

### Objective

Recover users who started trial but did not complete first scan.

### Suggested Cadence

- +10 minutes: setup reminder with download links.
- +24 hours: short walkthrough/GIF of first scan flow.
- +3 days: support-oriented message ("Need help getting your first scan?").

### Trigger Logic

Send only if `first_scan_completed` is still false at trigger time.

## Phase 4: In-App First-Run Guidance

### Objective

Reduce friction between first login and first scan.

### Requirements

- On first app login, open scan entry point by default.
- Show lightweight contextual tips for first scan.
- Show success confirmation after upload/scan completion.

## Analytics and Reporting

### Core Funnel

1. `trial_started`
2. `trial_download_clicked`
3. `app_first_open` (or best available proxy)
4. `app_first_login`
5. `first_scan_completed`

### KPIs

- Time from `trial_started` to `first_scan_completed`.
- Step-to-step conversion rates.
- Drop-off by context (**Android app** vs **web panel only**).

## Rollout Plan

1. Implement Phase 1 success screen + instrumentation.
2. Validate event quality in staging and production.
3. Launch to 100% of new trials.
4. Add Phase 2 checklist.
5. Add lifecycle nudges and in-app first-run improvements.

## Resolved / open

- **Source of truth for `first_scan_completed`:** API (`first_scan_completed` / `first_scan_completed_at` on entitlement); web checklist and analytics consume it.
- **Legacy URL:** `/trial-started` redirects to `/prueba-iniciada`; onboarding emails use `/prueba-iniciada`, `/panel/aplicacion`, `/panel`.

### Open

- SMS/WhatsApp “send link” vs clipboard-only.
- Deep link prefilled email on app login.
- Whether **web-only** upload counts as activation if added later.
