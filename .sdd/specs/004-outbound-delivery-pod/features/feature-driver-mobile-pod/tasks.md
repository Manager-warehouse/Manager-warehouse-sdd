# Tasks: Driver Mobile POD

**Input**: Design documents from `.sdd/specs/004-outbound-delivery-pod/features/feature-driver-mobile-pod/`

**Prerequisites**: [plan.md](plan.md), [feature-driver-mobile-pod.md](feature-driver-mobile-pod.md), [research.md](research.md), [data-model.md](data-model.md), [quickstart.md](quickstart.md), [contracts/driver-pod.openapi.yaml](contracts/driver-pod.openapi.yaml)

**Tests**: Required by the feature spec, quickstart, and constitution for service/business logic and API endpoints.

**Organization**: Tasks are grouped by independently testable user stories.

## Phase 1: Setup

**Purpose**: Review the driver mobile POD seams that this feature extends.

- [X] T001 Review current delivery-attempt and OTP entities in `backend/src/main/java/com/wms/entity/Delivery.java` and `backend/src/main/java/com/wms/entity/DeliveryOtpAttempt.java`
- [X] T002 [P] Review current trip, driver, and Delivery Order status models in `backend/src/main/java/com/wms/entity/Trip.java`, `backend/src/main/java/com/wms/entity/Driver.java`, and `backend/src/main/java/com/wms/enums`
- [X] T003 [P] Review current outbound inventory, invoice, and receivable seams in `backend/src/main/java/com/wms/entity/Inventory.java`, `backend/src/main/java/com/wms/entity/Invoice.java`, and `backend/src/main/java/com/wms/repository`
- [X] T004 [P] Review current mail, auth, multipart upload, and audit patterns in `backend/src/main/java/com/wms/config/MailConfig.java`, `backend/src/main/java/com/wms/service/AuthService.java`, and `backend/src/main/java/com/wms/service`

---

## Phase 2: Foundational

**Purpose**: Add shared delivery-attempt, OTP, file-upload, and API infrastructure that blocks all user stories.

**Critical**: No user-story implementation should begin until this phase is complete.

- [X] T005 Add Flyway migration for POD evidence, OTP lifecycle, admin reset, and delivery-attempt support fields or indexes in `backend/src/main/resources/db/migration`
- [X] T006 [P] Extend `DeliveryRepository` and `DeliveryOtpAttemptRepository` with current-attempt and single-row OTP lookup helpers in `backend/src/main/java/com/wms/repository`
- [X] T007 [P] Extend `TripRepository`, `TripDeliveryOrderRepository`, and `DriverRepository` with driver-assignment scoped trip queries in `backend/src/main/java/com/wms/repository`
- [X] T008 [P] Extend `DeliveryOrderRepository`, `DeliveryOrderItemRepository`, `InventoryRepository`, and `InvoiceRepository` with delivery-confirmation and trip-completion lookup helpers in `backend/src/main/java/com/wms/repository`
- [X] T009 [P] Add response DTOs for driver trip detail, delivery attempt detail, and OTP state in `backend/src/main/java/com/wms/dto/response`
- [X] T010 [P] Add request DTOs for OTP request, confirm delivery, fail delivery, admin reset, and driver trip completion in `backend/src/main/java/com/wms/dto/request`
- [X] T011 Add `DriverDeliveryService` contract and shared current-attempt, driver-assignment, OTP, and `IN_TRANSIT` inventory helper methods in `backend/src/main/java/com/wms/service/DriverDeliveryService.java` and `backend/src/main/java/com/wms/service/impl/DriverDeliveryServiceImpl.java`
- [X] T012 Add admin-facing delivery reset controller skeleton and driver/mobile controller skeleton in `backend/src/main/java/com/wms/controller/AdminDeliveryController.java` and `backend/src/main/java/com/wms/controller/TripController.java`
- [X] T013 Add or extend audit action and OTP status enum support if missing in `backend/src/main/java/com/wms/enums/AuditAction.java` and `backend/src/main/java/com/wms/enums/DeliveryOtpStatus.java`

**Checkpoint**: Foundation ready for user-story implementation.

---

## Phase 3: User Story 1 - Driver views assigned trip, uploads POD, and requests OTP (Priority: P1) MVP

**Goal**: Driver can see only assigned trip detail, upload both POD images for a Delivery Order in `IN_TRANSIT`, and request a dealer OTP after POD evidence exists.

**Independent Test**: Load `GET /api/v1/trips/{id}` for an assigned trip, upload valid POD evidence to `POST /api/v1/trips/{tripId}/delivery-orders/{doId}/pod-evidence`, then request OTP with `POST /api/v1/trips/{tripId}/delivery-orders/{doId}/delivery-otp` and verify trip assignment scope, file validation, dealer-email validation, and OTP-row creation rules.

### Tests for User Story 1

- [X] T014 [P] [US1] Add controller test for `GET /api/v1/trips/{id}` assigned-trip happy path in `backend/src/test/java/com/wms/controller/DriverDeliveryControllerTest.java`
- [X] T015 [P] [US1] Add controller test for `POST /api/v1/trips/{tripId}/delivery-orders/{doId}/pod-evidence` happy path and invalid-file failure in `backend/src/test/java/com/wms/controller/DriverDeliveryControllerTest.java`
- [X] T016 [P] [US1] Add controller test for `POST /api/v1/trips/{tripId}/delivery-orders/{doId}/delivery-otp` happy path and missing-POD failure in `backend/src/test/java/com/wms/controller/DriverDeliveryControllerTest.java`
- [X] T017 [P] [US1] Add service unit test for rejecting driver access outside the assigned trip in `backend/src/test/java/com/wms/service/DriverDeliveryServiceImplTest.java`
- [X] T018 [P] [US1] Add service unit test for rejecting missing, oversized, or non-image POD files in `backend/src/test/java/com/wms/service/DriverDeliveryServiceImplTest.java`
- [X] T019 [P] [US1] Add service unit test for blocking OTP request before both POD images exist in `backend/src/test/java/com/wms/service/DriverDeliveryServiceImplTest.java`
- [X] T020 [P] [US1] Add service unit test for rejecting OTP request when dealer email is missing in `backend/src/test/java/com/wms/service/DriverDeliveryServiceImplTest.java`

### Implementation for User Story 1

- [X] T021 [P] [US1] Add driver trip and OTP request DTO validation in `backend/src/main/java/com/wms/dto/request/DeliveryOtpRequest.java` and `backend/src/main/java/com/wms/dto/response/TripDriverViewResponse.java`
- [X] T022 [US1] Add `GET /api/v1/trips/{id}`, `POST /api/v1/trips/{tripId}/delivery-orders/{doId}/pod-evidence`, and `POST /api/v1/trips/{tripId}/delivery-orders/{doId}/delivery-otp` endpoints with validation and OpenAPI metadata in `backend/src/main/java/com/wms/controller/TripController.java`
- [X] T023 [US1] Implement assigned-trip detail loading and current-attempt mapping for the driver mobile view in `backend/src/main/java/com/wms/service/impl/DriverDeliveryServiceImpl.java`
- [X] T024 [US1] Implement POD evidence validation, file storage, and current-attempt POD URL persistence in `backend/src/main/java/com/wms/service/impl/DriverDeliveryServiceImpl.java`
- [X] T025 [US1] Implement first-time OTP generation, single-row OTP persistence, and dealer-email delivery in `backend/src/main/java/com/wms/service/impl/DriverDeliveryServiceImpl.java`
- [X] T026 [US1] Write `UPLOAD_POD` and `REQUEST_OTP` audit logs with before/after attempt and OTP state in `backend/src/main/java/com/wms/service/impl/DriverDeliveryServiceImpl.java`

**Checkpoint**: US1 is fully functional and testable as the MVP.

---

## Phase 4: User Story 2 - Driver confirms delivery with OTP and system finalizes the Delivery Order (Priority: P1)

**Goal**: Driver confirms full Delivery Order delivery using a valid OTP, while the system supports resend after expiry, blocks resend while active, locks after 3 incorrect submissions, and atomically finalizes the Delivery Order on success.

**Independent Test**: After POD and OTP exist, confirm delivery with `PUT /api/v1/trips/{tripId}/delivery-orders/{doId}/confirm-delivery` and verify OTP validation, retry/lock behavior, `IN_TRANSIT` inventory decrement, invoice/receivable creation, attempt transition to `DELIVERED`, and Delivery Order transition to `COMPLETED`.

### Tests for User Story 2

- [X] T027 [P] [US2] Add controller test for `PUT /api/v1/trips/{tripId}/delivery-orders/{doId}/confirm-delivery` happy path and invalid-OTP failure in `backend/src/test/java/com/wms/controller/DriverDeliveryControllerTest.java`
- [X] T028 [P] [US2] Add controller test for OTP resend conflict while active in `backend/src/test/java/com/wms/controller/DriverDeliveryControllerTest.java`
- [X] T029 [P] [US2] Add service unit test for updating the same OTP row after expiry resend in `backend/src/test/java/com/wms/service/DriverDeliveryServiceImplTest.java`
- [X] T030 [P] [US2] Add service unit test for wrong OTP incrementing `attempt_count` and locking after 3 submissions in `backend/src/test/java/com/wms/service/DriverDeliveryServiceImplTest.java`
- [X] T031 [P] [US2] Add service unit test for rejecting confirmation when OTP is expired or not requested in `backend/src/test/java/com/wms/service/DriverDeliveryServiceImplTest.java`
- [X] T032 [P] [US2] Add service unit test for successful confirmation updating attempt, consuming OTP, decrementing `IN_TRANSIT` inventory, and moving Delivery Order to `COMPLETED` in `backend/src/test/java/com/wms/service/DriverDeliveryServiceImplTest.java`
- [X] T033 [P] [US2] Add service unit test for rejecting confirmation when invoice already exists or `IN_TRANSIT` stock is missing in `backend/src/test/java/com/wms/service/DriverDeliveryServiceImplTest.java`

### Implementation for User Story 2

- [X] T034 [P] [US2] Add confirm-delivery request DTO validation in `backend/src/main/java/com/wms/dto/request/ConfirmDeliveryRequest.java`
- [X] T035 [US2] Extend `POST /api/v1/trips/{tripId}/delivery-orders/{doId}/delivery-otp` and add `PUT /api/v1/trips/{tripId}/delivery-orders/{doId}/confirm-delivery` OpenAPI responses for resend, expiry, lock, and success in `backend/src/main/java/com/wms/controller/TripController.java`
- [X] T036 [US2] Implement OTP resend-after-expiry and active-OTP conflict handling on the single OTP row in `backend/src/main/java/com/wms/service/impl/DriverDeliveryServiceImpl.java`
- [X] T037 [US2] Implement OTP verification, lock-after-3-failures, and consumed-state persistence in `backend/src/main/java/com/wms/service/impl/DriverDeliveryServiceImpl.java`
- [X] T038 [US2] Implement successful delivery confirmation transaction for attempt update, `IN_TRANSIT` inventory decrement, invoice/receivable creation, and Delivery Order completion in `backend/src/main/java/com/wms/service/impl/DriverDeliveryServiceImpl.java`
- [X] T039 [US2] Write `CONFIRM_DELIVERY` audit logs with before/after attempt, OTP, inventory, and Delivery Order state in `backend/src/main/java/com/wms/service/impl/DriverDeliveryServiceImpl.java`

**Checkpoint**: US2 is independently testable after US1 and fully finalizes one Delivery Order on successful OTP confirmation.

---

## Phase 5: User Story 3 - Driver records failed delivery and completes the trip after vehicle return (Priority: P1)

**Goal**: Driver can mark a Delivery Order as failed or refused without changing inventory, and later complete the trip only when every assigned Delivery Order is `COMPLETED` or `RETURNED`.

**Independent Test**: Submit `PUT /api/v1/trips/{tripId}/delivery-orders/{doId}/fail-delivery` for an `IN_TRANSIT` Delivery Order and verify current attempt becomes `FAILED`, Delivery Order becomes `RETURNED`, and inventory stays unchanged; then submit `PUT /api/v1/trips/{tripId}/complete` after all trip orders are terminal and verify the trip moves to `COMPLETED` and vehicle/driver become `AVAILABLE`.

### Tests for User Story 3

- [X] T040 [P] [US3] Add controller test for `PUT /api/v1/trips/{tripId}/delivery-orders/{doId}/fail-delivery` happy path in `backend/src/test/java/com/wms/controller/DriverDeliveryControllerTest.java`
- [X] T041 [P] [US3] Add controller test for `PUT /api/v1/trips/{tripId}/complete` happy path and readiness failure in `backend/src/test/java/com/wms/controller/DriverDeliveryControllerTest.java`
- [X] T042 [P] [US3] Add service unit test for failed delivery requiring `failureReason` and leaving inventory unchanged in `backend/src/test/java/com/wms/service/DriverDeliveryServiceImplTest.java`
- [X] T043 [P] [US3] Add service unit test for moving the current attempt to `FAILED` and Delivery Order to `RETURNED` in `backend/src/test/java/com/wms/service/DriverDeliveryServiceImplTest.java`
- [X] T044 [P] [US3] Add service unit test for rejecting trip completion before all assigned Delivery Orders are `COMPLETED` or `RETURNED` in `backend/src/test/java/com/wms/service/DriverDeliveryServiceImplTest.java`
- [X] T045 [P] [US3] Add service unit test for completing the trip and releasing vehicle/driver to `AVAILABLE` in `backend/src/test/java/com/wms/service/DriverDeliveryServiceImplTest.java`

### Implementation for User Story 3

- [X] T046 [P] [US3] Add fail-delivery and trip-complete request DTO validation in `backend/src/main/java/com/wms/dto/request/FailDeliveryRequest.java` and `backend/src/main/java/com/wms/dto/request/TripCompleteRequest.java`
- [X] T047 [US3] Add `PUT /api/v1/trips/{tripId}/delivery-orders/{doId}/fail-delivery` and `PUT /api/v1/trips/{tripId}/complete` endpoints with validation and OpenAPI metadata in `backend/src/main/java/com/wms/controller/TripController.java`
- [X] T048 [US3] Implement failed-delivery validation and current-attempt/Delivery Order status updates without inventory mutation in `backend/src/main/java/com/wms/service/impl/DriverDeliveryServiceImpl.java`
- [X] T049 [US3] Implement trip-completion terminal-outcome validation and vehicle/driver availability release in `backend/src/main/java/com/wms/service/impl/DriverDeliveryServiceImpl.java`
- [X] T050 [US3] Write `FAIL_DELIVERY` and `COMPLETE_TRIP` audit logs with before/after attempt, Delivery Order, trip, vehicle, and driver state in `backend/src/main/java/com/wms/service/impl/DriverDeliveryServiceImpl.java`

**Checkpoint**: US3 is independently testable after dispatched Delivery Orders exist and covers the non-success delivery path plus trip closeout.

---

## Future User Story 5 - Warehouse processes returned goods after failed delivery (Priority: P1)

**Goal**: Warehouse staff and Storekeeper complete the separate return flow for a `RETURNED` Delivery Order so goods move out of virtual `IN_TRANSIT` and the Delivery Order closes as `DELIVERY_FAILED`.

**Independent Test**: Starting from a Delivery Order in `RETURNED`, Storekeeper confirms the goods have physically arrived back at the warehouse, staff submit actual returned quantity plus quality-passed and quality-failed quantities with failure reasons, Storekeeper rejects once with a reason and staff resubmits, Storekeeper accepts the corrected result, Storekeeper creates a putaway plan, then staff confirm putaway; verify inventory moves from virtual `IN_TRANSIT` to the planned destination location and the Delivery Order moves to `DELIVERY_FAILED`.

### Tests for User Story 5

- [X] T076 [P] [US5] Add service unit test for Storekeeper returned-goods arrival confirmation opening `COUNT_QC_PENDING` while Delivery Order remains `RETURNED` and inventory remains virtual `IN_TRANSIT`.
- [X] T077 [P] [US5] Add service unit test for blocking staff count/QC before Storekeeper arrival confirmation.
- [X] T078 [P] [US5] Add service unit test for staff count/QC actual/pass/fail quantity validation, including pass + fail = actual and failure reason required when failed quantity is greater than zero.
- [X] T079 [P] [US5] Add service unit test for Storekeeper QC acceptance only after all returned items have valid actual/pass/fail count/QC results.
- [X] T080 [P] [US5] Add service unit test for Storekeeper QC rejection requiring a rejection reason and returning the flow to staff rework without allowing putaway planning.
- [X] T081 [P] [US5] Add service unit test for staff resubmitting count/QC after Storekeeper rejection and Storekeeper accepting the corrected result.
- [X] T082 [P] [US5] Add service unit test for Storekeeper creating a returned-goods putaway plan with valid destination locations after QC acceptance.
- [X] T083 [P] [US5] Add service unit test for staff putaway completion moving goods from virtual `IN_TRANSIT` to the planned destination locations and changing Delivery Order to `DELIVERY_FAILED`.
- [X] T084 [US5] Add controller integration tests for returned-goods arrival confirmation, count/QC submit/resubmit, Storekeeper accept/reject, putaway planning, and putaway completion role/state validation.

### Implementation for User Story 5

- [X] T085 [P] [US5] Add returned-goods receive endpoint and service method for Storekeeper goods-arrival confirmation before staff count/QC.
- [X] T086 [P] [US5] Extend returned-goods DTOs, entities, migrations, and response mapping for `actualQty`, `qualityPassQty`, `qualityFailQty`, `qualityFailureReason`, QC decision, rejection reason, and rework status.
- [X] T087 [US5] Update staff count/QC endpoint to allow initial submission only from `COUNT_QC_PENDING` and resubmission from `QC_REJECTED`.
- [X] T088 [US5] Update returned-goods QC decision logic to support `ACCEPT` and `REJECT`, require rejection reason for rejection, and require accepted QC before putaway planning.
- [X] T089 [US5] Update returned-goods putaway planning/completion to use Storekeeper-accepted pass/fail quantities and destination locations while keeping Delivery Order `RETURNED` until final putaway completion.
- [X] T090 [US5] Add warehouse-scoped `GET /api/v1/delivery-orders/{doId}/returned-goods` contract and frontend resume behavior for all returned-goods flow statuses, including arrival pending, QC rejected, QC accepted, putaway planned, and completed.
- [X] T091 [US5] Update frontend returned-goods panel so Storekeeper first confirms goods arrival, staff enters actual/pass/fail quantities and failure reasons, Storekeeper accepts/rejects with reason, staff can rework rejected QC, Storekeeper plans putaway after acceptance, and staff confirms putaway completion.
- [X] T092 [US5] Add audit logs for returned-goods arrival confirmation, QC submission/resubmission, QC acceptance, QC rejection, putaway planning, and putaway completion.

---

## Phase 6: User Story 4 - Admin resets a locked delivery OTP (Priority: P1)

**Goal**: Admin can reset a locked OTP row for the latest current delivery attempt so the driver can request a new OTP on the same row.

**Independent Test**: After OTP lock is triggered, submit `POST /api/v1/admin/delivery-orders/{doId}/delivery-otp/reset` with `resetReason` and verify the current OTP row moves to `EXPIRED`, `attempt_count` resets to `0`, `consumed_at` clears, and a new driver OTP request can update the same row.

### Tests for User Story 4

- [X] T051 [P] [US4] Add controller test for `POST /api/v1/admin/delivery-orders/{doId}/delivery-otp/reset` happy path and validation failure in `backend/src/test/java/com/wms/controller/AdminDeliveryControllerTest.java`
- [X] T052 [P] [US4] Add service unit test for rejecting OTP reset when the latest current attempt or OTP row is missing in `backend/src/test/java/com/wms/service/DriverDeliveryServiceImplTest.java`
- [X] T053 [P] [US4] Add service unit test for resetting the locked OTP row and allowing a later driver resend on the same row in `backend/src/test/java/com/wms/service/DriverDeliveryServiceImplTest.java`

### Implementation for User Story 4

- [X] T054 [P] [US4] Add admin OTP reset request DTO validation in `backend/src/main/java/com/wms/dto/request/ResetDeliveryOtpRequest.java`
- [X] T055 [US4] Add `POST /api/v1/admin/delivery-orders/{doId}/delivery-otp/reset` endpoint with validation and OpenAPI metadata in `backend/src/main/java/com/wms/controller/AdminDeliveryController.java`
- [X] T056 [US4] Implement latest-current-attempt OTP reset logic, resettable-state validation, and same-row reuse support in `backend/src/main/java/com/wms/service/impl/DriverDeliveryServiceImpl.java`
- [X] T057 [US4] Write `RESET_DELIVERY_OTP` audit logs with before/after OTP state in `backend/src/main/java/com/wms/service/impl/DriverDeliveryServiceImpl.java`

**Checkpoint**: US4 is independently testable after OTP lock behavior from US2 exists.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Align documentation, verify integration points, and run feature-level validation across all stories.

- [X] T058 [P] Update driver/mobile and admin endpoint OpenAPI annotations to match `contracts/driver-pod.openapi.yaml` in `backend/src/main/java/com/wms/controller/TripController.java` and `backend/src/main/java/com/wms/controller/AdminDeliveryController.java`
- [X] T059 [P] Review outbound spec alignment for POD evidence, OTP lifecycle, successful delivery, failure handling, and trip completion in `.sdd/specs/004-outbound-delivery-pod/spec.md`
- [X] T060 Run targeted driver delivery controller and service tests in `backend/pom.xml`
- [X] T061 Run backend compile to verify POD, OTP, inventory, invoice, and controller wiring in `backend/pom.xml`
- [X] T062 Verify quickstart scenarios against the implemented API using `.sdd/specs/004-outbound-delivery-pod/features/feature-driver-mobile-pod/quickstart.md`

---

## Phase 8: Driver Trip List Type Labels and Filters

**Purpose**: Make the driver trip list easy to scan when assigned trips include both outbound dealer delivery (`DELIVERY`) and internal transfer (`TRANSFER`) work.

**Independent Test**: Open `/outbound/driver/trips` as a Driver assigned to one `TRIP-*` dealer delivery and one `TTR-*` internal transfer; verify the title is `Chuyen xe cua toi`, the filters `Tat ca`, `Noi bo`, and `Dai ly` work, each card shows the correct type badge, and transfer cards show source/destination route instead of dealer delivery point wording.

### Tests for Driver Trip List Filters

- [X] T063 [P] Add backend response-mapping test proving `GET /api/v1/trips/driver` includes `tripType`, `tripTypeLabel`, and type-specific summary fields in `backend/src/test/java/com/wms/service/DriverDeliveryServiceImplTest.java`.
- [X] T064 [P] Add controller test for `GET /api/v1/trips/driver` returning assigned mixed `DELIVERY` and `TRANSFER` trip summaries only for the authenticated driver in `backend/src/test/java/com/wms/controller/DriverDeliveryControllerTest.java`.
- [X] T065 [P] Add frontend test for the three filter buttons and empty-filter states in `frontend/src/pages/Outbound/DriverTrip.test.jsx`.
- [X] T066 [P] Add frontend test proving transfer cards render route/source-destination wording and do not render dealer POD/OTP wording in `frontend/src/pages/Outbound/DriverTrip.test.jsx`.

### Implementation for Driver Trip List Filters

- [X] T067 [P] Extend `backend/src/main/java/com/wms/dto/response/TripDriverViewResponse.java` or add `DriverTripSummaryResponse.java` with `tripType`, `tripTypeLabel`, `sourceWarehouseCode`, `destinationWarehouseCode`, `deliveryStopCount`, and `transferLineCount`.
- [X] T068 Update `backend/src/main/java/com/wms/service/impl/DriverDeliveryServiceImpl.java` so `listMyTrips` maps assigned `DELIVERY` and `TRANSFER` trips into the shared driver list summary without exposing trips assigned to other drivers.
- [X] T069 Update `backend/src/main/java/com/wms/controller/TripController.java` OpenAPI annotations for `GET /api/v1/trips/driver` to document the mixed trip-type list response.
- [X] T070 Update `frontend/src/services/outbound.service.js` and `frontend/src/services/inter-warehouse-transfer.service.js` normalization so driver list rows consistently expose `tripType`, `tripTypeLabel`, `vehiclePlate`, `plannedStartAt`, `sourceWarehouseCode`, `destinationWarehouseCode`, `deliveryStopCount`, and `transferLineCount`.
- [X] T071 Update `frontend/src/pages/Outbound/DriverTrip.jsx` list header from delivery-only wording to `Van hanh / Chuyen xe` and `Chuyen xe cua toi`.
- [X] T072 Update `frontend/src/pages/Outbound/DriverTrip.jsx` to add filter controls `Tat ca`, `Noi bo`, and `Dai ly`, default to `Tat ca`, and filter rows client-side without mutating backend state.
- [X] T073 Update `frontend/src/pages/Outbound/DriverTrip.jsx` card rendering so `DELIVERY` cards show `Giao dai ly` and dealer stop count, while `TRANSFER` cards show `Dieu chuyen noi bo`, source-to-destination route, and transfer line count.
- [X] T074 Update backend Swagger annotations in `backend/src/main/java/com/wms/controller/TripController.java` after implementing the mixed assigned-trip list response fields.
- [X] T075 Update `.sdd/specs/005-inter-warehouse-transfer/quickstart.md`, `Userstory.md`, and `README.md` to cross-reference the shared driver trip list behavior for `TTR-*`.

**Checkpoint**: Driver can visually distinguish internal transfer and dealer delivery trips before opening details.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 Setup**: No dependencies.
- **Phase 2 Foundational**: Depends on setup review and blocks all user stories.
- **Phase 3 US1**: Depends on foundational repository, DTO, upload, and mail support; this is the MVP.
- **Phase 4 US2**: Depends on US1 because OTP confirmation builds on current attempt, POD evidence, and OTP-row creation.
- **Phase 5 US3**: Depends on dispatched `IN_TRANSIT` attempts from upstream outbound flow and shares the current-attempt infrastructure from US1.
- **Phase 6 US4**: Depends on US2 because OTP reset only matters after lock behavior exists.
- **Phase 7 Polish**: Depends on whichever user stories are included in the release scope.
- **Phase 8 Driver Trip List Type Labels and Filters**: Depends on existing driver list/detail APIs and Spec 005 transfer trip summary data; it does not depend on POD/OTP mutation changes.

### User Story Dependencies

- **US1**: First MVP story because it creates the core driver trip view, POD upload, and OTP-request flow.
- **US2**: Depends on US1 POD and OTP-row persistence.
- **US3**: Depends on driver assignment and current-attempt resolution from US1, but is otherwise independent of successful delivery confirmation.
- **US4**: Depends on OTP lock semantics from US2.
- **US5**: Depends on US3 because returned-goods processing starts only after a failed/refused delivery has moved the Delivery Order to `RETURNED`.
- **Driver Trip List Filters**: Cross-cuts US1 and Spec 005 visibility, but remains read-only and independently testable from delivery confirmation.

### Parallel Opportunities

- T002, T003, and T004 can run in parallel during setup.
- T006 through T010 can run in parallel where file ownership does not overlap.
- T014 through T020 can be written in parallel for US1 tests.
- T021 and T022 can run in parallel before POD and OTP service integration.
- T027 through T033 can be written in parallel for US2 tests.
- T040 through T045 can be written in parallel for US3 tests.
- T051 through T053 can be written in parallel for US4 tests.
- T058 and T059 can run in parallel during polish.
- T063 through T066 can run in parallel while T067 through T074 are implemented by file ownership.
- T076 through T084 can be written in parallel for US5 tests.
- T085 and T086 can run in parallel before T087 through T092.

## Parallel Example: User Story 1

```text
Task: "T014 [P] [US1] Add controller test for assigned-trip mobile view"
Task: "T015 [P] [US1] Add controller test for POD upload"
Task: "T017 [P] [US1] Add service unit test for driver-scope rejection"
Task: "T018 [P] [US1] Add service unit test for invalid POD files"
Task: "T020 [P] [US1] Add service unit test for dealer-email requirement"
```

## Parallel Example: User Story 2

```text
Task: "T027 [P] [US2] Add controller test for confirm-delivery"
Task: "T028 [P] [US2] Add controller test for active-OTP resend conflict"
Task: "T029 [P] [US2] Add service unit test for resend-after-expiry row update"
Task: "T030 [P] [US2] Add service unit test for OTP lock after 3 failures"
Task: "T032 [P] [US2] Add service unit test for successful delivery confirmation transaction"
```

## Parallel Example: User Story 3

```text
Task: "T040 [P] [US3] Add controller test for fail-delivery"
Task: "T041 [P] [US3] Add controller test for trip completion"
Task: "T042 [P] [US3] Add service unit test for failure-reason validation"
Task: "T044 [P] [US3] Add service unit test for trip-completion readiness guard"
Task: "T045 [P] [US3] Add service unit test for vehicle/driver release on completion"
```

## Implementation Strategy

### MVP First

1. Complete Phase 1 and Phase 2.
2. Complete Phase 3 (US1).
3. Validate assigned trip view, POD upload, and OTP request end-to-end.
4. Stop and demo the driver mobile evidence and OTP-request flow before adding confirmation and failure handling.

### Incremental Delivery

1. Deliver US1 trip view, POD upload, and OTP request.
2. Deliver US2 OTP confirmation, resend/lock behavior, and successful delivery finalization.
3. Deliver US3 failed-delivery flow and trip completion.
4. Deliver US4 admin OTP reset.
5. Finish polish verification and OpenAPI alignment.
6. Deliver the shared driver trip list labels and filters so mixed `TRIP-*`/`TTR-*` assignments are easy to identify.
7. Deliver US5 returned-goods processing so `RETURNED` orders close as `DELIVERY_FAILED` only after Storekeeper goods-arrival confirmation, staff actual/pass/fail count/QC, Storekeeper QC acceptance, Storekeeper putaway planning, and staff putaway completion.

### Validation Checklist

- Every driver action remains trip-assignment scoped and every admin reset remains role-gated.
- Current delivery attempt resolution always targets the latest non-terminal attempt only.
- OTP is never stored in raw form and only one OTP row exists per current attempt.
- OTP resend is blocked while active and reuses the same row after expiry.
- OTP locks after 3 incorrect submissions and requires admin reset before a new code can be generated.
- Successful delivery confirmation remains transactional, version-safe, and only decrements virtual `IN_TRANSIT` stock for the confirmed Delivery Order.
- Failed delivery never changes inventory and keeps goods in virtual `IN_TRANSIT`.
- Trip completion only happens after all assigned Delivery Orders are `COMPLETED` or `RETURNED`.
- Delivery Order `RETURNED` moves to `DELIVERY_FAILED` only after the separate returned-goods flow completes Storekeeper goods-arrival confirmation, staff actual/pass/fail count/QC, Storekeeper QC acceptance, Storekeeper putaway planning, and staff putaway confirmation.
- Storekeeper QC rejection requires a reason and returns the flow to staff rework without moving inventory or allowing putaway planning.
- Returned-goods putaway completion moves inventory from virtual `IN_TRANSIT` to the Storekeeper-approved destination location with non-negative quantity and version checks.
- Service and controller tests cover happy paths and business-error paths for each user story.
- Driver trip list uses neutral transport wording, exposes `Tat ca` / `Noi bo` / `Dai ly` filters, and renders `DELIVERY` versus `TRANSFER` summaries without enabling the wrong action set.
