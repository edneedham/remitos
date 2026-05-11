# Database transactions (backend)

Multi-step HTTP flows generally use the connection pool (`*pgxpool.Pool`) with **one statement per call**. A few flows use explicit SQL transactions where partial writes would be user-visible or hard to repair:

- **Mercado Pago webhook** (`internal/handlers/mercadopago_webhook.go`): `Begin` / commit around payment and invoice side effects.
- **Subscription renewal sweep** (`internal/billing/renewal.go`): transactional charge + invoice updates.
- **Plan change** (`internal/handlers/auth_change_plan.go`): nested transactions for downgrade/upgrade paths.

Other flows (signup, device registration, sync apply) rely on **per-statement atomicity** and idempotent-ish operations; if you add new cross-table invariants, consider wrapping the critical section in `pool.Begin` and passing `pgx.Tx` into repositories that accept `DBConn`.
