# Feature: System Admin Quản trị Tài khoản & Phân quyền Cách ly Kho (US-WMS-21)

## 1. Context and Goal
Để bảo mật hệ thống và cô lập dữ liệu nghiệp vụ giữa 3 kho (Hải Phòng, Hà Nội, Hồ Chí Minh), System Admin cần gán người dùng vào từng Vai trò (Role) và gán phạm vi Chi nhánh Kho cụ thể. Việc này ngăn chặn nhân viên kho Hải Phòng can thiệp vào kho Hà Nội và ngược lại.

## 2. Actors
* **System Admin**: Quản trị tài khoản, phân quyền vai trò và phân kho hoạt động cho nhân viên.

## 3. Functional Requirements (EARS)
* **Event-driven:**
  * WHEN a System Admin creates or modifies a user, the system SHALL validate username uniqueness, valid email format, and password strength.
  * WHEN a System Admin assigns a user to a specific warehouse, the system SHALL authorize that user to perform transactions only within that warehouse.
  * WHEN a System Admin assigns warehouses to a user whose role is NOT 'ADMIN' or 'CEO', the system SHALL enforce that the user can be assigned to at most 1 warehouse, returning a validation error if multiple warehouses are assigned.
  * WHEN a System Admin creates or modifies a user whose role is NOT 'ADMIN' or 'CEO', the system SHALL require exactly 1 warehouse assignment, returning a validation error if no warehouse assignment is provided.

* **Optional:**
  * WHERE the user has `ADMIN` role, the system SHALL bypass warehouse checks and allow access to all warehouses.
  * WHERE the user has `ADMIN` role, the system SHALL inherit all permissions and authority of all other roles (Role Hierarchy: `ADMIN > ALL_OTHER_ROLES`).
  * WHERE the logged-in user has `ADMIN` role, the Frontend system SHALL display all navigation menu items, pages, dashboards, and action buttons without enforcing individual role checks.
  * WHERE a user has the `CEO` role, the system SHALL allow view-only access to all dashboards and reports across all warehouses.
  * WHERE the user has the `WAREHOUSE_MANAGER` role, the system SHALL allow read-only access to the general Dashboard and inventory stock levels (available quantity of products) across all warehouses, while blocking access to detailed internal configurations (e.g., Bins, internal transactions) and all write operations on other warehouses.
  * WHERE the logged-in user has `ADMIN` or `CEO` role, the Frontend system SHALL display a Global Warehouse Selector in the header to allow filtering data for all warehouses or a specific warehouse.
  * WHERE the logged-in user has `WAREHOUSE_MANAGER` role, the Frontend system SHALL default the Global Warehouse Selector to their assigned warehouse and disable it in most pages, but enable selection of other warehouses ONLY on the Dashboard and Inventory Stock view in read-only mode.
  * WHERE the logged-in user has other roles (e.g. Storekeeper, Staff), the Frontend system SHALL lock the active warehouse to the user's assigned warehouse and hide or disable the Global Warehouse Selector.
  * WHERE the user has `ADMIN` or `CEO` role, the system SHALL NOT require nor store mappings in the `user_warehouse_assignments` table, treating them as authorized for all warehouses by default.





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
