# Tasks: Accountant Receivable Payment

**Input**: Design documents from `.sdd/specs/004-outbound-delivery-pod/features/feature-accountant-receivable-payment/`
**Prerequisites**: `plan.md`, `feature-accountant-receivable-payment.md`, `research.md`, `data-model.md`, `quickstart.md`, `contracts/auto-invoice.openapi.yaml`

**Tests**: Required by project Definition of Done and quickstart. Service/business logic tests are mandatory before implementation for each user story.

**Organization**: Tasks are grouped by user story so each story can be implemented and verified independently.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel because it touches different files or independent test fixtures
- **[Story]**: User story identifier (`US1`, `US2`, `US3`)
- Include exact file paths in every task

## Phase 1: Setup

**Purpose**: Confirm existing outbound, dealer, invoice, and audit seams before changing transactional delivery confirmation.

- [ ] T001 Read current delivery confirmation flow in `backend/src/main/java/com/wms/service/impl/DriverDeliveryServiceImpl.java` and identify the successful full-delivery branch.
- [ ] T002 [P] Inspect existing invoice and dealer entities in `backend/src/main/java/com/wms/entity/Invoice.java` and `backend/src/main/java/com/wms/entity/Dealer.java` for required fields and naming conventions.
- [ ] T003 [P] Inspect existing delivery order item pricing fields in `backend/src/main/java/com/wms/entity/DeliveryOrderItem.java` and confirm the persisted unit price snapshot field.
- [ ] T004 [P] Inspect audit logging conventions in `backend/src/main/java/com/wms/service` and `backend/src/main/java/com/wms/entity` to reuse the project audit pattern.

---

## Phase 2: Foundational

**Purpose**: Create the data and service foundation needed by all receivable automation stories.

**Critical**: No user-facing invoice creation endpoint belongs to this feature. Auto-invoice must be invoked internally from successful driver POD confirmation only.

- [ ] T005 Create or update Flyway migration in `backend/src/main/resources/db/migration/` to enforce one automatic invoice per delivery order.
- [ ] T006 Create or update Flyway migration in `backend/src/main/resources/db/migration/` for invoice line storage if invoice line persistence does not already exist.
- [ ] T007 [P] Add or update `InvoiceLine` entity in `backend/src/main/java/com/wms/entity/InvoiceLine.java` with invoice, delivery order item, product, quantity, unit price, and line amount fields.
- [ ] T008 [P] Add or update `InvoiceLineRepository` in `backend/src/main/java/com/wms/repository/InvoiceLineRepository.java`.
- [ ] T009 Add idempotency lookup methods to `backend/src/main/java/com/wms/repository/InvoiceRepository.java` for finding an invoice by delivery order id.
- [ ] T010 Add dealer balance persistence support to `backend/src/main/java/com/wms/repository/DealerRepository.java`, preserving optimistic locking or existing locking conventions.
- [ ] T011 Add delivery order item lookup support to `backend/src/main/java/com/wms/repository/DeliveryOrderItemRepository.java` for loading all items required to invoice a delivery order.
- [ ] T012 [P] Add internal service contract `backend/src/main/java/com/wms/service/AutoInvoiceService.java`.
- [ ] T013 [P] Add internal result DTO or record in `backend/src/main/java/com/wms/dto/outbound/AutoInvoiceResult.java` for invoice id, invoice number, amount, and idempotent flag.

**Checkpoint**: Data model and internal service seam exist. User stories can now be implemented independently.

---

## Phase 3: User Story 1 - Auto-create invoice after successful full POD confirmation (Priority: P1)

**Goal**: When a driver successfully confirms full delivery for one Delivery Order, the system creates an invoice and increases Dealer `current_balance` in the same transaction.

**Independent Test**: Confirm a full Delivery Order through the driver service and verify invoice total comes from `DeliveryOrderItem.unit_price`, due date is 30 days after issue date, Dealer balance increases once, and the Delivery Order completes.

### Tests for User Story 1

- [ ] T014 [P] [US1] Add service test for full delivery auto-invoice creation in `backend/src/test/java/com/wms/service/impl/AutoInvoiceServiceImplTest.java`.
- [ ] T015 [P] [US1] Add service test for due date calculation in `backend/src/test/java/com/wms/service/impl/AutoInvoiceServiceImplTest.java`.
- [ ] T016 [P] [US1] Add service test rejecting missing item unit price in `backend/src/test/java/com/wms/service/impl/AutoInvoiceServiceImplTest.java`.
- [ ] T017 [P] [US1] Add driver confirmation integration-style service test in `backend/src/test/java/com/wms/service/impl/DriverDeliveryServiceImplTest.java`.

### Implementation for User Story 1

- [ ] T018 [US1] Implement `AutoInvoiceServiceImpl` skeleton in `backend/src/main/java/com/wms/service/impl/AutoInvoiceServiceImpl.java` using constructor injection.
- [ ] T019 [US1] Implement item line amount calculation in `backend/src/main/java/com/wms/service/impl/AutoInvoiceServiceImpl.java` using persisted delivery order item `unit_price` snapshots only.
- [ ] T020 [US1] Implement invoice header creation in `backend/src/main/java/com/wms/service/impl/AutoInvoiceServiceImpl.java` with issue date as backend current local date and due date plus 30 calendar days.
- [ ] T021 [US1] Implement invoice line persistence in `backend/src/main/java/com/wms/service/impl/AutoInvoiceServiceImpl.java`.
- [ ] T022 [US1] Implement Dealer `current_balance` increase in `backend/src/main/java/com/wms/service/impl/AutoInvoiceServiceImpl.java` in the same transaction as invoice creation.
- [ ] T023 [US1] Reject non-full or partial delivery attempts before invoice creation in `backend/src/main/java/com/wms/service/impl/AutoInvoiceServiceImpl.java`.
- [ ] T024 [US1] Invoke `AutoInvoiceService` from the successful full-delivery branch in `backend/src/main/java/com/wms/service/impl/DriverDeliveryServiceImpl.java`.
- [ ] T025 [US1] Ensure delivery confirmation keeps the Delivery Order transition from `IN_TRANSIT` to completed status only after auto-invoice succeeds in `backend/src/main/java/com/wms/service/impl/DriverDeliveryServiceImpl.java`.
- [ ] T026 [US1] Update response mapping if needed in `backend/src/main/java/com/wms/dto/outbound/` to include invoice metadata without exposing a new invoice creation endpoint.

**Checkpoint**: User Story 1 should be independently testable and demonstrable through driver delivery confirmation.

---

## Phase 4: User Story 2 - Idempotency and same-trip isolation (Priority: P1)

**Goal**: Repeated confirmation attempts for the same Delivery Order must return the existing invoice without increasing Dealer balance again, and other Delivery Orders in the same trip must remain uninvoiced.

**Independent Test**: Execute confirmation twice for the same Delivery Order and verify one invoice, one Dealer balance increase, and no invoice for sibling Delivery Orders in the same trip.

### Tests for User Story 2

- [ ] T027 [P] [US2] Add idempotent existing invoice test in `backend/src/test/java/com/wms/service/impl/AutoInvoiceServiceImplTest.java`.
- [ ] T028 [P] [US2] Add duplicate confirmation test in `backend/src/test/java/com/wms/service/impl/DriverDeliveryServiceImplTest.java`.
- [ ] T029 [P] [US2] Add multi-delivery-order same-trip isolation test in `backend/src/test/java/com/wms/service/impl/DriverDeliveryServiceImplTest.java`.

### Implementation for User Story 2

- [ ] T030 [US2] Implement existing invoice lookup before any Dealer balance mutation in `backend/src/main/java/com/wms/service/impl/AutoInvoiceServiceImpl.java`.
- [ ] T031 [US2] Return `AutoInvoiceResult` with idempotent flag when an invoice already exists in `backend/src/main/java/com/wms/service/impl/AutoInvoiceServiceImpl.java`.
- [ ] T032 [US2] Handle unique constraint collision safely in `backend/src/main/java/com/wms/service/impl/AutoInvoiceServiceImpl.java` by returning the persisted invoice without duplicating Dealer balance.
- [ ] T033 [US2] Scope auto-invoice invocation to the confirmed Delivery Order only in `backend/src/main/java/com/wms/service/impl/DriverDeliveryServiceImpl.java`.
- [ ] T034 [US2] Ensure trip-level status logic does not invoice unconfirmed sibling Delivery Orders in `backend/src/main/java/com/wms/service/impl/DriverDeliveryServiceImpl.java`.
- [ ] T035 [US2] Add repository test or service assertion for unique invoice per Delivery Order in `backend/src/test/java/com/wms/`.

**Checkpoint**: User Story 2 prevents double receivables and preserves independent DO confirmation inside one trip.

---

## Phase 5: User Story 3 - Rollback, audit, and out-of-scope safeguards (Priority: P2)

**Goal**: If invoice or receivable persistence fails, the delivery confirmation rolls back completely; audit records reflect the financial side effect; payment and notification flows remain untouched.

**Independent Test**: Simulate invoice persistence failure and verify Delivery Order remains `IN_TRANSIT`, no invoice exists, Dealer balance is unchanged, and no payment or notification side effects occur.

### Tests for User Story 3

- [ ] T036 [P] [US3] Add rollback test for invoice save failure in `backend/src/test/java/com/wms/service/impl/DriverDeliveryServiceImplTest.java`.
- [ ] T037 [P] [US3] Add audit assertion test for invoice creation and Dealer balance before/after in `backend/src/test/java/com/wms/service/impl/AutoInvoiceServiceImplTest.java`.
- [ ] T038 [P] [US3] Add negative test proving payment receipt is not created by delivery confirmation in `backend/src/test/java/com/wms/service/impl/DriverDeliveryServiceImplTest.java`.
- [ ] T039 [P] [US3] Add negative test proving customer/dealer notification is not sent by delivery confirmation in `backend/src/test/java/com/wms/service/impl/DriverDeliveryServiceImplTest.java`.

### Implementation for User Story 3

- [ ] T040 [US3] Ensure transactional boundary covers Delivery Order completion, invoice creation, invoice lines, Dealer balance, and audit in `backend/src/main/java/com/wms/service/impl/DriverDeliveryServiceImpl.java`.
- [ ] T041 [US3] Add audit payload for invoice creation and Dealer balance before/after in `backend/src/main/java/com/wms/service/impl/AutoInvoiceServiceImpl.java`.
- [ ] T042 [US3] Ensure delivery confirmation does not create payment receipt records in `backend/src/main/java/com/wms/service/impl/DriverDeliveryServiceImpl.java`.
- [ ] T043 [US3] Ensure delivery confirmation does not trigger notification dispatch or due-date extension in `backend/src/main/java/com/wms/service/impl/DriverDeliveryServiceImpl.java`.

**Checkpoint**: User Story 3 protects accounting consistency and confirms out-of-scope behavior remains out of the implementation.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Validate OpenAPI contract, quickstart scenarios, compile/lint, and task completion quality.

- [ ] T044 [P] Update internal contract notes in `.sdd/specs/004-outbound-delivery-pod/features/feature-accountant-receivable-payment/contracts/auto-invoice.openapi.yaml` if implementation response metadata differs.
- [ ] T045 [P] Update quickstart verification notes in `.sdd/specs/004-outbound-delivery-pod/features/feature-accountant-receivable-payment/quickstart.md` if final command names or test classes differ.
- [ ] T046 Run backend targeted tests for auto invoice and driver delivery in `backend`.
- [ ] T047 Run `mvn compile` from `backend` and fix compile errors without weakening validation or audit rules.
- [ ] T048 Confirm no TODO comments, no new user-facing invoice creation endpoint, and no payment/notification behavior was introduced for this feature.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies
- **Foundational (Phase 2)**: Depends on Setup
- **User Stories (Phases 3-5)**: Depend on Foundational
- **Polish (Phase 6)**: Depends on completed user stories

### User Story Dependencies

- **US1 (P1)**: Core MVP and must complete first
- **US2 (P1)**: Depends on US1 invoice creation path
- **US3 (P2)**: Depends on US1 transaction path and can be completed after US2 or in parallel once US1 is stable

### Parallel Opportunities

- T002, T003, and T004 can run in parallel after T001.
- T007, T008, T012, and T013 can run in parallel after migration decisions are known.
- T014, T015, T016, and T017 can run in parallel because they target independent test cases.
- T027, T028, and T029 can run in parallel after US1 is complete.
- T036, T037, T038, and T039 can run in parallel after US1 transaction behavior exists.
- T044 and T045 can run in parallel during polish.

---

## Parallel Example: User Story 1

```bash
# Add independent tests first
Task: "T014 [P] [US1] Add service test for full delivery auto-invoice creation in backend/src/test/java/com/wms/service/impl/AutoInvoiceServiceImplTest.java"
Task: "T015 [P] [US1] Add service test for due date calculation in backend/src/test/java/com/wms/service/impl/AutoInvoiceServiceImplTest.java"
Task: "T016 [P] [US1] Add service test rejecting missing item unit price in backend/src/test/java/com/wms/service/impl/AutoInvoiceServiceImplTest.java"
Task: "T017 [P] [US1] Add driver confirmation integration-style service test in backend/src/test/java/com/wms/service/impl/DriverDeliveryServiceImplTest.java"
```

---

## Implementation Strategy

### MVP First

1. Complete Phase 1 and Phase 2.
2. Complete US1 and verify full delivery creates invoice plus Dealer receivable.
3. Run targeted service tests.
4. Stop and validate MVP behavior before adding idempotency and rollback hardening.

### Incremental Delivery

1. Deliver US1: automatic receivable creation from full POD confirmation.
2. Deliver US2: idempotency and same-trip isolation.
3. Deliver US3: rollback, audit, and explicit out-of-scope safeguards.
4. Complete polish and compile/test validation.

### Risk Notes

- Dealer balance mutation must be exactly-once and inside the delivery confirmation transaction.
- Invoice totals must use Delivery Order item unit price snapshots, never current product prices.
- Any invoice persistence failure must rollback Delivery Order completion and Dealer balance changes.
- No payment collection, payment approval, due-date extension, notification, or Delivery Order `CLOSED` transition belongs to this feature.
