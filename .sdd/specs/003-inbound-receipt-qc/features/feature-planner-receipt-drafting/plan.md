# Implementation Plan: Planner Receipt Drafting

**Branch**: `003-inbound-receipt-qc` | **Date**: 2026-06-13 | **Spec**: [feature-planner-receipt-drafting.md](./feature-planner-receipt-drafting.md)

**Input**: Feature specification from `.sdd/specs/003-inbound-receipt-qc/features/feature-planner-receipt-drafting/feature-planner-receipt-drafting.md`

## Summary

Implement supplier purchase receipt drafting for Planner users through `POST /api/v1/receipts`. The flow creates `PURCHASE` receipts only, validates supplier, warehouse, source channel, duplicate supplier/warehouse/source reference, and expected item lines, then persists the receipt in `PENDING_RECEIPT` with a generated unique receipt number and `RECEIPT_CREATE` audit trail. No inventory, batch, QC, or quarantine state changes happen in this feature.

## Technical Context

**Language/Version**: Java 21 with Spring Boot 3.4.5; React 18 if Planner UI is included

**Primary Dependencies**: Spring Web, Spring Data JPA/Hibernate, Jakarta Validation, Spring Security/JWT, springdoc OpenAPI; React 18, Tailwind CSS 3.x

**Storage**: PostgreSQL 18 via JPA entities `receipts`, `receipt_items`, and `document_sequences`; `receipt_items.expected_qty` is stored as `INTEGER`

**Testing**: JUnit 5, Mockito, Spring MVC integration tests; Jest for frontend work

**Target Platform**: Full-stack web application and REST API

**Project Type**: Web application with REST backend

**Performance Goals**: Receipt creation request completes within the inbound spec target of <= 2s; duplicate checks use indexed repository access where schema supports it

**Constraints**: No raw SQL; no negative inventory; no QC bypass; receipt creation must not update inventory; every warehouse mutation must create audit log data with actor/action/timestamp/before/after where relevant; RBAC must check role and warehouse assignment

**Scale/Scope**: Sprint 1 WMS, 3 physical warehouses, 1000+ products, 50+ suppliers/dealers, 1000+ transactions/month

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| Layered Architecture | PASS | Add Controller -> Service -> Repository -> Entity flow for receipt creation. |
| Inventory Integrity | PASS | Feature creates draft receipt only and explicitly does not mutate inventory. |
| FEFO/FIFO Batch Selection | PASS | Out of scope until approval/issue flows; no batch selection here. |
| QC Gate & Quarantine | PASS | Receipt remains `PENDING_RECEIPT`; QC is handled by later features. |
| In-Transit Tracking | PASS | Not a transfer feature. |
| Auth & RBAC | PASS | Planner must be authorized for target warehouse. |
| Test Coverage | PASS | Plan requires service unit tests and API integration tests. |

No constitution violations require justification.

## Project Structure

### Documentation (this feature)

```text
.sdd/specs/003-inbound-receipt-qc/features/feature-planner-receipt-drafting/
|-- feature-planner-receipt-drafting.md
|-- plan.md
|-- research.md
|-- data-model.md
|-- quickstart.md
`-- contracts/
    `-- create-receipt.openapi.yaml
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
|-- src/main/java/com/wms/repository/
|-- src/main/java/com/wms/service/
|-- src/main/java/com/wms/service/impl/
`-- src/test/java/com/wms/

frontend/
|-- src/components/
|-- src/pages/
|-- src/services/
`-- src/utils/
```

**Structure Decision**: Implement the API in the existing backend layered package layout. Frontend files are only needed if the Planner drafting screen is included in the implementation task set.

## Phase 0: Research

See [research.md](./research.md).

## Phase 1: Design And Contracts

See [data-model.md](./data-model.md), [quickstart.md](./quickstart.md), and [contracts/create-receipt.openapi.yaml](./contracts/create-receipt.openapi.yaml).

## Complexity Tracking

No constitution violations or extra complexity accepted for this feature.
