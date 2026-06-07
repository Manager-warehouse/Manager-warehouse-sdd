# Implementation Plan: Accountant Partner & Credit Limit Management

**Branch**: `ha-001-audit-logging` | **Date**: 2026-06-05 | **Spec**: `spec.md`

**Input**: Feature specification from `.sdd/specs/002-master-data-management/features/feature-accountant-partners/spec.md`

## Summary

Implement Dealer and Supplier master-data management for accountants, accountant-manager-only credit controls for dealers, and Delivery Order CRUD based on the existing `delivery_orders` schema. Implementation must follow the existing `Dealer`, `Supplier`, `DeliveryOrder`, and `DeliveryOrderItem` entities and Flyway schema, expose REST APIs under `/api/v1/dealers`, `/api/v1/suppliers`, and `/api/v1/delivery-orders`, enforce RBAC and Jakarta Validation at the controller/service boundary, reject Delivery Order creation for inactive dealers, and record field-level audit logs for every successful partner create/update/credit/status/deactivation/reactivation action.

## Technical Context

**Language/Version**: Java 21 / Spring Boot 3.4.5
**Primary Dependencies**: Spring Web, Spring Data JPA, Hibernate, Jakarta Validation, Spring Security, Lombok, springdoc OpenAPI
**Storage**: PostgreSQL 18 through JPA/Hibernate only
**Testing**: JUnit 5, Mockito, Spring MVC integration tests
**Target Platform**: Backend REST API
**Project Type**: Full-stack WMS; this feature is backend API and service logic first
**Performance Goals**: Partner list endpoints must support pagination/filtering for operational use; credit checks must run inside the transaction that attempts to create the dealer transaction
**Constraints**: No raw SQL, no physical delete of business records, no bypass of API validation, audit logging mandatory, RBAC required for accountant-manager credit controls
**Scale/Scope**: Sprint 1 master-data feature for 50+ dealers, supplier catalog, supplier received-order viewing, and Delivery Order CRUD.

## Entity and Schema Alignment

Use the existing backend entities and migration schema as the implementation source of truth.

**Dealer fields**: `id`, `code`, `name`, `phone`, `defaultDeliveryAddress`, `region`, `paymentTermDays`, `creditLimit`, `currentBalance`, `creditStatus`, `isActive`, `createdBy`, `updatedBy`, `createdAt`, `updatedAt`.

**Dealer table columns**: `id`, `code`, `name`, `phone`, `default_delivery_address`, `region`, `payment_term_days`, `credit_limit`, `current_balance`, `credit_status`, `is_active`, `created_by`, `updated_by`, `created_at`, `updated_at`.

**Supplier fields**: `id`, `code`, `companyName`, `taxCode`, `phone`, `contactPerson`, `address`, `isActive`, `createdBy`, `updatedBy`, `createdAt`, `updatedAt`.

**Supplier table columns**: `id`, `code`, `company_name`, `tax_code`, `phone`, `contact_person`, `address`, `is_active`, `created_by`, `updated_by`, `created_at`, `updated_at`.

Do not introduce dealer fields such as `companyName`, `taxCode`, `address`, or `contactPerson` unless the entity and migration are intentionally changed in a separate schema task.

Dealer create/profile DTOs must not expose manager-owned credit fields. On create, initialize `paymentTermDays` from `SystemConfigKey.DEFAULT_PAYMENT_TERM_DAYS`, `creditLimit` from `SystemConfigKey.DEFAULT_CREDIT_LIMIT`, `currentBalance = 0`, `creditStatus = ACTIVE`, and `isActive = true` so the existing NOT NULL entity/table fields are satisfied.

**DeliveryOrder fields**: `id`, `doNumber`, `dealer`, `warehouse`, `type`, `expectedDeliveryDate`, `status`, `createdBy`, `cancelReason`, `documentDate`, `accountingPeriod`, `notes`, `createdAt`, `updatedAt`, `packedBy`, `qcBy`.

**DeliveryOrder table columns**: `id`, `do_number`, `dealer_id`, `warehouse_id`, `type`, `expected_delivery_date`, `status`, `created_by`, `cancel_reason`, `document_date`, `accounting_period_id`, `notes`, `packed_by`, `qc_by`, `created_at`, `updated_at`.

Delivery Order create must generate `doNumber`, set `status = NEW`, set `createdBy` from the authenticated user who performs the create action, and reject creation if the referenced dealer is inactive. Actor fields for partner changes, Delivery Order creation, and audit logs must be resolved from the current authenticated user, not from request body input. Inbound receipt creation is handled by another integration/source and is not blocked or created in this feature.

Supplier received-order viewing uses existing `receipts` records as the source of truth for orders/shipments already received from a supplier.

## Constitution Check

*GATE: Passed before Phase 0 research.*

- Layered architecture: Controllers only handle HTTP/DTO validation; services own partner, credit, and inactive-account business rules; repositories remain Spring Data JPA only.
- Inventory integrity: This feature does not mutate inventory directly. It defines inactive dealer guards that Delivery Order and finance transaction services must call before creating new outbound/dealer records. Inbound receipt creation is handled by another source and is not created or blocked in this feature.
- Auth & RBAC: `ACCOUNTANT` can manage partner profiles; only `ACCOUNTANT_MANAGER` can update dealer credit limit, payment term, and manual credit status.
- Test coverage: Service logic for credit checks, inactive blocking, deactivation guards, and audit logging requires unit tests; all endpoints require integration tests.
- Audit logging: Mandatory for successful create, update, credit-limit change, payment-term change, credit-status change, soft delete, and reactivation.

## Project Structure

### Documentation (this feature)

```text
.sdd/specs/002-master-data-management/features/feature-accountant-partners/
|-- spec.md
|-- feature-accountant-partners.md
|-- plan.md
|-- research.md
|-- data-model.md
|-- quickstart.md
`-- contracts/
    `-- openapi.yaml
```

### Source Code (repository root)

```text
backend/
|-- src/main/java/com/wms/controller/
|   |-- DealerController.java
|   `-- SupplierController.java
|-- src/main/java/com/wms/dto/request/
|   |-- DealerCreateRequest.java
|   |-- DealerUpdateRequest.java
|   |-- DealerCreditLimitUpdateRequest.java
|   |-- DealerPaymentTermUpdateRequest.java
|   |-- DealerCreditStatusUpdateRequest.java
|   |-- SupplierCreateRequest.java
|   `-- SupplierUpdateRequest.java
|-- src/main/java/com/wms/dto/response/
|   |-- DealerResponse.java
|   `-- SupplierResponse.java
|-- src/main/java/com/wms/entity/
|   |-- Dealer.java
|   `-- Supplier.java
|-- src/main/java/com/wms/repository/
|   |-- DealerRepository.java
|   `-- SupplierRepository.java
|-- src/main/java/com/wms/service/
|   |-- DealerService.java
|   |-- PartnerEligibilityService.java
|   `-- SupplierService.java
|-- src/main/java/com/wms/service/impl/
|   |-- DealerServiceImpl.java
|   |-- PartnerEligibilityServiceImpl.java
|   `-- SupplierServiceImpl.java
`-- src/test/java/com/wms/
    |-- controller/
    |   |-- DealerControllerTest.java
    |   `-- SupplierControllerTest.java
    `-- service/
        |-- DealerServiceTest.java
        |-- PartnerEligibilityServiceTest.java
        `-- SupplierServiceTest.java
```

**Structure Decision**: Use the existing Spring Boot layered architecture under `backend/src/main/java/com/wms`. Keep partner eligibility checks in a small service so receipt, delivery, finance, and transaction flows can block inactive partners without duplicating rules.

## Complexity Tracking

No justified constitution violations.

## Phase 0 Research

Completed in `research.md`. All technical context items are resolved from project stack, constitution, AGENTS rules, and the existing backend package layout.

## Phase 1 Design

Completed artifacts:

- `data-model.md`: Dealer, Supplier, request DTO, response DTO, audit, and downstream eligibility entities/rules.
- Existing entity/schema alignment above overrides any earlier generic field wording in `data-model.md`.
- `contracts/openapi.yaml`: REST endpoint contract for dealer/supplier profile, credit, payment-term, status, delete, and reactivate operations.
- `quickstart.md`: Implementation and verification steps for backend developers.

## Post-Design Constitution Check

*GATE: Passed after Phase 1 design.*

- No raw SQL or direct inventory mutation introduced.
- Soft-delete is represented by `is_active = false`.
- Transaction history remains immutable; downstream operations must reject new references rather than alter historical records.
- API validation, RBAC, centralized errors, OpenAPI contract, and audit logging are explicitly covered.
