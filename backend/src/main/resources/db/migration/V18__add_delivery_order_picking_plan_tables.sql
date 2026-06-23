ALTER TABLE delivery_order_items
    ADD COLUMN IF NOT EXISTS zone_id BIGINT REFERENCES warehouse_locations(id),
    ADD COLUMN IF NOT EXISTS planned_qty DECIMAL(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS picked_qty DECIMAL(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS qc_pass_qty DECIMAL(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS qc_fail_qty DECIMAL(10,2) NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS delivery_order_item_allocations (
    id                     BIGSERIAL PRIMARY KEY,
    do_item_id             BIGINT        NOT NULL REFERENCES delivery_order_items(id),
    inventory_id           BIGINT        NOT NULL REFERENCES inventories(id),
    batch_id               BIGINT        NOT NULL REFERENCES batches(id),
    location_id            BIGINT        NOT NULL REFERENCES warehouse_locations(id),
    zone_id                BIGINT        NOT NULL REFERENCES warehouse_locations(id),
    planned_qty            DECIMAL(10,2) NOT NULL,
    picked_qty             DECIMAL(10,2) NOT NULL DEFAULT 0,
    is_replacement         BOOLEAN       NOT NULL DEFAULT FALSE,
    replaced_allocation_id BIGINT        REFERENCES delivery_order_item_allocations(id),
    created_by             BIGINT        NOT NULL REFERENCES users(id),
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CHECK (planned_qty > 0),
    CHECK (picked_qty >= 0)
);

CREATE INDEX IF NOT EXISTS idx_do_item_allocations_do_item
    ON delivery_order_item_allocations(do_item_id);

CREATE INDEX IF NOT EXISTS idx_do_item_allocations_inventory
    ON delivery_order_item_allocations(inventory_id);

CREATE TABLE IF NOT EXISTS delivery_order_item_return_to_bin_records (
    id                   BIGSERIAL PRIMARY KEY,
    do_item_id           BIGINT        NOT NULL REFERENCES delivery_order_items(id),
    allocation_id        BIGINT        NOT NULL REFERENCES delivery_order_item_allocations(id),
    product_id           BIGINT        NOT NULL REFERENCES products(id),
    batch_id             BIGINT        NOT NULL REFERENCES batches(id),
    original_location_id BIGINT        NOT NULL REFERENCES warehouse_locations(id),
    original_zone_id     BIGINT        NOT NULL REFERENCES warehouse_locations(id),
    source_location_id   BIGINT        REFERENCES warehouse_locations(id),
    returned_qty         DECIMAL(10,2) NOT NULL,
    reason               TEXT,
    created_by           BIGINT        NOT NULL REFERENCES users(id),
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CHECK (returned_qty > 0)
);

CREATE INDEX IF NOT EXISTS idx_return_to_bin_allocation
    ON delivery_order_item_return_to_bin_records(allocation_id);

CREATE TABLE IF NOT EXISTS delivery_order_item_replacements (
    id                      BIGSERIAL PRIMARY KEY,
    do_item_id              BIGINT        NOT NULL REFERENCES delivery_order_items(id),
    failed_inventory_id     BIGINT        NOT NULL REFERENCES inventories(id),
    replacement_inventory_id BIGINT       NOT NULL REFERENCES inventories(id),
    failed_batch_id         BIGINT        NOT NULL REFERENCES batches(id),
    failed_location_id      BIGINT        NOT NULL REFERENCES warehouse_locations(id),
    replacement_batch_id    BIGINT        NOT NULL REFERENCES batches(id),
    replacement_location_id BIGINT        NOT NULL REFERENCES warehouse_locations(id),
    quantity                DECIMAL(10,2) NOT NULL,
    reason                  TEXT          NOT NULL,
    created_by              BIGINT        NOT NULL REFERENCES users(id),
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CHECK (quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_do_item_replacements_do_item
    ON delivery_order_item_replacements(do_item_id);

DO $$
DECLARE
    constraint_name text;
BEGIN
    FOR constraint_name IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
        WHERE nsp.nspname = current_schema()
          AND rel.relname = 'audit_logs'
          AND con.contype = 'c'
          AND pg_get_constraintdef(con.oid) ILIKE '%action%'
    LOOP
        EXECUTE format('ALTER TABLE audit_logs DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END $$;

ALTER TABLE audit_logs
    ADD CONSTRAINT chk_audit_logs_action
        CHECK (action IN (
            'LOGIN', 'LOGOUT', 'CREATE', 'UPDATE', 'STATUS_CHANGE',
            'APPROVE', 'REJECT', 'CANCEL', 'SOFT_DELETE', 'ASSIGN',
            'UNASSIGN', 'UPLOAD_POD', 'REQUEST_OTP', 'CONFIRM_DELIVERY',
            'RESET_DELIVERY_OTP', 'FAIL_DELIVERY', 'COMPLETE_TRIP',
            'PICKING_PLAN_SAVE', 'PICKED_GOODS_RETURN_TO_BIN',
            'PICKING_REPLACEMENT_SAVE'
        ));
