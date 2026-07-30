# Inbound Receipt API Contracts: 003-inbound-receipt-qc

All write endpoints require:

- JWT authentication.
- Role authorization plus warehouse scope.
- `expectedVersion` for optimistic locking on every mutation of an existing receipt; create receipt is the only exception because no receipt aggregate exists yet.
- Centralized error response with `code`, `message`, `currentStatus` when relevant, and `allowedActions` when relevant.
- Audit log on success; denied warehouse-scope attempts log `AUTHORIZATION_DENIED`.
- HTTP `400` is used for request DTO validation failures, `409` for stale version/duplicate/idempotency conflicts, and `422` for business rule violations.

## 1. `POST /api/v1/receipts`

Create purchase receipt.

**Roles**: `PLANNER`

```json
{
  "supplierId": 31,
  "warehouseId": 2,
  "documentDate": "2026-07-28",
  "items": [
    {
      "productId": 101,
      "expectedQty": 50,
      "unitCost": 120000
    }
  ]
}
```

**Behavior**:

- Creates `PURCHASE` receipt in `PENDING_MANAGER_APPROVAL`.
- Generates `receiptNumber` as `PO-{YYYYMMDD}-{SEQ}` using `documentDate`, for example `PO-20260728-0001`.
- Does not accept a user-entered source PO/source document code, contact person, or source channel in Spec 003.
- Writes `RECEIPT_CREATE` audit with generated receipt number and document date.
- Does not create batch, regular inventory, quarantine inventory, or accounting document.

## 2. `PUT /api/v1/receipts/{id}/pre-receive-approval`

Approve a newly created purchase receipt so warehouse staff can count it, or reject it back to PLANNER for correction.

**Roles**: `WH_MANAGER`

```json
{
  "expectedVersion": 1,
  "decision": "APPROVE",
  "reason": "Receipt is valid for receiving"
}
```

**Behavior**:

- Allowed only from `PENDING_MANAGER_APPROVAL`.
- `APPROVE` moves the receipt to `PENDING_RECEIPT` and writes `RECEIPT_PRE_RECEIVE_APPROVE` audit.
- `REJECT` requires a reason, moves the receipt to `REVISION_REQUIRED`, and writes `RECEIPT_PRE_RECEIVE_REJECT` audit.
- Pre-receive approval/rejection audit includes actor, role, warehouse, before/after status, decision, and rejection reason when relevant.
- Does not create batch, regular inventory, quarantine inventory, or accounting document.

## 3. `PUT /api/v1/receipts/{id}/revision`

Revise and resubmit a pre-receive rejected purchase receipt.

**Roles**: `PLANNER`

```json
{
  "expectedVersion": 2,
  "documentDate": "2026-07-29",
  "items": [
    {
      "receiptItemId": 101,
      "productId": 101,
      "expectedQty": 45,
      "unitCost": 120000
    }
  ]
}
```

**Behavior**:

- Allowed only from `REVISION_REQUIRED`.
- Updates allowed receipt header/item expectation fields and moves the receipt to `PENDING_MANAGER_APPROVAL`.
- Writes `RECEIPT_PRE_RECEIVE_RESUBMIT` audit with before/after status, header, item expectation changes, actor, role, and warehouse.
- Does not create batch, regular inventory, quarantine inventory, or accounting document.

## 4. `PUT /api/v1/receipts/{id}/receive`

Submit or correct physical count.

**Roles**: `WH_STAFF`, `STOREKEEPER`

```json
{
  "expectedVersion": 0,
  "items": [
    {
      "receiptItemId": 101,
      "countedQty": 50
    }
  ]
}
```

**Behavior**:

- Allowed in `PENDING_RECEIPT`, `DRAFT`, `QC_COMPLETED`, `QC_FAILED`; rejected in `PENDING_MANAGER_APPROVAL` or `REVISION_REQUIRED`.
- Requires exactly one positive integer count per receipt item.
- Stores `actual_qty` and `over_received_qty`.
- If correcting after QC, clears QC data and returns receipt to `DRAFT`.
- Never auto-rejects or auto-cancels from variance.

## 5. `PUT /api/v1/receipts/{id}/qc`

Submit QC data.

**Roles**: `WH_STAFF`, `STOREKEEPER`, `WH_MANAGER`

```json
{
  "expectedVersion": 4,
  "items": [
    {
      "receiptItemId": 101,
      "sampleQty": 10,
      "samplePassedQty": 8,
      "sampleFailedQty": 2,
      "qualityPassedQty": 48,
      "qualityFailedQty": 2,
      "qcSamplingMethod": "RANDOM_SAMPLE",
      "failureReason": "Scratched handles"
    }
  ]
}
```

**Behavior**:

- Allowed in `DRAFT`.
- Validates sample totals and actual quality totals.
- Requires failure reason when failed quantity exists.
- Does not change regular inventory.

## 6. `PUT /api/v1/receipts/{id}/qc/confirm`

Confirm QC classification.

**Roles**: `WH_STAFF`, `STOREKEEPER`, `WH_MANAGER`

**Behavior**:

- If all quantity passed, status becomes `QC_COMPLETED`.
- If any failed quantity exists, status becomes `QC_FAILED` and only failed quantity is staged as Quarantine readiness.
- QC confirmation cannot reject the whole receipt.

## 7. `PUT /api/v1/receipts/{id}/approve`

WH_MANAGER approval decision.

**Roles**: `WH_MANAGER`

```json
{
  "expectedVersion": 2,
  "decisionNote": "Approved for putaway",
  "itemUnitCosts": [
    {
      "receiptItemId": 101,
      "unitCost": 120000
    }
  ]
}
```

**Behavior**:

- `QC_COMPLETED` -> `APPROVED`, `approved_qty = actual_qty`.
- `QC_FAILED` -> `PARTIALLY_APPROVED`, `approved_qty = quality_passed_qty`.
- Requires positive `unit_cost` for approved items.
- Resolves one generated `batchCode` per approved product receipt line but does not increase regular inventory.
- Returns `NO_PASSED_QUANTITY_TO_APPROVE` if `QC_FAILED` has no passed quantity.

## 8. `PUT /api/v1/receipts/{id}/reject`

Reject whole receipt.

**Roles**: `WH_MANAGER`

```json
{
  "expectedVersion": 2,
  "reason": "Supplier delivered wrong model"
}
```

**Behavior**:

- Allowed from `QC_COMPLETED` or `QC_FAILED`.
- Requires reason.
- Moves receipt to `RETURN_TO_SUPPLIER_PENDING`.
- Does not create regular inventory.
- If failed quarantine quantity already exists, it remains traceable and must be resolved by supplier handover/RTV/disposal linkage.

## 9. `POST /api/v1/receipts/{id}/putaway`

Complete putaway for approved goods.

**Roles**: `STOREKEEPER`

```json
{
  "expectedVersion": 3,
  "allocations": [
    {
      "receiptItemId": 101,
      "locationId": 501,
      "quantity": 50
    }
  ]
}
```

**Behavior**:

- Allowed from `APPROVED` or `PARTIALLY_APPROVED`.
- Total allocation quantity must equal `approved_qty`.
- Locations must be active regular bins in the receipt warehouse.
- Optional `expectedBatchCode` may be supplied per allocation for scan/manual confirmation; when present it must match the receipt item's generated batch.
- Validates bin capacity.
- Increases regular inventory by approved quantity only.
- Sets status to `PUTAWAY_COMPLETED`.
- Duplicate putaway returns `PUTAWAY_ALREADY_COMPLETED`.

## 10. `PUT /api/v1/receipts/{id}/return-to-supplier/confirm`

Confirm handover for whole-receipt rejection.

**Roles**: `STOREKEEPER`

**Behavior**:

- Allowed only from `RETURN_TO_SUPPLIER_PENDING`.
- Moves receipt to `RETURNED_TO_SUPPLIER`.
- Does not create regular inventory.

## 11. `POST /api/v1/receipts/{id}/rtv`

Create RTV for failed quarantine quantity.

**Roles**: `WH_MANAGER`

```json
{
  "expectedVersion": 7,
  "note": "Supplier accepted RTV for QC failed quantity"
}
```

**Behavior**:

- Allowed when finalized unresolved failed quarantine quantity exists.
- Creates pending `RETURN_TO_VENDOR` adjustment and Debit Note.
- Does not reduce Quarantine inventory yet.
- Duplicate RTV returns the existing RTV document and HTTP 409.

## 12. `PUT /api/v1/receipts/{id}/rtv/confirm`

Confirm physical RTV handover.

**Roles**: `STOREKEEPER`

```json
{
  "expectedVersion": 8,
  "returnedQty": 2,
  "note": "Supplier handover confirmed at dock"
}
```

**Behavior**:

- Requires confirmed quantity equals unresolved failed quarantine quantity.
- Reduces Quarantine inventory only after confirmation.
- Duplicate confirmation returns HTTP 409 and does not reduce stock again.

## 13. `POST /api/v1/receipts/{id}/cancel`

Cancel a non-final receipt.

**Roles**: `PLANNER`, `WH_MANAGER`

```json
{
  "expectedVersion": 2,
  "reason": "Supplier shipment was entered against wrong PO"
}
```

**Behavior**:

- Allowed before final inventory impact.
- Requires reason.
- Sets status to `CANCELLED`; no physical delete.
- Writes `RECEIPT_CANCEL` audit.

## 14. `POST /api/v1/receipts/{id}/reopen`

WH_MANAGER reopen for correction before finalization.

**Roles**: `WH_MANAGER`

```json
{
  "expectedVersion": 6,
  "reason": "Correct QC values before final putaway"
}
```

**Behavior**:

- Allowed from `APPROVED`, `PARTIALLY_APPROVED`, or `RETURN_TO_SUPPLIER_PENDING` only before putaway/handover finalization.
- Requires reason.
- Clears QC and approval readiness, reverses any temporary Quarantine readiness that has not become finalized Quarantine inventory, and returns receipt to `DRAFT`.
- Writes `RECEIPT_REOPEN` audit.

## Error Response

```json
{
  "code": "PUTAWAY_QTY_MISMATCH",
  "message": "Putaway quantity must equal approved quantity",
  "currentStatus": "PARTIALLY_APPROVED",
  "allowedActions": ["PUTAWAY_WITH_APPROVED_QTY", "REOPEN"]
}
```
