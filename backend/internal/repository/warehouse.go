package repository

import (
	"context"
	"time"

	"github.com/google/uuid"
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
		FROM warehouses WHERE id = $1
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
	if err != nil {
		return nil, err
	}
	return &w, nil
}

func (r *WarehouseRepository) GetByCompanyID(ctx context.Context, companyID uuid.UUID) ([]Warehouse, error) {
	query := `
		SELECT id, company_id, name, address, created_at, updated_at
		FROM warehouses WHERE company_id = $1
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
