# Implementation Plan: Driver Mobile POD

**Branch**: `ha-004` | **Date**: 2026-06-20 | **Spec**: [feature-driver-mobile-pod.md](feature-driver-mobile-pod.md)

**Input**: Feature specification from `.sdd/specs/004-outbound-delivery-pod/features/feature-driver-mobile-pod/feature-driver-mobile-pod.md`

## Summary

Driver users work from a mobile-focused trip view to upload POD evidence, request and confirm a dealer OTP, record dealer refusal, and complete the trip after vehicle return. For one Delivery Order split across multiple vehicles, only the lead driver operates the mobile workflow: confirm split departure, confirm whole-convoy dealer arrival, confirm whole-Delivery-Order handover, upload/replace the one shared POD pair, request/resend the one shared OTP, confirm delivery, report failure, and complete the whole convoy return. Support-vehicle drivers do not perform mobile actions for the split delivery. Replacing the complete POD pair invalidates the current usable OTP; email failure persists `SEND_FAILED` for immediate retry. Successful OTP confirmation updates the one current delivery attempt, consumes the OTP, decreases virtual `IN_TRANSIT` inventory only once, auto-creates invoice and receivable records, and moves the Delivery Order to `COMPLETED` without releasing vehicles. The lead driver later completes the whole split convoy return to release all linked split drivers and vehicles.

The driver mobile entry list must now use neutral transport wording rather than delivery-only wording. It must show both assigned `DELIVERY` and `TRANSFER` trips, label each card as `Giao dai ly` or `Dieu chuyen noi bo`, and provide three filters: `Tat ca`, `Noi bo`, and `Dai ly`. Delivery cards continue into the POD/OTP flow in this feature; transfer cards continue into the Spec 005 transfer departure/arrival/handover flow.

## Technical Context

**Language/Version**: Java 21 for backend, JavaScript/React 18 for frontend.

**Primary Dependencies**: Spring Boot 3.4.5, Spring Data JPA/Hibernate, Jakarta Validation, Spring Security JWT/RBAC, multipart upload support, mail delivery support, OpenAPI/Swagger.

**Storage**: PostgreSQL 18 stores Delivery/OTP records plus relative POD paths and metadata. POD image binaries are stored under a configurable persistent local-storage root on the VPS, outside the application release directory and covered by backup/restore procedures.

**Testing**: JUnit 5 + Mockito for driver assignment, OTP lifecycle, POD validation, delivery confirmation, failure, and trip completion rules; Spring controller integration tests for driver/mobile and admin reset endpoints; frontend/mobile tests only if a dedicated driver UI is implemented in scope.

**Target Platform**: WMS web application and REST API with driver mobile-facing endpoints.

**Project Type**: Full-stack web application with REST backend; this feature is primarily backend delivery-attempt, OTP, inventory, and API work.

**Performance Goals**: POD upload validation should reject invalid files before storage, OTP request/confirm should run without extra attempt lookups, and delivery confirmation should commit inventory, OTP, invoice, receivable, and status updates in one transaction.

**Constraints**: Driver may only act on trips assigned to their own driver profile, and split-delivery mutations are lead-driver-only. Sprint 1 uses full Delivery Order delivery only and never uses `OUT_FOR_DELIVERY`. A split Delivery Order has one current attempt, one shared POD pair, and one OTP row; only the lead driver can mutate split milestones, POD, OTP, failure, and whole-convoy return after lead-confirmed handover. POD paths must remain under the configured persistent VPS storage root, and image reads must pass Delivery Order authorization instead of using public static URLs. OTP is backend-generated, exactly 6 digits, valid for 5 minutes, stored only as a hash/verifier, and `SEND_FAILED` is immediately retryable on the same row. Returned goods remain in virtual `IN_TRANSIT` until the separate return flow completes. Every mutation requires audit logs and optimistic locking on inventory updates.

**Driver Trip List UX Constraint**: `GET /api/v1/trips/driver` remains a read-only list endpoint. It must expose or normalize `tripType`, `tripTypeLabel`, and type-specific summary fields so the frontend can filter locally without causing audit or state changes. If server-side filtering by trip type is later added, it must remain semantically equivalent to the same client-side filters.

**Scale/Scope**: Sprint 1 outbound mobile POD flow for three warehouses, one current delivery attempt per dispatched Delivery Order, OTP resend/reset, failure handling, and trip completion after downstream delivery outcomes.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| Layered Architecture | PASS | Driver/mobile controllers stay thin, service owns POD/OTP/delivery workflow, repositories remain persistence-only. |
| Inventory Integrity | PASS | Successful delivery decreases virtual `IN_TRANSIT` rows in one transaction with optimistic locking and non-negative checks. |
| Batch Candidate Ordering | PASS | This feature consumes already dispatched `IN_TRANSIT` stock and does not alter Storekeeper batch choice. |
| QC Gate & Quarantine | PASS | Only QC-passed dispatched goods are delivered; failed goods remain outside this flow in quarantine. |
| In-Transit Tracking | PASS | Delivery success and failure operate strictly on current attempts and virtual `IN_TRANSIT` inventory. |
| Auth & RBAC | PASS | Driver endpoints are trip-assignment scoped and admin reset stays role-gated. |
| Test Coverage | PASS | Plan includes service and controller coverage for POD upload, OTP lifecycle, confirmation, failure, reset, and trip completion. |

## Project Structure

### Documentation (this feature)

```text
.sdd/specs/004-outbound-delivery-pod/features/feature-driver-mobile-pod/
|-- feature-driver-mobile-pod.md
|-- plan.md
|-- research.md
|-- data-model.md
|-- quickstart.md
`-- contracts/
    `-- driver-pod.openapi.yaml
```

### Source Code (repository root)

```text
backend/
`-- src/
    |-- main/java/com/wms/
    |   |-- controller/TripController.java
    |   |-- controller/AdminDeliveryController.java
    |   |-- dto/request/DeliveryOtpRequest.java
    |   |-- dto/request/ConfirmDeliveryRequest.java
    |   |-- dto/request/FailDeliveryRequest.java
    |   |-- dto/request/ResetDeliveryOtpRequest.java
    |   |-- dto/request/TripCompleteRequest.java
    |   |-- dto/response/TripDriverViewResponse.java
    |   |-- dto/response/DriverTripSummaryResponse.java
    |   |-- dto/response/DeliveryAttemptResponse.java
    |   |-- entity/Trip.java
    |   |-- entity/TripDeliveryOrder.java
    |   |-- entity/Delivery.java
    |   |-- entity/DeliveryOtpAttempt.java
    |   |-- entity/DeliveryOrder.java
    |   |-- entity/DeliveryOrderItem.java
    |   |-- entity/Inventory.java
    |   |-- entity/Invoice.java
    |   |-- repository/TripRepository.java
    |   |-- repository/TripDeliveryOrderRepository.java
    |   |-- repository/DeliveryRepository.java
    |   |-- repository/DeliveryOtpAttemptRepository.java
    |   |-- repository/DeliveryOrderRepository.java
    |   |-- repository/DeliveryOrderItemRepository.java
    |   |-- repository/InventoryRepository.java
    |   |-- repository/DriverRepository.java
    |   |-- repository/InvoiceRepository.java
    |   |-- service/DriverDeliveryService.java
    |   |-- service/TripService.java
    |   `-- service/impl/DriverDeliveryServiceImpl.java
    `-- test/java/com/wms/
        |-- controller/DriverDeliveryControllerTest.java
        |-- controller/AdminDeliveryControllerTest.java
        `-- service/DriverDeliveryServiceImplTest.java
```

```text
frontend/
`-- src/
    |-- pages/Outbound/DriverTrip.jsx
    |-- services/outbound.service.js
    |-- services/inter-warehouse-transfer.service.js
    |-- routes/AppRoutes.jsx
```

**Structure Decision**: Implement a dedicated driver-delivery service around the existing `Delivery`, `DeliveryOtpAttempt`, and trip aggregates. Driver/mobile endpoints should live alongside trip endpoints, while admin OTP reset uses a separate admin-facing controller to keep driver-assignment and admin-reset concerns distinct.

Split delivery milestones remain owned by `SplitDeliveryPlanService`: the lead driver confirms whole-convoy arrival and handover, while `DriverDeliveryService` enforces the lead-driver rule before shared POD/OTP actions. Split return completion is also lead-only and releases every linked split leg trip, driver, and vehicle together.

## Phase 0: Research Summary

See [research.md](research.md).

## Phase 1: Design Summary

See [data-model.md](data-model.md), [quickstart.md](quickstart.md), and [contracts/driver-pod.openapi.yaml](contracts/driver-pod.openapi.yaml).

## Post-Design Constitution Check

| Principle | Status | Notes |
|-----------|--------|-------|
| Layered Architecture | PASS | Contracts and data model map cleanly to Controller -> Service -> Repository -> Entity. |
| Inventory Integrity | PASS | Successful delivery only decrements virtual `IN_TRANSIT` rows for the confirmed Delivery Order and keeps all updates version-safe. |
| Batch Candidate Ordering | PASS | Design consumes dispatched stock only and does not introduce received-date allocation enforcement. |
| QC Gate & Quarantine | PASS | POD flow never bypasses outbound QC or quarantine semantics. |
| In-Transit Tracking | PASS | Current delivery attempt, OTP lifecycle, and trip completion all remain anchored to `IN_TRANSIT` tracking. |
| Auth & RBAC | PASS | Driver endpoints remain assignment-scoped and admin reset remains separately role-gated. |
| Test Coverage | PASS | Quickstart covers controller and service tests for POD validation, OTP resend/lock/reset, confirmation, failure, and trip completion. |

## Complexity Tracking

No constitution violations.
