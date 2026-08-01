# Data Model: Feature 04 Manager Decision

## Tables Touched

| Table | Usage |
|-------|-------|
| `receipts` | Store WH_MANAGER decision status and metadata |
| `receipt_items` | Store `approved_qty` and batch lineage |
| `batches` | Resolve/create inbound batch for approved quantity |
| `audit_logs` | Record approve, partial approve, reject, and handover confirmation |

## Status Data

| Input Status | Output Status | Decision |
|--------------|---------------|----------|
| `QC_COMPLETED` | `APPROVED` | Approve all actual quantity |
| `QC_FAILED` | `PARTIALLY_APPROVED` | Approve passed quantity only |
| `QC_COMPLETED` | `RETURN_TO_SUPPLIER_PENDING` | Reject whole receipt with reason |
| `QC_FAILED` | `RETURN_TO_SUPPLIER_PENDING` | Reject whole receipt with reason |
| `RETURN_TO_SUPPLIER_PENDING` | `RETURNED_TO_SUPPLIER` | WH_MANAGER confirms supplier handover |

## Approval Data

```json
{
  "expectedVersion": 5,
  "decisionNote": "Approved passed quantity for putaway"
}
```

## Rejection Data

```json
{
  "expectedVersion": 5,
  "reason": "Supplier delivered wrong product model"
}
```

## Quantity Rules

| Status | approved_qty |
|--------|--------------|
| `APPROVED` | `actual_qty` |
| `PARTIALLY_APPROVED` | `quality_passed_qty` |
| `RETURN_TO_SUPPLIER_PENDING` | `0` |

When WH_MANAGER finalizes a `QC_FAILED` receipt through partial approval or whole rejection, `quarantine_qty` is set from `quarantine_ready_qty`, and `quarantine_ready_qty` is cleared. This is the first point where failed quantity becomes finalized Quarantine stock.

## Batch Data

Batch identity uses:

```text
(product_id, warehouse_id, receipt_number, document_date or received_date, supplier_id)
```

## Stored Data Rules

- WH_MANAGER decision requires role plus warehouse scope.
- WH_MANAGER decision is blocked until Storekeeper review approval has produced `QC_COMPLETED` or `QC_FAILED`.
- Approval requires positive `unit_cost` for approved items.
- Approval resolves batch lineage but does not increase regular inventory.
- Rejection requires `rejection_reason`.
- Rejecting a `QC_FAILED` receipt must keep finalized quarantine quantity traceable.

## Audit Data

- Full approval: `RECEIPT_APPROVE`.
- Partial approval: `RECEIPT_PARTIAL_APPROVE`.
- Whole rejection: `RECEIPT_REJECT`.
- Supplier handover: `RECEIPT_RETURN_CONFIRM`.
