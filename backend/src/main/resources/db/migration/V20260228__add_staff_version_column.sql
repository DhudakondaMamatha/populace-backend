-- Add version column for optimistic locking on staff_members table
-- This column is managed by JPA @Version annotation

ALTER TABLE staff_members ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- Initialize version to 0 for existing records
UPDATE staff_members SET version = 0 WHERE version IS NULL;

-- Make version NOT NULL after initialization
ALTER TABLE staff_members ALTER COLUMN version SET NOT NULL;

COMMENT ON COLUMN staff_members.version IS 'Optimistic locking version - auto-incremented by JPA on each update';
