# Research: Accountant Partner & Credit Limit Management

## Decision: Use existing Spring Boot layered backend only for Sprint 1

**Rationale**: The feature is primarily partner master data, credit control, and downstream service guards. The existing backend already has entity, enum, controller, service, repository, DTO, audit, and exception packages, so implementation should extend those layers.

**Alternatives considered**: A frontend-first implementation was rejected because the acceptance criteria are business-rule and API centric. A separate finance module was rejected for Sprint 1 because CLAUDE.md states finance/credit runs in the same backend and database without a message queue.

## Decision: Represent partner deactivation as `is_active = false`

**Rationale**: AGENTS.md and constitution both require master-data soft delete. Historical receipt, delivery, finance, and audit records must remain unchanged.

**Alternatives considered**: Physical deletion was rejected by project rules. Transaction-style `status = cancelled` was rejected because partner records are master data, not transaction records.

## Decision: Centralize partner eligibility checks

**Rationale**: Inactive dealers and suppliers must be blocked across delivery, inbound, purchasing, finance, and transaction creation flows. A small `PartnerEligibilityService` prevents scattered checks and keeps service methods under the constitution's size guidance.

**Alternatives considered**: Duplicating checks in every downstream service was rejected because it increases the chance of bypassing inactive partner rules. Database-only constraints were rejected because error responses must explain business reasons.

## Decision: Credit controls belong to Dealer service logic

**Rationale**: Dealer credit limit, payment terms, current balance, and credit status are tightly coupled to dealer transaction creation. Service methods must validate `credit_limit > 0`, `credit_limit > current_balance`, `payment_term_days in {30,60}`, and reject only the new transaction when it would exceed the limit.

**Alternatives considered**: Controller-only validation was rejected because transaction creation can be called by multiple workflows. Raw database triggers were rejected because the project forbids raw SQL in application logic and expects service-level tests.

## Decision: Dealer profile DTOs use current Dealer entity fields and service defaults

**Rationale**: The current Dealer entity/table uses `name`, `phone`, `default_delivery_address`, and `region` for profile data, while `payment_term_days`, `credit_limit`, `current_balance`, and `credit_status` are NOT NULL operational fields. Accountant profile create/update must not expose manager-owned credit fields, so create uses `DEFAULT_PAYMENT_TERM_DAYS`, `DEFAULT_CREDIT_LIMIT`, `current_balance = 0`, `credit_status = ACTIVE`, and `is_active = true`.

**Alternatives considered**: Adding supplier-style dealer fields such as `company_name`, `tax_code`, `address`, or `contact_person` was rejected because those fields are not in the current Dealer entity or Flyway dealer table.

## Decision: Use field-level audit entries for partner changes

**Rationale**: The feature requires before/after changed values for create, update, credit-limit, payment-term, credit-status, soft-delete, and reactivation. Existing audit log patterns should be reused so actor, role, action, entity type, entity id, timestamp, and changed fields are consistent.

**Alternatives considered**: Simple activity messages were rejected because they do not satisfy field-level before/after audit requirements.

## Decision: Supplier `tax_code` remains optional and non-unique

**Rationale**: The feature explicitly states the current schema does not define a unique constraint for supplier tax code. Only `code` must be unique.

**Alternatives considered**: Enforcing tax-code uniqueness was rejected because it would conflict with the current schema and feature requirement.
