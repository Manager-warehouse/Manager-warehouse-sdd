-- V36: Add planned_start_at and planned_end_at to trips table
-- These columns allow dispatchers to specify a planned time window for each trip.
-- Using IF NOT EXISTS because the columns may already exist in some environments.

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'trips' AND column_name = 'planned_start_at'
    ) THEN
        ALTER TABLE trips ADD COLUMN planned_start_at TIMESTAMPTZ;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'trips' AND column_name = 'planned_end_at'
    ) THEN
        ALTER TABLE trips ADD COLUMN planned_end_at TIMESTAMPTZ;
    END IF;
END;
$$;
