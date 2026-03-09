package models

import (
	"encoding/json"
	"time"

	"github.com/google/uuid"
)

type DeviceEventType string

const (
	DeviceEventRegistered DeviceEventType = "registered"
	DeviceEventApproved   DeviceEventType = "approved"
	DeviceEventRevoked    DeviceEventType = "revoked"
	DeviceEventReassigned DeviceEventType = "reassigned"
)

type DeviceEvent struct {
	ID          uuid.UUID       `json:"id"`
	DeviceID    uuid.UUID       `json:"device_id"`
	CompanyID   uuid.UUID       `json:"company_id"`
	WarehouseID uuid.UUID       `json:"warehouse_id"`
	EventType   DeviceEventType `json:"event_type"`
	PerformedBy *uuid.UUID      `json:"performed_by,omitempty"`
	Metadata    json.RawMessage `json:"metadata,omitempty"`
	CreatedAt   time.Time       `json:"created_at"`
}

type CreateDeviceEventRequest struct {
	DeviceID    string          `json:"device_id" binding:"required"`
	CompanyID   string          `json:"company_id" binding:"required"`
	WarehouseID string          `json:"warehouse_id" binding:"required"`
	EventType   DeviceEventType `json:"event_type" binding:"required"`
	Metadata    json.RawMessage `json:"metadata,omitempty"`
}
