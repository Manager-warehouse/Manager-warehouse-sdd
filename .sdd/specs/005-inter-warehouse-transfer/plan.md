# Implementation Plan: 005 Inter-Warehouse Transfer

**Branch**: `feat/son-005` | **Date**: 2026-06-16 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `.sdd/specs/005-inter-warehouse-transfer/spec.md`

## Summary

Implement Sprint 1 inter-warehouse transfer as a dedicated `TRF`/`TTR` workflow, separate from supplier inbound `RN` receipts. Planner manually creates transfer documents from external company instructions or from CEO-approved manager transfer requests. A warehouse manager may view read-only available stock in other warehouses, request stock for their own warehouse, and submit the request to CEO. After CEO approval, the system sends/generates the approved request template for the source Planner, who converts it into an executable `TRF`. Source warehouse manager approves and reserves inventory, Dispatcher at the source warehouse assigns one dedicated transfer trip, source storekeeper ships exact approved quantities, assigned driver moves goods to In-Transit, destination worker records counts, destination storekeeper checks count/QC, and destination manager final-confirms receipt. The implementation must preserve inventory invariants, warehouse-scoped RBAC, immutable audit trail, quarantine handling, and non-negative inventory.

## Technical Context

**Language/Version**: Java 21, JavaScript with React 18

**Primary Dependencies**: Spring Boot 3.4.5, Spring Data JPA/Hibernate, Jakarta Validation, Spring Security JWT, OpenAPI/Swagger, React 18, Tailwind CSS 3.x, axios

**Storage**: PostgreSQL 18 with Flyway migrations; no raw SQL in application code outside migrations

**Testing**: JUnit 5 + Mockito for services, Spring MVC/integration tests for endpoints, Jest for frontend

**Target Platform**: Full-stack web app with REST API under `/api/v1`

**Project Type**: Web application with Spring Boot backend and React frontend

**Performance Goals**: Transfer create/approval/depart/receive mutations should complete within 2s under normal Sprint 1 data volume; In-Transit query should be real-time enough for operations screens.

**Constraints**: No negative inventory; optimistic locking/version checks on inventory updates; every transfer mutation audited; role plus warehouse scope required; cross-warehouse stock visibility is read-only; CEO approval of a manager request does not reserve inventory; no source lot allocation requirement inside spec 005 transfer implementation; no per-unit serial/expiry/grade additions for this feature.

**Scale/Scope**: Three physical warehouses, one In-Transit warehouse, one quarantine location per destination warehouse, multi-item transfers, one dedicated trip per transfer.

## Constitution Check

*GATE: Must pass before implementation.*

| Principle | Status | Design Response |
|-----------|--------|-----------------|
| Layered Architecture | PASS | Backend tasks split Controller -> Service -> Repository -> Entity/DTO/Mapper; frontend split service/pages/components. |
| Inventory Integrity | PASS | Transfer service owns reservation, source decrement, In-Transit increment/decrement, destination/quarantine increment, adjustment creation, audit before/after. |
| Inventory Selection Principle | N/A for spec 005 | Transfer spec explicitly removed source lot allocation. This feature operates on aggregate product/warehouse/location quantities. Existing broader inventory rules remain untouched. |
| QC Gate & Quarantine | PASS | Receive-check requires QC totals and failure reason; final receive routes QC-failed stock to active quarantine location. |
| In-Transit Tracking | PASS | Depart moves source stock to In-Transit; final receive clears In-Transit only after destination confirmation. |
| Auth & RBAC | PASS | Tasks include role and source/destination warehouse scope checks for every mutation; manager cross-warehouse stock visibility is read-only and request creation is limited to the manager's assigned warehouse. |
| Test Coverage | PASS | Tasks include unit and integration tests for all service/business rules and API endpoints. |

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
├── controller/TransferController.java
├── controller/TransferRequestController.java
├── dto/request/transfer/*.java
├── dto/request/transferrequest/*.java
├── dto/response/transfer/*.java
├── dto/response/transferrequest/*.java
├── entity/Transfer.java
├── entity/TransferItem.java
├── entity/TransferRequest.java
├── entity/TransferRequestItem.java
├── enums/TransferStatus.java
├── enums/TransferRequestStatus.java
├── enums/AuditAction.java
├── repository/TransferRepository.java
├── repository/TransferItemRepository.java
├── repository/TransferRequestRepository.java
├── mapper/TransferMapper.java
├── mapper/TransferRequestMapper.java
├── service/TransferService.java
├── service/TransferRequestService.java
└── service/impl/TransferServiceImpl.java
└── service/impl/TransferRequestServiceImpl.java

backend/src/test/java/com/wms/
├── controller/TransferControllerIntegrationTest.java
└── service/TransferServiceImplTest.java

frontend/src/
├── services/transfer.service.js
├── pages/Transfer/
│   ├── TransferWorkspace.jsx
│   ├── TransferActionPanel.jsx
│   └── TransferStatusBadge.jsx
├── utils/transferStatus.js
└── routes/AppRoutes.jsx
```

**Structure Decision**: Keep transfer code in dedicated backend transfer files and a dedicated frontend transfer module. The current frontend uses one shared `TransferWorkspace` with role-aware and state-aware action panels instead of separate planner/ship/receive pages. Internal transfer receiving stays inside this transfer module and is intentionally not merged into the supplier inbound `RN` screens.

## Complexity Tracking

No constitution violations are planned. The feature is large but remains within the existing full-stack architecture and uses existing entities/services where possible.
