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

type DeviceRepository struct {
	pool *pgxpool.Pool
}

func NewDeviceRepository(pool *pgxpool.Pool) *DeviceRepository {
	return &DeviceRepository{pool: pool}
}

func (r *DeviceRepository) Create(ctx context.Context, device *models.Device) error {
	query := `
		INSERT INTO devices (
			id, company_id, warehouse_id, device_uuid, platform, model, 
			os_version, app_version, status, registered_at, last_seen_at
		) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
		ON CONFLICT (company_id, device_uuid) DO UPDATE SET
			last_seen_at = EXCLUDED.last_seen_at,
			os_version = EXCLUDED.os_version,
			app_version = EXCLUDED.app_version
	`
	_, err := r.pool.Exec(ctx, query,
		device.ID,
		device.CompanyID,
		device.WarehouseID,
		device.DeviceUUID,
		device.Platform,
		device.Model,
		device.OSVersion,
		device.AppVersion,
		device.Status,
		device.RegisteredAt,
		device.LastSeenAt,
	)
	return err
}

func (r *DeviceRepository) GetByUUID(ctx context.Context, companyID uuid.UUID, deviceUUID string) (*models.Device, error) {
	query := `
		SELECT id, company_id, warehouse_id, device_uuid, platform, model, 
			   os_version, app_version, status, approved_by, approved_at, 
			   registered_at, last_seen_at
		FROM devices 
		WHERE company_id = $1 AND device_uuid = $2
	`
	var device models.Device
	var approvedBy *uuid.UUID
	var approvedAt, lastSeenAt *time.Time
	err := r.pool.QueryRow(ctx, query, companyID, deviceUUID).Scan(
		&device.ID,
		&device.CompanyID,
		&device.WarehouseID,
		&device.DeviceUUID,
		&device.Platform,
		&device.Model,
		&device.OSVersion,
		&device.AppVersion,
		&device.Status,
		&approvedBy,
		&approvedAt,
		&device.RegisteredAt,
		&lastSeenAt,
	)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	device.LastSeenAt = lastSeenAt
	device.ApprovedBy = approvedBy
	device.ApprovedAt = approvedAt
	return &device, nil
}

func (r *DeviceRepository) GetByID(ctx context.Context, deviceID uuid.UUID) (*models.Device, error) {
	query := `
		SELECT id, company_id, warehouse_id, device_uuid, platform, model, 
			   os_version, app_version, status, approved_by, approved_at, 
			   registered_at, last_seen_at
		FROM devices 
		WHERE id = $1
	`
	var device models.Device
	var approvedBy *uuid.UUID
	var approvedAt, lastSeenAt *time.Time
	err := r.pool.QueryRow(ctx, query, deviceID).Scan(
		&device.ID,
		&device.CompanyID,
		&device.WarehouseID,
		&device.DeviceUUID,
		&device.Platform,
		&device.Model,
		&device.OSVersion,
		&device.AppVersion,
		&device.Status,
		&approvedBy,
		&approvedAt,
		&device.RegisteredAt,
		&lastSeenAt,
	)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	device.LastSeenAt = lastSeenAt
	device.ApprovedBy = approvedBy
	device.ApprovedAt = approvedAt
	return &device, nil
}

func (r *DeviceRepository) UpdateLastSeen(ctx context.Context, deviceID uuid.UUID) error {
	query := `UPDATE devices SET last_seen_at = $1 WHERE id = $2`
	_, err := r.pool.Exec(ctx, query, time.Now(), deviceID)
	return err
}

func (r *DeviceRepository) ApproveDevice(ctx context.Context, deviceID, approverID uuid.UUID) error {
	query := `UPDATE devices SET status = 'active', approved_by = $1, approved_at = $2 WHERE id = $3`
	_, err := r.pool.Exec(ctx, query, approverID, time.Now(), deviceID)
	return err
}

func (r *DeviceRepository) RevokeDevice(ctx context.Context, deviceID uuid.UUID) error {
	query := `UPDATE devices SET status = 'revoked' WHERE id = $1`
	_, err := r.pool.Exec(ctx, query, deviceID)
	return err
}

func (r *DeviceRepository) ListByCompany(ctx context.Context, companyID uuid.UUID) ([]models.Device, error) {
	query := `
		SELECT id, company_id, warehouse_id, device_uuid, platform, model, 
			   os_version, app_version, status, approved_by, approved_at, 
			   registered_at, last_seen_at
		FROM devices 
		WHERE company_id = $1
		ORDER BY registered_at DESC
	`
	rows, err := r.pool.Query(ctx, query, companyID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var devices []models.Device
	for rows.Next() {
		var device models.Device
		var approvedBy *uuid.UUID
		var approvedAt, lastSeenAt *time.Time
		err := rows.Scan(
			&device.ID,
			&device.CompanyID,
			&device.WarehouseID,
			&device.DeviceUUID,
			&device.Platform,
			&device.Model,
			&device.OSVersion,
			&device.AppVersion,
			&device.Status,
			&approvedBy,
			&approvedAt,
			&device.RegisteredAt,
			&lastSeenAt,
		)
		if err != nil {
			return nil, err
		}
		device.LastSeenAt = lastSeenAt
		device.ApprovedBy = approvedBy
		device.ApprovedAt = approvedAt
		devices = append(devices, device)
	}
	return devices, nil
}
