# Plan: Feature 03 QC Classification And Storekeeper Review

## Scope

Implement Staff QC classification validation inside the unified "Nhan hang & QC dau vao" save, then implement Storekeeper review approval/recount decision before WH_MANAGER approval is available.

## Implementation Notes

- Validate quality quantity sums against `actual_qty`.
- Require failure reason for failed quantity.
- Staff save moves receipt to `PENDING_STOREKEEPER_REVIEW`.
- Storekeeper recount request moves receipt to `RECOUNT_REQUIRED` and requires reason.
- Storekeeper approval moves all-passed receipt to `QC_COMPLETED`.
- Storekeeper approval moves any-failed receipt to `QC_FAILED`.
- Create/update Quarantine readiness only for failed quantity when Storekeeper approves to `QC_FAILED`.
- Keep passed quantity outside regular available stock.
- Emit `RECEIPT_RECEIVE_QC`, `RECEIPT_STOREKEEPER_REVIEW_APPROVE`, and `RECEIPT_STOREKEEPER_RECOUNT_REQUEST`.
- Do not create batch, putaway, supplier invoice, Debit Note, RTV, supplier-return status, finalized Quarantine stock, or warehouse-location occupancy.

## Verification

- Tests for Staff submit to `PENDING_STOREKEEPER_REVIEW`.
- Tests for Storekeeper recount request and reason-required error.
- Tests for Storekeeper all-passed approval.
- Tests for Storekeeper failed quantity approval to Quarantine readiness only.
- Tests for QC mismatch and failed-reason errors.
- Verify no regular inventory, batch, putaway, supplier invoice, Debit Note, or RTV impact.
