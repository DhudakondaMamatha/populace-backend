-- Remove the SHIFTS onboarding stage.
-- ON DELETE CASCADE on business_onboarding_progress.stage_id handles cleanup automatically.
DELETE FROM onboarding_stages WHERE code = 'SHIFTS';
