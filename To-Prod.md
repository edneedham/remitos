# To production: Neon, Vercel, Google Cloud

This runbook is for taking the **Remitos** stack from local development to production. It assumes:

- **Neon** — managed PostgreSQL
- **Vercel** — Next.js site (`website/`)
- **Google Cloud** — API backend (`backend/`), typically **Cloud Run** (recommended) or GCE

Do **not** commit real secrets. Use each platform’s **secret manager** or **environment variable** UI.

---

## 1. What you are deploying

| Piece | Repository path | Where it runs |
|--------|-----------------|---------------|
| HTTP API (Go) | `backend/` | Google Cloud (e.g. Cloud Run) |
| Web app (Next.js) | `website/` | Vercel |
| Database | `backend/db/migrations/` | Neon |

The browser talks to **Vercel**; the site calls the **API URL** you set in `NEXT_PUBLIC_API_URL`. The API talks to **Neon** and to **Mercado Pago** (and optionally Resend, GCS).

---

## 2. Neon (PostgreSQL)

### Create

1. In Neon, create a **project** and a **database** (name is up to you, e.g. `remitos`).
2. Copy the **connection string** (URI). It includes host, user, password, and database name.

### Credentials you need

- `DB_HOST` — hostname from the URI (not the full URL)
- `DB_PORT` — usually `5432`
- `DB_USER` / `DB_PASSWORD` — from the URI, or a dedicated Neon role
- `DB_NAME` — database name
- `DB_SSLMODE` — use **`require`** in production (Neon expects SSL)

You can also use a single `DATABASE_URL` if you later add a small wrapper; this repo’s `config` today uses discrete `DB_*` fields — set them to match Neon’s connection details.

### After the database exists

1. Point your **local** or **CI** environment at Neon (or a branch database) and run migrations (see **`OPERATIONS.md`** for how migrations run on deploy and rollback stance).
2. **Never** run `SEED_LOCAL_DEV_USERS=true` in production.
3. Store the connection values in **GCP Secret Manager** (or Cloud Run env from secrets) and/or Neon’s **Vercel integration** only if you use a DB client from Vercel (this app’s API uses the DB from **GCP**, not Vercel).

---

## 3. Google Cloud (API)

### Recommended: Cloud Run

1. **Artifact Registry** — store Docker images built from `backend/` (Dockerfile at repo root or under `backend/` if you add one).
2. **Cloud Run service** — deploy the image, set **port** to what the server listens on (see `PORT` in `backend/main.go` / Cloud Run defaults, often `8080`).
3. **Secrets / env** — inject all backend variables from **Secret Manager** or Cloud Run “Secrets as env vars”.

### Networking

- Give the service a **stable HTTPS URL** (e.g. `https://api.yourdomain.com`) via Cloud Run domain mapping or a **HTTPS load balancer** + serverless NEG.
- That URL is what you will put in **`NEXT_PUBLIC_API_URL`** on Vercel (no trailing slash).

### Credentials & env vars (API)

Set these in Cloud Run (values from your accounts — not committed):

| Variable | Purpose |
|----------|---------|
| `MERCADOPAGO_ACCESS_TOKEN` | Production access token (MP dashboard, **production** credentials) |
| `JWT_SECRET` | Long random string; signing web sessions |
| `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`, `DB_NAME`, `DB_SSLMODE=require` | Neon |
| `CORS_ALLOWED_ORIGINS` | Comma-separated origins, e.g. `https://www.yourdomain.com,https://yourdomain.com` |
| `PUBLIC_SITE_URL` | Canonical marketing/site URL **no trailing slash**, e.g. `https://www.yourdomain.com` — used in emails |
| `BILLING_USD_ARS_RATE` | Fallback FX if MEP API fails (**recommended** in prod) |
| `BILLING_FX_BUFFER_FRACTION` | Default `0.07` unless you change pricing policy |
| `SIGNUP_ALLOW_MOCK_PAYMENT` | **`false`** in production |
| `BILLING_STUB_AUTO_CHARGE` | **`false`** in production |
| `BILLING_RENEWAL_SECRET` | Enables `POST /internal/billing/trigger-renewal` (cron or ops); merchant-owned renewals with MEP-priced charges |
| `BILLING_AUTOMATIC_RENEWAL_ENABLED` | **`false`** until renewals verified; optional background sweep for expired `subscription_expires_at` |
| `BILLING_RENEWAL_POLL_MINUTES` | Sweep interval when automatic renewal is enabled (default 60) |
| `EMAIL_ENABLED`, `RESEND_API_KEY`, `EMAIL_FROM`, `EMAIL_REPLY_TO` | If using Resend |
| `GCS_RELEASES_BUCKET`, `ANDROID_RELEASE_OBJECT`, GCS credentials | If APK downloads via GCS (service account with sign-URL permission) |

Mercado Pago **webhook** URL (configure in MP dashboard):

- `POST https://<your-api-host>/webhooks/mercadopago`
- Enable **`payment`** for payment notifications on charges created by the API.

Also configure **webhook signature secret** in MP and plan to validate `x-signature` in the API when you harden (not yet required for first deploy, but do not skip long-term).

---

## 4. Vercel (website)

### Project

- Import the repo (or connect Git), set **root directory** to **`website`** if the monorepo is not only the Next app.
- **Framework**: Next.js.

### Environment variables (production)

| Variable | Purpose |
|----------|---------|
| `NEXT_PUBLIC_API_URL` | **HTTPS** URL of your Cloud Run API, no trailing slash |
| `NEXT_PUBLIC_SITE_URL` | Public site URL (no trailing slash), same idea as `PUBLIC_SITE_URL` on the API |
| `NEXT_PUBLIC_MERCADOPAGO_PUBLIC_KEY` | **Production** public key from Mercado Pago (pairs with server token) |
| `NEXT_PUBLIC_SIGNUP_USE_MOCK_PAYMENT` | Omit or **`false`** in production |
| `FORMSPREE_FORM_ID` | If you use the contact form |
| `API_URL` | Optional; used in `next.config.ts` for rewrites — align with your API if needed |

Redeploy after changing env vars.

### CORS

The API’s `CORS_ALLOWED_ORIGINS` must include your **exact** Vercel production origin(s), e.g. `https://your-app.vercel.app` and your custom domain.

---

## 5. Mercado Pago (single checklist)

1. **Production** application + **production** access token (server) and **production** public key (browser).
2. **Webhook** URL = `https://<api>/webhooks/mercadopago`, topic **`payment`**.
3. **Test** renewals and activation with **`BILLING_STUB_AUTO_CHARGE`** off only after sandbox validation; MP approval rules vary by card and region.
4. **Test** in MP **sandbox** first if available for your account region.

---

## 6. Suggested order of operations

1. Create **Neon** DB → run **migrations**.
2. Deploy **API** to **Cloud Run** with env + secrets → confirm **`GET /health`** on the public URL.
3. Configure **Mercado Pago** webhook pointing at the API.
4. Deploy **Vercel** with `NEXT_PUBLIC_*` → smoke-test login/signup against prod API.
5. End-to-end: signup → trial → **Activar suscripción** with real MP **production** keys in a **low-risk** window.

---

## 7. Files to keep in sync locally vs prod

- `backend/.env.example` — reference for API variables (copy pattern, not values).
- `website/.env.example` — reference for Vercel.
- `billing-doc.md` — billing behavior and MP subscription notes.

---

## 8. Security reminders

- Rotate **JWT_SECRET**, **Mercado Pago** tokens, and **DB password** if ever leaked.
- Restrict Neon IP allowlists if you use them; Cloud Run egress to Neon must be allowed.
- Use **HTTPS** everywhere; never send tokens over HTTP in production.

---

## 9. Post-deploy smoke & secret rotation

Use this after **every production API deploy** (new Cloud Run revision) and whenever you **rotate secrets**. It satisfies the operational checklist in `FAILURE-CHECK.md` (“webhook and secret rotation tested after each API/deploy change”).

### After each API deploy

1. **`GET /health`** on the public API URL → **200** and body `ok` (process up).
2. **`GET /health/ready`** → **200** `ok` if Postgres is reachable; **503** if not (use this for production uptime monitors—see **`OPERATIONS.md`**).
3. **Mercado Pago webhook reachability** — **`GET`** `https://<your-api-host>/webhooks/mercadopago` → **200** (Mercado Pago uses this when validating the webhook URL).
4. **Optional:** In the Mercado Pago dashboard, send a **test notification** for topic **`payment`** and confirm the API returns **200** in logs (payload may be ignored; avoid **5xx**).
5. **Auth smoke:** Log in on the production site once (`NEXT_PUBLIC_API_URL` → same API). Confirms **`JWT_SECRET`** and DB connectivity end-to-end.

**Note:** Long-term, validate Mercado Pago **`x-signature`** using the webhook signing secret configured in MP (see MP docs). That verification is not wired in this codebase yet; until then, smoke testing is **reachability + delivery logs**, not HMAC verification.

### When rotating secrets (each rotation event)

| Secret | What to verify |
|--------|----------------|
| **`MERCADOPAGO_ACCESS_TOKEN`** | Card attach/save, renewal charges, webhook processing; MP dashboard shows successful webhook deliveries if applicable. |
| **`JWT_SECRET`** | Existing JWTs invalidate — users must log in again. Smoke: login, panel, `/auth/me`-equivalent flows. |
| **DB password** (`DB_*`) | API starts; **`/health/ready`** returns **200**; no migration connection errors in logs. |
| **`BILLING_RENEWAL_SECRET`** | `POST /internal/billing/trigger-renewal` with header **`X-Billing-Secret`** succeeds only with the **new** secret; the old secret must fail with **401** (or equivalent). |

Keep rotation steps in runbooks or tickets so each event leaves an audit trail.

### Optional automation

- **CI/post-deploy:** After deploy, `curl` **`/health`**, **`/health/ready`**, and **`GET /webhooks/mercadopago`** (store production base URL in CI vars; no secrets in logs).
- **Monitoring:** Prefer synthetic checks on **`/health/ready`** (includes DB); alert on non-200.

---

*Last updated for deployments targeting Neon + Vercel + Google Cloud Run. Adjust service names if you use GKE or Compute Engine instead.*
