# Tasks: Accountant Partner & Credit Limit Management

**Input**: Design documents from `.sdd/specs/002-master-data-management/features/feature-accountant-partners/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/openapi.yaml`, `quickstart.md`

**Tests**: Required by `AGENTS.md`, constitution, and `quickstart.md`: service unit tests, controller integration tests, and endpoint/API documentation verification.

**Organization**: Tasks are grouped by independently testable user story. All user stories are P1 for Sprint 1 master-data and credit-control delivery.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel after its phase dependencies are satisfied.
- **[Story]**: User story label for traceability.
- Every task includes an exact target file path.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirm project structure, branch-gated automation context, and API contract baseline before implementation.

- [ ] T001 Run GitNexus impact analysis for Dealer, Supplier, AuditLog, Receipt, DeliveryOrder, and finance-related partner symbols and record any HIGH/CRITICAL warnings in `.sdd/specs/002-master-data-management/features/feature-accountant-partners/tasks.md`
- [ ] T002 Verify the active feature directory pin in `.specify/feature.json`
- [ ] T003 [P] Compare the generated REST contract against requested endpoints in `.sdd/specs/002-master-data-management/features/feature-accountant-partners/contracts/openapi.yaml`
- [ ] T004 [P] Confirm current backend package layout and existing partner entities in `backend/src/main/java/com/wms/entity/Dealer.java`
- [ ] T005 [P] Confirm current backend package layout and existing supplier entity in `backend/src/main/java/com/wms/entity/Supplier.java`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared entity/schema verification, repositories, DTOs, mapper contracts, exceptions, RBAC helpers, and audit utilities required before user stories.

**CRITICAL**: No user story work can begin until this phase is complete.

- [ ] T006 Verify dealer implementation uses existing fields `code`, `name`, `phone`, `defaultDeliveryAddress`, `region`, `paymentTermDays`, `creditLimit`, `currentBalance`, `creditStatus`, and `isActive` in `backend/src/main/java/com/wms/entity/Dealer.java`
- [ ] T007 Verify supplier implementation uses existing fields `code`, `companyName`, `taxCode`, `phone`, `contactPerson`, `address`, and `isActive` in `backend/src/main/java/com/wms/entity/Supplier.java`
- [ ] T008 [P] Add dealer Spring Data repository with code lookup and uniqueness checks in `backend/src/main/java/com/wms/repository/DealerRepository.java`
- [ ] T009 [P] Add supplier Spring Data repository with code lookup and uniqueness checks in `backend/src/main/java/com/wms/repository/SupplierRepository.java`
- [ ] T010 [P] Add dealer create DTO using only existing profile fields `code`, `name`, `phone`, `defaultDeliveryAddress`, and `region` with Jakarta Validation in `backend/src/main/java/com/wms/dto/request/DealerCreateRequest.java`
- [ ] T011 [P] Add dealer profile update DTO using existing dealer profile fields `name`, `phone`, `defaultDeliveryAddress`, and `region` in `backend/src/main/java/com/wms/dto/request/DealerUpdateRequest.java`
- [ ] T012 [P] Add dealer credit-limit DTO in `backend/src/main/java/com/wms/dto/request/DealerCreditLimitUpdateRequest.java`
- [ ] T013 [P] Add dealer payment-term DTO in `backend/src/main/java/com/wms/dto/request/DealerPaymentTermUpdateRequest.java`
- [ ] T014 [P] Add dealer credit-status DTO in `backend/src/main/java/com/wms/dto/request/DealerCreditStatusUpdateRequest.java`
- [ ] T015 [P] Add supplier create DTO with optional non-unique tax code in `backend/src/main/java/com/wms/dto/request/SupplierCreateRequest.java`
- [ ] T016 [P] Add supplier update DTO with optional non-unique tax code in `backend/src/main/java/com/wms/dto/request/SupplierUpdateRequest.java`
- [ ] T017 [P] Add dealer response DTO using existing dealer fields and credit fields in `backend/src/main/java/com/wms/dto/response/DealerResponse.java`
- [ ] T018 [P] Add supplier response DTO in `backend/src/main/java/com/wms/dto/response/SupplierResponse.java`
- [ ] T019 [P] Add dealer mapper methods in `backend/src/main/java/com/wms/mapper/DealerMapper.java`
- [ ] T020 [P] Add supplier mapper methods in `backend/src/main/java/com/wms/mapper/SupplierMapper.java`
- [ ] T021 Add duplicate-code conflict exception handling in `backend/src/main/java/com/wms/exception/DuplicateResourceException.java`
- [ ] T022 Add business-rule violation exception handling in `backend/src/main/java/com/wms/exception/BusinessRuleViolationException.java`
- [ ] T023 Create centralized HTTP status mapping with `{code, message, details, timestamp}` response format for validation, authorization, not-found, and conflict cases in `backend/src/main/java/com/wms/exception/GlobalExceptionHandler.java`
- [ ] T024 Extend audit enums or constants for partner actions and entity types in `backend/src/main/java/com/wms/enums/AuditAction.java`
- [ ] T025 Extend audit entity types for Dealer and Supplier if required in `backend/src/main/java/com/wms/enums/AuditEntityType.java`
- [ ] T026 Add reusable field-level partner audit helper that receives the current authenticated user performing the feature action as actor in `backend/src/main/java/com/wms/util/PartnerAuditUtil.java`
- [ ] T027 Define service interfaces for dealers, suppliers, and partner eligibility in `backend/src/main/java/com/wms/service/DealerService.java`
- [ ] T028 Define supplier service interface in `backend/src/main/java/com/wms/service/SupplierService.java`
- [ ] T029 Define partner eligibility service interface in `backend/src/main/java/com/wms/service/PartnerEligibilityService.java`

**Checkpoint**: Foundation ready. User story implementation can now begin in parallel.

---

## Phase 3: User Story 1 - Accountant Manages Dealer Profiles (Priority: P1) MVP

**Goal**: `ACCOUNTANT` can create, view, update, soft-delete, and reactivate dealers without touching manager-only credit controls through the generic profile update path.

**Independent Test**: Using an authenticated `ACCOUNTANT`, create a dealer with `code`, `name`, `phone`, `defaultDeliveryAddress`, and `region`, update those profile fields, list/detail it, deactivate it by setting `is_active = false`, reactivate it, and verify each successful mutation creates field-level audit data.

### Tests for User Story 1

- [ ] T030 [P] [US1] Add dealer service unit tests for create/update/list/detail/deactivate/reactivate and audit logging in `backend/src/test/java/com/wms/service/DealerServiceTest.java`
- [ ] T031 [P] [US1] Add dealer controller integration tests for profile endpoints, validation failures, not found, and authorization in `backend/src/test/java/com/wms/controller/DealerControllerTest.java`

### Implementation for User Story 1

- [ ] T032 [US1] Implement dealer profile create, update, list, detail, deactivate, and reactivate service methods using existing dealer fields in `backend/src/main/java/com/wms/service/impl/DealerServiceImpl.java`
- [ ] T033 [US1] Initialize dealer create defaults from `DEFAULT_PAYMENT_TERM_DAYS`, `DEFAULT_CREDIT_LIMIT`, `currentBalance = 0`, `creditStatus = ACTIVE`, and `isActive = true` in `backend/src/main/java/com/wms/service/impl/DealerServiceImpl.java`
- [ ] T034 [US1] Ensure normal dealer profile update rejects or ignores manager-only credit fields in `backend/src/main/java/com/wms/service/impl/DealerServiceImpl.java`
- [ ] T035 [US1] Add dealer REST endpoints under `/api/v1/dealers` with `@Valid` request validation in `backend/src/main/java/com/wms/controller/DealerController.java`
- [ ] T036 [US1] Add `ACCOUNTANT` RBAC annotations for dealer profile mutation endpoints in `backend/src/main/java/com/wms/controller/DealerController.java`
- [ ] T037 [US1] Add field-level audit logging for dealer profile create/update/deactivate/reactivate in `backend/src/main/java/com/wms/service/impl/DealerServiceImpl.java`
- [ ] T038 [US1] Add OpenAPI annotations for dealer profile endpoints in `backend/src/main/java/com/wms/controller/DealerController.java`

**Checkpoint**: Dealer profile management is independently usable and testable.

---

## Phase 4: User Story 2 - Accountant Manages Supplier Profiles (Priority: P1)

**Goal**: `ACCOUNTANT` can create, view, update, soft-delete, and reactivate suppliers while preserving optional non-unique `tax_code` and enforcing unique supplier `code`.

**Independent Test**: Using an authenticated `ACCOUNTANT`, create a supplier, reject duplicate `code`, update identity/contact fields, deactivate/reactivate it, and verify audit entries with before/after changed fields.

### Tests for User Story 2

- [ ] T039 [P] [US2] Add supplier service unit tests for create/update/list/detail/deactivate/reactivate, duplicate code, optional non-unique tax code, and audit logging in `backend/src/test/java/com/wms/service/SupplierServiceTest.java`
- [ ] T040 [P] [US2] Add supplier controller integration tests for profile endpoints, duplicate code, validation failures, not found, and authorization in `backend/src/test/java/com/wms/controller/SupplierControllerTest.java`

### Implementation for User Story 2

- [ ] T041 [US2] Implement supplier profile create, update, list, detail, deactivate, and reactivate service methods in `backend/src/main/java/com/wms/service/impl/SupplierServiceImpl.java`
- [ ] T042 [US2] Enforce supplier code uniqueness without enforcing tax-code uniqueness in `backend/src/main/java/com/wms/service/impl/SupplierServiceImpl.java`
- [ ] T043 [US2] Add supplier REST endpoints under `/api/v1/suppliers` with `@Valid` request validation in `backend/src/main/java/com/wms/controller/SupplierController.java`
- [ ] T044 [US2] Add `ACCOUNTANT` RBAC annotations for supplier profile mutation endpoints in `backend/src/main/java/com/wms/controller/SupplierController.java`
- [ ] T045 [US2] Add field-level audit logging for supplier profile create/update/deactivate/reactivate in `backend/src/main/java/com/wms/service/impl/SupplierServiceImpl.java`
- [ ] T046 [US2] Add OpenAPI annotations for supplier profile endpoints in `backend/src/main/java/com/wms/controller/SupplierController.java`

**Checkpoint**: Supplier profile management is independently usable and testable.

---

## Phase 5: User Story 3 - Accountant Manager Controls Dealer Credit (Priority: P1)

**Goal**: Only `ACCOUNTANT_MANAGER` can update dealer `credit_limit`, `payment_term_days`, and manual `credit_status`; dealer transaction creation can check limits and automatically place dealers on `CREDIT_HOLD` when a new transaction would exceed credit.

**Independent Test**: With a dealer at `current_balance = 480M` and `credit_limit = 500M`, verify `30M` is rejected with automatic `CREDIT_HOLD`, `20M` is allowed, invalid limits and payment terms are rejected, and non-manager users receive authorization errors.

### Tests for User Story 3

- [ ] T047 [P] [US3] Add dealer credit service unit tests for valid limit change, limit below current balance, non-positive limit, Net 30/60 validation, invalid payment term, manual credit status, and audit logging in `backend/src/test/java/com/wms/service/DealerCreditServiceTest.java`
- [ ] T048 [P] [US3] Add dealer credit controller integration tests for `/credit-limit`, `/payment-term`, `/credit-status`, validation errors, and unauthorized access in `backend/src/test/java/com/wms/controller/DealerCreditControllerTest.java`
- [ ] T049 [P] [US3] Add dealer transaction credit-check unit tests for exact-limit allowance, exceeded-limit rejection, automatic credit hold, and no transaction persistence on rejection in `backend/src/test/java/com/wms/service/DealerTransactionCreditPolicyTest.java`

### Implementation for User Story 3

- [ ] T050 [US3] Implement credit-limit, payment-term, and credit-status update methods in `backend/src/main/java/com/wms/service/impl/DealerServiceImpl.java`
- [ ] T051 [US3] Enforce `credit_limit > 0` and `credit_limit > current_balance` in `backend/src/main/java/com/wms/service/impl/DealerServiceImpl.java`
- [ ] T052 [US3] Enforce `payment_term_days` values of only `30` or `60` in `backend/src/main/java/com/wms/service/impl/DealerServiceImpl.java`
- [ ] T053 [US3] Implement dealer transaction credit policy method for `current_balance + transaction_amount <= credit_limit` in `backend/src/main/java/com/wms/service/impl/DealerServiceImpl.java`
- [ ] T054 [US3] Set `credit_status = CREDIT_HOLD` and audit the change when a new dealer transaction would exceed the credit limit in `backend/src/main/java/com/wms/service/impl/DealerServiceImpl.java`
- [ ] T055 [US3] Add manager-only credit endpoints under `/api/v1/dealers/{id}` in `backend/src/main/java/com/wms/controller/DealerController.java`
- [ ] T056 [US3] Add `ACCOUNTANT_MANAGER` RBAC annotations for dealer credit, payment-term, and credit-status endpoints in `backend/src/main/java/com/wms/controller/DealerController.java`
- [ ] T057 [US3] Add OpenAPI annotations for dealer credit-control endpoints in `backend/src/main/java/com/wms/controller/DealerController.java`

**Checkpoint**: Dealer credit controls and transaction credit policy are independently usable and testable.

---

## Phase 6: User Story 4 - Delivery Order CRUD With Inactive Dealer Guard (Priority: P1)

**Goal**: Implement full CRUD for `delivery_orders` using the existing `DeliveryOrder` and `DeliveryOrderItem` entities, and reject creation when the referenced dealer is inactive.

**Independent Test**: Create, list, view, update, and cancel a Delivery Order; then mark a dealer inactive and verify Delivery Order creation for that dealer is rejected before persistence.

### Tests for User Story 4

- [ ] T058 [P] [US4] Add delivery order service unit tests for create/list/detail/update/cancel in `backend/src/test/java/com/wms/service/DeliveryOrderServiceTest.java`
- [ ] T059 [P] [US4] Add partner eligibility unit tests for active dealer allowance and inactive dealer rejection in `backend/src/test/java/com/wms/service/PartnerEligibilityServiceTest.java`
- [ ] T060 [P] [US4] Add delivery order controller integration tests for CRUD endpoints and validation errors in `backend/src/test/java/com/wms/controller/DeliveryOrderControllerTest.java`
- [ ] T061 [P] [US4] Add delivery-order inactive dealer integration test in `backend/src/test/java/com/wms/controller/DeliveryOrderControllerTest.java`

### Implementation for User Story 4

- [ ] T062 [P] [US4] Add delivery order create DTO matching `delivery_orders` and `delivery_order_items` input fields in `backend/src/main/java/com/wms/dto/request/DeliveryOrderCreateRequest.java`
- [ ] T063 [P] [US4] Add delivery order item create DTO in `backend/src/main/java/com/wms/dto/request/DeliveryOrderItemCreateRequest.java`
- [ ] T064 [P] [US4] Add delivery order update DTO for editable header fields in `backend/src/main/java/com/wms/dto/request/DeliveryOrderUpdateRequest.java`
- [ ] T065 [P] [US4] Add delivery order response DTO in `backend/src/main/java/com/wms/dto/response/DeliveryOrderResponse.java`
- [ ] T066 [P] [US4] Add delivery order item response DTO in `backend/src/main/java/com/wms/dto/response/DeliveryOrderItemResponse.java`
- [ ] T067 [P] [US4] Add delivery order repository with dealer/status lookup methods in `backend/src/main/java/com/wms/repository/DeliveryOrderRepository.java`
- [ ] T068 [P] [US4] Add delivery order item repository in `backend/src/main/java/com/wms/repository/DeliveryOrderItemRepository.java`
- [ ] T069 [US4] Define delivery order service interface in `backend/src/main/java/com/wms/service/DeliveryOrderService.java`
- [ ] T070 [US4] Implement dealer active-state guard method in `backend/src/main/java/com/wms/service/impl/PartnerEligibilityServiceImpl.java`
- [ ] T071 [US4] Implement delivery order create/list/detail/update/cancel methods in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [ ] T072 [US4] Generate `doNumber`, set `status = NEW`, set `createdBy` from the current authenticated user performing the create action, and set timestamps during Delivery Order creation in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [ ] T073 [US4] Call inactive dealer guard before saving new Delivery Orders in `backend/src/main/java/com/wms/service/impl/DeliveryOrderServiceImpl.java`
- [ ] T074 [US4] Implement Delivery Order REST CRUD endpoints under `/api/v1/delivery-orders` in `backend/src/main/java/com/wms/controller/DeliveryOrderController.java`
- [ ] T075 [US4] Add role and validation annotations for Delivery Order endpoints in `backend/src/main/java/com/wms/controller/DeliveryOrderController.java`
- [ ] T076 [US4] Add OpenAPI annotations for Delivery Order endpoints in `backend/src/main/java/com/wms/controller/DeliveryOrderController.java`

**Checkpoint**: Delivery Order CRUD is independently usable and inactive dealers cannot be used for new Delivery Orders.

---

## Phase 7: User Story 5 - View Supplier Received Orders (Priority: P1)

**Goal**: `ACCOUNTANT` can view read-only orders/receipts already received from a supplier and inspect details without creating inbound documents.

**Independent Test**: Given a supplier with historical received orders/receipts, list received orders for that supplier and view one detail record.

### Tests for User Story 5

- [ ] T077 [P] [US5] Add supplier received-order service unit tests in `backend/src/test/java/com/wms/service/SupplierReceivedOrderServiceTest.java`
- [ ] T078 [P] [US5] Add supplier received-order controller integration tests in `backend/src/test/java/com/wms/controller/SupplierReceivedOrderControllerTest.java`

### Implementation for User Story 5

- [ ] T079 [P] [US5] Add supplier received-order list response DTO in `backend/src/main/java/com/wms/dto/response/SupplierReceivedOrderResponse.java`
- [ ] T080 [P] [US5] Add supplier received-order detail response DTO in `backend/src/main/java/com/wms/dto/response/SupplierReceivedOrderDetailResponse.java`
- [ ] T081 [US5] Use `receipts` as the source table for supplier received-order listing and detail lookup in `backend/src/main/java/com/wms/entity/Receipt.java`
- [ ] T082 [US5] Add repository query methods for supplier received-order listing and detail lookup in `backend/src/main/java/com/wms/repository/ReceiptRepository.java`
- [ ] T083 [US5] Implement supplier received-order read service in `backend/src/main/java/com/wms/service/impl/SupplierServiceImpl.java`
- [ ] T084 [US5] Add read-only supplier received-order endpoints under `/api/v1/suppliers/{id}/received-orders` in `backend/src/main/java/com/wms/controller/SupplierController.java`
- [ ] T085 [US5] Add OpenAPI annotations for supplier received-order endpoints in `backend/src/main/java/com/wms/controller/SupplierController.java`

**Checkpoint**: Supplier profile management can show received order history without creating inbound receipt records.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Documentation, validation hardening, regression checks, and final verification.

- [ ] T086 [P] Update implementation notes for partner and delivery-order APIs in `.sdd/specs/002-master-data-management/features/feature-accountant-partners/quickstart.md`
- [ ] T087 [P] Ensure generated Swagger/OpenAPI output matches `contracts/openapi.yaml` endpoint coverage in `backend/src/main/java/com/wms/controller/DealerController.java`
- [ ] T088 [P] Ensure generated Swagger/OpenAPI output matches `contracts/openapi.yaml` endpoint coverage in `backend/src/main/java/com/wms/controller/SupplierController.java`
- [ ] T089 [P] Ensure generated Swagger/OpenAPI output matches `contracts/openapi.yaml` endpoint coverage in `backend/src/main/java/com/wms/controller/DeliveryOrderController.java`
- [ ] T090 Run backend unit and integration tests from `backend/pom.xml`
- [ ] T091 Run backend compile/lint verification from `backend/pom.xml`
- [ ] T092 Run `gitnexus_detect_changes()` before commit and record changed-symbol summary in `.sdd/specs/002-master-data-management/features/feature-accountant-partners/tasks.md`
- [ ] T093 Review task completion against DoD checklist and record any verification gaps in `.sdd/specs/002-master-data-management/features/feature-accountant-partners/tasks.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 Setup**: No dependencies.
- **Phase 2 Foundational**: Depends on Phase 1 and blocks all user stories.
- **Phase 3 US1**: Depends on Phase 2; MVP scope.
- **Phase 4 US2**: Depends on Phase 2; can run in parallel with US1 after shared DTO/exception/repository foundation is complete.
- **Phase 5 US3**: Depends on Phase 2 and benefits from US1 dealer service/controller scaffolding.
- **Phase 6 US4**: Depends on Phase 2 and integrates with US1 dealer active-state rules.
- **Phase 7 US5**: Depends on Phase 2 and US2 supplier repository/service/controller scaffolding.
- **Phase 8 Polish**: Depends on implemented stories selected for release.

### User Story Dependencies

- **US1 Dealer profile management**: Earliest MVP after foundation.
- **US2 Supplier profile management**: Independent after foundation.
- **US3 Dealer credit controls**: Can start after foundation, but final endpoint wiring is simpler after US1 controller/service scaffolding.
- **US4 Delivery Order CRUD**: Can start after foundation; create flow depends on dealer active-state guard.
- **US5 Supplier received-order viewing**: Can start after supplier scaffolding and uses `receipts` as the received-order source.

### Within Each User Story

- Tests first, then service implementation, then controller endpoint wiring, then OpenAPI annotations.
- DTOs, repositories, mappers, exceptions, and audit helper tasks in Phase 2 must be complete before story service implementation.
- Audit logging must be added in the same story phase as the mutation that creates it.

## Parallel Opportunities

- T003, T004, and T005 can run in parallel after T001.
- T008 through T020 can run in parallel after entity alignment decisions are confirmed.
- T030 and T031 can run in parallel for US1.
- T038 and T039 can run in parallel for US2.
- T047, T048, and T049 can run in parallel for US3.
- T058, T059, T060, and T061 can run in parallel for US4.
- T062 through T068 can run in parallel for US4 DTO/repository work.
- T077 and T078 can run in parallel for US5 tests.
- US1 and US2 can proceed in parallel after Phase 2 if different developers own Dealer and Supplier files.

## Parallel Example: User Story 3

```text
Task: "Add dealer credit service unit tests for valid limit change, invalid limits, payment terms, manual status, and audit logging in backend/src/test/java/com/wms/service/DealerCreditServiceTest.java"
Task: "Add dealer credit controller integration tests for credit endpoints and unauthorized access in backend/src/test/java/com/wms/controller/DealerCreditControllerTest.java"
Task: "Add dealer transaction credit-check unit tests in backend/src/test/java/com/wms/service/DealerTransactionCreditPolicyTest.java"
```

## Implementation Strategy

### MVP First

1. Complete Phase 1 and Phase 2.
2. Complete Phase 3 for dealer profile management.
3. Validate US1 independently with service and controller tests.
4. Demo dealer create/update/list/detail/deactivate/reactivate plus audit evidence.

### Incremental Delivery

1. Add US2 supplier profile management and duplicate-code validation.
2. Add US3 dealer credit controls and transaction credit policy.
3. Add US4 Delivery Order CRUD with inactive dealer guard.
4. Add US5 supplier received-order viewing.
5. Run full backend verification and OpenAPI review before PR.

### Team Parallel Strategy

1. One developer owns Dealer profile and credit-control files.
2. One developer owns Supplier profile and deactivation guard files.
3. One developer owns Delivery Order DTO/repository/service/controller files.
4. One developer owns supplier received-order read endpoints.
5. Coordinate on shared `PartnerAuditUtil`, `GlobalExceptionHandler`, and audit enum changes to avoid merge conflicts.

## Notes

- Do not add raw SQL in application code.
- Do not physically delete Dealer or Supplier records.
- Do not change Dealer entity/profile fields to `companyName`, `taxCode`, `address`, or `contactPerson`; use existing `name`, `defaultDeliveryAddress`, and `region` fields.
- Do not allow normal dealer profile updates to bypass accountant-manager credit permissions.
- Do not add unsupported Supplier fields such as `email` unless the schema/spec is updated first.
- Stop and warn before continuing if GitNexus reports HIGH or CRITICAL risk.
