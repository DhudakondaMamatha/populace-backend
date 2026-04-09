ALTER TABLE time_blocks ALTER COLUMN allocation_run_id DROP NOT NULL;
ALTER TABLE time_blocks ADD COLUMN override_reason TEXT;
