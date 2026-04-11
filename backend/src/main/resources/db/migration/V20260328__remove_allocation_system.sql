-- Remove allocation system tables and allocation-specific columns from time_blocks.

-- 1. Drop tables with FK dependencies first
DROP TABLE IF EXISTS allocation_flags CASCADE;
DROP TABLE IF EXISTS break_scheduling_warnings CASCADE;
DROP TABLE IF EXISTS allocation_locks CASCADE;
DROP TABLE IF EXISTS break_overrides CASCADE;
DROP TABLE IF EXISTS allocation_runs CASCADE;

-- 2. Clean up allocation-specific columns from time_blocks
ALTER TABLE time_blocks DROP COLUMN IF EXISTS allocation_run_id;
ALTER TABLE time_blocks DROP COLUMN IF EXISTS break_trigger_type;

-- 3. Drop allocation-related enum types
DROP TYPE IF EXISTS run_status CASCADE;
DROP TYPE IF EXISTS run_type CASCADE;
DROP TYPE IF EXISTS flag_type CASCADE;
DROP TYPE IF EXISTS severity_level CASCADE;
DROP TYPE IF EXISTS break_override_type CASCADE;
DROP TYPE IF EXISTS break_warning_severity CASCADE;
DROP TYPE IF EXISTS break_trigger_type CASCADE;
DROP TYPE IF EXISTS allocation_readiness CASCADE;
