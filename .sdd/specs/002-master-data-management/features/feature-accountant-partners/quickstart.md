# Quickstart: Accountant Partner & Credit Limit Management

## Prerequisites

- Java 21
- Maven
- PostgreSQL 18 configured for the backend profile

## Implementation Steps

1. Confirm existing `Dealer`, `Supplier`, `CreditStatus`, `AuditLog`, and audit service/entity conventions.
2. Add missing repositories, DTOs, mappers, services, and controllers listed in `plan.md`.
3. Implement dealer profile CRUD with soft delete/reactivation and audit logs.
4. Implement supplier profile CRUD with soft delete/reactivation, supplier-code uniqueness, non-unique optional tax code, deactivation guards, and audit logs.
5. Implement accountant-manager-only endpoints for dealer credit limit, payment term, and credit status.
6. Add `PartnerEligibilityService` and wire downstream receipt, delivery, finance, and transaction creation services to block inactive partners.
7. Add dealer credit check to the dealer transaction creation path before the transaction is persisted.
8. Update OpenAPI/Swagger annotations to match `contracts/openapi.yaml`.

## Verification

Run from repo root:

```powershell
cd backend
mvn test
mvn compile
```

Required test coverage:

- Unit tests for dealer credit-limit validation, exact-limit allowance, exceeded-limit rejection, automatic credit hold, payment term validation, manual credit status update, and audit logging.
- Unit tests for inactive dealer/supplier guards.
- Unit tests for Delivery Order CRUD and inactive dealer rejection before Delivery Order creation.
- Unit tests for supplier received-order read-only listing and detail lookup from `receipts`.
- Integration tests for all dealer and supplier endpoints, including happy paths, validation failures, duplicate supplier code, unauthorized credit update, and soft delete/reactivation behavior.

## Manual API Smoke Tests

Use authenticated users with `ACCOUNTANT` and `ACCOUNTANT_MANAGER` roles:

- Create dealer, update profile, deactivate, reactivate.
- Update dealer credit limit to a valid higher amount.
- Attempt credit limit below current balance and expect validation failure.
- Update payment term to `60`; attempt `45` and expect validation failure.
- Manually set credit status to `CREDIT_HOLD` and verify new dealer transactions are blocked.
- Create supplier, attempt duplicate code, deactivate with and without open operations, reactivate.
