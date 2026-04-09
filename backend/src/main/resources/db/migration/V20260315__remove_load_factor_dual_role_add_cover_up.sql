-- Remove loadFactor and dualRole columns, add cover-up support

-- 1. Drop constraints that reference columns being removed
ALTER TABLE time_blocks DROP CONSTRAINT IF EXISTS chk_time_blocks_valid_dual_role;

-- 2. Drop columns from time_blocks
ALTER TABLE time_blocks DROP COLUMN IF EXISTS load_factor;
ALTER TABLE time_blocks DROP COLUMN IF EXISTS is_dual_role;
ALTER TABLE time_blocks DROP COLUMN IF EXISTS primary_block_id;

-- 3. Add covering_block_id to time_blocks (links a cover-up block to the work block it covers)
ALTER TABLE time_blocks ADD COLUMN covering_block_id BIGINT NULL;
ALTER TABLE time_blocks ADD CONSTRAINT fk_time_blocks_covering_block
    FOREIGN KEY (covering_block_id) REFERENCES time_blocks(id) ON DELETE SET NULL;

-- 4. Add cover_up to block_type enum
ALTER TYPE block_type ADD VALUE IF NOT EXISTS 'cover_up';

-- 5. Drop load_factor from roles
ALTER TABLE roles DROP COLUMN IF EXISTS load_factor;

-- 6. Drop dual_role_enabled from sites
ALTER TABLE sites DROP COLUMN IF EXISTS dual_role_enabled;

-- 7. Replace dual_role_count with cover_up_count in allocation_runs
ALTER TABLE allocation_runs DROP COLUMN IF EXISTS dual_role_count;
ALTER TABLE allocation_runs ADD COLUMN cover_up_count INT DEFAULT 0 CHECK (cover_up_count >= 0);

-- 8. Add index for covering_block_id lookups
CREATE INDEX idx_time_blocks_covering_block ON time_blocks(covering_block_id) WHERE covering_block_id IS NOT NULL;
