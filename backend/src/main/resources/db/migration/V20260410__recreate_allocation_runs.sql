-- Recreate allocation_runs table (dropped in V20260328).
-- Required by AllocationEngine.saveAllocationRun().

CREATE TABLE IF NOT EXISTS allocation_runs (
    id              BIGSERIAL PRIMARY KEY,
    run_id          VARCHAR(255) NOT NULL UNIQUE,
    business_id     BIGINT NOT NULL,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    total_shifts    INTEGER,
    shifts_filled   INTEGER,
    shifts_partial  INTEGER,
    shifts_unfilled INTEGER,
    total_allocations INTEGER,
    status          VARCHAR(50) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at    TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_allocation_runs_business_id ON allocation_runs(business_id);
CREATE INDEX IF NOT EXISTS idx_allocation_runs_run_id ON allocation_runs(run_id);
