package repository

import (
	"context"
	"errors"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgxpool"
	"server/internal/models"
)

var ErrWaitlistDuplicate = errors.New("waitlist email already registered")

// WaitlistCreateParams is a single insert into waitlist_entries.
type WaitlistCreateParams struct {
	EmailNormalized         string
	FullName                *string
	CompanyName             *string
	Source                  *string
	DeliveryNotesPerDayBand *string
	ProcessingMode          *string
	DigitalApplication      *string
	LogisticsPainPoints     *string
	WarehouseCount          *int
	ProductUpdatesOptIn     bool
}

type WaitlistRepository struct {
	pool *pgxpool.Pool
}

func NewWaitlistRepository(pool *pgxpool.Pool) *WaitlistRepository {
	return &WaitlistRepository{pool: pool}
}

// Create inserts a waitlist row. ErrWaitlistDuplicate if email_normalized already exists.
func (r *WaitlistRepository) Create(ctx context.Context, p WaitlistCreateParams) (*models.WaitlistEntry, error) {
	id := uuid.New()
	query := `
		INSERT INTO waitlist_entries (
			id, email_normalized, full_name, company_name, source,
			delivery_notes_per_day_band, processing_mode, digital_application, logistics_pain_points, warehouse_count,
			product_updates_opt_in
		)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
		RETURNING id, email_normalized, full_name, company_name, source,
			delivery_notes_per_day_band, processing_mode, digital_application, logistics_pain_points, warehouse_count,
			product_updates_opt_in, created_at
	`
	var row models.WaitlistEntry
	err := r.pool.QueryRow(ctx, query,
		id,
		p.EmailNormalized,
		p.FullName,
		p.CompanyName,
		p.Source,
		p.DeliveryNotesPerDayBand,
		p.ProcessingMode,
		p.DigitalApplication,
		p.LogisticsPainPoints,
		p.WarehouseCount,
		p.ProductUpdatesOptIn,
	).Scan(
		&row.ID,
		&row.EmailNormalized,
		&row.FullName,
		&row.CompanyName,
		&row.Source,
		&row.DeliveryNotesPerDayBand,
		&row.ProcessingMode,
		&row.DigitalApplication,
		&row.LogisticsPainPoints,
		&row.WarehouseCount,
		&row.ProductUpdatesOptIn,
		&row.CreatedAt,
	)
	if err != nil {
		var pgErr *pgconn.PgError
		if errors.As(err, &pgErr) && pgErr.Code == "23505" {
			return nil, ErrWaitlistDuplicate
		}
		return nil, err
	}
	return &row, nil
}
