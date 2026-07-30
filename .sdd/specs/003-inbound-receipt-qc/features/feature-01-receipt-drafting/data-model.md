# Data Model: Feature 01 Receipt Drafting

## Tables Touched

| Table | Usage |
|-------|-------|
| `receipts` | Create receipt header in `PENDING_MANAGER_APPROVAL` |
| `receipt_items` | Create expected item lines |
| `suppliers` | Validate active supplier |
| `warehouses` | Validate active warehouse and actor scope |
| `products` | Validate active products |
| `audit_logs` | Record `RECEIPT_CREATE` |

## Status Data

| Input Status | Output Status | Notes |
|--------------|---------------|-------|
| none | `PENDING_MANAGER_APPROVAL` | New purchase receipt created and waiting for WH_MANAGER approval before counting |
| `REVISION_REQUIRED` | `PENDING_MANAGER_APPROVAL` | PLANNER corrects and resubmits after WH_MANAGER pre-receive rejection |

## Required Data

```json
{
  "supplierId": 11,
  "warehouseId": 1,
  "documentDate": "2026-07-28",
  "items": [
    {
      "productId": 501,
      "expectedQty": 100,
      "unitCost": 125000
    }
  ]
}
```

## Stored Data Rules

- `receipts.type = PURCHASE`.
- `receipts.receipt_number` is system-generated as `PO-{YYYYMMDD}-{SEQ}` and is returned to the user after creation.
- `receipts.source_order_code` is not required or exposed in the Spec 003 create form.
- `receipts.contact_person` and `receipts.source_channel` are not required or exposed in the Spec 003 create form; they may remain nullable/internal fields for future external-channel integrations.
- `receipts.status = PENDING_MANAGER_APPROVAL`.
- `REVISION_REQUIRED` receipts can be edited/resubmitted by PLANNER but cannot be counted or sent to QC.
- `receipt_items.expected_qty > 0`.
- `receipt_items.actual_qty`, QC fields, `approved_qty`, and `quarantine_qty` are empty or zero.
- Duplicate protection is enforced by generated unique `receipt_number`; users do not provide source PO/source document identifiers.

## Audit Data

- `RECEIPT_CREATE` stores receipt header before/after, actor, role, warehouse, supplier, generated receipt number, and document date.
- `RECEIPT_PRE_RECEIVE_RESUBMIT` stores corrected receipt header/items before/after, actor, role, warehouse, and transition from `REVISION_REQUIRED` to `PENDING_MANAGER_APPROVAL`.
