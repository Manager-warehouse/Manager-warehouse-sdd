-- Spec 005 EX-19: UOM and Packaging Unit Snapshot fields to prevent drift during IN_TRANSIT
ALTER TABLE inter_warehouse_transfer_items
    ADD COLUMN IF NOT EXISTS uom_unit_snapshot VARCHAR(30),
    ADD COLUMN IF NOT EXISTS uom_pack_rate_snapshot INT,
    ADD COLUMN IF NOT EXISTS unit_weight_snapshot DECIMAL(10, 3),
    ADD COLUMN IF NOT EXISTS unit_volume_snapshot DECIMAL(10, 5);

-- Backfill existing rows using current product attributes
UPDATE inter_warehouse_transfer_items item
SET uom_unit_snapshot = COALESCE(item.uom_unit_snapshot, p.unit),
    uom_pack_rate_snapshot = COALESCE(item.uom_pack_rate_snapshot, p.unit_per_pack, 1),
    unit_weight_snapshot = COALESCE(item.unit_weight_snapshot, p.weight_kg),
    unit_volume_snapshot = COALESCE(item.unit_volume_snapshot, p.volume_m3)
FROM products p
WHERE item.product_id = p.id;
