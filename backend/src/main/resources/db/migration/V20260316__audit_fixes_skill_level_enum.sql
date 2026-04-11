-- Migration: Convert staff_roles.skill_level from VARCHAR(2) to native skill_level enum
-- Aligns live DB with schema.sql which uses the native enum type

-- Step 1: Create the skill_level enum if it does not exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'skill_level') THEN
        CREATE TYPE skill_level AS ENUM ('L1', 'L2', 'L3');
    END IF;
END$$;

-- Step 2: Convert the column from VARCHAR to the native enum
ALTER TABLE staff_roles
ALTER COLUMN skill_level TYPE skill_level USING skill_level::skill_level;

-- Step 3: Set the default to use the enum value
ALTER TABLE staff_roles
ALTER COLUMN skill_level SET DEFAULT 'L2'::skill_level;

-- Step 4: Drop the old skill_level_enum type if it exists (was never applied to column)
DROP TYPE IF EXISTS skill_level_enum CASCADE;
