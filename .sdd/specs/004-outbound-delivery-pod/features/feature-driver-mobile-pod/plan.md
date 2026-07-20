# Implementation Plan: Driver Mobile POD

**Branch**: `ha-004` | **Date**: 2026-06-20 | **Spec**: [feature-driver-mobile-pod.md](feature-driver-mobile-pod.md)

**Input**: Feature specification from `.sdd/specs/004-outbound-delivery-pod/features/feature-driver-mobile-pod/feature-driver-mobile-pod.md`

## Summary

Driver users work from a mobile-focused trip view to upload POD evidence, request and confirm a dealer OTP, record dealer refusal, and complete the trip after vehicle return. Successful OTP confirmation must update the current delivery attempt, consume the OTP, decrease virtual `IN_TRANSIT` inventory only for the confirmed Delivery Order, auto-create invoice and receivable records, and move that Delivery Order to `COMPLETED` in one transaction. Failed delivery keeps goods in virtual `IN_TRANSIT`, while trip completion only releases the operational trip after every assigned Delivery Order is `COMPLETED` or `RETURNED`.

The driver mobile entry list must now use neutral transport wording rather than delivery-only wording. It must show both assigned `DELIVERY` and `TRANSFER` trips, label each card as `Giao dai ly` or `Dieu chuyen noi bo`, and provide three filters: `Tat ca`, `Noi bo`, and `Dai ly`. Delivery cards continue into the POD/OTP flow in this feature; transfer cards continue into the Spec 005 transfer departure/arrival/handover flow.

## Technical Context

**Language/Version**: Java 21 for backend, JavaScript/React 18 for frontend.

**Primary Dependencies**: Spring Boot 3.4.5, Spring Data JPA/Hibernate, Jakarta Validation, Spring Security JWT/RBAC, multipart upload support, mail delivery support, OpenAPI/Swagger.

**Storage**: PostgreSQL 18 with existing `deliveries`, `delivery_otp_attempts`, `trips`, and outbound inventory tables plus Flyway migration for any missing attempt, OTP-lock, POD-note, or invoice-link support fields.

**Testing**: JUnit 5 + Mockito for driver assignment, OTP lifecycle, POD validation, delivery confirmation, failure, and trip completion rules; Spring controller integration tests for driver/mobile and admin reset endpoints; frontend/mobile tests only if a dedicated driver UI is implemented in scope.

**Target Platform**: WMS web application and REST API with driver mobile-facing endpoints.

**Project Type**: Full-stack web application with REST backend; this feature is primarily backend delivery-attempt, OTP, inventory, and API work.

**Performance Goals**: POD upload validation should reject invalid files before storage, OTP request/confirm should run without extra attempt lookups, and delivery confirmation should commit inventory, OTP, invoice, receivable, and status updates in one transaction.

**Constraints**: Driver may only act on trips assigned to their own driver profile. Sprint 1 uses full Delivery Order delivery only and never uses `OUT_FOR_DELIVERY`. OTP is always backend-generated, exactly 6 digits, valid for 5 minutes, and stored only as a hash/verifier. Only one active OTP row exists per current delivery attempt. Returned goods remain in virtual `IN_TRANSIT` until a separate return flow handles them. Every mutation requires audit logs and optimistic locking on inventory updates.

**Driver Trip List UX Constraint**: `GET /api/v1/trips/driver` remains a read-only list endpoint. It must expose or normalize `tripType`, `tripTypeLabel`, and type-specific summary fields so the frontend can filter locally without causing audit or state changes. If server-side filtering by trip type is later added, it must remain semantically equivalent to the same client-side filters.

**Scale/Scope**: Sprint 1 outbound mobile POD flow for three warehouses, one current delivery attempt per dispatched Delivery Order, OTP resend/reset, failure handling, and trip completion after downstream delivery outcomes.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| Layered Architecture | PASS | Driver/mobile controllers stay thin, service owns POD/OTP/delivery workflow, repositories remain persistence-only. |
| Inventory Integrity | PASS | Successful delivery decreases virtual `IN_TRANSIT` rows in one transaction with optimistic locking and non-negative checks. |
| FIFO Batch Selection | PASS | This feature consumes already dispatched `IN_TRANSIT` stock and does not alter FIFO planning policy. |
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

## Phase 0: Research Summary

See [research.md](research.md).

## Phase 1: Design Summary

See [data-model.md](data-model.md), [quickstart.md](quickstart.md), and [contracts/driver-pod.openapi.yaml](contracts/driver-pod.openapi.yaml).

## Post-Design Constitution Check

| Principle | Status | Notes |
|-----------|--------|-------|
| Layered Architecture | PASS | Contracts and data model map cleanly to Controller -> Service -> Repository -> Entity. |
| Inventory Integrity | PASS | Successful delivery only decrements virtual `IN_TRANSIT` rows for the confirmed Delivery Order and keeps all updates version-safe. |
| FIFO Batch Selection | PASS | Design consumes dispatched stock only and does not weaken earlier FIFO allocation rules. |
| QC Gate & Quarantine | PASS | POD flow never bypasses outbound QC or quarantine semantics. |
| In-Transit Tracking | PASS | Current delivery attempt, OTP lifecycle, and trip completion all remain anchored to `IN_TRANSIT` tracking. |
| Auth & RBAC | PASS | Driver endpoints remain assignment-scoped and admin reset remains separately role-gated. |
| Test Coverage | PASS | Quickstart covers controller and service tests for POD validation, OTP resend/lock/reset, confirmation, failure, and trip completion. |

## Complexity Tracking

No constitution violations.
