# Tasks: Feature 02 Receipt Counting Within Receive & QC

- [ ] F02-T001 Add count-focused unit tests for receive-and-QC save storing `actual_qty` and `over_received_qty`.
- [ ] F02-T002 Add tests for incomplete, duplicate, invalid, and wrong receipt item counts.
- [ ] F02-T003 Add tests for blocking receive-and-QC submission while receipt is `PENDING_MANAGER_APPROVAL`, `REVISION_REQUIRED`, `PENDING_STOREKEEPER_REVIEW`, or manager-finalized.
- [ ] F02-T004 Add tests for over-received quantity staying outside inventory.
- [ ] F02-T005 Add tests for receive-and-QC resubmission from `RECOUNT_REQUIRED`, including visible `recount_reason`, clearing old QC/quarantine readiness before writing new values, and returning to `PENDING_STOREKEEPER_REVIEW`.
- [ ] F02-T006 Implement all-or-nothing count validation.
- [ ] F02-T007 Implement count variance and over-received storage.
- [ ] F02-T008 Implement receive-and-QC resubmission from `RECOUNT_REQUIRED` with `expectedVersion`.
- [ ] F02-T009 Add audit `RECEIPT_RECEIVE_QC` with actor, role, warehouse, before/after status, and before/after item quantities.
- [ ] F02-T010 Add integration tests for receive-and-QC validation `400`, stale version `409`, invalid-state `422`, blocked pre-approval/revision-required/pending-Storekeeper-review submission `422`, and clearing only non-finalized Quarantine readiness.
