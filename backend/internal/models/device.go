package models

import (
	"time"

	"github.com/google/uuid"
)

type Device struct {
	ID          uuid.UUID `json:"id"`
	CompanyID   uuid.UUID `json:"company_id"`
	WarehouseID uuid.UUID `json:"warehouse_id"`

	// Device identity
	DeviceUUID string  `json:"device_uuid"`
	Platform   string  `json:"platform"`
	Model      *string `json:"model,omitempty"`
	OSVersion  *string `json:"os_version,omitempty"`
	AppVersion *string `json:"app_version,omitempty"`

	// Security state
	Status     string     `json:"status"`
	ApprovedBy *uuid.UUID `json:"approved_by,omitempty"`
	ApprovedAt *time.Time `json:"approved_at,omitempty"`

	// Metadata
	RegisteredAt time.Time  `json:"registered_at"`
	LastSeenAt   *time.Time `json:"last_seen_at,omitempty"`
}

type CreateDeviceRequest struct {
	DeviceUUID  string  `json:"device_uuid" binding:"required"`
	Platform    string  `json:"platform" binding:"required"`
	WarehouseID string  `json:"warehouse_id" binding:"required"`
	Model       *string `json:"model,omitempty"`
	OSVersion   *string `json:"os_version,omitempty"`
	AppVersion  *string `json:"app_version,omitempty"`
}

type ApproveDeviceRequest struct {
	DeviceID string `json:"device_id" binding:"required"`
}
