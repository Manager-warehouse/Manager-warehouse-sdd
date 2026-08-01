# Tasks: 003-inbound-receipt-qc

**Status**: Draft - split by feature
**Input**: Root spec plus feature-level `spec.md`, `plan.md`, and `tasks.md`.

## Feature Task Lists

| Feature | Data | Tasks |
|---------|------|-------|
| F01 Receipt Drafting | [data-model.md](./features/feature-01-receipt-drafting/data-model.md) | [tasks.md](./features/feature-01-receipt-drafting/tasks.md) |
| F02 Staff Receipt Counting Within Receive & QC | [data-model.md](./features/feature-02-receipt-counting/data-model.md) | [tasks.md](./features/feature-02-receipt-counting/tasks.md) |
| F03 QC Classification And Storekeeper Review | [data-model.md](./features/feature-03-qc-classification/data-model.md) | [tasks.md](./features/feature-03-qc-classification/tasks.md) |
| F04 Manager Decision | [data-model.md](./features/feature-04-manager-decision/data-model.md) | [tasks.md](./features/feature-04-manager-decision/tasks.md) |
| F05 Putaway And Inventory | [data-model.md](./features/feature-05-putaway-inventory/data-model.md) | [tasks.md](./features/feature-05-putaway-inventory/tasks.md) |
| F06 Quarantine RTV | [data-model.md](./features/feature-06-quarantine-rtv/data-model.md) | [tasks.md](./features/feature-06-quarantine-rtv/tasks.md) |

## Cross-Feature Tasks

- [X] T-ROOT-001 Verify OpenAPI status enum includes `PENDING_MANAGER_APPROVAL`, `REVISION_REQUIRED`, `PENDING_RECEIPT`, `PENDING_STOREKEEPER_REVIEW`, `RECOUNT_REQUIRED`, `DRAFT`, `QC_COMPLETED`, `QC_FAILED`, `APPROVED`, `PARTIALLY_APPROVED`, `PUTAWAY_COMPLETED`, `RETURN_TO_SUPPLIER_PENDING`, `RETURNED_TO_SUPPLIER`, and `CANCELLED`.
- [X] T-ROOT-002 Verify every write endpoint documents HTTP `400`, `403`, `409`, and `422` with actionable error response containing `code`, `message`, optional `currentStatus`, and optional `allowedActions`.
- [X] T-ROOT-003 Verify Spec 004 outbound excludes pending receipt and Quarantine stock from available inventory.
- [X] T-ROOT-004 Verify Spec 008 AP/period closing uses accepted quantity and handles unresolved Quarantine.
- [ ] T-ROOT-005 Run the Quickstart smoke checklist for all-pass, partial-failed, whole-rejected, cancel, reopen, Storekeeper recount, Storekeeper review, and RTV receipt paths after endpoint-level coverage is complete.
- [X] T-ROOT-006 Verify all mutation audit actions contain actor, role, warehouse, before, and after.
- [X] T-ROOT-007 Implement and test cancel receipt endpoint before final inventory impact with `expectedVersion`, reason, `CANCELLED` status, no physical delete, and `RECEIPT_CANCEL` audit.
- [X] T-ROOT-008 Implement and test reopen receipt endpoint before putaway/handover finalization with `expectedVersion`, reason, QC/approval readiness reset, Quarantine readiness reversal, and `RECEIPT_REOPEN` audit.
- [X] T-ROOT-009 Verify canonical OpenAPI request bodies exist for create receipt, pre-receive approval, revision resubmission, receive-and-QC, Storekeeper review, approve with item unit costs, RTV create, RTV confirm, cancel, and reopen; implementation/test coverage remains tracked in feature tasks.
- [ ] T-ROOT-010 Add endpoint integration tests for validation `400`, stale version `409`, and business rule `422` across every Spec 003 mutation.

## Phase 1: Convergence

- [X] T008 CRITICAL Add immutable Flyway migration coverage for Spec 003 receipt statuses and quantity fields, including `PARTIALLY_APPROVED`, `PUTAWAY_COMPLETED`, `CANCELLED`, approved quantity, Quarantine readiness/finalized quantity, generated receipt-number uniqueness, and RTV idempotency per plan.
- [X] T009 CRITICAL Remove ratio-based automatic whole-receipt rejection from receive counting and QC confirmation so only WH_MANAGER rejection can move receipts to `RETURN_TO_SUPPLIER_PENDING`.
- [X] T010 Rework QC confirmation to require `expectedVersion`, validate quality passed/failed totals against `actual_qty`, require failed reasons, and avoid creating batch, inventory, location occupancy, supplier-return status, or `INVENTORY_UPDATE` audit before Storekeeper/manager decisions.
- [X] T011 Implement WH_MANAGER partial approval from `QC_FAILED` to `PARTIALLY_APPROVED`, including approved quantity from passed quantity, unit-cost validation, batch lineage, finalization of failed readiness into Quarantine stock, and `RECEIPT_PARTIAL_APPROVE` audit.
- [X] T012 Update whole-receipt rejection from `QC_FAILED` to finalize failed readiness into Quarantine stock while preserving traceability, and keep `QC_COMPLETED` rejection free of inventory/batch/RTV/Debit Note side effects.
- [X] T013 Update putaway to allow `PARTIALLY_APPROVED`, require allocation quantity equals approved quantity, increase regular inventory by approved quantity rather than `actual_qty`, and cover duplicate/idempotent putaway without double counting.
- [X] T014 Replace cancel/reopen query-param mutations with validated request bodies containing required `expectedVersion` and reason, enforce stale versions as HTTP `409`, and reverse only non-finalized Quarantine readiness on reopen.
- [X] T015 Align implemented controller routes and Swagger/OpenAPI contract for putaway, QC confirm, cancel, and reopen, including `400`, `403`, `409`, `422` response docs and actionable error payloads.
- [X] T016 Add or repair service and endpoint integration tests for all-pass, partial-failed, whole-rejected, cancel, reopen, RTV, validation, warehouse-scope, stale-version, and business-rule paths against the corrected Spec 003 state model.
- [X] T017 Implement frontend screen authorization for Receipt List, Receipt Create, "Nhan hang & QC dau vao", Putaway Plan, Quarantine Workspace, and Returns Workspace so visible actions match the Spec 003 allowed-role matrix.
- [X] T018 Add Jest/React Testing Library coverage for Spec 003 inbound screen authorization and hidden/disabled mutation actions across PLANNER, WH_STAFF, STOREKEEPER, WH_MANAGER, ACCOUNTANT, ACCT_MANAGER, and CEO roles.

## Phase 2: Convergence

- [X] T019 Align batch schema, entity, DTOs, OpenAPI, and tests around generated `batch_code` using `LOT-{WAREHOUSE_CODE}-{YYYYMMDD}-{SEQ}` while preserving existing batch lineage references.
- [X] T020 Enforce and test distinct batch creation per product receipt line and receipt event so the same product and supplier received in separate receipts never merge into one batch.
- [X] T021 Update inventory availability summaries and related tests to exclude Quarantine, In-Transit, locked, inactive, and non-bin rows from regular available stock.
- [X] T022 Move supplier billing notification and supplier invoice eligibility to accepted putaway quantity after `PUTAWAY_COMPLETED`, including partial-approved receipts and unresolved Quarantine handling.
- [ ] T023 Add endpoint/integration coverage for all-pass, partial-failed, whole-rejected, cancel, reopen, Storekeeper review/recount, RTV, validation 400, stale-version 409, business-rule 422, audit payloads, and distinct batch traceability; T-ROOT-005 is the final smoke pass after these endpoint tests exist.

## Phase 3: Convergence

- [X] T024 Remove user-entered source PO/source document fields from Spec 003 create receipt DTO/API contract implementation, including `source_reference`, `contact_person`, and `source_channel` validation/requirements, and add required `documentDate` input.
- [X] T025 Generate purchase receipt numbers as `PO-{YYYYMMDD}-{SEQ}` from request `documentDate` with daily sequence uniqueness.
- [X] T026 Remove duplicate active source-reference enforcement and stale duplicate-source error messaging from receipt creation, repository usage, controller Swagger docs, and save exception translation; replace with receipt-number conflict handling.
- [X] T027 Update the Receipt Create frontend to remove the source PO/source document field, stop submitting `source_reference`, `contact_person`, or `source_channel`, preserve `documentDate`, and display the generated `PO-{YYYYMMDD}-{SEQ}` receipt number after creation/list/detail.
- [X] T028 Update backend controller/service tests, frontend mocks, and frontend tests to expect generated `PO-{YYYYMMDD}-{SEQ}`, omit user-entered source PO/source document/contact/channel fields, assert `RECEIPT_CREATE` audit contains generated receipt number and document date, and cover validation/error docs for the new create receipt contract.

## Phase 4: Pre-Receive Manager Approval

- [X] T029 Add immutable Flyway migration for `PENDING_MANAGER_APPROVAL`, `REVISION_REQUIRED`, `pre_receive_approved_by`, `pre_receive_approved_at`, `pre_receive_rejection_reason`, and `RECEIPT_PRE_RECEIVE_APPROVE` / `RECEIPT_PRE_RECEIVE_REJECT` audit actions.
- [X] T030 Change create receipt implementation to persist new purchase receipts as `PENDING_MANAGER_APPROVAL` instead of `PENDING_RECEIPT`, with no inventory, batch, quarantine, AP, or Debit Note side effects.
- [X] T031 Add WH_MANAGER pre-receive approval mutation with `expectedVersion`: `APPROVE` moves `PENDING_MANAGER_APPROVAL` to `PENDING_RECEIPT`; `REJECT` requires reason and returns the receipt to `REVISION_REQUIRED`.
- [X] T032 Add PLANNER revision/resubmission mutation from `REVISION_REQUIRED` back to `PENDING_MANAGER_APPROVAL`.
- [X] T033 Block WH_STAFF count and QC submission for receipts in `PENDING_MANAGER_APPROVAL` or `REVISION_REQUIRED`, returning business-rule error `RECEIPT_PENDING_MANAGER_APPROVAL` without partial save; STOREKEEPER is the downstream review actor, not the count-entry actor.
- [X] T034 Add service, integration, OpenAPI/Swagger, audit payload, migration, and frontend authorization coverage for pre-receive approval, rejection, revision resubmission, stale version, forbidden warehouse scope, and blocked count before manager approval.

## Phase 5: Staff Unified Receive & Inbound QC Screen

- [X] T035 Replace old ReceiptReceive and QCInbound routes/actions with the unified "Nhan hang & QC dau vao" Staff entry screen, or route both old actions to the new screen without duplicating logic.
- [X] T036 Implement table columns Ma hang, Ten hang, SL du kien, SL thuc nhan, QC dat, QC loi, Ly do loi, Ket qua with readonly expected quantity and clear mismatch warning.
- [X] T037 Implement UI defaults: entering SL thuc nhan sets QC dat to SL thuc nhan and QC loi to 0; editing QC loi recalculates QC dat as SL thuc nhan minus QC loi.
- [X] T038 Implement `PUT /api/v1/receipts/{id}/receive-qc` with `expectedVersion`, WH_STAFF-only entry, allowed statuses `PENDING_RECEIPT`, `RECOUNT_REQUIRED`, and `DRAFT`, and blocked statuses `PENDING_MANAGER_APPROVAL`, `REVISION_REQUIRED`, `PENDING_STOREKEEPER_REVIEW`, `QC_COMPLETED`, `QC_FAILED`, `APPROVED`, `PARTIALLY_APPROVED`, `PUTAWAY_COMPLETED`, `RETURN_TO_SUPPLIER_PENDING`, `RETURNED_TO_SUPPLIER`, and `CANCELLED`.
- [X] T039 Validate `quality_passed_qty + quality_failed_qty = actual_qty`, require QC failure reason when failed quantity exists, and reject stale `expectedVersion` with HTTP `409`.
- [X] T040 Store `over_received_qty` when `actual_qty > expected_qty` without creating regular inventory, batch, putaway, supplier invoice, Debit Note, RTV, supplier-return status, or finalized Quarantine stock.
- [X] T041 On successful WH_STAFF receive-and-QC save, set status to `PENDING_STOREKEEPER_REVIEW`, return to Receipt List, and display "Cho thu kho duyet".
- [X] T042 When resubmitting receive-and-QC after `RECOUNT_REQUIRED`, clear old QC data and non-finalized Quarantine readiness before saving replacement values.
- [X] T043 Write `RECEIPT_RECEIVE_QC` audit with Staff actor, role, warehouse, before/after receipt status, and before/after item quantities.
- [X] T044 Add canonical service and integration tests for Staff submit, QC quantity mismatch, failed reason required, no inventory/batch/putaway side effects, optimistic locking, and all blocked receive-and-QC statuses from T038.

## Phase 5A: Storekeeper Count/QC Review

- [X] T045 Add `PENDING_STOREKEEPER_REVIEW` and `RECOUNT_REQUIRED` status/migration/OpenAPI coverage and receipt list labels "Cho thu kho duyet" / "Can dem lai".
- [X] T046 Implement `PUT /api/v1/receipts/{id}/storekeeper-review` for STOREKEEPER `APPROVE` and `REQUEST_RECOUNT` decisions with `expectedVersion`, reason required for recount, persisted `recount_reason` visible to WH_STAFF, role plus warehouse scope, and audit actions `RECEIPT_STOREKEEPER_REVIEW_APPROVE` / `RECEIPT_STOREKEEPER_RECOUNT_REQUEST`.
- [X] T047 On Storekeeper approval, move all-passed receipts to `QC_COMPLETED` and receipts with failed quantity to `QC_FAILED`; stage failed quantity as Quarantine readiness only at this point.
- [X] T048 Block WH_MANAGER approve/reject while receipt is `PENDING_STOREKEEPER_REVIEW` or `RECOUNT_REQUIRED`, returning `STOREKEEPER_REVIEW_PENDING`.
- [X] T049 Ensure putaway requires STOREKEEPER-selected regular bin/location per allocation and keeps bin-capacity validation before inventory mutation.
