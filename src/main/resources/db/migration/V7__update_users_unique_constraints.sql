-- Remove unique constraint from username column
ALTER TABLE users DROP INDEX username;

-- Add unique constraint on (provider, provider_id) combination
ALTER TABLE users ADD CONSTRAINT uk_provider_provider_id UNIQUE (provider, provider_id);

-- Add index on username for search optimization
CREATE INDEX idx_username ON users(username);

