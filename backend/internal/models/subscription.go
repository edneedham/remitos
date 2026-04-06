package models

import (
	"time"

	"github.com/google/uuid"
)

type SubscriptionFeatures struct {
	OfflineMode     bool `json:"offlineMode"`
	ConnectedMode   bool `json:"connectedMode"`
	PremiumFeatures bool `json:"premiumFeatures"`
}

type Subscription struct {
	ID              uuid.UUID            `json:"id"`
	UserID          uuid.UUID            `json:"userId"`
	DeviceID        *uuid.UUID           `json:"deviceId,omitempty"`
	Status          string               `json:"subscriptionStatus"`
	DeviceConnected bool                 `json:"deviceConnected"`
	Features        SubscriptionFeatures `json:"features"`
	CreatedAt       time.Time            `json:"createdAt"`
	UpdatedAt       time.Time            `json:"updatedAt"`
}
