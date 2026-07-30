# Implementation Plan: 003-inbound-receipt-qc

**Status**: Draft - split by feature  
**Spec**: [spec.md](./spec.md)

## 1. Implementation Order

1. F01 Receipt Drafting
2. F02 Receipt Counting
3. F03 QC Classification
4. F04 Manager Decision
5. F05 Putaway And Inventory
6. F06 Quarantine RTV

## 2. Shared Technical Context

- Backend: Spring Boot 3.4.5, Java 21, Spring Data JPA.
- Frontend: React 18.
- Database: PostgreSQL 18, Flyway.
- Tests: JUnit 5, Mockito, integration tests, Jest.
- API docs: OpenAPI/Swagger.

## 3. Shared Implementation Constraints

- No raw SQL in application code.
- All write endpoints use DTO validation and centralized exception handling.
- All warehouse-scoped actions validate role plus warehouse assignment.
- Every write request includes `expectedVersion`.
- Create receipt is exempt from `expectedVersion` because no receipt version exists before creation.
- Validation failures return HTTP 400; stale version and duplicate/idempotency conflicts return HTTP 409; business rule violations return HTTP 422.
- Inventory mutations preserve non-negative total, reserved, and available quantity.
- Every mutation writes audit with actor, role, warehouse, before, after.
- Flyway migration impact must be verified before implementation: `receipts`,
  `receipt_items`, quarantine inventory fields, receipt statuses, optimistic
  locking columns, `batches.batch_code` uniqueness/sequence support, and
  constraints for generated `receipt_number` uniqueness/daily sequence conflict
  handling, pre-receive approval fields/statuses (`PENDING_MANAGER_APPROVAL`,
  `REVISION_REQUIRED`, `pre_receive_*`), distinct receipt-line batch lineage,
  and RTV idempotency must exist or be added through new immutable migrations.

## 4. Feature Plans

| Feature | Data | Plan |
|---------|------|------|
| F01 Receipt Drafting | [data-model.md](./features/feature-01-receipt-drafting/data-model.md) | [plan.md](./features/feature-01-receipt-drafting/plan.md) |
| F02 Receipt Counting | [data-model.md](./features/feature-02-receipt-counting/data-model.md) | [plan.md](./features/feature-02-receipt-counting/plan.md) |
| F03 QC Classification | [data-model.md](./features/feature-03-qc-classification/data-model.md) | [plan.md](./features/feature-03-qc-classification/plan.md) |
| F04 Manager Decision | [data-model.md](./features/feature-04-manager-decision/data-model.md) | [plan.md](./features/feature-04-manager-decision/plan.md) |
| F05 Putaway And Inventory | [data-model.md](./features/feature-05-putaway-inventory/data-model.md) | [plan.md](./features/feature-05-putaway-inventory/plan.md) |
| F06 Quarantine RTV | [data-model.md](./features/feature-06-quarantine-rtv/data-model.md) | [plan.md](./features/feature-06-quarantine-rtv/plan.md) |

## 5. Cross-Feature Verification

- Full pass path: `PENDING_MANAGER_APPROVAL -> PENDING_RECEIPT -> DRAFT -> QC_COMPLETED -> APPROVED -> PUTAWAY_COMPLETED`.
- Pre-receive rejection path: `PENDING_MANAGER_APPROVAL -> REVISION_REQUIRED -> PENDING_MANAGER_APPROVAL`; counting and QC remain blocked until manager approval moves the receipt to `PENDING_RECEIPT`.
- Partial failed path: `DRAFT -> QC_FAILED -> PARTIALLY_APPROVED -> PUTAWAY_COMPLETED`, failed quantity remains Quarantine.
- Whole reject path: `QC_COMPLETED/QC_FAILED -> RETURN_TO_SUPPLIER_PENDING -> RETURNED_TO_SUPPLIER`.
- Correction paths: `QC_COMPLETED/QC_FAILED -> DRAFT` before WH_MANAGER finalization; `APPROVED/PARTIALLY_APPROVED/RETURN_TO_SUPPLIER_PENDING -> DRAFT` before putaway or handover finalization.
- Cancel path: `PENDING_MANAGER_APPROVAL/REVISION_REQUIRED/PENDING_RECEIPT/DRAFT/QC_COMPLETED/QC_FAILED -> CANCELLED` before final inventory impact.
- RTV path: finalized Quarantine quantity creates pending RTV/Debit Note, then physical confirmation reduces Quarantine exactly once.
- Outbound excludes pending receipt, approved-not-putaway, partial-approved-not-putaway, and Quarantine stock.
- AP notification uses accepted/putaway quantity only.
