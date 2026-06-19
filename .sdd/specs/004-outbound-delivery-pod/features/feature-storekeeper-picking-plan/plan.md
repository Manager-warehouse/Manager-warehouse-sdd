# Implementation Plan: Thủ kho Lập Kế Hoạch Lấy Hàng

**Branch**: `ha-004` | **Date**: 2026-06-19 | **Spec**: [feature-storekeeper-picking-plan.md](feature-storekeeper-picking-plan.md)

**Input**: Feature specification from `.sdd/specs/004-outbound-delivery-pod/features/feature-storekeeper-picking-plan/feature-storekeeper-picking-plan.md`

## Summary

Storekeeper converts planner-level Delivery Order reservations into concrete FIFO-ranked batch/bin/zone allocations for each item, saving a complete picking plan before the order can move from `NEW` to `WAITING_PICKING`. The same feature also supports plan revision while `WAITING_PICKING`, mandatory return-to-bin handling for changed picked allocations, and replacement planning after outbound QC fail while preserving optimistic locking, warehouse scope, and audit trail requirements.

## Technical Context

**Language/Version**: Java 21 for backend, JavaScript/React 18 for frontend.

**Primary Dependencies**: Spring Boot 3.4.5, Spring Data JPA/Hibernate, Jakarta Validation, Spring Security JWT/RBAC, OpenAPI/Swagger.

**Storage**: PostgreSQL 18 with Flyway migrations for outbound allocation, return, and replacement tables.

**Testing**: JUnit 5 + Mockito for allocation/revision/replacement service rules; Spring controller integration tests for REST endpoints; Jest only if frontend picking UI is implemented in the same scope.

**Target Platform**: WMS web application and REST API.

**Project Type**: Full-stack web application with REST backend; this feature is primarily backend domain and API work.

**Performance Goals**: Picking-plan save should complete in a single transaction, fetch FIFO candidates with indexed warehouse/product/location lookups, and avoid N+1 reads when validating multi-allocation payloads.

**Constraints**: FIFO only for current household-goods domain; exclude quarantine, outbound staging, In-Transit, inactive, and non-available stock; no negative inventory or over-reservation; optimistic locking required on `warehouse_product_reservations` and `inventories`; audit log required for picking plan save, return-to-bin, and replacement save; RBAC must check both role and assigned warehouse.

**Scale/Scope**: Sprint 1 outbound flow for three warehouses, multi-bin allocation per Delivery Order item, and coordination with downstream warehouse-staff QC and warehouse-manager approval flows.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| Layered Architecture | PASS | Controller accepts picking payloads, dedicated service owns allocation/revision/replacement rules, repositories remain persistence-only. |
| Inventory Integrity | PASS | Planner reservation decreases and concrete reservation increases are transaction-scoped with optimistic locking and non-negative checks. |
| FIFO Batch Selection | PASS | Feature explicitly ranks oldest received valid inventory first and does not introduce FEFO for the current domain. |
| QC Gate & Quarantine | PASS | Candidate inventory excludes quarantine and replacement only uses quality-valid regular stock. |
| In-Transit Tracking | PASS | Picking-plan feature must exclude In-Transit stock and does not bypass later outbound movement stages. |
| Auth & RBAC | PASS | Storekeeper mutations require both storekeeper role and assigned warehouse scope. |
| Test Coverage | PASS | Plan includes service and controller coverage for save, revise, return-to-bin, replacement, conflicts, and audit. |

## Project Structure

### Documentation (this feature)

```text
.sdd/specs/004-outbound-delivery-pod/features/feature-storekeeper-picking-plan/
|-- feature-storekeeper-picking-plan.md
|-- plan.md
|-- research.md
|-- data-model.md
|-- quickstart.md
`-- contracts/
    `-- delivery-order-picking.openapi.yaml
```

### Source Code (repository root)

```text
backend/
`-- src/
    |-- main/java/com/wms/
    |   |-- controller/DeliveryOrderController.java
    |   |-- dto/request/DeliveryOrderPickingPlanRequest.java
    |   |-- dto/request/DeliveryOrderReplacementPlanRequest.java
    |   |-- dto/request/DeliveryOrderAllocationRequest.java
    |   |-- dto/request/DeliveryOrderReturnToBinRequest.java
    |   |-- dto/response/DeliveryOrderResponse.java
    |   |-- dto/response/DeliveryOrderAllocationResponse.java
    |   |-- entity/DeliveryOrder.java
    |   |-- entity/DeliveryOrderItem.java
    |   |-- entity/DeliveryOrderItemAllocation.java
    |   |-- entity/DeliveryOrderItemReturnToBinRecord.java
    |   |-- entity/DeliveryOrderItemReplacement.java
    |   |-- entity/Inventory.java
    |   |-- repository/DeliveryOrderRepository.java
    |   |-- repository/DeliveryOrderItemRepository.java
    |   |-- repository/DeliveryOrderItemAllocationRepository.java
    |   |-- repository/DeliveryOrderItemReturnToBinRecordRepository.java
    |   |-- repository/DeliveryOrderItemReplacementRepository.java
    |   |-- repository/InventoryRepository.java
    |   |-- repository/WarehouseProductReservationRepository.java
    |   |-- service/DeliveryOrderService.java
    |   `-- service/impl/DeliveryOrderServiceImpl.java
    `-- test/java/com/wms/
        |-- controller/DeliveryOrderControllerTest.java
        `-- service/DeliveryOrderServiceImplTest.java
```

**Structure Decision**: Extend the existing Delivery Order backend module instead of introducing a separate outbound submodule. Add allocation, return, and replacement persistence around the current `DeliveryOrderService` flow so planner creation, storekeeper planning, warehouse-staff QC, and cancellation can share one consistent Delivery Order aggregate.

## Phase 0: Research Summary

See [research.md](research.md).

## Phase 1: Design Summary

See [data-model.md](data-model.md), [quickstart.md](quickstart.md), and [contracts/delivery-order-picking.openapi.yaml](contracts/delivery-order-picking.openapi.yaml).

## Post-Design Constitution Check

| Principle | Status | Notes |
|-----------|--------|-------|
| Layered Architecture | PASS | Contracts and data model map cleanly to Controller -> Service -> Repository -> Entity. |
| Inventory Integrity | PASS | Allocation diffing, return-to-bin validation, and replacement reservation all preserve non-negative quantities and versioned updates. |
| FIFO Batch Selection | PASS | Research and contracts keep FIFO ranking on valid regular stock and reject FEFO assumptions for this domain. |
| QC Gate & Quarantine | PASS | Returned goods go back to original valid rows; replacements never consume quarantine or staging stock. |
| In-Transit Tracking | PASS | Contracts explicitly exclude In-Transit candidate stock and leave departure movement to later features. |
| Auth & RBAC | PASS | Both endpoints require Storekeeper role and warehouse assignment check against the Delivery Order warehouse. |
| Test Coverage | PASS | Quickstart lists unit/integration coverage for initial plan, revise, return-to-bin, replacement, and optimistic-lock conflicts. |

## Complexity Tracking

No constitution violations.
