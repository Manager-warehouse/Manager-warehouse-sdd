# Data Model: System Audit Logging

**Feature**: System Audit Logging
**Date**: 2026-06-03

---

## Entity: AuditLog

**Table**: `audit_logs`

**Immutability**: Append-only. No UPDATE or DELETE is allowed after creation.

### Fields

| Field | Java Type | DB Type | Nullable | Notes |
|-------|-----------|---------|----------|-------|
| id | Long | BIGSERIAL | No | Primary key |
| actor | User | BIGINT FK | No | Authenticated user who performed the action |
| actorRole | String | VARCHAR(50) | No | Snapshot role at action time |
| action | AuditAction | VARCHAR(50) | No | Must match backend `AuditAction.java`; includes generic and domain-specific audit actions such as receipt, delivery, transfer, stocktake, price, and quarantine actions |
| entityType | String | VARCHAR(100) | Yes | Affected entity group/type; nullable when the action has no natural affected entity row |
| entityId | Long | BIGINT | Yes | Affected entity ID; nullable when the action has no natural affected entity row |
| description | String | TEXT | No | Auto-generated summary for table display |
| warehouse | Warehouse | BIGINT FK | Yes | Optional warehouse filter context |
| oldValue | Map/String JSON | JSONB | Yes | Changed fields before value |
| newValue | Map/String JSON | JSONB | Yes | Changed fields after value |
| timestamp | OffsetDateTime | TIMESTAMPTZ | No | Defaults to creation time |
| ipAddress | String | VARCHAR(45) | Yes | Client IP |

### Relationships

```text
AuditLog * -> 1 User
AuditLog * -> 0..1 Warehouse
```

### Indexes

| Index Name | Columns | Purpose |
|------------|---------|---------|
| idx_audit_logs_timestamp | timestamp DESC | Default newest-first list |
| idx_audit_logs_actor_id | actor_id | Actor lookups |
| idx_audit_logs_entity | entity_type, entity_id | Entity detail/history lookup |
| idx_audit_logs_warehouse_id | warehouse_id | Warehouse filter |
| idx_audit_logs_warehouse_timestamp | warehouse_id, timestamp DESC | Warehouse + time filter |
| idx_audit_actor_role | actor_role | Role filter/debugging |

### Validation Rules

- `actor_id` must reference an authenticated user.
- `action` must match backend `AuditAction.java`.
- `old_value` and `new_value` must contain only changed fields.
- If no entity field changed, `old_value` and `new_value` are empty.
- Sensitive field names remain auditable, but their before/after values are omitted from `old_value` and `new_value`.
- AuditLog cannot be updated or deleted.

---

## DTO: AuditLogListItemResponse

Used by the existing Audit Trail table in `UserManagement`.

| Field | Type | Notes |
|-------|------|-------|
| id | Long | Row key |
| timestamp | String | ISO 8601 |
| actorId | Long | |
| actorName | String | From actor full name |
| actorRole | String | Snapshot role |
| action | String | Enum value from DB |
| entityType | String | Nullable |
| entityId | Long | Nullable |
| description | String | Table summary text |
| warehouseId | Long | Nullable |
| warehouseCode | String | Nullable display value |

## DTO: AuditLogDetailResponse

Returned when the user opens detail for a row.

| Field | Type | Notes |
|-------|------|-------|
| id | Long | |
| timestamp | String | ISO 8601 |
| actorId | Long | |
| actorName | String | |
| actorRole | String | |
| action | String | |
| entityType | String | Nullable |
| entityId | Long | Nullable |
| description | String | |
| warehouseId | Long | Nullable |
| warehouseCode | String | Nullable |
| oldValue | Map<String, Object> | Changed fields before values |
| newValue | Map<String, Object> | Changed fields after values |
| ipAddress | String | Nullable |

## DTO: AuditLogPageResponse

| Field | Type | Notes |
|-------|------|-------|
| data | List<AuditLogListItemResponse> | Current page rows |
| page | Integer | 1-based page number |
| pageSize | Integer | Fixed at 30 |
| hasNext | Boolean | True if another page is available |
| hasPrevious | Boolean | True if page > 1 |
| requiresFilterForOlder | Boolean | True when unfiltered browsing would go beyond the newest 1,500 entries |

---

## Frontend View Model

### Existing Table Columns

The existing frontend table can be retained with these columns:

| Column | Source Field |
|--------|--------------|
| Thời gian | `timestamp` |
| Người thực hiện | `actorName` |
| Thao tác | `action` |
| Chi tiết đối tượng | `entityType`, `entityId` |
| Nội dung | `description` |

### Detail View

Opening a row loads `GET /api/v1/admin/audit-logs/{id}` and renders:

- Header metadata: timestamp, actor, role, action, entity, warehouse, IP address.
- Changed fields table: field name, before value, after value.
- Empty changed fields state for actions where old/new values are not applicable, such as `LOGIN` and `LOGOUT`.
