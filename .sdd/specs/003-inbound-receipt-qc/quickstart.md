# Quickstart: 003-inbound-receipt-qc

Use this checklist to verify the canonical Spec 003 flow after implementation.

## Automated Checks

```bash
mvn test -Dtest=ReceiptServiceTest,ReceiptQcServiceTest,ReceiptApprovalServiceTest,PutawayServiceTest,QuarantineRtvServiceTest
```

Expected coverage:

- Count variance is retained and never auto-rejects.
- PLANNER-created receipts start in `PENDING_MANAGER_APPROVAL`; counting is blocked until WH_MANAGER approval moves the receipt to `PENDING_RECEIPT`.
- WH_MANAGER rejection before receiving moves the receipt to `REVISION_REQUIRED`; counting and QC stay blocked until PLANNER resubmits and WH_MANAGER approves.
- WH_STAFF receive-and-QC submit -> `PENDING_STOREKEEPER_REVIEW`.
- STOREKEEPER recount request -> `RECOUNT_REQUIRED`; WH_STAFF resubmission returns to `PENDING_STOREKEEPER_REVIEW`.
- STOREKEEPER approves all-passed review -> `QC_COMPLETED`.
- STOREKEEPER approves review with failed quantity -> `QC_FAILED`; failed quantity is staged as Quarantine readiness only.
- WH_MANAGER full approve -> `APPROVED`; regular inventory unchanged.
- WH_MANAGER approves passed quantity from `QC_FAILED` -> `PARTIALLY_APPROVED`; failed quantity becomes finalized Quarantine stock.
- STOREKEEPER-selected putaway from `APPROVED` or `PARTIALLY_APPROVED` increases regular inventory by `approved_qty` only.
- Whole-receipt rejection requires `WH_MANAGER` role, warehouse scope, version, and reason after Storekeeper review approval.
- RTV create generates pending adjustment/Debit Note and does not reduce Quarantine stock.
- RTV confirm reduces Quarantine stock exactly once.
- Duplicate putaway/RTV return HTTP 409.
- Request DTO validation failures return HTTP 400.
- Business rule violations return HTTP 422.
- Existing receipt mutations require `expectedVersion`; create receipt does not.

## Manual Flow 1: All Goods Passed

1. PLANNER creates purchase receipt -> `PENDING_MANAGER_APPROVAL`.
2. WH_MANAGER approves for receiving -> `PENDING_RECEIPT`.
3. WH_STAFF submits complete count and QC -> `PENDING_STOREKEEPER_REVIEW`.
4. STOREKEEPER approves count/QC review -> `QC_COMPLETED`.
5. WH_MANAGER approves -> `APPROVED`; available regular stock is unchanged.
6. STOREKEEPER selects regular bin/location and putaways goods -> `PUTAWAY_COMPLETED`; available regular stock increases.

## Manual Flow 2: Some Goods Failed QC

1. WH_STAFF submits count and QC with passed and failed quantities -> `PENDING_STOREKEEPER_REVIEW`.
2. STOREKEEPER approves count/QC review -> `QC_FAILED`.
3. Verify failed quantity is staged as Quarantine readiness and excluded from outbound available stock.
4. WH_MANAGER approves passed quantity -> `PARTIALLY_APPROVED`; failed quantity becomes finalized Quarantine stock.
5. STOREKEEPER selects regular bin/location and putaways approved passed quantity -> `PUTAWAY_COMPLETED`.
6. WH_MANAGER creates RTV for failed quantity -> pending `RETURN_TO_VENDOR` adjustment and Debit Note.
7. STOREKEEPER confirms RTV handover -> Quarantine stock decreases.

## Manual Flow 3: Storekeeper Recount

1. WH_STAFF submits count and QC -> `PENDING_STOREKEEPER_REVIEW`.
2. STOREKEEPER rejects review with recount reason -> `RECOUNT_REQUIRED`.
3. WH_STAFF submits corrected count and QC -> `PENDING_STOREKEEPER_REVIEW`.
4. STOREKEEPER approves review -> `QC_COMPLETED` or `QC_FAILED`.

## Manual Flow 4: Correction And Cancel

1. Before Storekeeper approval, STOREKEEPER requests correction -> `RECOUNT_REQUIRED`; Staff resubmission clears old QC fields and non-finalized Quarantine readiness.
2. Before final inventory impact, PLANNER or WH_MANAGER cancels with reason -> `CANCELLED`; no physical delete.
3. Before putaway or supplier handover finalization, WH_MANAGER reopens with reason -> `DRAFT`; QC and approval readiness are cleared.

## Manual Flow 5: Pre-Receive Rejected For Correction

1. PLANNER creates purchase receipt -> `PENDING_MANAGER_APPROVAL`.
2. WH_MANAGER rejects with reason -> `REVISION_REQUIRED`.
3. WH_STAFF count attempt returns `RECEIPT_PENDING_MANAGER_APPROVAL`.
4. PLANNER corrects and resubmits -> `PENDING_MANAGER_APPROVAL`.
5. WH_MANAGER approves -> `PENDING_RECEIPT`.

## Manual Flow 6: Whole Receipt Rejected

1. Receipt is Storekeeper-approved as `QC_COMPLETED` or `QC_FAILED`.
2. WH_MANAGER rejects with reason -> `RETURN_TO_SUPPLIER_PENDING`.
3. WH_MANAGER confirms supplier handover -> `RETURNED_TO_SUPPLIER`.
4. Verify no regular inventory was created.

## Error Checks

- Counting before manager approval or while revision is required -> `RECEIPT_PENDING_MANAGER_APPROVAL`.
- Staff overwrite while receipt is `PENDING_STOREKEEPER_REVIEW` -> `RECEIPT_PENDING_STOREKEEPER_REVIEW`.
- Storekeeper recount request without reason -> `STOREKEEPER_REVIEW_REASON_REQUIRED`.
- WH_MANAGER decision before Storekeeper approval -> `STOREKEEPER_REVIEW_PENDING`.
- Missing rejection reason -> `REJECTION_REASON_REQUIRED`.
- Stale version -> `INVENTORY_VERSION_CONFLICT`.
- QC quantity mismatch -> `QC_QUANTITY_MISMATCH`.
- Approve `QC_FAILED` with zero passed quantity -> `NO_PASSED_QUANTITY_TO_APPROVE`.
- Putaway quantity not equal approved quantity -> `PUTAWAY_QTY_MISMATCH`.
- Putaway into Quarantine/wrong warehouse/inactive bin -> `PUTAWAY_LOCATION_INVALID`.
- Bin over capacity -> `BIN_CAPACITY_EXCEEDED`.
- Duplicate RTV -> `RTV_ALREADY_EXISTS`.
