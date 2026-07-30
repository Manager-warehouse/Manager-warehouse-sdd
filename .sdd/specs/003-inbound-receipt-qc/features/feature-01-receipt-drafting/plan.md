# Plan: Feature 01 Receipt Drafting

## Scope

Create purchase receipt in `PENDING_MANAGER_APPROVAL` without inventory impact, requiring WH_MANAGER approval before warehouse counting.

## Implementation Notes

- Add/verify create receipt endpoint and DTO validation.
- Add/verify WH_MANAGER pre-receive approval endpoint that moves `PENDING_MANAGER_APPROVAL` to `PENDING_RECEIPT` or rejects to `REVISION_REQUIRED` with reason.
- Add/verify PLANNER revision/resubmission endpoint that moves `REVISION_REQUIRED` back to `PENDING_MANAGER_APPROVAL`.
- Enforce supplier, warehouse, item, expected quantity, and receipt document date validation.
- Generate unique receipt number as `PO-{YYYYMMDD}-{SEQ}`; do not accept source PO/source document, contact person, or source channel fields from the create form.
- Do not create batch, inventory, quarantine, AP, or Debit Note.
- Emit `RECEIPT_CREATE` audit.
- Emit `RECEIPT_PRE_RECEIVE_APPROVE` / `RECEIPT_PRE_RECEIVE_REJECT` audit for the manager decision and `RECEIPT_PRE_RECEIVE_RESUBMIT` for PLANNER correction.

## Verification

- Unit tests for generated receipt number, sequence conflict retry/handling, invalid expected quantity, pre-receive approval/rejection/resubmission transitions, and audit payloads containing generated receipt number and document date.
- Integration test for successful create.
- Integration tests proving WH_STAFF/STOREKEEPER cannot count `PENDING_MANAGER_APPROVAL` or `REVISION_REQUIRED`.
- Authorization test for warehouse scope.
