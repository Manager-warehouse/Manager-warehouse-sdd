# Feature Specification: [FEATURE NAME]

**Feature Branch**: `[###-feature-name]`

**Created**: [DATE]

**Status**: Draft

**Input**: User description: "$ARGUMENTS"

## 1. Context And Goal

[Business context, target actors, and why this feature matters for WMS.]

## 2. Actors

| Actor | Role | Responsibilities |
|-------|------|------------------|
| [Actor] | [Maker/Checker/Admin/etc.] | [What they do] |

## 3. User Scenarios & Testing *(mandatory)*

### User Story 1 - [Brief Title] (Priority: P1)

[Describe this user journey in business language.]

**Why this priority**: [Business value]

**Independent Test**: [How to verify this story independently]

**Acceptance Scenarios**:

1. **Given** [initial state], **When** [action], **Then** [expected outcome]
2. **Given** [initial state], **When** [action], **Then** [expected outcome]

---

### User Story 2 - [Brief Title] (Priority: P2)

[Describe this user journey.]

**Why this priority**: [Business value]

**Independent Test**: [How to verify independently]

**Acceptance Scenarios**:

1. **Given** [initial state], **When** [action], **Then** [expected outcome]

---

### Edge Cases

- [Validation boundary, e.g. zero/negative quantity]
- [Concurrency case, e.g. inventory version conflict]
- [Authorization case, e.g. user lacks warehouse scope]
- [State transition case, e.g. invalid status transition]

## 4. Functional Requirements (EARS)

- **FR-001**: WHEN [trigger], the system SHALL [observable behavior].
- **FR-002**: WHILE [state], the system SHALL [constraint].
- **FR-003**: IF [condition], the system SHALL [alternate behavior].
- **FR-004**: WHERE [scope/entity], the system SHALL [data/business rule].

## 5. Non-functional Requirements

| ID | Requirement | Target |
|----|-------------|--------|
| NFR-001 | [Performance/reliability/security requirement] | [Measurable target] |

## 6. Data Model

### [table_name]

- `id` (BIGSERIAL, PK)
- [columns, constraints, relationships]

### Entity Rules

- [Soft-delete/cancel behavior]
- [Inventory/QC/accounting invariants if applicable]

## 7. API Spec

| Method | Endpoint | Purpose | Auth |
|--------|----------|---------|------|
| POST | `/api/v1/[resource]` | [Create/action] | [Role + warehouse scope] |

Request/response schemas MUST be reflected in OpenAPI/Swagger.

## 8. Error Handling

| Error | HTTP | Condition |
|-------|------|-----------|
| [ERROR_CODE] | [400/401/403/404/409/422] | [Condition] |

## 9. Audit Trail

- Every mutation SHALL create an audit log entry.
- Audit action names: [ACTION_1, ACTION_2]
- Audit payload MUST include actor, actor role, entity type, entity id, warehouse when relevant, timestamp, and before/after values when relevant.
- Sensitive values such as passwords, JWTs, refresh tokens, secrets, and credentials MUST NOT be logged.

## 10. Business Invariants

- Inventory: [None or total/reserved/version/available rules].
- QC/quarantine: [None or QC gate/quarantine rules].
- Transfer: [None or In-Transit/discrepancy rules].
- Accounting/credit: [None or period/debt/COGS rules].
- Master/transaction data: [soft delete or cancel rules].

## 11. Success Criteria

- **SC-001**: [Measurable outcome]
- **SC-002**: [Measurable outcome]

## 12. Assumptions

- [Assumption]

## 13. Out Of Scope

- [Explicit exclusions]
