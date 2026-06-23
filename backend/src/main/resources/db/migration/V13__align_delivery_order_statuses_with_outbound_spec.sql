ALTER TABLE delivery_orders
    DROP CONSTRAINT IF EXISTS delivery_orders_status_check;

UPDATE delivery_orders
SET status = CASE status
    WHEN 'PENDING_WAREHOUSE_APPROVAL' THEN 'QC_COMPLETED'
    WHEN 'READY_TO_SHIP' THEN 'WAREHOUSE_APPROVED'
    WHEN 'OUT_FOR_DELIVERY' THEN 'IN_TRANSIT'
    WHEN 'DELIVERED' THEN 'COMPLETED'
    ELSE status
END
WHERE status IN ('PENDING_WAREHOUSE_APPROVAL', 'READY_TO_SHIP', 'OUT_FOR_DELIVERY', 'DELIVERED');

ALTER TABLE delivery_orders
    ADD CONSTRAINT delivery_orders_status_check
    CHECK (status IN (
        'NEW',
        'PICKING_PLANNED',
        'WAITING_PICKING',
        'PICKING',
        'QC_PENDING_APPROVAL',
        'QC_COMPLETED',
        'WAREHOUSE_APPROVED',
        'IN_TRANSIT',
        'RETURNED',
        'DELIVERY_FAILED',
        'COMPLETED',
        'CLOSED',
        'REJECTED',
        'CANCELLED'
    ));

CREATE OR REPLACE VIEW v_pending_delivery_orders AS
SELECT
    dord.do_number,
    dord.status,
    dord.type,
    d.name            AS dealer_name,
    d.phone           AS dealer_phone,
    dord.expected_delivery_date,
    u.full_name       AS created_by_name,
    dord.document_date,
    dord.created_at
FROM delivery_orders dord
JOIN dealers d ON d.id = dord.dealer_id
JOIN users   u ON u.id = dord.created_by
WHERE dord.status IN (
    'NEW',
    'PICKING_PLANNED',
    'WAITING_PICKING',
    'PICKING',
    'QC_PENDING_APPROVAL',
    'QC_COMPLETED',
    'WAREHOUSE_APPROVED'
)
ORDER BY dord.expected_delivery_date ASC NULLS LAST, dord.created_at ASC;
