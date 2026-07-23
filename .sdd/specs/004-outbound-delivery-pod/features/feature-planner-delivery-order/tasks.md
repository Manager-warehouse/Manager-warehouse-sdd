# Tasks: Planner Delivery Order & Credit/Stock Reservation

**Input**: Design documents from `.sdd/specs/004-outbound-delivery-pod/features/feature-planner-delivery-order/`

**Prerequisites**: [plan.md](plan.md), [feature-planner-delivery-order.md](feature-planner-delivery-order.md), [research.md](research.md), [data-model.md](data-model.md), [quickstart.md](quickstart.md), [contracts/delivery-orders.openapi.yaml](contracts/delivery-orders.openapi.yaml)

**Tests**: Required by quickstart and constitution for service/business logic and API endpoints.

**Organization**: Tasks are grouped by independently testable user stories.

## Phase 1: Setup

**Purpose**: Prepare the existing Delivery Order module for this feature.

- [x] T001 Review current Delivery Order create/cancel implementation in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [x] T002 [P] Review current Delivery Order request/response DTOs in `backend/src/main/java/com/wms/dto/request/DeliveryOrderCreateRequest.java` and `backend/src/main/java/com/wms/dto/request/DeliveryOrderItemCreateRequest.java`
- [x] T003 [P] Review existing reservation model and repository in `backend/src/main/java/com/wms/entity/WarehouseProductReservation.java` and `backend/src/main/java/com/wms/repository/WarehouseProductReservationRepository.java`
- [x] T004 [P] Review current Delivery Order OpenAPI annotations in `backend/src/main/java/com/wms/controller/DeliveryOrderController.java`

---

## Phase 2: Foundational

**Purpose**: Shared infrastructure that blocks all user stories.

- [x] T005 Add or verify outbound error codes for `WAREHOUSE_SCOPE_FORBIDDEN`, `CREDIT_HOLD`, `INSUFFICIENT_STOCK`, `DELIVERY_ORDER_CANCEL_FORBIDDEN`, and `INVENTORY_VERSION_CONFLICT` in `backend/src/main/java/com/wms/exception`
- [x] T006 Add repository query for active regular quality-valid inventory availability by warehouse/product in `backend/src/main/java/com/wms/repository/InventoryRepository.java`
- [x] T007 Add repository query for unpaid dealer invoices overdue beyond the dealer's configured payment term days in `backend/src/main/java/com/wms/repository/InvoiceRepository.java`
- [x] T008 Add warehouse assignment validation helper for Planner and Warehouse Manager scope in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [x] T009 Add server-side order value calculation helper using backend pricing data in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [x] T010 Add reservation delta helper for warehouse/product aggregate reservations with optimistic locking in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [x] T011 Add audit snapshot helpers for credit check result and reservation deltas in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`

**Checkpoint**: Foundation ready for user-story implementation.

---

## Phase 3: User Story 1 - Planner creates a valid Delivery Order (Priority: P1) MVP

**Goal**: Planner assigned to the selected warehouse creates a `NEW` Delivery Order when dealer credit and warehouse stock are valid.

**Independent Test**: Submit a valid `POST /api/v1/delivery-orders` request and verify DO status `NEW`, DO item `reserved_qty`, `warehouse_product_reservations.reserved_qty`, and `DELIVERY_ORDER_CREATE` audit.

### Tests for User Story 1

- [x] T012 [P] [US1] Add service unit test for successful create with `current_balance + order_value = credit_limit` in `backend/src/test/java/com/wms/service/DeliveryOrderServiceImplTest.java`
- [x] T013 [P] [US1] Add service unit test for successful warehouse/product reservation increment with optimistic version in `backend/src/test/java/com/wms/service/DeliveryOrderServiceImplTest.java`
- [x] T014 [P] [US1] Add integration test for `POST /api/v1/delivery-orders` happy path in `backend/src/test/java/com/wms/controller/DeliveryOrderControllerIntegrationTest.java`

### Implementation for User Story 1

- [x] T015 [US1] Update `createDeliveryOrder` to enforce Planner warehouse assignment before persistence in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [x] T016 [US1] Update `createDeliveryOrder` to calculate backend order value and run dealer credit checks before persistence in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [x] T017 [US1] Update `createDeliveryOrder` to calculate valid regular warehouse availability and reserve `warehouse_product_reservations` in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [x] T018 [US1] Set each `DeliveryOrderItem.reservedQty` to requested quantity and leave batch/location unset on create in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [x] T019 [US1] Write `DELIVERY_ORDER_CREATE` audit with credit-check result and reservation deltas in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [x] T020 [US1] Update `POST /api/v1/delivery-orders` OpenAPI annotations and response codes in `backend/src/main/java/com/wms/controller/DeliveryOrderController.java`

**Checkpoint**: US1 can be tested independently as the MVP.

---

## Phase 4: User Story 2 - Planner gets clear rejection for invalid create request (Priority: P1)

**Goal**: System rejects Delivery Order creation when credit, overdue invoice, warehouse scope, or stock availability fails, without creating DO or reservations.

**Independent Test**: Submit invalid create requests and verify proper error code, no Delivery Order, no reservation delta, and no success audit.

### Tests for User Story 2

- [x] T021 [P] [US2] Add service unit test for `CREDIT_HOLD` dealer rejection in `backend/src/test/java/com/wms/service/DeliveryOrderServiceImplTest.java`
- [x] T022 [P] [US2] Add service unit test for overdue invoice rejection in `backend/src/test/java/com/wms/service/DeliveryOrderServiceImplTest.java`
- [x] T023 [P] [US2] Add service unit test for Planner warehouse scope rejection in `backend/src/test/java/com/wms/service/DeliveryOrderServiceImplTest.java`
- [x] T024 [P] [US2] Add service unit test for availability subtracting `warehouse_product_reservations.reserved_qty` in `backend/src/test/java/com/wms/service/DeliveryOrderServiceImplTest.java`
- [x] T025 [P] [US2] Add service unit test excluding quarantine and non-quality inventory from availability in `backend/src/test/java/com/wms/service/DeliveryOrderServiceImplTest.java`
- [x] T026 [P] [US2] Add integration tests for credit, stock, and warehouse-scope errors on `POST /api/v1/delivery-orders` in `backend/src/test/java/com/wms/controller/DeliveryOrderControllerIntegrationTest.java`

### Implementation for User Story 2

- [x] T027 [US2] Return `WAREHOUSE_SCOPE_FORBIDDEN` when Planner is not assigned to selected warehouse in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [x] T028 [US2] Return `CREDIT_HOLD` for dealer credit hold, credit limit exceeded, or unpaid invoice overdue beyond the dealer's configured payment term days in `backend/src/main/java/com/wms/service/order_fulfillment/impl/DeliveryOrderServiceImpl.java`
- [x] T029 [US2] Return `INSUFFICIENT_STOCK` without suggested alternative warehouses when the selected warehouse lacks enough valid availability in `backend/src/main/java/com/wms/service/order_fulfillment/impl/DeliveryOrderServiceImpl.java`
- [x] T030 [US2] Ensure rejected create requests do not persist Delivery Order, DeliveryOrderItem, reservation delta, or success audit in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [x] T031 [US2] Document create error responses in `backend/src/main/java/com/wms/controller/DeliveryOrderController.java`

**Checkpoint**: US2 error cases are independently testable without US3.

---

## Phase 5: User Story 3 - Warehouse Manager cancels before warehouse approval (Priority: P1)

**Goal**: Warehouse Manager cancels a Delivery Order before `WAREHOUSE_APPROVED`, releases reservations, and records audit.

**Independent Test**: Cancel a pre-approval DO and verify status `CANCELLED`, reservation release, cancel reason, and `DELIVERY_ORDER_CANCEL` audit; then verify cancellation is rejected at `WAREHOUSE_APPROVED` or by non-manager actor.

### Tests for User Story 3

- [x] T032 [P] [US3] Add service unit test for successful pre-approval cancellation releasing planner-level reservation in `backend/src/test/java/com/wms/service/DeliveryOrderServiceImplTest.java`
- [x] T033 [P] [US3] Add service unit test for cancellation rejection at `WAREHOUSE_APPROVED` or later in `backend/src/test/java/com/wms/service/DeliveryOrderServiceImplTest.java`
- [x] T034 [P] [US3] Add service unit test for non-Warehouse Manager cancellation rejection in `backend/src/test/java/com/wms/service/DeliveryOrderServiceImplTest.java`
- [x] T035 [P] [US3] Add integration tests for `PUT /api/v1/delivery-orders/{id}/cancel` happy path and forbidden paths in `backend/src/test/java/com/wms/controller/DeliveryOrderControllerIntegrationTest.java`

### Implementation for User Story 3

- [x] T036 [P] [US3] Add `DeliveryOrderCancelRequest` with required `cancelReason` in `backend/src/main/java/com/wms/dto/request/DeliveryOrderCancelRequest.java`
- [x] T037 [US3] Replace current delete-style cancellation with `PUT /api/v1/delivery-orders/{id}/cancel` request handling in `backend/src/main/java/com/wms/controller/DeliveryOrderController.java`
- [x] T038 [US3] Enforce Warehouse Manager role and warehouse assignment for cancellation in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [x] T039 [US3] Reject cancellation when Delivery Order is `WAREHOUSE_APPROVED` or later in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [x] T040 [US3] Release remaining `DeliveryOrderItem.reservedQty` and `warehouse_product_reservations.reserved_qty` during cancellation in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [x] T041 [US3] Release concrete inventory reservation if picking plan already assigned inventory before cancellation in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [x] T042 [US3] Persist cancel reason, set status `CANCELLED`, and write `DELIVERY_ORDER_CANCEL` audit in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [x] T043 [US3] Document cancel endpoint request and responses in `backend/src/main/java/com/wms/controller/DeliveryOrderController.java`

**Checkpoint**: US3 is independently testable after foundational reservation helpers exist.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Verification and documentation alignment after stories are complete.

- [x] T044 [P] Update generated/static OpenAPI documentation to match `contracts/delivery-orders.openapi.yaml` in `backend/src/main/java/com/wms/controller/DeliveryOrderController.java`
- [x] T045 [P] Review outbound spec references for create/cancel behavior in `.sdd/specs/004-outbound-delivery-pod/spec.md`
- [x] T046 Run backend tests for Delivery Order service and controller in `backend/pom.xml`
- [x] T047 Run backend compile to catch API/DTO/entity wiring errors in `backend/pom.xml`
- [ ] T048 Verify quickstart scenarios manually against local API using `.sdd/specs/004-outbound-delivery-pod/features/feature-planner-delivery-order/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 Setup**: No dependencies.
- **Phase 2 Foundational**: Depends on setup review.
- **Phase 3 US1**: Depends on foundational helpers and is the MVP.
- **Phase 4 US2**: Depends on foundational helpers; can be implemented after or alongside US1 once shared create flow exists.
- **Phase 5 US3**: Depends on foundational reservation helpers and can be implemented independently from US2.
- **Phase 6 Polish**: Depends on implemented stories selected for release.

### User Story Dependencies

- **US1**: First MVP story because it establishes create flow and reservation mutation.
- **US2**: Uses the same create validation seams as US1; can be delivered after US1 or parallel with clear service ownership.
- **US3**: Uses shared reservation release helpers; no dependency on US2 behavior.

### Parallel Opportunities

- T002, T003, and T004 can run in parallel.
- T012, T013, and T014 can be written in parallel.
- T021 through T026 can be written in parallel.
- T032 through T035 can be written in parallel.
- T036 can be implemented in parallel with service cancellation tasks if the endpoint contract is stable.
- T044 and T045 can run in parallel during polish.

## Parallel Example: User Story 1

```text
Task: "T012 [P] [US1] Add service unit test for successful create with credit-limit equality"
Task: "T013 [P] [US1] Add service unit test for reservation increment"
Task: "T014 [P] [US1] Add integration test for POST happy path"
```

## Parallel Example: User Story 2

```text
Task: "T021 [P] [US2] Add service unit test for CREDIT_HOLD"
Task: "T022 [P] [US2] Add service unit test for overdue invoice"
Task: "T023 [P] [US2] Add service unit test for warehouse scope"
Task: "T024 [P] [US2] Add service unit test for aggregate reservation availability"
Task: "T025 [P] [US2] Add service unit test excluding quarantine inventory"
Task: "T026 [P] [US2] Add integration tests for create error paths"
```

## Parallel Example: User Story 3

```text
Task: "T032 [P] [US3] Add service unit test for successful cancellation"
Task: "T033 [P] [US3] Add service unit test for state rejection"
Task: "T034 [P] [US3] Add service unit test for actor rejection"
Task: "T035 [P] [US3] Add integration tests for cancel endpoint"
```

## Implementation Strategy

### MVP First

1. Complete Phase 1 and Phase 2.
2. Complete Phase 3 (US1).
3. Validate successful `POST /api/v1/delivery-orders` end-to-end.
4. Stop and demo create flow before adding broader rejection/cancel behavior.

### Incremental Delivery

1. Deliver US1 create success with reservation and audit.
2. Deliver US2 create rejection and suggestion behavior.
3. Deliver US3 Warehouse Manager cancellation.
4. Finish polish verification and OpenAPI alignment.

### Validation Checklist

- Every create/cancel mutation is transactional.
- No negative reservation or inventory values are possible.
- No concrete inventory row is assigned during Planner create.
- All successful warehouse mutations write audit logs.
- All write endpoints use request DTO validation.
- Service unit tests cover business invariants.
- Integration tests cover HTTP happy and error paths.
