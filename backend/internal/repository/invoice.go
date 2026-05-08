package repository

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"strings"
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
	ID                         uuid.UUID
	CompanyID                  uuid.UUID
	AmountMinor                int64
	Currency                   string
	Status                     string
	Description                string
	IssuedAt                   time.Time
	USDListAmount              sql.NullFloat64
	ARSPerUSD                  sql.NullFloat64
	FXSource                   sql.NullString
	FXEffectiveDate            sql.NullTime
	MpPaymentID                *string
	ReceiptEmailSentAt         sql.NullTime
	RenewalFailureNoticeSentAt sql.NullTime
	// AFIP factura electrónica (populated by billing.FacturaEmitter).
	FacturaTipo       sql.NullInt32
	FacturaPtoVta     sql.NullInt32
	FacturaNumero     sql.NullInt64
	FacturaCAE        sql.NullString
	FacturaCAEVto     sql.NullTime
	FacturaEmittedAt  sql.NullTime
	FacturaRequestID  sql.NullString
	FacturaLastError  sql.NullString
	FacturaAttempts   int
}

// InvoiceFXSnapshot stores conversion metadata captured at invoice issuance time.
// Nil/zero fields mean "not available" for that invoice path.
type InvoiceFXSnapshot struct {
	USDListAmount   *float64
	ARSPerUSD       *float64
	FXSource        string
	FXEffectiveDate *time.Time
}

func (r *InvoiceRepository) ListByCompanyID(ctx context.Context, companyID uuid.UUID) ([]BillingInvoice, error) {
	rows, err := r.pool.Query(ctx, `
		SELECT id, company_id, amount_minor, currency, status,
		       COALESCE(description, ''), issued_at,
		       usd_list_amount, ars_per_usd, fx_source, fx_effective_date,
		       mp_payment_id,
		       receipt_email_sent_at, renewal_failure_notice_sent_at,
		       factura_tipo, factura_pto_vta, factura_numero,
		       factura_cae, factura_cae_vto, factura_emitted_at,
		       factura_request_id, factura_last_error, factura_attempts
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
		var mpID sql.NullString
		if err := rows.Scan(
			&inv.ID,
			&inv.CompanyID,
			&inv.AmountMinor,
			&inv.Currency,
			&inv.Status,
			&inv.Description,
			&inv.IssuedAt,
			&inv.USDListAmount,
			&inv.ARSPerUSD,
			&inv.FXSource,
			&inv.FXEffectiveDate,
			&mpID,
			&inv.ReceiptEmailSentAt,
			&inv.RenewalFailureNoticeSentAt,
			&inv.FacturaTipo,
			&inv.FacturaPtoVta,
			&inv.FacturaNumero,
			&inv.FacturaCAE,
			&inv.FacturaCAEVto,
			&inv.FacturaEmittedAt,
			&inv.FacturaRequestID,
			&inv.FacturaLastError,
			&inv.FacturaAttempts,
		); err != nil {
			return nil, err
		}
		if mpID.Valid {
			s := mpID.String
			inv.MpPaymentID = &s
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
	return r.InsertPendingWithSnapshot(ctx, conn, companyID, amountMinor, currency, description, nil)
}

// InsertPendingWithSnapshot creates a pending invoice and optionally captures FX snapshot metadata.
func (r *InvoiceRepository) InsertPendingWithSnapshot(ctx context.Context, conn DBConn, companyID uuid.UUID, amountMinor int64, currency, description string, fx *InvoiceFXSnapshot) (uuid.UUID, error) {
	var (
		usdListAmount   any
		arsPerUSD       any
		fxSource        any
		fxEffectiveDate any
	)
	if fx != nil {
		if fx.USDListAmount != nil {
			usdListAmount = *fx.USDListAmount
		}
		if fx.ARSPerUSD != nil {
			arsPerUSD = *fx.ARSPerUSD
		}
		if s := strings.TrimSpace(fx.FXSource); s != "" {
			fxSource = s
		}
		if fx.FXEffectiveDate != nil {
			fxEffectiveDate = *fx.FXEffectiveDate
		}
	}

	query := `
		INSERT INTO billing_invoices (
			company_id, amount_minor, currency, status, description, issued_at,
			usd_list_amount, ars_per_usd, fx_source, fx_effective_date
		)
		VALUES ($1, $2, $3, 'pending', $4, NOW(), $5, $6, $7, $8)
		RETURNING id
	`
	var id uuid.UUID
	err := conn.QueryRow(
		ctx,
		query,
		companyID,
		amountMinor,
		currency,
		description,
		usdListAmount,
		arsPerUSD,
		fxSource,
		fxEffectiveDate,
	).Scan(&id)
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
	_, err := r.InsertPaidInvoiceTx(ctx, r.pool, companyID, amountMinor, currency, description, mpPaymentID)
	return err
}

// InsertPaidInvoiceTx records a paid invoice using an existing connection or transaction and returns the new row id.
func (r *InvoiceRepository) InsertPaidInvoiceTx(ctx context.Context, conn DBConn, companyID uuid.UUID, amountMinor int64, currency, description, mpPaymentID string) (uuid.UUID, error) {
	if mpPaymentID == "" {
		return uuid.Nil, fmt.Errorf("mp payment id required")
	}
	var id uuid.UUID
	err := conn.QueryRow(ctx, `
		INSERT INTO billing_invoices (company_id, amount_minor, currency, status, description, issued_at, mp_payment_id)
		VALUES ($1, $2, $3, 'paid', $4, NOW(), $5)
		RETURNING id
	`, companyID, amountMinor, currency, description, mpPaymentID).Scan(&id)
	return id, err
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
		       COALESCE(description, ''), issued_at,
		       usd_list_amount, ars_per_usd, fx_source, fx_effective_date,
		       mp_payment_id,
		       receipt_email_sent_at, renewal_failure_notice_sent_at,
		       factura_tipo, factura_pto_vta, factura_numero,
		       factura_cae, factura_cae_vto, factura_emitted_at,
		       factura_request_id, factura_last_error, factura_attempts
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
		&inv.USDListAmount,
		&inv.ARSPerUSD,
		&inv.FXSource,
		&inv.FXEffectiveDate,
		&mpID,
		&inv.ReceiptEmailSentAt,
		&inv.RenewalFailureNoticeSentAt,
		&inv.FacturaTipo,
		&inv.FacturaPtoVta,
		&inv.FacturaNumero,
		&inv.FacturaCAE,
		&inv.FacturaCAEVto,
		&inv.FacturaEmittedAt,
		&inv.FacturaRequestID,
		&inv.FacturaLastError,
		&inv.FacturaAttempts,
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

// SetReceiptEmailSentIfUnset records that a payment receipt was sent (idempotent).
func (r *InvoiceRepository) SetReceiptEmailSentIfUnset(ctx context.Context, invoiceID uuid.UUID) (bool, error) {
	tag, err := r.pool.Exec(ctx, `
		UPDATE billing_invoices
		SET receipt_email_sent_at = NOW()
		WHERE id = $1 AND status = 'paid' AND mp_payment_id IS NOT NULL
			AND receipt_email_sent_at IS NULL
	`, invoiceID)
	if err != nil {
		return false, err
	}
	return tag.RowsAffected() > 0, nil
}

// SetRenewalFailureNoticeSentIfUnset records a renewal-failure email for this invoice (idempotent).
func (r *InvoiceRepository) SetRenewalFailureNoticeSentIfUnset(ctx context.Context, invoiceID uuid.UUID) (bool, error) {
	tag, err := r.pool.Exec(ctx, `
		UPDATE billing_invoices
		SET renewal_failure_notice_sent_at = NOW()
		WHERE id = $1 AND status = 'pending'
			AND renewal_failure_notice_sent_at IS NULL
	`, invoiceID)
	if err != nil {
		return false, err
	}
	return tag.RowsAffected() > 0, nil
}

const MaxFacturaAttempts = 10

// ListPaidInvoicesPendingFactura returns invoice ids that need AFIP CAE emission.
func (r *InvoiceRepository) ListPaidInvoicesPendingFactura(ctx context.Context, limit int) ([]uuid.UUID, error) {
	if limit <= 0 {
		limit = 50
	}
	rows, err := r.pool.Query(ctx, `
		SELECT id FROM billing_invoices
		WHERE status = 'paid'
			AND mp_payment_id IS NOT NULL
			AND factura_emitted_at IS NULL
			AND factura_attempts < $1
		ORDER BY issued_at ASC
		LIMIT $2
	`, MaxFacturaAttempts, limit)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var out []uuid.UUID
	for rows.Next() {
		var id uuid.UUID
		if err := rows.Scan(&id); err != nil {
			return nil, err
		}
		out = append(out, id)
	}
	return out, rows.Err()
}

// BeginFacturaAttempt allocates a stable request UUID for this FECAE idempotency key and bumps attempts.
func (r *InvoiceRepository) BeginFacturaAttempt(ctx context.Context, invoiceID uuid.UUID) (uuid.UUID, error) {
	var rid uuid.UUID
	err := r.pool.QueryRow(ctx, `
		UPDATE billing_invoices
		SET factura_request_id = COALESCE(factura_request_id, gen_random_uuid()),
			factura_attempts = factura_attempts + 1
		WHERE id = $1
			AND status = 'paid'
			AND factura_emitted_at IS NULL
			AND factura_attempts < $2
		RETURNING factura_request_id
	`, invoiceID, MaxFacturaAttempts).Scan(&rid)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return uuid.Nil, fmt.Errorf("invoice not eligible for factura attempt")
		}
		return uuid.Nil, err
	}
	return rid, nil
}

// MarkFacturaEmitted stores CAE and comprobante metadata after a successful FECAESolicitar.
func (r *InvoiceRepository) MarkFacturaEmitted(
	ctx context.Context,
	invoiceID uuid.UUID,
	tipo, ptoVta int,
	numero int64,
	cae string,
	caeVto time.Time,
) error {
	tag, err := r.pool.Exec(ctx, `
		UPDATE billing_invoices SET
			factura_tipo = $2,
			factura_pto_vta = $3,
			factura_numero = $4,
			factura_cae = $5,
			factura_cae_vto = $6,
			factura_emitted_at = NOW(),
			factura_last_error = NULL
		WHERE id = $1 AND status = 'paid' AND factura_emitted_at IS NULL
	`, invoiceID, tipo, ptoVta, numero, cae, caeVto)
	if err != nil {
		return err
	}
	if tag.RowsAffected() == 0 {
		return fmt.Errorf("mark factura: no row updated for %s", invoiceID)
	}
	return nil
}

// RecordFacturaAttempt logs SOAP / AFIP outcome for support and debugging.
func (r *InvoiceRepository) RecordFacturaAttempt(ctx context.Context, invoiceID uuid.UUID, ok bool, errCode, errMsg, reqXML, respXML string) error {
	_, err := r.pool.Exec(ctx, `
		INSERT INTO billing_invoice_factura_attempts (invoice_id, ok, error_code, error_msg, request_xml, response_xml)
		VALUES ($1, $2, NULLIF($3,''), NULLIF($4,''), NULLIF($5,''), NULLIF($6,''))
	`, invoiceID, ok, errCode, errMsg, reqXML, respXML)
	return err
}

// SetFacturaLastError updates only the human-readable last error (attempt counter managed elsewhere).
func (r *InvoiceRepository) SetFacturaLastError(ctx context.Context, invoiceID uuid.UUID, msg string) error {
	_, err := r.pool.Exec(ctx, `
		UPDATE billing_invoices SET factura_last_error = $2 WHERE id = $1
	`, invoiceID, strings.TrimSpace(msg))
	return err
}
