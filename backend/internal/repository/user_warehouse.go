package repository

import (
	"context"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

type UserWarehouseRepository struct {
	pool *pgxpool.Pool
}

func NewUserWarehouseRepository(pool *pgxpool.Pool) *UserWarehouseRepository {
	return &UserWarehouseRepository{pool: pool}
}

func (r *UserWarehouseRepository) HasWarehouseAccess(ctx context.Context, userID, warehouseID uuid.UUID) (bool, error) {
	query := `
		SELECT EXISTS(
			SELECT 1 FROM user_warehouses 
			WHERE user_id = $1 AND warehouse_id = $2
		)
	`
	var hasAccess bool
	err := r.pool.QueryRow(ctx, query, userID, warehouseID).Scan(&hasAccess)
	if err != nil {
		return false, err
	}
	return hasAccess, nil
}

func (r *UserWarehouseRepository) GetUserWarehouseRoles(ctx context.Context, userID uuid.UUID) ([]UserWarehouseRole, error) {
	query := `
		SELECT uw.warehouse_id, uw.role, w.name
		FROM user_warehouses uw
		JOIN warehouses w ON w.id = uw.warehouse_id
		WHERE uw.user_id = $1
	`
	rows, err := r.pool.Query(ctx, query, userID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var roles []UserWarehouseRole
	for rows.Next() {
		var role UserWarehouseRole
		if err := rows.Scan(&role.WarehouseID, &role.Role, &role.WarehouseName); err != nil {
			return nil, err
		}
		roles = append(roles, role)
	}
	return roles, nil
}

type UserWarehouseRole struct {
	WarehouseID   uuid.UUID
	Role          string
	WarehouseName string
}

func (r *UserWarehouseRepository) Create(ctx context.Context, userID, warehouseID uuid.UUID, role string) error {
	query := `
		INSERT INTO user_warehouses (id, user_id, warehouse_id, role, company_id)
		SELECT gen_random_uuid(), $1, $2, $3, company_id FROM users WHERE id = $1
		ON CONFLICT (user_id, warehouse_id) DO UPDATE SET role = EXCLUDED.role
	`
	_, err := r.pool.Exec(ctx, query, userID, warehouseID, role)
	return err
}

func (r *UserWarehouseRepository) GetByUserAndWarehouse(ctx context.Context, userID, warehouseID uuid.UUID) (string, error) {
	query := `SELECT role FROM user_warehouses WHERE user_id = $1 AND warehouse_id = $2`
	var role string
	err := r.pool.QueryRow(ctx, query, userID, warehouseID).Scan(&role)
	if err == pgx.ErrNoRows {
		return "", nil
	}
	if err != nil {
		return "", err
	}
	return role, nil
}
