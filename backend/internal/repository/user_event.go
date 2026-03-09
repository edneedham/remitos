package repository

import (
	"context"
	"encoding/json"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"server/internal/models"
)

type UserEventRepository struct {
	pool *pgxpool.Pool
}

func NewUserEventRepository(pool *pgxpool.Pool) *UserEventRepository {
	return &UserEventRepository{pool: pool}
}

func (r *UserEventRepository) Create(ctx context.Context, event *models.UserEvent) error {
	query := `
		INSERT INTO user_events (
			id, user_id, company_id, event_type, performed_by, 
			metadata, ip_address, user_agent, created_at
		) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
	`
	_, err := r.pool.Exec(ctx, query,
		event.ID,
		event.UserID,
		event.CompanyID,
		event.EventType,
		event.PerformedBy,
		event.Metadata,
		event.IPAddress,
		event.UserAgent,
		event.CreatedAt,
	)
	return err
}

func (r *UserEventRepository) GetByUser(ctx context.Context, userID uuid.UUID, limit int) ([]models.UserEvent, error) {
	if limit <= 0 {
		limit = 100
	}
	query := `
		SELECT id, user_id, company_id, event_type, performed_by, 
			   metadata, ip_address, user_agent, created_at
		FROM user_events 
		WHERE user_id = $1
		ORDER BY created_at DESC
		LIMIT $2
	`
	rows, err := r.pool.Query(ctx, query, userID, limit)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var events []models.UserEvent
	for rows.Next() {
		var event models.UserEvent
		err := rows.Scan(
			&event.ID,
			&event.UserID,
			&event.CompanyID,
			&event.EventType,
			&event.PerformedBy,
			&event.Metadata,
			&event.IPAddress,
			&event.UserAgent,
			&event.CreatedAt,
		)
		if err != nil {
			return nil, err
		}
		events = append(events, event)
	}
	return events, nil
}

func (r *UserEventRepository) LogEvent(ctx context.Context, eventType models.UserEventType, userID, companyID uuid.UUID, performedBy *uuid.UUID, metadata map[string]interface{}, ipAddress, userAgent *string) error {
	metadataJSON, _ := json.Marshal(metadata)

	event := &models.UserEvent{
		ID:          uuid.New(),
		UserID:      userID,
		CompanyID:   companyID,
		EventType:   eventType,
		PerformedBy: performedBy,
		Metadata:    metadataJSON,
		IPAddress:   ipAddress,
		UserAgent:   userAgent,
		CreatedAt:   time.Now(),
	}

	return r.Create(ctx, event)
}
