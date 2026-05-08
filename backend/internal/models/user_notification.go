package models

import (
	"encoding/json"
	"time"

	"github.com/google/uuid"
)

// UserNotificationKind identifies notification templates for routing and analytics.
const (
	UserNotificationKindSignupWelcome UserNotificationKind = "signup_welcome"
)

type UserNotificationKind string

type UserNotification struct {
	ID        uuid.UUID           `json:"id"`
	UserID    uuid.UUID           `json:"-"`
	CompanyID uuid.UUID           `json:"-"`
	Kind      UserNotificationKind `json:"kind"`
	Title     string              `json:"title"`
	Body      *string             `json:"body,omitempty"`
	ActionURL *string             `json:"action_url,omitempty"`
	ReadAt    *time.Time          `json:"read_at,omitempty"`
	Metadata  json.RawMessage     `json:"metadata,omitempty"`
	CreatedAt time.Time           `json:"created_at"`
}
