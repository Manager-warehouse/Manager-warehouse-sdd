# Tasks: Feature 03 QC Classification And Storekeeper Review

- [ ] F03-T001 Add tests for Staff receive-and-QC save and status `PENDING_RECEIPT -> PENDING_STOREKEEPER_REVIEW`.
- [ ] F03-T002 Add tests for quality passed/failed mismatch against `actual_qty`.
- [ ] F03-T003 Add tests for failed quantity requiring reason.
- [ ] F03-T004 Implement unified receive-and-QC submit validation.
- [ ] F03-T005 Implement Storekeeper all-passed review approval to `QC_COMPLETED`.
- [ ] F03-T006 Implement Storekeeper failed-quantity review approval to `QC_FAILED`.
- [ ] F03-T007 Stage only failed quantity as Quarantine readiness on Storekeeper approval; do not finalize Quarantine stock at QC confirmation.
- [ ] F03-T008 Verify receive-and-QC save cannot reject whole receipt.
- [ ] F03-T009 Add audit `RECEIPT_RECEIVE_QC`, `RECEIPT_STOREKEEPER_REVIEW_APPROVE`, and `RECEIPT_STOREKEEPER_RECOUNT_REQUEST`.
- [ ] F03-T010 Add OpenAPI request bodies and integration tests for receive-and-QC validation `400`, stale version `409`, QC mismatch `422`, failed reason `422`, Storekeeper review, blocked statuses, no regular inventory/batch/putaway side effects, and receipt list labels "Cho thu kho duyet" / "Can dem lai" / "Da QC" / "QC co hang loi".
