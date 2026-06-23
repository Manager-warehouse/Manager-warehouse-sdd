# Implementation Plan: Accountant Receivable Payment

**Branch**: `ha-004` | **Date**: 2026-06-20 | **Spec**: [feature-accountant-receivable-payment.md](feature-accountant-receivable-payment.md)

**Input**: Feature specification from `.sdd/specs/004-outbound-delivery-pod/features/feature-accountant-receivable-payment/feature-accountant-receivable-payment.md`

## Summary

Successful Driver Mobile POD confirmation must automatically create exactly one invoice and receivable effect for the confirmed Delivery Order only. Invoice amount is calculated from Delivery Order item full delivered quantity and the `unit_price` snapshot captured during picking preparation, then Dealer `current_balance` is increased in the same transaction that completes delivery. The flow is idempotent per Delivery Order: if the invoice already exists, the existing invoice result is returned without increasing Dealer balance again. Notifications, payment collection, approval, deduction, due-date extension, and Delivery Order `CLOSED` transition stay out of scope.

## Technical Context

**Language/Version**: Java 21 for backend, JavaScript/React 18 for frontend.

**Primary Dependencies**: Spring Boot 3.4.5, Spring Data JPA/Hibernate, Jakarta Validation, Spring Security JWT/RBAC, OpenAPI/Swagger for adjacent driver-delivery endpoint documentation.

**Storage**: PostgreSQL 18 with existing `invoices`, `dealers.current_balance`, `delivery_orders`, and `delivery_order_items`; Flyway migration may add invoice idempotency constraints, invoice line/detail table, or audit linkage if missing.

**Testing**: JUnit 5 + Mockito for auto-invoice service logic, idempotency, rollback, amount calculation, and Dealer balance effects; controller-level coverage through driver confirm-delivery tests because this feature has no user-facing endpoint of its own.

**Target Platform**: WMS web application and REST API.

**Project Type**: Full-stack web application with REST backend; this feature is an internal backend workflow triggered by Driver Mobile POD confirmation.

**Performance Goals**: Invoice creation should load Delivery Order, dealer, and item pricing in one transaction without re-querying current product prices; idempotent retry should be a quick invoice lookup by Delivery Order.

**Constraints**: No manual invoice endpoint in this feature. No notifications or payment mutation behavior. No partial delivery invoices. Invoice creation, Dealer balance increase, Delivery Order completion, and driver delivery confirmation effects must commit or roll back together. Existing invoice must not double-count Dealer balance.

**Scale/Scope**: Sprint 1 outbound full-delivery confirmation for three warehouses, one invoice per completed Delivery Order, Dealer current-balance increase, and audit linkage to `CONFIRM_DELIVERY`.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| Layered Architecture | PASS | Internal service owns invoice/receivable logic, repositories remain persistence-only, driver controller only triggers through confirm-delivery. |
| Inventory Integrity | PASS | This feature participates in the same confirm-delivery transaction and does not bypass inventory updates. |
| FIFO Batch Selection | PASS | Invoice uses Delivery Order item snapshots and does not alter batch allocation rules. |
| QC Gate & Quarantine | PASS | Invoice is created only after successful full POD + OTP confirmation for goods already QC-passed and dispatched. |
| In-Transit Tracking | PASS | Invoice creation is tied to successful confirmation that removes the specific Delivery Order from virtual `IN_TRANSIT`. |
| Auth & RBAC | PASS | No standalone endpoint; access is inherited from the driver confirm-delivery flow. |
| Test Coverage | PASS | Plan includes service tests for amount calculation, idempotency, rollback, and same-trip isolation. |

## Project Structure

### Documentation (this feature)

```text
.sdd/specs/004-outbound-delivery-pod/features/feature-accountant-receivable-payment/
|-- feature-accountant-receivable-payment.md
|-- plan.md
|-- research.md
|-- data-model.md
|-- quickstart.md
`-- contracts/
    `-- auto-invoice.openapi.yaml
```

### Source Code (repository root)

```text
backend/
`-- src/
    |-- main/java/com/wms/
    |   |-- entity/Invoice.java
    |   |-- entity/InvoiceLine.java
    |   |-- entity/Dealer.java
    |   |-- entity/DeliveryOrder.java
    |   |-- entity/DeliveryOrderItem.java
    |   |-- repository/InvoiceRepository.java
    |   |-- repository/InvoiceLineRepository.java
    |   |-- repository/DealerRepository.java
    |   |-- repository/DeliveryOrderRepository.java
    |   |-- repository/DeliveryOrderItemRepository.java
    |   |-- service/AutoInvoiceService.java
    |   |-- service/DriverDeliveryService.java
    |   `-- service/impl/AutoInvoiceServiceImpl.java
    `-- test/java/com/wms/
        |-- service/AutoInvoiceServiceImplTest.java
        `-- service/DriverDeliveryServiceImplTest.java
```

**Structure Decision**: Implement invoice and receivable creation as a dedicated internal `AutoInvoiceService` invoked by the successful driver confirm-delivery transaction. This keeps financial calculation and idempotency separate from OTP/POD mechanics while preserving one transaction boundary.

## Phase 0: Research Summary

See [research.md](research.md).

## Phase 1: Design Summary

See [data-model.md](data-model.md), [quickstart.md](quickstart.md), and [contracts/auto-invoice.openapi.yaml](contracts/auto-invoice.openapi.yaml).

## Post-Design Constitution Check

| Principle | Status | Notes |
|-----------|--------|-------|
| Layered Architecture | PASS | Auto-invoice logic is isolated in service methods invoked by driver delivery confirmation. |
| Inventory Integrity | PASS | Financial effects share the same transaction as Delivery Order confirmation and inventory decrement. |
| FIFO Batch Selection | PASS | Invoice uses stored item price/quantity snapshots and does not re-plan stock. |
| QC Gate & Quarantine | PASS | Invoice cannot be created before successful full POD + OTP confirmation. |
| In-Transit Tracking | PASS | Creation aligns with the exact Delivery Order removed from virtual `IN_TRANSIT`. |
| Auth & RBAC | PASS | No public mutation endpoint is introduced for this feature. |
| Test Coverage | PASS | Quickstart defines focused service and integration coverage for idempotency, rollback, and same-trip isolation. |

## Complexity Tracking

No constitution violations.
