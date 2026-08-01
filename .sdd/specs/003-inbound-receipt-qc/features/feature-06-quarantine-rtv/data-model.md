# Data Model: Feature 06 Quarantine RTV

## Tables Touched

| Table | Usage |
|-------|-------|
| `receipts` | Source receipt remains traceable for failed quantity |
| `receipt_items` | Track unresolved `quarantine_qty` |
| `inventories` | Decrease Quarantine inventory only on RTV confirmation |
| `adjustments` | Create pending `RETURN_TO_VENDOR` document |
| `debit_notes` | Create supplier Debit Note (`status = PENDING`); apply action later flips it to `APPLIED` and mutates `suppliers.current_balance` |
| `suppliers` | `current_balance` decreased only when ACCOUNTANT applies an `APPLIED`-eligible Debit Note |
| `audit_logs` | Record RTV create/confirm, Debit Note apply, and inventory update |

## Debit Note Fields (new)

| Field | Notes |
|-------|-------|
| `status` | `PENDING` (default, set at RTV/rejection creation) → `APPLIED` (set by accountant apply action). Immutable once `APPLIED`. |
| `applied_by` | User id of the `ACCOUNTANT`/`ACCOUNTANT_MANAGER` who applied it; `NULL` while `PENDING`. |
| `applied_at` | Timestamp of apply; `NULL` while `PENDING`. |

Migration: add `status` (default `PENDING`, NOT NULL), `applied_by` (nullable FK to `users`), `applied_at` (nullable) to `debit_notes`. Existing rows created before this change (if any) should backfill `status = PENDING` — they represent supplier claims that were never actually applied to the ledger under the old behavior, so backfilling as `PENDING` (not `APPLIED`) is the accurate state, not a guess.

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
| `debit_notes` (`status = PENDING`) | Created once with RTV |
| Quarantine inventory decrease | Only when STOREKEEPER confirms physical handover |
| `debit_notes.status -> APPLIED`, `suppliers.current_balance` decrease | Only when ACCOUNTANT applies the Debit Note, and only after physical handover is confirmed |

## Audit Data

- `QUARANTINE_RTV_CREATE`: adjustment and Debit Note references.
- `QUARANTINE_RTV_CONFIRM`: returned quantity and handover metadata.
- `DEBIT_NOTE_APPLY`: Debit Note id, supplier id, amount, supplier balance before/after.
- `INVENTORY_UPDATE`: Quarantine inventory before/after.
