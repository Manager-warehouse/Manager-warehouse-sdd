ALTER TABLE stock_alerts
    DROP CONSTRAINT IF EXISTS stock_alerts_warehouse_id_product_id_alert_type_is_resolved_key;

DROP INDEX IF EXISTS stock_alerts_warehouse_id_product_id_alert_type_is_resolved_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_stock_alerts_open
    ON stock_alerts (warehouse_id, product_id, alert_type)
    WHERE is_resolved = false;

CREATE OR REPLACE FUNCTION fn_check_stock_alert()
RETURNS TRIGGER AS $$
DECLARE
    v_reorder   DECIMAL;
    v_total_qty DECIMAL;
BEGIN
    SELECT reorder_point INTO v_reorder
    FROM products WHERE id = NEW.product_id;

    SELECT COALESCE(SUM(total_qty), 0) INTO v_total_qty
    FROM inventories
    WHERE warehouse_id = NEW.warehouse_id
      AND product_id   = NEW.product_id;

    IF v_total_qty <= 0 THEN
        INSERT INTO stock_alerts
            (warehouse_id, product_id, current_qty, reorder_point, alert_type)
        VALUES
            (NEW.warehouse_id, NEW.product_id, v_total_qty, COALESCE(v_reorder,0), 'OUT_OF_STOCK')
        ON CONFLICT DO NOTHING;
    ELSIF v_reorder IS NOT NULL AND v_total_qty <= v_reorder THEN
        INSERT INTO stock_alerts
            (warehouse_id, product_id, current_qty, reorder_point, alert_type)
        VALUES
            (NEW.warehouse_id, NEW.product_id, v_total_qty, v_reorder, 'LOW_STOCK')
        ON CONFLICT DO NOTHING;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
