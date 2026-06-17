# Data Model: Warehouse Staff Receipt Counting

## Receipt

Existing transaction header for inbound receipt.

| Field | Type | Rules |
|-------|------|-------|
| `id` | BIGINT | Required path target for receive counting |
| `receipt_number` | VARCHAR(50) | Used as audit entity code |
| `status` | enum | Allowed receive counting states: `PENDING_RECEIPT`, `DRAFT`, `QC_COMPLETED`, `QC_FAILED`; final states: `APPROVED`, `REJECTED` |
| `warehouse_id` | BIGINT | Used for RBAC warehouse assignment check |
| `updated_at` | timestamp | Updated when counts/status change |

### State Transitions

| From | Event | To |
|------|-------|----|
| `PENDING_RECEIPT` | Complete count submission accepted | `DRAFT` |
| `DRAFT` | Count correction accepted | `DRAFT` |
| `QC_COMPLETED` | Count correction accepted | `DRAFT` |
| `QC_FAILED` | Count correction accepted | `DRAFT` |
| `APPROVED` | Count submission attempted | Reject with `RECEIPT_ALREADY_FINALIZED` |
| `REJECTED` | Count submission attempted | Reject with `RECEIPT_ALREADY_FINALIZED` |

## ReceiptItem

Existing receipt line entity updated by this feature.

| Field | Type | Rules |
|-------|------|-------|
| `id` | BIGINT | Must belong to target receipt |
| `receipt_id` | BIGINT | Must equal path receipt id |
| `expected_qty` | INTEGER | Already positive from drafting feature |
| `actual_qty` | INTEGER | Set to `counted_qty` when `counted_qty <= expected_qty`; set to `expected_qty` when over-received |
| `over_received_qty` | INTEGER | Set to `0` when `counted_qty <= expected_qty`; set to `counted_qty - expected_qty` when over-received |
| `sample_qty` | INTEGER | Cleared on count correction after QC data exists |
| `sample_passed_qty` | INTEGER | Cleared on count correction after QC data exists |
| `sample_failed_qty` | INTEGER | Cleared on count correction after QC data exists |
| `qc_sampling_method` | enum/null | Cleared on count correction after QC data exists |
| `qc_result` | enum/null | Cleared on count correction after QC data exists |
| `qc_failure_reason` | text/null | Cleared on count correction after QC data exists |

### Calculation Rules

```text
if counted_qty <= expected_qty:
    actual_qty = counted_qty
    over_received_qty = 0
else:
    actual_qty = expected_qty
    over_received_qty = counted_qty - expected_qty
```

## ReceiveReceiptRequest

Request body for `PUT /api/v1/receipts/{id}/receive`.

| Field | Type | Rules |
|-------|------|-------|
| `items` | array | Required; must contain exactly one entry for every receipt item in the target receipt |

## ReceiveReceiptItemRequest

| Field | Type | Rules |
|-------|------|-------|
| `receipt_item_id` | BIGINT | Required; must belong to target receipt; duplicates rejected |
| `counted_qty` | INTEGER | Required; must be greater than 0 |

## AuditLog

Audit entry created on every successful receive count or correction.

| Field | Value |
|-------|-------|
| `actor` | Current Warehouse Staff user |
| `action` | `RECEIPT_RECEIVE` equivalent (`UPDATE` if enum remains generic) |
| `entity_type` | `RECEIPT` |
| `entity_id` | Receipt id |
| `entity_code` | Receipt number |
| `warehouse_id` | Receipt warehouse id |
| `before` | Receipt status and item count/QC fields before change |
| `after` | Receipt status and item count/QC fields after change |

## Validation Summary

- Reject missing receipt with `404`.
- Reject non-Warehouse Staff or unassigned warehouse access with `403`.
- Reject non-integer, zero, or negative counts with `INVALID_RECEIPT_COUNT`.
- Reject missing/extra/duplicate receipt item ids with `RECEIPT_COUNT_INCOMPLETE` or `INVALID_RECEIPT_COUNT`.
- Reject status `APPROVED` or `REJECTED` with `RECEIPT_ALREADY_FINALIZED`.
- Save no partial changes on any validation failure.
