# Tasks: 005 Inter-Warehouse Transfer Remediation

**Input**: `.sdd/specs/005-inter-warehouse-transfer/spec.md`, `plan.md`, `data-model.md`, `contracts/openapi.yaml`, `quickstart.md`

**Last updated**: 2026-07-13

**Purpose**: Replace the stale, duplicate task list with an executable remediation backlog for the production-correct internal transfer flow:
`TRQ draft -> submit -> CEO approve -> Planner revalidate & convert once -> Source manager reserve FIFO eligible -> Dispatcher capacity/overlap plan -> pick + outbound QC + load/handover -> driver depart -> IN_TRANSIT -> driver arrive/handover -> blind count -> storekeeper count/QC/bin-capacity check -> manager final confirmation`.

**Important**: Do not edit, rename, or delete already-applied Flyway migrations. Schema fixes must use the next additive migration after the latest deployed version.

## Requirement-to-Test Map

| Requirement | Minimum required tests |
|---|---|
| P0-C1 schema/code alignment | PostgreSQL/Flyway migration test + create/reject/quarantine transfer integration test |
| P0-C2 FIFO eligible reservation | service test proving quarantine/inactive/locked locations are excluded |
| P0-C3 outbound QC and bin capacity | service/controller tests for blocked depart before QC and blocked receive when capacity is exceeded |
| P0-C4 optimistic locking/concurrency | stale version tests for transfer, request conversion, final receive, and trip assignment |
| P0-C5 line-level audit | audit assertion tests for header, items, allocations, QC, trip, inventory movement |
| P0-C6 real DB/frontend coverage | Testcontainers/Flyway test and frontend workflow tests |
| Arrival/handover | receive-count blocked before arrival/handover; allowed after handover |
| Wrong SKU detail | validation tests for expected SKU, actual SKU, line, quantity, reason, and optional photo refs |
| Trip capacity/reassignment/resource release | capacity exceed test, reassignment before departure, lock after departure, resource release guard |
| Overdue return-to-source | only overdue IN_TRANSIT transfer can return, reason required, photo refs supported when available |
| Contract alignment | OpenAPI path test/docs review against controller paths |
| Frontend action buttons | role/state visibility tests + successful click + failed API response + post-success refresh for every primary transfer button |
| Transfer request edit/delete | backend service/controller tests for DRAFT update and soft-cancel; frontend button visibility and modal save/cancel behavior |
| Photo-gated actions | UI tests or manual smoke proving outbound QC, load handover, arrival handover, return handover, and driver POD buttons stay disabled until image selection/capture |
| Frontend-to-backend smoke | full-stack happy path from `TRQ` to final receive plus backend assertions for inventory, audit, and DB state |
| Deploy gate | backend unit/controller/integration + real DB migration + frontend tests/build + backend compile must all pass |

## Phase 1: Documentation and Contract Alignment

**Purpose**: Make source-of-truth docs match the intended production flow and current controller naming.

- [x] T001 Update `.sdd/specs/005-inter-warehouse-transfer/spec.md` with canonical flow, P0 invariants, arrival/handover, return leg, wrong-SKU detail, trip capacity, and requirement-to-test expectations.
- [x] T002 Update `.sdd/specs/005-inter-warehouse-transfer/plan.md` to point to actual `InterWarehouseTransfer*` backend/frontend files and the remediation implementation order.
- [x] T003 Update `.sdd/specs/005-inter-warehouse-transfer/contracts/openapi.yaml` to use `/api/v1/inter-warehouse-transfers`, `/approve`, `/final-receive`, `/request-return`, `/approve-return`, `/reject-return`, and transfer-request `/approve|reject|convert`.
- [x] T004 [P] Update `.sdd/specs/005-inter-warehouse-transfer/data-model.md` with version fields, nullable planned item batch, outbound QC fields, arrival/handover timestamps, wrong-SKU report lines, trip capacity totals, and discrepancy incident/hold entities.
- [x] T005 [P] Update `.sdd/specs/005-inter-warehouse-transfer/quickstart.md` with the full happy path, return path, and blocking-path verification checklist.
- [x] T006 [P] Update feature docs under `.sdd/specs/005-inter-warehouse-transfer/features/` so shipment and receiving docs include outbound QC, load handover, driver arrival, bin capacity, wrong-SKU detail, and return leg.

## Phase 2: Foundational Database and Concurrency

**Purpose**: Fix DB/runtime mismatches and stale-write risks before changing business behavior.

- [x] T007 Create additive Flyway migration `backend/src/main/resources/db/migration/V6__inter_warehouse_transfer_hardening.sql` or the next available version if `V6` already exists.
- [x] T008 In the new migration, replace transfer status check constraints to include `REJECTED` and `QUARANTINED` on the actual inter-warehouse transfer table.
- [x] T009 In the new migration, make planned transfer item `batch_id` nullable while preserving batch traceability on `inter_warehouse_transfer_allocations`.
- [x] T010 In the new migration, add version columns for `inter_warehouse_transfers`, `inter_warehouse_transfer_items`, `transfer_requests`, and transfer trip/resource tables as needed.
- [x] T011 In the new migration, add outbound QC photo refs, load handover photo refs, driver arrival, arrival handover, return departure, and return arrival fields to the transfer schema.
- [x] T012 In the new migration, add wrong-SKU report/report-item fields or tables with expected product, actual product, quantity, reason, optional photo refs, status, reporter, and decision metadata.
- [x] T013 In the new migration, add calculated transfer trip weight/volume fields or verify compatible existing trip columns.
- [x] T014 In the new migration, add discrepancy incident/hold data needed for shortage and physical over-receipt tracking.
- [x] T015 Add `@Version` and DTO version exposure to `backend/src/main/java/com/wms/entity/InterWarehouseTransfer.java`.
- [x] T016 Add `@Version` where needed to `backend/src/main/java/com/wms/entity/InterWarehouseTransferItem.java`.
- [x] T017 Add `@Version` to `backend/src/main/java/com/wms/entity/TransferRequest.java`.
- [x] T018 Add stale-write handling and 409 mapping in the shared exception handling layer.
- [x] T019 Add a PostgreSQL/Flyway migration integration test in `backend/src/test/java/com/wms/db/InterWarehouseTransferMigrationIntegrationTest.java`.

## Phase 3: FIFO Reservation and Inventory Integrity

**Purpose**: Ensure source approval reserves only stock that is legally available for transfer.

- [x] T020 Update `backend/src/main/java/com/wms/repository/InventoryRepository.java` reservation queries to exclude quarantine, inactive, locked, and wrong-warehouse locations.
- [x] T021 Update `backend/src/main/java/com/wms/service/transfer/impl/InterWarehouseTransferApprovalService.java` to reserve FIFO-eligible allocation rows only.
- [x] T022 Update `backend/src/main/java/com/wms/service/transfer/impl/InterWarehouseTransferShippingService.java` so departure fails instead of clamping invalid reserved quantities.
- [x] T023 Add service tests in `backend/src/test/java/com/wms/service/InterWarehouseTransferServiceImplTest.java` for FIFO order, quarantine exclusion, inactive location exclusion, and reserved quantity conflicts.
- [x] T024 Add integration test coverage proving approval cannot reserve inventory that cross-warehouse availability would hide.

## Phase 4: Request Approval and One-Time Conversion

**Purpose**: Make `TRQ` reliable under concurrent approval/conversion.

- [x] T025 Update `backend/src/main/java/com/wms/dto/request/TransferRequestCreateRequest.java` and `TransferRequestUpdateRequest.java` so `neededByDate`, `businessReason`, observed quantities, and shortage reasons match the spec.
- [x] T026 Update `backend/src/main/java/com/wms/enums/TransferRequestStatus.java` to use documented statuses or add a compatibility mapping if legacy values are retained.
- [x] T027 Update `backend/src/main/java/com/wms/service/transfer/impl/TransferRequestServiceImpl.java` to revalidate source availability before submit/CEO approve/conversion.
- [x] T028 Add a unique one-active-transfer guard for `transfer_request_id` in the new migration and repository/service conversion path.
- [x] T029 Add stale conversion tests in `backend/src/test/java/com/wms/service/TransferRequestServiceImplTest.java`.
- [x] T030 Add controller tests in `backend/src/test/java/com/wms/controller/TransferRequestControllerTest.java` for approve/reject/convert path names and duplicate conversion.

## Phase 5: Trip Planning, Capacity, and Resource Lifecycle

**Purpose**: Make `TTR` planning match real transport constraints.

- [x] T031 Update `backend/src/main/java/com/wms/dto/request/InterWarehouseTransferTripAssignRequest.java` to carry planned start/end and version fields.
- [x] T032 Update `backend/src/main/java/com/wms/service/transfer/impl/InterWarehouseTransferShippingService.java` to calculate trip weight/volume from transfer item quantities and product/package metadata.
- [x] T033 Update trip assignment to reject `TRIP_CAPACITY_EXCEEDED` when calculated totals exceed selected vehicle capacity.
- [x] T034 Update trip assignment to allow vehicle/driver/schedule reassignment before departure and audit it as `TRANSFER_TRIP_REASSIGN`.
- [x] T035 Update trip assignment to reject reassignment after departure with `TRANSFER_TRIP_LOCKED`.
- [x] T036 Update terminal receive/quarantine code so vehicle/driver are released only when they have no other active assignment.
- [x] T037 Add tests for overlap, capacity exceed, reassignment before departure, lock after departure, and guarded resource release.

## Phase 6: Outbound QC, Load Handover, Departure

**Purpose**: Prevent bad or unverified goods from leaving the source warehouse.

- [x] T038 Add outbound QC request/response DTOs with required photo refs and no Barcode/QR requirement in `backend/src/main/java/com/wms/dto/request/`.
- [x] T039 Add outbound QC fields to `backend/src/main/java/com/wms/dto/response/InterWarehouseTransferResponse.java`.
- [x] T040 Implement photo-confirmed `recordOutboundQc` in `backend/src/main/java/com/wms/service/transfer/impl/InterWarehouseTransferShippingService.java`.
- [x] T041 Implement source load/handover photo confirmation before departure in `InterWarehouseTransferShippingService.java`.
- [x] T042 Update `departTransfer` to require outbound QC passed, shipment recorded, load handover recorded, assigned driver, and valid version.
- [x] T043 Add endpoints in `backend/src/main/java/com/wms/controller/InterWarehouseTransferController.java` for outbound QC and load handover.
- [x] T044 Add audit actions in `backend/src/main/java/com/wms/enums/AuditAction.java` for `TRANSFER_OUTBOUND_QC` and `TRANSFER_LOAD_HANDOVER`.
- [x] T045 Add tests blocking ship/depart when outbound QC is missing, failed, or missing required photo refs.

## Phase 7: Arrival, Handover, Receiving, and Bin Capacity

**Purpose**: Make destination/source receiving start only after physical arrival and prevent overfilled bins.

- [x] T046 Add arrival and arrival-handover endpoints in `backend/src/main/java/com/wms/controller/InterWarehouseTransferController.java`.
- [x] T047 Implement driver arrival and receiving-warehouse handover in `backend/src/main/java/com/wms/service/transfer/impl/InterWarehouseTransferShippingService.java`.
- [x] T048 Update `backend/src/main/java/com/wms/service/transfer/impl/InterWarehouseTransferReceivingService.java` to block receive-count until arrival and handover exist.
- [x] T049 Update receive-check/final-receive to validate destination bin capacity for QC-passed quantity before inventory is posted.
- [x] T050 Add discrepancy incident/hold handling for physical over-receipt so regular inventory posting remains blocked.
- [x] T051 Add audit actions for `TRANSFER_ARRIVE` and `TRANSFER_ARRIVAL_HANDOVER`.
- [x] T052 Add tests for receive before arrival blocked, receive after handover allowed, bin capacity exceeded, and over-receipt discrepancy hold.

## Phase 8: Wrong SKU and Return Leg

**Purpose**: Replace reason-only wrong-SKU handling with line-level return control and physical trip events.

- [x] T053 Replace or extend `backend/src/main/java/com/wms/dto/request/TransferReturnRequest.java` with line-level expected product, actual product, quantity, reason, and optional photo refs.
- [x] T054 Add wrong-SKU response data to `backend/src/main/java/com/wms/dto/response/InterWarehouseTransferResponse.java`.
- [x] T055 Update `backend/src/main/java/com/wms/service/transfer/impl/InterWarehouseTransferReceivingService.java` so wrong-SKU report stores item-level detail while stock remains `IN_TRANSIT`.
- [x] T056 Update approve-return to require destination manager scope and pending wrong-SKU report details.
- [x] T057 Add return departure and source return arrival/handover service methods.
- [x] T058 Block source return receive-count until return departure and return arrival/handover are recorded.
- [x] T059 Update overdue `returnToSource` to require actual overdue trip, non-blank reason, and optional photo refs when available.
- [x] T060 Add endpoints for return departure and return arrival/handover in `InterWarehouseTransferController.java`.
- [x] T061 Add audit actions for `TRANSFER_RETURN_DEPART`, `TRANSFER_RETURN_ARRIVE`, and `TRANSFER_RETURN_HANDOVER`.
- [x] T062 Add tests for wrong-SKU missing line details, manager approval, storekeeper self-approval blocked, return receiving before return arrival blocked, and overdue return reason required.

## Phase 9: Audit and Incident Traceability

**Purpose**: Make audit logs reconstruct the full inventory and transport mutation.

- [x] T063 Update `backend/src/main/java/com/wms/service/transfer/impl/InterWarehouseTransferHelper.java` audit snapshot logic to include header, items, allocations, wrong-SKU report lines, QC quantities, trip/resource state, and inventory movement references.
- [x] T064 Ensure `TRANSFER_DISCREPANCY_CREATE` audit includes shortage quantity, product, warehouse, transfer item, adjustment id, and reason.
- [x] T065 Ensure quarantine rejection audit includes target warehouse, quarantine bin, affected item quantities, and transfer-origin references.
- [x] T066 Add audit tests for approve, outbound QC, depart, arrival/handover, receive-check, final-receive, wrong-SKU return, overdue return, and quarantine reject.

## Phase 10: API Contract and Backend Controller Coverage

**Purpose**: Keep OpenAPI, controllers, and frontend service URLs synchronized.

- [x] T067 Update Swagger/OpenAPI annotations in `backend/src/main/java/com/wms/controller/InterWarehouseTransferController.java` for every transfer endpoint.
- [x] T068 Update Swagger/OpenAPI annotations in `backend/src/main/java/com/wms/controller/TransferRequestController.java` for `/approve`, `/reject`, `/convert`, and `/stock-lookup`.
- [x] T069 Add controller tests for `/api/v1/inter-warehouse-transfers/{id}/approve`, `/final-receive`, `/request-return`, `/approve-return`, and `/reject-return`.
- [x] T070 Add controller tests for new outbound QC, load handover, arrival/handover, return departure, and return arrival endpoints.
- [x] T071 Add a contract smoke test or documented review step proving `.sdd/.../contracts/openapi.yaml` path names match controller paths.

## Phase 11: Frontend Workflow

**Purpose**: Expose the new controls without letting users execute steps out of order.

- [x] T072 Update `frontend/src/services/inter-warehouse-transfer.service.js` with outbound QC, load handover, arrival/handover, return departure, return arrival, and expanded wrong-SKU APIs.
- [x] T073 Update `frontend/src/pages/InterWarehouseTransfer/InterWarehouseTransferActionPanel.jsx` to show outbound QC before ship/depart.
- [x] T074 Update `InterWarehouseTransferActionPanel.jsx` to show load handover before driver departure.
- [x] T075 Update `InterWarehouseTransferActionPanel.jsx` to show driver arrival and arrival handover before receive-count.
- [x] T076 Update `InterWarehouseTransferActionPanel.jsx` to block/hide receiving actions until arrival/handover is complete.
- [x] T077 Update `InterWarehouseTransferActionPanel.jsx` wrong-SKU form to collect line item, expected SKU, actual SKU, affected quantity, reason, and optional photo refs.
- [x] T078 Update `InterWarehouseTransferActionPanel.jsx` to show return departure and return arrival/handover states for approved returns.
- [x] T079 Update `frontend/src/pages/InterWarehouseTransfer/TransferRequestWorkspace.jsx` to show `neededByDate`, business reason, observed source/requesting availability, and one-time conversion state.
- [x] T080 Update `frontend/src/utils/interWarehouseTransferStatus.js` and `InterWarehouseTransferStatusBadge.jsx` for new status/action labels.
- [x] T081 Add frontend tests for service URL paths in `frontend/src/services/inter-warehouse-transfer.service.test.js`.
- [x] T082 Add frontend workflow tests for action visibility/order in `frontend/src/pages/InterWarehouseTransfer/`.

## Phase 12: End-to-End Verification and Quality Gates

**Purpose**: Prove the hardened flow works on realistic boundaries.

- [x] T083 Add Testcontainers PostgreSQL or equivalent real-DB integration tests for Flyway + core transfer flow.
- [x] T084 Add happy-path integration test from `TRQ` through final receive with arrival/handover and bin capacity validation.
- [x] T085 Add manual `TRF` happy-path integration test from planner creation through final receive.
- [x] T086 Add exception-path tests for shortage incident + adjustment, over-receipt hold, QC fail to Quarantine, wrong-SKU return, and overdue return.
- [x] T087 Run targeted backend tests for transfer services/controllers and migration tests.
- [x] T088 Run `mvn compile` for backend.
- [x] T089 Run frontend tests/build for the inter-warehouse transfer module.
- [x] T090 Update `.sdd/specs/005-inter-warehouse-transfer/quickstart.md` with final verified commands and known residual risks.
- [x] T091 Add backend endpoint coverage matrix tests for every `InterWarehouseTransferController` action in `backend/src/test/java/com/wms/controller/InterWarehouseTransferControllerTest.java`.
- [x] T092 Add backend endpoint coverage matrix tests for every `TransferRequestController` action in `backend/src/test/java/com/wms/controller/TransferRequestControllerTest.java`.
- [x] T093 Add service unhappy-path matrix tests for invalid status transitions, missing required photos, missing reasons, invalid warehouse scope, invalid role, missing arrival/handover, and stale versions in `backend/src/test/java/com/wms/service/InterWarehouseTransferServiceImplTest.java`.
- [x] T094 Add frontend action-button coverage tests for every transfer workspace button in `frontend/src/pages/InterWarehouseTransfer/InterWarehouseTransferActionPanel.test.jsx`.
- [x] T095 Add frontend transfer-request button coverage tests for create, submit, approve, reject, convert, validation failure, API failure, and refresh state in `frontend/src/pages/InterWarehouseTransfer/TransferRequestWorkspace.test.jsx`.
- [x] T096 Add frontend-to-backend smoke test for `TRQ -> CEO approve -> convert -> approve -> trip -> outbound QC photo -> ship -> load handover photo -> depart -> arrive -> handover -> receive-count -> receive-check -> final-receive`.
- [x] T097 Add frontend-to-backend smoke test for wrong-SKU return including report line details, destination manager approval, return departure, source arrival/handover, and source final receive.
- [x] T098 Add frontend-to-backend smoke test for unhappy deploy blockers: invalid driver scope, overloaded trip, missing outbound QC photo, receive before arrival, bin capacity exceeded, and stale version conflict.
- [x] T099 Add CI/deploy verification documentation showing required commands for backend tests, DB migration tests, frontend tests, frontend build, backend compile, and full-stack smoke tests.
- [x] T100 Block marking spec 005 deploy-ready until every requirement-to-test row in this file has a passing test reference recorded in `.sdd/specs/005-inter-warehouse-transfer/quickstart.md`.
- [x] T101 Add backend `POST /api/v1/transfer-requests/{id}/cancel` soft-cancel endpoint and service method so UI `Xoa` never physically deletes request history.
- [x] T102 Add backend service/controller tests for `DRAFT -> CANCELLED` transfer request soft-cancel and non-DRAFT cancellation rejection.
- [x] T103 Update `frontend/src/pages/InterWarehouseTransfer/TransferRequestWorkspace.jsx` to show `Sua`/`Xoa` on DRAFT request cards and detail modal, reuse the create form for edit, and refresh after update/cancel.
- [x] T104 Add a shared frontend photo selector/camera component and use it for transfer outbound QC, arrival handover, return handover, and outbound driver POD evidence.
- [x] T105 Gate all photo-required action buttons so they remain disabled until an image has been selected or captured.
- [x] T106 Update spec 005, feature docs, OpenAPI contract, and CLAUDE swimlane notes for DRAFT request edit/soft-delete and photo-gated confirmations.

## Dependencies

- Phase 2 blocks backend behavior phases because schema and optimistic locking must exist first.
- Phase 3 must complete before departure/final receive tests can be trusted.
- Phase 5 and Phase 6 must complete before Phase 7 receiving gate.
- Phase 8 depends on Phase 7 arrival/handover concepts.
- Phase 11 depends on backend contract shape from Phases 6-10.
- Phase 12 is the final acceptance gate.
- No backend or frontend implementation task is complete unless its matching happy-path and unhappy-path tests are added or updated in the same slice.

## Implementation Strategy

1. Ship the migration and DB integration test first.
2. Harden backend invariants in small slices: reservation, concurrency, trip, outbound QC, arrival/receiving, return leg, audit.
3. Update OpenAPI and controller tests as each endpoint becomes real.
4. Update frontend only after backend DTO/API shape is stable.
5. Treat a task as complete only when the corresponding requirement-to-test row has passing evidence.
6. Before deploy, run the whole test gate: backend unit/controller/integration, PostgreSQL/Flyway migration tests, frontend tests, frontend build, backend compile, and full-stack smoke flows.
