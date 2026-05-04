package integration

import (
	"context"
	"fmt"
	"testing"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"server/internal/repository"
)

// SeedHash is a bcrypt hash for a dummy password (same as local_dev_users seed).
const SeedHash = "$2a$10$KB81/FzuiHMDYmEsqqHvUOSfmx3aRi4xr6FGtQKOxotMJpi.TszMq"

// CompanyOwnerAndPaidInvoice inserts a minimal tenant with a company_owner and one paid billing_invoices row.
// Truncates tenant data first. Returns company id, invoice id, and owner email.
func CompanyOwnerAndPaidInvoice(ctx context.Context, t *testing.T, pool *pgxpool.Pool) (companyID, invoiceID uuid.UUID, ownerEmail string) {
	t.Helper()
	TruncateTenantData(t, pool)

	companyID = uuid.New()
	whID := uuid.New()
	userID := uuid.New()
	ownerEmail = fmt.Sprintf("int-owner-%s@test.local", companyID.String()[:8])
	code := fmt.Sprintf("I%s", companyID.String()[:6])

	var roleID uuid.UUID
	err := pool.QueryRow(ctx, `SELECT id FROM roles WHERE name = 'company_owner' LIMIT 1`).Scan(&roleID)
	if err != nil {
		t.Fatalf("roles.company_owner: %v", err)
	}

	_, err = pool.Exec(ctx, `
		INSERT INTO companies (
			id, code, name, created_at, updated_at,
			status, is_verified, subscription_plan,
			max_warehouses, max_users, documents_monthly_limit,
			mp_customer_id, mp_card_id
		) VALUES ($1, $2, $3, NOW(), NOW(), 'active', true, 'pyme', 2, 5, 1000, 'int_cust', 'int_card')
	`, companyID, code, "Integration Co")
	if err != nil {
		t.Fatalf("insert company: %v", err)
	}

	_, err = pool.Exec(ctx, `
		INSERT INTO warehouses (id, company_id, name, address, created_at, updated_at)
		VALUES ($1, $2, 'Main', '', NOW(), NOW())
	`, whID, companyID)
	if err != nil {
		t.Fatalf("insert warehouse: %v", err)
	}

	_, err = pool.Exec(ctx, `
		INSERT INTO users (
			id, company_id, warehouse_id, email, username, password_hash,
			role, role_id, status, is_verified, created_at, updated_at
		) VALUES ($1, $2, $3, $4, 'owner', $5, 'company_owner', $6, 'active', true, NOW(), NOW())
	`, userID, companyID, whID, ownerEmail, SeedHash, roleID)
	if err != nil {
		t.Fatalf("insert user: %v", err)
	}

	invRepo := repository.NewInvoiceRepository(pool)
	mpPayment := fmt.Sprintf("mp_int_%s", companyID.String())
	invoiceID, err = invRepo.InsertPaidInvoiceTx(ctx, pool, companyID, 1320000, "ARS", "Suscripción (test)", mpPayment)
	if err != nil {
		t.Fatalf("insert paid invoice: %v", err)
	}

	return companyID, invoiceID, ownerEmail
}

// PendingRenewalInvoice inserts a tenant with a pending billing invoice (failed renewal scenario).
func PendingRenewalInvoice(ctx context.Context, t *testing.T, pool *pgxpool.Pool) (companyID uuid.UUID, invoiceID uuid.UUID, ownerEmail string) {
	t.Helper()
	TruncateTenantData(t, pool)

	companyID = uuid.New()
	var invID uuid.UUID
	whID := uuid.New()
	userID := uuid.New()
	ownerEmail = fmt.Sprintf("dun-%s@test.local", companyID.String()[:8])
	code := fmt.Sprintf("D%s", companyID.String()[:6])

	var roleID uuid.UUID
	err := pool.QueryRow(ctx, `SELECT id FROM roles WHERE name = 'company_owner' LIMIT 1`).Scan(&roleID)
	if err != nil {
		t.Fatalf("roles.company_owner: %v", err)
	}

	_, err = pool.Exec(ctx, `
		INSERT INTO companies (
			id, code, name, created_at, updated_at,
			status, is_verified, subscription_plan,
			max_warehouses, max_users, documents_monthly_limit,
			mp_customer_id, mp_card_id
		) VALUES ($1, $2, $3, NOW(), NOW(), 'active', true, 'pyme', 2, 5, 1000, 'int_cust', 'int_card')
	`, companyID, code, "Dunning Co")
	if err != nil {
		t.Fatalf("insert company: %v", err)
	}

	_, err = pool.Exec(ctx, `
		INSERT INTO warehouses (id, company_id, name, address, created_at, updated_at)
		VALUES ($1, $2, 'Main', '', NOW(), NOW())
	`, whID, companyID)
	if err != nil {
		t.Fatalf("insert warehouse: %v", err)
	}

	_, err = pool.Exec(ctx, `
		INSERT INTO users (
			id, company_id, warehouse_id, email, username, password_hash,
			role, role_id, status, is_verified, created_at, updated_at
		) VALUES ($1, $2, $3, $4, 'owner', $5, 'company_owner', $6, 'active', true, NOW(), NOW())
	`, userID, companyID, whID, ownerEmail, SeedHash, roleID)
	if err != nil {
		t.Fatalf("insert user: %v", err)
	}

	invRepo := repository.NewInvoiceRepository(pool)
	tx, err := pool.Begin(ctx)
	if err != nil {
		t.Fatalf("begin: %v", err)
	}
	defer func() { _ = tx.Rollback(ctx) }()

	invID, err = invRepo.InsertPending(ctx, tx, companyID, 1500000, "ARS", "Suscripción mensual")
	if err != nil {
		t.Fatalf("insert pending: %v", err)
	}
	if err := tx.Commit(ctx); err != nil {
		t.Fatalf("commit: %v", err)
	}

	return companyID, invID, ownerEmail
}

// CompanyAndOwner inserts company + warehouse + company_owner (no billing rows).
func CompanyAndOwner(ctx context.Context, t *testing.T, pool *pgxpool.Pool) (companyID uuid.UUID, ownerEmail string) {
	t.Helper()
	TruncateTenantData(t, pool)

	companyID = uuid.New()
	whID := uuid.New()
	userID := uuid.New()
	ownerEmail = fmt.Sprintf("wh-owner-%s@test.local", companyID.String()[:8])
	code := fmt.Sprintf("W%s", companyID.String()[:6])

	var roleID uuid.UUID
	err := pool.QueryRow(ctx, `SELECT id FROM roles WHERE name = 'company_owner' LIMIT 1`).Scan(&roleID)
	if err != nil {
		t.Fatalf("roles.company_owner: %v", err)
	}

	_, err = pool.Exec(ctx, `
		INSERT INTO companies (
			id, code, name, created_at, updated_at,
			status, is_verified, subscription_plan,
			max_warehouses, max_users, documents_monthly_limit,
			mp_customer_id, mp_card_id
		) VALUES ($1, $2, $3, NOW(), NOW(), 'active', true, 'pyme', 2, 5, 1000, 'wc', 'wc')
	`, companyID, code, "Webhook Co")
	if err != nil {
		t.Fatalf("insert company: %v", err)
	}

	_, err = pool.Exec(ctx, `
		INSERT INTO warehouses (id, company_id, name, address, created_at, updated_at)
		VALUES ($1, $2, 'Main', '', NOW(), NOW())
	`, whID, companyID)
	if err != nil {
		t.Fatalf("insert warehouse: %v", err)
	}

	_, err = pool.Exec(ctx, `
		INSERT INTO users (
			id, company_id, warehouse_id, email, username, password_hash,
			role, role_id, status, is_verified, created_at, updated_at
		) VALUES ($1, $2, $3, $4, 'owner', $5, 'company_owner', $6, 'active', true, NOW(), NOW())
	`, userID, companyID, whID, ownerEmail, SeedHash, roleID)
	if err != nil {
		t.Fatalf("insert user: %v", err)
	}

	return companyID, ownerEmail
}

// SleepBrief allows async goroutines (e.g. QueuePaymentReceipt) to finish in integration tests.
func SleepBrief() {
	time.Sleep(150 * time.Millisecond)
}
