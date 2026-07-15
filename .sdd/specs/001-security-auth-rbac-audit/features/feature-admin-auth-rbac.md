# Feature: System Admin Quản trị Tài khoản & Phân quyền Cách ly Kho (US-WMS-21)

## 1. Context and Goal
Để bảo mật hệ thống và cô lập dữ liệu nghiệp vụ giữa 3 kho (Hải Phòng, Hà Nội, Hồ Chí Minh), System Admin cần gán người dùng vào từng Vai trò (Role) và gán phạm vi Chi nhánh Kho cụ thể. Việc này ngăn chặn nhân viên kho Hải Phòng can thiệp vào kho Hà Nội và ngược lại.

## 2. Actors
* **System Admin**: Quản trị tài khoản, phân quyền vai trò và phân kho hoạt động cho nhân viên.

## 3. Functional Requirements (EARS)
* **Event-driven:**
  * WHEN a System Admin creates or modifies a user, the system SHALL validate username uniqueness, valid email format, and password strength.
  * WHEN a System Admin assigns a user to a specific warehouse, the system SHALL authorize that user to perform transactions only within that warehouse.
* **Optional:**
  * WHERE the user has `ADMIN` role, the system SHALL bypass warehouse checks and allow access to all warehouses.
  * WHERE a user has the `CEO` role, the system SHALL allow view-only access to all dashboards and reports across all warehouses.

## 4. API Endpoints
* `GET /api/v1/admin/users` - Danh sách tài khoản người dùng kèm vai trò và kho được gán.
* `POST /api/v1/admin/users` - Tạo tài khoản người dùng mới.
* `PUT /api/v1/admin/users/{id}` - Cập nhật thông tin tài khoản, vai trò và kho được gán.
* `DELETE /api/v1/admin/users/{id}` - Xóa mềm người dùng (`is_active = false`).

## 5. Acceptance Criteria

**Scenario: User Warehouse Assignment and Isolation**
* Given a user with role `STOREKEEPER` assigned only to warehouse "Ha Noi"
* When the user attempts to access or modify resources with warehouse_id = "HCM"
* Then the system SHALL return 403 Forbidden.
