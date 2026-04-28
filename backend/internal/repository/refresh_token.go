package repository

import (
	"context"
	"errors"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"golang.org/x/crypto/bcrypt"
	"server/internal/models"
)

type RefreshTokenRepository struct {
	pool *pgxpool.Pool
}

func NewRefreshTokenRepository(pool *pgxpool.Pool) *RefreshTokenRepository {
	return &RefreshTokenRepository{pool: pool}
}

func (r *RefreshTokenRepository) Create(ctx context.Context, token *models.RefreshToken) error {
	query := `
		INSERT INTO refresh_tokens (id, user_id, token_hash, device_name, expires_at, created_at)
		VALUES ($1, $2, $3, $4, $5, $6)
	`
	_, err := r.pool.Exec(ctx, query,
		token.ID,
		token.UserID,
		token.TokenHash,
		token.DeviceName,
		token.ExpiresAt,
		token.CreatedAt,
	)
	return err
}

func (r *RefreshTokenRepository) GetByHash(ctx context.Context, tokenHash string) (*models.RefreshToken, error) {
	query := `
		SELECT id, user_id, token_hash, device_name, expires_at, revoked_at, created_at
		FROM refresh_tokens 
		WHERE token_hash = $1 AND revoked_at IS NULL AND expires_at > NOW()
	`
	var token models.RefreshToken
	var revokedAt *time.Time
	err := r.pool.QueryRow(ctx, query, tokenHash).Scan(
		&token.ID,
		&token.UserID,
		&token.TokenHash,
		&token.DeviceName,
		&token.ExpiresAt,
		&revokedAt,
		&token.CreatedAt,
	)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	token.RevokedAt = revokedAt
	return &token, nil
}

func (r *RefreshTokenRepository) GetValidByRawToken(ctx context.Context, rawToken string) (*models.RefreshToken, error) {
	query := `
		SELECT id, user_id, token_hash, device_name, expires_at, revoked_at, created_at
		FROM refresh_tokens
		WHERE revoked_at IS NULL AND expires_at > NOW()
	`
	rows, err := r.pool.Query(ctx, query)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	for rows.Next() {
		var token models.RefreshToken
		var revokedAt *time.Time
		if err := rows.Scan(
			&token.ID,
			&token.UserID,
			&token.TokenHash,
			&token.DeviceName,
			&token.ExpiresAt,
			&revokedAt,
			&token.CreatedAt,
		); err != nil {
			return nil, err
		}

		if bcrypt.CompareHashAndPassword([]byte(token.TokenHash), []byte(rawToken)) == nil {
			token.RevokedAt = revokedAt
			return &token, nil
		}
	}
	if err := rows.Err(); err != nil {
		return nil, err
	}
	return nil, nil
}

func (r *RefreshTokenRepository) Revoke(ctx context.Context, tokenID uuid.UUID) error {
	query := `UPDATE refresh_tokens SET revoked_at = NOW() WHERE id = $1`
	_, err := r.pool.Exec(ctx, query, tokenID)
	return err
}

func (r *RefreshTokenRepository) RevokeUserTokens(ctx context.Context, userID uuid.UUID) error {
	query := `UPDATE refresh_tokens SET revoked_at = NOW() WHERE user_id = $1 AND revoked_at IS NULL`
	_, err := r.pool.Exec(ctx, query, userID)
	return err
}

func (r *RefreshTokenRepository) CleanupExpired(ctx context.Context) error {
	query := `DELETE FROM refresh_tokens WHERE expires_at < NOW() OR revoked_at < NOW()`
	_, err := r.pool.Exec(ctx, query)
	return err
}
