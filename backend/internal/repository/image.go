package repository

import (
	"context"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
)

// Image represents an image stored in Google Cloud Storage
type Image struct {
	ID           string    `json:"id"`
	GcsPath      string    `json:"gcs_path"`
	ContentType  string    `json:"content_type"`
	FileSize     int64     `json:"file_size"`
	EntityType   string    `json:"entity_type"`
	EntityID     int64     `json:"entity_id"`
	WarehouseID  *string   `json:"warehouse_id,omitempty"`
	UploadedBy   *string   `json:"uploaded_by,omitempty"`
	UploadedAt   time.Time `json:"uploaded_at"`
	StorageClass string    `json:"storage_class"`
}

// ImageRepository handles database operations for images
type ImageRepository struct {
	pool *pgxpool.Pool
}

// NewImageRepository creates a new ImageRepository
func NewImageRepository(pool *pgxpool.Pool) *ImageRepository {
	return &ImageRepository{pool: pool}
}

// Create inserts a new image record into the database
func (r *ImageRepository) Create(ctx context.Context, image *Image) error {
	query := `
		INSERT INTO images (id, gcs_path, content_type, file_size, entity_type, entity_id, warehouse_id, uploaded_by, uploaded_at, storage_class)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
		ON CONFLICT (entity_type, entity_id) DO UPDATE SET
			gcs_path = EXCLUDED.gcs_path,
			content_type = EXCLUDED.content_type,
			file_size = EXCLUDED.file_size,
			warehouse_id = EXCLUDED.warehouse_id,
			uploaded_by = EXCLUDED.uploaded_by,
			uploaded_at = EXCLUDED.uploaded_at,
			storage_class = EXCLUDED.storage_class
		RETURNING id
	`

	err := r.pool.QueryRow(ctx, query,
		image.ID,
		image.GcsPath,
		image.ContentType,
		image.FileSize,
		image.EntityType,
		image.EntityID,
		image.WarehouseID,
		image.UploadedBy,
		image.UploadedAt,
		image.StorageClass,
	).Scan(&image.ID)

	return err
}

// GetByEntity retrieves an image by entity type and ID
func (r *ImageRepository) GetByEntity(ctx context.Context, entityType string, entityID int64) (*Image, error) {
	query := `
		SELECT id, gcs_path, content_type, file_size, entity_type, entity_id, warehouse_id, uploaded_by, uploaded_at, storage_class
		FROM images
		WHERE entity_type = $1 AND entity_id = $2
	`

	image := &Image{}
	err := r.pool.QueryRow(ctx, query, entityType, entityID).Scan(
		&image.ID,
		&image.GcsPath,
		&image.ContentType,
		&image.FileSize,
		&image.EntityType,
		&image.EntityID,
		&image.WarehouseID,
		&image.UploadedBy,
		&image.UploadedAt,
		&image.StorageClass,
	)

	return image, err
}

// GetByID retrieves an image by its ID
func (r *ImageRepository) GetByID(ctx context.Context, id string) (*Image, error) {
	query := `
		SELECT id, gcs_path, content_type, file_size, entity_type, entity_id, warehouse_id, uploaded_by, uploaded_at, storage_class
		FROM images
		WHERE id = $1
	`

	image := &Image{}
	err := r.pool.QueryRow(ctx, query, id).Scan(
		&image.ID,
		&image.GcsPath,
		&image.ContentType,
		&image.FileSize,
		&image.EntityType,
		&image.EntityID,
		&image.WarehouseID,
		&image.UploadedBy,
		&image.UploadedAt,
		&image.StorageClass,
	)

	return image, err
}

// Delete removes an image record from the database
func (r *ImageRepository) Delete(ctx context.Context, id string) error {
	query := `DELETE FROM images WHERE id = $1`
	_, err := r.pool.Exec(ctx, query, id)
	return err
}
