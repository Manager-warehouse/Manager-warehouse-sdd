# Plan: Feature 02 Staff Receipt Counting Within Receive & QC

## Scope

Implement complete WH_STAFF physical count submit/correction as part of the unified "Nhan hang & QC dau vao" mutation, with variance preservation and old QC/quarantine readiness reset before replacement values are saved after Storekeeper recount request.

## Implementation Notes

- Validate complete receive-and-QC payload atomically.
- Allow Staff submit only from `PENDING_RECEIPT`, `RECOUNT_REQUIRED`, or legacy `DRAFT`.
- Move successful Staff submit to `PENDING_STOREKEEPER_REVIEW`.
- Preserve expected-vs-actual variance.
- Store over-received quantity outside inventory.
- Clear old QC fields and non-finalized Quarantine readiness on correction after `RECOUNT_REQUIRED`.
- Use `expectedVersion`.
- Emit `RECEIPT_RECEIVE_QC` with Staff actor, before/after receipt status, and item quantities.
- Do not create regular inventory, batch, putaway, supplier invoice, Debit Note, RTV, or finalized Quarantine stock.

## Verification

- Unit tests for Staff submit, incomplete count, invalid quantity, wrong item, and over-received quantity.
- Unit test for resubmission after `RECOUNT_REQUIRED` clearing old QC/quarantine readiness before replacement values.
- Integration tests for stale version and blocked `PENDING_MANAGER_APPROVAL`, `REVISION_REQUIRED`, and `PENDING_STOREKEEPER_REVIEW`.
