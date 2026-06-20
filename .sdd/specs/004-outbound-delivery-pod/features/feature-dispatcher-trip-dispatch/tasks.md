# Tasks: Dispatcher Trip Dispatch

**Input**: Design documents from `.sdd/specs/004-outbound-delivery-pod/features/feature-dispatcher-trip-dispatch/`

**Prerequisites**: [plan.md](plan.md), [feature-dispatcher-trip-dispatch.md](feature-dispatcher-trip-dispatch.md), [research.md](research.md), [data-model.md](data-model.md), [quickstart.md](quickstart.md), [contracts/trips.openapi.yaml](contracts/trips.openapi.yaml)

**Tests**: Required by the feature spec, quickstart, and constitution for service/business logic and API endpoints.

**Organization**: Tasks are grouped by independently testable user stories.

## Phase 1: Setup

**Purpose**: Review the trip-dispatch seams that this feature extends.

- [X] T001 Review current trip, trip-delivery-order, and delivery entities in `backend/src/main/java/com/wms/entity/Trip.java`, `backend/src/main/java/com/wms/entity/TripDeliveryOrder.java`, and `backend/src/main/java/com/wms/entity/Delivery.java`
- [X] T002 [P] Review current vehicle, driver, and status enums in `backend/src/main/java/com/wms/entity/Vehicle.java`, `backend/src/main/java/com/wms/entity/Driver.java`, and `backend/src/main/java/com/wms/enums`
- [X] T003 [P] Review outbound delivery-order status and staged inventory support in `backend/src/main/java/com/wms/entity/DeliveryOrder.java`, `backend/src/main/java/com/wms/entity/DeliveryOrderItem.java`, and `backend/src/main/java/com/wms/repository/InventoryRepository.java`
- [X] T004 [P] Review current controller, security, and audit patterns in `backend/src/main/java/com/wms/controller` and `backend/src/main/java/com/wms/service`

---

## Phase 2: Foundational

**Purpose**: Add shared trip persistence, validation, and API infrastructure that blocks all user stories.

**Critical**: No user-story implementation should begin until this phase is complete.

- [X] T005 Add Flyway migration for trip dispatch support fields, constraints, and indexes in `backend/src/main/resources/db/migration`
- [X] T006 [P] Add `TripType` support and extend `Trip` entity mappings if needed in `backend/src/main/java/com/wms/entity/Trip.java` and `backend/src/main/java/com/wms/enums/TripType.java`
- [X] T007 [P] Add `TripRepository`, `TripDeliveryOrderRepository`, and `DeliveryRepository` query helpers for active-trip conflict checks and detailed trip loading in `backend/src/main/java/com/wms/repository`
- [X] T008 [P] Extend `DeliveryOrderRepository`, `DeliveryOrderItemRepository`, and `InventoryRepository` with trip-readiness and staged-inventory lookup helpers in `backend/src/main/java/com/wms/repository`
- [X] T009 [P] Extend `VehicleRepository` and `DriverRepository` with warehouse-scoped availability helpers in `backend/src/main/java/com/wms/repository`
- [X] T010 Add `TripResponse` and `TripDeliveryOrderResponse` DTOs in `backend/src/main/java/com/wms/dto/response`
- [X] T011 Add request DTOs for create, update, cancel, depart, complete, and stop-order rows in `backend/src/main/java/com/wms/dto/request`
- [X] T012 Add `TripService` contract and shared warehouse, assignment, capacity, and active-trip validation helpers in `backend/src/main/java/com/wms/service/TripService.java` and `backend/src/main/java/com/wms/service/impl/TripServiceImpl.java`
- [X] T013 Add audit action enum values for `TRIP_CREATE`, `TRIP_UPDATE`, `TRIP_CANCEL`, `TRIP_DEPART`, `DELIVERY_ATTEMPT_CREATE`, and `COMPLETE_TRIP` if missing in `backend/src/main/java/com/wms/enums/AuditAction.java`

**Checkpoint**: Foundation ready for user-story implementation.

---

## Phase 3: User Story 1 - Dispatcher creates a planned outbound trip (Priority: P1) MVP

**Goal**: Dispatcher creates a `PLANNED` trip from warehouse-approved Delivery Orders in the same warehouse with an available vehicle, available driver, unique stop order, and valid capacity totals.

**Independent Test**: Submit a valid `POST /api/v1/trips` request and verify the trip is created in `PLANNED`, trip members persist with stop order, Delivery Orders stay `WAREHOUSE_APPROVED`, and validation rejects cross-warehouse, unavailable-resource, duplicate-stop-order, overload, or active-trip conflicts.

### Tests for User Story 1

- [X] T014 [P] [US1] Add controller test for `POST /api/v1/trips` happy path in `backend/src/test/java/com/wms/controller/TripControllerTest.java`
- [X] T015 [P] [US1] Add service unit test for rejecting cross-warehouse Delivery Orders in `backend/src/test/java/com/wms/service/TripServiceImplTest.java`
- [X] T016 [P] [US1] Add service unit test for rejecting unavailable or wrong-warehouse vehicle and driver in `backend/src/test/java/com/wms/service/TripServiceImplTest.java`
- [X] T017 [P] [US1] Add service unit test for rejecting duplicate stop order and Delivery Order already assigned to another active trip in `backend/src/test/java/com/wms/service/TripServiceImplTest.java`
- [X] T018 [P] [US1] Add service unit test for rejecting vehicle overload by weight or configured volume in `backend/src/test/java/com/wms/service/TripServiceImplTest.java`

### Implementation for User Story 1

- [X] T019 [P] [US1] Add create-trip request DTO validation in `backend/src/main/java/com/wms/dto/request/TripCreateRequest.java` and `backend/src/main/java/com/wms/dto/request/TripDeliveryOrderRequest.java`
- [X] T020 [US1] Add `POST /api/v1/trips` endpoint with validation and OpenAPI metadata in `backend/src/main/java/com/wms/controller/TripController.java`
- [X] T021 [US1] Implement create-trip warehouse scope, Delivery Order readiness, stop order, active-trip, and resource availability validation in `backend/src/main/java/com/wms/service/impl/TripServiceImpl.java`
- [X] T022 [US1] Implement trip capacity calculation and `PLANNED` trip persistence with stop-order rows in `backend/src/main/java/com/wms/service/impl/TripServiceImpl.java`
- [X] T023 [US1] Add trip response mapping for header, totals, and ordered Delivery Orders in `backend/src/main/java/com/wms/service/impl/TripServiceImpl.java`
- [X] T024 [US1] Write `TRIP_CREATE` audit logs with before/after trip state in `backend/src/main/java/com/wms/service/impl/TripServiceImpl.java`

**Checkpoint**: US1 is fully functional and testable as the MVP.

---

## Phase 4: User Story 2 - Dispatcher updates or cancels a planned trip (Priority: P1)

**Goal**: Dispatcher revises a `PLANNED` trip by changing vehicle, driver, planned date, notes, and Delivery Order list, or cancels the trip while keeping Delivery Orders eligible for reassignment.

**Independent Test**: Submit a valid `PUT /api/v1/trips/{id}` request with a revised Delivery Order list and verify the trip stays `PLANNED` after revalidation; submit a valid `PUT /api/v1/trips/{id}/cancel` request and verify the trip moves to `CANCELLED`, Delivery Orders remain `WAREHOUSE_APPROVED`, and the historical vehicle and driver references stay on the trip.

### Tests for User Story 2

- [X] T025 [P] [US2] Add controller test for `PUT /api/v1/trips/{id}` happy path and editability failure in `backend/src/test/java/com/wms/controller/TripControllerTest.java`
- [X] T026 [P] [US2] Add controller test for `PUT /api/v1/trips/{id}/cancel` happy path in `backend/src/test/java/com/wms/controller/TripControllerTest.java`
- [X] T027 [P] [US2] Add service unit test for update revalidating the full revised Delivery Order list and ignoring the current trip in active-trip checks in `backend/src/test/java/com/wms/service/TripServiceImplTest.java`
- [X] T028 [P] [US2] Add service unit test for removed Delivery Orders remaining `WAREHOUSE_APPROVED` after update in `backend/src/test/java/com/wms/service/TripServiceImplTest.java`
- [X] T029 [P] [US2] Add service unit test for cancellation only working from `PLANNED` in `backend/src/test/java/com/wms/service/TripServiceImplTest.java`

### Implementation for User Story 2

- [X] T030 [P] [US2] Add update and cancel request DTO validation in `backend/src/main/java/com/wms/dto/request/TripUpdateRequest.java` and `backend/src/main/java/com/wms/dto/request/TripCancelRequest.java`
- [X] T031 [US2] Add `PUT /api/v1/trips/{id}` and `PUT /api/v1/trips/{id}/cancel` endpoints with validation and OpenAPI metadata in `backend/src/main/java/com/wms/controller/TripController.java`
- [X] T032 [US2] Implement planned-trip update with full list replacement, revalidation, and ordered membership persistence in `backend/src/main/java/com/wms/service/impl/TripServiceImpl.java`
- [X] T033 [US2] Implement planned-trip cancellation, cancellation reason persistence, and active-assignment release semantics in `backend/src/main/java/com/wms/service/impl/TripServiceImpl.java`
- [X] T034 [US2] Write `TRIP_UPDATE` and `TRIP_CANCEL` audit logs with before/after trip membership and resource state in `backend/src/main/java/com/wms/service/impl/TripServiceImpl.java`

**Checkpoint**: US2 is independently testable after US1 and keeps planned-trip management safe before departure.

---

## Phase 5: User Story 3 - Assigned driver departs and completes the trip (Priority: P1)

**Goal**: The assigned driver dispatches a `PLANNED` trip only when all Delivery Orders are still ready and fully QC-passed in staging, then later completes the trip after vehicle return when every assigned Delivery Order is `COMPLETED` or `RETURNED`.

**Independent Test**: Submit a valid `PUT /api/v1/trips/{id}/depart` request and verify staged inventory moves to virtual `IN_TRANSIT`, item `issued_qty` is set, delivery attempts are created, Delivery Orders move to `IN_TRANSIT`, and vehicle/driver move to `ON_TRIP`; then submit a valid `PUT /api/v1/trips/{id}/complete` request after terminal Delivery Order outcomes and verify the trip moves to `COMPLETED` and vehicle/driver return to `AVAILABLE`.

### Tests for User Story 3

- [X] T035 [P] [US3] Add controller test for `PUT /api/v1/trips/{id}/depart` happy path and driver-scope failure in `backend/src/test/java/com/wms/controller/TripControllerTest.java`
- [X] T036 [P] [US3] Add controller test for `PUT /api/v1/trips/{id}/complete` happy path and readiness failure in `backend/src/test/java/com/wms/controller/TripControllerTest.java`
- [X] T037 [P] [US3] Add service unit test for rejecting departure when any Delivery Order is no longer `WAREHOUSE_APPROVED` in `backend/src/test/java/com/wms/service/TripServiceImplTest.java`
- [X] T038 [P] [US3] Add service unit test for rejecting departure when staged QC-pass quantity is below requested quantity in `backend/src/test/java/com/wms/service/TripServiceImplTest.java`
- [X] T039 [P] [US3] Add service unit test for moving staging stock to virtual `IN_TRANSIT`, creating delivery attempts, and marking vehicle/driver `ON_TRIP` in `backend/src/test/java/com/wms/service/TripServiceImplTest.java`
- [X] T040 [P] [US3] Add service unit test for rejecting complete when any assigned Delivery Order is not `COMPLETED` or `RETURNED` in `backend/src/test/java/com/wms/service/TripServiceImplTest.java`
- [X] T041 [P] [US3] Add service unit test for completing the trip and releasing vehicle/driver to `AVAILABLE` in `backend/src/test/java/com/wms/service/TripServiceImplTest.java`

### Implementation for User Story 3

- [X] T042 [P] [US3] Add depart and complete request DTO validation in `backend/src/main/java/com/wms/dto/request/TripDepartRequest.java` and `backend/src/main/java/com/wms/dto/request/TripCompleteRequest.java`
- [X] T043 [US3] Add `PUT /api/v1/trips/{id}/depart` and `PUT /api/v1/trips/{id}/complete` endpoints with validation and OpenAPI metadata in `backend/src/main/java/com/wms/controller/TripController.java`
- [X] T044 [US3] Implement departure assignment checks, Delivery Order readiness validation, and item issued-quantity updates in `backend/src/main/java/com/wms/service/impl/TripServiceImpl.java`
- [X] T045 [US3] Implement version-safe staging-to-`IN_TRANSIT` inventory movement and delivery-attempt initialization in `backend/src/main/java/com/wms/service/impl/TripServiceImpl.java`
- [X] T046 [US3] Implement trip completion terminal-outcome validation and vehicle/driver availability release in `backend/src/main/java/com/wms/service/impl/TripServiceImpl.java`
- [X] T047 [US3] Write `TRIP_DEPART`, `DELIVERY_ATTEMPT_CREATE`, and `COMPLETE_TRIP` audit logs in `backend/src/main/java/com/wms/service/impl/TripServiceImpl.java`

**Checkpoint**: US3 is independently testable after planned trip creation and staged outbound inventory are available.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Align documentation, verify integration points, and run feature-level validation across all stories.

- [X] T048 [P] Update trip endpoint OpenAPI annotations to match `contracts/trips.openapi.yaml` in `backend/src/main/java/com/wms/controller/TripController.java`
- [X] T049 [P] Review outbound spec alignment for trip creation, planned-trip revision, departure, delivery-attempt initialization, and completion in `.sdd/specs/004-outbound-delivery-pod/spec.md`
- [X] T050 Run targeted trip controller and service tests in `backend/pom.xml`
- [X] T051 Run backend compile to verify trip DTO, repository, service, and controller wiring in `backend/pom.xml`
- [X] T052 Verify quickstart scenarios against the implemented API using `.sdd/specs/004-outbound-delivery-pod/features/feature-dispatcher-trip-dispatch/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 Setup**: No dependencies.
- **Phase 2 Foundational**: Depends on setup review and blocks all user stories.
- **Phase 3 US1**: Depends on foundational DTO, repository, validation, and audit support; this is the MVP.
- **Phase 4 US2**: Depends on US1 because planned-trip revision and cancellation build on persisted trip membership and shared active-trip validation.
- **Phase 5 US3**: Depends on US1 trip creation and on outbound staged inventory from earlier outbound features; completion depends on departure support.
- **Phase 6 Polish**: Depends on whichever user stories are included in the release scope.

### User Story Dependencies

- **US1**: First MVP story because it creates the core dispatcher trip-planning flow.
- **US2**: Depends on US1 trip persistence and shared resource validation.
- **US3**: Depends on US1 planned trip existence and on upstream warehouse-approved, QC-passed outbound readiness.

### Parallel Opportunities

- T002, T003, and T004 can run in parallel during setup.
- T006 through T011 can run in parallel where file ownership does not overlap.
- T014 through T018 can be written in parallel for US1 tests.
- T019 and T020 can run in parallel before create-trip service integration.
- T025 through T029 can be written in parallel for US2 tests.
- T030 and T031 can run in parallel before update/cancel service integration.
- T035 through T041 can be written in parallel for US3 tests.
- T042 and T043 can run in parallel before depart/complete service integration.
- T048 and T049 can run in parallel during polish.

## Parallel Example: User Story 1

```text
Task: "T014 [P] [US1] Add controller test for create-trip happy path"
Task: "T015 [P] [US1] Add service unit test for cross-warehouse Delivery Order rejection"
Task: "T016 [P] [US1] Add service unit test for unavailable vehicle and driver rejection"
Task: "T017 [P] [US1] Add service unit test for duplicate stop order and active-trip conflict"
Task: "T018 [P] [US1] Add service unit test for capacity overload rejection"
```

## Parallel Example: User Story 2

```text
Task: "T025 [P] [US2] Add controller test for planned-trip update"
Task: "T026 [P] [US2] Add controller test for planned-trip cancel"
Task: "T027 [P] [US2] Add service unit test for revised-list revalidation"
Task: "T028 [P] [US2] Add service unit test for removed Delivery Orders staying WAREHOUSE_APPROVED"
Task: "T029 [P] [US2] Add service unit test for PLANNED-only cancellation"
```

## Parallel Example: User Story 3

```text
Task: "T035 [P] [US3] Add controller test for depart endpoint"
Task: "T036 [P] [US3] Add controller test for complete endpoint"
Task: "T037 [P] [US3] Add service unit test for departure status guard"
Task: "T039 [P] [US3] Add service unit test for staging-to-IN_TRANSIT movement and delivery attempts"
Task: "T041 [P] [US3] Add service unit test for trip completion resource release"
```

## Implementation Strategy

### MVP First

1. Complete Phase 1 and Phase 2.
2. Complete Phase 3 (US1).
3. Validate trip creation end-to-end with warehouse, availability, stop-order, and capacity rules.
4. Stop and demo the dispatcher trip-planning flow before adding planned-trip management and departure logic.

### Incremental Delivery

1. Deliver US1 trip creation and `PLANNED` membership persistence.
2. Deliver US2 planned-trip update and cancellation flows.
3. Deliver US3 driver departure, staging-to-`IN_TRANSIT` movement, delivery-attempt initialization, and trip completion.
4. Finish polish verification and OpenAPI alignment.

### Validation Checklist

- Every trip mutation remains warehouse-scoped and role-protected.
- No Delivery Order is assigned to more than one active trip.
- Trip create/update always validate same-warehouse membership, unique stop order, and vehicle capacity.
- Departure only dispatches Delivery Orders that are still `WAREHOUSE_APPROVED` and fully QC-passed in staging.
- Staging inventory movement to virtual `IN_TRANSIT` remains transactional, version-safe, and non-negative.
- Delivery attempts are created only at departure and start in `IN_TRANSIT`.
- Trip completion only happens after all assigned Delivery Orders are `COMPLETED` or `RETURNED`.
- Returned goods remain in virtual `IN_TRANSIT` until the separate return flow handles them.
- Service and controller tests cover happy paths and business-error paths for each user story.
