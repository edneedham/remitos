package repository

import (
	"context"
	"encoding/json"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"server/internal/models"
)

type DeviceEventRepository struct {
	pool *pgxpool.Pool
}

func NewDeviceEventRepository(pool *pgxpool.Pool) *DeviceEventRepository {
	return &DeviceEventRepository{pool: pool}
}

func (r *DeviceEventRepository) Create(ctx context.Context, event *models.DeviceEvent) error {
	query := `
		INSERT INTO device_events (
			id, device_id, company_id, warehouse_id, event_type, 
			performed_by, metadata, created_at
		) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
	`
	_, err := r.pool.Exec(ctx, query,
		event.ID,
		event.DeviceID,
		event.CompanyID,
		event.WarehouseID,
		event.EventType,
		event.PerformedBy,
		event.Metadata,
		event.CreatedAt,
	)
	return err
}

func (r *DeviceEventRepository) GetByDevice(ctx context.Context, deviceID uuid.UUID) ([]models.DeviceEvent, error) {
	query := `
		SELECT id, device_id, company_id, warehouse_id, event_type, 
			   performed_by, metadata, created_at
		FROM device_events 
		WHERE device_id = $1
		ORDER BY created_at DESC
	`
	rows, err := r.pool.Query(ctx, query, deviceID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var events []models.DeviceEvent
	for rows.Next() {
		var event models.DeviceEvent
		err := rows.Scan(
			&event.ID,
			&event.DeviceID,
			&event.CompanyID,
			&event.WarehouseID,
			&event.EventType,
			&event.PerformedBy,
			&event.Metadata,
			&event.CreatedAt,
		)
		if err != nil {
			return nil, err
		}
		events = append(events, event)
	}
	return events, nil
}

func (r *DeviceEventRepository) GetByCompany(ctx context.Context, companyID uuid.UUID, limit int) ([]models.DeviceEvent, error) {
	if limit <= 0 {
		limit = 100
	}
	query := `
		SELECT id, device_id, company_id, warehouse_id, event_type, 
			   performed_by, metadata, created_at
		FROM device_events 
		WHERE company_id = $1
		ORDER BY created_at DESC
		LIMIT $2
	`
	rows, err := r.pool.Query(ctx, query, companyID, limit)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var events []models.DeviceEvent
	for rows.Next() {
		var event models.DeviceEvent
		err := rows.Scan(
			&event.ID,
			&event.DeviceID,
			&event.CompanyID,
			&event.WarehouseID,
			&event.EventType,
			&event.PerformedBy,
			&event.Metadata,
			&event.CreatedAt,
		)
		if err != nil {
			return nil, err
		}
		events = append(events, event)
	}
	return events, nil
}

func (r *DeviceEventRepository) LogEvent(ctx context.Context, eventType models.DeviceEventType, deviceID, companyID, warehouseID uuid.UUID, performedBy *uuid.UUID, metadata map[string]interface{}) error {
	metadataJSON, err := json.Marshal(metadata)
	if err != nil {
		metadataJSON = []byte("{}")
	}

	event := &models.DeviceEvent{
		ID:          uuid.New(),
		DeviceID:    deviceID,
		CompanyID:   companyID,
		WarehouseID: warehouseID,
		EventType:   eventType,
		PerformedBy: performedBy,
		Metadata:    metadataJSON,
		CreatedAt:   time.Now(),
	}

	return r.Create(ctx, event)
}
