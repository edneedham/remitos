package repository

import (
	"context"
	"errors"

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
			mp_customer_id, mp_card_id
		)
		VALUES ($1, $2, $3, $4, $5, 'active', false, 'trial', $6, $7, $8, $9, $10)
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
	)
	return err
}
