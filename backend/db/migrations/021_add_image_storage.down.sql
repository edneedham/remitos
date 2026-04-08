-- Migration 021: Rollback image storage

DROP INDEX IF EXISTS idx_images_uploaded_by;
DROP INDEX IF EXISTS idx_images_uploaded_at;
DROP INDEX IF EXISTS idx_images_warehouse;
DROP INDEX IF EXISTS idx_images_entity;
DROP TABLE IF EXISTS images;
