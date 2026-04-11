-- Migration: Unify Proficiency and Competence into SkillLevel
-- This migration adds skill_level column and populates from proficiency_level
-- Does NOT drop old columns - that happens in future cleanup migration

-- Step 1: Add skill_level column to staff_roles
ALTER TABLE staff_roles
ADD COLUMN skill_level VARCHAR(2);

-- Step 2: Populate skill_level from proficiency_level
UPDATE staff_roles
SET skill_level = CASE
    WHEN proficiency_level = 'trainee' THEN 'L1'
    WHEN proficiency_level = 'competent' THEN 'L2'
    WHEN proficiency_level = 'expert' THEN 'L3'
    ELSE 'L2'
END
WHERE skill_level IS NULL;

-- Step 3: Set default for new records
ALTER TABLE staff_roles
ALTER COLUMN skill_level SET DEFAULT 'L2';

-- Step 4: Make skill_level NOT NULL after population
ALTER TABLE staff_roles
ALTER COLUMN skill_level SET NOT NULL;

-- Step 5: Create enum type for skill_level (PostgreSQL specific)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'skill_level_enum') THEN
        CREATE TYPE skill_level_enum AS ENUM ('L1', 'L2', 'L3');
    END IF;
END$$;

-- Step 6: Add index for skill_level queries
CREATE INDEX IF NOT EXISTS idx_staff_roles_skill_level
ON staff_roles(skill_level);

-- Note: The following columns are deprecated but NOT dropped in this migration:
-- - staff_roles.proficiency_level (will be dropped in cleanup migration)
-- - staff_roles.competence_level (will be dropped in cleanup migration)
-- The staff_competence_levels table will also be dropped in cleanup migration
