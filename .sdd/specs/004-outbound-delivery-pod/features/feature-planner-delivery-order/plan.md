# Implementation Plan: Planner Delivery Order Fleet Capacity Guard

**Branch**: `fix/update_return_flow` | **Date**: 2026-08-03 | **Spec**: [feature-planner-delivery-order.md](./feature-planner-delivery-order.md)

**Input**: Feature specification from `.sdd/specs/004-outbound-delivery-pod/features/feature-planner-delivery-order/feature-planner-delivery-order.md`

## Summary

Prevent Planner from creating or updating a `NEW` Delivery Order whose calculated goods weight cannot fit in one coordinated delivery wave even when every active vehicle assigned to the selected warehouse is counted. The guard runs before persistence or reservation mutation and returns a clear instruction to split the demand into multiple Delivery Orders.

## Technical Context

**Language/Version**: Java 21 / Spring Boot 3.4.5; React 18 + JavaScript when UI is touched

**Primary Dependencies**: Spring Web, Spring Data JPA, Hibernate, Spring Security, Lombok, Springdoc OpenAPI, React, Tailwind CSS

**Storage**: PostgreSQL 18 via Flyway migrations and Spring Data JPA

**Testing**: JUnit 5 + Mockito for backend; Spring integration tests for APIs; Jest + React Testing Library for frontend business UI

**Target Platform**: Full-stack WMS web application and REST API

**Project Type**: Backend + frontend web application

**Performance Goals**: Fleet-capacity validation completes within the existing create/update response target and adds at most one aggregate fleet lookup per request.

**Constraints**: Must preserve WMS invariants: no negative inventory, QC gates, audit logs, role + warehouse authorization, no raw SQL in application code

**Scale/Scope**: 3 physical warehouses, In-Transit warehouse, 1000+ products, 50+ dealers, 1000+ transactions/month

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] Layered architecture preserved: Controller -> Service -> Repository -> Entity.
- [x] Write endpoints use existing request DTOs with Jakarta Validation.
- [x] Service methods own fleet-capacity business rules and transaction ordering.
- [x] All DB access goes through Spring Data JPA/Hibernate; no raw SQL in application code.
- [x] Inventory invariants are preserved because rejection occurs before reservation mutation.
- [x] QC/quarantine/transfer/accounting state rules are unchanged.
- [x] Existing successful create/update audit actions remain unchanged; rejected requests write no success audit.
- [x] OpenAPI/Swagger impact is identified for changed 422 responses.
- [x] No Flyway migration is required because existing Product and Vehicle fields are reused.
- [x] Unit, controller, and frontend test strategy covers boundary and error paths.

## Domain Impact

**Actors/Roles**: Planner creating or updating a Delivery Order for an assigned warehouse.

**State Changes**: Successful create remains `null -> NEW`; successful update remains `NEW -> NEW`; rejected requests create no state transition.

**Inventory Impact**: Rejected requests must not mutate `warehouse_product_reservations`, `delivery_order_items.reserved_qty`, or concrete inventory.

**Audit Actions**: Existing `DELIVERY_ORDER_CREATE` and `DELIVERY_ORDER_UPDATE` apply only after validation succeeds. No success audit is written for capacity rejection.

**Security/Authorization**: Existing JWT Planner role and warehouse-assignment checks remain mandatory before warehouse fleet data is used.

**Accounting Impact**: None; existing credit and accounting-period validation remains unchanged.

## Data Model / Migration Impact

- Entities/tables read: `products.weight_kg`, `vehicles.warehouse_id`, `vehicles.max_weight_kg`, `vehicles.is_active`.
- Entities/tables mutated: existing Delivery Order and reservation entities only after all validations pass.
- New/changed columns or constraints: none.
- Flyway plan: no migration.
- Backfill/seed data: none; products without a valid positive weight are rejected with `PRODUCT_WEIGHT_MISSING`.

## API / Contract Impact

- Endpoints changed: `POST /api/v1/delivery-orders`, `PUT /api/v1/delivery-orders/{id}`.
- Request DTOs: existing create/update item quantities; no new request fields.
- Response DTOs: unchanged.
- Error codes/statuses: `422 DELIVERY_ORDER_EXCEEDS_WAREHOUSE_FLEET_CAPACITY`, `422 PRODUCT_WEIGHT_MISSING`.
- OpenAPI path/schema updates: document fleet-capacity and missing-weight rejection on create/update responses.

## Test Strategy

- Service unit tests: exceeds total fleet capacity, equals capacity, busy/maintenance vehicles included, inactive/other-warehouse vehicles excluded, missing product weight, update revalidation, and no persistence/reservation/audit on rejection.
- Repository/query tests: active warehouse fleet lookup or aggregate behavior.
- Controller/API tests: create and update return HTTP 422 with stable error code/message.
- Frontend tests: Planner receives and displays the backend business message on create failure.
- Regression tests for invariants: existing credit, stock, reservation, audit, and warehouse-scope tests remain passing.

## Project Structure

### Documentation

```text
.sdd/specs/[###-feature]/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
└── tasks.md
```

### Source Code

```text
backend/src/main/java/com/wms/
├── controller/
├── dto/request/
├── dto/response/
├── entity/
├── enums/
├── exception/
├── mapper/
├── repository/
├── service/
└── service/impl/

backend/src/main/resources/db/migration/

backend/src/test/java/com/wms/

frontend/src/
├── components/
├── hooks/
├── pages/
├── services/
├── stores/
└── utils/
```

**Structure Decision**: Extend `DeliveryOrderServiceImpl` validation using `VehicleRepository`; reuse Product weight and Vehicle maximum payload fields; update `DeliveryOrderServiceImplTest`, `DeliveryOrderControllerTest`, Planner create UI tests, and the existing OpenAPI contract.

## Complexity Tracking

> Fill only if a constitution gate is violated and justify why the simpler path is not viable.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| None | N/A | N/A |
