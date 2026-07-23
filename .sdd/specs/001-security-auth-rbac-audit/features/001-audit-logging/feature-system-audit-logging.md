# Feature: Nhật ký Hoạt động Hệ thống (Audit Log)

## 1. Context and Goal

Để đảm bảo tính minh bạch, kiểm soát rủi ro, và ghi nhận dấu vết giao dịch, hệ thống tự động ghi nhật ký (Audit Log) cho mọi hành động người dùng thực hiện làm thay đổi dữ liệu hoặc trạng thái trong hệ thống.

Audit Log là bằng chứng nghiệp vụ bất biến: sau khi được ghi, không người dùng nào, kể cả System Admin, được sửa hoặc xóa bản ghi log.

## Clarifications

### Session 2026-07-22

- Q: Which audit action names are canonical? -> A: Use the existing backend `AuditAction.java` enum values as the canonical action list.
- Q: Which filters are required for Sprint 1 audit log queries? -> A: Only warehouse and date/time range filters are supported.
- Q: How should non-mutation audit events store before/after state? -> A: Leave old/new changed-field payloads empty when no entity fields changed.
- Q: Can entity reference fields be null for audit events without a natural entity row? -> A: Yes, `entity_type` and `entity_id` are nullable.
- Q: How granular is one audit log entry? -> A: One entry summarizes all changed fields for the affected entity.
- Q: How are sensitive field changes represented? -> A: Keep the field name but omit before/after values.
- Q: What is the unfiltered browsing limit? -> A: Show fixed 30-row pages for up to 50 pages; older searches require a time filter with minimum 1-hour range increments.
- Q: Can CEO query audit logs? -> A: No; CEO can be audited as actor only.
- Q: Should read-only report/dashboard views be audited? -> A: No, view-only report/dashboard access is not audited in Sprint 1.

## 2. Actors

- **Người dùng hệ thống**: Thực hiện thao tác trong hệ thống; các thao tác thay đổi dữ liệu hoặc trạng thái sẽ được ghi audit log.
- **System Admin**: Được xem và tra cứu toàn bộ audit log.
- **CEO**: Không được xem audit log; CEO chỉ là actor bị ghi audit khi thực hiện thao tác thuộc phạm vi audit.

## 3. Functional Requirements (EARS)

- **Ubiquitous:**
  - The system SHALL always record an audit log entry for every user action that creates, updates, approves, rejects, cancels, soft-deletes, assigns, unassigns, or changes the status of data in the system.
  - The system SHALL record authentication state-changing actions, including login and logout.
  - The system SHALL NOT record read-only view actions in audit log, including ordinary list/detail screens, finance/analytics reports, CEO dashboards, and cross-warehouse dashboards.
  - The system SHALL NOT record export actions for read-only screens, reports, or dashboards in audit log.
  - The system SHALL store only fields that changed between the before state and after state; if no entity field changed, before/after changed-field payloads SHALL be empty.
  - The system SHALL NOT store sensitive information in audit log, including passwords, password hashes, JWT tokens, refresh tokens, credentials, secrets, or API keys.
  - The system SHALL keep sensitive changed field names in audit log while omitting their before/after values.
  - The system SHALL NOT allow any user, including System Admin, to update or delete an audit log entry after it has been created.
- **Event-driven:**
  - WHEN a user's profile, role, active status, or warehouse assignment changes, the system SHALL log the changed fields with before/after values.
  - WHEN warehouse, inventory, receipt, QC, putaway, issue, transfer, stocktake, adjustment, master data, system configuration, finance, return, scrap, or disposal data changes, the system SHALL create an audit log entry for the changed fields.
- **Role-based access:**
  - WHEN a user requests audit log data, the system SHALL allow access only if the user role is System Admin.
  - WHEN a user without System Admin role requests audit log data, the system SHALL reject the request with `FORBIDDEN_AUDIT_ACCESS`.
- **Query and pagination:**
  - WHEN System Admin opens audit log without filters, the system SHALL return page 1 with 30 newest log entries ordered by timestamp descending.
  - WHEN System Admin opens or changes an audit log page, the client SHALL request that page from the backend instead of paginating only the first loaded result set.
  - The audit log page response SHALL include `data`, `page`, `pageSize`, `totalItems`, `totalPages`, `hasNext`, `hasPrevious`, and `requiresFilterForOlder`.
  - WHEN System Admin navigates audit log without filters, the system SHALL allow browsing up to 50 pages of newest logs with a fixed page size of 30 entries.
  - WHEN System Admin needs logs older than the newest 1,500 log entries, the system SHALL require a time filter.
  - WHEN System Admin filters audit log by time range, the `from` to `to` duration SHALL be at least 1 hour and SHALL increase only in 1-hour increments.
  - WHEN System Admin filters audit log, the system SHALL support optional filters only by time range and warehouse.

## 4. Audited Scope

### Audited actions

The system SHALL use the existing backend `AuditAction.java` enum as the canonical audit action list. Generic action categories include:

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

- Viewing ordinary list or detail screens.
- Searching or filtering data on ordinary list/detail screens.

The system SHALL NOT create audit log entries for:

- Viewing finance/analytics reports (Aging Report, P&L, Inventory Valuation) or CEO/cross-warehouse dashboards.
- Exporting a read-only report or dashboard to file.

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

- Each audit log entry SHALL include `actor_id`, `actor_role`, `action`, `timestamp`, and changed fields; `entity_type` and `entity_id` are included when the action has a natural affected entity row.
- `actor_id` SHALL reference the authenticated user who performed the action.
- `action` SHALL be one of the values defined in backend `AuditAction.java`.
- `entity_type` and `entity_id` MAY be null when the audited action does not have a natural affected entity row.
- The system SHALL NOT create audit log entries for background jobs unless the action is represented by the existing `AuditAction.java` enum and an authenticated or provisioned actor is available.
- Changed fields SHALL be stored as one aggregated field-level before/after payload for the affected entity.
- If no entity field changed, `old_value` and `new_value` SHALL be empty.
- If a changed field is sensitive, the system SHALL include the field name but omit its before/after values.
- Audit log entries SHALL be append-only and immutable.

## 6. API Endpoints

- `GET /api/v1/admin/audit-logs` - Tra cứu nhật ký hệ thống.
  - Access: System Admin only.
  - Default sort: `timestamp DESC`.
  - Fixed page size: 30 entries.
  - Default browsing limit without filters: 50 pages / newest 1,500 entries.
  - Optional filters: `from`, `to`, `warehouse_id`.
  - Time filter rule: `from` to `to` duration must be at least 1 hour and increase in 1-hour increments.
- `GET /api/v1/admin/audit-logs/{id}` - Xem chi tiết một bản ghi audit log.
  - Access: System Admin only.

## 7. Error Handling

| Error                  | HTTP | Condition                                                                   |
| ---------------------- | ---- | --------------------------------------------------------------------------- |
| FORBIDDEN_AUDIT_ACCESS | 403  | User is not System Admin                                                    |
| AUDIT_LOG_NOT_FOUND    | 404  | Audit log entry does not exist                                              |
| INVALID_DATE_RANGE     | 400  | `from` is after `to` or date format is invalid                              |
| QUERY_RANGE_TOO_LARGE  | 400  | User requests logs outside the newest 1,500 entries without a time filter   |
| VALIDATION_ERROR       | 400  | Pagination or filter parameter is invalid                                   |

## 8. Acceptance Criteria

**Scenario: Audit Log Creation for Data Change**

- Given a valid authenticated session
- When the user creates or modifies any audited entity
- Then the system SHALL create an audit log entry with actor_id, actor_role, action, timestamp, changed fields, and entity_type/entity_id when applicable.

**Scenario: Field-Level Before/After Values**

- Given an existing audited entity
- When the user changes one or more fields
- Then the audit log SHALL store only the changed fields with before and after values.

**Scenario: Sensitive Data Exclusion**

- Given a user changes a password, token, credential, secret, or API key value
- When the system records the audit log
- Then the audit log SHALL keep the sensitive field name but omit the before and after values.

**Scenario: Immutable Audit Log**

- Given an audit log entry already exists
- When any user, including System Admin, attempts to update or delete the audit log entry
- Then the system SHALL reject the operation and preserve the original audit log entry unchanged.

**Scenario: Audit Log Access Allowed**

- Given the authenticated user is System Admin
- When the user requests `GET /api/v1/admin/audit-logs`
- Then the system SHALL return audit logs ordered by newest timestamp first.

**Scenario: Audit Log Access Forbidden**

- Given the authenticated user is not System Admin
- When the user requests audit log data
- Then the system SHALL reject the request with `FORBIDDEN_AUDIT_ACCESS`.

**Scenario: Default Pagination**

- Given the authenticated user is System Admin
- When the user opens audit log without filters
- Then the system SHALL return 30 newest log entries on page 1 with total item and total page metadata.

**Scenario: Server-Side Page Navigation**

- Given the authenticated user is System Admin
- When the user navigates from page 1 to page 2
- Then the client SHALL request page 2 from the backend and display those audit log entries instead of slicing page 1 locally.

**Scenario: Browse Limit Without Filters**

- Given the authenticated user is System Admin
- When the user requests logs beyond page 50 without filters
- Then the system SHALL reject the request with `QUERY_RANGE_TOO_LARGE`.

**Scenario: Time or Warehouse Filter**

- Given the authenticated user is System Admin
- When the user filters audit log by time range of at least 1 hour in 1-hour increments or by warehouse
- Then the system SHALL return matching audit log entries ordered by newest timestamp first.

**Scenario: Read and Export Actions Are Not Audited**

- Given a valid authenticated session
- When the user only views, searches, filters, or exports data on an ordinary list/detail screen, report, or dashboard
- Then the system SHALL NOT create an audit log entry for that action.
