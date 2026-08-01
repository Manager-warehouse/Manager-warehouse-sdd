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

## 4. `PUT /api/v1/receipts/{id}/receive-qc`

Submit or correct physical receipt count and inbound QC classification from the unified "Nhan hang & QC dau vao" Staff entry screen.

**Roles**: `WH_STAFF`

```json
{
  "expectedVersion": 0,
  "items": [
    {
      "receiptItemId": 101,
      "actualQty": 52,
      "qualityPassedQty": 50,
      "qualityFailedQty": 2,
      "failureReason": "Cracked handle"
    }
  ]
}
```

**Behavior**:

- Allowed in `PENDING_RECEIPT`, `RECOUNT_REQUIRED`, and `DRAFT`; rejected in `PENDING_MANAGER_APPROVAL`, `REVISION_REQUIRED`, `PENDING_STOREKEEPER_REVIEW`, `QC_COMPLETED`, `QC_FAILED`, `APPROVED`, `PARTIALLY_APPROVED`, `PUTAWAY_COMPLETED`, `RETURN_TO_SUPPLIER_PENDING`, `RETURNED_TO_SUPPLIER`, or `CANCELLED`.
- Requires exactly one item payload per receipt item.
- Requires non-negative integer `actualQty`, `qualityPassedQty`, and `qualityFailedQty`.
- Stores `actual_qty` as submitted and computes `over_received_qty = max(actual_qty - expected_qty, 0)`.
- Validates `qualityPassedQty + qualityFailedQty = actualQty`.
- Requires `failureReason` when `qualityFailedQty > 0`.
- If correcting after `RECOUNT_REQUIRED`, clears old QC data and non-finalized Quarantine readiness before writing the new values.
- Status becomes `PENDING_STOREKEEPER_REVIEW`; WH_MANAGER decision is not available until STOREKEEPER approves the count/QC result.
- Does not create regular inventory, batch, putaway, supplier invoice, Debit Note, RTV, supplier-return status, finalized Quarantine stock, or warehouse-location occupancy.
- Writes `RECEIPT_RECEIVE_QC` audit with Staff actor, role, warehouse, before/after status, and before/after item quantities.
- QC save cannot reject the whole receipt.

## 5. `PUT /api/v1/receipts/{id}/storekeeper-review`

Review submitted Staff count/QC.

**Roles**: `WH_MANAGER`

```json
{
  "expectedVersion": 1,
  "decision": "APPROVE",
  "reason": "Count and QC verified"
}
```

**Behavior**:

- Allowed only from `PENDING_STOREKEEPER_REVIEW`.
- `REQUEST_RECOUNT` decision requires reason, stores `recount_reason` for WH_STAFF visibility, moves receipt to `RECOUNT_REQUIRED`, and writes `RECEIPT_STOREKEEPER_RECOUNT_REQUEST`.
- `APPROVE` moves all-pass receipts to `QC_COMPLETED`; if any `qualityFailedQty > 0`, moves receipt to `QC_FAILED` and stages only failed quantity as Quarantine readiness.
- Does not create regular inventory, batch, putaway, supplier invoice, Debit Note, RTV, supplier-return status, finalized Quarantine stock, or warehouse-location occupancy.
- Writes `RECEIPT_STOREKEEPER_REVIEW_APPROVE` audit for approval with actor, role, warehouse, before/after status, and reviewed item quantities.

## 6. `PUT /api/v1/receipts/{id}/approve`

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

- Allowed only after Storekeeper review approval has produced `QC_COMPLETED` or `QC_FAILED`.
- `QC_COMPLETED` -> `APPROVED`, `approved_qty = actual_qty`.
- `QC_FAILED` -> `PARTIALLY_APPROVED`, `approved_qty = quality_passed_qty`.
- Decision is rejected for `PENDING_STOREKEEPER_REVIEW` or `RECOUNT_REQUIRED`.
- Requires positive `unit_cost` for approved items.
- Resolves one generated `batchCode` per approved product receipt line but does not increase regular inventory.
- Returns `NO_PASSED_QUANTITY_TO_APPROVE` if `QC_FAILED` has no passed quantity.

## 7. `PUT /api/v1/receipts/{id}/reject`

Reject whole receipt.

**Roles**: `WH_MANAGER`

```json
{
  "expectedVersion": 2,
  "reason": "Supplier delivered wrong model"
}
```

**Behavior**:

- Allowed only after Storekeeper review approval has produced `QC_COMPLETED` or `QC_FAILED`.
- Requires reason.
- Moves receipt to `RETURN_TO_SUPPLIER_PENDING`.
- Does not create regular inventory.
- If failed quarantine quantity already exists, it remains traceable and must be resolved by supplier handover/RTV/disposal linkage.

## 8. `POST /api/v1/receipts/{id}/putaway`

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
- STOREKEEPER must provide the target bin/location per allocation; locations must be active regular bins in the receipt warehouse.
- Optional `expectedBatchCode` may be supplied per allocation for scan/manual confirmation; when present it must match the receipt item's generated batch.
- Validates bin capacity.
- Increases regular inventory by approved quantity only.
- Sets status to `PUTAWAY_COMPLETED`.
- Duplicate putaway returns `PUTAWAY_ALREADY_COMPLETED`.

## 9. `PUT /api/v1/receipts/{id}/return-to-supplier/confirm`

Confirm handover for whole-receipt rejection.

**Roles**: `STOREKEEPER`

**Behavior**:

- Allowed only from `RETURN_TO_SUPPLIER_PENDING`.
- Moves receipt to `RETURNED_TO_SUPPLIER`.
- Does not create regular inventory.

## 10. `POST /api/v1/receipts/{id}/rtv`

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

## 11. `PUT /api/v1/receipts/{id}/rtv/confirm`

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

## 12. `POST /api/v1/receipts/{id}/cancel`

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

## 13. `POST /api/v1/receipts/{id}/reopen`

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
