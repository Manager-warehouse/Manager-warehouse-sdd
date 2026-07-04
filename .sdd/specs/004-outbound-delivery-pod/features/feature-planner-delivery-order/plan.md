# Implementation Plan: Planner Lập Delivery Order & Credit/Stock Reservation

**Branch**: `ha-004` | **Date**: 2026-06-17 | **Spec**: [feature-planner-delivery-order.md](feature-planner-delivery-order.md)

**Input**: Feature specification from `.sdd/specs/004-outbound-delivery-pod/features/feature-planner-delivery-order/feature-planner-delivery-order.md`

## Summary

Planner creates outbound Delivery Orders only for assigned warehouses after automatic dealer credit validation and warehouse-level stock availability validation. Successful creation creates a `NEW` Delivery Order, reserves requested product quantities at warehouse/product level through `warehouse_product_reservations`, and leaves final batch/bin/zone allocation to the Storekeeper picking-plan feature. Warehouse Manager may cancel the Delivery Order before `WAREHOUSE_APPROVED`, releasing any planner-level and concrete reservations that exist at the current lifecycle step.

## Technical Context

**Language/Version**: Java 21 for backend, JavaScript/React 18 for frontend.

**Primary Dependencies**: Spring Boot 3.4.5, Spring Data JPA/Hibernate, Jakarta Validation, Spring Security JWT/RBAC, OpenAPI/Swagger.

**Storage**: PostgreSQL 18 with Flyway migrations.

**Testing**: JUnit 5 + Mockito for service/business rules; Spring integration tests for REST endpoints; Jest for frontend only if planner UI changes are implemented.

**Target Platform**: WMS web application and REST API.

**Project Type**: Full-stack web application with REST backend; this feature is primarily backend API/service work.

**Performance Goals**: Create/cancel Delivery Order mutations should complete in one database transaction and use indexed warehouse/product reservation lookups.

**Constraints**: No negative inventory or over-reservation; optimistic locking required for reservation updates; no direct raw SQL in application code; audit log required for successful create/cancel; RBAC must check both role and assigned warehouse.

**Scale/Scope**: Sprint 1 outbound Delivery Order creation and pre-approval cancellation for three physical warehouses.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| Layered Architecture | PASS | Controller delegates to service; service owns credit, stock, reservation, cancellation rules; repositories remain persistence-only. |
| Inventory Integrity | PASS | Reservation updates use optimistic locking and must not make available quantity negative. |
| FIFO Batch Selection | PASS | Planner feature only validates warehouse-level availability; concrete FIFO batch/bin/zone selection is deferred to picking plan. |
| QC Gate & Quarantine | PASS | Availability excludes quarantine and non-quality stock. |
| In-Transit Tracking | PASS | Not reached by this feature. |
| Auth & RBAC | PASS | Planner create and Warehouse Manager cancel require role plus warehouse assignment checks. |
| Test Coverage | PASS | Plan includes unit and integration tests for credit, stock, reservation, warehouse scope, cancellation, and audit. |

## Project Structure

### Documentation (this feature)

```text
.sdd/specs/004-outbound-delivery-pod/features/feature-planner-delivery-order/
├── feature-planner-delivery-order.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
    └── delivery-orders.openapi.yaml
```

### Source Code (repository root)

```text
backend/
└── src/
    ├── main/java/com/wms/
    │   ├── controller/DeliveryOrderController.java
    │   ├── dto/request/DeliveryOrderCreateRequest.java
    │   ├── dto/request/DeliveryOrderItemCreateRequest.java
    │   ├── dto/request/DeliveryOrderCancelRequest.java
    │   ├── dto/response/DeliveryOrderResponse.java
    │   ├── entity/DeliveryOrder.java
    │   ├── entity/DeliveryOrderItem.java
    │   ├── entity/WarehouseProductReservation.java
    │   ├── enums/DeliveryOrderStatus.java
    │   ├── repository/DeliveryOrderRepository.java
    │   ├── repository/WarehouseProductReservationRepository.java
    │   ├── service/DeliveryOrderService.java
    │   └── service/impl/DeliveryOrderServiceImpl.java
    └── test/java/com/wms/
        ├── controller/DeliveryOrderControllerIntegrationTest.java
        └── service/DeliveryOrderServiceImplTest.java
```

**Structure Decision**: Implement inside the existing backend layered architecture and existing Delivery Order module. Add only DTO/repository/service helpers needed for reservation and cancel reason handling.

## Phase 0: Research Summary

See [research.md](research.md).

## Phase 1: Design Summary

See [data-model.md](data-model.md), [quickstart.md](quickstart.md), and [contracts/delivery-orders.openapi.yaml](contracts/delivery-orders.openapi.yaml).

## Post-Design Constitution Check

| Principle | Status | Notes |
|-----------|--------|-------|
| Layered Architecture | PASS | Data model and contracts map to existing Controller -> Service -> Repository -> Entity flow. |
| Inventory Integrity | PASS | Reservation deltas and availability checks are transaction-scoped and versioned. |
| FIFO Batch Selection | PASS | FIFO remains in picking-plan; planner does not assign final inventory rows. |
| QC Gate & Quarantine | PASS | Availability definition includes only quality-valid regular inventory. |
| In-Transit Tracking | PASS | Not applicable in this feature. |
| Auth & RBAC | PASS | Contracts require Planner/Warehouse Manager role and warehouse scope checks. |
| Test Coverage | PASS | Quickstart defines required unit/integration tests. |

## Complexity Tracking

No constitution violations.
