# Tasks: Warehouse Staff Picking & QC Outbound

**Input**: Design documents from `.sdd/specs/004-outbound-delivery-pod/features/feature-warehouse-staff-picking-qc/`

**Prerequisites**: [plan.md](plan.md), [feature-warehouse-staff-picking-qc.md](feature-warehouse-staff-picking-qc.md), [research.md](research.md), [data-model.md](data-model.md), [quickstart.md](quickstart.md), [contracts/delivery-order-pick-qc.openapi.yaml](contracts/delivery-order-pick-qc.openapi.yaml)

**Tests**: Required by the feature spec, quickstart, and constitution for service/business logic and API endpoints.

**Organization**: Tasks are grouped by independently testable user stories.

## Phase 1: Setup

**Purpose**: Review the outbound delivery-order seams that this feature extends.

- [X] T001 Review current outbound workflow handling in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [X] T002 [P] Review current delivery-order controller endpoints and security annotations in `backend/src/main/java/com/wms/controller/DeliveryOrderController.java`
- [X] T003 [P] Review current outbound DTOs, mapper, and response contracts in `backend/src/main/java/com/wms/dto/request`, `backend/src/main/java/com/wms/dto/response`, and `backend/src/main/java/com/wms/mapper/DeliveryOrderMapper.java`
- [X] T004 [P] Review current inventory, quarantine, and adjustment support classes in `backend/src/main/java/com/wms/entity/Inventory.java`, `backend/src/main/java/com/wms/entity/QuarantineRecord.java`, and `backend/src/main/java/com/wms/entity/InventoryAdjustment.java`

---

## Phase 2: Foundational

**Purpose**: Add shared persistence, validation, and service infrastructure that blocks all user stories.

**Critical**: No user-story implementation should begin until this phase is complete.

- [X] T005 Add Flyway migration for outbound QC idempotency, quarantine references, and warehouse reject return support in `backend/src/main/resources/db/migration`
- [X] T006 [P] Add or extend `OutboundQcRecord` entity for allocation-level QC result persistence, payload hash, and idempotency metadata in `backend/src/main/java/com/wms/entity/OutboundQcRecord.java`
- [X] T007 [P] Add entity support for warehouse reject return rows if missing in `backend/src/main/java/com/wms/entity`
- [X] T008 [P] Extend `QuarantineRecord` and `InventoryAdjustment` entities with outbound QC fail references in `backend/src/main/java/com/wms/entity/QuarantineRecord.java` and `backend/src/main/java/com/wms/entity/InventoryAdjustment.java`
- [X] T009 [P] Extend `OutboundQcRecordRepository` with active-cycle lookup and idempotency replay helpers in `backend/src/main/java/com/wms/repository/OutboundQcRecordRepository.java`
- [X] T010 [P] Extend `DeliveryOrderRepository`, `DeliveryOrderItemRepository`, and `DeliveryOrderItemAllocationRepository` with detail-loading queries for planned allocations, staged pass quantities, and QC status in `backend/src/main/java/com/wms/repository`
- [X] T011 [P] Extend `InventoryRepository` with version-safe source, staging, and quarantine inventory lookup helpers in `backend/src/main/java/com/wms/repository/InventoryRepository.java`
- [X] T012 [P] Extend `QuarantineRecordRepository` and `InventoryAdjustmentRepository` for outbound QC fail persistence checks in `backend/src/main/java/com/wms/repository`
- [X] T013 Add shared outbound QC helper methods for warehouse scope, active allocation set resolution, quantity aggregation, and optimistic-lock validation in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [X] T014 Add audit action enum values for `DELIVERY_ORDER_PICK_COMPLETE`, `OUTBOUND_QC_FAIL_QUARANTINE`, `DELIVERY_ORDER_QC_APPROVE`, and `DELIVERY_ORDER_WAREHOUSE_APPROVE` if missing in `backend/src/main/java/com/wms/enums/AuditAction.java`

**Checkpoint**: Foundation ready for user-story implementation.

---

## Phase 3: User Story 1 - Warehouse staff saves one full pick/QC submission (Priority: P1) MVP

**Goal**: Warehouse staff assigned to the Delivery Order warehouse records one complete pick/QC result set for the active allocations and moves the order from `WAITING_PICKING` to `QC_PENDING_APPROVAL`.

**Independent Test**: Submit a valid `PUT /api/v1/delivery-orders/{id}/pick-qc-result` request for a `WAITING_PICKING` Delivery Order and verify allocation-level QC rows persist, pass quantity moves to outbound staging, fail quantity moves to quarantine with adjustment and quarantine records, audit logs are written, and the order moves to `QC_PENDING_APPROVAL`.

### Tests for User Story 1

- [X] T015 [P] [US1] Add controller test for `PUT /api/v1/delivery-orders/{id}/pick-qc-result` happy path in `backend/src/test/java/com/wms/controller/DeliveryOrderControllerTest.java`
- [X] T016 [P] [US1] Add service unit test for rejecting `pickedQty != qcPassQty + qcFailQty` in `backend/src/test/java/com/wms/service/DeliveryOrderServiceImplTest.java`
- [X] T017 [P] [US1] Add service unit test for rejecting partial submission when not every active allocation is included in `backend/src/test/java/com/wms/service/DeliveryOrderServiceImplTest.java`
- [X] T018 [P] [US1] Add service unit test for moving pass quantity to staging and fail quantity to quarantine in one transaction in `backend/src/test/java/com/wms/service/DeliveryOrderServiceImplTest.java`
- [X] T019 [P] [US1] Add service unit test for creating quarantine and `QC_FAIL_OUTBOUND` adjustment records for failed quantity in `backend/src/test/java/com/wms/service/DeliveryOrderServiceImplTest.java`
- [X] T020 [P] [US1] Add service unit test for rejecting warehouse staff outside assigned warehouse on pick/QC save in `backend/src/test/java/com/wms/service/DeliveryOrderServiceImplTest.java`

### Implementation for User Story 1

- [X] T021 [P] [US1] Add `DeliveryOrderPickQcResultRequest` in `backend/src/main/java/com/wms/dto/request/DeliveryOrderPickQcResultRequest.java`
- [X] T022 [P] [US1] Add `DeliveryOrderPickQcRowRequest` in `backend/src/main/java/com/wms/dto/request/DeliveryOrderPickQcRowRequest.java`
- [X] T023 [P] [US1] Add response DTO fields for picked, QC pass, and QC fail progress in `backend/src/main/java/com/wms/dto/response/DeliveryOrderResponse.java`
- [X] T024 [US1] Add `saveDeliveryOrderPickQcResult` method contract to `backend/src/main/java/com/wms/service/DeliveryOrderService.java`
- [X] T025 [US1] Add `PUT /api/v1/delivery-orders/{id}/pick-qc-result` endpoint with validation and OpenAPI metadata in `backend/src/main/java/com/wms/controller/DeliveryOrderController.java`
- [X] T026 [US1] Implement one-shot pick/QC submission validation for active allocations, item totals, staging/quarantine location rules, and warehouse assignment in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [X] T027 [US1] Persist allocation-level `OutboundQcRecord` rows and update item QC summary quantities in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [X] T028 [US1] Move QC-passed quantity from source inventory to outbound staging with version-safe inventory updates in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [X] T029 [US1] Move QC-failed quantity from source inventory to quarantine and create `QuarantineRecord` plus `QC_FAIL_OUTBOUND` adjustment records in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [X] T030 [US1] Write `DELIVERY_ORDER_PICK_COMPLETE` and `OUTBOUND_QC_FAIL_QUARANTINE` audit logs and move the Delivery Order to `QC_PENDING_APPROVAL` in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`

**Checkpoint**: US1 is fully functional and testable as the MVP.

---

## Phase 4: User Story 2 - System blocks duplicate QC submission and supports safe replacement-cycle replay (Priority: P1)

**Goal**: The system prevents duplicate allocation-level QC results, supports idempotent retry for the same payload, and only requires still-active allocations when the Delivery Order returns to `WAITING_PICKING` after replacement planning.

**Independent Test**: Save a successful pick/QC submission, retry it with the same `idempotencyKey` and payload to verify replay is safe, retry with a changed payload to verify conflict, and verify a replacement cycle only accepts replacement or still-`PLANNED` allocations instead of already-passed staging allocations.

### Tests for User Story 2

- [X] T031 [P] [US2] Add service unit test for blocking duplicate allocation submission without matching idempotent replay in `backend/src/test/java/com/wms/service/DeliveryOrderServiceImplTest.java`
- [X] T032 [P] [US2] Add service unit test for returning previous successful result for the same `idempotencyKey` and identical payload in `backend/src/test/java/com/wms/service/DeliveryOrderServiceImplTest.java`
- [X] T033 [P] [US2] Add service unit test for rejecting reused `idempotencyKey` with different payload in `backend/src/test/java/com/wms/service/DeliveryOrderServiceImplTest.java`
- [X] T034 [P] [US2] Add service unit test for replacement cycle accepting only new active allocations without resubmitting already-passed staged allocations in `backend/src/test/java/com/wms/service/DeliveryOrderServiceImplTest.java`
- [X] T035 [P] [US2] Add controller test for duplicate, replay, and idempotency conflict responses on `PUT /api/v1/delivery-orders/{id}/pick-qc-result` in `backend/src/test/java/com/wms/controller/DeliveryOrderControllerTest.java`

### Implementation for User Story 2

- [X] T036 [US2] Add payload hashing and idempotency replay support for pick/QC submission in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [X] T037 [US2] Reject duplicate QC submission when any submitted allocation already has an outbound QC row outside a valid replay in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [X] T038 [US2] Restrict active allocation resolution to replacement allocations or still-`PLANNED` allocations without QC rows after replacement planning in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [X] T039 [US2] Return the previous successful delivery-order result for safe idempotent retry without applying inventory movement again in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [X] T040 [US2] Document duplicate-handling, replay, and idempotency conflict responses in `backend/src/main/java/com/wms/controller/DeliveryOrderController.java`

**Checkpoint**: US2 is independently testable after US1 and preserves one successful QC record per allocation per cycle.

---

## Phase 5: User Story 3 - Storekeeper and Warehouse Manager complete outbound approval or rejection (Priority: P1)

**Goal**: Storekeeper approves outbound quality only after all requested quantity is QC-passed in staging, and Warehouse Manager can then approve release or reject the order by returning staged goods to source bins while keeping failed goods in quarantine.

**Independent Test**: Approve quality on a `QC_PENDING_APPROVAL` Delivery Order only when all requested quantity is satisfied in staging, approve warehouse release from `QC_COMPLETED`, and reject warehouse release by returning all staged pass quantity to source bins while failed goods stay quarantined and the order moves to `REJECTED`.

### Tests for User Story 3

- [X] T041 [P] [US3] Add controller test for `PUT /api/v1/delivery-orders/{id}/quality-approval` happy path and validation failure in `backend/src/test/java/com/wms/controller/DeliveryOrderControllerTest.java`
- [X] T042 [P] [US3] Add controller test for `PUT /api/v1/delivery-orders/{id}/warehouse-approval` and `PUT /api/v1/delivery-orders/{id}/warehouse-reject` in `backend/src/test/java/com/wms/controller/DeliveryOrderControllerTest.java`
- [X] T043 [P] [US3] Add service unit test for blocking quality approval when unresolved fail quantity or missing staged pass quantity remains in `backend/src/test/java/com/wms/service/DeliveryOrderServiceImplTest.java`
- [X] T044 [P] [US3] Add service unit test for warehouse approval moving the order from `QC_COMPLETED` to `WAREHOUSE_APPROVED` in `backend/src/test/java/com/wms/service/DeliveryOrderServiceImplTest.java`
- [X] T045 [P] [US3] Add service unit test for warehouse reject returning staged pass quantity to original inventory rows and keeping failed goods quarantined in `backend/src/test/java/com/wms/service/DeliveryOrderServiceImplTest.java`
- [X] T046 [P] [US3] Add service unit test for rejecting warehouse reject when return rows do not cover all staged pass quantity in `backend/src/test/java/com/wms/service/DeliveryOrderServiceImplTest.java`

### Implementation for User Story 3

- [X] T047 [P] [US3] Add `DeliveryOrderQualityApprovalRequest` in `backend/src/main/java/com/wms/dto/request/DeliveryOrderQualityApprovalRequest.java`
- [X] T048 [P] [US3] Add `DeliveryOrderWarehouseApprovalRequest`, `DeliveryOrderWarehouseRejectRequest`, and `DeliveryOrderWarehouseRejectReturnRequest` in `backend/src/main/java/com/wms/dto/request`
- [X] T049 [US3] Add `approveDeliveryOrderQuality`, `approveDeliveryOrderWarehouseRelease`, and `rejectDeliveryOrderWarehouseRelease` method contracts to `backend/src/main/java/com/wms/service/DeliveryOrderService.java`
- [X] T050 [US3] Add `PUT /api/v1/delivery-orders/{id}/quality-approval`, `PUT /api/v1/delivery-orders/{id}/warehouse-approval`, and `PUT /api/v1/delivery-orders/{id}/warehouse-reject` endpoints with validation and OpenAPI metadata in `backend/src/main/java/com/wms/controller/DeliveryOrderController.java`
- [X] T051 [US3] Implement quality approval validation for `QC_PENDING_APPROVAL`, full requested quantity coverage in staging, and `QC_REPLACEMENT_REQUIRED` handling in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [X] T052 [US3] Implement warehouse approval transition from `QC_COMPLETED` to `WAREHOUSE_APPROVED` with warehouse-manager scope validation in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [X] T053 [US3] Implement warehouse reject return-to-bin validation and version-safe inventory movement from staging back to original source rows in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [X] T054 [US3] Release staging reservation for returned pass quantity, preserve failed quantity in quarantine, store reject reason, and move the Delivery Order to `REJECTED` in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [X] T055 [US3] Write `DELIVERY_ORDER_QC_APPROVE`, `DELIVERY_ORDER_WAREHOUSE_APPROVE`, `DELIVERY_ORDER_WAREHOUSE_REJECT`, and `PICKED_GOODS_RETURN_TO_BIN` audit logs in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`

**Checkpoint**: US3 is independently testable after staged QC-pass inventory exists.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Align documentation, verify integration points, and run feature-level validation across all stories.

- [X] T056 [P] Update controller OpenAPI annotations to match `contracts/delivery-order-pick-qc.openapi.yaml` in `backend/src/main/java/com/wms/controller/DeliveryOrderController.java`
- [X] T057 [P] Review outbound spec alignment for pick/QC submission, replacement re-entry, quality approval, and warehouse reject return flow in `.sdd/specs/004-outbound-delivery-pod/spec.md`
- [X] T058 Run targeted delivery-order controller and service tests in `backend/pom.xml`
- [X] T059 Run backend compile to verify DTO, entity, repository, and service wiring in `backend/pom.xml`
- [X] T060 Verify quickstart scenarios against the implemented API using `.sdd/specs/004-outbound-delivery-pod/features/feature-warehouse-staff-picking-qc/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 Setup**: No dependencies.
- **Phase 2 Foundational**: Depends on setup review and blocks all user stories.
- **Phase 3 US1**: Depends on foundational persistence, repository, and shared validation helpers; this is the MVP.
- **Phase 4 US2**: Depends on US1 because duplicate blocking and replay build on successful QC record persistence and inventory movement.
- **Phase 5 US3**: Depends on US1 for staged/quarantine inventory state and on replacement planning support already present in the outbound flow.
- **Phase 6 Polish**: Depends on whichever user stories are included in the release scope.

### User Story Dependencies

- **US1**: First MVP story because it creates the core warehouse-staff pick/QC submission flow.
- **US2**: Depends on US1 outbound QC record persistence and successful pick/QC workflow.
- **US3**: Depends on QC-passed staging state produced by US1 and replacement-cycle semantics enforced by US2.

### Parallel Opportunities

- T002, T003, and T004 can run in parallel during setup.
- T006 through T012 can run in parallel where file ownership does not overlap.
- T015 through T020 can be written in parallel for US1 tests.
- T021 through T023 can be implemented in parallel before service and controller integration tasks.
- T031 through T035 can be written in parallel for US2 tests.
- T041 through T046 can be written in parallel for US3 tests.
- T047 and T048 can be implemented in parallel before approval-flow integration tasks.
- T056 and T057 can run in parallel during polish.

## Parallel Example: User Story 1

```text
Task: "T015 [P] [US1] Add controller test for pick-qc-result happy path"
Task: "T016 [P] [US1] Add service unit test for invalid picked/pass/fail quantity rule"
Task: "T017 [P] [US1] Add service unit test for partial submission rejection"
Task: "T018 [P] [US1] Add service unit test for staging and quarantine movement"
Task: "T019 [P] [US1] Add service unit test for quarantine and adjustment records"
Task: "T020 [P] [US1] Add service unit test for warehouse-scope rejection"
```

## Parallel Example: User Story 2

```text
Task: "T031 [P] [US2] Add service unit test for duplicate submission blocking"
Task: "T032 [P] [US2] Add service unit test for safe idempotent replay"
Task: "T033 [P] [US2] Add service unit test for idempotency key conflict"
Task: "T034 [P] [US2] Add service unit test for replacement-cycle active allocation set"
Task: "T035 [P] [US2] Add controller test for duplicate and replay responses"
```

## Parallel Example: User Story 3

```text
Task: "T041 [P] [US3] Add controller test for quality approval"
Task: "T042 [P] [US3] Add controller test for warehouse approval and reject"
Task: "T043 [P] [US3] Add service unit test for quality-approval blocking rules"
Task: "T045 [P] [US3] Add service unit test for warehouse reject return-to-bin flow"
Task: "T046 [P] [US3] Add service unit test for incomplete return-row rejection"
```

## Implementation Strategy

### MVP First

1. Complete Phase 1 and Phase 2.
2. Complete Phase 3 (US1).
3. Validate `pick-qc-result` end-to-end with staging and quarantine movement.
4. Stop and demo the warehouse-staff pick/QC flow before adding replay and approval logic.

### Incremental Delivery

1. Deliver US1 one-shot pick/QC submission and inventory movement.
2. Deliver US2 duplicate blocking, idempotency replay, and replacement-cycle submission rules.
3. Deliver US3 quality approval plus warehouse approval/reject flow.
4. Finish polish verification and OpenAPI alignment.

### Validation Checklist

- Every outbound mutation remains transactional, version-safe, and non-negative.
- No `PICKING` status is introduced; warehouse staff submits while the Delivery Order is `WAITING_PICKING`.
- Active pick/QC submission always covers the full required allocation set for the current cycle.
- QC-passed quantity stays reserved in outbound staging until later outbound release flow.
- QC-failed quantity moves to quarantine, creates supporting records, and never becomes regular available stock.
- Duplicate QC submission is blocked unless the same request is safely replayed with matching idempotency data.
- Quality approval is blocked until requested quantity is fully covered by QC-passed staging stock.
- Warehouse reject returns all staged pass quantity to original bins and keeps failed quantity quarantined.
- Service and controller tests cover happy paths and business-error paths for each user story.
