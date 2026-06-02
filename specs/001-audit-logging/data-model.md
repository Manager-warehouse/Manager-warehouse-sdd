# Data Model: Audit Logging

**Feature**: System Audit Logging
**Date**: 2026-06-03

---

## Entity: AuditLog

**Table**: `audit_logs`
**Immutability**: No UPDATE or DELETE allowed after creation.

### Fields

| Field | Java Type | DB Type | Nullable | Notes |
|-------|-----------|---------|----------|-------|
| id | Long | BIGSERIAL (PK) | No | Auto-increment |
| actor | User (FK) | BIGINT | No | `@ManyToOne(LAZY)` → users(id) |
| actorRole | String | VARCHAR(50) | No | Snapshot of user's role at time of action |
| action | AuditAction | VARCHAR(50) | No | Enum: CREATE, UPDATE, APPROVE, REJECT, CANCEL, DELETE |
| entityType | String | VARCHAR(100) | No | e.g., RECEIPT, TRANSFER, INVENTORY |
| entityId | Long | BIGINT | No | PK of the affected entity |
| description | String | TEXT | No | Auto-generated: "{ACTION} {ENTITY_TYPE} {ENTITY_CODE}" |
| warehouse | Warehouse (FK) | BIGINT | Yes | `@ManyToOne(LAZY)` → warehouses(id). Null for global entities |
| oldValue | String | JSONB | Yes | Diff-only: only changed fields. Null for CREATE. Sensitive fields excluded |
| newValue | String | JSONB | Yes | Diff-only: only changed fields. Sensitive fields excluded |
| timestamp | OffsetDateTime | TIMESTAMPTZ | No | DEFAULT NOW() |
| ipAddress | String | VARCHAR(45) | Yes | Client IP from HttpServletRequest |

### Relationships

```
AuditLog *──1 User (actor)
AuditLog *──0..1 Warehouse
```

### Indexes

| Index Name | Columns | Purpose |
|------------|---------|---------|
| idx_audit_logs_timestamp | timestamp DESC | Default sort + date range filter |
| idx_audit_logs_actor_id | actor_id | Filter by actor |
| idx_audit_logs_entity | entity_type, entity_id | Filter by entity |
| idx_audit_logs_warehouse_id | warehouse_id | Filter by warehouse (RBAC) |

### Validation Rules

- `actor` must reference a valid, existing user — NOT NULL
- `actorRole` must be a valid role string from UserRole enum
- `action` must be one of the AuditAction enum values
- `entityType` must be one of the defined business entity types (see enum below)
- `description` is auto-generated, never user-supplied
- `oldValue` / `newValue` must never contain `password_hash` or credential fields

### Entity Types (Enum: AuditEntityType)

| Value | Mô tả |
|-------|--------|
| RECEIPT | Phiếu nhập kho |
| ISSUE | Phiếu xuất kho |
| TRANSFER | Phiếu điều chuyển liên kho |
| ADJUSTMENT | Phiếu điều chỉnh tồn kho |
| STOCKTAKE | Phiếu kiểm kê |
| DELIVERY_ORDER | Đơn giao hàng |
| BATCH | Lô hàng |
| INVENTORY | Tồn kho (quantity changes) |
| RETURN | Phiếu trả hàng |
| SCRAP_DISPOSAL | Phiếu hủy/thanh lý |
| TRIP | Chuyến xe giao hàng |

---

## DTO: AuditLogResponse

Response DTO for `GET /api/v1/audit-logs`:

| Field | Type | Notes |
|-------|------|-------|
| id | Long | |
| actorId | Long | |
| actorName | String | Resolved from actor.fullName |
| actorRole | String | |
| action | String | |
| entityType | String | |
| entityId | Long | |
| description | String | |
| warehouseId | Long | Nullable |
| oldValue | Map<String, Object> | Parsed from JSONB |
| newValue | Map<String, Object> | Parsed from JSONB |
| timestamp | OffsetDateTime | ISO 8601 format |
| ipAddress | String | |

## DTO: AuditLogPageResponse

Cursor-based page wrapper:

| Field | Type | Notes |
|-------|------|-------|
| data | List<AuditLogResponse> | |
| nextCursor | Long | ID of last entry, null if no more pages |
| hasNext | Boolean | |

---

## Special Business Rule: Transfer → 2 Entries

When a Transfer is created/shipped/received, the system creates **2 audit log entries**:

1. **Source warehouse entry**: `warehouse_id = source`, action reflects outbound context
2. **Destination warehouse entry**: `warehouse_id = destination`, action reflects inbound context

This ensures warehouse managers only see audit entries relevant to their assigned warehouses.
