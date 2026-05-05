# AGENTS.md

## Git Workflow

- Always use `scripts/committer` to commit changes to git. Do not use `git commit` directly.
- After committing, push changes using `git push`.

## Billing / subscription ops

- Renewals and scheduled downgrades (`pending_plan`) are applied when a **paid renewal** succeeds: webhook credits may extend `subscription_expires_at` and call `ApplyPendingPlanIfAny`, or the optional **`BillingAutomaticRenewalEnabled`** sweep plus **`POST /internal/billing/trigger-renewal`** (protected by **`BillingRenewalSecret`**). Ensure Mercado Pago webhooks reach the API and env flags match how you intend to charge.
