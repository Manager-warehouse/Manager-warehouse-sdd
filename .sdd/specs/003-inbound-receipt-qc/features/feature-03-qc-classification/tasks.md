# Tasks: Feature 03 QC Classification

- [ ] F03-T001 Add tests for QC sample mismatch.
- [ ] F03-T002 Add tests for quality passed/failed mismatch.
- [ ] F03-T003 Add tests for failed quantity requiring reason.
- [ ] F03-T004 Implement QC submit validation.
- [ ] F03-T005 Implement all-passed confirmation to `QC_COMPLETED`.
- [ ] F03-T006 Implement failed-quantity confirmation to `QC_FAILED`.
- [ ] F03-T007 Stage only failed quantity as Quarantine readiness; do not finalize Quarantine stock at QC confirmation.
- [ ] F03-T008 Verify QC confirmation cannot reject whole receipt.
- [ ] F03-T009 Add audit `RECEIPT_QC_SUBMIT` and `RECEIPT_QC_CONFIRM`.
- [ ] F03-T010 Add OpenAPI request body and integration tests for QC submit/confirm validation `400`, stale version `409`, QC mismatch `422`, and no regular inventory impact.
