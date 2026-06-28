# Tasks: Storekeeper Picking Plan

**Input**: Design documents from `.sdd/specs/004-outbound-delivery-pod/features/feature-storekeeper-picking-plan/`

**Prerequisites**: [plan.md](plan.md), [feature-storekeeper-picking-plan.md](feature-storekeeper-picking-plan.md), [research.md](research.md), [data-model.md](data-model.md), [quickstart.md](quickstart.md), [contracts/delivery-order-picking.openapi.yaml](contracts/delivery-order-picking.openapi.yaml)

**Tests**: Required by the feature spec, quickstart, and constitution for service/business logic and API endpoints.

**Organization**: Tasks are grouped by independently testable user stories.

## Phase 1: Setup

**Purpose**: Review the existing outbound delivery-order implementation seams that this feature extends.

- [X] T001 Review current Delivery Order outbound flows in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [X] T002 [P] Review current Delivery Order controller and security annotations in `backend/src/main/java/com/wms/controller/DeliveryOrderController.java`
- [X] T003 [P] Review current outbound DTO and response mapping classes in `backend/src/main/java/com/wms/dto/request` and `backend/src/main/java/com/wms/dto/response`
- [X] T004 [P] Review current inventory, reservation, and audit support classes in `backend/src/main/java/com/wms/entity/Inventory.java`, `backend/src/main/java/com/wms/entity/WarehouseProductReservation.java`, and `backend/src/main/java/com/wms/util/PartnerAuditUtil.java`

---

## Phase 2: Foundational

**Purpose**: Add shared schema, persistence, and service infrastructure that blocks all user stories.

**⚠️ CRITICAL**: No user-story implementation should begin until this phase is complete.

- [X] T005 Add Flyway migration for `delivery_order_item_allocations`, `delivery_order_item_return_to_bin_records`, and `delivery_order_item_replacements` in `backend/src/main/resources/db/migration`
- [X] T006 [P] Add `DeliveryOrderItemAllocation` entity in `backend/src/main/java/com/wms/entity/DeliveryOrderItemAllocation.java`
- [X] T007 [P] Add `DeliveryOrderItemReturnToBinRecord` entity in `backend/src/main/java/com/wms/entity/DeliveryOrderItemReturnToBinRecord.java`
- [X] T008 [P] Add `DeliveryOrderItemReplacement` entity in `backend/src/main/java/com/wms/entity/DeliveryOrderItemReplacement.java`
- [X] T009 [P] Add `DeliveryOrderItemAllocationRepository` in `backend/src/main/java/com/wms/repository/DeliveryOrderItemAllocationRepository.java`
- [X] T010 [P] Add `DeliveryOrderItemReturnToBinRecordRepository` in `backend/src/main/java/com/wms/repository/DeliveryOrderItemReturnToBinRecordRepository.java`
- [X] T011 [P] Add `DeliveryOrderItemReplacementRepository` in `backend/src/main/java/com/wms/repository/DeliveryOrderItemReplacementRepository.java`
- [X] T012 Extend `InventoryRepository` with FIFO-ranked valid-stock lookup, concrete reservation lookup, and version-safe helpers in `backend/src/main/java/com/wms/repository/InventoryRepository.java`
- [X] T013 [P] Extend `DeliveryOrderRepository` and `DeliveryOrderItemRepository` with detail-loading queries for allocations and outbound statuses in `backend/src/main/java/com/wms/repository/DeliveryOrderRepository.java` and `backend/src/main/java/com/wms/repository/DeliveryOrderItemRepository.java`
- [X] T014 [P] Extend `WarehouseProductReservationRepository` with optimistic-lock-aware lookup helpers for picking-plan reservation transfer in `backend/src/main/java/com/wms/repository/WarehouseProductReservationRepository.java`
- [X] T015 Add shared outbound planning helpers for warehouse scope, status validation, per-item allocation totals, and audit snapshots in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [X] T016 Add outbound audit actions for `PICKING_PLAN_SAVE`, `PICKED_GOODS_RETURN_TO_BIN`, and `PICKING_REPLACEMENT_SAVE` in `backend/src/main/java/com/wms/enums/AuditAction.java`

**Checkpoint**: Foundation ready for user-story implementation.

---

## Phase 3: User Story 1 - Storekeeper saves the initial picking plan (Priority: P1) 🎯 MVP

**Goal**: Storekeeper assigned to the Delivery Order warehouse saves a complete FIFO-based concrete picking plan and moves the order from `NEW` to `WAITING_PICKING`.

**Independent Test**: Submit a valid `PUT /api/v1/delivery-orders/{id}/picking-plan` request for a `NEW` Delivery Order and verify concrete allocations persist, `warehouse_product_reservations.reserved_qty` decreases, selected `inventories.reserved_qty` increases, audit is written, and the order moves to `WAITING_PICKING`.

### Tests for User Story 1

- [X] T017 [P] [US1] Add controller test for `PUT /api/v1/delivery-orders/{id}/picking-plan` happy path in `backend/src/test/java/com/wms/controller/DeliveryOrderControllerTest.java`
- [X] T018 [P] [US1] Add service unit test for initial plan converting warehouse/product reservation into concrete inventory reservation in `backend/src/test/java/com/wms/service/DeliveryOrderServiceImplTest.java`
- [X] T019 [P] [US1] Add service unit test for rejecting incomplete per-item planned quantity in `backend/src/test/java/com/wms/service/DeliveryOrderServiceImplTest.java`
- [X] T020 [P] [US1] Add service unit test for rejecting storekeeper outside assigned warehouse on initial save in `backend/src/test/java/com/wms/service/DeliveryOrderServiceImplTest.java`

### Implementation for User Story 1

- [X] T021 [P] [US1] Add `DeliveryOrderPickingPlanRequest` in `backend/src/main/java/com/wms/dto/request/DeliveryOrderPickingPlanRequest.java`
- [X] T022 [P] [US1] Add `DeliveryOrderAllocationRequest` in `backend/src/main/java/com/wms/dto/request/DeliveryOrderAllocationRequest.java`
- [X] T023 [P] [US1] Add `DeliveryOrderReturnToBinRequest` in `backend/src/main/java/com/wms/dto/request/DeliveryOrderReturnToBinRequest.java`
- [X] T024 [P] [US1] Add `DeliveryOrderAllocationResponse` in `backend/src/main/java/com/wms/dto/response/DeliveryOrderAllocationResponse.java`
- [X] T025 [US1] Extend `DeliveryOrderResponse` and `DeliveryOrderMapper` to include current allocations and planned quantities in `backend/src/main/java/com/wms/dto/response/DeliveryOrderResponse.java` and `backend/src/main/java/com/wms/mapper/DeliveryOrderMapper.java`
- [X] T026 [US1] Add `saveDeliveryOrderPickingPlan` method contract to `DeliveryOrderService` in `backend/src/main/java/com/wms/service/DeliveryOrderService.java`
- [X] T027 [US1] Add `PUT /api/v1/delivery-orders/{id}/picking-plan` endpoint with validation and OpenAPI metadata in `backend/src/main/java/com/wms/controller/DeliveryOrderController.java`
- [X] T028 [US1] Implement initial picking-plan save with FIFO-valid inventory validation, allocation persistence, planner-reservation transfer, status change, and `PICKING_PLAN_SAVE` audit in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [X] T029 [US1] Update `DeliveryOrderItem` summary quantity fields from allocation totals during initial plan save in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`

**Checkpoint**: US1 is fully functional and testable as the MVP.

---

## Phase 4: User Story 2 - Storekeeper revises a picking plan with conditional return-to-bin (Priority: P1)

**Goal**: Storekeeper revises a `WAITING_PICKING` plan, releasing and reserving concrete rows by delta, while enforcing return-to-bin only for changed picked allocations.

**Independent Test**: Submit a revised `PUT /api/v1/delivery-orders/{id}/picking-plan` request for a `WAITING_PICKING` Delivery Order and verify changed unpicked allocations re-reserve by delta; then verify changed picked allocations require valid `returnToBinRecords`, unchanged picked allocations do not, and valid returns write `PICKED_GOODS_RETURN_TO_BIN` audit before the revised plan is saved.

### Tests for User Story 2

- [X] T030 [P] [US2] Add service unit test for revising a `WAITING_PICKING` plan by releasing removed concrete reservation and reserving added rows in `backend/src/test/java/com/wms/service/DeliveryOrderServiceImplTest.java`
- [X] T031 [P] [US2] Add service unit test for requiring `returnToBinRecords` when a picked allocation is removed or reduced in `backend/src/test/java/com/wms/service/DeliveryOrderServiceImplTest.java`
- [X] T032 [P] [US2] Add service unit test for allowing unchanged picked allocations without return records in `backend/src/test/java/com/wms/service/DeliveryOrderServiceImplTest.java`
- [X] T033 [P] [US2] Add service unit test for rejecting invalid return quantity or wrong original location in `backend/src/test/java/com/wms/service/DeliveryOrderServiceImplTest.java`
- [X] T034 [P] [US2] Add controller test for `PICKED_GOODS_RETURN_REQUIRED` and happy-path revision on `PUT /api/v1/delivery-orders/{id}/picking-plan` in `backend/src/test/java/com/wms/controller/DeliveryOrderControllerTest.java`

### Implementation for User Story 2

- [X] T035 [US2] Extend `saveDeliveryOrderPickingPlan` in `DeliveryOrderServiceImpl` to diff current and requested allocations and adjust concrete reservations by delta in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [X] T036 [US2] Validate changed picked allocations against `outbound_qc_records` before allowing removal or reduction in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [X] T037 [US2] Persist return-to-bin records and move returned quantity back to the original inventory row before saving revised allocations in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [X] T038 [US2] Write `PICKED_GOODS_RETURN_TO_BIN` audit with source state, original allocation, and before/after inventory state in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [X] T039 [US2] Keep `WAITING_PICKING` status and update allocation summaries after successful revision in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [X] T040 [US2] Document revision-specific error responses and `returnToBinRecords` payload rules in `backend/src/main/java/com/wms/controller/DeliveryOrderController.java`

**Checkpoint**: US2 is independently testable after US1 and does not require replacement flow.

---

## Phase 5: User Story 3 - Storekeeper saves replacement plan after QC fail (Priority: P1)

**Goal**: Storekeeper selects replacement goods for unresolved QC-failed quantity while the Delivery Order is `QC_PENDING_APPROVAL`, records replacement history, reserves replacement stock, and moves the order back to `WAITING_PICKING`.

**Independent Test**: Submit a valid `PUT /api/v1/delivery-orders/{id}/replacement-plan` request for a `QC_PENDING_APPROVAL` Delivery Order with QC-failed quantity and verify replacement history and replacement allocation persist, replacement inventory is reserved, audit is written, and the order returns to `WAITING_PICKING`.

### Tests for User Story 3

- [X] T041 [P] [US3] Add controller test for `PUT /api/v1/delivery-orders/{id}/replacement-plan` happy path and forbidden status in `backend/src/test/java/com/wms/controller/DeliveryOrderControllerTest.java`
- [X] T042 [P] [US3] Add service unit test for saving replacement plan only from `QC_PENDING_APPROVAL` in `backend/src/test/java/com/wms/service/DeliveryOrderServiceImplTest.java`
- [X] T043 [P] [US3] Add service unit test for rejecting replacement quantity greater than unresolved QC-failed quantity in `backend/src/test/java/com/wms/service/DeliveryOrderServiceImplTest.java`
- [X] T044 [P] [US3] Add service unit test for optimistic-lock conflict on replacement inventory reservation in `backend/src/test/java/com/wms/service/DeliveryOrderServiceImplTest.java`

### Implementation for User Story 3

- [X] T045 [P] [US3] Add `DeliveryOrderReplacementPlanRequest` in `backend/src/main/java/com/wms/dto/request/DeliveryOrderReplacementPlanRequest.java`
- [X] T046 [P] [US3] Add `DeliveryOrderReplacementAllocationRequest` in `backend/src/main/java/com/wms/dto/request/DeliveryOrderReplacementAllocationRequest.java`
- [X] T047 [US3] Add `saveDeliveryOrderReplacementPlan` method contract to `DeliveryOrderService` in `backend/src/main/java/com/wms/service/DeliveryOrderService.java`
- [X] T048 [US3] Add `PUT /api/v1/delivery-orders/{id}/replacement-plan` endpoint with validation and OpenAPI metadata in `backend/src/main/java/com/wms/controller/DeliveryOrderController.java`
- [X] T049 [US3] Implement replacement-plan validation, replacement history persistence, replacement allocation persistence, replacement inventory reservation, and status move back to `WAITING_PICKING` in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [X] T050 [US3] Write `PICKING_REPLACEMENT_SAVE` audit with failed source, replacement source, quantity, and actor in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [X] T051 [US3] Expose replacement allocations and replacement metadata through response mapping in `backend/src/main/java/com/wms/mapper/DeliveryOrderMapper.java`

**Checkpoint**: US3 is independently testable once QC-fail data exists and does not depend on warehouse-manager approval flow.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Align documentation, verify integration points, and run feature-level validation across all stories.

- [X] T052 [P] Update controller OpenAPI annotations to match `contracts/delivery-order-picking.openapi.yaml` in `backend/src/main/java/com/wms/controller/DeliveryOrderController.java`
- [X] T053 [P] Review outbound spec alignment for picking-plan, return-to-bin, and replacement behaviors in `.sdd/specs/004-outbound-delivery-pod/spec.md`
- [X] T054 Run targeted delivery-order controller and service tests in `backend/pom.xml`
- [X] T055 Run backend compile to verify DTO, entity, repository, and service wiring in `backend/pom.xml`
- [X] T056 Verify quickstart scenarios against the local API using `.sdd/specs/004-outbound-delivery-pod/features/feature-storekeeper-picking-plan/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 Setup**: No dependencies.
- **Phase 2 Foundational**: Depends on setup review and blocks all user stories.
- **Phase 3 US1**: Depends on foundational schema, repository, and service helpers; this is the MVP.
- **Phase 4 US2**: Depends on US1 because revision builds on existing allocation save and concrete reservation flow.
- **Phase 5 US3**: Depends on foundational work and allocation persistence; it can start after US1 once the allocation model exists.
- **Phase 6 Polish**: Depends on whichever user stories are included in the release scope.

### User Story Dependencies

- **US1**: First MVP story because it creates the concrete picking-plan foundation.
- **US2**: Depends on US1 allocation persistence and reservation-transfer behavior.
- **US3**: Depends on allocation persistence from US1 and downstream QC-fail data, but not on US2 revision behavior.

### Parallel Opportunities

- T002, T003, and T004 can run in parallel during setup.
- T006 through T014 can run in parallel where file ownership does not overlap.
- T017 through T020 can be written in parallel for US1 tests.
- T021 through T024 can be implemented in parallel before T025 through T029 consolidate the flow.
- T030 through T034 can be written in parallel for US2 tests.
- T041 through T044 can be written in parallel for US3 tests.
- T045 and T046 can be implemented in parallel before service/controller integration tasks.
- T052 and T053 can run in parallel during polish.

## Parallel Example: User Story 1

```text
Task: "T017 [P] [US1] Add controller test for picking-plan happy path"
Task: "T018 [P] [US1] Add service unit test for initial reservation transfer"
Task: "T019 [P] [US1] Add service unit test for incomplete planned quantity rejection"
Task: "T020 [P] [US1] Add service unit test for warehouse-scope rejection"
```

## Parallel Example: User Story 2

```text
Task: "T030 [P] [US2] Add service unit test for reservation delta on revision"
Task: "T031 [P] [US2] Add service unit test requiring return-to-bin for changed picked allocation"
Task: "T032 [P] [US2] Add service unit test for unchanged picked allocation"
Task: "T033 [P] [US2] Add service unit test for invalid return record"
Task: "T034 [P] [US2] Add controller test for revision error and success paths"
```

## Parallel Example: User Story 3

```text
Task: "T041 [P] [US3] Add controller test for replacement-plan endpoint"
Task: "T042 [P] [US3] Add service unit test for QC_PENDING_APPROVAL requirement"
Task: "T043 [P] [US3] Add service unit test for unresolved fail quantity validation"
Task: "T044 [P] [US3] Add service unit test for optimistic-lock conflict on replacement inventory"
```

## Implementation Strategy

### MVP First

1. Complete Phase 1 and Phase 2.
2. Complete Phase 3 (US1).
3. Validate the initial `picking-plan` save end-to-end.
4. Stop and demo the storekeeper planning flow before adding revision and replacement logic.

### Incremental Delivery

1. Deliver US1 initial picking-plan save and reservation transfer.
2. Deliver US2 revision with conditional return-to-bin.
3. Deliver US3 replacement plan after QC fail.
4. Finish polish verification and OpenAPI alignment.

### Validation Checklist

- Every outbound mutation remains transactional and version-safe.
- FIFO candidate selection excludes quarantine, staging, In-Transit, inactive, and non-available stock.
- Planner-level reservation is converted to concrete reservation without double-counting.
- Picked allocation changes require return-to-bin only when the allocation is reduced or removed.
- Replacement planning preserves failed-to-replacement traceability and returns the order to `WAITING_PICKING`.
- All write endpoints use validated DTOs and emit audit logs.
- Service and controller tests cover happy paths and business-error paths for each user story.
