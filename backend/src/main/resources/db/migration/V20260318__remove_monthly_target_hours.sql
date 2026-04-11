-- Remove monthly_target_hours from staff_work_parameters
-- This field was a soft scoring factor superseded by hard constraints min/max_hours_per_month.

ALTER TABLE staff_work_parameters DROP CONSTRAINT IF EXISTS chk_swp_monthly_target_positive;
ALTER TABLE staff_work_parameters DROP COLUMN IF EXISTS monthly_target_hours;
