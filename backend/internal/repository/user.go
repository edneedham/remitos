package repository

import (
	"context"
	"errors"
	"strings"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"server/internal/models"
)

var (
	ErrUserExists = errors.New("user already exists")
)

type UserRepository struct {
	pool *pgxpool.Pool
}

func NewUserRepository(pool *pgxpool.Pool) *UserRepository {
	return &UserRepository{pool: pool}
}

func (r *UserRepository) Create(ctx context.Context, user *models.User) error {
	query := `
		INSERT INTO users (id, company_id, warehouse_id, email, username, password_hash, role, role_id, status, is_verified, created_at, updated_at)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
	`
	_, err := r.pool.Exec(ctx, query,
		user.ID,
		user.CompanyID,
		user.WarehouseID,
		user.Email,
		user.Username,
		user.PasswordHash,
		user.Role,
		user.RoleID,
		user.Status,
		user.IsVerified,
		user.CreatedAt,
		user.UpdatedAt,
	)
	if err != nil {
		return err
	}
	return nil
}

func (r *UserRepository) GetByEmail(ctx context.Context, email string) (*models.User, error) {
	query := `
		SELECT id, company_id, warehouse_id, email, username, password_hash, role, role_id, status, is_verified, created_at, updated_at
		FROM users WHERE email = $1
	`
	var user models.User
	err := r.pool.QueryRow(ctx, query, email).Scan(
		&user.ID,
		&user.CompanyID,
		&user.WarehouseID,
		&user.Email,
		&user.Username,
		&user.PasswordHash,
		&user.Role,
		&user.RoleID,
		&user.Status,
		&user.IsVerified,
		&user.CreatedAt,
		&user.UpdatedAt,
	)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &user, nil
}

func (r *UserRepository) GetByEmailAndCompanyID(ctx context.Context, email string, companyID uuid.UUID) (*models.User, error) {
	query := `
		SELECT id, company_id, warehouse_id, email, username, password_hash, role, role_id, status, is_verified, created_at, updated_at
		FROM users WHERE email = $1 AND company_id = $2
	`
	var user models.User
	err := r.pool.QueryRow(ctx, query, email, companyID).Scan(
		&user.ID,
		&user.CompanyID,
		&user.WarehouseID,
		&user.Email,
		&user.Username,
		&user.PasswordHash,
		&user.Role,
		&user.RoleID,
		&user.Status,
		&user.IsVerified,
		&user.CreatedAt,
		&user.UpdatedAt,
	)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &user, nil
}

func (r *UserRepository) GetByUsernameAndCompanyID(ctx context.Context, username string, companyID uuid.UUID) (*models.User, error) {
	query := `
		SELECT id, company_id, warehouse_id, email, username, password_hash, role, role_id, status, is_verified, created_at, updated_at
		FROM users WHERE username = $1 AND company_id = $2
	`
	var user models.User
	err := r.pool.QueryRow(ctx, query, username, companyID).Scan(
		&user.ID,
		&user.CompanyID,
		&user.WarehouseID,
		&user.Email,
		&user.Username,
		&user.PasswordHash,
		&user.Role,
		&user.RoleID,
		&user.Status,
		&user.IsVerified,
		&user.CreatedAt,
		&user.UpdatedAt,
	)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &user, nil
}

func (r *UserRepository) GetByID(ctx context.Context, id uuid.UUID) (*models.User, error) {
	query := `
		SELECT id, company_id, warehouse_id, email, username, password_hash, role, role_id, status, is_verified, created_at, updated_at
		FROM users WHERE id = $1
	`
	var user models.User
	err := r.pool.QueryRow(ctx, query, id).Scan(
		&user.ID,
		&user.CompanyID,
		&user.WarehouseID,
		&user.Email,
		&user.Username,
		&user.PasswordHash,
		&user.Role,
		&user.RoleID,
		&user.Status,
		&user.IsVerified,
		&user.CreatedAt,
		&user.UpdatedAt,
	)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &user, nil
}

func (r *UserRepository) GetByCompanyID(ctx context.Context, companyID uuid.UUID) ([]*models.User, error) {
	query := `
		SELECT id, company_id, warehouse_id, email, username, password_hash, role, role_id, status, is_verified, created_at, updated_at
		FROM users WHERE company_id = $1
	`
	rows, err := r.pool.Query(ctx, query, companyID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var users []*models.User
	for rows.Next() {
		var user models.User
		err := rows.Scan(
			&user.ID,
			&user.CompanyID,
			&user.WarehouseID,
			&user.Email,
			&user.Username,
			&user.PasswordHash,
			&user.Role,
			&user.RoleID,
			&user.Status,
			&user.IsVerified,
			&user.CreatedAt,
			&user.UpdatedAt,
		)
		if err != nil {
			return nil, err
		}
		users = append(users, &user)
	}
	if err = rows.Err(); err != nil {
		return nil, err
	}
	return users, nil
}

func (r *UserRepository) UpdateStatus(ctx context.Context, id uuid.UUID, status string) error {
	query := `UPDATE users SET status = $1, updated_at = NOW() WHERE id = $2`
	_, err := r.pool.Exec(ctx, query, status, id)
	return err
}

func (r *UserRepository) UpdatePassword(ctx context.Context, id uuid.UUID, passwordHash string) error {
	query := `UPDATE users SET password_hash = $1, updated_at = NOW() WHERE id = $2`
	_, err := r.pool.Exec(ctx, query, passwordHash, id)
	return err
}

// GetCompanyOwnerPrimaryEmail returns the first company_owner email or username-like identifier for payer metadata.
func (r *UserRepository) GetCompanyOwnerPrimaryEmail(ctx context.Context, companyID uuid.UUID) (string, error) {
	query := `
		SELECT COALESCE(NULLIF(TRIM(email), ''), NULLIF(TRIM(username), ''), '')
		FROM users
		WHERE company_id = $1 AND role = 'company_owner'
		ORDER BY created_at ASC
		LIMIT 1
	`
	var s string
	err := r.pool.QueryRow(ctx, query, companyID).Scan(&s)
	if errors.Is(err, pgx.ErrNoRows) {
		return "", nil
	}
	if err != nil {
		return "", err
	}
	return strings.TrimSpace(s), nil
}
