package repository

import (
	"context"
	"fmt"
	"time"

	"github.com/google/uuid"
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
func (r *InvoiceRepository) MarkPaid(ctx context.Context, conn DBConn, invoiceID, companyID uuid.UUID, mpPaymentID string) error {
	tag, err := conn.Exec(ctx, `
		UPDATE billing_invoices
		SET status = 'paid', mp_payment_id = $3
		WHERE id = $1 AND company_id = $2 AND status = 'pending'
	`, invoiceID, companyID, mpPaymentID)
	if err != nil {
		return err
	}
	if tag.RowsAffected() == 0 {
		return fmt.Errorf("invoice %s not pending or not found for company", invoiceID)
	}
	return nil
}
