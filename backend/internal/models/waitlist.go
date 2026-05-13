package models

import (
	"time"

	"github.com/google/uuid"
)

// WaitlistRequest is the body for POST /public/waitlist.
type WaitlistRequest struct {
	Email       string `json:"email" validate:"required,email,max=254"`
	FullName    string `json:"full_name" validate:"required,max=200"`
	CompanyName string `json:"company_name" validate:"required,max=200"`
	Source      string `json:"source" validate:"omitempty,max=500"`
	// DeliveryNotesPerDayBand: lt10 | lt50 | lt200 | gt200 (average remitos/día).
	DeliveryNotesPerDayBand string `json:"delivery_notes_per_day_band" validate:"required,oneof=lt10 lt50 lt200 gt200"`
	// ProcessingMode: digital | manual.
	ProcessingMode     string `json:"processing_mode" validate:"required,oneof=digital manual"`
	DigitalApplication string `json:"digital_application" validate:"omitempty,max=200"`
	// LogisticsPainPoints: optional free text (Argentina logistics context).
	LogisticsPainPoints string `json:"logistics_pain_points" validate:"omitempty,max=2000"`
	// WarehouseCount pointer so JSON 0 is valid (validator `required` rejects plain int 0).
	WarehouseCount *int `json:"warehouse_count" validate:"required,gte=0,lte=50000"`
}

// WaitlistEntry is a persisted waitlist row (email_normalized is unique).
type WaitlistEntry struct {
	ID                      uuid.UUID `json:"id"`
	EmailNormalized         string    `json:"email_normalized"`
	FullName                *string   `json:"full_name,omitempty"`
	CompanyName             *string   `json:"company_name,omitempty"`
	Source                  *string   `json:"source,omitempty"`
	DeliveryNotesPerDayBand *string   `json:"delivery_notes_per_day_band,omitempty"`
	ProcessingMode          *string   `json:"processing_mode,omitempty"`
	DigitalApplication      *string   `json:"digital_application,omitempty"`
	LogisticsPainPoints     *string   `json:"logistics_pain_points,omitempty"`
	WarehouseCount          *int      `json:"warehouse_count,omitempty"`
	CreatedAt               time.Time `json:"created_at"`
}
