package repository

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

type InvoiceRepository struct {
	pool *pgxpool.Pool
}

func NewInvoiceRepository(pool *pgxpool.Pool) *InvoiceRepository {
	return &InvoiceRepository{pool: pool}
}

// BillingInvoice is a persisted invoice row for a company.
type BillingInvoice struct {
	ID          uuid.UUID
	CompanyID   uuid.UUID
	AmountMinor int64
	Currency    string
	Status      string
	Description string
	IssuedAt    time.Time
	MpPaymentID *string
}

func (r *InvoiceRepository) ListByCompanyID(ctx context.Context, companyID uuid.UUID) ([]BillingInvoice, error) {
	rows, err := r.pool.Query(ctx, `
		SELECT id, company_id, amount_minor, currency, status,
		       COALESCE(description, ''), issued_at, mp_payment_id
		FROM billing_invoices
		WHERE company_id = $1
		ORDER BY issued_at DESC, created_at DESC
	`, companyID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	out := make([]BillingInvoice, 0)
	for rows.Next() {
		var inv BillingInvoice
		if err := rows.Scan(
			&inv.ID,
			&inv.CompanyID,
			&inv.AmountMinor,
			&inv.Currency,
			&inv.Status,
			&inv.Description,
			&inv.IssuedAt,
			&inv.MpPaymentID,
		); err != nil {
			return nil, err
		}
		out = append(out, inv)
	}
	if err := rows.Err(); err != nil {
		return nil, err
	}
	return out, nil
}

// InsertPending creates a billing invoice in pending status (issued, awaiting payment confirmation).
func (r *InvoiceRepository) InsertPending(ctx context.Context, conn DBConn, companyID uuid.UUID, amountMinor int64, currency, description string) (uuid.UUID, error) {
	query := `
		INSERT INTO billing_invoices (company_id, amount_minor, currency, status, description, issued_at)
		VALUES ($1, $2, $3, 'pending', $4, NOW())
		RETURNING id
	`
	var id uuid.UUID
	err := conn.QueryRow(ctx, query, companyID, amountMinor, currency, description).Scan(&id)
	return id, err
}

// MarkPaid sets status=paid and stores the Mercado Pago payment id for a pending invoice row.
// CountByCompanyID returns how many billing invoice rows exist for a company (any status).
func (r *InvoiceRepository) CountByCompanyID(ctx context.Context, companyID uuid.UUID) (int64, error) {
	var n int64
	err := r.pool.QueryRow(ctx, `
		SELECT COUNT(*) FROM billing_invoices WHERE company_id = $1
	`, companyID).Scan(&n)
	return n, err
}

// InsertPaidInvoice records a paid invoice with a Mercado Pago payment id (caller ensures idempotency).
func (r *InvoiceRepository) InsertPaidInvoice(ctx context.Context, companyID uuid.UUID, amountMinor int64, currency, description, mpPaymentID string) error {
	return r.InsertPaidInvoiceTx(ctx, r.pool, companyID, amountMinor, currency, description, mpPaymentID)
}

// InsertPaidInvoiceTx records a paid invoice using an existing connection or transaction.
func (r *InvoiceRepository) InsertPaidInvoiceTx(ctx context.Context, conn DBConn, companyID uuid.UUID, amountMinor int64, currency, description, mpPaymentID string) error {
	if mpPaymentID == "" {
		return fmt.Errorf("mp payment id required")
	}
	_, err := conn.Exec(ctx, `
		INSERT INTO billing_invoices (company_id, amount_minor, currency, status, description, issued_at, mp_payment_id)
		VALUES ($1, $2, $3, 'paid', $4, NOW(), $5)
	`, companyID, amountMinor, currency, description, mpPaymentID)
	return err
}

// ExistsMpPaymentID returns true if an invoice already references this MP payment id.
func (r *InvoiceRepository) ExistsMpPaymentID(ctx context.Context, mpPaymentID string) (bool, error) {
	var n int64
	err := r.pool.QueryRow(ctx, `
		SELECT COUNT(*) FROM billing_invoices WHERE mp_payment_id = $1
	`, mpPaymentID).Scan(&n)
	return n > 0, err
}

// MarkPaid sets status=paid for a pending invoice. Returns true if a row was updated.
func (r *InvoiceRepository) MarkPaid(ctx context.Context, conn DBConn, invoiceID, companyID uuid.UUID, mpPaymentID string) (bool, error) {
	tag, err := conn.Exec(ctx, `
		UPDATE billing_invoices
		SET status = 'paid', mp_payment_id = $3
		WHERE id = $1 AND company_id = $2 AND status = 'pending'
	`, invoiceID, companyID, mpPaymentID)
	if err != nil {
		return false, err
	}
	return tag.RowsAffected() > 0, nil
}

// GetByID returns a billing invoice by primary key.
func (r *InvoiceRepository) GetByID(ctx context.Context, invoiceID uuid.UUID) (*BillingInvoice, error) {
	var inv BillingInvoice
	var mpID sql.NullString
	err := r.pool.QueryRow(ctx, `
		SELECT id, company_id, amount_minor, currency, status,
		       COALESCE(description, ''), issued_at, mp_payment_id
		FROM billing_invoices
		WHERE id = $1
	`, invoiceID).Scan(
		&inv.ID,
		&inv.CompanyID,
		&inv.AmountMinor,
		&inv.Currency,
		&inv.Status,
		&inv.Description,
		&inv.IssuedAt,
		&mpID,
	)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return nil, nil
		}
		return nil, err
	}
	if mpID.Valid {
		s := mpID.String
		inv.MpPaymentID = &s
	}
	return &inv, nil
}
