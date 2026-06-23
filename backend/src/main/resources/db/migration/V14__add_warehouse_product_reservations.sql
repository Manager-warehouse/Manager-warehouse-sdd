CREATE TABLE IF NOT EXISTS warehouse_product_reservations (
    id           BIGSERIAL     PRIMARY KEY,
    warehouse_id BIGINT        NOT NULL REFERENCES warehouses(id),
    product_id   BIGINT        NOT NULL REFERENCES products(id),
    reserved_qty DECIMAL(10,2) NOT NULL DEFAULT 0,
    version      INTEGER       NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    UNIQUE (warehouse_id, product_id),
    CHECK (reserved_qty >= 0)
);

CREATE INDEX IF NOT EXISTS idx_wpr_warehouse_product
    ON warehouse_product_reservations(warehouse_id, product_id);
