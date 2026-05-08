package repository

import (
	"context"
	"errors"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"server/internal/models"
)

type UserNotificationRepository struct {
	pool *pgxpool.Pool
}

func NewUserNotificationRepository(pool *pgxpool.Pool) *UserNotificationRepository {
	return &UserNotificationRepository{pool: pool}
}

func (r *UserNotificationRepository) Insert(ctx context.Context, n *models.UserNotification) error {
	if n.ID == uuid.Nil {
		n.ID = uuid.New()
	}
	q := `
		INSERT INTO user_notifications (
			id, user_id, company_id, kind, title, body, action_url, read_at, metadata, created_at
		) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
	`
	_, err := r.pool.Exec(ctx, q,
		n.ID,
		n.UserID,
		n.CompanyID,
		string(n.Kind),
		n.Title,
		n.Body,
		n.ActionURL,
		n.ReadAt,
		n.Metadata,
		n.CreatedAt,
	)
	return err
}

func (r *UserNotificationRepository) ListForUser(
	ctx context.Context,
	userID, companyID uuid.UUID,
	limit, offset int,
	unreadOnly bool,
) ([]models.UserNotification, error) {
	if limit <= 0 || limit > 100 {
		limit = 50
	}
	if offset < 0 {
		offset = 0
	}
	q := `
		SELECT id, user_id, company_id, kind, title, body, action_url, read_at, metadata, created_at
		FROM user_notifications
		WHERE user_id = $1 AND company_id = $2
	`
	args := []interface{}{userID, companyID}
	if unreadOnly {
		q += ` AND read_at IS NULL`
	}
	q += ` ORDER BY created_at DESC LIMIT $3 OFFSET $4`
	args = append(args, limit, offset)

	rows, err := r.pool.Query(ctx, q, args...)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var out []models.UserNotification
	for rows.Next() {
		var n models.UserNotification
		var kind string
		if err := rows.Scan(
			&n.ID,
			&n.UserID,
			&n.CompanyID,
			&kind,
			&n.Title,
			&n.Body,
			&n.ActionURL,
			&n.ReadAt,
			&n.Metadata,
			&n.CreatedAt,
		); err != nil {
			return nil, err
		}
		n.Kind = models.UserNotificationKind(kind)
		out = append(out, n)
	}
	return out, rows.Err()
}

func (r *UserNotificationRepository) CountUnread(ctx context.Context, userID, companyID uuid.UUID) (int64, error) {
	q := `
		SELECT COUNT(*) FROM user_notifications
		WHERE user_id = $1 AND company_id = $2 AND read_at IS NULL
	`
	var n int64
	err := r.pool.QueryRow(ctx, q, userID, companyID).Scan(&n)
	return n, err
}

func (r *UserNotificationRepository) MarkRead(
	ctx context.Context,
	id, userID, companyID uuid.UUID,
) error {
	q := `
		UPDATE user_notifications
		SET read_at = NOW()
		WHERE id = $1 AND user_id = $2 AND company_id = $3 AND read_at IS NULL
	`
	tag, err := r.pool.Exec(ctx, q, id, userID, companyID)
	if err != nil {
		return err
	}
	if tag.RowsAffected() == 0 {
		return ErrNotificationNotFound
	}
	return nil
}

// ErrNotificationNotFound is returned when no row matched mark-read.
var ErrNotificationNotFound = errors.New("notification not found")
