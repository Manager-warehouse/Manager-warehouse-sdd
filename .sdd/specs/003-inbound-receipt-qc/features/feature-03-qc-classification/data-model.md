# Data Model: Feature 03 QC Classification

## Tables Touched

| Table | Usage |
|-------|-------|
| `receipts` | Move `DRAFT` to `QC_COMPLETED` or `QC_FAILED` |
| `receipt_items` | Store QC sample, passed, failed, failure reason data, and `quarantine_ready_qty` |
| `audit_logs` | Record QC submit/confirm without inventory movement |

## Status Data

| Input Status | Output Status | Condition |
|--------------|---------------|-----------|
| `DRAFT` | `QC_COMPLETED` | All `quality_failed_qty = 0` |
| `DRAFT` | `QC_FAILED` | Any `quality_failed_qty > 0` |

## QC Data Example: All Passed

```json
{
  "expectedVersion": 4,
  "items": [
    {
      "receiptItemId": 1001,
      "sampleQty": 10,
      "samplePassedQty": 10,
      "sampleFailedQty": 0,
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
      "sampleQty": 10,
      "samplePassedQty": 9,
      "sampleFailedQty": 1,
      "qualityPassedQty": 90,
      "qualityFailedQty": 10,
      "qcResult": "FAILED",
      "qcFailureReason": "Broken handle"
    }
  ]
}
```

## Stored Data Rules

- `sample_passed_qty + sample_failed_qty = sample_qty`.
- `quality_passed_qty + quality_failed_qty = actual_qty`.
- Failed quantity requires `qc_failure_reason`.
- `QC_FAILED` creates `quarantine_ready_qty` only for `quality_failed_qty`; finalized `quarantine_qty` is created by WH_MANAGER approval/rejection.
- Passed quantity remains outside regular available inventory until WH_MANAGER approval and putaway.
- QC confirmation does not create batch, regular inventory, Quarantine inventory, supplier-return status, or warehouse-location occupancy.

## Audit Data

- `RECEIPT_QC_SUBMIT`: stores QC payload before confirmation.
- `RECEIPT_QC_CONFIRM`: stores status transition.
- Quarantine movement and `INVENTORY_UPDATE` audit belong to WH_MANAGER finalization or later RTV/disposal confirmation, not QC confirmation.
