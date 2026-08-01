# Data Model: Feature 02 Receipt Counting

## Tables Touched

| Table | Usage |
|-------|-------|
| `receipts` | Move receipt to `PENDING_STOREKEEPER_REVIEW` after Staff receive-and-QC save; increment version |
| `receipt_items` | Store `actual_qty` and `over_received_qty` |
| `audit_logs` | Record `RECEIPT_RECEIVE_QC` |

## Status Data

| Input Status | Output Status | Notes |
|--------------|---------------|-------|
| `PENDING_MANAGER_APPROVAL` | no change | Counting is blocked until WH_MANAGER approves receipt for receiving |
| `REVISION_REQUIRED` | no change | Counting is blocked while PLANNER correction is required |
| `PENDING_RECEIPT` | `PENDING_STOREKEEPER_REVIEW` | First Staff receive-and-QC save |
| `RECOUNT_REQUIRED` | `PENDING_STOREKEEPER_REVIEW` | Corrected Staff receive-and-QC save after Storekeeper recount request |
| `DRAFT` | `PENDING_STOREKEEPER_REVIEW` | Legacy/correction Staff save before manager decision |

## Request Data

```json
{
  "expectedVersion": 3,
  "items": [
    {
      "receiptItemId": 1001,
      "actualQty": 98,
      "qualityPassedQty": 98,
      "qualityFailedQty": 0
    },
    {
      "receiptItemId": 1002,
      "actualQty": 120,
      "qualityPassedQty": 116,
      "qualityFailedQty": 4,
      "failureReason": "Broken lids"
    }
  ]
}
```

## Quantity Examples

| expected_qty | submitted actual_qty | stored actual_qty | over_received_qty | Meaning |
|--------------|-------------|------------|-------------------|---------|
| 100 | 98 | 98 | 0 | Shortage of 2 retained for review |
| 100 | 100 | 100 | 0 | Exact receipt |
| 100 | 120 | 120 | 20 | Excess is recorded but not inventory |

## Stored Data Rules

- Count payload must include exactly every receipt item once.
- Receipt must be `PENDING_RECEIPT`, `RECOUNT_REQUIRED`, or `DRAFT`; `PENDING_MANAGER_APPROVAL`, `REVISION_REQUIRED`, `PENDING_STOREKEEPER_REVIEW`, `QC_COMPLETED`, and `QC_FAILED` are rejected for Staff entry unless a Storekeeper/manager correction request first moves the receipt to `RECOUNT_REQUIRED`.
- `actualQty` must be a non-negative integer.
- When `actualQty > expected_qty`, `over_received_qty = actualQty - expected_qty`; the excess is not inventory, batch, putaway, supplier invoice, Debit Note, RTV, or finalized Quarantine stock.
- Count correction after `RECOUNT_REQUIRED` clears old `quality_passed_qty`, `quality_failed_qty`, `qc_result`, `qc_failure_reason`, `approved_qty`, and non-finalized `quarantine_ready_qty` before saving new count/QC values.
- Count correction is blocked after `quarantine_qty` has been finalized by WH_MANAGER approval/rejection.
- Counting never creates batch, regular inventory, or quarantine inventory.

## Audit Data

- Staff receive-and-QC save: `RECEIPT_RECEIVE_QC`.
- Audit before/after includes item quantities and receipt status.
