package repository

import (
	"context"
	"database/sql"
	"errors"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"server/internal/models"
)

type CompanyRepository struct {
	pool *pgxpool.Pool
}

func NewCompanyRepository(pool *pgxpool.Pool) *CompanyRepository {
	return &CompanyRepository{pool: pool}
}

func (r *CompanyRepository) GetByCode(ctx context.Context, code string) (*models.Company, error) {
	query := `
		SELECT id, code, name, created_at, updated_at
		FROM companies WHERE code = $1
	`
	var company models.Company
	err := r.pool.QueryRow(ctx, query, code).Scan(
		&company.ID,
		&company.Code,
		&company.Name,
		&company.CreatedAt,
		&company.UpdatedAt,
	)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &company, nil
}

// GetByIDForBilling loads company rows needed for subscription / download entitlement checks.
func (r *CompanyRepository) GetByIDForBilling(ctx context.Context, id uuid.UUID) (*models.Company, error) {
	query := `
		SELECT
			id, code, name, COALESCE(cuit, '') AS cuit,
			status, is_verified, subscription_plan,
			subscription_expires_at, trial_ends_at,
			max_warehouses, max_users, documents_monthly_limit,
			created_at, updated_at, archived_at
		FROM companies WHERE id = $1
	`
	var c models.Company
	var maxW, maxU, maxDoc sql.NullInt32
	err := r.pool.QueryRow(ctx, query, id).Scan(
		&c.ID,
		&c.Code,
		&c.Name,
		&c.Cuit,
		&c.Status,
		&c.IsVerified,
		&c.SubscriptionPlan,
		&c.SubscriptionExpiresAt,
		&c.TrialEndsAt,
		&maxW,
		&maxU,
		&maxDoc,
		&c.CreatedAt,
		&c.UpdatedAt,
		&c.ArchivedAt,
	)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	if maxW.Valid {
		v := int(maxW.Int32)
		c.MaxWarehouses = &v
	}
	if maxU.Valid {
		v := int(maxU.Int32)
		c.MaxUsers = &v
	}
	if maxDoc.Valid {
		v := int(maxDoc.Int32)
		c.DocumentsMonthlyLimit = &v
	}
	return &c, nil
}

func (r *CompanyRepository) GetByID(ctx context.Context, id string) (*models.Company, error) {
	query := `
		SELECT id, code, name, created_at, updated_at
		FROM companies WHERE id = $1
	`
	var company models.Company
	err := r.pool.QueryRow(ctx, query, id).Scan(
		&company.ID,
		&company.Code,
		&company.Name,
		&company.CreatedAt,
		&company.UpdatedAt,
	)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &company, nil
}

func (r *CompanyRepository) Create(ctx context.Context, company *models.Company) error {
	query := `
		INSERT INTO companies (id, code, name, created_at, updated_at)
		VALUES ($1, $2, $3, $4, $5)
	`
	_, err := r.pool.Exec(ctx, query,
		company.ID,
		company.Code,
		company.Name,
		company.CreatedAt,
		company.UpdatedAt,
	)
	if err != nil {
		return err
	}
	return nil
}

// CreateTrial inserts a company with 7-day-style trial fields and Mercado Pago ids (card on file for later charges).
func (r *CompanyRepository) CreateTrial(ctx context.Context, company *models.Company) error {
	query := `
		INSERT INTO companies (
			id, code, name, created_at, updated_at,
			status, is_verified, subscription_plan,
			trial_ends_at, max_warehouses, max_users,
			mp_customer_id, mp_card_id, documents_monthly_limit
		)
		VALUES ($1, $2, $3, $4, $5, 'active', false, 'trial', $6, $7, $8, $9, $10, $11)
	`
	_, err := r.pool.Exec(ctx, query,
		company.ID,
		company.Code,
		company.Name,
		company.CreatedAt,
		company.UpdatedAt,
		company.TrialEndsAt,
		company.MaxWarehouses,
		company.MaxUsers,
		company.MpCustomerID,
		company.MpCardID,
		company.DocumentsMonthlyLimit,
	)
	return err
}
