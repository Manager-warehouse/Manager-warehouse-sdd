# Feature Specification: Hệ thống Quản trị & Phân quyền (System Admin & RBAC)

**Spec ID**: 001-system-admin
**Created**: 2026-05-30
**Status**: Draft
**Features**: US-WMS-01, US-WMS-21, Audit Log

---

## 1. Context and Goal

Hệ thống WMS Phúc Anh phục vụ nhiều vai trò (CEO, Trưởng kho, Kế toán, Thủ kho, Tài xế...)
và vận hành trên 3 kho vật lý (Hải Phòng, Hà Nội, Hồ Chí Minh). Mỗi vai trò có quyền hạn
khác nhau, và mỗi nhân viên chỉ được thao tác trên kho được phân công.

**Goal:** Xây dựng nền tảng quản trị tập trung cho phép System Admin cấu hình hệ thống,
phân quyền chi tiết theo vai trò và phạm vi kho, đồng thời ghi nhận mọi thao tác
qua Audit Log để phục vụ kiểm toán và truy vết.

## 2. Actors

| Actor | Vai trò | Ghi chú |
|-------|---------|---------|
| CEO | Checker cấp cao | Duyệt cấu hình quan trọng |
| System Admin | Admin | Quản trị hệ thống, phân quyền |
| Mọi người dùng | End User | Bị ảnh hưởng bởi RBAC |

## 3. Functional Requirements (EARS)

**Ubiquitous:**
- The system SHALL always require authentication via JWT (access + refresh token)
  for all endpoints except `/api/v1/auth/login` and `/api/v1/auth/refresh`.
- The system SHALL always hash passwords using bcrypt with cost factor ≥ 12.
- The system SHALL always record an audit log entry for every CREATE, UPDATE,
  DELETE, APPROVE, REJECT, or CANCEL operation on any business entity.

**Event-driven:**
- WHEN a System Admin creates or modifies a user, the system SHALL validate
  username uniqueness and password strength (min 8 chars, mixed case + digits).
- WHEN a user accesses any warehouse-scoped resource, the system SHALL verify
  that the user's warehouse assignment includes the target warehouse.
- WHEN a user's role or warehouse assignment changes, the system SHALL log
  the change with before/after state in the audit log.
- WHEN System Admin changes a system configuration parameter, the system SHALL
  record the previous value, new value, and actor identity.

**State-driven:**
- WHILE a user account is `is_active = false`, the system SHALL reject all
  authentication attempts.
- WHILE the system is in a closed accounting period, the system SHALL prevent
  any modification to transactions within that period.

**Optional:**
- WHERE the user has `SYSTEM_ADMIN` role, they SHALL have access to all
  warehouses regardless of individual warehouse assignments.
- WHERE a user has the `CEO` role, they SHALL have view-only access to all
  dashboards and reports across all warehouses.

## 4. Non-functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-001 | Authentication response time (p95) | ≤ 500ms |
| NFR-002 | Audit log query performance for last 90 days | ≤ 2s |
| NFR-003 | Audit log retention | Minimum 5 years |
| NFR-004 | Authorization check overhead per request | ≤ 10ms |
| NFR-005 | Audit log immutability | No DELETE, no UPDATE after creation |

## 5. Data Model

### User
| Field | Type | Notes |
|-------|------|-------|
| id | BIGINT (PK) | Auto-increment |
| username | VARCHAR(50) | UNIQUE, NOT NULL |
| password_hash | VARCHAR(255) | bcrypt, NOT NULL |
| full_name | VARCHAR(100) | NOT NULL |
| email | VARCHAR(100) | |
| is_active | BOOLEAN | DEFAULT true, soft-delete |
| version | INTEGER | @Version |

### Role
| Field | Type | Notes |
|-------|------|-------|
| id | BIGINT (PK) | |
| name | VARCHAR(30) | UNIQUE: CEO, SYSTEM_ADMIN, WAREHOUSE_MANAGER, ACCOUNTANT_MANAGER, PLANNER, DISPATCHER, STORE_KEEPER, WAREHOUSE_STAFF, ACCOUNTANT, DRIVER |
| description | VARCHAR(200) | |

### UserRole (join)
| Field | Type | Notes |
|-------|------|-------|
| user_id | BIGINT (FK) | |
| role_id | BIGINT (FK) | |
| warehouse_id | BIGINT (FK) | NULL = all warehouses |

### AuditLog
| Field | Type | Notes |
|-------|------|-------|
| id | BIGINT (PK) | |
| actor_id | BIGINT (FK→User) | |
| action | VARCHAR(50) | e.g., "RECEIPT_CREATED" |
| entity_type | VARCHAR(50) | |
| entity_id | BIGINT | |
| before_state | JSONB | |
| after_state | JSONB | |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() |

## 6. API Spec

### Auth
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | /api/v1/auth/login | No | Login, returns JWT pair |
| POST | /api/v1/auth/refresh | No | Refresh access token |
| POST | /api/v1/auth/logout | Bearer | Invalidate refresh token |
| GET | /api/v1/auth/me | Bearer | Current user profile + permissions |

### Admin Users
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | /api/v1/admin/users | SYSTEM_ADMIN | List users (paginated) |
| POST | /api/v1/admin/users | SYSTEM_ADMIN | Create user with roles + warehouses |
| PUT | /api/v1/admin/users/{id} | SYSTEM_ADMIN | Update user |
| GET | /api/v1/admin/users/{id} | SYSTEM_ADMIN | Get user detail |
| GET | /api/v1/admin/roles | SYSTEM_ADMIN | List all roles |
| GET | /api/v1/admin/warehouses | SYSTEM_ADMIN | List all warehouses |

### System Config
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | /api/v1/admin/system-config | SYSTEM_ADMIN | Get config parameters |
| PUT | /api/v1/admin/system-config | SYSTEM_ADMIN | Update config parameters |

### Audit Logs
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | /api/v1/audit-logs | SYSTEM_ADMIN, WAREHOUSE_MANAGER | Query audit logs (filterable) |

## 7. Error Handling

| Error | HTTP | Condition |
|-------|------|-----------|
| INVALID_CREDENTIALS | 401 | Wrong username/password |
| TOKEN_EXPIRED | 401 | Access/refresh token expired |
| FORBIDDEN_WAREHOUSE | 403 | User not assigned to warehouse |
| FORBIDDEN_ROLE | 403 | User lacks required role |
| USERNAME_TAKEN | 409 | Duplicate username |
| WEAK_PASSWORD | 400 | Password fails strength policy |
| VALIDATION_ERROR | 400 | Request body validation failure |

## 8. Acceptance Criteria

### Gherkin Scenarios

**Scenario 1: Role-Based Access Control**
- Given a user with role `WAREHOUSE_STAFF` assigned to warehouse "Hai Phong"
- When the user attempts to access `GET /api/v1/reports/financial`
- Then the system SHALL return 403 Forbidden

**Scenario 2: Warehouse-Scoped Isolation**
- Given a user with role `STORE_KEEPER` assigned only to warehouse "Ha Noi"
- When the user attempts to `POST /api/v1/receipts` with warehouse_id = "HCM"
- Then the system SHALL return 403 Forbidden

**Scenario 3: Audit Log Creation**
- Given a valid authenticated session
- When the user creates any warehouse operation (receipt, issue, transfer, etc.)
- Then the system SHALL create an audit log entry with actor_id, action, timestamp, before_state, after_state

**Scenario 4: System Config Change**
- Given a user with `SYSTEM_ADMIN` role
- When they update a system configuration parameter
- Then the system SHALL record the old and new values in the audit log

## 9. Out of Scope

- OAuth2 / SSO integration (future)
- LDAP / Active Directory sync
- Two-factor authentication
- IP whitelisting or geo-restrictions
- User self-registration
- Password expiration policy
