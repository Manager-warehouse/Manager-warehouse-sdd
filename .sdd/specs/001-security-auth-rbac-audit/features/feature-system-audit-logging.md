# Feature: Nhật ký Hoạt động Hệ thống (Audit Log)

## 1. Context and Goal

Để đảm bảo tính minh bạch, kiểm soát rủi ro, và ghi nhận dấu vết giao dịch, hệ thống tự động ghi nhật ký (Audit Log) cho mọi hành động người dùng thực hiện làm thay đổi dữ liệu hoặc trạng thái trong hệ thống.

Audit Log là bằng chứng nghiệp vụ bất biến: sau khi được ghi, không người dùng nào, kể cả System Admin, được sửa hoặc xóa bản ghi log.

## 2. Actors

- **Người dùng hệ thống**: Thực hiện thao tác trong hệ thống; các thao tác thay đổi dữ liệu hoặc trạng thái sẽ được ghi audit log.
- **System Admin**: Được xem và tra cứu toàn bộ audit log.
- **CEO**: Không được xem audit log.

## 3. Functional Requirements (EARS)

- **Ubiquitous:**
  - The system SHALL always record an audit log entry for every user action that creates, updates, approves, rejects, cancels, soft-deletes, assigns, unassigns, or changes the status of data in the system.
  - The system SHALL record authentication state-changing actions, including login and logout.
  - The system SHALL NOT record read-only view actions or export actions in audit log.
  - The system SHALL store only fields that changed between the before state and after state.
  - The system SHALL NOT store sensitive information in audit log, including passwords, password hashes, JWT tokens, refresh tokens, credentials, secrets, or API keys.
  - The system SHALL NOT allow any user, including System Admin, to update or delete an audit log entry after it has been created.
- **Event-driven:**
  - WHEN a user's profile, role, active status, or warehouse assignment changes, the system SHALL log the changed fields with before/after values.
  - WHEN warehouse, inventory, receipt, QC, putaway, issue, transfer, stocktake, adjustment, master data, system configuration, finance, return, scrap, or disposal data changes, the system SHALL create an audit log entry for the changed fields.
- **Role-based access:**
  - WHEN a user requests audit log data, the system SHALL allow access only if the user role is System Admin.
  - WHEN a user without System Admin role requests audit log data, the system SHALL reject the request with `FORBIDDEN_AUDIT_ACCESS`.
- **Query and pagination:**
  - WHEN System Admin opens audit log without filters, the system SHALL return page 1 with 30 newest log entries ordered by timestamp descending.
  - WHEN System Admin navigates audit log without filters, the system SHALL allow browsing up to 50 pages of newest logs.
  - WHEN System Admin needs logs older than the default 50-page window, the system SHALL require filters to narrow the query.
  - WHEN System Admin filters audit log, the system SHALL support optional filters by time range and warehouse.

## 4. Audited Scope

### Audited actions

The system SHALL audit the following action categories:

- `LOGIN`
- `LOGOUT`
- `CREATE`
- `UPDATE`
- `STATUS_CHANGE`
- `APPROVE`
- `REJECT`
- `CANCEL`
- `SOFT_DELETE`
- `ASSIGN`
- `UNASSIGN`

### Non-audited actions

The system SHALL NOT create audit log entries for:

- Viewing list or detail screens.
- Searching or filtering data.
- Exporting data or reports.

### Audited entity groups

The system SHALL audit changes to these entity groups:

- Authentication and user profile data.
- User role and warehouse assignment data.
- System configuration and approval threshold data.
- Master data, including product, warehouse, bin, supplier, dealer, vehicle, and driver data.
- Warehouse operations, including receipt, QC, putaway, issue, transfer, stocktake, and adjustment data.
- Finance and pricing data.
- Return, scrap, and disposal data.

## 5. Audit Log Data Rules

- Each audit log entry SHALL include `actor_id`, `actor_role`, `action`, `entity_type`, `entity_id`, `timestamp`, and changed fields.
- `actor_id` SHALL reference the authenticated user who performed the action.
- The system SHALL NOT create audit log entries for background jobs because the system does not perform autonomous business actions.
- Changed fields SHALL be stored as field-level before/after values.
- If a changed field is sensitive, the system SHALL omit that field from audit log instead of masking or storing it.
- Audit log entries SHALL be append-only and immutable.

## 6. API Endpoints

- `GET /api/v1/audit-logs` - Tra cứu nhật ký hệ thống.
  - Access: System Admin only.
  - Default sort: `timestamp DESC`.
  - Default page size: 30 entries.
  - Default browsing limit without filters: 50 pages.
  - Optional filters: `from`, `to`, `warehouse_id`.
- `GET /api/v1/audit-logs/{id}` - Xem chi tiết một bản ghi audit log.
  - Access: System Admin only.

## 7. Error Handling

| Error                  | HTTP | Condition                                                                   |
| ---------------------- | ---- | --------------------------------------------------------------------------- |
| FORBIDDEN_AUDIT_ACCESS | 403  | User is not System Admin                                                    |
| AUDIT_LOG_NOT_FOUND    | 404  | Audit log entry does not exist                                              |
| INVALID_DATE_RANGE     | 400  | `from` is after `to` or date format is invalid                              |
| QUERY_RANGE_TOO_LARGE  | 400  | User requests logs outside the default 50-page window without a time filter |
| VALIDATION_ERROR       | 400  | Pagination or filter parameter is invalid                                   |

## 8. Acceptance Criteria

**Scenario: Audit Log Creation for Data Change**

- Given a valid authenticated session
- When the user creates or modifies any audited entity
- Then the system SHALL create an audit log entry with actor_id, actor_role, action, entity_type, entity_id, timestamp, and changed fields.

**Scenario: Field-Level Before/After Values**

- Given an existing audited entity
- When the user changes one or more fields
- Then the audit log SHALL store only the changed fields with before and after values.

**Scenario: Sensitive Data Exclusion**

- Given a user changes a password, token, credential, secret, or API key value
- When the system records the audit log
- Then the audit log SHALL omit the sensitive field value.

**Scenario: Immutable Audit Log**

- Given an audit log entry already exists
- When any user, including System Admin, attempts to update or delete the audit log entry
- Then the system SHALL reject the operation and preserve the original audit log entry unchanged.

**Scenario: Audit Log Access Allowed**

- Given the authenticated user is System Admin
- When the user requests `GET /api/v1/audit-logs`
- Then the system SHALL return audit logs ordered by newest timestamp first.

**Scenario: Audit Log Access Forbidden**

- Given the authenticated user is not System Admin
- When the user requests audit log data
- Then the system SHALL reject the request with `FORBIDDEN_AUDIT_ACCESS`.

**Scenario: Default Pagination**

- Given the authenticated user is System Admin
- When the user opens audit log without filters
- Then the system SHALL return 30 newest log entries on page 1.

**Scenario: Browse Limit Without Filters**

- Given the authenticated user is System Admin
- When the user requests logs beyond page 50 without filters
- Then the system SHALL reject the request with `QUERY_RANGE_TOO_LARGE`.

**Scenario: Time or Warehouse Filter**

- Given the authenticated user is System Admin
- When the user filters audit log by time range or warehouse
- Then the system SHALL return matching audit log entries ordered by newest timestamp first.

**Scenario: Read and Export Actions Are Not Audited**

- Given a valid authenticated session
- When the user only views, searches, filters, or exports data
- Then the system SHALL NOT create an audit log entry for that action.
