# Data Model: Feature 03 QC Classification

## Tables Touched

| Table | Usage |
|-------|-------|
| `receipts` | Move Staff submission to `PENDING_STOREKEEPER_REVIEW`; Storekeeper approval moves it to `QC_COMPLETED` or `QC_FAILED`; recount request moves it to `RECOUNT_REQUIRED` |
| `receipt_items` | Store QC passed, failed, failure reason data, and `quarantine_ready_qty` |
| `audit_logs` | Record unified receive-and-QC save without inventory movement |

## Status Data

| Input Status | Output Status | Condition |
|--------------|---------------|-----------|
| `PENDING_RECEIPT` | `PENDING_STOREKEEPER_REVIEW` | WH_STAFF saves valid count/QC |
| `PENDING_STOREKEEPER_REVIEW` | `RECOUNT_REQUIRED` | STOREKEEPER rejects review with `REQUEST_RECOUNT` decision and recount reason |
| `RECOUNT_REQUIRED` | `PENDING_STOREKEEPER_REVIEW` | WH_STAFF resubmits corrected count/QC |
| `PENDING_STOREKEEPER_REVIEW` | `QC_COMPLETED` | STOREKEEPER approves and all `quality_failed_qty = 0` |
| `PENDING_STOREKEEPER_REVIEW` | `QC_FAILED` | STOREKEEPER approves and any `quality_failed_qty > 0` |

## QC Data Example: All Passed

```json
{
  "expectedVersion": 4,
  "items": [
    {
      "receiptItemId": 1001,
      "actualQty": 100,
      "qualityPassedQty": 100,
      "qualityFailedQty": 0,
      "qcResult": "PASSED"
    }
  ]
}
```

## QC Data Example: Some Failed

```json
{
  "expectedVersion": 4,
  "items": [
    {
      "receiptItemId": 1001,
      "actualQty": 100,
      "qualityPassedQty": 90,
      "qualityFailedQty": 10,
      "qcResult": "FAILED",
      "qcFailureReason": "Broken handle"
    }
  ]
}
```

## Stored Data Rules

- `quality_passed_qty + quality_failed_qty = actual_qty`.
- Failed quantity requires `qc_failure_reason`.
- Storekeeper recount request stores `recount_reason` and makes it visible to WH_STAFF on the corrected receive-and-QC screen.
- `QC_FAILED` creates `quarantine_ready_qty` only for `quality_failed_qty` after Storekeeper approval; finalized `quarantine_qty` is created by WH_MANAGER approval/rejection.
- Passed quantity remains outside regular available inventory until WH_MANAGER approval and putaway.
- QC save and Storekeeper review do not create batch, regular inventory, finalized Quarantine inventory, putaway, supplier invoice, Debit Note, RTV, supplier-return status, or warehouse-location occupancy.
- The UI defaults `quality_passed_qty = actual_qty` and `quality_failed_qty = 0` when actual quantity is entered, and recalculates passed quantity as `actual_qty - quality_failed_qty` when failed quantity changes.

## Audit Data

- `RECEIPT_RECEIVE_QC`: stores receive-and-QC payload and status transition.
- `RECEIPT_STOREKEEPER_REVIEW_APPROVE`: stores Storekeeper approval and status transition to `QC_COMPLETED` or `QC_FAILED`.
- `RECEIPT_STOREKEEPER_RECOUNT_REQUEST`: stores Storekeeper recount request, reason, and status transition to `RECOUNT_REQUIRED`.
- Quarantine movement and `INVENTORY_UPDATE` audit belong to WH_MANAGER finalization or later RTV/disposal confirmation, not QC confirmation.
