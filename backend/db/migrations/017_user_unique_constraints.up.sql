-- Ensure users have unique identifiers and username where provided
-- Add external_id for public unique identifier (for sharing without exposing internal UUID)
ALTER TABLE users ADD COLUMN IF NOT EXISTS external_id VARCHAR(50) UNIQUE;

-- Ensure username is unique within company when not null
-- This handles the case where we want unique usernames but also allow null
