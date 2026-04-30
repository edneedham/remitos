package repository

import (
	"context"
	"database/sql"
	"errors"
	"time"

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

func (r *CompanyRepository) UpdateSignupPlan(
	ctx context.Context,
	companyID uuid.UUID,
	plan string,
	maxWarehouses *int,
	maxUsers *int,
	documentsMonthlyLimit *int,
) error {
	query := `
		UPDATE companies
		SET
			subscription_plan = $2,
			max_warehouses = $3,
			max_users = $4,
			documents_monthly_limit = $5,
			updated_at = NOW(),
			trial_activation_at = CASE
				WHEN ($2::text IN ('pyme', 'empresa')) AND trial_activation_at IS NULL THEN NOW()
				ELSE trial_activation_at
			END
		WHERE id = $1
	`
	_, err := r.pool.Exec(
		ctx,
		query,
		companyID,
		plan,
		maxWarehouses,
		maxUsers,
		documentsMonthlyLimit,
	)
	return err
}

// TrialOnboardingNudgeRow is one company eligible for onboarding reminder processing.
type TrialOnboardingNudgeRow struct {
	CompanyID         uuid.UUID
	CompanyName       string
	CompanyCode       string
	TrialActivationAt time.Time
	SetupSent         sql.NullTime
	Day1Sent          sql.NullTime
	Day3Sent          sql.NullTime
	OwnerEmail        string
}

// ListCompaniesForTrialOnboardingNudges returns active trial companies with no inbound notes yet,
// joined to the company_owner email for outbound mail.
func (r *CompanyRepository) ListCompaniesForTrialOnboardingNudges(ctx context.Context) ([]TrialOnboardingNudgeRow, error) {
	query := `
		SELECT DISTINCT ON (c.id)
			c.id,
			c.name,
			c.code,
			c.trial_activation_at,
			c.onboarding_nudge_setup_sent_at,
			c.onboarding_nudge_day1_sent_at,
			c.onboarding_nudge_day3_sent_at,
			COALESCE(NULLIF(TRIM(u.email), ''), NULLIF(TRIM(u.username), ''), '') AS owner_email
		FROM companies c
		INNER JOIN users u ON u.company_id = c.id AND u.role = 'company_owner'
		WHERE c.trial_activation_at IS NOT NULL
			AND c.status = 'active'
			AND c.archived_at IS NULL
			AND (c.trial_ends_at IS NULL OR c.trial_ends_at > NOW())
			AND NOT EXISTS (
				SELECT 1 FROM inbound_notes n WHERE n.company_id = c.id LIMIT 1
			)
		ORDER BY c.id, u.created_at ASC
	`
	rows, err := r.pool.Query(ctx, query)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var out []TrialOnboardingNudgeRow
	for rows.Next() {
		var row TrialOnboardingNudgeRow
		if err := rows.Scan(
			&row.CompanyID,
			&row.CompanyName,
			&row.CompanyCode,
			&row.TrialActivationAt,
			&row.SetupSent,
			&row.Day1Sent,
			&row.Day3Sent,
			&row.OwnerEmail,
		); err != nil {
			return nil, err
		}
		out = append(out, row)
	}
	return out, rows.Err()
}

// ApplyTrialOnboardingNudgeSent marks the given stage (and implied earlier stages) as sent.
func (r *CompanyRepository) ApplyTrialOnboardingNudgeSent(ctx context.Context, companyID uuid.UUID, stage string) error {
	var q string
	switch stage {
	case "setup":
		q = `
			UPDATE companies
			SET onboarding_nudge_setup_sent_at = NOW(), updated_at = NOW()
			WHERE id = $1
		`
	case "day1":
		q = `
			UPDATE companies
			SET
				onboarding_nudge_setup_sent_at = COALESCE(onboarding_nudge_setup_sent_at, NOW()),
				onboarding_nudge_day1_sent_at = NOW(),
				updated_at = NOW()
			WHERE id = $1
		`
	case "day3":
		q = `
			UPDATE companies
			SET
				onboarding_nudge_setup_sent_at = COALESCE(onboarding_nudge_setup_sent_at, NOW()),
				onboarding_nudge_day1_sent_at = COALESCE(onboarding_nudge_day1_sent_at, NOW()),
				onboarding_nudge_day3_sent_at = NOW(),
				updated_at = NOW()
			WHERE id = $1
		`
	default:
		return errors.New("unknown onboarding nudge stage")
	}
	_, err := r.pool.Exec(ctx, q, companyID)
	return err
}
