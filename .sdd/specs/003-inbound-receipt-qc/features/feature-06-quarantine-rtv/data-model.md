# Data Model: Feature 06 Quarantine RTV

## Tables Touched

| Table | Usage |
|-------|-------|
| `receipts` | Source receipt remains traceable for failed quantity |
| `receipt_items` | Track unresolved `quarantine_qty` |
| `inventories` | Decrease Quarantine inventory only on RTV confirmation |
| `adjustments` | Create pending `RETURN_TO_VENDOR` document |
| `debit_notes` | Create supplier Debit Note |
| `audit_logs` | Record RTV create/confirm and inventory update |

## Status Data

| Input Status | Output Status | Notes |
|--------------|---------------|-------|
| `PARTIALLY_APPROVED` | `PARTIALLY_APPROVED` | Failed quantity can still be handled while passed quantity proceeds |
| `RETURN_TO_SUPPLIER_PENDING` | `RETURN_TO_SUPPLIER_PENDING` | Finalized failed quantity can be handed back while whole receipt remains rejected |

## RTV Create Data

```json
{
  "expectedVersion": 7,
  "note": "Failed inbound QC"
}
```

## RTV Confirm Data

```json
{
  "expectedVersion": 8,
  "returnedQty": 10,
  "note": "Supplier vehicle collected failed goods"
}
```

## Quantity Rules

- `quarantine_qty = quality_failed_qty - resolved_quarantine_qty`.
- RTV creation requires finalized unresolved failed quarantine quantity.
- RTV creation does not reduce Quarantine inventory.
- RTV confirmation requires returned quantity equals unresolved failed quarantine quantity.
- RTV confirmation reduces Quarantine inventory exactly once.

## Accounting Data

| Record | Timing |
|--------|--------|
| `adjustments(type = RETURN_TO_VENDOR)` | Created when WH_MANAGER creates RTV |
| `debit_notes` | Created once with RTV |
| Quarantine inventory decrease | Only when STOREKEEPER confirms physical handover |

## Audit Data

- `QUARANTINE_RTV_CREATE`: adjustment and Debit Note references.
- `QUARANTINE_RTV_CONFIRM`: returned quantity and handover metadata.
- `INVENTORY_UPDATE`: Quarantine inventory before/after.
