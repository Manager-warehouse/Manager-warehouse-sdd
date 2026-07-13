-- V9__inter_warehouse_transfer_hardening.sql
-- Additive migration: production hardening for Spec 005 Inter-Warehouse Transfer
-- IMPORTANT: DO NOT edit V1–V7. All schema fixes go here as additive ALTER statements.

-- ============================================================
-- T008: Fix CHECK constraint to include REJECTED and QUARANTINED
-- The original constraint in V1 may not include these statuses.
-- ============================================================
ALTER TABLE inter_warehouse_transfers
    DROP CONSTRAINT IF EXISTS inter_warehouse_transfers_status_check;

ALTER TABLE inter_warehouse_transfers
    ADD CONSTRAINT inter_warehouse_transfers_status_check
    CHECK (status IN (
        'NEW', 'APPROVED', 'REJECTED', 'IN_TRANSIT',
        'COMPLETED', 'COMPLETED_WITH_DISCREPANCY',
        'CANCELLED', 'QUARANTINED', 'RETURNED'
    ));

-- ============================================================
-- T009: Make planned batch_id nullable in transfer items
-- batch is assigned during approval/FIFO allocation, not at creation time
-- ============================================================
ALTER TABLE inter_warehouse_transfer_items
    ALTER COLUMN batch_id DROP NOT NULL;

-- ============================================================
-- T010: Add optimistic locking version columns
-- ============================================================
ALTER TABLE inter_warehouse_transfers
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE inter_warehouse_transfer_items
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE transfer_requests
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- ============================================================
-- T011: Add outbound QC, load handover, arrival, and handover fields
-- ============================================================

-- Outbound QC at source warehouse
ALTER TABLE inter_warehouse_transfers
    ADD COLUMN IF NOT EXISTS outbound_qc_passed    BOOLEAN,
    ADD COLUMN IF NOT EXISTS outbound_qc_note      TEXT,
    ADD COLUMN IF NOT EXISTS outbound_qc_photo_ref TEXT,
    ADD COLUMN IF NOT EXISTS outbound_qc_by        BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS outbound_qc_at        TIMESTAMPTZ;

-- Load handover: source storekeeper hands goods to driver
ALTER TABLE inter_warehouse_transfers
    ADD COLUMN IF NOT EXISTS load_handover_photo_ref TEXT,
    ADD COLUMN IF NOT EXISTS load_handover_by        BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS load_handover_at        TIMESTAMPTZ;

-- Driver arrival at destination warehouse
ALTER TABLE inter_warehouse_transfers
    ADD COLUMN IF NOT EXISTS driver_arrived_at      TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS arrival_handover_at    TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS arrival_handover_by    BIGINT REFERENCES users(id);

-- Return leg: driver confirms return departure + arrival at source
ALTER TABLE inter_warehouse_transfers
    ADD COLUMN IF NOT EXISTS return_departed_at     TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS return_arrived_at      TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS return_arrival_handover_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS return_arrival_handover_by BIGINT REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS return_photo_ref       TEXT;

-- ============================================================
-- T012: Wrong-SKU report and report items tables
-- ============================================================
CREATE TABLE IF NOT EXISTS wrong_sku_reports (
    id                  BIGSERIAL PRIMARY KEY,
    transfer_id         BIGINT NOT NULL REFERENCES inter_warehouse_transfers(id),
    status              VARCHAR(30) NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    reported_by         BIGINT NOT NULL REFERENCES users(id),
    reported_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    manager_decision_by BIGINT REFERENCES users(id),
    manager_decision_at TIMESTAMPTZ,
    manager_note        TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS wrong_sku_report_items (
    id                  BIGSERIAL PRIMARY KEY,
    report_id           BIGINT NOT NULL REFERENCES wrong_sku_reports(id),
    transfer_item_id    BIGINT REFERENCES inter_warehouse_transfer_items(id),
    expected_product_id BIGINT NOT NULL REFERENCES products(id),
    actual_product_id   BIGINT NOT NULL REFERENCES products(id),
    affected_qty        NUMERIC(10,2) NOT NULL,
    reason              TEXT NOT NULL,
    photo_ref           TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_wrong_sku_reports_transfer
    ON wrong_sku_reports(transfer_id);

-- ============================================================
-- T013: Add trip weight/volume total columns (calculated, not hardcoded 0)
-- ============================================================
ALTER TABLE trips
    ADD COLUMN IF NOT EXISTS calculated_weight_kg NUMERIC(10,2),
    ADD COLUMN IF NOT EXISTS calculated_volume_m3 NUMERIC(10,4);

-- ============================================================
-- T014: Discrepancy incidents and hold tracking
-- ============================================================
CREATE TABLE IF NOT EXISTS discrepancy_incidents (
    id              BIGSERIAL PRIMARY KEY,
    transfer_id     BIGINT NOT NULL REFERENCES inter_warehouse_transfers(id),
    product_id      BIGINT NOT NULL REFERENCES products(id),
    incident_type   VARCHAR(30) NOT NULL
                        CHECK (incident_type IN ('SHORTAGE', 'OVER_RECEIPT')),
    quantity        NUMERIC(10,2) NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'OPEN'
                        CHECK (status IN ('OPEN', 'RESOLVED_ACCEPTED', 'RESOLVED_RETURNED')),
    resolution_note TEXT,
    resolved_by     BIGINT REFERENCES users(id),
    resolved_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Discrepancy hold: physical over-receipt held in special location
CREATE TABLE IF NOT EXISTS discrepancy_hold_entries (
    id                  BIGSERIAL PRIMARY KEY,
    incident_id         BIGINT NOT NULL REFERENCES discrepancy_incidents(id),
    warehouse_id        BIGINT NOT NULL REFERENCES warehouses(id),
    product_id          BIGINT NOT NULL REFERENCES products(id),
    batch_id            BIGINT REFERENCES batches(id),
    hold_qty            NUMERIC(10,2) NOT NULL,
    hold_location_id    BIGINT REFERENCES warehouse_locations(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_discrepancy_incidents_transfer
    ON discrepancy_incidents(transfer_id);
