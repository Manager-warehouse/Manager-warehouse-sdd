-- Align inbound receipt counting and QC quantity fields to integer semantics.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM receipt_items
        WHERE actual_qty IS NOT NULL
          AND actual_qty <> TRUNC(actual_qty)
    ) THEN
        RAISE EXCEPTION 'Cannot convert receipt_items.actual_qty to INTEGER while fractional values exist';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM receipt_items
        WHERE over_received_qty IS NOT NULL
          AND over_received_qty <> TRUNC(over_received_qty)
    ) THEN
        RAISE EXCEPTION 'Cannot convert receipt_items.over_received_qty to INTEGER while fractional values exist';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM receipt_items
        WHERE sample_qty IS NOT NULL
          AND sample_qty <> TRUNC(sample_qty)
    ) THEN
        RAISE EXCEPTION 'Cannot convert receipt_items.sample_qty to INTEGER while fractional values exist';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM receipt_items
        WHERE sample_passed_qty IS NOT NULL
          AND sample_passed_qty <> TRUNC(sample_passed_qty)
    ) THEN
        RAISE EXCEPTION 'Cannot convert receipt_items.sample_passed_qty to INTEGER while fractional values exist';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM receipt_items
        WHERE sample_failed_qty IS NOT NULL
          AND sample_failed_qty <> TRUNC(sample_failed_qty)
    ) THEN
        RAISE EXCEPTION 'Cannot convert receipt_items.sample_failed_qty to INTEGER while fractional values exist';
    END IF;
END $$;

ALTER TABLE receipt_items
    ALTER COLUMN actual_qty TYPE INTEGER
    USING actual_qty::INTEGER,
    ALTER COLUMN over_received_qty TYPE INTEGER
    USING over_received_qty::INTEGER,
    ALTER COLUMN sample_qty TYPE INTEGER
    USING sample_qty::INTEGER,
    ALTER COLUMN sample_passed_qty TYPE INTEGER
    USING sample_passed_qty::INTEGER,
    ALTER COLUMN sample_failed_qty TYPE INTEGER
    USING sample_failed_qty::INTEGER;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'receipt_items_actual_qty_positive'
    ) THEN
        ALTER TABLE receipt_items
            ADD CONSTRAINT receipt_items_actual_qty_positive
            CHECK (actual_qty IS NULL OR actual_qty > 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'receipt_items_over_received_qty_non_negative'
    ) THEN
        ALTER TABLE receipt_items
            ADD CONSTRAINT receipt_items_over_received_qty_non_negative
            CHECK (over_received_qty IS NULL OR over_received_qty >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'receipt_items_sample_qty_non_negative'
    ) THEN
        ALTER TABLE receipt_items
            ADD CONSTRAINT receipt_items_sample_qty_non_negative
            CHECK (sample_qty IS NULL OR sample_qty >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'receipt_items_sample_passed_qty_non_negative'
    ) THEN
        ALTER TABLE receipt_items
            ADD CONSTRAINT receipt_items_sample_passed_qty_non_negative
            CHECK (sample_passed_qty IS NULL OR sample_passed_qty >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'receipt_items_sample_failed_qty_non_negative'
    ) THEN
        ALTER TABLE receipt_items
            ADD CONSTRAINT receipt_items_sample_failed_qty_non_negative
            CHECK (sample_failed_qty IS NULL OR sample_failed_qty >= 0);
    END IF;
END $$;
