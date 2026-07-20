# Implementation Plan: 005 Inter-Warehouse Transfer

**Branch**: `feat/son-005` | **Date**: 2026-07-12 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `.sdd/specs/005-inter-warehouse-transfer/spec.md`

## Summary

Implement and harden Sprint 1 inter-warehouse transfer as a dedicated `TRF`/`TTR` workflow, separate from supplier inbound `RN` receipts. The target flow is `TRQ draft -> submit -> CEO approve -> Planner revalidate & convert once -> Source manager reserve FIFO eligible -> Dispatcher capacity/overlap plan -> pick + photo-confirmed outbound QC + photo-confirmed load/handover -> driver depart -> IN_TRANSIT -> driver arrive/handover -> blind count -> storekeeper count/QC/bin-capacity check -> manager final confirmation`. Planner may also create a manual `TRF` from an external instruction. Physically damaged transfer stock is handed off to spec 009 disposal with transfer-item traceability. Shortages become incident/discrepancy records plus quantity-only adjustments; over-receipts are blocked from regular inventory and held as discrepancy incidents; destination quantity and value are calculated only from physically received and accepted goods. Intact wrong SKU requires line-item expected/actual SKU details, affected quantity, optional photo references, destination manager decision, driver return departure/arrival, and source-side receiving. The implementation must preserve inventory invariants, warehouse-scoped RBAC, immutable line-level audit trail, quarantine handling, non-negative inventory, concurrency protection, PostgreSQL/Flyway compatibility, and the no-Barcode/QR Sprint 1 scope.

Driver-facing `TTR-*` visibility is implemented through the shared driver trip screen from Spec 004. Transfer summaries must carry `tripType = TRANSFER`, `tripTypeLabel = Dieu chuyen noi bo`, source/destination warehouse route, line count, vehicle, schedule, status, and weight so the `Noi bo` filter can isolate internal transfer work without exposing dealer POD/OTP actions.

## Technical Context

**Language/Version**: Java 21, JavaScript with React 18

**Primary Dependencies**: Spring Boot 3.4.5, Spring Data JPA/Hibernate, Jakarta Validation, Spring Security JWT, OpenAPI/Swagger, React 18, Tailwind CSS 3.x, axios

**Storage**: PostgreSQL 18 with Flyway migrations; no raw SQL in application code outside migrations

**Testing**: JUnit 5 + Mockito for services, Spring MVC/integration tests for endpoints, Jest for frontend

**Target Platform**: Full-stack web app with REST API under `/api/v1`

**Project Type**: Web application with Spring Boot backend and React frontend

**Performance Goals**: Transfer create/approval/depart/receive mutations should complete within 2s under normal Sprint 1 data volume; In-Transit query should be real-time enough for operations screens.

**Constraints**: No negative inventory; optimistic locking/version checks on inventory, transfer, transfer request, and trip/resource updates; every transfer mutation audited with line-level before/after state; role plus warehouse scope required; cross-warehouse stock visibility is read-only; CEO approval of a manager request does not reserve inventory; source reservation must allocate only FIFO-eligible stock from active non-quarantine locations; no per-unit serial/expiry/grade additions for this feature; internal-transfer quarantine goods cannot use supplier RTV; missing quantity cannot become quarantine inventory; internal transfer creates no invoice or receivable; wrong-SKU return requires destination Storekeeper item-level report and destination Manager approval; applied Flyway migrations must be immutable and schema fixes must use the next additive migration.

**Shared Driver List Constraint**: Transfer trip list visibility is read-only and must not mutate transfer status, trip status, inventory, resource assignment, or audit logs. The shared list can be filtered locally by `tripType`; opening a `TRANSFER` trip must continue to use Spec 005 transfer actions rather than Spec 004 dealer-delivery actions.

**Scale/Scope**: Three physical warehouses, one In-Transit warehouse, one quarantine location per destination warehouse, multi-item transfers, one dedicated trip per transfer.

## Constitution Check

*GATE: Must pass before implementation.*

| Principle | Status | Design Response |
|-----------|--------|-----------------|
| Layered Architecture | PASS | Backend tasks split Controller -> Service -> Repository -> Entity/DTO/Mapper; frontend split service/pages/components. |
| Inventory Integrity | PASS | Transfer service owns FIFO-eligible reservation, source decrement, In-Transit increment/decrement, destination/quarantine increment, discrepancy incident/adjustment creation, and line-level audit before/after. |
| Inventory Selection Principle | PASS | Reservation uses FIFO allocations and excludes quarantine, inactive, locked, or unavailable source locations. Planned transfer item batch may be nullable because allocation rows carry batch fidelity after approval. |
| QC Gate & Quarantine | PASS | Outbound QC is required before load/departure. Receive-check requires QC totals, failure reason, quarantine validation, and destination bin capacity. Final receive routes physical QC-failed stock to active quarantine with internal-transfer origin; spec 009 owns disposal, and RTV is blocked. |
| In-Transit Tracking | PASS | Depart moves source stock to In-Transit; driver arrival/handover is required before receiving; final receive clears In-Transit only after destination/source-return confirmation. |
| Auth & RBAC | PASS | Tasks include role and source/destination warehouse scope checks for every mutation; manager cross-warehouse stock visibility is read-only and request creation is limited to the manager's assigned warehouse. |
| Test Coverage | PASS | Tasks include unit, controller, PostgreSQL/Flyway integration, and frontend workflow tests mapped to every P0 requirement and exception branch. |

## Project Structure

### Documentation

```text
.sdd/specs/005-inter-warehouse-transfer/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── openapi.yaml
├── tasks.md
└── features/
    ├── feature-planner-transfer-planning.md
    ├── feature-warehouse-manager-transfer-request.md
    ├── feature-storekeeper-transfer-ship.md
    └── feature-storekeeper-transfer-receive.md
```

### Source Code

```text
backend/src/main/java/com/wms/
├── controller/InterWarehouseTransferController.java
├── controller/TransferRequestController.java
├── dto/request/InterWarehouseTransfer*.java
├── dto/request/TransferRequest*.java
├── dto/response/InterWarehouseTransfer*.java
├── dto/response/TransferRequest*.java
├── entity/InterWarehouseTransfer.java
├── entity/InterWarehouseTransferItem.java
├── entity/InterWarehouseTransferAllocation.java
├── entity/TransferItem.java
├── entity/TransferRequest.java
├── entity/TransferRequestItem.java
├── enums/InterWarehouseTransferStatus.java
├── enums/TransferRequestStatus.java
├── enums/AuditAction.java
├── repository/InterWarehouseTransferRepository.java
├── repository/InterWarehouseTransferItemRepository.java
├── repository/InterWarehouseTransferAllocationRepository.java
├── repository/TransferRequestRepository.java
├── mapper/InterWarehouseTransferMapper.java
├── service/transfer/InterWarehouseTransferService.java
├── service/TransferRequestService.java
├── service/transfer/impl/InterWarehouseTransferServiceImpl.java
├── service/transfer/impl/InterWarehouseTransferPlanningService.java
├── service/transfer/impl/InterWarehouseTransferApprovalService.java
├── service/transfer/impl/InterWarehouseTransferShippingService.java
├── service/transfer/impl/InterWarehouseTransferReceivingService.java
├── service/transfer/impl/InterWarehouseTransferHelper.java
└── service/transfer/impl/TransferRequestServiceImpl.java

backend/src/test/java/com/wms/
├── controller/InterWarehouseTransferControllerTest.java
├── service/InterWarehouseTransferServiceImplTest.java
├── service/InterWarehouseTransferFlowE2ETest.java
└── db/InterWarehouseTransferMigrationIntegrationTest.java

frontend/src/
├── services/inter-warehouse-transfer.service.js
├── pages/InterWarehouseTransfer/
│   ├── InterWarehouseTransferWorkspace.jsx
│   ├── InterWarehouseTransferActionPanel.jsx
│   ├── TransferRequestWorkspace.jsx
│   └── InterWarehouseTransferStatusBadge.jsx
├── utils/interWarehouseTransferStatus.js
└── routes/AppRoutes.jsx
```

**Structure Decision**: Keep transfer code in the existing `InterWarehouseTransfer*` backend services/controllers/entities and the existing `frontend/src/pages/InterWarehouseTransfer` module. The current frontend uses one shared workspace with role-aware and state-aware action panels instead of separate planner/ship/receive pages. Internal transfer receiving stays inside this transfer module and is intentionally not merged into the supplier inbound `RN` screens.

## Remediation Plan Addendum

The next implementation pass SHALL prioritize these blockers before further feature polish:

1. Add an additive Flyway migration after the latest deployed migration to align transfer status constraints, make planned item `batch_id` nullable, add version columns, wrong-SKU report detail, arrival/handover timestamps, outbound QC fields, trip calculated weight/volume, and discrepancy incident/hold data. Do not modify `V1`-`V5`.
2. Harden reservation so approval locks and reserves only FIFO-eligible inventory from active, non-quarantine locations.
3. Add optimistic locking and stale-write handling on transfer, transfer request, trip/resource, and inventory mutations.
4. Add outbound QC, load/handover, driver arrival/handover, return departure/arrival, and receive gating.
5. Add trip capacity calculation and allow driver/vehicle/trip reassignment only before departure.
6. Add destination bin-capacity validation before posting QC-passed stock.
7. Expand wrong-SKU report storage and workflow to item-level expected/actual SKU, quantity, reason, and optional photo references.
8. Restrict overdue return-to-source to genuinely overdue trips and require reason; support photo references when available.
9. Update OpenAPI to use `/api/v1/inter-warehouse-transfers`, `/approve`, and `/final-receive` exactly as implemented.
10. Replace stale task tracking with a clean requirement-to-test mapped task list.
11. Treat testing as a deploy gate: every primary frontend button and backend endpoint must have happy, unhappy, authorization/scope, invalid-state, and stale/concurrency coverage where applicable, plus at least one frontend-to-backend smoke path.

## Complexity Tracking

No constitution violations are planned. The feature is large and should be implemented as small, testable remediation slices because it touches inventory, trip resources, migration safety, audit, and role-scoped frontend workflows. A slice is not deploy-ready until its backend behavior, frontend action, audit side effect, and database behavior are tested together where applicable.
