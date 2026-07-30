# Tasks: 003-inbound-receipt-qc

**Status**: Draft - split by feature  
**Input**: Root spec plus feature-level `spec.md`, `plan.md`, and `tasks.md`.

## Feature Task Lists

| Feature | Data | Tasks |
|---------|------|-------|
| F01 Receipt Drafting | [data-model.md](./features/feature-01-receipt-drafting/data-model.md) | [tasks.md](./features/feature-01-receipt-drafting/tasks.md) |
| F02 Receipt Counting | [data-model.md](./features/feature-02-receipt-counting/data-model.md) | [tasks.md](./features/feature-02-receipt-counting/tasks.md) |
| F03 QC Classification | [data-model.md](./features/feature-03-qc-classification/data-model.md) | [tasks.md](./features/feature-03-qc-classification/tasks.md) |
| F04 Manager Decision | [data-model.md](./features/feature-04-manager-decision/data-model.md) | [tasks.md](./features/feature-04-manager-decision/tasks.md) |
| F05 Putaway And Inventory | [data-model.md](./features/feature-05-putaway-inventory/data-model.md) | [tasks.md](./features/feature-05-putaway-inventory/tasks.md) |
| F06 Quarantine RTV | [data-model.md](./features/feature-06-quarantine-rtv/data-model.md) | [tasks.md](./features/feature-06-quarantine-rtv/tasks.md) |

## Cross-Feature Tasks

- [X] T-ROOT-001 Verify OpenAPI status enum includes `PENDING_MANAGER_APPROVAL`, `REVISION_REQUIRED`, `PARTIALLY_APPROVED`, `PUTAWAY_COMPLETED`, `RETURN_TO_SUPPLIER_PENDING`, `RETURNED_TO_SUPPLIER`, and `CANCELLED`.
- [X] T-ROOT-002 Verify every write endpoint documents HTTP `400`, `403`, `409`, and `422` with actionable error response containing `code`, `message`, optional `currentStatus`, and optional `allowedActions`.
- [ ] T-ROOT-003 Verify Spec 004 outbound excludes pending receipt and Quarantine stock from available inventory.
- [ ] T-ROOT-004 Verify Spec 008 AP/period closing uses accepted quantity and handles unresolved Quarantine.
- [ ] T-ROOT-005 Run complete end-to-end tests for all-pass, partial-failed, whole-rejected, cancel, reopen, and RTV receipt paths.
- [ ] T-ROOT-006 Verify all mutation audit actions contain actor, role, warehouse, before, and after.
- [X] T-ROOT-007 Implement and test cancel receipt endpoint before final inventory impact with `expectedVersion`, reason, `CANCELLED` status, no physical delete, and `RECEIPT_CANCEL` audit.
- [X] T-ROOT-008 Implement and test reopen receipt endpoint before putaway/handover finalization with `expectedVersion`, reason, QC/approval readiness reset, Quarantine readiness reversal, and `RECEIPT_REOPEN` audit.
- [X] T-ROOT-009 Verify canonical OpenAPI request bodies exist for create receipt, pre-receive approval, revision resubmission, submit QC, approve with item unit costs, RTV create, RTV confirm, cancel, and reopen; implementation/test coverage remains tracked in feature tasks.
- [ ] T-ROOT-010 Add endpoint integration tests for validation `400`, stale version `409`, and business rule `422` across every Spec 003 mutation.

## Phase 1: Convergence

- [X] T008 CRITICAL Add immutable Flyway migration coverage for Spec 003 receipt statuses and quantity fields, including `PARTIALLY_APPROVED`, `PUTAWAY_COMPLETED`, `CANCELLED`, approved quantity, Quarantine readiness/finalized quantity, generated receipt-number uniqueness, and RTV idempotency per plan: migration impact and Constitution IX (partial)
- [X] T009 CRITICAL Remove ratio-based automatic whole-receipt rejection from receive counting and QC confirmation so only WH_MANAGER rejection can move receipts to `RETURN_TO_SUPPLIER_PENDING` per Shared Business Rules, F02-FR-005, and F03-FR-005 (contradicts)
- [X] T010 Rework QC confirmation to require `expectedVersion`, validate quality passed/failed totals against `actual_qty`, require failed reasons, move failed receipts only to `QC_FAILED`, and stage failed quantity as Quarantine readiness without creating batch, inventory, location occupancy, supplier-return status, or `INVENTORY_UPDATE` audit per F03-FR-001 through F03-FR-005 (contradicts)
- [X] T011 Implement WH_MANAGER partial approval from `QC_FAILED` to `PARTIALLY_APPROVED`, including approved quantity from passed quantity, unit-cost validation, batch lineage, finalization of failed readiness into Quarantine stock, and `RECEIPT_PARTIAL_APPROVE` audit per F04-FR-001 through F04-FR-005 (missing)
- [X] T012 Update whole-receipt rejection from `QC_FAILED` to finalize failed readiness into Quarantine stock while preserving traceability, and keep `QC_COMPLETED` rejection free of inventory/batch/RTV/Debit Note side effects per F04-FR-004 and F04-FR-005 (partial)
- [X] T013 Update putaway to allow `PARTIALLY_APPROVED`, require allocation quantity equals approved quantity, increase regular inventory by approved quantity rather than `actual_qty`, and cover duplicate/idempotent putaway without double counting per F05-FR-001 through F05-FR-006 (partial)
- [X] T014 Replace cancel/reopen query-param mutations with validated request bodies containing required `expectedVersion` and reason, enforce stale versions as HTTP `409`, and reverse only non-finalized Quarantine readiness on reopen per Shared Business Rules, T-ROOT-007, and T-ROOT-008 (partial)
- [X] T015 Align implemented controller routes and Swagger/OpenAPI contract for putaway, QC confirm, cancel, and reopen, including `400`, `403`, `409`, `422` response docs and actionable error payloads per SC-001, T-ROOT-002, and T-ROOT-009 (partial)
- [X] T016 Add or repair service and endpoint integration tests for all-pass, partial-failed, whole-rejected, cancel, reopen, RTV, validation, warehouse-scope, stale-version, and business-rule paths against the corrected Spec 003 state model per SC-002 and T-ROOT-010 (partial)
- [X] T017 Implement frontend screen authorization for Receipt List, Receipt Create, Receipt Receive, QC Inbound, Putaway Plan, Quarantine Workspace, and Returns Workspace so visible actions match the Spec 003 allowed-role matrix, including view-only behavior for CEO/finance/read-only roles per spec: Screen Authorization (partial)
- [X] T018 Add Jest/React Testing Library coverage for Spec 003 inbound screen authorization and hidden/disabled mutation actions across PLANNER, WH_STAFF, STOREKEEPER, WH_MANAGER, ACCOUNTANT, ACCT_MANAGER, and CEO roles per Constitution Section 9 and plan: Frontend React/Jest context (missing)

## Phase 2: Convergence

- [X] T019 Align batch schema, entity, DTOs, OpenAPI, and tests around generated `batch_code` using `LOT-{WAREHOUSE_CODE}-{YYYYMMDD}-{SEQ}` while preserving existing batch lineage references per Clarification 2026-07-28 and SC-005 (partial)
- [X] T020 Enforce and test distinct batch creation per product receipt line and receipt event so the same product and supplier received in separate receipts never merge into one batch per Batch Identity and SC-005 (contradicts)
- [X] T021 Update inventory availability summaries and related tests to exclude Quarantine, In-Transit, locked, inactive, and non-bin rows from regular available stock per SC-004 and T-ROOT-003 (partial)
- [X] T022 Move supplier billing notification and supplier invoice eligibility to accepted putaway quantity after `PUTAWAY_COMPLETED`, including partial-approved receipts and unresolved Quarantine handling per T-ROOT-004 and plan: Cross-Feature Verification (contradicts)
- [ ] T023 Add complete endpoint/integration coverage for all-pass, partial-failed, whole-rejected, cancel, reopen, RTV, validation 400, stale-version 409, business-rule 422, audit payloads, and distinct batch traceability per SC-002, T-ROOT-005, T-ROOT-006, and T-ROOT-010 (partial)

## Phase 3: Convergence

- [X] T024 Remove user-entered source PO/source document fields from Spec 003 create receipt DTO/API contract implementation, including `source_reference`, `contact_person`, and `source_channel` validation/requirements, and add required `documentDate` input per F01-FR-005 and `POST /api/v1/receipts` contract (contradicts)
- [X] T025 Generate purchase receipt numbers as `PO-{YYYYMMDD}-{SEQ}` from request `documentDate` with daily sequence uniqueness, replacing the current `RN-{today}-{global sequence}` generation and covering sequence conflict handling per F01-FR-003 (contradicts)
- [X] T026 Remove duplicate active source-reference enforcement and stale duplicate-source error messaging from receipt creation, repository usage, controller Swagger docs, and save exception translation; replace with receipt-number conflict handling per F01 errors and F01-FR-005 (contradicts)
- [X] T027 Update the Receipt Create frontend to remove the source PO/source document field, stop submitting `source_reference`, `contact_person`, or `source_channel`, preserve `documentDate`, and display the generated `PO-{YYYYMMDD}-{SEQ}` receipt number after creation/list/detail per F01 user story and screen intent (contradicts)
- [X] T028 Update backend controller/service tests, frontend mocks, and frontend tests to expect generated `PO-{YYYYMMDD}-{SEQ}`, omit user-entered source PO/source document/contact/channel fields, assert `RECEIPT_CREATE` audit contains generated receipt number and document date, and cover validation/error docs for the new create receipt contract per SC-001, SC-002, SC-003, and Constitution IX-X (partial)

## Phase 4: Pre-Receive Manager Approval

- [X] T029 Add immutable Flyway migration for `PENDING_MANAGER_APPROVAL`, `REVISION_REQUIRED`, `pre_receive_approved_by`, `pre_receive_approved_at`, `pre_receive_rejection_reason`, and `RECEIPT_PRE_RECEIVE_APPROVE` / `RECEIPT_PRE_RECEIVE_REJECT` audit actions.
- [X] T030 Change create receipt implementation to persist new purchase receipts as `PENDING_MANAGER_APPROVAL` instead of `PENDING_RECEIPT`, with no inventory, batch, quarantine, AP, or Debit Note side effects.
- [X] T031 Add WH_MANAGER pre-receive approval mutation with `expectedVersion`: `APPROVE` moves `PENDING_MANAGER_APPROVAL` to `PENDING_RECEIPT`; `REJECT` requires reason and returns the receipt to `REVISION_REQUIRED`.
- [X] T032 Add PLANNER revision/resubmission mutation from `REVISION_REQUIRED` back to `PENDING_MANAGER_APPROVAL`.
- [X] T033 Block WH_STAFF/STOREKEEPER count and QC submission for receipts in `PENDING_MANAGER_APPROVAL` or `REVISION_REQUIRED`, returning business-rule error `RECEIPT_PENDING_MANAGER_APPROVAL` without partial save.
- [X] T034 Add service, integration, OpenAPI/Swagger, audit payload, migration, and frontend authorization coverage for pre-receive approval, rejection, revision resubmission, stale version, forbidden warehouse scope, and blocked count before manager approval.
