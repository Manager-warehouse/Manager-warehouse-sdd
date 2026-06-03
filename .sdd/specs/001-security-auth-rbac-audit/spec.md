# Feature Specification: Hệ thống Quản trị & Phân quyền (System Admin & RBAC)

**Spec ID**: 001-security-auth-rbac-audit
**Created**: 2026-05-30
**Status**: Draft
**Features**: US-WMS-01, US-WMS-21, Audit Log

---

## 1. Context and Goal

Hệ thống WMS Phúc Anh phục vụ nhiều vai trò (CEO, Trưởng kho, Kế toán, Thủ kho, Tài xế...) và vận hành trên 3 kho vật lý (Hải Phòng, Hà Nội, Hồ Chí Minh). 

**Goal:** Xây dựng nền tảng quản trị tập trung cho phép System Admin cấu hình hệ thống, phân quyền chi tiết theo vai trò và phạm vi kho, đồng thời ghi nhận mọi thao tác qua Audit Log.

### Features List
* [US-WMS-01: Cấu hình Tham số Hệ thống](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/001-security-auth-rbac-audit/features/feature-admin-system-config.md)
* [US-WMS-21: Phân quyền & Cách ly Kho](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/001-security-auth-rbac-audit/features/feature-admin-auth-rbac.md)
* [Xác thực Người dùng (JWT)](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/001-security-auth-rbac-audit/features/feature-user-auth.md)
* [Nhật ký Hoạt động (Audit Log)](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/001-security-auth-rbac-audit/features/feature-system-audit-logging.md)

## 2. Actors

| Actor | Vai trò | Ghi chú |
|-------|---------|---------|
| CEO | Checker cấp cao | Xem dashboard chiến lược |
| System Admin | Admin | Quản trị hệ thống, phân quyền |
| Mọi người dùng | End User | Bị ảnh hưởng bởi RBAC |

## 3. Functional Requirements (EARS)
*Vui lòng xem chi tiết yêu cầu chức năng EARS tại các tài liệu đặc tả tính năng:*
* [EARS - Admin System Config](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/001-security-auth-rbac-audit/features/feature-admin-system-config.md#3-functional-requirements-ears)
* [EARS - Admin Auth & RBAC](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/001-security-auth-rbac-audit/features/feature-admin-auth-rbac.md#3-functional-requirements-ears)
* [EARS - User Auth](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/001-security-auth-rbac-audit/features/feature-user-auth.md#3-functional-requirements-ears)
* [EARS - System Audit Log](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/001-security-auth-rbac-audit/features/feature-system-audit-logging.md#3-functional-requirements-ears)

## 4. Non-functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-001 | Authentication response time (p95) | ≤ 500ms |
| NFR-002 | Audit log query performance for last 90 days | ≤ 2s |
| NFR-003 | Audit log retention | Minimum 5 years |
| NFR-004 | Authorization check overhead per request | ≤ 10ms |
| NFR-005 | Audit log immutability | No DELETE, no UPDATE after creation |

## 5. Data Model

### users
| Field | Type | Notes |
|-------|------|-------|
| id | BIGSERIAL (PK) | Auto-increment |
| code | VARCHAR(50) | UNIQUE, NOT NULL (Mã nhân viên) |
| full_name | VARCHAR(255) | NOT NULL |
| email | VARCHAR(255) | UNIQUE, NOT NULL |
| phone | VARCHAR(20) | |
| password_hash | VARCHAR(255) | bcrypt cost >= 12, NOT NULL |
| role | VARCHAR(50) | NOT NULL, CHECK (role IN ('ADMIN', 'CEO', 'WAREHOUSE_MANAGER', 'STOREKEEPER', 'WAREHOUSE_STAFF', 'ACCOUNTANT', 'ACCOUNTANT_MANAGER', 'PLANNER', 'DISPATCHER', 'DRIVER', 'REPORT_VIEWER')) |
| job_title | VARCHAR(100) | |
| shift | VARCHAR(50) | Ca làm việc (cho nhân viên kho) |
| region | VARCHAR(100) | Khu vực phụ trách (cho Dispatcher) |
| is_active | BOOLEAN | DEFAULT true, soft-delete |
| created_at | TIMESTAMPTZ | DEFAULT NOW() |
| updated_at | TIMESTAMPTZ | DEFAULT NOW() |

### user_warehouse_assignments
| Field | Type | Notes |
|-------|------|-------|
| id | BIGSERIAL (PK) | |
| user_id | BIGINT (FK) | REFERENCES users(id), NOT NULL |
| warehouse_id | BIGINT (FK) | REFERENCES warehouses(id), NOT NULL |
| assigned_by | BIGINT (FK) | REFERENCES users(id), NOT NULL |
| assigned_at | TIMESTAMPTZ | DEFAULT NOW(), NOT NULL |
| UNIQUE(user_id, warehouse_id) | | |

### audit_logs
| Field | Type | Notes |
|-------|------|-------|
| id | BIGSERIAL (PK) | |
| actor_id | BIGINT (FK) | REFERENCES users(id) (NULL = system job) |
| actor_role | VARCHAR(50) | Snapshot role tại thời điểm thực hiện |
| action | VARCHAR(50) | CHECK (action IN ('CREATE','UPDATE','APPROVE','REJECT','CANCEL','DELETE')) |
| entity_type | VARCHAR(100) | NOT NULL |
| entity_id | BIGINT | NOT NULL |
| old_value | JSONB | |
| new_value | JSONB | |
| timestamp | TIMESTAMPTZ | DEFAULT NOW() |
| ip_address | VARCHAR(45) | |

### system_configs
| Field | Type | Notes |
|-------|------|-------|
| id | BIGSERIAL (PK) | |
| config_key | VARCHAR(100) | UNIQUE, NOT NULL |
| config_value | TEXT | NOT NULL |
| description | TEXT | |
| updated_by | BIGINT (FK) | REFERENCES users(id) |
| updated_at | TIMESTAMPTZ | DEFAULT NOW() |

## 6. API Spec
*Vui lòng xem chi tiết API endpoints tại các tài liệu đặc tả tính năng:*
* [APIs - Admin System Config](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/001-security-auth-rbac-audit/features/feature-admin-system-config.md#4-api-endpoints)
* [APIs - Admin Auth & RBAC](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/001-security-auth-rbac-audit/features/feature-admin-auth-rbac.md#4-api-endpoints)
* [APIs - User Auth](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/001-security-auth-rbac-audit/features/feature-user-auth.md#4-api-endpoints)
* [APIs - System Audit Log](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/001-security-auth-rbac-audit/features/feature-system-audit-logging.md#4-api-endpoints)

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
*Vui lòng xem chi tiết kịch bản kiểm thử tại các tài liệu đặc tả tính năng:*
* [Acceptance - Admin System Config](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/001-security-auth-rbac-audit/features/feature-admin-system-config.md#5-acceptance-criteria)
* [Acceptance - Admin Auth & RBAC](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/001-security-auth-rbac-audit/features/feature-admin-auth-rbac.md#5-acceptance-criteria)
* [Acceptance - User Auth](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/001-security-auth-rbac-audit/features/feature-user-auth.md#5-acceptance-criteria)
* [Acceptance - System Audit Log](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/001-security-auth-rbac-audit/features/feature-system-audit-logging.md#5-acceptance-criteria)

## 9. Out of Scope

- OAuth2 / SSO integration (future)
- LDAP / Active Directory sync
- Two-factor authentication
- IP whitelisting or geo-restrictions
- User self-registration
- Password expiration policy
