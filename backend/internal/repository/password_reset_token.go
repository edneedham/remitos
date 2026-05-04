package repository

import (
	"context"
	"errors"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

// PasswordResetTokenRepository persists hashed reset tokens.
type PasswordResetTokenRepository struct {
	pool *pgxpool.Pool
}

func NewPasswordResetTokenRepository(pool *pgxpool.Pool) *PasswordResetTokenRepository {
	return &PasswordResetTokenRepository{pool: pool}
}

type PasswordResetToken struct {
	ID        uuid.UUID
	UserID    uuid.UUID
	TokenHash string
	ExpiresAt time.Time
	UsedAt    *time.Time
	CreatedAt time.Time
}

// DeletePendingForUser removes unused tokens for a user before issuing a new one.
func (r *PasswordResetTokenRepository) DeletePendingForUser(ctx context.Context, userID uuid.UUID) error {
	q := `DELETE FROM password_reset_tokens WHERE user_id = $1 AND used_at IS NULL`
	_, err := r.pool.Exec(ctx, q, userID)
	return err
}

// Insert creates a new reset token row.
func (r *PasswordResetTokenRepository) Insert(ctx context.Context, userID uuid.UUID, tokenHash string, expiresAt time.Time) (uuid.UUID, error) {
	id := uuid.New()
	q := `
		INSERT INTO password_reset_tokens (id, user_id, token_hash, expires_at)
		VALUES ($1, $2, $3, $4)
	`
	_, err := r.pool.Exec(ctx, q, id, userID, tokenHash, expiresAt)
	if err != nil {
		return uuid.Nil, err
	}
	return id, nil
}

// GetValidByTokenHash returns an unused, non-expired token row.
func (r *PasswordResetTokenRepository) GetValidByTokenHash(ctx context.Context, tokenHash string) (*PasswordResetToken, error) {
	q := `
		SELECT id, user_id, token_hash, expires_at, used_at, created_at
		FROM password_reset_tokens
		WHERE token_hash = $1 AND used_at IS NULL AND expires_at > NOW()
	`
	var row PasswordResetToken
	err := r.pool.QueryRow(ctx, q, tokenHash).Scan(
		&row.ID,
		&row.UserID,
		&row.TokenHash,
		&row.ExpiresAt,
		&row.UsedAt,
		&row.CreatedAt,
	)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &row, nil
}

// MarkUsed sets used_at for a token.
func (r *PasswordResetTokenRepository) MarkUsed(ctx context.Context, id uuid.UUID) error {
	q := `UPDATE password_reset_tokens SET used_at = NOW() WHERE id = $1 AND used_at IS NULL`
	_, err := r.pool.Exec(ctx, q, id)
	return err
}
