package models

import (
	"time"

	"github.com/google/uuid"
)

type Company struct {
	ID   uuid.UUID `json:"id"`
	Name string    `json:"name"`
	Code string    `json:"code,omitempty"`
	Cuit string    `json:"cuit,omitempty"`

	Status           string `json:"status"`
	IsVerified       bool   `json:"is_verified"`
	SubscriptionPlan string `json:"subscription_plan"`

	SubscriptionExpiresAt *time.Time `json:"subscription_expires_at,omitempty"`
	TrialEndsAt           *time.Time `json:"trial_ends_at,omitempty"`
	MaxWarehouses         *int       `json:"max_warehouses,omitempty"`
	MaxUsers              *int       `json:"max_users,omitempty"`
	MpCustomerID          *string    `json:"-"`
	MpCardID              *string    `json:"-"`

	CreatedAt  time.Time  `json:"created_at"`
	UpdatedAt  time.Time  `json:"updated_at"`
	ArchivedAt *time.Time `json:"archived_at,omitempty"`
}

// SignupTrialRequest is the public web signup with a 7-day trial and card tokenization (Mercado Pago).
type SignupTrialRequest struct {
	Email       string `json:"email" validate:"required,email"`
	Password    string `json:"password" validate:"required,min=8,max=72"`
	CompanyName string `json:"company_name" validate:"required,min=2,max=200"`
	CompanyCode string `json:"company_code" validate:"required,min=2,max=32"`
	CardToken   string `json:"card_token"` // validated in handler (required unless dev mock)
}

type Warehouse struct {
	ID        uuid.UUID `json:"id"`
	CompanyID uuid.UUID `json:"company_id"`
	Name      string    `json:"name"`
	Address   string    `json:"address,omitempty"`
	CreatedAt time.Time `json:"created_at"`
	UpdatedAt time.Time `json:"updated_at"`
}

type User struct {
	ID           uuid.UUID `json:"id"`
	CompanyID    uuid.UUID `json:"company_id"`
	Email        *string   `json:"email,omitempty"`
	PasswordHash string    `json:"-"`
	Username     *string   `json:"username,omitempty"`

	Status     string     `json:"status"`
	IsVerified bool       `json:"is_verified"`
	RoleID     *uuid.UUID `json:"role_id,omitempty"`
	Role       string     `json:"role,omitempty"`

	WarehouseID *uuid.UUID `json:"warehouse_id,omitempty"`
	CreatedAt   time.Time  `json:"created_at"`
	UpdatedAt   time.Time  `json:"updated_at"`
}

type CreateUserRequest struct {
	Email       *string `json:"email" validate:"omitempty"`
	Username    *string `json:"username" validate:"omitempty"`
	Password    string  `json:"password" validate:"required,min=8,max=72"`
	Role        string  `json:"role" validate:"omitempty"` // Deprecated: use role_id
	RoleID      string  `json:"role_id,omitempty"`
	WarehouseID string  `json:"warehouse_id,omitempty"`
	CompanyCode string  `json:"company_code" validate:"omitempty"`
	CompanyName string  `json:"company_name" validate:"omitempty"`
}
