-- V35__add_trip_planned_times.sql
-- Add planned_start_at and planned_end_at to replace planned_date in trips table

ALTER TABLE trips ADD COLUMN IF NOT EXISTS planned_start_at TIMESTAMP;
ALTER TABLE trips ADD COLUMN IF NOT EXISTS planned_end_at TIMESTAMP;

-- Migrate existing data, assuming 8:00 to 17:00
UPDATE trips SET planned_start_at = planned_date::timestamp + interval '8 hours' WHERE planned_start_at IS NULL AND planned_date IS NOT NULL;
UPDATE trips SET planned_end_at = planned_date::timestamp + interval '17 hours' WHERE planned_end_at IS NULL AND planned_date IS NOT NULL;

-- Fallback for any NULLs if planned_date was NULL (though it should have been NOT NULL)
UPDATE trips SET planned_start_at = CURRENT_TIMESTAMP WHERE planned_start_at IS NULL;
UPDATE trips SET planned_end_at = CURRENT_TIMESTAMP + interval '8 hours' WHERE planned_end_at IS NULL;

-- Make them NOT NULL
ALTER TABLE trips ALTER COLUMN planned_start_at SET NOT NULL;
ALTER TABLE trips ALTER COLUMN planned_end_at SET NOT NULL;

-- Drop the old column
ALTER TABLE trips DROP COLUMN IF EXISTS planned_date;
