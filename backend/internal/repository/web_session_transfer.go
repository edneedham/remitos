package repository

import (
	"context"
	"errors"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"server/internal/models"
)

type WebSessionTransferRepository struct {
	pool *pgxpool.Pool
}

func NewWebSessionTransferRepository(pool *pgxpool.Pool) *WebSessionTransferRepository {
	return &WebSessionTransferRepository{pool: pool}
}

func (r *WebSessionTransferRepository) Create(ctx context.Context, transfer *models.WebSessionTransfer) error {
	query := `
		INSERT INTO web_session_transfers (
			id, token_hash, user_id, desktop_refresh_token_id, expires_at, created_at
		)
		VALUES ($1, $2, $3, $4, $5, $6)
	`
	_, err := r.pool.Exec(ctx, query,
		transfer.ID,
		transfer.TokenHash,
		transfer.UserID,
		transfer.DesktopRefreshTokenID,
		transfer.ExpiresAt,
		transfer.CreatedAt,
	)
	return err
}

func (r *WebSessionTransferRepository) ConsumeByTokenHash(ctx context.Context, tokenHash string) (*models.WebSessionTransfer, error) {
	query := `
		UPDATE web_session_transfers
		SET used_at = NOW()
		WHERE token_hash = $1
			AND used_at IS NULL
			AND expires_at > NOW()
		RETURNING id, token_hash, user_id, desktop_refresh_token_id, phone_refresh_token_id, expires_at, used_at, created_at
	`
	var transfer models.WebSessionTransfer
	var phoneRefreshTokenID *uuid.UUID
	var usedAt *time.Time
	err := r.pool.QueryRow(ctx, query, tokenHash).Scan(
		&transfer.ID,
		&transfer.TokenHash,
		&transfer.UserID,
		&transfer.DesktopRefreshTokenID,
		&phoneRefreshTokenID,
		&transfer.ExpiresAt,
		&usedAt,
		&transfer.CreatedAt,
	)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	transfer.PhoneRefreshTokenID = phoneRefreshTokenID
	transfer.UsedAt = usedAt
	return &transfer, nil
}

func (r *WebSessionTransferRepository) AttachPhoneRefreshToken(ctx context.Context, transferID uuid.UUID, phoneRefreshTokenID uuid.UUID) error {
	query := `
		UPDATE web_session_transfers
		SET phone_refresh_token_id = $1
		WHERE id = $2
	`
	_, err := r.pool.Exec(ctx, query, phoneRefreshTokenID, transferID)
	return err
}
