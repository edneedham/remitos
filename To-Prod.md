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

1. Point your **local** or **CI** environment at Neon (or a branch database) and run migrations (see `backend/db` and your usual migrate command).
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
| `MERCADOPAGO_PREAPPROVAL_PLAN_PYME` | Plan id from MP Suscripciones / preapproval plan |
| `MERCADOPAGO_PREAPPROVAL_PLAN_EMPRESA` | Same for Empresa plan |
| `MERCADOPAGO_SUBSCRIPTION_BACK_URL` | Optional; defaults with `PUBLIC_SITE_URL` (see below) |
| `JWT_SECRET` | Long random string; signing web sessions |
| `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`, `DB_NAME`, `DB_SSLMODE=require` | Neon |
| `CORS_ALLOWED_ORIGINS` | Comma-separated origins, e.g. `https://www.yourdomain.com,https://yourdomain.com` |
| `PUBLIC_SITE_URL` | Canonical marketing/site URL **no trailing slash**, e.g. `https://www.yourdomain.com` — used in emails and Mercado Pago `back_url` |
| `BILLING_USD_ARS_RATE` | Fallback FX if MEP API fails (**recommended** in prod) |
| `BILLING_FX_BUFFER_FRACTION` | Default `0.07` unless you change pricing policy |
| `SIGNUP_ALLOW_MOCK_PAYMENT` | **`false`** in production |
| `BILLING_STUB_AUTO_CHARGE` | **`false`** in production |
| `BILLING_RENEWAL_SECRET` | Only if you use internal `POST /internal/billing/trigger-renewal`; omit or leave empty if **only** MP webhooks renew subscriptions |
| `EMAIL_ENABLED`, `RESEND_API_KEY`, `EMAIL_FROM`, `EMAIL_REPLY_TO` | If using Resend |
| `GCS_RELEASES_BUCKET`, `ANDROID_RELEASE_OBJECT`, GCS credentials | If APK downloads via GCS (service account with sign-URL permission) |

Mercado Pago **webhook** URL (configure in MP dashboard):

- `POST https://<your-api-host>/webhooks/mercadopago`
- Enable **`payment`** (and any subscription topics MP lists for your integration).

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
2. Create **preapproval plans** (PyME / Empresa) and copy ids into `MERCADOPAGO_PREAPPROVAL_PLAN_*` on the API.
3. **Webhook** URL = `https://<api>/webhooks/mercadopago`, topic **`payment`** (plus whatever MP requires for Suscripciones).
4. **Test** in MP **sandbox** first if available for your account region.
5. Ensure `PUBLIC_SITE_URL` / `MERCADOPAGO_SUBSCRIPTION_BACK_URL` match a **real** route on the Vercel site (e.g. `/dashboard/payment-success`).

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

*Last updated for deployments targeting Neon + Vercel + Google Cloud Run. Adjust service names if you use GKE or Compute Engine instead.*
