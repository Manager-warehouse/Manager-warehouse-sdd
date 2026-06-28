# Implementation Plan: Nhân viên kho Nhập kết quả lấy hàng & QC Outbound

**Branch**: `ha-004` | **Date**: 2026-06-19 | **Spec**: [feature-warehouse-staff-picking-qc.md](feature-warehouse-staff-picking-qc.md)

**Input**: Feature specification from `.sdd/specs/004-outbound-delivery-pod/features/feature-warehouse-staff-picking-qc/feature-warehouse-staff-picking-qc.md`

## Summary

Warehouse staff records one-shot picking and QC results against the concrete allocations prepared by Storekeeper while the Delivery Order stays in `WAITING_PICKING`. The feature must move QC-passed goods into outbound staging, move QC-failed goods into quarantine with adjustment and quarantine records, prevent duplicate submission through allocation-level QC records and idempotency, and support downstream quality approval plus warehouse approval/reject flows without violating optimistic locking, warehouse RBAC, or inventory integrity.

## Technical Context

**Language/Version**: Java 21 for backend, JavaScript/React 18 for frontend.

**Primary Dependencies**: Spring Boot 3.4.5, Spring Data JPA/Hibernate, Jakarta Validation, Spring Security JWT/RBAC, OpenAPI/Swagger.

**Storage**: PostgreSQL 18 with Flyway migrations for outbound QC records, idempotency columns, quarantine references, and approval/reject support tables as needed.

**Testing**: JUnit 5 + Mockito for picking/QC, staging/quarantine movement, idempotency, approval, and reject rules; Spring controller integration tests for outbound write endpoints; Jest only if a dedicated warehouse-staff UI is implemented in scope.

**Target Platform**: WMS web application and REST API.

**Project Type**: Full-stack web application with REST backend; this feature is primarily backend workflow, inventory mutation, and API work.

**Performance Goals**: Pick/QC submission should finish in a single transaction, validate the full allocation set without N+1 queries, and apply staging/quarantine inventory movement with deterministic allocation-level writes.

**Constraints**: No `PICKING` status; Delivery Order remains `WAITING_PICKING` until one complete pick/QC submission succeeds. Every affected inventory row must remain non-negative, pass optimistic locking, and create audit plus supporting business records. Duplicate allocation-level QC submission must be blocked unless the request is a safe idempotent retry with the same payload.

**Scale/Scope**: Sprint 1 outbound flow for three warehouses, multi-allocation Delivery Orders, outbound staging and quarantine movement, replacement re-entry into `WAITING_PICKING`, and warehouse approval/reject integration after QC.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| Layered Architecture | PASS | Controller handles payload and role gating, service owns workflow and inventory mutations, repositories remain persistence-only. |
| Inventory Integrity | PASS | Source, staging, and quarantine rows all mutate in one transaction with optimistic locking and non-negative checks. |
| FIFO Batch Selection | PASS | This feature consumes pre-planned allocations and replacement allocations created from FIFO-valid stock, without introducing FEFO. |
| QC Gate & Quarantine | PASS | QC-failed goods move into quarantine and are excluded from regular available inventory by rule. |
| In-Transit Tracking | PASS | QC-passed goods remain in outbound staging and do not leave stock until later trip departure flow. |
| Auth & RBAC | PASS | Warehouse staff, Storekeeper, and Warehouse Manager mutations all require role plus warehouse assignment. |
| Test Coverage | PASS | Plan includes service and controller coverage for full submission, duplicate blocking, idempotency, approvals, reject returns, and conflict handling. |

## Project Structure

### Documentation (this feature)

```text
.sdd/specs/004-outbound-delivery-pod/features/feature-warehouse-staff-picking-qc/
|-- feature-warehouse-staff-picking-qc.md
|-- plan.md
|-- research.md
|-- data-model.md
|-- quickstart.md
`-- contracts/
    `-- delivery-order-pick-qc.openapi.yaml
```

### Source Code (repository root)

```text
backend/
`-- src/
    |-- main/java/com/wms/
    |   |-- controller/DeliveryOrderController.java
    |   |-- dto/request/DeliveryOrderPickQcResultRequest.java
    |   |-- dto/request/DeliveryOrderPickQcRowRequest.java
    |   |-- dto/request/DeliveryOrderQualityApprovalRequest.java
    |   |-- dto/request/DeliveryOrderWarehouseApprovalRequest.java
    |   |-- dto/request/DeliveryOrderWarehouseRejectRequest.java
    |   |-- dto/request/DeliveryOrderWarehouseRejectReturnRequest.java
    |   |-- dto/response/DeliveryOrderResponse.java
    |   |-- entity/DeliveryOrder.java
    |   |-- entity/DeliveryOrderItem.java
    |   |-- entity/DeliveryOrderItemAllocation.java
    |   |-- entity/OutboundQcRecord.java
    |   |-- entity/Inventory.java
    |   |-- entity/InventoryAdjustment.java
    |   |-- entity/QuarantineRecord.java
    |   |-- repository/DeliveryOrderRepository.java
    |   |-- repository/DeliveryOrderItemRepository.java
    |   |-- repository/DeliveryOrderItemAllocationRepository.java
    |   |-- repository/OutboundQcRecordRepository.java
    |   |-- repository/InventoryRepository.java
    |   |-- repository/QuarantineRecordRepository.java
    |   |-- repository/InventoryAdjustmentRepository.java
    |   |-- service/DeliveryOrderService.java
    |   `-- service/impl/DeliveryOrderServiceImpl.java
    `-- test/java/com/wms/
        |-- controller/DeliveryOrderControllerTest.java
        `-- service/DeliveryOrderServiceImplTest.java
```

**Structure Decision**: Extend the existing Delivery Order backend module so picking plan, warehouse-staff QC, quality approval, and warehouse approval/reject continue to share one Delivery Order aggregate and one transaction boundary for outbound lifecycle changes.

## Phase 0: Research Summary

See [research.md](research.md).

## Phase 1: Design Summary

See [data-model.md](data-model.md), [quickstart.md](quickstart.md), and [contracts/delivery-order-pick-qc.openapi.yaml](contracts/delivery-order-pick-qc.openapi.yaml).

## Post-Design Constitution Check

| Principle | Status | Notes |
|-----------|--------|-------|
| Layered Architecture | PASS | Contracts and data model map cleanly to Controller -> Service -> Repository -> Entity. |
| Inventory Integrity | PASS | Design keeps source, staging, quarantine, and reject-return movement versioned and non-negative. |
| FIFO Batch Selection | PASS | QC records consume the concrete allocation chosen earlier and preserve replacement re-entry without changing FIFO policy. |
| QC Gate & Quarantine | PASS | Failed quantity moves to quarantine with explicit supporting records and stays out of available stock. |
| In-Transit Tracking | PASS | Passed quantity stays reserved in outbound staging until later warehouse approval and trip flow. |
| Auth & RBAC | PASS | All actor-specific endpoints retain warehouse assignment checks in addition to role checks. |
| Test Coverage | PASS | Quickstart captures service/controller coverage for duplicate blocking, idempotency, approvals, reject return flow, and conflict cases. |

## Complexity Tracking

No constitution violations.
