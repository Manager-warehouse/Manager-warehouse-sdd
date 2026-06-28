# Implementation Plan: Warehouse Staff Receipt Counting

**Branch**: `ha-spec-003` | **Date**: 2026-06-14 | **Spec**: [feature-warehouse-staff-receipt-counting.md](./feature-warehouse-staff-receipt-counting.md)

**Input**: Feature specification from `.sdd/specs/003-inbound-receipt-qc/features/feature-warehouse-staff-receipt-counting/feature-warehouse-staff-receipt-counting.md`

## Summary

Implement complete physical receipt counting for Warehouse Staff through `PUT /api/v1/receipts/{id}/receive`. The endpoint accepts one positive integer `counted_qty` for every receipt item, atomically derives `actual_qty` and `over_received_qty`, moves `PENDING_RECEIPT` receipts to `DRAFT`, allows count correction while receipt status is `DRAFT`, `QC_COMPLETED`, or `QC_FAILED`, invalidates prior QC data on correction, and records a `RECEIPT_RECEIVE` audit trail. This feature does not create batches, update inventory, assign locations, putaway goods, or perform QC inspection.

## Technical Context

**Language/Version**: Java 21 with Spring Boot 3.4.5; React 18 only if a Warehouse Staff receive-count UI is included

**Primary Dependencies**: Spring Web, Spring Data JPA/Hibernate, Jakarta Validation, Spring Security/JWT, springdoc OpenAPI; existing `ReceiptService`, `ReceiptRepository`, `ReceiptItemRepository`, `ReceiptMapper`, and `AuditLogService`

**Storage**: PostgreSQL 18 via JPA entities `receipts` and `receipt_items`; quantity fields for this inbound flow are integer semantics (`expected_qty`, `actual_qty`, `over_received_qty`, `sample_qty`, `sample_passed_qty`, `sample_failed_qty`)

**Testing**: JUnit 5, Mockito, Spring MVC integration tests; Jest for frontend work if added

**Target Platform**: Full-stack web application and REST API

**Project Type**: Web application with REST backend

**Performance Goals**: Count submission completes within the inbound receipt flow target of <= 2s; validation uses receipt item lookup scoped by receipt id and avoids N+1 item updates

**Constraints**: No raw SQL; no inventory, batch, quarantine, or location mutation; every receive/count mutation must create audit log data with actor/action/timestamp/before/after; RBAC must check Warehouse Staff role and warehouse assignment; request is all-or-nothing

**Scale/Scope**: Sprint 1 WMS, 3 physical warehouses, 1000+ products, 50+ suppliers/dealers, 1000+ transactions/month

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| Layered Architecture | PASS | Add Controller -> Service -> Repository -> Entity/DTO flow for receive counting. |
| Inventory Integrity | PASS | Feature updates receipt item count fields only and explicitly does not mutate inventory. |
| FIFO Batch Allocation | PASS | Out of scope; no batch selection or batch mutation in this feature. |
| QC Gate & Quarantine | PASS | Counting moves to `DRAFT`; QC remains a later feature and prior QC data is invalidated on count correction. |
| In-Transit Tracking | PASS | Not a transfer feature. |
| Auth & RBAC | PASS | Warehouse Staff must be authorized for the receipt warehouse. |
| Test Coverage | PASS | Plan requires service unit tests and API integration tests for happy/error paths. |

No constitution violations require justification.

## Project Structure

### Documentation (this feature)

```text
.sdd/specs/003-inbound-receipt-qc/features/feature-warehouse-staff-receipt-counting/
|-- feature-warehouse-staff-receipt-counting.md
|-- plan.md
|-- research.md
|-- data-model.md
|-- quickstart.md
`-- contracts/
    `-- receive-receipt.openapi.yaml
```

### Source Code (repository root)

```text
backend/
|-- src/main/java/com/wms/controller/
|-- src/main/java/com/wms/dto/request/
|-- src/main/java/com/wms/dto/response/
|-- src/main/java/com/wms/entity/
|-- src/main/java/com/wms/enums/
|-- src/main/java/com/wms/exception/
|-- src/main/java/com/wms/mapper/
|-- src/main/java/com/wms/repository/
|-- src/main/java/com/wms/service/
|-- src/main/resources/db/migration/
`-- src/test/java/com/wms/

frontend/
|-- src/components/
|-- src/pages/
|-- src/services/
`-- src/utils/
```

**Structure Decision**: Implement the receive-counting API in the existing backend layered package layout. Frontend files are optional and only needed if this slice includes the Warehouse Staff receive-count screen.

## Phase 0: Research

See [research.md](./research.md).

## Phase 1: Design And Contracts

See [data-model.md](./data-model.md), [quickstart.md](./quickstart.md), and [contracts/receive-receipt.openapi.yaml](./contracts/receive-receipt.openapi.yaml).

## Constitution Check Post-Design

| Principle | Status | Notes |
|-----------|--------|-------|
| Layered Architecture | PASS | Design keeps HTTP, business validation, persistence, and entity mapping separated. |
| Inventory Integrity | PASS | Contract and model explicitly exclude inventory, batch, quarantine, and location writes. |
| FIFO Batch Allocation | PASS | No batch selection occurs. |
| QC Gate & Quarantine | PASS | Corrections after QC clear QC data and return receipt to `DRAFT`. |
| In-Transit Tracking | PASS | Not applicable. |
| Auth & RBAC | PASS | Service validates Warehouse Staff role and warehouse assignment. |
| Test Coverage | PASS | Required tests cover calculation, status transitions, atomic rejection, audit, and API errors. |

## Complexity Tracking

No constitution violations or extra complexity accepted for this feature.
