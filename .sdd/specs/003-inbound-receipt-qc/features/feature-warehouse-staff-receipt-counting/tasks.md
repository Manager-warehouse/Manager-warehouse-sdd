# Tasks: Warehouse Staff Receipt Counting

**Input**: Design documents from `.sdd/specs/003-inbound-receipt-qc/features/feature-warehouse-staff-receipt-counting/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/receive-receipt.openapi.yaml](./contracts/receive-receipt.openapi.yaml), [quickstart.md](./quickstart.md)

**Tests**: Required by project constitution; write service and controller tests before implementation.

**Organization**: Three P1 user stories, each independently testable through `PUT /api/v1/receipts/{id}/receive`.

## Phase 1: Setup

**Purpose**: Align persistence and shared contracts with integer receive-count semantics before endpoint work.

- [x] T001 Create migration `V8__align_receipt_counting_integer_quantities.sql` to convert `actual_qty`, `over_received_qty`, `sample_qty`, `sample_passed_qty`, and `sample_failed_qty` to positive integer-compatible columns with appropriate checks in `backend/src/main/resources/db/migration/V8__align_receipt_counting_integer_quantities.sql`
- [x] T002 Change `ReceiptItem.actualQty`, `overReceivedQty`, `sampleQty`, `samplePassedQty`, and `sampleFailedQty` from `BigDecimal` to `Integer` in `backend/src/main/java/com/wms/entity/ReceiptItem.java`
- [x] T003 Update receipt drafting initialization of `overReceivedQty` from `BigDecimal.ZERO` to integer zero in `backend/src/main/java/com/wms/service/ReceiptService.java`
- [x] T004 [P] Add `ReceiveReceiptItemRequest` with `receipt_item_id` and positive integer `counted_qty` validation in `backend/src/main/java/com/wms/dto/request/ReceiveReceiptItemRequest.java`
- [x] T005 [P] Add `ReceiveReceiptRequest` with non-empty `items` validation in `backend/src/main/java/com/wms/dto/request/ReceiveReceiptRequest.java`
- [x] T006 [P] Extend `ReceiptItemResponse` with `receipt_item_id`, `actual_qty`, and `over_received_qty` in `backend/src/main/java/com/wms/dto/response/ReceiptItemResponse.java`
- [x] T007 Update `ReceiptMapper` to map receipt item id, integer `actualQty`, and integer `overReceivedQty` in `backend/src/main/java/com/wms/mapper/ReceiptMapper.java`

---

## Phase 2: Foundational

**Purpose**: Shared repository, authorization, error, and audit support needed by all receive-counting stories.

**Critical**: No user story implementation should start until this phase is complete.

- [x] T008 Add repository method to load all receipt items by receipt id in `backend/src/main/java/com/wms/repository/ReceiptItemRepository.java`
- [x] T009 Add repository method or query support to load receipt with warehouse for receive-count authorization in `backend/src/main/java/com/wms/repository/ReceiptRepository.java`
- [x] T010 Add service contract `receiveReceiptCounts(Long receiptId, ReceiveReceiptRequest request, User actor)` in `backend/src/main/java/com/wms/service/ReceiptService.java`
- [x] T011 Add centralized exception mapping for `INVALID_RECEIPT_COUNT`, `RECEIPT_COUNT_INCOMPLETE`, `INVALID_RECEIPT_STATUS`, and `RECEIPT_ALREADY_FINALIZED` semantics in `backend/src/main/java/com/wms/exception/GlobalExceptionHandler.java`
- [x] T012 Add helper methods for receipt count snapshots before/after mutation in `backend/src/main/java/com/wms/service/ReceiptService.java`
- [x] T013 Add helper method for Warehouse Staff role and receipt warehouse assignment checks in `backend/src/main/java/com/wms/service/ReceiptService.java`

**Checkpoint**: DTOs, integer fields, repository access, error mapping, audit helpers, and RBAC helpers are ready.

---

## Phase 3: User Story 1 - Submit Complete Receipt Counts (Priority: P1) MVP

**Goal**: Warehouse Staff submits one positive integer count for every receipt item on a `PENDING_RECEIPT` receipt; the system calculates count fields, moves the receipt to `DRAFT`, and writes audit data without inventory mutation.

**Independent Test**: Call `PUT /api/v1/receipts/{id}/receive` as assigned Warehouse Staff for a `PENDING_RECEIPT` receipt with all item ids; verify HTTP 200, `status = DRAFT`, `actual_qty`/`over_received_qty` calculations, audit entry, and no inventory/batch/quarantine/location changes.

### Tests for User Story 1

- [x] T014 [P] [US1] Add service unit test for complete count submission with equal/short count calculations in `backend/src/test/java/com/wms/service/ReceiptServiceTest.java`
- [x] T015 [P] [US1] Add service unit test for over-received calculation and capping `actualQty` at `expectedQty` in `backend/src/test/java/com/wms/service/ReceiptServiceTest.java`
- [x] T016 [P] [US1] Add service unit test proving successful receive count creates before/after audit data and does not touch inventory collaborators in `backend/src/test/java/com/wms/service/ReceiptServiceTest.java`
- [x] T017 [P] [US1] Add controller integration test for happy-path `PUT /api/v1/receipts/{id}/receive` returning updated receipt fields in `backend/src/test/java/com/wms/controller/ReceiptControllerTest.java`

### Implementation for User Story 1

- [x] T018 [US1] Implement complete item coverage validation for target receipt in `backend/src/main/java/com/wms/service/ReceiptService.java`
- [x] T019 [US1] Implement integer count calculation for `actualQty` and `overReceivedQty` in `backend/src/main/java/com/wms/service/ReceiptService.java`
- [x] T020 [US1] Implement `PENDING_RECEIPT` to `DRAFT` transition and `updatedAt` update in `backend/src/main/java/com/wms/service/ReceiptService.java`
- [x] T021 [US1] Implement `RECEIPT_RECEIVE` equivalent audit logging using existing audit infrastructure in `backend/src/main/java/com/wms/service/ReceiptService.java`
- [x] T022 [US1] Add `PUT /api/v1/receipts/{id}/receive` with `@Valid`, current user lookup, response mapping, `@PreAuthorize`, and OpenAPI annotations in `backend/src/main/java/com/wms/controller/ReceiptController.java`
- [x] T023 [US1] Verify the service does not call or mutate inventory, batch, quarantine, adjustment, or location data in `backend/src/main/java/com/wms/service/ReceiptService.java`

**Checkpoint**: US1 is fully functional and independently testable through the receive-count API.

---

## Phase 4: User Story 2 - Reject Invalid Or Incomplete Count Requests (Priority: P1)

**Goal**: Invalid count requests are rejected atomically with proper status codes and no partial receipt item updates.

**Independent Test**: Submit incomplete items, duplicate item ids, item ids from another receipt, zero/negative/non-integer counts, unauthorized user, and final receipt status; verify expected error response and unchanged receipt/item state.

### Tests for User Story 2

- [x] T024 [P] [US2] Add service unit test for missing receipt item coverage with no partial changes in `backend/src/test/java/com/wms/service/ReceiptServiceTest.java`
- [x] T025 [P] [US2] Add service unit test for duplicate receipt item ids and item ids from another receipt in `backend/src/test/java/com/wms/service/ReceiptServiceTest.java`
- [x] T026 [P] [US2] Add service unit test for zero and negative `countedQty` rejection in `backend/src/test/java/com/wms/service/ReceiptServiceTest.java`
- [x] T027 [P] [US2] Add service unit test for `APPROVED` and `REJECTED` receipt rejection in `backend/src/test/java/com/wms/service/ReceiptServiceTest.java`
- [x] T028 [P] [US2] Add controller integration tests for 403, 409, and 422 receive-count responses in `backend/src/test/java/com/wms/controller/ReceiptControllerTest.java`

### Implementation for User Story 2

- [x] T029 [US2] Implement atomic validation before mutating receipt items in `backend/src/main/java/com/wms/service/ReceiptService.java`
- [x] T030 [US2] Implement `RECEIPT_COUNT_INCOMPLETE` behavior for missing or extra target receipt items in `backend/src/main/java/com/wms/service/ReceiptService.java`
- [x] T031 [US2] Implement `INVALID_RECEIPT_COUNT` behavior for duplicate items, wrong receipt items, and non-positive counts in `backend/src/main/java/com/wms/service/ReceiptService.java`
- [x] T032 [US2] Implement `RECEIPT_ALREADY_FINALIZED` behavior for `APPROVED` and `REJECTED` receipts in `backend/src/main/java/com/wms/service/ReceiptService.java`
- [x] T033 [US2] Implement Warehouse Staff role and warehouse assignment enforcement for receive counting in `backend/src/main/java/com/wms/service/ReceiptService.java`

**Checkpoint**: US2 rejects invalid receive-count requests independently without partial data changes.

---

## Phase 5: User Story 3 - Correct Counts Before Manager Finalization (Priority: P1)

**Goal**: Warehouse Staff corrects counts while receipt status is `DRAFT`, `QC_COMPLETED`, or `QC_FAILED`; the system recalculates counts, clears stale QC data when present, returns the receipt to `DRAFT`, and writes audit data.

**Independent Test**: Start with a `QC_COMPLETED` or `QC_FAILED` receipt containing QC fields, submit corrected counts for all items, and verify recalculated item quantities, cleared QC fields, `status = DRAFT`, audit entry, and no inventory/batch/quarantine/location changes.

### Tests for User Story 3

- [x] T034 [P] [US3] Add service unit test for correcting a `DRAFT` receipt and preserving `DRAFT` status in `backend/src/test/java/com/wms/service/ReceiptServiceTest.java`
- [x] T035 [P] [US3] Add service unit test for correcting `QC_COMPLETED` receipt and clearing all QC fields in `backend/src/test/java/com/wms/service/ReceiptServiceTest.java`
- [x] T036 [P] [US3] Add service unit test for correcting `QC_FAILED` receipt and returning receipt to `DRAFT` in `backend/src/test/java/com/wms/service/ReceiptServiceTest.java`
- [x] T037 [P] [US3] Add controller integration test for correction after QC data exists in `backend/src/test/java/com/wms/controller/ReceiptControllerTest.java`

### Implementation for User Story 3

- [x] T038 [US3] Allow receive-count correction for `DRAFT`, `QC_COMPLETED`, and `QC_FAILED` receipts in `backend/src/main/java/com/wms/service/ReceiptService.java`
- [x] T039 [US3] Clear `qcResult`, `sampleQty`, `samplePassedQty`, `sampleFailedQty`, `qcSamplingMethod`, and `qcFailureReason` on corrected receipt items with QC data in `backend/src/main/java/com/wms/service/ReceiptService.java`
- [x] T040 [US3] Return corrected receipts to `DRAFT` and refresh audit before/after snapshots in `backend/src/main/java/com/wms/service/ReceiptService.java`
- [x] T041 [US3] Update `ReceiptMapper` and response expectations for corrected count fields after QC invalidation in `backend/src/main/java/com/wms/mapper/ReceiptMapper.java`

**Checkpoint**: US3 correction flow is independently functional through the receive-count API.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Documentation, OpenAPI consistency, and final verification across all stories.

- [x] T042 [P] Align controller OpenAPI annotations with `contracts/receive-receipt.openapi.yaml` in `backend/src/main/java/com/wms/controller/ReceiptController.java`
- [x] T043 [P] Update receive-count quickstart examples if implementation response fields differ in `.sdd/specs/003-inbound-receipt-qc/features/feature-warehouse-staff-receipt-counting/quickstart.md`
- [ ] T044 Run backend unit and integration tests with `mvn test` using `backend/pom.xml`
- [ ] T045 Run backend compile verification with `mvn compile` using `backend/pom.xml`
- [x] T046 Confirm no unfinished comments, `System.out`, raw SQL, inventory mutation, missing audit logging, or missing validation remain in receipt counting files under `backend/src/main/java/com/wms/`
- [x] T047 Confirm generated API docs expose `PUT /api/v1/receipts/{id}/receive` with the same request/response semantics as `.sdd/specs/003-inbound-receipt-qc/features/feature-warehouse-staff-receipt-counting/contracts/receive-receipt.openapi.yaml`

---

## Dependencies & Execution Order

### Phase Dependencies

- Setup (Phase 1) has no dependencies.
- Foundational (Phase 2) depends on Setup completion.
- US1 (Phase 3) depends on Foundational completion and is the MVP.
- US2 (Phase 4) depends on US1 validation/calculation structure.
- US3 (Phase 5) depends on US1 calculation structure and US2 status validation.
- Polish (Phase 6) depends on selected user stories being complete.

### User Story Dependencies

- **US1**: MVP; can start after Phase 2.
- **US2**: Uses the same endpoint/service but validates failure paths; start after US1 service skeleton exists.
- **US3**: Uses the same endpoint/service but adds correction and QC invalidation; start after US1 calculations are stable.

### Within Each User Story

- Tests T014-T017, T024-T028, and T034-T037 should be written and fail before implementation tasks for their story.
- DTOs, entity integer alignment, repositories, error mapping, and authorization helpers must exist before service implementation.
- Service implementation must exist before controller endpoint behavior can pass integration tests.
- Audit and no-inventory-mutation checks must pass before each story checkpoint.

### Parallel Opportunities

- T004, T005, and T006 can run in parallel after T001-T003 are coordinated.
- T008, T009, T011, T012, and T013 can run in parallel after DTO/entity setup.
- Tests within each story marked [P] can be written in parallel.
- T042 and T043 can run in parallel after endpoint behavior stabilizes.

---

## Parallel Example: User Story 1

```text
Task: "Add service unit test for complete count submission with equal/short count calculations in backend/src/test/java/com/wms/service/ReceiptServiceTest.java"
Task: "Add service unit test for over-received calculation and capping actualQty at expectedQty in backend/src/test/java/com/wms/service/ReceiptServiceTest.java"
Task: "Add service unit test proving successful receive count creates before/after audit data and does not touch inventory collaborators in backend/src/test/java/com/wms/service/ReceiptServiceTest.java"
Task: "Add controller integration test for happy-path PUT /api/v1/receipts/{id}/receive returning updated receipt fields in backend/src/test/java/com/wms/controller/ReceiptControllerTest.java"
```

## Parallel Example: User Story 2

```text
Task: "Add service unit test for missing receipt item coverage with no partial changes in backend/src/test/java/com/wms/service/ReceiptServiceTest.java"
Task: "Add service unit test for duplicate receipt item ids and item ids from another receipt in backend/src/test/java/com/wms/service/ReceiptServiceTest.java"
Task: "Add controller integration tests for 403, 409, and 422 receive-count responses in backend/src/test/java/com/wms/controller/ReceiptControllerTest.java"
```

## Parallel Example: User Story 3

```text
Task: "Add service unit test for correcting QC_COMPLETED receipt and clearing all QC fields in backend/src/test/java/com/wms/service/ReceiptServiceTest.java"
Task: "Add service unit test for correcting QC_FAILED receipt and returning receipt to DRAFT in backend/src/test/java/com/wms/service/ReceiptServiceTest.java"
Task: "Add controller integration test for correction after QC data exists in backend/src/test/java/com/wms/controller/ReceiptControllerTest.java"
```

---

## Implementation Strategy

### MVP First

1. Complete Phase 1 and Phase 2.
2. Write failing tests T014-T017 for US1.
3. Implement T018-T023.
4. Validate US1 independently with quickstart happy path.
5. Stop and demo if only MVP is needed.

### Incremental Delivery

1. Deliver integer quantity alignment and receive-count DTOs.
2. Deliver US1 happy path with audit and no inventory mutation.
3. Deliver US2 validation/error surface and atomic rejection.
4. Deliver US3 correction/QC invalidation.
5. Run final compile, tests, quickstart, and OpenAPI checks.

### Notes

- [P] tasks operate on different files or can be written independently without blocking other marked tasks.
- Keep implementation inside existing backend layered architecture.
- Do not introduce raw SQL in application code; use Flyway for schema alignment and JPA repositories for data access.
- Do not add serial handling to this feature; it is explicitly out of scope.
