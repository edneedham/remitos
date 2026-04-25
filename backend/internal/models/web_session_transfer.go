package models

import (
	"time"

	"github.com/google/uuid"
)

type WebSessionTransfer struct {
	ID                    uuid.UUID  `json:"id"`
	TokenHash             string     `json:"-"`
	UserID                uuid.UUID  `json:"user_id"`
	DesktopRefreshTokenID uuid.UUID  `json:"desktop_refresh_token_id"`
	PhoneRefreshTokenID   *uuid.UUID `json:"phone_refresh_token_id,omitempty"`
	ExpiresAt             time.Time  `json:"expires_at"`
	UsedAt                *time.Time `json:"used_at,omitempty"`
	CreatedAt             time.Time  `json:"created_at"`
}
