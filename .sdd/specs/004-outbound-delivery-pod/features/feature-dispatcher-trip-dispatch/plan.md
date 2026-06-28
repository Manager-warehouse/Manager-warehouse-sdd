# Implementation Plan: Dispatcher Trip Dispatch

**Branch**: `ha-004` | **Date**: 2026-06-20 | **Spec**: [feature-dispatcher-trip-dispatch.md](feature-dispatcher-trip-dispatch.md)

**Input**: Feature specification from `.sdd/specs/004-outbound-delivery-pod/features/feature-dispatcher-trip-dispatch/feature-dispatcher-trip-dispatch.md`

## Summary

Dispatcher groups warehouse-approved Delivery Orders into a warehouse-scoped outbound trip, assigns one in-warehouse vehicle and driver, and keeps the trip editable only while it remains `PLANNED`. When the assigned driver confirms departure, the feature must move QC-passed staged stock into virtual `IN_TRANSIT`, release staging reservations, initialize one current delivery attempt per Delivery Order, and transition both trip resources and Delivery Orders into `IN_TRANSIT`. Trip completion then returns the operational trip to `COMPLETED` only after the vehicle comes back and every assigned Delivery Order has reached a terminal outbound result.

## Technical Context

**Language/Version**: Java 21 for backend, JavaScript/React 18 for frontend.

**Primary Dependencies**: Spring Boot 3.4.5, Spring Data JPA/Hibernate, Jakarta Validation, Spring Security JWT/RBAC, OpenAPI/Swagger.

**Storage**: PostgreSQL 18 with existing `trips`, `trip_delivery_orders`, and `deliveries` tables plus Flyway migration for any missing request/audit support fields and repository indexes.

**Testing**: JUnit 5 + Mockito for trip create/update/cancel/depart/complete business rules and inventory movement; Spring controller integration tests for trip endpoints; Jest only if a dedicated dispatcher or driver UI is implemented in the same scope.

**Target Platform**: WMS web application and REST API.

**Project Type**: Full-stack web application with REST backend; this feature is primarily backend trip workflow, inventory mutation, and API work.

**Performance Goals**: Trip create/update should validate warehouse, active-trip conflicts, stop order uniqueness, and capacity without N+1 loading. Trip departure should move staged inventory and create delivery attempts in one transaction with deterministic product/batch aggregation.

**Constraints**: Outbound trips in Sprint 1 use `tripType = DELIVERY` only. All Delivery Orders in one trip must share the same warehouse as the Dispatcher, vehicle, and driver. No Delivery Order may belong to more than one active trip. Departure must only dispatch Delivery Orders still in `WAREHOUSE_APPROVED` and with QC-passed staged quantity equal to requested quantity. All inventory/resource changes require optimistic locking, non-negative inventory, and audit logs.

**Scale/Scope**: Sprint 1 outbound dispatch flow for three warehouses, multi-DO planned trips, driver departure, delivery-attempt initialization, and trip completion after downstream delivery outcomes.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| Layered Architecture | PASS | Trip controller should stay thin, service owns dispatch workflow, repositories remain persistence-only. |
| Inventory Integrity | PASS | Departure must move staging to `IN_TRANSIT` in one transaction with non-negative and version-safe updates. |
| FIFO Batch Selection | PASS | This feature consumes already approved and staged outbound quantities; it does not change FIFO selection policy. |
| QC Gate & Quarantine | PASS | Departure requires QC-passed staged stock only; failed goods remain quarantined and are out of scope for dispatch movement. |
| In-Transit Tracking | PASS | Trip departure is the boundary that moves goods into virtual `IN_TRANSIT` and initializes delivery attempts. |
| Auth & RBAC | PASS | Dispatcher and driver mutations both require role plus warehouse or assignment scope. |
| Test Coverage | PASS | Plan includes service and controller coverage for create/update/cancel/depart/complete, active-trip conflicts, and staged inventory movement. |

## Project Structure

### Documentation (this feature)

```text
.sdd/specs/004-outbound-delivery-pod/features/feature-dispatcher-trip-dispatch/
|-- feature-dispatcher-trip-dispatch.md
|-- plan.md
|-- research.md
|-- data-model.md
|-- quickstart.md
`-- contracts/
    `-- trips.openapi.yaml
```

### Source Code (repository root)

```text
backend/
`-- src/
    |-- main/java/com/wms/
    |   |-- controller/TripController.java
    |   |-- dto/request/TripCreateRequest.java
    |   |-- dto/request/TripUpdateRequest.java
    |   |-- dto/request/TripCancelRequest.java
    |   |-- dto/request/TripDepartRequest.java
    |   |-- dto/request/TripCompleteRequest.java
    |   |-- dto/request/TripDeliveryOrderRequest.java
    |   |-- dto/response/TripResponse.java
    |   |-- dto/response/TripDeliveryOrderResponse.java
    |   |-- entity/Trip.java
    |   |-- entity/TripDeliveryOrder.java
    |   |-- entity/Delivery.java
    |   |-- entity/DeliveryOrder.java
    |   |-- entity/DeliveryOrderItem.java
    |   |-- entity/DeliveryOrderItemAllocation.java
    |   |-- entity/Inventory.java
    |   |-- entity/Vehicle.java
    |   |-- entity/Driver.java
    |   |-- repository/TripRepository.java
    |   |-- repository/TripDeliveryOrderRepository.java
    |   |-- repository/DeliveryRepository.java
    |   |-- repository/DeliveryOrderRepository.java
    |   |-- repository/DeliveryOrderItemRepository.java
    |   |-- repository/InventoryRepository.java
    |   |-- repository/VehicleRepository.java
    |   |-- repository/DriverRepository.java
    |   |-- service/TripService.java
    |   `-- service/impl/TripServiceImpl.java
    `-- test/java/com/wms/
        |-- controller/TripControllerTest.java
        `-- service/TripServiceImplTest.java
```

**Structure Decision**: Implement trip dispatch as a dedicated `TripService` and `TripController` centered on existing `Trip`, `TripDeliveryOrder`, `Delivery`, `Vehicle`, and `Driver` entities. Delivery Order inventory and lifecycle updates remain coordinated through outbound domain repositories rather than being embedded inside the current `DeliveryOrderServiceImpl`.

## Phase 0: Research Summary

See [research.md](research.md).

## Phase 1: Design Summary

See [data-model.md](data-model.md), [quickstart.md](quickstart.md), and [contracts/trips.openapi.yaml](contracts/trips.openapi.yaml).

## Post-Design Constitution Check

| Principle | Status | Notes |
|-----------|--------|-------|
| Layered Architecture | PASS | Contracts and data model map cleanly to Controller -> Service -> Repository -> Entity. |
| Inventory Integrity | PASS | Departure design keeps staging decrement, reservation release, and `IN_TRANSIT` increment in one version-safe transaction. |
| FIFO Batch Selection | PASS | Design consumes pre-approved staged stock only and does not weaken earlier FIFO planning rules. |
| QC Gate & Quarantine | PASS | Dispatch contract blocks departure unless requested quantity is fully covered by QC-passed staged stock. |
| In-Transit Tracking | PASS | Delivery attempts are created at departure and trip completion does not prematurely receive returned goods back to regular stock. |
| Auth & RBAC | PASS | Dispatcher endpoints remain warehouse-scoped and driver endpoints remain assignment-scoped. |
| Test Coverage | PASS | Quickstart covers service/controller tests for conflicts, capacity, departure movement, delivery-attempt initialization, and completion gates. |

## Complexity Tracking

No constitution violations.
