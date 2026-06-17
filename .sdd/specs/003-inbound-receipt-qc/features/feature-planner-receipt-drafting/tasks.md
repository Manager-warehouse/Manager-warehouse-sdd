# Tasks: Planner Receipt Drafting

**Input**: Design documents from `.sdd/specs/003-inbound-receipt-qc/features/feature-planner-receipt-drafting/`

**Prerequisites**: [plan.md](./plan.md), [feature-planner-receipt-drafting.md](./feature-planner-receipt-drafting.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/create-receipt.openapi.yaml](./contracts/create-receipt.openapi.yaml), [quickstart.md](./quickstart.md)

**Tests**: Required by quickstart and project constitution; write service and controller tests before implementation.

**Organization**: One P1 user story, independently testable through `POST /api/v1/receipts`.

## Phase 1: Setup

**Purpose**: Prepare feature-specific constants and repository access required by the receipt creation slice.

- [x] T001 Create `ReceiptSourceChannel` enum with `ZALO` and `EMAIL` in `backend/src/main/java/com/wms/enums/ReceiptSourceChannel.java`
- [x] T002 [P] Create `ProductRepository` with active product lookup methods in `backend/src/main/java/com/wms/repository/ProductRepository.java`
- [x] T003 [P] Create `WarehouseRepository` with active warehouse lookup methods in `backend/src/main/java/com/wms/repository/WarehouseRepository.java`
- [x] T004 [P] Create `ReceiptItemRepository` in `backend/src/main/java/com/wms/repository/ReceiptItemRepository.java`
- [x] T005 Add supplier/warehouse/source/type duplicate detection query to `backend/src/main/java/com/wms/repository/ReceiptRepository.java`
- [x] T006 Change `ReceiptItem.expectedQty` to `Integer` in `backend/src/main/java/com/wms/entity/ReceiptItem.java`
- [x] T007 Add DB migration for integer `receipt_items.expected_qty`, source channel check, positive quantity check, receipt document counter, and purchase duplicate index in `backend/src/main/resources/db/migration/V7__enforce_receipt_drafting_constraints.sql`

---

## Phase 2: Foundational

**Purpose**: Shared request, response, mapping, and error surface that must exist before the endpoint implementation.

**Critical**: No user story implementation should start until this phase is complete.

- [x] T008 [P] Create `CreateReceiptItemRequest` with `product_id` and integer `expected_qty` validation in `backend/src/main/java/com/wms/dto/request/CreateReceiptItemRequest.java`
- [x] T009 [P] Create `CreateReceiptRequest` with supplier, warehouse, contact, source reference, `ReceiptSourceChannel`, and non-empty item validation in `backend/src/main/java/com/wms/dto/request/CreateReceiptRequest.java`
- [x] T010 [P] Create `ReceiptItemResponse` with integer `expected_qty` in `backend/src/main/java/com/wms/dto/response/ReceiptItemResponse.java`
- [x] T011 [P] Create `ReceiptResponse` for created receipt payloads in `backend/src/main/java/com/wms/dto/response/ReceiptResponse.java`
- [x] T012 Create `ReceiptMapper` for entity-to-response conversion in `backend/src/main/java/com/wms/mapper/ReceiptMapper.java`
- [x] T013 Extend centralized exception handling for validation, duplicate source reference, inactive master data, and warehouse authorization errors in `backend/src/main/java/com/wms/exception/GlobalExceptionHandler.java`

**Checkpoint**: DTOs, repositories, mappers, and error responses are ready for endpoint implementation.

---

## Phase 3: User Story 1 - Draft Supplier Purchase Receipt (Priority: P1) MVP

**Goal**: Planner creates a supplier purchase receipt from Zalo/Email data, producing a `PURCHASE` receipt in `PENDING_RECEIPT` with generated receipt number, integer expected quantities, duplicate protection, and audit trail.

**Independent Test**: Call `POST /api/v1/receipts` with an authorized Planner, active supplier, active warehouse, active product, `source_channel = ZALO`, and `expected_qty = 500`; verify HTTP 201, generated `receipt_number`, `type = PURCHASE`, `status = PENDING_RECEIPT`, persisted receipt/items, no inventory mutation, and an audit entry.

### Tests for User Story 1

- [x] T014 [US1] Add service unit test for successful receipt creation and audit logging in `backend/src/test/java/com/wms/service/ReceiptServiceTest.java`
- [x] T015 [US1] Add service unit test for missing/inactive supplier, inactive warehouse, inactive product, and unauthorized warehouse in `backend/src/test/java/com/wms/service/ReceiptServiceTest.java`
- [x] T016 [US1] Add service unit test for integer `expected_qty` rules including zero, negative, and fractional rejection in `backend/src/test/java/com/wms/service/ReceiptServiceTest.java`
- [x] T017 [US1] Add service unit test for duplicate supplier/warehouse/source reference rejection in `backend/src/test/java/com/wms/service/ReceiptServiceTest.java`
- [x] T018 [US1] Add controller integration test for happy-path `POST /api/v1/receipts` in `backend/src/test/java/com/wms/controller/ReceiptControllerTest.java`
- [x] T019 [US1] Add controller integration test for invalid payload, invalid channel, duplicate source reference, and return-flow rejection in `backend/src/test/java/com/wms/controller/ReceiptControllerTest.java`

### Implementation for User Story 1

- [x] T020 [US1] Replace demo `ReceiptService` with real transactional create receipt contract in `backend/src/main/java/com/wms/service/ReceiptService.java`
- [x] T021 [US1] Implement receipt number generation for unique purchase receipt codes in `backend/src/main/java/com/wms/service/ReceiptService.java`
- [x] T022 [US1] Implement supplier, warehouse, product active-state validation in `backend/src/main/java/com/wms/service/ReceiptService.java`
- [x] T023 [US1] Implement Planner warehouse authorization check using `UserWarehouseAssignmentRepository` in `backend/src/main/java/com/wms/service/ReceiptService.java`
- [x] T024 [US1] Implement source channel, purchase-only, duplicate source reference, and integer `expected_qty` business rules in `backend/src/main/java/com/wms/service/ReceiptService.java`
- [x] T025 [US1] Persist `Receipt` and `ReceiptItem` rows with `type = PURCHASE`, `status = PENDING_RECEIPT`, null QC/batch/location fields, and zero `overReceivedQty` in `backend/src/main/java/com/wms/service/ReceiptService.java`
- [x] T026 [US1] Write `RECEIPT_CREATE` audit data through existing audit infrastructure using `AuditAction.CREATE` and entity type `RECEIPT` in `backend/src/main/java/com/wms/service/ReceiptService.java`
- [x] T027 [US1] Create `ReceiptController` with `POST /api/v1/receipts`, `@Valid` request handling, current user lookup, HTTP 201 response, and OpenAPI annotations in `backend/src/main/java/com/wms/controller/ReceiptController.java`
- [x] T028 [US1] Verify receipt creation does not call inventory, batch, QC, quarantine, adjustment, or putaway code in `backend/src/main/java/com/wms/service/ReceiptService.java`

**Checkpoint**: User Story 1 is fully functional and independently testable through the API contract.

---

## Phase 4: Polish & Cross-Cutting Concerns

**Purpose**: Documentation, validation, and final verification for this feature slice.

- [x] T029 [P] Update parent inbound spec links to point at the folder-based feature spec in `.sdd/specs/003-inbound-receipt-qc/spec.md`
- [x] T030 [P] Align generated OpenAPI documentation with `contracts/create-receipt.openapi.yaml` through annotations in `backend/src/main/java/com/wms/controller/ReceiptController.java`
- [ ] T031 Run backend unit and integration tests with `mvn test` from `backend/`
- [ ] T032 Run backend compile verification with `mvn compile` from `backend/`
- [x] T033 Confirm no TODO comments, `System.out`, raw SQL, inventory mutation, or missing audit logging remain in receipt drafting files under `backend/src/main/java/com/wms/`

---

## Dependencies & Execution Order

### Phase Dependencies

- Setup (Phase 1) has no dependencies.
- Foundational (Phase 2) depends on Setup completion.
- User Story 1 (Phase 3) depends on Foundational completion.
- Polish (Phase 4) depends on User Story 1 completion.

### User Story Dependencies

- US1 is the MVP and has no dependency on other user stories.

### Within User Story 1

- Tests T014-T019 should be written and fail before implementation tasks T020-T028.
- DTOs and repositories must exist before service implementation.
- Service implementation must exist before controller implementation.
- Audit logging and no-inventory-mutation verification must pass before the story checkpoint.

### Parallel Opportunities

- T002, T003, T004 can run in parallel after T001.
- T007, T008, T009, T010 can run in parallel after T001.
- T029 and T030 can run in parallel after US1 implementation is complete.

---

## Parallel Example: User Story 1

```text
Task: "Add service unit test for successful receipt creation and audit logging in backend/src/test/java/com/wms/service/ReceiptServiceTest.java"
Task: "Add service unit test for duplicate supplier/warehouse/source reference rejection in backend/src/test/java/com/wms/service/ReceiptServiceTest.java"
Task: "Add controller integration test for happy-path POST /api/v1/receipts in backend/src/test/java/com/wms/controller/ReceiptControllerTest.java"
Task: "Add controller integration test for invalid payload, invalid channel, duplicate source reference, and return-flow rejection in backend/src/test/java/com/wms/controller/ReceiptControllerTest.java"
```

---

## Implementation Strategy

### MVP First

1. Complete Phase 1 and Phase 2.
2. Write failing tests T014-T019 for US1.
3. Implement T020-T028.
4. Validate through the independent API test and quickstart request.
5. Run T030-T032 before handing off.

### Incremental Delivery

1. Deliver repositories and DTOs.
2. Deliver service rules with tests.
3. Deliver controller and OpenAPI documentation.
4. Validate duplicate, validation, audit, and no-inventory-mutation behavior.

### Notes

- `receipt_items.expected_qty` is integer in API, entity, and database schema for this feature.
- GitNexus resources were unavailable in this environment, so impact analysis for task generation was performed manually from local files.
- `setup-tasks.ps1` expects `spec.md`; this feature-level folder uses `feature-planner-receipt-drafting.md`, so tasks were generated from the provided plan path and adjacent design artifacts.
