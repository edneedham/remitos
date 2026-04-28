-- No-op: this data normalization is intentionally not reversed automatically.
-- Reintroducing users.role='admin' would re-create legacy state that we are deprecating.
SELECT 1;
