# Tasks: Feature 01 Receipt Drafting

- [ ] F01-T001 Add/verify DTO validation for create purchase receipt.
- [ ] F01-T002 Add/verify generated receipt number `PO-{YYYYMMDD}-{SEQ}` with uniqueness and sequence conflict handling.
- [ ] F01-T003 Implement create receipt status `PENDING_MANAGER_APPROVAL`.
- [ ] F01-T004 Ensure create receipt has no inventory, batch, quarantine, AP, or Debit Note side effects.
- [ ] F01-T005 Add `RECEIPT_CREATE` audit.
- [ ] F01-T006 Implement WH_MANAGER pre-receive approval: approve `PENDING_MANAGER_APPROVAL` to `PENDING_RECEIPT`, reject to `REVISION_REQUIRED` with reason.
- [ ] F01-T007 Implement PLANNER revision/resubmission from `REVISION_REQUIRED` to `PENDING_MANAGER_APPROVAL`.
- [ ] F01-T008 Add `RECEIPT_PRE_RECEIVE_APPROVE`, `RECEIPT_PRE_RECEIVE_REJECT`, and `RECEIPT_PRE_RECEIVE_RESUBMIT` audit.
- [ ] F01-T009 Add service tests for success with generated receipt number, sequence conflict handling, invalid expected quantity, and pre-receive approval/rejection/resubmission transitions.
- [ ] F01-T010 Add integration tests for create receipt with warehouse scope and blocked count before WH_MANAGER approval.
- [ ] F01-T011 Add implementation Swagger/OpenAPI annotations and integration tests for create receipt validation `400`, duplicate `409`, forbidden warehouse `403`, no `expectedVersion` on create, pre-receive approval errors, and revision resubmission errors.
