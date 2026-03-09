package models

import (
	"time"

	"github.com/google/uuid"
)

type ScanEvent struct {
	ID              uuid.UUID  `json:"id"`
	WarehouseID     uuid.UUID  `json:"warehouse_id"`
	UserID          *uuid.UUID `json:"user_id,omitempty"`
	DeviceID        *uuid.UUID `json:"device_id,omitempty"`
	Source          string     `json:"source"`
	TextExtracted   string     `json:"text_extracted,omitempty"`
	ConfidenceScore float64    `json:"confidence_score,omitempty"`
	ProcessedAt     time.Time  `json:"processed_at"`
	CreatedAt       time.Time  `json:"created_at"`
}

type ImageMetadata struct {
	ID          uuid.UUID  `json:"id"`
	ScanEventID *uuid.UUID `json:"scan_event_id,omitempty"`
	FileName    string     `json:"file_name,omitempty"`
	FileSize    int        `json:"file_size,omitempty"`
	Width       int        `json:"width,omitempty"`
	Height      int        `json:"height,omitempty"`
	Format      string     `json:"format,omitempty"`
	CreatedAt   time.Time  `json:"created_at"`
}
