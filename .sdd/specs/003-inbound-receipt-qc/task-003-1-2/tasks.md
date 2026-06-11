# Tasks: Inbound Receipt Approval & Quarantine Handling

**Input**: Design documents from `.sdd/specs/003-inbound-receipt-qc/`

**Prerequisites**: `spec.md`, `features/feature-manager-receipt-approval.md`, `features/feature-manager-quarantine-handling.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/openapi.yaml`, `quickstart.md`

**Tests**: REQUIRED for service/business logic and API endpoints touched by this feature.

**Organization**: Tasks are grouped by independently testable user story.

## Phase 1: Setup & Design Alignment

**Purpose**: Confirm implementation scope and avoid reintroducing serial/expiry/grade rules.

- [ ] T001 Read `.specify/memory/constitution.md`, `AGENTS.md`, `.sdd/specs/003-inbound-receipt-qc/spec.md`, `.sdd/specs/003-inbound-receipt-qc/features/feature-manager-receipt-approval.md`, and `.sdd/specs/003-inbound-receipt-qc/features/feature-manager-quarantine-handling.md`.
- [ ] T002 Run `gitnexus detect-changes --repo Manager-warehouse-sdd` and `gitnexus impact ReceiptService --repo Manager-warehouse-sdd`; record HIGH/CRITICAL warnings before coding.
- [ ] T003 [P] Review current receipt backend files: `backend/src/main/java/com/wms/entity/Receipt.java`, `backend/src/main/java/com/wms/entity/ReceiptItem.java`, `backend/src/main/java/com/wms/entity/Batch.java`, `backend/src/main/java/com/wms/service/ReceiptService.java`, `backend/src/main/java/com/wms/repository/ReceiptRepository.java`.
- [ ] T004 [P] Review current inbound frontend files: `frontend/src/services/inbound.service.js`, `frontend/src/pages/Inbound/ReceiptList.jsx`, `frontend/src/pages/Inbound/PutawayPlan.jsx`, and `frontend/src/pages/Inbound/QuarantineWorkspace.jsx`.
- [ ] T005 [P] Review current Flyway receipt/batch/inventory migrations in `backend/src/main/resources/db/migration/` and confirm whether migrations are already applied in a shared environment before editing any existing migration.

---

## Phase 2: Foundational Backend

**Purpose**: Shared schema, DTO, repository, service, and error foundations required by US-WMS-05 and US-WMS-04.

- [ ] T006 Update `backend/src/main/java/com/wms/enums/ReceiptStatus.java` to include `RETURNED_TO_SUPPLIER` and ensure all receipt lifecycle statuses match `.sdd/specs/003-inbound-receipt-qc/spec.md`.
- [ ] T007 Update or add Flyway migration in `backend/src/main/resources/db/migration/` so `receipts.status` CHECK includes `RETURNED_TO_SUPPLIER`; do not delete applied migrations.
- [ ] T008 Remove active serial/grade/expiry usage from receipt/batch domain code by updating `backend/src/main/java/com/wms/entity/ReceiptItem.java`, `backend/src/main/java/com/wms/entity/Batch.java`, and dependent code to stop requiring `serialNumber`, `BatchGrade`, or `expiryDate` for inbound receipt flows.
- [ ] T009 Add `backend/src/main/java/com/wms/repository/ReceiptItemRepository.java` with queries to load items by receipt id and assign batch/location.
- [ ] T010 Add `backend/src/main/java/com/wms/repository/AdjustmentRepository.java` with lookup for `RETURN_TO_VENDOR` by `referenceType='RECEIPT'` and `referenceId`.
- [ ] T011 Extend `backend/src/main/java/com/wms/repository/InventoryRepository.java` with methods to find/update inventory by warehouse/product/batch/location using optimistic locking.
- [ ] T012 Add request DTOs in `backend/src/main/java/com/wms/dto/request/`: `ReceiptReturnConfirmRequest.java`, `ReceiptRtvCreateRequest.java`, and `ReceiptRtvConfirmRequest.java` with Jakarta Validation.
- [ ] T013 Update existing `backend/src/main/java/com/wms/dto/request/ReceiptDecisionRequest.java` and `ReceiptPutawayRequest.java` to include `expectedVersion` and required validation fields from `contracts/openapi.yaml`.
- [ ] T014 Add response DTOs in `backend/src/main/java/com/wms/dto/response/`: `ReceiptActionResponse.java` and `RtvActionResponse.java`.
- [ ] T015 Add or update business exceptions and centralized mapping in `backend/src/main/java/com/wms/exception/` for `FORBIDDEN_RECEIPT_WAREHOUSE`, `RECEIPT_ALREADY_DECIDED`, `RTV_ALREADY_EXISTS`, `RTV_ALREADY_CONFIRMED`, `RTV_QUANTITY_MISMATCH`, and `INVENTORY_VERSION_CONFLICT`.
- [ ] T016 Add receipt number/batch number/adjustment number/debit note number generation helpers in `backend/src/main/java/com/wms/service/ReceiptService.java` or a dedicated sequence helper, using existing project conventions.
- [ ] T017 Add warehouse-scope authorization helper in `backend/src/main/java/com/wms/service/ReceiptService.java` or a shared auth service to verify role plus assigned warehouse for Trưởng kho and Storekeeper actions.
- [ ] T018 Update `backend/src/main/java/com/wms/enums/AuditAction.java` or audit constants to support semantic actions `RECEIPT_APPROVE`, `RECEIPT_REJECT`, `RECEIPT_RETURN_CONFIRM`, `RECEIPT_PUTAWAY_COMPLETE`, `QUARANTINE_RTV_CREATE`, `QUARANTINE_RTV_CONFIRM`, and `INVENTORY_UPDATE` if the current enum strategy cannot represent them.

**Checkpoint**: Backend compiles and foundational code no longer requires serial, expiry, or grade for inbound receipt flows.

---

## Phase 3: US-WMS-05 - Receipt Approval, Reject, Return Handover, Putaway (Priority: P1)

**Goal**: Manager approval unlocks putaway without increasing inventory; rejection waits for supplier vehicle handover; putaway later increases regular inventory.

**Independent Test**: A `QC_COMPLETED` receipt can be approved then put away into a regular Bin; a separate `QC_COMPLETED` receipt can be rejected then confirmed returned to supplier without inventory, RTV, or Debit Note records.

### Tests for US-WMS-05

- [ ] T019 [P] [US1] Add service tests for approve happy path, invalid status, duplicate decision, stale version, and forbidden warehouse in `backend/src/test/java/com/wms/service/ReceiptServiceApprovalTest.java`.
- [ ] T020 [P] [US1] Add service tests for reject and return-confirm happy paths and error paths in `backend/src/test/java/com/wms/service/ReceiptServiceReturnToSupplierTest.java`.
- [ ] T021 [P] [US1] Add service tests for putaway capacity, regular Bin validation, inventory increase, and no negative inventory in `backend/src/test/java/com/wms/service/ReceiptPutawayServiceTest.java`.
- [ ] T022 [P] [US1] Add controller integration tests for `PUT /api/v1/receipts/{id}/approve`, `/reject`, `/return-to-supplier/confirm`, and `/complete` in `backend/src/test/java/com/wms/controller/ReceiptControllerApprovalIT.java`.

### Implementation for US-WMS-05

- [ ] T023 [US1] Replace demo-only `approveReceipt(Receipt, User)` logic with repository-backed transactional approval in `backend/src/main/java/com/wms/service/ReceiptService.java` or `backend/src/main/java/com/wms/service/impl/ReceiptServiceImpl.java`.
- [ ] T024 [US1] Implement batch resolve/create on approval using product plus source receipt/date in `backend/src/main/java/com/wms/service/ReceiptService.java`; do not use grade or expiry.
- [ ] T025 [US1] Implement reject logic in `backend/src/main/java/com/wms/service/ReceiptService.java` to set `RETURN_TO_SUPPLIER_PENDING`, store reason, and avoid batch/inventory/RTV/Debit Note creation.
- [ ] T026 [US1] Implement Storekeeper supplier handover confirmation in `backend/src/main/java/com/wms/service/ReceiptService.java` to set `RETURNED_TO_SUPPLIER` without inventory changes.
- [ ] T027 [US1] Implement putaway completion in `backend/src/main/java/com/wms/service/ReceiptService.java` with regular Bin validation, capacity validation, inventory increase, version checks, and audit logs.
- [ ] T028 [US1] Add `backend/src/main/java/com/wms/controller/ReceiptController.java` endpoints for approve, reject, return-confirm, and complete using validated DTOs.
- [ ] T029 [US1] Update OpenAPI/Swagger annotations in `backend/src/main/java/com/wms/controller/ReceiptController.java` and keep `.sdd/specs/003-inbound-receipt-qc/contracts/openapi.yaml` aligned.
- [ ] T030 [US1] Update frontend API methods in `frontend/src/services/inbound.service.js` for approve, reject, return-to-supplier confirm, and complete putaway payloads including `expectedVersion`.
- [ ] T031 [US1] Update receipt UI in `frontend/src/pages/Inbound/ReceiptList.jsx` and `frontend/src/pages/Inbound/PutawayPlan.jsx` to show `RETURN_TO_SUPPLIER_PENDING`, `RETURNED_TO_SUPPLIER`, and return-confirm action for Storekeeper.
- [ ] T032 [P] [US1] Add frontend tests for approval/reject/return-confirm button visibility and service calls in `frontend/src/pages/Inbound/ReceiptList.test.jsx`.

**Checkpoint**: US-WMS-05 endpoints and UI work independently; regular inventory increases only after putaway.

---

## Phase 4: US-WMS-04 - Quarantine RTV And Debit Note (Priority: P1)

**Goal**: Manager creates RTV and Debit Note for `QC_FAILED` quarantine receipt; Storekeeper confirms full physical return and only then quarantine inventory is deducted.

**Independent Test**: A `QC_FAILED` receipt with quarantine inventory can create one pending RTV and Debit Note without stock deduction; confirming exactly the full quantity deducts quarantine inventory, while partial quantity is rejected with HTTP 422.

### Tests for US-WMS-04

- [ ] T033 [P] [US2] Add service tests for RTV create happy path, duplicate RTV, non-`QC_FAILED` status, missing quarantine inventory, and forbidden warehouse in `backend/src/test/java/com/wms/service/ReceiptRtvCreateServiceTest.java`.
- [ ] T034 [P] [US2] Add service tests for RTV confirm full quantity, partial quantity mismatch, duplicate confirmation, stale version, and quarantine inventory non-negative in `backend/src/test/java/com/wms/service/ReceiptRtvConfirmServiceTest.java`.
- [ ] T035 [P] [US2] Add controller integration tests for `POST /api/v1/receipts/{id}/rtv` and `PUT /api/v1/receipts/{id}/rtv/confirm` in `backend/src/test/java/com/wms/controller/ReceiptQuarantineControllerIT.java`.

### Implementation for US-WMS-04

- [ ] T036 [US2] Implement RTV create service logic in `backend/src/main/java/com/wms/service/ReceiptService.java` with `QC_FAILED` validation, warehouse authorization, duplicate RTV check, pending `RETURN_TO_VENDOR` adjustment creation, and system Debit Note creation.
- [ ] T037 [US2] Implement Debit Note creation using `backend/src/main/java/com/wms/repository/DebitNoteRepository.java`, linked to receipt and supplier, with failed quantity and amount from receipt items/unit cost.
- [ ] T038 [US2] Implement RTV confirm service logic in `backend/src/main/java/com/wms/service/ReceiptService.java` requiring returned quantity to equal full quarantined quantity.
- [ ] T039 [US2] Implement quarantine inventory deduction with optimistic locking in `backend/src/main/java/com/wms/service/ReceiptService.java` and `backend/src/main/java/com/wms/repository/InventoryRepository.java`.
- [ ] T040 [US2] Add controller endpoints for RTV create/confirm in `backend/src/main/java/com/wms/controller/ReceiptController.java`.
- [ ] T041 [US2] Update OpenAPI/Swagger annotations for RTV endpoints and error codes in `backend/src/main/java/com/wms/controller/ReceiptController.java`.
- [ ] T042 [US2] Update frontend quarantine service methods in `frontend/src/services/inbound.service.js` to create RTV without immediate deduction and confirm RTV with full quantity only.
- [ ] T043 [US2] Update `frontend/src/pages/Inbound/QuarantineWorkspace.jsx` to show only "Trả NCC" for Spec 003, block partial quantity confirmation, and show pending/confirmed RTV state.
- [ ] T044 [P] [US2] Add frontend tests for RTV create/confirm and partial quantity validation in `frontend/src/pages/Inbound/QuarantineWorkspace.test.jsx`.

**Checkpoint**: US-WMS-04 endpoints and UI work independently; quarantine inventory is deducted only after full RTV confirmation.

---

## Phase 5: Cross-Cutting Verification & Documentation

- [ ] T045 Update `.sdd/specs/003-inbound-receipt-qc/spec.md`, `features/feature-manager-receipt-approval.md`, and `features/feature-manager-quarantine-handling.md` if implementation choices differ from this plan.
- [ ] T046 Update `.sdd/specs/003-inbound-receipt-qc/quickstart.md` with any final test data setup details discovered during implementation.
- [ ] T047 Run `mvn compile` from `backend/`.
- [ ] T048 Run backend tests for receipt approval/quarantine with `mvn test -Dtest='*Receipt*Test,*Quarantine*Test,*Inbound*Test'` from `backend/`.
- [ ] T049 Run frontend tests/build relevant to inbound pages if frontend changed.
- [ ] T050 Run `rg -n "System\\.out|console\\.log|TODO|has_serial|has_expiry|FEFO|BatchGrade|serialNumber" backend/src/main/java frontend/src .sdd/specs/003-inbound-receipt-qc` and remove violations or justify non-domain matches.
- [ ] T051 Run `gitnexus detect-changes --repo Manager-warehouse-sdd` before commit and report risk.

## Dependencies

- Phase 1 must complete before Phase 2.
- Phase 2 blocks both US-WMS-05 and US-WMS-04.
- US-WMS-05 can be implemented before US-WMS-04 and is the MVP because putaway/inventory timing is central to receipt approval.
- US-WMS-04 depends on foundational inventory, adjustment, debit note, authorization, and audit helpers but can proceed in parallel with frontend work after Phase 2.

## Parallel Execution Examples

- T003, T004, and T005 can run in parallel during setup.
- T019, T020, T021, and T022 can be written in parallel before US-WMS-05 implementation.
- T033, T034, and T035 can be written in parallel before US-WMS-04 implementation.
- T030/T031 can run after backend contracts stabilize, while T023-T027 continue.
- T042/T043 can run after RTV contract is stable, while T036-T039 continue.

## Implementation Strategy

1. MVP: Complete Phase 1, Phase 2, and US-WMS-05 backend endpoints/tests first.
2. Add US-WMS-05 frontend actions after backend contracts pass integration tests.
3. Implement US-WMS-04 backend RTV create/confirm and tests.
4. Add Quarantine UI changes.
5. Run full compile/test, OpenAPI verification, audit verification, and GitNexus change detection.
