# Data Model: Feature 05 Putaway And Inventory

## Tables Touched

| Table | Usage |
|-------|-------|
| `receipts` | Move approved receipt to `PUTAWAY_COMPLETED` |
| `receipt_items` | Store Storekeeper-selected putaway location/allocation reference |
| `inventories` | Increase regular inventory by approved quantity |
| `warehouse_locations` | Validate regular bin and capacity |
| `audit_logs` | Record putaway and inventory update |

## Status Data

| Input Status | Output Status | Inventory Impact |
|--------------|---------------|------------------|
| `APPROVED` | `PUTAWAY_COMPLETED` | Increase regular inventory by `actual_qty` |
| `PARTIALLY_APPROVED` | `PUTAWAY_COMPLETED` | Increase regular inventory by `approved_qty` only |

## Putaway Data

```json
{
  "expectedVersion": 6,
  "allocations": [
    {
      "receiptItemId": 1001,
      "expectedBatchCode": "LOT-HP-20260728-0001",
      "locationId": 301,
      "quantity": 90
    }
  ]
}
```

## Quantity Rules

- Sum of allocation quantity per receipt item must equal `approved_qty`.
- STOREKEEPER must provide the target bin/location for each allocation; location must be active, regular, and in the receipt warehouse.
- Quarantine location is never valid for regular putaway.
- Bin capacity must be checked before inventory update.
- Duplicate putaway must not increase inventory again.
- Putaway must preserve the receipt item's generated `batch_code`; users may confirm the batch but must not merge separate receipt batches for the same product and supplier.
- `expectedBatchCode` is optional; when supplied from scan/manual confirmation, it must match the receipt item's generated batch before inventory is updated.

## Inventory Data

| Field | Rule |
|-------|------|
| `inventories.total_qty` | Increases by approved putaway quantity |
| `inventories.reserved_qty` | Unchanged by inbound putaway |
| `inventories.batch_id` | References the generated receiving batch; `batches.batch_code` is displayed through this FK |
| `inventories.location_id` | Tracks the bin/location where this batch quantity is stored |
| `inventories.version` | Checked and incremented by optimistic locking |

## Audit Data

- `RECEIPT_PUTAWAY_COMPLETE`: receipt status and location allocation.
- `INVENTORY_UPDATE`: before/after `total_qty`, `reserved_qty`, location, batch.
