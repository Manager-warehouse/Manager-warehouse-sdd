# Feature: System Audit Logging

## 1. Context and Goal

The system records immutable audit logs for user-driven actions that change data or state. Audit logs are shown to System Admin in the existing Audit Trail tab inside `UserManagement`.

## 2. Actors

- **System Admin (`ADMIN`)**: Can view and filter audit logs.
- **CEO and all other roles**: Cannot view audit logs.
- **Authenticated user**: Can generate audit logs by changing data or state, but cannot read audit logs unless role is `ADMIN`.

## 3. Functional Requirements

- The system SHALL log user-driven changes across authentication state, RBAC, system configuration, master data, warehouse operations, finance, returns, scrap, and disposal.
- The system SHALL log `LOGIN` and `LOGOUT`.
- The system SHALL NOT log read-only view, search, filter, or export actions.
- The system SHALL store only changed fields in `old_value` and `new_value`.
- The system SHALL omit sensitive fields from audit log values.
- The system SHALL prevent update and delete of audit log rows after creation.
- The system SHALL allow only `ADMIN` to access audit log list and detail APIs.
- The system SHALL return 30 logs per page, ordered by newest timestamp first.
- The system SHALL allow unfiltered browsing through page 50.
- The system SHALL require a time or warehouse filter for requests beyond page 50.

## 4. Frontend Design

Audit log remains a tab in `frontend/src/pages/Admin/UserManagement.jsx`.

The existing table design is retained:

| Column | Field |
|--------|-------|
| Thời gian | `timestamp` |
| Người thực hiện | `actorName` |
| Thao tác | `action` |
| Chi tiết đối tượng | `entityType`, `entityId` |
| Nội dung | `description` |

Additional UI behavior:

- Add page controls for page-based pagination.
- Add optional filters for time range and warehouse.
- Add row detail modal/drawer that displays metadata and before/after changed fields.
- Do not create a standalone audit-log route.

## 5. API Endpoints

### `GET /api/v1/audit-logs`

**Authorization**: `ADMIN` only

| Parameter | Type | Required | Default |
|-----------|------|----------|---------|
| page | Integer | No | 1 |
| pageSize | Integer | No | 30 |
| from | ISO date/datetime | No | null |
| to | ISO date/datetime | No | null |
| warehouseId | Long | No | null |

### `GET /api/v1/audit-logs/{id}`

**Authorization**: `ADMIN` only

Returns one audit log with changed field before/after values.

## 6. Data Model

| Field | Type | Notes |
|-------|------|-------|
| id | BIGSERIAL | Primary key |
| actor_id | BIGINT FK | NOT NULL, authenticated user |
| actor_role | VARCHAR(50) | NOT NULL, snapshot role |
| action | VARCHAR(50) | LOGIN, LOGOUT, CREATE, UPDATE, STATUS_CHANGE, APPROVE, REJECT, CANCEL, SOFT_DELETE, ASSIGN, UNASSIGN |
| entity_type | VARCHAR(100) | NOT NULL |
| entity_id | BIGINT | NOT NULL |
| description | TEXT | NOT NULL |
| warehouse_id | BIGINT FK | Nullable |
| old_value | JSONB | Changed fields before values |
| new_value | JSONB | Changed fields after values |
| timestamp | TIMESTAMPTZ | NOT NULL, default NOW() |
| ip_address | VARCHAR(45) | Nullable |

## 7. Acceptance Criteria

**Scenario: Admin Views Audit Table**
- Given the authenticated user has role `ADMIN`
- When the user opens the Audit Trail tab in User Management
- Then the system shows page 1 with 30 newest audit logs.

**Scenario: CEO Cannot View Audit Logs**
- Given the authenticated user has role `CEO`
- When the user requests audit log list or detail
- Then the system returns `FORBIDDEN_AUDIT_ACCESS`.

**Scenario: Detail View**
- Given an audit log row is visible
- When Admin opens detail
- Then the system displays metadata and changed fields with before/after values.

**Scenario: Page Limit**
- Given no filters are applied
- When Admin requests page 51
- Then the system returns `QUERY_RANGE_TOO_LARGE`.

**Scenario: Filtered Older Search**
- Given Admin applies time or warehouse filter
- When Admin requests older logs
- Then the system returns matching logs ordered by newest timestamp first.

**Scenario: Immutability**
- Given an audit log entry exists
- When any update or delete is attempted
- Then the system rejects the mutation and preserves the original row.
