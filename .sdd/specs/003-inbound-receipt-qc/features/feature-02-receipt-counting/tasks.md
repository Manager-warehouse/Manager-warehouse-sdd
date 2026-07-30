# Tasks: Feature 02 Receipt Counting

- [ ] F02-T001 Add tests for complete count and status `PENDING_RECEIPT -> DRAFT`.
- [ ] F02-T002 Add tests for incomplete, duplicate, invalid, and wrong receipt item counts.
- [ ] F02-T003 Add tests for blocking count submission while receipt is `PENDING_MANAGER_APPROVAL` or `REVISION_REQUIRED`.
- [ ] F02-T004 Add tests for over-received quantity staying outside inventory.
- [ ] F02-T005 Add tests for count correction clearing QC and returning to `DRAFT`.
- [ ] F02-T006 Implement all-or-nothing count validation.
- [ ] F02-T007 Implement count variance and over-received storage.
- [ ] F02-T008 Implement count correction with `expectedVersion`.
- [ ] F02-T009 Add audit `RECEIPT_RECEIVE` and `RECEIPT_CORRECTION`.
- [ ] F02-T010 Add integration tests for receive/correction validation `400`, stale version `409`, invalid-state `422`, blocked pre-approval/revision-required count `422`, and clearing only non-finalized Quarantine readiness.
