# Feature: Nhật ký Hoạt động Hệ thống (Audit Log)

## 1. Context and Goal
Để đảm bảo tính minh bạch, kiểm soát rủi ro, và ghi nhận dấu vết giao dịch, hệ thống tự động ghi nhật ký (Audit Log) cho mọi hành động tạo, sửa, phê duyệt, hủy hoặc xóa trên các thực thể nghiệp vụ kho.

## 2. Actors
* **System**: Hệ thống tự động ghi log khi người dùng thực hiện các giao dịch nghiệp vụ kho.
* **System Admin / CEO**: Xem và tra cứu toàn bộ lịch sử nhật ký.
* **Warehouse Manager**: Xem nhật ký thuộc kho mình được phân công quản lý.
* **Accountant**: Xem nhật ký liên quan đến các phiếu/đơn nghiệp vụ kho.

## 3. Functional Requirements (EARS)

### 3.1 Ghi log tự động
* **Ubiquitous:**
  * The system SHALL always record an audit log entry for every CREATE, UPDATE, DELETE, APPROVE, REJECT, or CANCEL operation on warehouse business entities.
* **Event-driven:**
  * WHEN a warehouse business entity is created or modified, the system SHALL capture only the changed fields in `old_value` and `new_value` (diff-only), excluding sensitive fields (`password_hash`, etc.).
  * WHEN a Transfer operation occurs, the system SHALL create 2 audit log entries — one for the source warehouse (outbound) and one for the destination warehouse (inbound).

### 3.2 Phạm vi log (entity_type)
Chỉ log các entity liên quan trực tiếp đến nghiệp vụ kho (hàng hóa, phiếu, đơn):

| entity_type | Mô tả |
|---|---|
| `RECEIPT` | Phiếu nhập kho |
| `ISSUE` | Phiếu xuất kho |
| `TRANSFER` | Phiếu điều chuyển liên kho |
| `ADJUSTMENT` | Phiếu điều chỉnh tồn kho |
| `STOCKTAKE` | Phiếu kiểm kê |
| `DELIVERY_ORDER` | Đơn giao hàng |
| `BATCH` | Lô hàng |
| `INVENTORY` | Tồn kho (số lượng thay đổi) |
| `RETURN` | Phiếu trả hàng |
| `SCRAP_DISPOSAL` | Phiếu hủy/thanh lý |
| `TRIP` | Chuyến xe giao hàng |

**Không log**: Login/logout, user management, system config, master data (product, warehouse, bin, supplier, dealer), finance (invoice, payment, price list).

### 3.3 Mô tả hành động (description)
* **Ubiquitous:**
  * The system SHALL auto-generate the `description` field using the format: `{ACTION} {ENTITY_TYPE} {ENTITY_CODE}`.
  * Examples: `"CREATE RECEIPT PN-2026-001"`, `"APPROVE TRANSFER TC-2026-003"`.

### 3.4 Dữ liệu diff (old_value / new_value)
* Chỉ lưu **các field đã thay đổi** (diff-only), không lưu toàn bộ object.
* Với action `CREATE`: `old_value = null`, `new_value = {các field khởi tạo}`.
* Với action `DELETE` (soft-delete): `old_value = {"status": "ACTIVE"}`, `new_value = {"status": "CANCELLED"}`.
* **Sensitive fields** (`password_hash`, v.v.) phải được **loại bỏ** khỏi JSONB trước khi ghi.

### 3.5 actor_id — bắt buộc NOT NULL
* Mọi hành động trong hệ thống đều do người dùng thao tác, không có system job tự động.
* `actor_id` luôn NOT NULL — mỗi log entry phải gắn với một người dùng cụ thể.

### 3.6 Tính bất biến (Immutability)
* Audit log entries KHÔNG được UPDATE hoặc DELETE sau khi tạo.
* Dữ liệu log được lưu trữ vĩnh viễn, không archive, không xóa, không nén.

## 4. API Endpoints

### `GET /api/v1/audit-logs` — Tra cứu nhật ký hệ thống

**Authorization**: ADMIN, CEO, WAREHOUSE_MANAGER, ACCOUNTANT

**Query Parameters:**

| Parameter | Type | Required | Default | Mô tả |
|---|---|---|---|---|
| `cursor` | Long | No | null | ID của record cuối cùng từ page trước (cursor-based pagination) |
| `size` | Integer | No | 30 | Số records mỗi trang |
| `actorId` | Long | No | null | Lọc theo người thực hiện |
| `entityType` | String | No | null | Lọc theo loại entity |
| `action` | String | No | null | Lọc theo loại hành động |
| `warehouseId` | Long | No | null | Lọc theo kho |
| `startDate` | LocalDate | No | 7 ngày trước | Ngày bắt đầu (inclusive, 00:00:00) |
| `endDate` | LocalDate | No | hôm nay | Ngày kết thúc (inclusive, 23:59:59.999999) |

**Sorting:** Mặc định `timestamp DESC` (mới nhất lên trên cùng).

**Date range xử lý tại Backend:**
* Nếu frontend không truyền `startDate` / `endDate` → mặc định lấy **7 ngày gần nhất**.
* `endDate` phải được chuyển thành cuối ngày: `endDate.atTime(23, 59, 59)` trước khi query.

**Access Control (RBAC):**
* `ADMIN` / `CEO`: Xem toàn bộ log.
* `WAREHOUSE_MANAGER`: Chỉ xem log có `warehouse_id` thuộc kho được phân công.
* `ACCOUNTANT`: Xem toàn bộ log (cần dữ liệu nghiệp vụ cho đối soát).

**Response DTO:**

```json
{
  "data": [
    {
      "id": 1234,
      "actorId": 5,
      "actorName": "Nguyễn Văn A",
      "actorRole": "STOREKEEPER",
      "action": "APPROVE",
      "entityType": "RECEIPT",
      "entityId": 101,
      "description": "APPROVE RECEIPT PN-2026-001",
      "warehouseId": 2,
      "oldValue": {"status": "PENDING_APPROVAL"},
      "newValue": {"status": "APPROVED"},
      "timestamp": "2026-06-02T14:30:00+07:00",
      "ipAddress": "192.168.1.100"
    }
  ],
  "nextCursor": 1233,
  "hasNext": true
}
```

## 5. Data Model — audit_logs

| Field | Type | Notes |
|-------|------|-------|
| id | BIGSERIAL (PK) | Auto-increment |
| actor_id | BIGINT (FK) | REFERENCES users(id), **NOT NULL** |
| actor_role | VARCHAR(50) | NOT NULL, snapshot role tại thời điểm thực hiện |
| action | VARCHAR(50) | NOT NULL, CHECK (action IN ('CREATE','UPDATE','APPROVE','REJECT','CANCEL','DELETE')) |
| entity_type | VARCHAR(100) | NOT NULL |
| entity_id | BIGINT | NOT NULL |
| description | TEXT | NOT NULL, auto-generated: "{ACTION} {ENTITY_TYPE} {ENTITY_CODE}" |
| warehouse_id | BIGINT (FK) | REFERENCES warehouses(id), nullable cho entity không thuộc kho cụ thể |
| old_value | JSONB | Chỉ chứa các field đã thay đổi (diff-only), null khi CREATE |
| new_value | JSONB | Chỉ chứa các field đã thay đổi (diff-only) |
| timestamp | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() |
| ip_address | VARCHAR(45) | Client IP address |

**Indexes:**
* `idx_audit_logs_timestamp` ON (timestamp DESC) — sort + date range filter
* `idx_audit_logs_actor_id` ON (actor_id) — filter by actor
* `idx_audit_logs_entity` ON (entity_type, entity_id) — filter by entity
* `idx_audit_logs_warehouse_id` ON (warehouse_id) — filter by warehouse

**Constraints:**
* Không có UPDATE hoặc DELETE permission trên bảng này (immutable).
* Sensitive fields (password_hash, v.v.) KHÔNG được xuất hiện trong old_value/new_value.

## 6. Acceptance Criteria

**Scenario 1: Audit Log Creation on Receipt Approval**
* Given a WAREHOUSE_MANAGER approves receipt PN-2026-001
* When the system processes the approval
* Then an audit log entry SHALL be created with:
  - actor_id = user who approved
  - actor_role = "WAREHOUSE_MANAGER"
  - action = "APPROVE"
  - entity_type = "RECEIPT"
  - entity_id = receipt ID
  - description = "APPROVE RECEIPT PN-2026-001"
  - warehouse_id = warehouse of the receipt
  - old_value = `{"status": "PENDING_APPROVAL"}`
  - new_value = `{"status": "APPROVED"}`

**Scenario 2: Transfer Creates 2 Log Entries**
* Given a STOREKEEPER ships a transfer from Hà Nội to Hồ Chí Minh
* When the transfer is confirmed
* Then the system SHALL create 2 audit log entries:
  - Entry 1: warehouse_id = Hà Nội, action = "CREATE", description includes outbound context
  - Entry 2: warehouse_id = Hồ Chí Minh, action = "CREATE", description includes inbound context

**Scenario 3: Cursor-based Pagination**
* Given 100 audit log entries exist
* When a user calls `GET /api/v1/audit-logs?size=30`
* Then the system SHALL return 30 entries sorted by timestamp DESC, with `nextCursor` and `hasNext = true`.

**Scenario 4: Default Date Range**
* Given no startDate/endDate parameters are provided
* When a user calls `GET /api/v1/audit-logs`
* Then the system SHALL return logs from the last 7 days only.

**Scenario 5: RBAC Access Control**
* Given a WAREHOUSE_MANAGER assigned only to warehouse "Ha Noi"
* When they call `GET /api/v1/audit-logs`
* Then the system SHALL return only log entries where warehouse_id matches "Ha Noi".

**Scenario 6: Sensitive Field Exclusion**
* Given a user updates another user's profile (if logged)
* When the audit log is created
* Then the old_value and new_value SHALL NOT contain `password_hash` or any sensitive credential field.

**Scenario 7: Immutability**
* Given an existing audit log entry
* When any attempt to UPDATE or DELETE the entry is made
* Then the system SHALL reject the operation (no API or database path allows mutation).
