-- Migration 022: Rollback sync tables

DROP TABLE IF EXISTS sync_metadata;
DROP TABLE IF EXISTS outbound_line_edit_history;
DROP TABLE IF EXISTS outbound_line_status_history;
DROP TABLE IF EXISTS outbound_lines;
DROP TABLE IF EXISTS outbound_lists;
DROP TABLE IF EXISTS inbound_notes;

ALTER TABLE documents DROP COLUMN IF EXISTS cloud_id;
ALTER TABLE documents DROP COLUMN IF EXISTS company_id;