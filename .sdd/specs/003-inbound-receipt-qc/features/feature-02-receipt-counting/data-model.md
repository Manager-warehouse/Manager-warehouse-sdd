# Data Model: Feature 02 Receipt Counting

## Tables Touched

| Table | Usage |
|-------|-------|
| `receipts` | Move receipt to `DRAFT`; increment version |
| `receipt_items` | Store `actual_qty` and `over_received_qty` |
| `audit_logs` | Record `RECEIPT_RECEIVE` or `RECEIPT_CORRECTION` |

## Status Data

| Input Status | Output Status | Notes |
|--------------|---------------|-------|
| `PENDING_MANAGER_APPROVAL` | no change | Counting is blocked until WH_MANAGER approves receipt for receiving |
| `REVISION_REQUIRED` | no change | Counting is blocked while PLANNER correction is required |
| `PENDING_RECEIPT` | `DRAFT` | First physical count |
| `DRAFT` | `DRAFT` | Count correction before QC |
| `QC_COMPLETED` | `DRAFT` | Count correction clears QC |
| `QC_FAILED` | `DRAFT` | Count correction clears QC/quarantine draft data |

## Request Data

```json
{
  "expectedVersion": 3,
  "items": [
    {
      "receiptItemId": 1001,
      "countedQty": 98
    },
    {
      "receiptItemId": 1002,
      "countedQty": 120
    }
  ]
}
```

## Quantity Examples

| expected_qty | counted_qty | actual_qty | over_received_qty | Meaning |
|--------------|-------------|------------|-------------------|---------|
| 100 | 98 | 98 | 0 | Shortage of 2 retained for review |
| 100 | 100 | 100 | 0 | Exact receipt |
| 100 | 120 | 100 | 20 | Excess is recorded but not inventory |

## Stored Data Rules

- Count payload must include exactly every receipt item once.
- Receipt must be `PENDING_RECEIPT`, `DRAFT`, `QC_COMPLETED`, or non-finalized `QC_FAILED`; `PENDING_MANAGER_APPROVAL` and `REVISION_REQUIRED` are rejected without saving counts.
- `countedQty` must be positive integer.
- Count correction before WH_MANAGER finalization clears `sample_qty`, `sample_passed_qty`, `sample_failed_qty`, `quality_passed_qty`, `quality_failed_qty`, `qc_result`, `qc_failure_reason`, `approved_qty`, and `quarantine_ready_qty`.
- Count correction is blocked after `quarantine_qty` has been finalized by WH_MANAGER approval/rejection.
- Counting never creates batch, regular inventory, or quarantine inventory.

## Audit Data

- First count: `RECEIPT_RECEIVE`.
- Correction: `RECEIPT_CORRECTION`.
- Audit before/after includes item quantities and receipt status.
