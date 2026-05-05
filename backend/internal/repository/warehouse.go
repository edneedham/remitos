package repository

import (
	"context"
	"errors"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

type WarehouseRepository struct {
	pool *pgxpool.Pool
}

func NewWarehouseRepository(pool *pgxpool.Pool) *WarehouseRepository {
	return &WarehouseRepository{pool: pool}
}

func (r *WarehouseRepository) Create(ctx context.Context, warehouse *Warehouse) error {
	query := `
		INSERT INTO warehouses (id, company_id, name, address, created_at, updated_at)
		VALUES ($1, $2, $3, $4, $5, $6)
	`
	_, err := r.pool.Exec(ctx, query,
		warehouse.ID,
		warehouse.CompanyID,
		warehouse.Name,
		warehouse.Address,
		warehouse.CreatedAt,
		warehouse.UpdatedAt,
	)
	return err
}

func (r *WarehouseRepository) GetByID(ctx context.Context, id uuid.UUID) (*Warehouse, error) {
	query := `
		SELECT id, company_id, name, address, created_at, updated_at
		FROM warehouses WHERE id = $1 AND archived_at IS NULL
	`
	var w Warehouse
	err := r.pool.QueryRow(ctx, query, id).Scan(
		&w.ID,
		&w.CompanyID,
		&w.Name,
		&w.Address,
		&w.CreatedAt,
		&w.UpdatedAt,
	)
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &w, nil
}

// CountByCompanyID returns the number of non-archived warehouses for a company.
func (r *WarehouseRepository) CountByCompanyID(ctx context.Context, companyID uuid.UUID) (int64, error) {
	query := `SELECT COUNT(*) FROM warehouses WHERE company_id = $1 AND archived_at IS NULL`
	var n int64
	err := r.pool.QueryRow(ctx, query, companyID).Scan(&n)
	if err != nil {
		return 0, err
	}
	return n, nil
}

// GetByCompanyID returns non-archived warehouses for a company.
func (r *WarehouseRepository) GetByCompanyID(ctx context.Context, companyID uuid.UUID) ([]Warehouse, error) {
	query := `
		SELECT id, company_id, name, address, created_at, updated_at
		FROM warehouses
		WHERE company_id = $1 AND archived_at IS NULL
		ORDER BY name ASC
	`
	rows, err := r.pool.Query(ctx, query, companyID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var warehouses []Warehouse
	for rows.Next() {
		var w Warehouse
		if err := rows.Scan(&w.ID, &w.CompanyID, &w.Name, &w.Address, &w.CreatedAt, &w.UpdatedAt); err != nil {
			return nil, err
		}
		warehouses = append(warehouses, w)
	}
	return warehouses, nil
}

// Update renames a warehouse and/or changes its address. Returns false if the row was
// not found or already archived.
func (r *WarehouseRepository) Update(ctx context.Context, id, companyID uuid.UUID, name, address string) (bool, error) {
	query := `
		UPDATE warehouses
		SET name = $3, address = $4, updated_at = NOW()
		WHERE id = $1 AND company_id = $2 AND archived_at IS NULL
	`
	tag, err := r.pool.Exec(ctx, query, id, companyID, name, address)
	if err != nil {
		return false, err
	}
	return tag.RowsAffected() > 0, nil
}

// Archive soft-deletes a warehouse. Returns false if the row was not found or was
// already archived.
func (r *WarehouseRepository) Archive(ctx context.Context, id, companyID uuid.UUID) (bool, error) {
	query := `
		UPDATE warehouses
		SET archived_at = NOW(), updated_at = NOW()
		WHERE id = $1 AND company_id = $2 AND archived_at IS NULL
	`
	tag, err := r.pool.Exec(ctx, query, id, companyID)
	if err != nil {
		return false, err
	}
	return tag.RowsAffected() > 0, nil
}

// CountActiveDevicesByWarehouseID returns devices that are currently active for the warehouse
// (used to refuse archiving a warehouse that still has live devices).
func (r *WarehouseRepository) CountActiveDevicesByWarehouseID(ctx context.Context, warehouseID uuid.UUID) (int64, error) {
	query := `SELECT COUNT(*) FROM devices WHERE warehouse_id = $1 AND status = 'active'`
	var n int64
	err := r.pool.QueryRow(ctx, query, warehouseID).Scan(&n)
	if err != nil {
		return 0, err
	}
	return n, nil
}

type Warehouse struct {
	ID        uuid.UUID `json:"id"`
	CompanyID uuid.UUID `json:"company_id"`
	Name      string    `json:"name"`
	Address   string    `json:"address,omitempty"`
	CreatedAt time.Time `json:"created_at"`
	UpdatedAt time.Time `json:"updated_at"`
}

type Company struct {
	ID   uuid.UUID `json:"id"`
	Code string    `json:"code"`
	Name string    `json:"name"`
}

func (r *WarehouseRepository) GetCompanyByCode(ctx context.Context, code string) (*Company, error) {
	query := `
		SELECT id, code, name FROM companies WHERE code = $1
	`
	var c Company
	err := r.pool.QueryRow(ctx, query, code).Scan(&c.ID, &c.Code, &c.Name)
	if err != nil {
		return nil, err
	}
	return &c, nil
}
