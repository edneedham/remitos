package models

import (
	"encoding/json"
	"time"

	"github.com/google/uuid"
)

type UserEventType string

const (
	UserEventLogin           UserEventType = "login"
	UserEventLogout          UserEventType = "logout"
	UserEventCreated         UserEventType = "created"
	UserEventUpdated         UserEventType = "updated"
	UserEventRoleChanged     UserEventType = "role_changed"
	UserEventSuspended       UserEventType = "suspended"
	UserEventActivated       UserEventType = "activated"
	UserEventPasswordChanged UserEventType = "password_changed"
)

type UserEvent struct {
	ID          uuid.UUID       `json:"id"`
	UserID      uuid.UUID       `json:"user_id"`
	CompanyID   uuid.UUID       `json:"company_id"`
	EventType   UserEventType   `json:"event_type"`
	PerformedBy *uuid.UUID      `json:"performed_by,omitempty"`
	Metadata    json.RawMessage `json:"metadata,omitempty"`
	IPAddress   *string         `json:"ip_address,omitempty"`
	UserAgent   *string         `json:"user_agent,omitempty"`
	CreatedAt   time.Time       `json:"created_at"`
}
