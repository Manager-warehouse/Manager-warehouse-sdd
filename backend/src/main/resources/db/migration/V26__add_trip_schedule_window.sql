ALTER TABLE trips
    ADD COLUMN planned_start_at TIMESTAMP,
    ADD COLUMN planned_end_at TIMESTAMP;

UPDATE trips
SET planned_start_at = COALESCE(planned_start_at, planned_date::timestamp + TIME '08:00:00'),
    planned_end_at = COALESCE(planned_end_at, planned_date::timestamp + TIME '17:00:00');

ALTER TABLE trips
    ALTER COLUMN planned_start_at SET NOT NULL,
    ALTER COLUMN planned_end_at SET NOT NULL;

ALTER TABLE trips
    ADD CONSTRAINT chk_trips_planned_window
        CHECK (planned_end_at > planned_start_at);

CREATE INDEX idx_trips_driver_schedule
    ON trips (driver_id, planned_start_at, planned_end_at);

CREATE INDEX idx_trips_vehicle_schedule
    ON trips (vehicle_id, planned_start_at, planned_end_at);
