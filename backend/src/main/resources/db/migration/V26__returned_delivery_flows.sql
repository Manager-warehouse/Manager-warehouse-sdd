CREATE TABLE returned_delivery_flows (
    id BIGSERIAL PRIMARY KEY,
    do_id BIGINT NOT NULL UNIQUE REFERENCES delivery_orders(id),
    status VARCHAR(30) NOT NULL,
    counted_by_staff_id BIGINT REFERENCES users(id),
    approved_by_storekeeper_id BIGINT REFERENCES users(id),
    putaway_planned_by_storekeeper_id BIGINT REFERENCES users(id),
    putaway_completed_by_staff_id BIGINT REFERENCES users(id),
    notes TEXT,
    approval_notes TEXT,
    putaway_notes TEXT,
    completion_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT returned_delivery_flows_status_check CHECK (status IN (
        'COUNT_QC_SUBMITTED',
        'APPROVED',
        'PUTAWAY_PLANNED',
        'PUTAWAY_COMPLETED'
    ))
);

CREATE TABLE returned_delivery_flow_items (
    id BIGSERIAL PRIMARY KEY,
    flow_id BIGINT NOT NULL REFERENCES returned_delivery_flows(id) ON DELETE CASCADE,
    do_item_id BIGINT NOT NULL REFERENCES delivery_order_items(id),
    product_id BIGINT NOT NULL REFERENCES products(id),
    batch_id BIGINT NOT NULL REFERENCES batches(id),
    expected_qty NUMERIC(10, 2) NOT NULL CHECK (expected_qty >= 0),
    counted_qty NUMERIC(10, 2) NOT NULL CHECK (counted_qty >= 0),
    quality_result VARCHAR(20) NOT NULL CHECK (quality_result IN ('PASSED', 'FAILED')),
    quality_reason TEXT,
    destination_location_id BIGINT REFERENCES warehouse_locations(id),
    planned_qty NUMERIC(10, 2) CHECK (planned_qty >= 0),
    putaway_completed_qty NUMERIC(10, 2) CHECK (putaway_completed_qty >= 0),
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT returned_delivery_flow_items_line_unique UNIQUE (flow_id, do_item_id, batch_id)
);

CREATE INDEX idx_returned_delivery_flow_items_flow_id ON returned_delivery_flow_items(flow_id);
CREATE INDEX idx_returned_delivery_flows_do_id ON returned_delivery_flows(do_id);
