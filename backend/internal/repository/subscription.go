package repository

import (
	"context"
	"encoding/json"
	"errors"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"server/internal/models"
)

type SubscriptionRepository struct {
	pool *pgxpool.Pool
}

func NewSubscriptionRepository(pool *pgxpool.Pool) *SubscriptionRepository {
	return &SubscriptionRepository{pool: pool}
}

func (r *SubscriptionRepository) Create(ctx context.Context, sub *models.Subscription) error {
	featuresJSON, err := json.Marshal(sub.Features)
	if err != nil {
		return err
	}
	query := `
		INSERT INTO subscriptions (id, user_id, device_id, status, device_connected, features, created_at, updated_at)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
	`
	_, err = r.pool.Exec(ctx, query,
		sub.ID,
		sub.UserID,
		sub.DeviceID,
		sub.Status,
		sub.DeviceConnected,
		featuresJSON,
		sub.CreatedAt,
		sub.UpdatedAt,
	)
	return err
}

func (r *SubscriptionRepository) GetByUserID(ctx context.Context, userID uuid.UUID) (*models.Subscription, error) {
	query := `
		SELECT id, user_id, device_id, status, device_connected, features, created_at, updated_at
		FROM subscriptions
		WHERE user_id = $1
		LIMIT 1
	`
	var sub models.Subscription
	var deviceID *uuid.UUID
	var featuresJSON []byte
	err := r.pool.QueryRow(ctx, query, userID).Scan(
		&sub.ID,
		&sub.UserID,
		&deviceID,
		&sub.Status,
		&sub.DeviceConnected,
		&featuresJSON,
		&sub.CreatedAt,
		&sub.UpdatedAt,
	)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	sub.DeviceID = deviceID
	if err := json.Unmarshal(featuresJSON, &sub.Features); err != nil {
		return nil, err
	}
	return &sub, nil
}

func (r *SubscriptionRepository) LinkDevice(ctx context.Context, userID uuid.UUID, deviceID uuid.UUID) error {
	query := `
		UPDATE subscriptions SET device_id = $1, device_connected = TRUE, updated_at = $2
		WHERE user_id = $3
	`
	result, err := r.pool.Exec(ctx, query, deviceID, time.Now(), userID)
	if err != nil {
		return err
	}
	if result.RowsAffected() == 0 {
		return pgx.ErrNoRows
	}
	return nil
}

func (r *SubscriptionRepository) UpdateStatus(ctx context.Context, userID uuid.UUID, status string, features models.SubscriptionFeatures) error {
	featuresJSON, err := json.Marshal(features)
	if err != nil {
		return err
	}
	query := `
		UPDATE subscriptions SET status = $1, features = $2, updated_at = $3
		WHERE user_id = $4
	`
	result, err := r.pool.Exec(ctx, query, status, featuresJSON, time.Now(), userID)
	if err != nil {
		return err
	}
	if result.RowsAffected() == 0 {
		return pgx.ErrNoRows
	}
	return nil
}
