# Implementation Plan: Inbound Receipt Approval & Quarantine Handling

**Branch**: `003-inbound-receipt-qc` | **Date**: 2026-06-11 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specifications:
- [feature-manager-receipt-approval.md](./features/feature-manager-receipt-approval.md)
- [feature-manager-quarantine-handling.md](./features/feature-manager-quarantine-handling.md)

## Summary

Implement Sprint 1 manager approval and quarantine handling for inbound receipts. A Trưởng kho can approve `QC_COMPLETED` receipts to unlock putaway, reject `QC_COMPLETED` receipts into `RETURN_TO_SUPPLIER_PENDING`, and Storekeeper later confirms supplier handover into `RETURNED_TO_SUPPLIER`. Separately, `QC_FAILED` receipts in Quarantine can be returned to vendor through RTV: Trưởng kho creates a pending `RETURN_TO_VENDOR` adjustment and system-generated Debit Note, then Storekeeper confirms full-quantity handover before quarantine inventory is deducted.

## Technical Context

**Language/Version**: Java 21 / Spring Boot 3.4.5; React 18 + JavaScript for inbound UI updates

**Primary Dependencies**: Spring Web, Spring Data JPA, Hibernate, Spring Security/JWT, Jakarta Validation, Lombok, Springdoc OpenAPI, React, Tailwind CSS

**Storage**: PostgreSQL 18 via Flyway migrations and Spring Data JPA

**Testing**: JUnit 5 + Mockito for service/business logic; Spring integration tests for APIs; Jest + React Testing Library for inbound UI business behavior if frontend is changed

**Target Platform**: Full-stack WMS web application and REST API

**Project Type**: Backend + frontend web application

**Performance Goals**:
- Receipt approve/reject/return-confirm response: <= 2s
- RTV create/confirm and quarantine inventory mutation: <= 2s
- Duplicate/concurrent state conflict detection: immediate HTTP 409 without duplicate inventory or accounting records

**Constraints**:
- No negative inventory; quarantine inventory cannot go below zero
- No regular inventory increase on approval; regular inventory increases only after putaway completion
- RTV confirmation must return the full quarantined quantity; partial confirmation is rejected
- No serial, expiry, or quality-tier reclassification for household goods
- All write endpoints use DTO validation, role + warehouse authorization, optimistic locking, and audit logs
- Application code must use Spring Data JPA/Hibernate; no raw SQL in services

**Scale/Scope**: 3 physical warehouses, 1000+ products, 1000+ transactions/month; scope is Spec 003 US-WMS-04 and US-WMS-05 plus direct putaway/status dependencies

## Constitution Check

*GATE: Pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] Layered architecture preserved: Controller -> Service -> Repository -> Entity.
- [x] Write endpoints use request DTOs with Jakarta Validation.
- [x] Service methods own business rules, transactions, authorization, and audit logging.
- [x] All DB access goes through Spring Data JPA/Hibernate; no raw SQL in application code.
- [x] Inventory invariants preserved when touched: `total_qty >= 0`, `reserved_qty >= 0`, `available = total_qty - reserved_qty >= 0`, `@Version`.
- [x] QC/quarantine/accounting state rules listed.
- [x] Audit action, entity type, before/after payload, and warehouse scope identified.
- [x] OpenAPI/Swagger impact identified for every new or changed endpoint.
- [x] Flyway migration impact identified; no migration deletion.
- [x] Unit and integration test strategy covers happy path and error paths.

## Domain Impact

**Actors/Roles**:
- Trưởng kho: approve/reject `QC_COMPLETED` receipts and create RTV for assigned warehouses only.
- Storekeeper: complete putaway, confirm rejected-goods handover, confirm RTV physical return.
- Kế toán viên: follows system-generated Debit Notes; does not manually create Debit Note in this flow.

**State Changes**:
- `QC_COMPLETED -> APPROVED` on manager approval.
- `APPROVED -> putaway completed` through `RECEIPT_PUTAWAY_COMPLETE`; status may remain `APPROVED` unless implementation introduces a separate putaway marker.
- `QC_COMPLETED -> RETURN_TO_SUPPLIER_PENDING -> RETURNED_TO_SUPPLIER` for rejected receipts.
- `QC_FAILED` remains `QC_FAILED` after RTV completion; RTV completion is represented by confirmed `RETURN_TO_VENDOR` adjustment.

**Inventory Impact**:
- Approve: no inventory increase.
- Reject: no inventory, batch, RTV, or Debit Note.
- Return-confirm for rejected receipt: no inventory mutation.
- Putaway complete: increase regular inventory by approved actual quantity at a non-quarantine Bin.
- RTV create: no quarantine deduction.
- RTV confirm: deduct full quarantined quantity only; reject partial/different quantities with HTTP 422.

**Audit Actions**:
- `RECEIPT_APPROVE`: before/after receipt status, approver, warehouse.
- `RECEIPT_REJECT`: before/after receipt status, rejection reason, actor, warehouse.
- `RECEIPT_RETURN_CONFIRM`: before/after receipt status, Storekeeper actor, timestamp, warehouse.
- `RECEIPT_PUTAWAY_COMPLETE`: bin/location assignment and inventory delta.
- `QUARANTINE_RTV_CREATE`: adjustment and Debit Note creation references, no inventory delta.
- `QUARANTINE_RTV_CONFIRM`: full quarantine deduction before/after.
- `INVENTORY_UPDATE`: before/after `total_qty`, `reserved_qty`, `location_id`, `version`.

**Security/Authorization**:
- JWT-authenticated user required.
- Trưởng kho actions require manager role and warehouse assignment for receipt warehouse.
- Storekeeper confirmation actions require Storekeeper role and warehouse assignment.
- Reject cross-warehouse attempts with HTTP 403.

**Accounting Impact**:
- RTV create generates `debit_notes` automatically for `QC_FAILED` receipts only.
- Rejecting `QC_COMPLETED` receipts does not create Debit Note.
- Debit Note links to supplier and receipt; no invoice/payment impact in this feature.

## Data Model / Migration Impact

- Entities/tables touched:
  - `receipts`, `receipt_items`, `batches`, `inventories`, `adjustments`, `debit_notes`, `warehouse_locations`, `audit_logs`
- New/changed columns or constraints:
  - `receipts.status` must include `RETURNED_TO_SUPPLIER`.
  - `receipts.version` is required for optimistic locking.
  - Existing `receipt_items.grade`, `receipt_items.serial_number`, `batches.expiry_date`, `batches.grade` must not be used by this feature; schema cleanup can be a separate migration if not already shared.
  - Need storage for return confirmation metadata if not represented only in audit logs. Preferred Sprint 1 path: audit log as source of confirmation actor/timestamp, no extra columns unless UI requires direct query fields.
- Flyway plan:
  - Existing `V7__align_receipt_status_with_inbound_flow.sql` adds receipt lifecycle statuses and should include `RETURNED_TO_SUPPLIER`.
  - Existing `V8__add_receipt_version.sql` adds optimistic locking.
  - Add a new migration only if implementation needs additional confirmation columns or missing schema for `debit_notes`/RTV constraints.
- Backfill/seed data:
  - Convert legacy `REJECTED` receipt status to `RETURN_TO_SUPPLIER_PENDING`.

## API / Contract Impact

- Endpoints added/changed:
  - `PUT /api/v1/receipts/{id}/approve`
  - `PUT /api/v1/receipts/{id}/reject`
  - `PUT /api/v1/receipts/{id}/return-to-supplier/confirm`
  - `PUT /api/v1/receipts/{id}/complete`
  - `POST /api/v1/receipts/{id}/rtv`
  - `PUT /api/v1/receipts/{id}/rtv/confirm`
- Request DTOs:
  - `ReceiptDecisionRequest`: `expectedVersion`, optional notes, required `rejectionReason` for reject.
  - `ReceiptReturnConfirmRequest`: `expectedVersion`, optional note/handover reference.
  - `ReceiptPutawayRequest`: `expectedVersion`, Bin id per line or target Bin id, quantity validation.
  - `ReceiptRtvCreateRequest`: `expectedVersion`, reason, document date/accounting period if required.
  - `ReceiptRtvConfirmRequest`: `expectedVersion`, `returnedQty` must equal full quarantined quantity.
- Response DTOs:
  - Receipt status response with `id`, `receiptNumber`, `status`, `version`, audit timestamp.
  - RTV response with adjustment id/number, Debit Note id/number, quarantine quantity.
- Error codes/statuses:
  - 400 validation failures
  - 403 warehouse scope/role failure
  - 404 receipt/adjustment/bin not found
  - 409 duplicate final decision, duplicate RTV, stale version, already confirmed
  - 422 invalid state, RTV quantity mismatch, non-regular Bin, bin over capacity, inventory invariant violation
- OpenAPI path/schema updates:
  - Add all endpoint contracts in [contracts/openapi.yaml](./contracts/openapi.yaml) and mirror annotations in controller.

## Test Strategy

- Service unit tests:
  - Approval unlocks putaway and does not increase inventory.
  - Reject moves to `RETURN_TO_SUPPLIER_PENDING` without batch/inventory/RTV/Debit Note.
  - Return-confirm moves to `RETURNED_TO_SUPPLIER`.
  - RTV create creates pending adjustment + Debit Note without quarantine deduction.
  - RTV confirm requires full quantity and deducts quarantine only once.
  - Authorization, invalid state, duplicate action, stale version.
- Repository/query tests:
  - Load receipt with items, warehouse, supplier; find existing RTV by receipt reference.
  - Inventory row locking/version behavior if custom queries are introduced.
- Controller/API integration tests:
  - Happy paths and error paths for all changed endpoints.
  - Validate HTTP 403/409/422 responses.
- Frontend tests:
  - Receipt approval modal, reject flow, return-confirm action, quarantine RTV create/confirm full quantity validation.
- Regression tests:
  - No negative inventory, no available inventory from quarantine, audit logs for all warehouse mutations.

## Project Structure

### Documentation

```text
.sdd/specs/003-inbound-receipt-qc/
├── spec.md
├── features/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/openapi.yaml
└── tasks.md
```

### Source Code

```text
backend/src/main/java/com/wms/
├── controller/ReceiptController.java
├── dto/request/ReceiptDecisionRequest.java
├── dto/request/ReceiptPutawayRequest.java
├── dto/request/ReceiptReturnConfirmRequest.java
├── dto/request/ReceiptRtvCreateRequest.java
├── dto/request/ReceiptRtvConfirmRequest.java
├── dto/response/ReceiptResponse.java
├── dto/response/ReceiptDecisionResponse.java
├── entity/Receipt.java
├── entity/ReceiptItem.java
├── entity/Adjustment.java
├── entity/DebitNote.java
├── enums/ReceiptStatus.java
├── repository/ReceiptRepository.java
├── repository/ReceiptItemRepository.java
├── repository/AdjustmentRepository.java
├── repository/DebitNoteRepository.java
├── repository/InventoryRepository.java
├── service/ReceiptService.java
└── service/impl/ReceiptServiceImpl.java

backend/src/main/resources/db/migration/
frontend/src/pages/Inbound/
frontend/src/services/inbound.service.js
```

**Structure Decision**: Move receipt business logic out of demo-style `ReceiptService` methods into a transactional service implementation or make the current service fully repository-backed. Add a dedicated `ReceiptController` for `/api/v1/receipts/*` endpoints. Keep API validation in DTOs and business invariants in service methods.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| None | N/A | N/A |

## Speckit Execution Note

`pwsh` is not installed in this environment, so `.specify/scripts/powershell/setup-plan.ps1 -Json` could not be executed. This plan was generated manually from the Speckit template and placed in the same path the script would target via `SPECIFY_FEATURE_DIRECTORY=.sdd/specs/003-inbound-receipt-qc`.
