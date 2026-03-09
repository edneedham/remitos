package models

import (
	"github.com/google/uuid"
)

type Role struct {
	ID   uuid.UUID `json:"id"`
	Name string    `json:"name"`
}

const (
	RoleCompanyOwner   = "company_owner"
	RoleWarehouseAdmin = "warehouse_admin"
	RoleOperator       = "operator"
	RoleReadOnly       = "read_only"
)
