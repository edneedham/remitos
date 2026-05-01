# Billing: implementation summary

This document summarizes billing-related work in the repo: **contract in USD**, **invoices and charges in ARS**, **MEP-based conversion**, and what remains to ship a full commercial flow.

---

## Legal notice (Spanish, customer-facing)

The product copy follows `billing.LegalNoticeAR(bufferFraction)` (default buffer 7%) and must stay aligned with `website/src/app/lib/billingLegalNotice.ts` for pre-fetch UI.

> «Los precios están denominados en dólares estadounidenses (USD). Los importes en pesos argentinos (ARS) se calcularán a partir del dólar MEP (bolsa) vigente en la fecha de facturación, con un recargo del 7% sobre esa cotización de referencia, y se cobrarán en pesos enteros.»

It appears on: **Facturación** (dashboard), **Activar suscripción**, **pricing** (marketing), and **signup plan selector**. The API `GET /auth/me/plan-pricing` also returns `legal_notice_ar` for the activate flow.

---

## What’s implemented

### Data model
- **`billing_invoices`**: `amount_minor` (ARS centavos), `currency` (e.g. ARS), `status` (`pending` / `paid` / `void`), optional `mp_payment_id`.
- **`companies`**: `subscription_plan`, `subscription_expires_at`, `trial_ends_at`, Mercado Pago `mp_customer_id` / `mp_card_id`, limits, etc.

### Catalog pricing (USD list)
- Backend: **`billing.MonthlyListPriceUSD`** (`internal/billing/pricing.go`) — e.g. PyME USD 29, Empresa USD 59.
- Website: **`PLAN_MONTHLY_LIST_PRICE_USD`** and labels in **`website/src/app/lib/planCatalog.ts`** — must stay aligned with the Go map.

### ARS amount from USD × MEP
- **Live rate**: `GET` **[dolarapi.com](https://dolarapi.com/v1/dolares/bolsa)** — JSON object with **`venta`** as ARS per 1 USD (`internal/billing/mep`). Legacy array-shaped endpoints remain supported if you override **`BILLING_MEP_BOLSA_URL`**.
- **Fallback**: env **`BILLING_USD_ARS_RATE`** if the API is unreachable (`internal/billing/ratesource.go` — `MEPWithFallback`).
- **Optional URL override**: **`BILLING_MEP_BOLSA_URL`**.
- **Formula**: `charged_rate = mep_venta × (1 + fx_buffer)` (default buffer 7%), then `amount_minor = round(usd_list × charged_rate) × 100` (whole pesos, see `InvoiceAmountMinorARS`).

### API endpoints
- **`GET /auth/me/plan-pricing?plan_id=pyme|empresa`** — ARS `amount_minor`, `ars_per_usd` (MEP reference × (1+buffer)), `mep_ars_per_usd`, `fx_buffer_fraction`, `fx_source`, `fx_effective_date`, `legal_notice_ar`.
- **`POST /auth/me/activate-subscription`** — plan `pyme`/`empresa`, saves card (Mercado Pago or mock), sets paid window (`subscription_expires_at` +1 month from activation), persists MP ids. **Corporativo** is not self-serve on this endpoint.
- **`POST /internal/billing/trigger-renewal`** (header **`X-Billing-Secret`**) — invoice → charge hook → extend subscription; **`amount_minor: 0`** uses catalog × MEP (or fallback). Requires **`BILLING_RENEWAL_SECRET`**.

### Mercado Pago (server)
- **`SaveCard`** (new customer + card) and **`AttachCardToCustomer`** (existing customer).
- **`ChargeRenewal`**: stub path for dev; live off-session card charge **not** wired (documented error — needs Subscriptions / Orders or similar).

### Web UX
- **Signup**: still frictionless (no card on default signup form); optional `card_token` on API saves MP ids when configured.
- **Gate**: users without download entitlement are steered to **`/dashboard/activate-subscription`**, except they can open **`/dashboard/billing`** (avoids redirect loops).
- **Activate subscription**: plan choice, Mercado Pago Card Brick amount from **`plan-pricing`**, or mock activation when **`NEXT_PUBLIC_SIGNUP_USE_MOCK_PAYMENT`** + API **`SIGNUP_ALLOW_MOCK_PAYMENT`**.
- **`postWithWebAuth`** for authenticated POSTs.

### Entitlement
- **`CompanyHasAppDownloadAccess`** / **`can_download_app`** drive who can use entitled product features (trial vs paid period).

---

## Configuration (quick reference)

| Variable | Where | Purpose |
|----------|--------|---------|
| `BILLING_MEP_BOLSA_URL` | API | Optional bolsa URL (default dolarapi.com `/v1/dolares/bolsa`) |
| `BILLING_USD_ARS_RATE` | API | Fallback ARS/USD if MEP API fails |
| `BILLING_FX_BUFFER_FRACTION` | API | Surcharge on MEP reference before USD→ARS (default `0.07`) |
| `BILLING_RENEWAL_SECRET` | API | Enables internal renewal route |
| `BILLING_STUB_AUTO_CHARGE` | API | Dev: skip real MP on renewal |
| `MERCADOPAGO_ACCESS_TOKEN` | API | Server MP calls |
| `NEXT_PUBLIC_MERCADOPAGO_PUBLIC_KEY` | Website | Card Brick |
| `NEXT_PUBLIC_SIGNUP_USE_MOCK_PAYMENT` | Website | Dev UI without Brick |
| `SIGNUP_ALLOW_MOCK_PAYMENT` | API | Dev: mock card paths |

---

## What’s left (recommended order)

1. **Legal / tax** — Align copy and implementation with counsel: IVA, factura tipo, Régimen de información, and whether “MEP” must cite a specific official source beyond the bolsa feed you use (e.g. dolarapi).
2. **Persist FX on each invoice** — Store `ars_per_usd`, `fx_source`, `fx_effective_date` (and optionally `usd_list_amount`) on `billing_invoices` so PDFs and support match what was charged.
3. **Real recurring charge** — Wire Mercado Pago **Subscriptions / Preapproval** or **Orders** recurring; webhooks to mark invoices paid and extend `subscription_expires_at`; retries and dunning.
4. **Activation = first invoice** — Optionally create a **paid** (or **pending** then settled) invoice row on activation with the same ARS amount as the Brick.
5. **Corporativo** — Self-serve or sales-only; custom USD price and limits if not catalog.
6. **Rate caching** — Short TTL cache for MEP fetch to reduce API load and smooth spikes (still “billing date” semantics if you snapshot at invoice time).
7. **Admin / ops** — Manual override of rate, invoice void/credit notes, export for accountant.
8. **Android / sync** — Confirm any server-side limits beyond web entitlement (e.g. document caps) match billing state.
9. **Tests** — Contract tests for pricing + integration test against a mocked bolsa HTTP response in CI.

---

## Files to know

| Area | Path |
|------|------|
| USD list + ARS math | `backend/internal/billing/pricing.go` |
| MEP fetch | `backend/internal/billing/mep/bolsa.go` |
| MEP + fallback | `backend/internal/billing/ratesource.go` |
| Renewal pipeline | `backend/internal/billing/renewal.go` |
| Plan pricing API | `backend/internal/handlers/auth_plan_pricing.go` |
| Activation API | `backend/internal/handlers/auth_activate_subscription.go` |
| Internal renewal | `backend/internal/handlers/billing_renewal.go` |
| Legal string (TS) | `website/src/app/lib/billingLegalNotice.ts` |
| Activate UI | `website/src/app/dashboard/activate-subscription/` |
| Gate | `website/src/app/dashboard/ActivateSubscriptionGate.tsx` |

---

*Last updated to reflect the billing stack as implemented in-repo; adjust this doc when shipping new billing features.*
