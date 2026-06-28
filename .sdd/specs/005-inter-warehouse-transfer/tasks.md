# Tasks: 005 Inter-Warehouse Transfer

**Input**: Design documents from `.sdd/specs/005-inter-warehouse-transfer/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/openapi.yaml, quickstart.md

**Last updated**: 2026-06-24 — Sprint 1 core transfer flow implemented; added manager-initiated transfer request and CEO approval scope.

**Organization**: Tasks are grouped by user story and implementation layer. Backend and frontend work is split into dedicated files.

## Phase 1: Setup and Contract Alignment

**Purpose**: Prepare shared constants, migrations, DTO package structure, frontend module shell, and API documentation before feature work.

- [x] T001 Update active Spec Kit plan marker in AGENTS.md to `.sdd/specs/005-inter-warehouse-transfer/plan.md`
- [x] T002 Create Flyway migration for transfer schema deltas in backend/src/main/resources/db/migration/V7__update_transfer_flow.sql
- [x] T003 [P] Extend TransferStatus with REJECTED in backend/src/main/java/com/wms/enums/TransferStatus.java
- [x] T004 [P] Extend AuditAction with transfer-specific actions in backend/src/main/java/com/wms/enums/AuditAction.java
- [x] T005 [P] Create transfer request DTO package in backend/src/main/java/com/wms/dto/request/transfer/
- [x] T006 [P] Create transfer response DTO package in backend/src/main/java/com/wms/dto/response/transfer/
- [x] T007 [P] Create frontend transfer module folder in frontend/src/pages/Transfers/
- [ ] T008 [P] Create frontend transfer component folder in frontend/src/components/transfer/
- [ ] T009 Copy contract definitions from .sdd/specs/005-inter-warehouse-transfer/contracts/openapi.yaml into backend OpenAPI annotations planning notes in backend/src/main/java/com/wms/controller/TransferController.java

---

## Phase 2: Foundational Backend Services

**Purpose**: Shared entities, repositories, service boundaries, authorization helpers, inventory helpers, and audit helpers required by all user stories.

- [x] T010 Update Transfer entity fields in backend/src/main/java/com/wms/entity/Transfer.java
- [x] T011 Update TransferItem entity fields in backend/src/main/java/com/wms/entity/TransferItem.java
- [x] T012 [P] Create TransferRepository in backend/src/main/java/com/wms/repository/TransferRepository.java
- [x] T013 [P] Create TransferItemRepository in backend/src/main/java/com/wms/repository/TransferItemRepository.java
- [x] T014 [P] Extend WarehouseLocationRepository quarantine lookup in backend/src/main/java/com/wms/repository/WarehouseLocationRepository.java
- [x] T015 [P] Extend InventoryRepository transfer-safe lookup methods in backend/src/main/java/com/wms/repository/InventoryRepository.java
- [x] T016 [P] Create TransferMapper in backend/src/main/java/com/wms/mapper/TransferMapper.java
- [x] T017 Create TransferService interface in backend/src/main/java/com/wms/service/TransferService.java
- [x] T018 Create TransferServiceImpl skeleton in backend/src/main/java/com/wms/service/impl/TransferServiceImpl.java
- [x] T019 Create TransferController skeleton in backend/src/main/java/com/wms/controller/TransferController.java
- [x] T020 Create warehouse-scope helper methods in backend/src/main/java/com/wms/service/impl/TransferServiceImpl.java
- [x] T021 Create transfer audit helper methods in backend/src/main/java/com/wms/service/impl/TransferServiceImpl.java
- [x] T022 Create inventory invariant helper methods in backend/src/main/java/com/wms/service/impl/TransferServiceImpl.java
- [x] T023 [P] Create frontend transfer service shell in frontend/src/services/transfer.service.js
- [x] T024 [P] Create transfer status utility in frontend/src/utils/transferStatus.js

---

## Phase 3: US1 - Planner Transfer Creation and Maintenance (Priority: P2)

**Goal**: Planner creates, loads, edits, and cancels `NEW` transfers from external instructions.

**Independent Test**: Planner can create a multi-item transfer with external instruction code, edit the loaded item list, remove omitted items, reject duplicate active instruction, and cancel only while `NEW`.

### Tests for US1

- [x] T025 [P] [US1] Add service tests for create/update/cancel NEW transfer in backend/src/test/java/com/wms/service/TransferServiceImplTest.java
- [ ] T026 [P] [US1] Add controller integration tests for POST/GET/PUT/cancel transfer in backend/src/test/java/com/wms/controller/TransferControllerIntegrationTest.java
- [ ] T027 [P] [US1] Add frontend service tests for transfer create/update/cancel in frontend/src/services/transfer.service.test.js

### Backend Implementation for US1

- [x] T028 [P] [US1] Create TransferCreateRequest in backend/src/main/java/com/wms/dto/request/transfer/TransferCreateRequest.java
- [x] T029 [P] [US1] Create TransferUpdateRequest in backend/src/main/java/com/wms/dto/request/transfer/TransferUpdateRequest.java
- [x] T030 [P] [US1] Create TransferItemRequest in backend/src/main/java/com/wms/dto/request/transfer/TransferItemRequest.java
- [x] T031 [P] [US1] Create TransferResponse in backend/src/main/java/com/wms/dto/response/transfer/TransferResponse.java
- [x] T032 [P] [US1] Create TransferItemResponse in backend/src/main/java/com/wms/dto/response/transfer/TransferItemResponse.java
- [x] T033 [US1] Implement createTransfer in backend/src/main/java/com/wms/service/impl/TransferServiceImpl.java
- [x] T034 [US1] Implement getTransferById in backend/src/main/java/com/wms/service/impl/TransferServiceImpl.java
- [x] T035 [US1] Implement updateTransfer with full item-list replacement in backend/src/main/java/com/wms/service/impl/TransferServiceImpl.java
- [x] T036 [US1] Implement cancelTransfer for Planner NEW only in backend/src/main/java/com/wms/service/impl/TransferServiceImpl.java
- [x] T037 [US1] Add Planner endpoints in backend/src/main/java/com/wms/controller/TransferController.java
- [x] T038 [US1] Add OpenAPI annotations for Planner endpoints in backend/src/main/java/com/wms/controller/TransferController.java

### Frontend Implementation for US1

- [x] T039 [P] [US1] Implement transfer API methods in frontend/src/services/transfer.service.js
- [ ] T040 [P] [US1] Create TransferList page in frontend/src/pages/Transfers/TransferList.jsx
- [ ] T041 [P] [US1] Create TransferForm page in frontend/src/pages/Transfers/TransferForm.jsx
- [ ] T042 [P] [US1] Create TransferItemsEditor component in frontend/src/components/transfer/TransferItemsEditor.jsx
- [ ] T043 [US1] Create TransferDetail page shell in frontend/src/pages/Transfers/TransferDetail.jsx
- [x] T044 [US1] Register transfer routes in frontend/src/routes/AppRoutes.jsx

---

## Phase 4: US2 - Source Approval, Shipment, Unship, and Departure (Priority: P1) 🎯 MVP

**Goal**: Source manager approves/rejects, Dispatcher assigns dedicated trip, source storekeeper ships exact quantities, unship is required before cancelling loaded goods, and assigned driver departs to In-Transit.

**Independent Test**: A transfer can move from `NEW` to `APPROVED`, reserve inventory, assign one trip, ship exact quantity, block mismatch, block cancel after ship, unship then cancel, or depart to `IN_TRANSIT`.

### Tests for US2

- [x] T045 [P] [US2] Add service tests for approve/reject/cancel approved in backend/src/test/java/com/wms/service/TransferServiceImplTest.java
- [x] T046 [P] [US2] Add service tests for trip assign/ship/unship/depart in backend/src/test/java/com/wms/service/TransferServiceImplTest.java
- [ ] T047 [P] [US2] Add controller integration tests for approve/reject/trip/ship/unship/depart in backend/src/test/java/com/wms/controller/TransferControllerIntegrationTest.java

### Backend Implementation for US2

- [x] T048 [P] [US2] Create RejectTransferRequest in backend/src/main/java/com/wms/dto/request/transfer/RejectTransferRequest.java
- [x] T049 [P] [US2] Create AssignTransferTripRequest in backend/src/main/java/com/wms/dto/request/transfer/AssignTransferTripRequest.java
- [x] T050 [P] [US2] Create ShipTransferRequest in backend/src/main/java/com/wms/dto/request/transfer/ShipTransferRequest.java
- [x] T051 [P] [US2] Create ShipTransferItemRequest in backend/src/main/java/com/wms/dto/request/transfer/ShipTransferItemRequest.java
- [x] T052 [US2] Implement approveTransfer reservation logic in backend/src/main/java/com/wms/service/impl/TransferServiceImpl.java
- [x] T053 [US2] Implement rejectTransfer with reason in backend/src/main/java/com/wms/service/impl/TransferServiceImpl.java
- [x] T054 [US2] Implement assignTransferTrip availability checks in backend/src/main/java/com/wms/service/impl/TransferServiceImpl.java
- [x] T055 [US2] Implement shipTransfer exact quantity validation in backend/src/main/java/com/wms/service/impl/TransferServiceImpl.java
- [x] T056 [US2] Implement unshipTransfer clearing sent quantities in backend/src/main/java/com/wms/service/impl/TransferServiceImpl.java
- [x] T057 [US2] Implement departTransfer inventory movement to In-Transit in backend/src/main/java/com/wms/service/impl/TransferServiceImpl.java
- [x] T058 [US2] Add source shipment endpoints in backend/src/main/java/com/wms/controller/TransferController.java
- [x] T059 [US2] Add audit logging for source shipment actions in backend/src/main/java/com/wms/service/impl/TransferServiceImpl.java

### Frontend Implementation for US2

- [ ] T060 [P] [US2] Create TransferApprovalPanel component in frontend/src/components/transfer/TransferApprovalPanel.jsx
- [ ] T061 [P] [US2] Create TransferTripPanel component in frontend/src/components/transfer/TransferTripPanel.jsx
- [ ] T062 [P] [US2] Create TransferShipWorkspace page in frontend/src/pages/Transfers/TransferShipWorkspace.jsx
- [ ] T063 [P] [US2] Create TransferDepartPanel component in frontend/src/components/transfer/TransferDepartPanel.jsx
- [ ] T064 [US2] Wire source role action visibility in frontend/src/pages/Transfers/TransferDetail.jsx

---

## Phase 5: US3 - Destination Receive Count, QC Check, and Final Confirmation (Priority: P1)

**Goal**: Destination worker records counts, destination storekeeper checks count/QC, and destination manager final-confirms receipt with quarantine and discrepancy handling.

**Independent Test**: An `IN_TRANSIT` transfer can record count, reject over-receipt, require issue reasons, approve receive-check, require checker note on changed count, quarantine QC failed quantity, complete or complete with discrepancy.

### Tests for US3

- [x] T065 [P] [US3] Add service tests for receive-count validations in backend/src/test/java/com/wms/service/TransferServiceImplTest.java
- [x] T066 [P] [US3] Add service tests for receive-check QC/quarantine validations in backend/src/test/java/com/wms/service/TransferServiceImplTest.java
- [x] T067 [P] [US3] Add service tests for final receive inventory and discrepancy adjustment in backend/src/test/java/com/wms/service/TransferServiceImplTest.java
- [x] T085 [P] [US3] Add unit tests for quarantineReject success and validation errors in backend/src/test/java/com/wms/service/TransferServiceImplTest.java
- [ ] T068 [P] [US3] Add controller integration tests for receive-count/receive-check/receive in backend/src/test/java/com/wms/controller/TransferControllerIntegrationTest.java

### Backend Implementation for US3

- [x] T069 [P] [US3] Create ReceiveCountRequest in backend/src/main/java/com/wms/dto/request/transfer/ReceiveCountRequest.java
- [x] T070 [P] [US3] Create ReceiveCountItemRequest in backend/src/main/java/com/wms/dto/request/transfer/ReceiveCountItemRequest.java
- [x] T071 [P] [US3] Create ReceiveCheckRequest in backend/src/main/java/com/wms/dto/request/transfer/ReceiveCheckRequest.java
- [x] T072 [P] [US3] Create ReceiveCheckItemRequest in backend/src/main/java/com/wms/dto/request/transfer/ReceiveCheckItemRequest.java
- [x] T073 [P] [US3] Create ReceiveConfirmRequest in backend/src/main/java/com/wms/dto/request/transfer/ReceiveConfirmRequest.java
- [x] T074 [US3] Implement receiveCount in backend/src/main/java/com/wms/service/impl/TransferServiceImpl.java
- [x] T075 [US3] Implement receiveCheck in backend/src/main/java/com/wms/service/impl/TransferServiceImpl.java
- [x] T076 [US3] Implement confirmReceive with destination/quarantine inventory updates in backend/src/main/java/com/wms/service/impl/TransferServiceImpl.java
- [x] T077 [US3] Implement shortage adjustment creation in backend/src/main/java/com/wms/service/impl/TransferServiceImpl.java
- [x] T078 [US3] Add destination receive endpoints in backend/src/main/java/com/wms/controller/TransferController.java
- [x] T079 [US3] Add audit logging for receive actions in backend/src/main/java/com/wms/service/impl/TransferServiceImpl.java
- [x] T086 [P] [US3] Create TransferRejectRequest DTO in backend/src/main/java/com/wms/dto/request/TransferRejectRequest.java
- [x] T087 [US3] Implement quarantineReject service logic and transit inventory transfer to quarantine location in backend/src/main/java/com/wms/service/impl/TransferServiceImpl.java
- [x] T088 [US3] Create POST /api/v1/transfers/{id}/quarantine-reject endpoint in backend/src/main/java/com/wms/controller/TransferController.java

### Frontend Implementation for US3

- [x] T080 [P] [US3] Implement receive-count, receive-check, final-receive forms in TransferActionPanel.jsx (integrated into single workspace, not separate pages)
- [x] T081 [P] [US3] Add quarantine destination hint UI (dynamic amber badge showing target quarantine bin when qcFailedQty > 0) in frontend/src/pages/Transfer/TransferActionPanel.jsx
- [x] T082 [P] [US3] Filter quarantine bins from QC-passed bin dropdown (client-side) in frontend/src/pages/Transfer/TransferActionPanel.jsx
- [x] T089 [P] [US3] Implement quarantineReject API call in frontend/src/services/transfer.service.js
- [x] T090 [P] [US3] Add "Từ chối & Cách ly toàn bộ" buttons with required reason inputs for Storekeeper and Manager in frontend/src/pages/Transfer/TransferActionPanel.jsx
- [x] T091 [P] [US3] Update TRANSFER_STATUS_LABELS and TransferStatusBadge to support QUARANTINED status
- [ ] T083 [P] [US3] Create standalone ReceiveConfirmPanel component if workspace grows beyond 300 lines in frontend/src/components/transfer/
- [ ] T084 [US3] Wire destination role action visibility in frontend/src/pages/Transfers/TransferDetail.jsx

---

## Phase 6: US4 - Manager Transfer Request and CEO Approval (Priority: P1)

**Goal**: Warehouse manager views other warehouses' available stock read-only, creates a transfer request for their own shortage, submits it to CEO, and after CEO approval the source Planner receives an approved template to create the executable `TRF`.

**Independent Test**: HP warehouse manager can see HCM available stock, create/submit a request, CEO can approve/reject, Planner can convert only an approved request to one `TRF`, and conversion is blocked before approval or after prior conversion.

### Tests for US4

- [ ] T093 [P] [US4] Add service tests for cross-warehouse stock lookup authorization and quarantine-excluded availability in backend/src/test/java/com/wms/service/TransferRequestServiceImplTest.java
- [ ] T094 [P] [US4] Add service tests for create/update/submit manager transfer request in backend/src/test/java/com/wms/service/TransferRequestServiceImplTest.java
- [ ] T095 [P] [US4] Add service tests for CEO approve/reject and required rejection reason in backend/src/test/java/com/wms/service/TransferRequestServiceImplTest.java
- [ ] T096 [P] [US4] Add service tests for Planner convert-to-transfer, one-time conversion, and request-to-TRF field copy in backend/src/test/java/com/wms/service/TransferRequestServiceImplTest.java
- [ ] T097 [P] [US4] Add controller integration tests for transfer-request endpoints in backend/src/test/java/com/wms/controller/TransferRequestControllerIntegrationTest.java
- [ ] T098 [P] [US4] Add frontend tests for transfer request API methods and manager request form in frontend/src/services/transferRequest.service.test.js

### Backend Implementation for US4

- [ ] T099 [P] [US4] Create TransferRequestStatus enum in backend/src/main/java/com/wms/enums/TransferRequestStatus.java
- [ ] T100 [P] [US4] Extend AuditAction with TRANSFER_REQUEST_CREATE/SUBMIT/CEO_APPROVE/CEO_REJECT/CONVERT in backend/src/main/java/com/wms/enums/AuditAction.java
- [ ] T101 [P] [US4] Create TransferRequest and TransferRequestItem entities in backend/src/main/java/com/wms/entity/
- [ ] T102 [P] [US4] Create TransferRequestRepository in backend/src/main/java/com/wms/repository/TransferRequestRepository.java
- [ ] T103 [P] [US4] Create request/response DTOs in backend/src/main/java/com/wms/dto/request/transferrequest/ and backend/src/main/java/com/wms/dto/response/transferrequest/
- [ ] T104 [P] [US4] Create TransferRequestMapper in backend/src/main/java/com/wms/mapper/TransferRequestMapper.java
- [ ] T105 [US4] Create Flyway migration for transfer_requests, transfer_request_items, and transfers.transfer_request_id in backend/src/main/resources/db/migration/
- [ ] T106 [US4] Implement cross-warehouse available stock lookup excluding quarantine inventory in the appropriate stock service/repository layer
- [ ] T107 [US4] Implement TransferRequestService create/update/submit with warehouse-scope authorization in backend/src/main/java/com/wms/service/impl/TransferRequestServiceImpl.java
- [ ] T108 [US4] Implement CEO approve/reject and approved template/notification assignment to source Planner in backend/src/main/java/com/wms/service/impl/TransferRequestServiceImpl.java
- [ ] T109 [US4] Implement Planner convert-to-transfer by delegating to existing TransferService create flow and linking request to transfer
- [ ] T110 [US4] Add TransferRequestController endpoints in backend/src/main/java/com/wms/controller/TransferRequestController.java
- [ ] T111 [US4] Add OpenAPI/Swagger annotations for transfer-request endpoints

### Frontend Implementation for US4

- [ ] T112 [P] [US4] Create frontend transfer request service in frontend/src/services/transferRequest.service.js
- [ ] T113 [P] [US4] Add cross-warehouse stock search UI for Warehouse Manager in frontend/src/pages/Transfer/TransferWorkspace.jsx or a dedicated transfer request page
- [ ] T114 [P] [US4] Create TransferRequestForm component for manager request creation and submission
- [ ] T115 [P] [US4] Create CEO approval panel/list for submitted transfer requests
- [ ] T116 [P] [US4] Add Planner approved-request inbox/template view and convert-to-TRF action
- [ ] T117 [US4] Wire role-scoped action visibility for Warehouse Manager, CEO, and Planner

---

## Phase 6A: Transfer Quarantine Handoff to Spec 009 (Priority: P1)

**Goal**: Classify transfer exceptions by physical condition and hand physically damaged transfer stock to the spec 009 disposal flow without enabling supplier RTV.

**Independent Test**: Damaged physical stock moves to Quarantine and can be disposed with transfer traceability; shortage creates only a discrepancy adjustment; intact wrong SKU can return to source; transfer-origin quarantine stock cannot create RTV or Debit Note.

### Tests for Cross-Spec Handoff

- [ ] T118 [P] Add service test proving transfer shortage does not create quarantine inventory or a disposal candidate.
- [ ] T119 [P] Add service test proving physically damaged transfer stock retains transfer-item origin when moved to Quarantine.
- [ ] T120 [P] Add service test blocking RTV and supplier Debit Note for `INTERNAL_TRANSFER` quarantine origin.
- [ ] T121 [P] Add integration test covering transfer quarantine → spec 009 disposal approval → exact inventory deduction and audit.
- [ ] T122 [P] Add end-to-end service test: destination Storekeeper reports wrong SKU, destination Manager approves, assigned driver turns back, and source Staff/Storekeeper/Manager complete count/check/final-receive.
- [ ] T128 [P] Add valuation test proving 30 sent/28 received imports and values only 28 units, keeps 2 missing units as quantity-only discrepancy, and creates no commercial billing records.
- [ ] T129 [P] Add authorization tests blocking Storekeeper self-approval and actors outside destination warehouse scope from approving wrong-SKU return.
- [ ] T130 [P] Add source receiving tests for returned goods: passed to regular inventory, failed to source Quarantine, shortage to `TRANSFER_DISCREPANCY`.

### Implementation and Contract Mapping

- [ ] T123 Generalize `quarantine_records` with origin type/id and remaining quantity; support `INTERNAL_TRANSFER` references while retaining transfer/trip/vehicle/driver traceability.
- [ ] T124 Add transfer-item disposal endpoint/contract in spec 009 and expose the action only for physically present quarantine quantity.
- [ ] T125 Block RTV and Debit Note creation for internal-transfer and dealer-return quarantine origins.
- [ ] T126 Update Quarantine Workspace to display origin and route actions: supplier inbound = RTV/disposal, internal transfer = disposal only, intact wrong SKU = Return to Source in transfer workspace.
- [ ] T127 Add audit coverage for `TRANSFER_DISPOSAL_HANDOFF`, disposal approval, inventory before/after, and warehouse scope.
- [ ] T131 Add a new Flyway migration for return reason/request/approval fields; do not modify applied migrations.
- [ ] T132 Add wrong-SKU return request and manager decision DTOs with Jakarta Validation.
- [ ] T133 Implement destination Storekeeper wrong-SKU report and destination Warehouse Manager approve/reject transitions while transfer remains `IN_TRANSIT`.
- [ ] T134 Keep the same trip/vehicle/driver active for the approved return leg and instruct only the assigned driver to turn back.
- [ ] T135 Reuse receive-count, receive-check/QC, and final-receive with source warehouse scope when `is_returned = true`.
- [ ] T136 Ensure shortage finalization calculates destination inventory quantity/value from received quantity only and keeps missing quantity as quantity-only discrepancy without invoice, receivable, payable, Debit Note, or automatic employee liability.
- [ ] T137 Add `TRANSFER_RETURN_REQUEST`, `TRANSFER_RETURN_APPROVE`, and `TRANSFER_RETURN_REJECT` audit actions with before/after state.
- [ ] T138 Add controller endpoints and OpenAPI/Swagger contract for wrong-SKU report and destination Manager decision.
- [ ] T139 Add Storekeeper “Báo gửi nhầm SKU” form and destination Manager approval panel/button in the transfer workspace.
- [ ] T140 Add source warehouse return-receiving guidance and terminal label “Đã hoàn về kho nguồn” for completed transfers with `is_returned = true`.

---

## Phase 7: Polish, Quality Gates, and Documentation

**Purpose**: Cross-cutting checks required before coding is considered complete.

- [x] T085 [P] Update Swagger/OpenAPI documentation annotations in backend/src/main/java/com/wms/controller/TransferController.java
- [x] T086 [P] Add frontend transfer status labels/actions in frontend/src/utils/transferStatus.js
- [x] T087 Run backend unit and integration tests with coverage for transfer service
- [ ] T088 Run frontend tests/build for transfer module
- [x] T089 Run `mvn compile` and frontend lint/build checks
- [ ] T090 Verify audit log rows for every transfer mutation using backend/src/test/java/com/wms/controller/TransferControllerIntegrationTest.java
- [ ] T091 Verify no transfer implementation file exceeds project size/function conventions where practical
- [ ] T092 Update quickstart verification notes in .sdd/specs/005-inter-warehouse-transfer/quickstart.md

---

## Dependencies and Execution Order

### Phase Dependencies

- Phase 1 must complete first.
- Phase 2 blocks all user story implementation.
- US4 manager request can be implemented after Phase 2 and can run before or alongside US1 UI polish because it converts into the existing Planner create flow.
- US2 and US3 are P1 operational flow and should be implemented before US1 UI polish if delivery pressure exists.
- US3 depends on US2 depart behavior for an `IN_TRANSIT` transfer.
- US4 convert-to-transfer depends on US1 backend transfer creation behavior.
- Phase 7 depends on selected user stories being implemented.

### User Story Dependencies

- **US1 Planner Creation**: Can be tested independently through `NEW` transfer lifecycle.
- **US4 Manager Request + CEO Approval**: Can be tested independently until CEO approval; conversion requires US1 create behavior.
- **US2 Source Shipment**: Requires US1 create behavior and foundational inventory helpers.
- **US3 Destination Receive**: Requires US2 depart behavior to create In-Transit state.

### Parallel Opportunities

- DTO creation tasks can run in parallel with frontend component skeletons.
- Controller integration tests and service tests can be written in parallel.
- US4 transfer-request backend can be developed in parallel with remaining transfer receive UI because it does not change `IN_TRANSIT` inventory movement.
- Frontend pages/components for US2 and US3 can be built in parallel after `transfer.service.js` is stable.

## Implementation Strategy

### MVP First

1. Complete Phase 1 and Phase 2.
2. Implement US1 backend create/detail/update/cancel enough to create `NEW` transfers.
3. Implement US2 backend source flow to reach `IN_TRANSIT`.
4. Implement US3 backend receive flow to complete the transfer.
5. Implement US4 manager request/CEO approval if manager-initiated replenishment is in the Sprint 1 release cut.
6. Add frontend screens incrementally after API contracts are stable.

### Split Work Safely

- Backend engineer: migrations/entities/repositories/services/controllers/tests.
- Frontend engineer: `transfer.service.js`, pages, components, route wiring, frontend tests.
- Keep business rules in backend service layer; frontend only mirrors validation for user ergonomics.
