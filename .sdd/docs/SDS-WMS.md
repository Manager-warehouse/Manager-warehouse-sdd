**Warehouse Management System (WMS) — Công ty Phúc Anh**

**Software Design Specification**

– Hà Nội, 2026 –

# Record of Changes

| Date       | A\*M, D | In charge    | Change Description                                                                           |
| ---------- | ------- | ------------ | -------------------------------------------------------------------------------------------- |
| 2026-07-14 | A       | WMS Dev Team | Khởi tạo SDS dựa trên backend thực tế `backend/src/main/java/com/wms/`, tương ứng RDS-WMS.md |
| 2026-07-15 | M       | WMS Dev Team | Đồng bộ catalog spec 001–012; Spec 011–012 là quality/testing cross-cutting. SDS này vẫn mô tả chi tiết luồng inbound đại diện của Spec 003. |

_A - Added M - Modified D - Deleted_

---

# I. Overview

## 1. Code Packages

Backend theo layered architecture bắt buộc **Controller → Service → Repository → Entity** (xem `backend/CLAUDE.md`), main package `com.wms`. Package diagram tổng quan:

```
com.wms
 ├── controller ───► service / service.impl ───► repository ───► entity
 │                         │
 │                         ├──► dto.request / dto.response / dto.auth
 │                         ├──► enums (state machines, role, action)
 │                         ├──► exception (typed exceptions)
 │                         ├──► mapper
 │                         └──► util (FIFOSelector, AuditLogUtil, ...)
 ├── config (Security, JWT filter, Flyway, mail, UploadResourceConfig)
 └── aop (cross-cutting behavior khi có)
```

**_Package descriptions_**

| No  | Package                                                             | Description                                                                                                                                                                                                                                                                                                                                                                                                           |
| --- | ------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `com.wms.controller`                                                | REST controllers dưới `/api/v1/...`. Chỉ nhận HTTP request, validate DTO (`@Valid`), trả HTTP status, không chứa business logic. Lấy actor hiện tại qua `CurrentUserService`/Spring Security context.                                                                                                                                                                                                                 |
| 02  | `com.wms.service` / `com.wms.service.impl`                          | Business logic, quản lý transaction (`@Transactional`), authorization (role + warehouse scope), state transition, audit logging, invariant tồn kho/QC/transfer/accounting. Một số service (VD `ReceiptService`, `ReceiptApprovalService`, `QuarantineRtvService`) được implement trực tiếp không tách interface/impl; một số khác (VD `PaymentReceiptService`, `AutoInvoiceService`) tách interface + `*ServiceImpl`. |
| 03  | `com.wms.repository`                                                | Spring Data JPA interface, không dùng raw SQL trong application code.                                                                                                                                                                                                                                                                                                                                                 |
| 04  | `com.wms.entity`                                                    | JPA entity mapping bảng DB và quan hệ, ưu tiên Lombok `@Getter/@Setter/@NoArgsConstructor/@AllArgsConstructor/@Builder`, không dùng `@Data` cho entity có lazy relationship.                                                                                                                                                                                                                                          |
| 05  | `com.wms.dto.request` / `com.wms.dto.response` / `com.wms.dto.auth` | DTO cho request (Jakarta Validation) và response, không trả entity trực tiếp từ controller.                                                                                                                                                                                                                                                                                                                           |
| 06  | `com.wms.enums`                                                     | State machine và constant miền (VD `ReceiptStatus`, `ReceiptType`, `UserRole`, `InterWarehouseTransferStatus`, `AuditAction`, `AuditEntityType`).                                                                                                                                                                                                                                                                     |
| 07  | `com.wms.exception`                                                 | Typed exception + `GlobalExceptionHandler` — mapping lỗi tập trung.                                                                                                                                                                                                                                                                                                                                                   |
| 08  | `com.wms.config`                                                    | Security config, JWT filter, Flyway, mail config, `UploadResourceConfig` cho `/uploads/**`.                                                                                                                                                                                                                                                                                                                           |
| 09  | `com.wms.mapper`                                                    | DTO ⇄ entity mapping helper.                                                                                                                                                                                                                                                                                                                                                                                          |
| 10  | `com.wms.util`                                                      | Utility tập trung, không tạo side-effect nghiệp vụ ẩn (VD `FIFOSelector`, `AuditLogUtil` — build diff before/after, filter sensitive field).                                                                                                                                                                                                                                                                          |
| 11  | `com.wms.aop`                                                       | Cross-cutting concern khi cần (logging, timing...).                                                                                                                                                                                                                                                                                                                                                                   |

Frontend (React 18 + Tailwind, ngoài phạm vi chi tiết của SDS backend này): `components/` (PascalCase), `pages/`, `hooks/` (camelCase), `services/` (API client Axios), `stores/` (Zustand), `utils/`.

## 2. Database Design

### a. Database Schema

PostgreSQL 18, quản lý version qua Flyway (`backend/src/main/resources/db/migration/`), naming convention `snake_case`. Nhóm quan hệ chính (chi tiết ERD từng domain nên vẽ riêng khi cần trình bày trực quan):

```
users ──< user_warehouse_assignments >── warehouses ──< warehouse_locations
warehouses ──< receipts ──< receipt_items >── products ──< batches
receipts ──< adjustments (RETURN_TO_VENDOR) ──< debit_notes
warehouses ──< delivery_orders ──< delivery_order_items ──< delivery_order_item_allocations
delivery_orders ──< deliveries ──< delivery_otp_attempts
warehouses ──< transfers ──< transfer_items
warehouses ──< inventories >── products >── batches >── warehouse_locations
warehouses ──< stocktakes ──< stocktake_items
products ──< price_history
dealers ──< invoices ──< invoice_lines
dealers ──< payment_receipts
dealers ──< credit_notes
all mutating tables ──> audit_logs (append-only, actor_id nullable cho SYSTEM)
```

### b. Table Description

| No  | Table                             | Description                                                                                                                                                                                                                 |
| --- | --------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `users`                           | Tài khoản người dùng, `role` (VARCHAR CHECK theo `UserRole` enum: `ADMIN, CEO, WAREHOUSE_MANAGER, STOREKEEPER, WAREHOUSE_STAFF, ACCOUNTANT, ACCOUNTANT_MANAGER, PLANNER, DISPATCHER, DRIVER`).                              |
| 02  | `user_warehouse_assignments`      | Gán user vào 1+ kho cho RBAC warehouse-scope.                                                                                                                                                                               |
| 03  | `warehouses`                      | 3 kho vật lý (Hải Phòng/Hà Nội/HCM) + kho ảo `IN_TRANSIT`.                                                                                                                                                                  |
| 04  | `warehouse_locations`             | Zone + Bin Location; `is_quarantine`, `capacity`.                                                                                                                                                                           |
| 05  | `products`                        | Danh mục SKU đồ gia dụng — không serial, không hạn dùng, không grade.                                                                                                                                                       |
| 06  | `price_history`                   | Giá vốn/giá bán theo kỳ hiệu lực (`effective_date`/`end_date`).                                                                                                                                                             |
| 07  | `batches`                         | Lô hàng theo product + `receipt_id` + `received_date` — khóa FIFO.                                                                                                                                                          |
| 08  | `inventories`                     | `total_qty`, `reserved_qty`, `version` (optimistic lock); CHECK `total_qty >= 0`, `reserved_qty >= 0`.                                                                                                                      |
| 09  | `receipts`                        | `status` CHECK (`ReceiptStatus`): `PENDING_RECEIPT, DRAFT, QC_COMPLETED, QC_FAILED, APPROVED, RETURN_TO_SUPPLIER_PENDING, RETURNED_TO_SUPPLIER`. Dùng chung cho `type = PURCHASE` (Spec 003) và `type = RETURN` (Spec 009). |
| 10  | `receipt_items`                   | `expected_qty`, `actual_qty`, `over_received_qty`, `qc_result`, `qc_failure_reason`.                                                                                                                                        |
| 11  | `adjustments`                     | `type` CHECK (`DISPOSAL, RETURN_TO_VENDOR, TRANSFER_DISCREPANCY, STOCKTAKE, ...`).                                                                                                                                          |
| 12  | `debit_notes`                     | Chứng từ đòi bồi hoàn NCC khi RTV, liên kết `receipt_id`/`supplier_id`.                                                                                                                                                     |
| 13  | `quarantine_records`              | Bản ghi hàng lỗi trong Quarantine, giữ `origin` (`RECEIPT_QC_FAIL`/`INTERNAL_TRANSFER`) để truy vết.                                                                                                                        |
| 14  | `delivery_orders`                 | `status`: `NEW, WAITING_PICKING, QC_PENDING_APPROVAL, QC_COMPLETED, WAREHOUSE_APPROVED, IN_TRANSIT, COMPLETED, RETURNED, CLOSED, REJECTED, CANCELLED`.                                                                      |
| 15  | `delivery_order_items`            | Dòng hàng DO, `unit_price` snapshot tại thời điểm tạo DO.                                                                                                                                                                   |
| 16  | `delivery_order_item_allocations` | Kế hoạch lấy hàng theo `batch_id`/`location_id`/`zone_id`, có `status`/`version`.                                                                                                                                           |
| 17  | `trips`                           | `trip_type` (`DELIVERY`/`TRANSFER`), `vehicle_id`, `driver_id`, `warehouse_id`.                                                                                                                                             |
| 18  | `deliveries`                      | Attempt giao vật lý của 1 DO, `attempt_number`, `status` (`IN_TRANSIT/DELIVERED/FAILED`).                                                                                                                                   |
| 19  | `delivery_otp_attempts`           | Chỉ lưu hash/verifier OTP, `expires_at`, `attempt_count`, `status`.                                                                                                                                                         |
| 20  | `transfers`                       | `status` CHECK: `NEW, APPROVED, REJECTED, IN_TRANSIT, COMPLETED, COMPLETED_WITH_DISCREPANCY, CANCELLED, QUARANTINED`.                                                                                                       |
| 21  | `transfer_items`                  | `sent_qty`, `received_qty`, `qc_passed_qty`, `qc_failed_qty`.                                                                                                                                                               |
| 22  | `stocktakes` / `stocktake_items`  | Phiếu kiểm kê và dòng đếm thực tế, tính Variance.                                                                                                                                                                           |
| 23  | `dealers`                         | `current_balance`, `credit_limit`, `credit_status` (`ACTIVE`/`CREDIT_HOLD`), `payment_term_days`.                                                                                                                           |
| 24  | `suppliers`                       | Hồ sơ NCC phục vụ Receipt/RTV/Debit Note.                                                                                                                                                                                   |
| 25  | `vehicles` / `drivers`            | Xe tải và tài xế nội bộ, phạm vi kho hoạt động.                                                                                                                                                                             |
| 26  | `invoices` / `invoice_lines`      | Hóa đơn tự động tạo khi DO `COMPLETED`, `issue_date`, `due_date = issue_date+30`.                                                                                                                                           |
| 27  | `payment_receipts`                | Phiếu thu, cấn trừ hóa đơn.                                                                                                                                                                                                 |
| 28  | `credit_notes`                    | Ghi giảm công nợ khi hàng hoàn trả.                                                                                                                                                                                         |
| 29  | `billing_notifications`           | Worklist đối chiếu DO đã giao (không phải bước bắt buộc tạo invoice).                                                                                                                                                       |
| 30  | `accounting_periods`              | Kỳ kế toán, `status` (`OPEN`/`CLOSED`).                                                                                                                                                                                     |
| 31  | `damage_reports`                  | Biên bản hư hỏng phục vụ tiêu hủy.                                                                                                                                                                                          |
| 32  | `stock_alerts`                    | Cảnh báo tồn kho thấp tự động, unique `(warehouse_id, product_id, alert_type, is_resolved=false)`.                                                                                                                          |
| 33  | `system_configs`                  | Tham số hệ thống dạng key-value có kiểm soát.                                                                                                                                                                               |
| 34  | `audit_logs`                      | `actor_id` (nullable), `actor_role`, `action`, `entity_type`, `entity_id`, `timestamp`, `old_value`, `new_value` (JSON); append-only.                                                                                       |

---

# II. Code Designs

> **Phạm vi phần này:** thiết kế chi tiết Class Diagram / Class Specifications / Sequence Diagram / Database Queries cho **toàn bộ 10 nhóm nghiệp vụ (Spec 001–010)**, đồng bộ với `docs/RDS-WMS.md`. Các class/method liệt kê dưới đây lấy trực tiếp từ code thực tế tại `backend/src/main/java/com/wms/`, tham chiếu class tương ứng trong `backend/CLAUDE.md` mục "Current Backend Flows". Spec 011–012 (testing/SonarQube) là quality cross-cutting, không có class nghiệp vụ riêng.

## 1. Security, Authentication & RBAC (Spec 001)

### 1.1 System Configuration

#### a. Class Diagram

```
SystemConfigController ──uses──► SystemConfigService ──uses──► SystemConfigRepository ──maps──► SystemConfig (entity)
        │                              │
        └──DTO: UpdateConfigRequest    └──uses──► AuditLogService
```

#### b. Class Specifications

**SystemConfigController Class**

| No  | Method                                       | Description                                                                                                    |
| --- | --------------------------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| 01  | `getConfigs()`                                | `GET /api/v1/admin/system-configs` — role `ADMIN`. Trả toàn bộ tham số hệ thống hiện tại.                    |
| 02  | `updateConfig(String key, UpdateConfigRequest)` | `PUT /api/v1/admin/system-configs/{key}` — role `ADMIN`. Cập nhật 1 tham số (VD `DEFAULT_CREDIT_LIMIT`, `DEFAULT_MIN_STOCK`, `DEFAULT_PAYMENT_TERM_DAYS`, `PERIOD_CLOSE_DAY`). |

**SystemConfigService Class**

| No  | Method                                                     | Description                                                                                                                                                                                                                                                                            |
| --- | ------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 01  | `updateConfig(String key, String newValue, User actor)`      | **Input**: `key` (VD `DEFAULT_CREDIT_LIMIT`), `newValue`, actor (phải `ADMIN`). **Xử lý nội bộ**: `requireAdmin(actor)` → validate `newValue` theo kiểu dữ liệu của key (numeric/date) → `validateRange` (không âm, không vượt ngưỡng hợp lý) → load config cũ để snapshot → `systemConfigRepository.save()` → ghi audit `SYSTEM_CONFIG_UPDATED` với before/after. **Output**: `SystemConfigResponse`. |
| 02 (private) | `requireAdmin` / `validateRange`                    | Kiểm tra role + validate giá trị theo business rule của từng key.                                                                                                                                                                                                                       |

#### c. Sequence Diagram(s)

```
System Admin → SystemConfigController.updateConfig(key, request)
  SystemConfigController → SystemConfigService.updateConfig(key, newValue, actor)
    SystemConfigService → requireAdmin(actor)
    SystemConfigService → validateRange(key, newValue)
    SystemConfigService → systemConfigRepository.findByKey(key)   → old value (snapshot)
    SystemConfigService → systemConfigRepository.save(key, newValue)
    SystemConfigService → auditLogService.log(SYSTEM_CONFIG_UPDATED, before, after)
  SystemConfigService --> SystemConfigController : SystemConfigResponse
SystemConfigController --> System Admin : 200 OK
```

#### d. Database Queries

```sql
-- 1/ Đọc toàn bộ config hiện tại
SELECT config_key, config_value, updated_at FROM system_configs ORDER BY config_key;

-- 2/ Cập nhật 1 tham số
UPDATE system_configs
SET config_value = ?, updated_at = NOW()
WHERE config_key = ?;
```

---

### 1.2 User & Warehouse Assignment Management

#### a. Class Diagram

```
AdminController ──uses──► UserService ──uses──► UserRepository ──maps──► User (entity)
        │                       │                                             └──► UserWarehouseAssignment
        │                       └──uses──► RbacService (role/permission validate)
        └──DTO: CreateUserRequest / UpdateUserRequest ──► UserResponse
```

#### b. Class Specifications

**AdminController Class**

| No  | Method                                              | Description                                                                                              |
| --- | ----------------------------------------------------- | ------------------------------------------------------------------------------------------------------------ |
| 01  | `getUsers(...)`                                     | `GET /api/v1/admin/users` — role `ADMIN`. Trả danh sách user + role + warehouse assignments.               |
| 02  | `createUser(CreateUserRequest request)`             | `POST /api/v1/admin/users` — role `ADMIN`. Input: email, username, password, fullName, role, warehouseIds[]. |
| 03  | `updateUser(Long id, UpdateUserRequest request)`     | `PUT /api/v1/admin/users/{id}` — cập nhật role/warehouse assignment.                                        |
| 04  | `deactivateUser(Long id)`                            | `DELETE /api/v1/admin/users/{id}` — soft-delete, `is_active = false`.                                       |

**UserService Class**

| No  | Method                                                                 | Description                                                                                                                                                                                                                                                                                                                                                                    |
| --- | ------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 01  | `createUser(CreateUserRequest request, User actor)`                    | **Input**: request (email/username unique, password ≥8 ký tự, role hợp lệ trong 10 role, warehouseIds[]), actor (`ADMIN`). **Xử lý nội bộ**: `requireAdmin` → `validateUniqueEmail`/`validateUniqueUsername` → `passwordEncoder.encode` (bcrypt cost ≥12) → lưu `User` (`is_active = true`) → lưu `UserWarehouseAssignment[]` cho mỗi warehouseId → ghi audit `USER_CREATED`. **Output**: `UserResponse`.                                                            |
| 02  | `updateUserAssignment(Long userId, UpdateUserRequest request, User actor)` | **Input**: `userId`, request (role mới, warehouseIds mới), actor. **Xử lý nội bộ**: diff warehouseIds cũ/mới → xóa assignment không còn, thêm assignment mới → cập nhật role nếu đổi → ghi audit `USER_UPDATED` với before/after. **Output**: `UserResponse`.                                                                                                                    |
| 03  | `deactivateUser(Long userId, User actor)`                              | **Input**: `userId`, actor. **Xử lý nội bộ**: set `is_active = false`, không xóa record; ghi audit `USER_DEACTIVATED`. **Output**: `UserResponse`.                                                                                                                                                                                                                              |

#### c. Sequence Diagram(s)

```
System Admin → AdminController.createUser(request)
  AdminController → UserService.createUser(request, actor)
    UserService → requireAdmin(actor)
    UserService → userRepository.existsByEmail(email)        [duplicate check]
    UserService → userRepository.existsByUsername(username)  [duplicate check]
    UserService → passwordEncoder.encode(password)            → bcrypt hash
    UserService → userRepository.save(user)                   → User{is_active=true}
    loop each warehouseId
      UserService → userWarehouseAssignmentRepository.save(userId, warehouseId)
    end
    UserService → auditLogService.log(USER_CREATED, before=null, after=snapshot)
  UserService --> AdminController : UserResponse
AdminController --> System Admin : 201 Created
```

#### d. Database Queries

```sql
-- 1/ Kiểm tra trùng email/username
SELECT EXISTS (SELECT 1 FROM users WHERE email = ?);
SELECT EXISTS (SELECT 1 FROM users WHERE username = ?);

-- 2/ Tạo user
INSERT INTO users (email, username, password_hash, full_name, phone, job_title, role, is_active, created_at)
VALUES (?, ?, ?, ?, ?, ?, ?, true, NOW()) RETURNING id;

-- 3/ Gán warehouse
INSERT INTO user_warehouse_assignments (user_id, warehouse_id, created_at)
VALUES (?, ?, NOW());

-- 4/ Vô hiệu hóa user
UPDATE users SET is_active = false, updated_at = NOW() WHERE id = ?;
```

---

### 1.3 Authentication (Login/JWT)

#### a. Class Diagram

```
AuthController ──uses──► AuthenticationService ──uses──► UserRepository
        │                       │
        │                       ├──uses──► JwtTokenProvider (generate/verify access + refresh token)
        │                       └──uses──► PasswordEncoder (bcrypt)
        └──DTO: LoginRequest / RefreshTokenRequest ──► AuthResponse
```

#### b. Class Specifications

**AuthController Class**

| No  | Method                                    | Description                                                                                 |
| --- | -------------------------------------------- | ----------------------------------------------------------------------------------------------- |
| 01  | `login(LoginRequest request)`               | `POST /api/v1/auth/login` — public. Input: email/password. Output: access + refresh token. |
| 02  | `refresh(RefreshTokenRequest request)`      | `POST /api/v1/auth/refresh` — public. Input: refresh token. Output: access token mới.       |
| 03  | `logout()`                                  | `POST /api/v1/auth/logout` — authenticated. Xóa refresh token hash.                        |
| 04  | `getMe()`                                   | `GET /api/v1/auth/me` — authenticated. Trả thông tin user hiện tại + warehouse assignments. |

**AuthenticationService Class**

| No  | Method                                                | Description                                                                                                                                                                                                                                                                                             |
| --- | -------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 01  | `login(String email, String password)`                  | **Input**: email, password. **Xử lý nội bộ**: `userRepository.findByEmail` (throw `UnauthorizedException` nếu không tồn tại hoặc `is_active=false`) → `passwordEncoder.matches(password, user.passwordHash)` (throw nếu sai) → `jwtTokenProvider.generateAccessToken` (15m) + `generateRefreshToken` (7d) → hash refresh token (SHA-256), lưu `users.refresh_token_hash` + `refresh_token_expires_at` → ghi audit `USER_LOGIN`. **Output**: `AuthResponse{accessToken, refreshToken}`. |
| 02  | `refreshToken(String refreshToken)`                     | **Input**: refresh token (plain). **Xử lý nội bộ**: hash input, so khớp `users.refresh_token_hash` + kiểm tra `refresh_token_expires_at > NOW()` → sinh access token mới. **Output**: `AuthResponse{accessToken}`.                                                                                       |
| 03  | `logout(User actor)`                                    | **Input**: actor hiện tại. **Xử lý nội bộ**: set `refresh_token_hash = NULL`, `refresh_token_expires_at = NULL`. **Output**: void.                                                                                                                                                                        |

#### c. Sequence Diagram(s)

```
User → AuthController.login(request)
  AuthController → AuthenticationService.login(email, password)
    AuthenticationService → userRepository.findByEmail(email)     → User (must be is_active)
    AuthenticationService → passwordEncoder.matches(password, hash)
    AuthenticationService → jwtTokenProvider.generateAccessToken(user)   [15m]
    AuthenticationService → jwtTokenProvider.generateRefreshToken(user)  [7d]
    AuthenticationService → userRepository.save(user{refresh_token_hash})
    AuthenticationService → auditLogService.log(USER_LOGIN)
  AuthenticationService --> AuthController : AuthResponse
AuthController --> User : 200 OK (accessToken, refreshToken)
```

#### d. Database Queries

```sql
-- 1/ Tìm user theo email để login
SELECT id, full_name, email, role, password_hash, is_active
FROM users WHERE email = ? AND is_active = true;

-- 2/ Lưu refresh token hash sau login thành công
UPDATE users
SET refresh_token_hash = ?, refresh_token_expires_at = NOW() + INTERVAL '7 days'
WHERE id = ?;

-- 3/ Verify refresh token khi refresh
SELECT id, role, email FROM users
WHERE refresh_token_hash = ? AND refresh_token_expires_at > NOW();

-- 4/ Logout — xóa refresh token
UPDATE users SET refresh_token_hash = NULL, refresh_token_expires_at = NULL WHERE id = ?;
```

---

### 1.4 Audit Log Query

#### a. Class Diagram

```
AuditLogController ──uses──► AuditLogService ──uses──► AuditLogRepository ──maps──► AuditLog (entity, append-only)
```

#### b. Class Specifications

**AuditLogController Class**

| No  | Method                    | Description                                                                                                            |
| --- | --------------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| 01  | `getAuditLogs(...)`      | `GET /api/v1/admin/audit-logs` — role `ADMIN`. Filter: actor_id, action, entity_type, warehouse_id, from/to date; phân trang. |

**AuditLogService Class**

| No  | Method                                              | Description                                                                                                                                     |
| --- | ------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `queryAuditLogs(AuditLogFilterRequest filter, User actor)` | **Input**: filter (actor_id, action, entity_type, warehouse_id, dateFrom, dateTo, page/size), actor (`ADMIN`). **Xử lý nội bộ**: build dynamic query theo filter non-null → sort `created_at DESC`. **Output**: `Page<AuditLogResponse>`. Read-only, không mutate. |
| 02 (static/util) | `AuditLogUtil.log(action, entityType, entityId, actor, before, after)` | Utility dùng bởi MỌI service khác để ghi audit; build JSON diff before/after, filter field nhạy cảm (password_hash, otp_hash...). |

#### c. Sequence Diagram(s)

```
System Admin → AuditLogController.getAuditLogs(filter)
  AuditLogController → AuditLogService.queryAuditLogs(filter, actor)
    AuditLogService → requireAdmin(actor)
    AuditLogService → auditLogRepository.findByFilters(filter, pageable)
  AuditLogService --> AuditLogController : Page<AuditLogResponse>
AuditLogController --> System Admin : 200 OK
```

#### d. Database Queries

```sql
SELECT al.id, al.actor_id, al.actor_role, al.action, al.entity_type, al.entity_id,
       al.warehouse_id, al.old_value, al.new_value, al.created_at
FROM audit_logs al
WHERE (? IS NULL OR al.actor_id = ?)
  AND (? IS NULL OR al.action = ?)
  AND (? IS NULL OR al.entity_type = ?)
  AND (? IS NULL OR al.warehouse_id = ?)
  AND (? IS NULL OR al.created_at >= ?)
  AND (? IS NULL OR al.created_at <= ?)
ORDER BY al.created_at DESC
LIMIT ? OFFSET ?;
```

---

## 2. Master Data Management (Spec 002)

### 2.1 Product/SKU Management

#### a. Class Diagram

```
ProductController ──uses──► ProductService ──uses──► ProductRepository ──maps──► Product (entity)
        │                          │
        │                          └──uses──► PriceHistoryRepository (COGS lookup)
        └──DTO: CreateProductRequest / UpdateProductRequest ──► ProductResponse
```

#### b. Class Specifications

**ProductController Class**

| No  | Method                                        | Description                                                                              |
| --- | ------------------------------------------------ | ---------------------------------------------------------------------------------------------- |
| 01  | `getProducts(...)`                              | `GET /api/v1/products` — search theo SKU/name, filter `is_active`.                          |
| 02  | `createProduct(CreateProductRequest request)`   | `POST /api/v1/products` — role `STOREKEEPER`/`ADMIN`. SKU unique, immutable sau tạo.       |
| 03  | `updateProduct(Long id, UpdateProductRequest)`  | `PUT /api/v1/products/{id}` — chỉ cho sửa name/unit/weight/volume/reorder_point, KHÔNG sửa SKU. |
| 04  | `deactivateProduct(Long id)`                    | `DELETE /api/v1/products/{id}` — soft-delete `is_active = false`.                          |

**ProductService Class**

| No  | Method                                                     | Description                                                                                                                                                                                                                                                       |
| --- | -------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `createProduct(CreateProductRequest request, User actor)`      | **Input**: request (sku unique, name, unit, weightKg, volumeM3, unitPerPack, reorderPoint), actor (`STOREKEEPER`/`ADMIN`). **Xử lý nội bộ**: `validateUniqueSku` → lưu `Product` (`is_active=true`) → ghi audit `PRODUCT_CREATED`. **Output**: `ProductResponse`.  |
| 02  | `updateProduct(Long id, UpdateProductRequest request, User actor)` | **Input**: `id`, request (chỉ field cho phép sửa). **Xử lý nội bộ**: load product, apply field updates (bỏ qua nếu request cố gửi `sku` khác) → lưu → ghi audit `PRODUCT_UPDATED`. **Output**: `ProductResponse`.                                                    |
| 03  | `deactivateProduct(Long id, User actor)`                        | **Input**: `id`. **Xử lý nội bộ**: kiểm tra product không có transaction đang mở (optional warning) → set `is_active=false` → ghi audit `PRODUCT_DEACTIVATED`. **Output**: void.                                                                                    |

#### c. Sequence Diagram(s)

```
Thủ kho → ProductController.createProduct(request)
  ProductController → ProductService.createProduct(request, actor)
    ProductService → productRepository.existsBySku(sku)   [duplicate check]
    ProductService → productRepository.save(product)      → Product{is_active=true}
    ProductService → auditLogService.log(PRODUCT_CREATED, before=null, after=snapshot)
  ProductService --> ProductController : ProductResponse
ProductController --> Thủ kho : 201 Created
```

#### d. Database Queries

```sql
-- 1/ Danh sách sản phẩm active, search theo sku/name
SELECT id, sku, name, unit, weight_kg, volume_m3, reorder_point
FROM products
WHERE is_active = true AND (sku ILIKE ? OR name ILIKE ?)
ORDER BY created_at DESC;

-- 2/ Kiểm tra SKU trùng
SELECT EXISTS (SELECT 1 FROM products WHERE sku = ?);

-- 3/ Tạo sản phẩm
INSERT INTO products (sku, name, unit, weight_kg, volume_m3, unit_per_pack, reorder_point, is_active, created_at)
VALUES (?, ?, ?, ?, ?, ?, ?, true, NOW()) RETURNING id;

-- 4/ Vô hiệu hóa
UPDATE products SET is_active = false, updated_at = NOW() WHERE id = ?;
```

---

### 2.2 Warehouse Zone & Bin Location Configuration

#### a. Class Diagram

```
WarehouseController ──uses──► WarehouseService ──uses──► WarehouseRepository ──maps──► Warehouse (entity)
BinLocationController ──uses──► BinLocationService ──uses──► WarehouseLocationRepository ──maps──► WarehouseLocation (entity)
                                       │
                                       └──uses──► LocationLockService (lock/unlock khi stocktake)
```

#### b. Class Specifications

**WarehouseController / BinLocationController Class**

| No  | Method                                             | Description                                                                                     |
| --- | ------------------------------------------------------ | ------------------------------------------------------------------------------------------------------ |
| 01  | `getWarehouses()`                                    | `GET /api/v1/warehouses` — danh sách kho vật lý + IN_TRANSIT.                                       |
| 02  | `createZone(Long warehouseId, CreateZoneRequest)`    | `POST /api/v1/warehouses/{id}/locations` (type=ZONE) — role `WAREHOUSE_MANAGER`/`ADMIN`.            |
| 03  | `createBin(Long warehouseId, CreateBinRequest)`      | `POST /api/v1/warehouses/{id}/locations` (type=BIN, parent_id=zone) — kèm capacity_m3/capacity_kg. |
| 04  | `lockLocations(Long warehouseId)`                    | Internal — gọi bởi `StocktakeService` khi bắt đầu kiểm kê.                                          |

**BinLocationService Class**

| No  | Method                                                              | Description                                                                                                                                                                                                                                                       |
| --- | ------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `createLocation(Long warehouseId, CreateLocationRequest request, User actor)` | **Input**: warehouseId, request (code unique trong kho, type ZONE/BIN, parent_id nếu BIN, capacity, is_quarantine), actor (`WAREHOUSE_MANAGER`/`ADMIN`). **Xử lý nội bộ**: `validateUniqueCode` → `validateHierarchy` (BIN phải có parent là ZONE, không cho BIN làm parent) → lưu `WarehouseLocation` → ghi audit `LOCATION_CREATED`. **Output**: `LocationResponse`.  |
| 02  | `lockLocationsForStocktake(Long warehouseId)`                             | **Input**: warehouseId (gọi từ `StocktakeService.startStocktake`). **Xử lý nội bộ**: `UPDATE warehouse_locations SET is_locked=true WHERE warehouse_id=? AND type='BIN' AND is_quarantine=false`. **Output**: void.                                              |
| 03  | `unlockLocationsAfterStocktake(Long warehouseId)`                         | Ngược lại của trên, gọi khi stocktake `CLOSED`/`REJECTED`.                                                                                                                                                                                                          |

#### c. Sequence Diagram(s)

```
Trưởng kho → BinLocationController.createBin(warehouseId, request)
  BinLocationController → BinLocationService.createLocation(warehouseId, request, actor)
    BinLocationService → validateUniqueCode(warehouseId, code)
    BinLocationService → validateHierarchy(parentId, type)
    BinLocationService → warehouseLocationRepository.save(location)
    BinLocationService → auditLogService.log(LOCATION_CREATED, before=null, after=snapshot)
  BinLocationService --> BinLocationController : LocationResponse
BinLocationController --> Trưởng kho : 201 Created
```

#### d. Database Queries

```sql
-- 1/ Kiểm tra code trùng trong kho
SELECT EXISTS (SELECT 1 FROM warehouse_locations WHERE warehouse_id = ? AND code = ?);

-- 2/ Tạo zone/bin
INSERT INTO warehouse_locations (warehouse_id, code, name, type, parent_id, capacity_m3, capacity_kg, is_quarantine, is_active, created_at)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, true, NOW()) RETURNING id;

-- 3/ Lock toàn bộ bin (trừ quarantine) khi bắt đầu stocktake
UPDATE warehouse_locations
SET is_locked = true, updated_at = NOW()
WHERE warehouse_id = ? AND type = 'BIN' AND is_quarantine = false;

-- 4/ Unlock sau stocktake
UPDATE warehouse_locations
SET is_locked = false, updated_at = NOW()
WHERE warehouse_id = ? AND type = 'BIN' AND is_locked = true;
```

---

### 2.3 Dealer/Supplier & Credit Limit Management

#### a. Class Diagram

```
DealerController ──uses──► DealerService ──uses──► DealerRepository ──maps──► Dealer (entity)
SupplierController ──uses──► SupplierService ──uses──► SupplierRepository ──maps──► Supplier (entity)
DealerService ──uses──► CreditLimitService (set limit, check credit_status)
```

#### b. Class Specifications

**DealerController Class**

| No  | Method                                              | Description                                                                                        |
| --- | ------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------- |
| 01  | `createDealer(CreateDealerRequest request)`           | `POST /api/v1/dealers` — role `ACCOUNTANT`. Tạo hồ sơ Đại lý cơ bản, `credit_status = ACTIVE` mặc định. |
| 02  | `updateCreditLimit(Long dealerId, CreditLimitRequest)` | `PUT /api/v1/dealers/{id}/credit-limit` — role `ACCOUNTANT_MANAGER`.                                    |

**CreditLimitService Class**

| No  | Method                                                                    | Description                                                                                                                                                                                                                                                              |
| --- | ------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `updateCreditLimit(Long dealerId, BigDecimal newLimit, Integer paymentTermDays, User actor)` | **Input**: dealerId, newLimit (>0), paymentTermDays (30/60), actor (`ACCOUNTANT_MANAGER`). **Xử lý nội bộ**: `validateRange(newLimit)` → snapshot old value → `dealerRepository.updateLimit()` → ghi audit `CREDIT_LIMIT_UPDATED` với before/after. **Output**: `DealerResponse`. |
| 02  | `checkCreditStatus(Long dealerId, BigDecimal newDoValue)`                       | **Input**: dealerId, newDoValue (giá trị DO sắp tạo). **Xử lý nội bộ**: query `current_balance` + `credit_limit` + overdue invoices (`due_date < NOW() - 30d`) → `IF balance + newDoValue > limit OR hasOverdue THEN return BLOCKED`. **Output**: `CreditCheckResult{allowed, reason}`. Dùng bởi `DeliveryOrderService.createDeliveryOrder`. |

#### c. Sequence Diagram(s)

```
Kế toán trưởng → DealerController.updateCreditLimit(dealerId, request)
  DealerController → CreditLimitService.updateCreditLimit(dealerId, newLimit, termDays, actor)
    CreditLimitService → validateRange(newLimit)
    CreditLimitService → dealerRepository.findById(dealerId)   → old limit (snapshot)
    CreditLimitService → dealerRepository.save(dealer{credit_limit=newLimit})
    CreditLimitService → auditLogService.log(CREDIT_LIMIT_UPDATED, before, after)
  CreditLimitService --> DealerController : DealerResponse
DealerController --> Kế toán trưởng : 200 OK
```

#### d. Database Queries

```sql
-- 1/ Tạo dealer
INSERT INTO dealers (name, address, phone, email, credit_limit, credit_status, payment_term_days, current_balance, created_at)
VALUES (?, ?, ?, ?, 0, 'ACTIVE', 30, 0, NOW()) RETURNING id;

-- 2/ Cập nhật Credit Limit
UPDATE dealers SET credit_limit = ?, payment_term_days = ?, updated_at = NOW() WHERE id = ?;

-- 3/ Credit Check (dùng bởi DO creation)
SELECT d.current_balance, d.credit_limit, d.credit_status,
       EXISTS (
         SELECT 1 FROM invoices i
         WHERE i.dealer_id = d.id AND i.status IN ('UNPAID','PARTIALLY_PAID')
           AND i.due_date < NOW() - INTERVAL '30 days'
       ) AS has_overdue
FROM dealers d WHERE d.id = ?;
```

---

### 2.4 Vehicle & Driver Management

#### a. Class Diagram

```
VehicleController ──uses──► VehicleService ──uses──► VehicleRepository ──maps──► Vehicle (entity)
DriverController ──uses──► DriverService ──uses──► DriverRepository ──maps──► Driver (entity)
                                   │
                                   └──FK──► User (driver.user_id), Warehouse (driver scope)
```

#### b. Class Specifications

**VehicleController / DriverController Class**

| No  | Method                                     | Description                                                                                     |
| --- | ---------------------------------------------- | ------------------------------------------------------------------------------------------------------ |
| 01  | `createVehicle(CreateVehicleRequest request)` | `POST /api/v1/vehicles` — role `DISPATCHER`/`ADMIN`. Biển số unique, max_weight_kg, max_volume_m3. |
| 02  | `createDriver(CreateDriverRequest request)`   | `POST /api/v1/drivers` — liên kết `user_id`, gán 1+ warehouse phạm vi hoạt động.                    |

**VehicleService / DriverService Class**

| No  | Method                                                        | Description                                                                                                                                                                                                                     |
| --- | ------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 01  | `createVehicle(CreateVehicleRequest request, User actor)`           | **Input**: plateNumber (unique), maxWeightKg, maxVolumeM3 (nullable), actor. **Xử lý nội bộ**: `validateUniquePlate` → lưu `Vehicle` (`is_active=true`) → ghi audit `VEHICLE_CREATED`. **Output**: `VehicleResponse`.               |
| 02  | `createDriver(CreateDriverRequest request, User actor)`              | **Input**: userId (phải có role `DRIVER`), warehouseIds[], actor. **Xử lý nội bộ**: validate user tồn tại + role DRIVER → lưu `Driver` linked `user_id` + warehouse scope → ghi audit `DRIVER_CREATED`. **Output**: `DriverResponse`. |
| 03  | `getEligibleDrivers(Long warehouseId)`                               | Trả danh sách driver active thuộc `warehouseId`, dùng bởi `TripDispatchService` khi Dispatcher chọn tài xế.                                                                                                                        |

#### c. Sequence Diagram(s)

```
Dispatcher → VehicleController.createVehicle(request)
  VehicleController → VehicleService.createVehicle(request, actor)
    VehicleService → vehicleRepository.existsByPlateNumber(plate)   [duplicate check]
    VehicleService → vehicleRepository.save(vehicle)
    VehicleService → auditLogService.log(VEHICLE_CREATED, before=null, after=snapshot)
  VehicleService --> VehicleController : VehicleResponse
VehicleController --> Dispatcher : 201 Created
```

#### d. Database Queries

```sql
-- 1/ Kiểm tra biển số trùng
SELECT EXISTS (SELECT 1 FROM vehicles WHERE plate_number = ?);

-- 2/ Tạo vehicle
INSERT INTO vehicles (plate_number, max_weight_kg, max_volume_m3, is_active, created_at)
VALUES (?, ?, ?, true, NOW()) RETURNING id;

-- 3/ Tạo driver gán warehouse scope
INSERT INTO drivers (user_id, is_active, created_at) VALUES (?, true, NOW()) RETURNING id;
INSERT INTO driver_warehouse_assignments (driver_id, warehouse_id) VALUES (?, ?);

-- 4/ Danh sách tài xế eligible cho 1 kho
SELECT d.id, u.full_name
FROM drivers d
JOIN users u ON d.user_id = u.id
JOIN driver_warehouse_assignments dwa ON dwa.driver_id = d.id
WHERE dwa.warehouse_id = ? AND d.is_active = true;
```

---

## 3. Inbound Receipt & QC (Spec 003)

### 3.1 Receipt Creation & Physical Counting

### a. Class Diagram

```
ReceiptController ──uses──► ReceiptService ──uses──► ReceiptRepository ──maps──► Receipt (entity)
        │                         │                          └────────────────► ReceiptItem (entity)
        │                         ├──uses──► ReceiptItemRepository
        │                         └──uses──► ReceiptValidationService (shared validation: warehouse access, request shape)
        └──DTO: CreateReceiptRequest / ReceiveReceiptRequest ──► ReceiptResponse
```

Quan hệ: `ReceiptController` (`@RestController`, `@RequestMapping("/api/v1/receipts")`) chỉ điều phối HTTP, gọi `ReceiptService` (business logic + `@Transactional`). `ReceiptService` phụ thuộc `ReceiptRepository`, `ReceiptItemRepository` (Spring Data JPA) để truy xuất `Receipt`/`ReceiptItem` entity, và ghi audit qua `AuditLogUtil`/`AuditLogService`.

### b. Class Specifications

#### ReceiptController Class

| No  | Method                                                   | Description                                                                                                                                                                                                 |
| --- | -------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `getReceipts(...)`                                       | `GET /api/v1/receipts` — trả danh sách phiếu nhập theo filter (kho, type, status); yêu cầu warehouse-scope của actor hiện tại.                                                                              |
| 02  | `getReceiptById(Long id)`                                | `GET /api/v1/receipts/{id}` — trả chi tiết 1 phiếu nhập kèm danh sách `receipt_items`.                                                                                                                      |
| 03  | `createReceipt(CreateReceiptRequest request)`            | `POST /api/v1/receipts` — role `PLANNER`. Input: kho, `source_order_code`, danh sách SKU + `expected_qty`. Output: `ReceiptResponse` với `status = PENDING_RECEIPT`.                                        |
| 04  | `receiveReceipt(Long id, ReceiveReceiptRequest request)` | `PUT /api/v1/receipts/{id}/receive` — role `WAREHOUSE_STAFF`/`ADMIN`. Input: `counted_qty` cho toàn bộ dòng hàng. Output: `ReceiptResponse` với `actual_qty`/`over_received_qty` đã tính, `status = DRAFT`. |
| 05  | `processQc(...)`                                         | `PUT /api/v1/receipts/{id}/qc` — role `WAREHOUSE_STAFF`/`STOREKEEPER`/`WAREHOUSE_MANAGER`/`ADMIN`. Ủy quyền cho `ReceiptQcService.processQc` (xem mục 2).                                                   |

#### ReceiptService Class

| No           | Method                                                                            | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| ------------ | --------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01           | `getReceiptsByWarehouse(Long warehouseId, User actor)`                            | Trả danh sách `ReceiptResponse` theo kho, kiểm tra `actor` có quyền truy cập kho đó.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| 02           | `getReceiptsByWarehouseAndType(Long warehouseId, ReceiptType type, User actor)`   | Lọc thêm theo `type` (`PURCHASE`/`RETURN`) nếu truyền vào; nếu không truyền, lấy tất cả loại.                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| 03           | `getReceiptById(Long id, User actor)`                                             | Đọc `Receipt` + join `ReceiptItem` (sắp theo `id ASC`), throw `NotFoundException` nếu không tồn tại hoặc actor không có quyền.                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| 04           | `createPurchaseReceipt(CreateReceiptRequest request, User actor)`                 | **Input**: request (kho, `source_order_code`, items), actor (phải role `PLANNER`, qua `requirePlanner`). **Xử lý nội bộ**: `validateRequest` → `validateItems` → `validateDuplicateSource` (chặn trùng `source_order_code` cùng kho) → `buildItems` map DTO sang entity → lưu `Receipt` (`PENDING_RECEIPT`) và `ReceiptItem[]` → ghi audit `RECEIPT_CREATE` với `snapshot` before/after. **Output**: `ReceiptResponse`.                                                                                                                                                 |
| 05           | `receiveReceiptCounts(Long receiptId, ReceiveReceiptRequest request, User actor)` | **Input**: `receiptId`, request (`counted_qty` mọi dòng), actor (phải `WAREHOUSE_STAFF`, qua `requireWarehouseStaff` + `requireWarehouseAccess`). **Xử lý nội bộ**: `validateReceiveRequest` (đủ dòng, `counted_qty` nguyên dương, không trùng `receipt_item_id`) → với mỗi item tính `actual_qty = min(counted_qty, expected_qty)`, `over_received_qty = max(counted_qty - expected_qty, 0)` → lưu `ReceiptItem[]` → cập nhật `Receipt.status = DRAFT`, nếu đã có QC data thì clear QC field → ghi audit `RECEIPT_RECEIVE`. **Output**: `ReceiptResponse` đã cập nhật. |
| 06 (private) | `requirePlanner` / `requireWarehouseStaff` / `requireWarehouseAccess`             | Kiểm tra role và warehouse-scope của actor; throw `ForbiddenException` nếu vi phạm (BR-SEC-01).                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| 07 (private) | `validateDuplicateSource` / `validateRequest` / `validateItems`                   | Validate input theo business rule (không cho request rỗng, `counted_qty <= 0`, item không thuộc receipt...).                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| 08 (private) | `buildItems` / `snapshot`                                                         | `buildItems` map `CreateReceiptItemRequest → ReceiptItem` entity; `snapshot` build `Map<String,Object>` before/after cho audit log.                                                                                                                                                                                                                                                                                                                                                                                                                                     |

### c. Sequence Diagram(s)

**Sequence: Create Purchase Receipt (`POST /api/v1/receipts`)**

```
Planner → ReceiptController.createReceipt(request)
  ReceiptController → ReceiptService.createPurchaseReceipt(request, actor)
    ReceiptService → requirePlanner(actor)                         [role check]
    ReceiptService → validateRequest(request)                      [shape check]
    ReceiptService → validateItems(request.items)                  [SKU/qty check]
    ReceiptService → receiptRepository.existsBySourceOrderCode(...) [duplicate check]
    ReceiptService → buildItems(request.items, savedReceipt)
    ReceiptService → receiptRepository.save(receipt)                → Receipt{status=PENDING_RECEIPT}
    ReceiptService → receiptItemRepository.saveAll(items)
    ReceiptService → auditLogService.log(RECEIPT_CREATE, before=null, after=snapshot)
  ReceiptService --> ReceiptController : ReceiptResponse
ReceiptController --> Planner : 201 Created (ReceiptResponse)
```

**Sequence: Record Physical Receive Count (`PUT /api/v1/receipts/{id}/receive`)**

```
Nhân viên kho → ReceiptController.receiveReceipt(id, request)
  ReceiptController → ReceiptService.receiveReceiptCounts(id, request, actor)
    ReceiptService → requireWarehouseStaff(actor)
    ReceiptService → receiptRepository.findById(id)                → Receipt
    ReceiptService → requireWarehouseAccess(actor, receipt.warehouseId)
    ReceiptService → validateReceiveRequest(request, items)
    loop each ReceiptItem
      ReceiptService → compute actual_qty / over_received_qty
    end
    ReceiptService → receiptItemRepository.saveAll(items)
    ReceiptService → receipt.setStatus(DRAFT); if hadQcData: clear QC fields
    ReceiptService → receiptRepository.save(receipt)
    ReceiptService → auditLogService.log(RECEIPT_RECEIVE, before, after)
  ReceiptService --> ReceiptController : ReceiptResponse
ReceiptController --> Nhân viên kho : 200 OK (ReceiptResponse)
```

### d. Database Queries

```sql
-- 1/ Kiểm tra trùng nguồn nhập trong cùng kho khi tạo Receipt
SELECT EXISTS (
    SELECT 1 FROM receipts
    WHERE warehouse_id = ? AND source_order_code = ? AND type = 'PURCHASE'
);

-- 2/ Tạo Receipt (PENDING_RECEIPT) và các dòng hàng
INSERT INTO receipts (receipt_number, source_order_code, type, warehouse_id, status, created_by, created_at)
VALUES (?, ?, 'PURCHASE', ?, 'PENDING_RECEIPT', ?, NOW())
RETURNING id;

INSERT INTO receipt_items (receipt_id, product_id, expected_qty)
VALUES (?, ?, ?);

-- 3/ Đọc phiếu + dòng hàng theo id, sắp theo thứ tự tạo
SELECT * FROM receipts WHERE id = ?;
SELECT * FROM receipt_items WHERE receipt_id = ? ORDER BY id ASC;

-- 4/ Cập nhật số lượng đếm thực tế và chuyển trạng thái DRAFT
UPDATE receipt_items
SET actual_qty = ?, over_received_qty = ?
WHERE id = ? AND receipt_id = ?;

UPDATE receipts
SET status = 'DRAFT', updated_at = NOW()
WHERE id = ? AND status IN ('PENDING_RECEIPT','DRAFT','QC_COMPLETED','QC_FAILED');
```

---

### 3.2 Inbound QC Inspection

### a. Class Diagram

```
ReceiptController.processQc(...) ──delegates──► ReceiptQcService.processQc(...)
                                                      │
                                                      ├──uses──► ReceiptRepository
                                                      ├──uses──► ReceiptItemRepository
                                                      └──uses──► AuditLogService
```

### b. Class Specifications

#### ReceiptQcService Class

| No  | Method                                                                   | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| --- | ------------------------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `processQc(Long receiptId, ReceiptQcRequest request, String actorEmail)` | **Input**: `receiptId`, request (danh sách `receipt_item_id` + `qc_result` Đạt/Lỗi + `qc_failure_reason` khi Lỗi), `actorEmail` để resolve actor + role. **Xử lý nội bộ**: kiểm tra `Receipt.status == DRAFT`; validate mỗi dòng có `qc_failure_reason` khi `qc_result = FAILED`; cập nhật `ReceiptItem.qc_result`/`qc_failure_reason`; tính trạng thái tổng của Receipt — `QC_COMPLETED` nếu có ít nhất 1 dòng đạt, `QC_FAILED` nếu toàn bộ lỗi; ghi audit `RECEIPT_QC_RECORD`. **Output**: `ReceiptQcResponse` (trạng thái mới + tổng hợp Đạt/Lỗi theo dòng). |

### c. Sequence Diagram(s)

```
Nhân viên kho/Thủ kho → ReceiptController.processQc(id, request)
  ReceiptController → ReceiptQcService.processQc(id, request, actorEmail)
    ReceiptQcService → receiptRepository.findById(id)         → Receipt (must be DRAFT)
    ReceiptQcService → validate qc_failure_reason khi FAILED
    ReceiptQcService → receiptItemRepository.saveAll(updatedItems)
    ReceiptQcService → compute overall status (QC_COMPLETED / QC_FAILED)
    ReceiptQcService → receiptRepository.save(receipt)
    ReceiptQcService → auditLogService.log(RECEIPT_QC_RECORD, before, after)
  ReceiptQcService --> ReceiptController : ReceiptQcResponse
ReceiptController --> Actor : 200 OK
```

### d. Database Queries

```sql
UPDATE receipt_items
SET qc_result = ?, qc_failure_reason = ?
WHERE id = ? AND receipt_id = ?;

UPDATE receipts
SET status = ?, updated_at = NOW()
WHERE id = ? AND status = 'DRAFT';
```

---

### 3.3 Receipt Approval & Putaway

### a. Class Diagram

```
ReceiptApprovalController ──uses──► ReceiptApprovalService
                                          │
                                          ├──uses──► ReceiptRepository
                                          ├──uses──► ReceiptItemRepository
                                          ├──uses──► BatchRepository
                                          ├──uses──► InventoryRepository
                                          ├──uses──► WarehouseLocationRepository
                                          ├──uses──► ReceiptValidationService
                                          └──uses──► AuditLogService
```

### b. Class Specifications

#### ReceiptApprovalController Class

| No  | Method                                                          | Description                                                                                                |
| --- | --------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------- |
| 01  | `approve(Long id, ...)`                                         | `PUT /api/v1/receipts/{id}/approve` — role Trưởng kho. Ủy quyền `ReceiptApprovalService.approveReceipt`.   |
| 02  | `reject(Long id, ReceiptDecisionRequest)`                       | `PUT /api/v1/receipts/{id}/reject` — role Trưởng kho, bắt buộc lý do. Ủy quyền `rejectReceipt`.            |
| 03  | `confirmReturnToSupplier(Long id, ReceiptReturnConfirmRequest)` | `PUT /api/v1/receipts/{id}/return-to-supplier/confirm` — role Thủ kho. Ủy quyền `confirmReturnToSupplier`. |
| 04  | `complete(Long id, ReceiptPutawayRequest)`                      | `PUT /api/v1/receipts/{id}/complete` — role Thủ kho, cất hàng vào Bin. Ủy quyền `completePutaway`.         |

#### ReceiptApprovalService Class

| No  | Method                                                                              | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| --- | ----------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `approveReceipt(Long receiptId, ...)`                                               | **Input**: `receiptId`, actor (Trưởng kho, đúng kho). **Xử lý**: kiểm tra `Receipt.status == QC_COMPLETED`; cập nhật `status = APPROVED`; KHÔNG cộng tồn kho ở bước này; ghi audit `RECEIPT_APPROVE`. **Output**: `ReceiptActionResponse`.                                                                                                                                                                                                                                                                                                                                                |
| 02  | `rejectReceipt(Long receiptId, ReceiptDecisionRequest request, ...)`                | **Input**: `receiptId`, `request.reason` (bắt buộc), actor. **Xử lý**: kiểm tra `status == QC_COMPLETED`; cập nhật `status = RETURN_TO_SUPPLIER_PENDING`, lưu `reject_reason`; KHÔNG tạo inventory/batch/RTV/Debit Note; ghi audit `RECEIPT_REJECT`. **Output**: `ReceiptActionResponse`.                                                                                                                                                                                                                                                                                                 |
| 03  | `confirmReturnToSupplier(Long receiptId, ReceiptReturnConfirmRequest request, ...)` | **Input**: `receiptId`, actor (Thủ kho). **Xử lý**: kiểm tra `status == RETURN_TO_SUPPLIER_PENDING`; cập nhật `status = RETURNED_TO_SUPPLIER`; ghi audit `RECEIPT_RETURN_CONFIRM`. **Output**: `ReceiptActionResponse`.                                                                                                                                                                                                                                                                                                                                                                   |
| 04  | `completePutaway(Long receiptId, ReceiptPutawayRequest request, ...)`               | **Input**: `receiptId`, `request.items[]` (mỗi item: `receipt_item_id`, `location_id`), actor (Thủ kho). **Xử lý**: kiểm tra `status == APPROVED`; với mỗi dòng: kiểm tra `warehouse_locations.is_quarantine = false` và còn đủ `capacity` (qua `warehouseLocationRepository` + `inventoryRepository` — BR-BAT-02); tạo/tìm `batches` theo product + receipt + `received_date`; cộng `inventories.total_qty` với optimistic lock (`version`); ghi audit `RECEIPT_PUTAWAY_COMPLETE`. Đây là bước DUY NHẤT tăng available inventory của luồng inbound. **Output**: `ReceiptActionResponse`. |

### c. Sequence Diagram(s)

**Sequence: Approve Receipt & Putaway**

```
Trưởng kho → ReceiptApprovalController.approve(id)
  ReceiptApprovalController → ReceiptApprovalService.approveReceipt(id, actor)
    ReceiptApprovalService → receiptRepository.findById(id)     → Receipt (must be QC_COMPLETED)
    ReceiptApprovalService → receipt.setStatus(APPROVED)
    ReceiptApprovalService → receiptRepository.save(receipt)
    ReceiptApprovalService → auditLogService.log(RECEIPT_APPROVE, before, after)
  ReceiptApprovalService --> ReceiptApprovalController : ReceiptActionResponse
ReceiptApprovalController --> Trưởng kho : 200 OK

Thủ kho → ReceiptApprovalController.complete(id, putawayRequest)
  ReceiptApprovalController → ReceiptApprovalService.completePutaway(id, request, actor)
    ReceiptApprovalService → receiptRepository.findById(id)     → Receipt (must be APPROVED)
    loop each putaway item
      ReceiptApprovalService → warehouseLocationRepository.findById(locationId)  [is_quarantine=false, capacity check]
      ReceiptApprovalService → batchRepository.findOrCreate(product, receipt, receivedDate)
      ReceiptApprovalService → inventoryRepository.increaseTotalQty(warehouse, product, batch, location, version)
    end
    ReceiptApprovalService → auditLogService.log(RECEIPT_PUTAWAY_COMPLETE, before, after)
  ReceiptApprovalService --> ReceiptApprovalController : ReceiptActionResponse
ReceiptApprovalController --> Thủ kho : 200 OK
```

### d. Database Queries

```sql
-- 1/ Duyệt phiếu nhập
UPDATE receipts SET status = 'APPROVED', approved_by = ?, approved_at = NOW()
WHERE id = ? AND status = 'QC_COMPLETED';

-- 2/ Từ chối phiếu nhập
UPDATE receipts SET status = 'RETURN_TO_SUPPLIER_PENDING', reject_reason = ?
WHERE id = ? AND status = 'QC_COMPLETED';

-- 3/ Kiểm tra sức chứa Bin trước putaway
SELECT wl.id, wl.capacity, wl.is_quarantine,
       COALESCE(SUM(i.total_qty), 0) AS current_qty
FROM warehouse_locations wl
LEFT JOIN inventories i ON i.location_id = wl.id
WHERE wl.id = ? AND wl.is_quarantine = false
GROUP BY wl.id, wl.capacity, wl.is_quarantine;

-- 4/ Tìm hoặc tạo batch theo product + receipt + received_date
SELECT id FROM batches WHERE product_id = ? AND receipt_id = ? AND received_date = ?;
INSERT INTO batches (product_id, receipt_id, received_date, created_at)
VALUES (?, ?, ?, NOW()) RETURNING id;

-- 5/ Cộng tồn kho sau putaway (optimistic locking qua version)
UPDATE inventories
SET total_qty = total_qty + ?, version = version + 1
WHERE warehouse_id = ? AND product_id = ? AND batch_id = ? AND location_id = ? AND version = ?;
```

---

### 3.4 Quarantine & Return-to-Vendor (RTV)

### a. Class Diagram

```
QuarantineController ──uses──► (read-only quarantine listing)
QuarantineRtvController ──uses──► QuarantineRtvService
                                        │
                                        ├──uses──► ReceiptRepository
                                        ├──uses──► ReceiptItemRepository
                                        ├──uses──► AdjustmentRepository
                                        ├──uses──► DebitNoteRepository
                                        ├──uses──► InventoryRepository
                                        ├──uses──► QuarantineRecordRepository
                                        ├──uses──► PriceHistoryRepository (định giá Debit Note)
                                        ├──uses──► ReceiptValidationService
                                        └──uses──► AuditLogService
```

### b. Class Specifications

#### QuarantineController Class

| No  | Method                    | Description                                                                                                                                                                                                 |
| --- | ------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `getQuarantineItems(...)` | `GET /api/v1/quarantine/items` — role `WAREHOUSE_STAFF`/`STOREKEEPER`/`WAREHOUSE_MANAGER`/`ADMIN`. Trả danh sách hàng đang trong Quarantine theo kho, kèm `origin` (`RECEIPT_QC_FAIL`/`INTERNAL_TRANSFER`). |

#### QuarantineRtvController Class

| No  | Method                                                 | Description                                                                                    |
| --- | ------------------------------------------------------ | ---------------------------------------------------------------------------------------------- |
| 01  | `createRtv(Long receiptId, ReceiptRtvCreateRequest)`   | `POST /api/v1/receipts/{id}/rtv` — role Trưởng kho. Ủy quyền `QuarantineRtvService.createRtv`. |
| 02  | `confirmRtv(Long receiptId, ReceiptRtvConfirmRequest)` | `PUT /api/v1/receipts/{id}/rtv/confirm` — role Thủ kho. Ủy quyền `confirmRtv`.                 |

#### QuarantineRtvService Class

| No  | Method                                                              | Description                                                                                                                                                                                                                                                                                                                                           |
| --- | ------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `createRtv(Long receiptId, ReceiptRtvCreateRequest request, ...)`   | **Input**: `receiptId` (phải có `QC_FAILED` items còn trong Quarantine), actor (Trưởng kho). **Xử lý**: tạo `debit_notes` cho supplier (định giá theo `price_history` hiệu lực); tạo `adjustments` `type = RETURN_TO_VENDOR` ở trạng thái pending; KHÔNG trừ tồn Quarantine ngay; ghi audit `QUARANTINE_RTV_CREATE`. **Output**: `RtvActionResponse`. |
| 02  | `confirmRtv(Long receiptId, ReceiptRtvConfirmRequest request, ...)` | **Input**: `receiptId`, actor (Thủ kho), số lượng xác nhận bàn giao (phải bằng ĐÚNG số lượng RTV — không cho một phần, trả 422 nếu lệch). **Xử lý**: trừ `inventories`/`quarantine_records` theo `receiptId`; giữ `receipts.status = QC_FAILED`; ghi audit `QUARANTINE_RTV_CONFIRM`. **Output**: `RtvActionResponse`.                                 |
| 03  | `getQuarantineItems(Long warehouseId, User actor)`                  | Trả `List<QuarantineItemResponse>` cho kho, kiểm tra warehouse-scope của actor.                                                                                                                                                                                                                                                                       |

### c. Sequence Diagram(s)

```
Trưởng kho → QuarantineRtvController.createRtv(receiptId, request)
  QuarantineRtvController → QuarantineRtvService.createRtv(receiptId, request, actor)
    QuarantineRtvService → receiptRepository.findById(receiptId)      → Receipt (must have QC_FAILED items)
    QuarantineRtvService → priceHistoryRepository.findEffectivePrice(productId, date)
    QuarantineRtvService → debitNoteRepository.save(debitNote)
    QuarantineRtvService → adjustmentRepository.save(adjustment{type=RETURN_TO_VENDOR, status=PENDING})
    QuarantineRtvService → auditLogService.log(QUARANTINE_RTV_CREATE, before, after)
  QuarantineRtvService --> QuarantineRtvController : RtvActionResponse
QuarantineRtvController --> Trưởng kho : 201 Created

Thủ kho → QuarantineRtvController.confirmRtv(receiptId, request)
  QuarantineRtvController → QuarantineRtvService.confirmRtv(receiptId, request, actor)
    QuarantineRtvService → validate confirmedQty == rtvQty (else 422)
    QuarantineRtvService → inventoryRepository.decreaseQuarantineQty(...)
    QuarantineRtvService → quarantineRecordRepository.markCleared(...)
    QuarantineRtvService → auditLogService.log(QUARANTINE_RTV_CONFIRM, before, after)
  QuarantineRtvService --> QuarantineRtvController : RtvActionResponse
QuarantineRtvController --> Thủ kho : 200 OK
```

### d. Database Queries

```sql
-- 1/ Danh sách hàng trong Quarantine theo kho
SELECT qr.id, qr.product_id, qr.quantity, qr.origin, r.receipt_number
FROM quarantine_records qr
JOIN receipts r ON r.id = qr.receipt_id
WHERE qr.warehouse_id = ? AND qr.quantity > 0;

-- 2/ Tạo Debit Note + Adjustment (RTV) — pending, chưa trừ tồn
INSERT INTO debit_notes (receipt_id, supplier_id, amount, created_at)
VALUES (?, ?, ?, NOW()) RETURNING id;

INSERT INTO adjustments (receipt_id, type, quantity, status, created_at)
VALUES (?, 'RETURN_TO_VENDOR', ?, 'PENDING', NOW()) RETURNING id;

-- 3/ Xác nhận bàn giao đủ — trừ tồn Quarantine
UPDATE quarantine_records
SET quantity = quantity - ?
WHERE receipt_id = ? AND product_id = ? AND quantity >= ?;

UPDATE adjustments SET status = 'CONFIRMED', confirmed_at = NOW()
WHERE id = ?;
```

---

## 4. Outbound Delivery & POD (Spec 004)

### 4.1 DO Creation & Credit Check

#### a. Class Diagram

```
DeliveryOrderController ──uses──► DeliveryOrderService ──uses──► DeliveryOrderRepository ──maps──► DeliveryOrder (entity)
        │                              │                                                                  └──► DeliveryOrderItem
        │                              ├──uses──► CreditLimitService (credit check)
        │                              ├──uses──► InventoryRepository (available check)
        │                              ├──uses──► PriceHistoryRepository (COGS snapshot)
        │                              └──uses──► WarehouseProductReservationRepository
        └──DTO: CreateDeliveryOrderRequest ──► DeliveryOrderResponse
```

#### b. Class Specifications

**DeliveryOrderController Class**

| No  | Method                                          | Description                                                                                    |
| --- | -------------------------------------------------- | ---------------------------------------------------------------------------------------------------- |
| 01  | `getDeliveryOrders(...)`                          | `GET /api/v1/delivery-orders` — filter theo kho/dealer/status; warehouse-scoped.                    |
| 02  | `createDeliveryOrder(CreateDeliveryOrderRequest)` | `POST /api/v1/delivery-orders` — role `PLANNER`. Tra 422 `CREDIT_HOLD`/`INSUFFICIENT_STOCK` neu fail. |

**DeliveryOrderService Class**

| No  | Method                                                            | Description                                                                                                                                                                                                                                                                                                                                                                          |
| --- | ---------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `createDeliveryOrder(CreateDeliveryOrderRequest request, User actor)`   | **Input**: request (warehouseId, dealerId, requestedDate, items[]), actor (`PLANNER`). **Xu ly noi bo**: `requirePlanner` -> `creditLimitService.checkCreditStatus(dealerId, totalValue)` (throw `CreditHoldException` neu block) -> `validateAvailableStock` cho tung item (`total_qty - reserved_qty >= requestedQty`) -> `priceHistoryService.snapshotPrice` cho tung item -> luu `DeliveryOrder` (`status=NEW`) + `DeliveryOrderItem[]` -> `warehouseProductReservationRepository.reserve` (cap warehouse, chua cap batch) -> ghi audit `DO_CREATED`. **Output**: `DeliveryOrderResponse`. |

#### c. Sequence Diagram(s)

```
Planner -> DeliveryOrderController.createDeliveryOrder(request)
  DeliveryOrderController -> DeliveryOrderService.createDeliveryOrder(request, actor)
    DeliveryOrderService -> requirePlanner(actor)
    DeliveryOrderService -> creditLimitService.checkCreditStatus(dealerId, totalValue)   [throw neu CREDIT_HOLD]
    loop each item
      DeliveryOrderService -> inventoryRepository.getAvailableQty(warehouseId, productId)  [throw neu thieu]
      DeliveryOrderService -> priceHistoryRepository.findEffectivePrice(productId, today)
    end
    DeliveryOrderService -> deliveryOrderRepository.save(do{status=NEW})
    DeliveryOrderService -> deliveryOrderItemRepository.saveAll(items)
    DeliveryOrderService -> warehouseProductReservationRepository.reserve(warehouseId, productId, qty)
    DeliveryOrderService -> auditLogService.log(DO_CREATED, before=null, after=snapshot)
  DeliveryOrderService --> DeliveryOrderController : DeliveryOrderResponse
DeliveryOrderController --> Planner : 201 Created
```

#### d. Database Queries

```sql
-- 1/ Credit Check
SELECT d.current_balance, d.credit_limit, d.credit_status,
       EXISTS (SELECT 1 FROM invoices i WHERE i.dealer_id = d.id
               AND i.status IN ('UNPAID','PARTIALLY_PAID') AND i.due_date < NOW() - INTERVAL '30 days') AS has_overdue
FROM dealers d WHERE d.id = ?;

-- 2/ Kiem tra ton kha dung
SELECT COALESCE(SUM(total_qty - reserved_qty), 0) AS available_qty
FROM inventories WHERE warehouse_id = ? AND product_id = ?;

-- 3/ Snapshot gia hieu luc
SELECT cost_price, selling_price FROM price_history
WHERE product_id = ? AND effective_date <= CURRENT_DATE AND (end_date IS NULL OR end_date > CURRENT_DATE)
ORDER BY effective_date DESC LIMIT 1;

-- 4/ Tao DO + items
INSERT INTO delivery_orders (warehouse_id, dealer_id, status, created_by, created_at)
VALUES (?, ?, 'NEW', ?, NOW()) RETURNING id;

INSERT INTO delivery_order_items (do_id, product_id, requested_qty, unit_price)
VALUES (?, ?, ?, ?);

-- 5/ Reserve cap warehouse
UPDATE warehouse_product_reservations
SET reserved_qty = reserved_qty + ?, version = version + 1
WHERE warehouse_id = ? AND product_id = ? AND version = ?;
```

---

### 4.2 Picking Plan (FIFO)

#### a. Class Diagram

```
PickingPlanController ──uses──► PickingPlanService ──uses──► FIFOSelector (util)
                                       │
                                       ├──uses──► DeliveryOrderItemAllocationRepository
                                       └──uses──► InventoryRepository / BatchRepository
```

#### b. Class Specifications

**PickingPlanController Class**

| No  | Method                                              | Description                                                                       |
| --- | ------------------------------------------------------ | ---------------------------------------------------------------------------------------- |
| 01  | `createPickingPlan(Long doId, PickingPlanRequest)`    | `PUT /api/v1/delivery-orders/{id}/picking-plan` — role `STOREKEEPER`.               |

**PickingPlanService Class**

| No  | Method                                                        | Description                                                                                                                                                                                                                                                                        |
| --- | ------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `createPickingPlan(Long doId, PickingPlanRequest request, User actor)` | **Input**: doId (`status=NEW`), request.allocations[] (batchId/locationId/zoneId/plannedQty theo `FIFOSelector` goi y), actor (`STOREKEEPER`). **Xu ly noi bo**: `fifoSelector.suggestBatches(warehouseId, productId)` order by `batch.received_date ASC` -> validate `sum(allocations.plannedQty) == doItem.requestedQty` -> luu `DeliveryOrderItemAllocation[]` -> chuyen DO `WAITING_PICKING` -> ghi audit `PICKING_PLAN_CREATED`. **Output**: `DeliveryOrderResponse`. |

#### c. Sequence Diagram(s)

```
Thu kho -> PickingPlanController.createPickingPlan(doId, request)
  PickingPlanController -> PickingPlanService.createPickingPlan(doId, request, actor)
    PickingPlanService -> deliveryOrderRepository.findById(doId)     -> DO (must be NEW)
    PickingPlanService -> fifoSelector.suggestBatches(warehouseId, productId)   [order by received_date ASC]
    PickingPlanService -> validate sum(allocations) == requestedQty
    PickingPlanService -> allocationRepository.saveAll(allocations)
    PickingPlanService -> deliveryOrder.setStatus(WAITING_PICKING)
    PickingPlanService -> auditLogService.log(PICKING_PLAN_CREATED, before, after)
  PickingPlanService --> PickingPlanController : DeliveryOrderResponse
PickingPlanController --> Thu kho : 200 OK
```

#### d. Database Queries

```sql
-- 1/ Goi y batch theo FIFO
SELECT i.batch_id, i.location_id, wl.zone_id, b.received_date, (i.total_qty - i.reserved_qty) AS available_qty
FROM inventories i
JOIN batches b ON b.id = i.batch_id
JOIN warehouse_locations wl ON wl.id = i.location_id
WHERE i.warehouse_id = ? AND i.product_id = ? AND wl.is_quarantine = false
  AND (i.total_qty - i.reserved_qty) > 0
ORDER BY b.received_date ASC;

-- 2/ Luu allocation
INSERT INTO delivery_order_item_allocations (do_item_id, batch_id, location_id, zone_id, planned_qty, status)
VALUES (?, ?, ?, ?, ?, 'PLANNED');

-- 3/ Chuyen trang thai DO
UPDATE delivery_orders SET status = 'WAITING_PICKING', updated_at = NOW() WHERE id = ? AND status = 'NEW';
```

---

### 4.3 Picking & Outbound QC

#### a. Class Diagram

```
OutboundQCController ──uses──► OutboundQCService ──uses──► DeliveryOrderItemAllocationRepository
                                       │
                                       ├──uses──► QuarantineRecordRepository (hang fail)
                                       └──uses──► AdjustmentRepository
```

#### b. Class Specifications

**OutboundQCService Class**

| No  | Method                                                           | Description                                                                                                                                                                                                                                                                                             |
| --- | ----------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `executePickingAndQc(Long doId, PickingQcRequest request, User actor)`  | **Input**: doId (`WAITING_PICKING`), request.lines[] (allocationId, pickedQty, qcResult Pass/Fail, failReason), actor (`WAREHOUSE_STAFF`). **Xu ly noi bo**: validate `pickedQty <= allocation.plannedQty` (else `OVER_PICKED`) -> hang Pass: cap nhat `allocation.qcPassQty`, di chuyen vao outbound staging location -> hang Fail: tao `quarantine_records` (`origin=OUTBOUND_QC_FAIL`) + `adjustments`, release phan reserve tuong ung -> tinh trang thai DO tong (`QC_PENDING_APPROVAL`) -> ghi audit `PICKING_QC_EXECUTED`. **Output**: `DeliveryOrderResponse`. |

#### c. Sequence Diagram(s)

```
Nhan vien kho -> OutboundQCController.executePickingAndQc(doId, request)
  OutboundQCController -> OutboundQCService.executePickingAndQc(doId, request, actor)
    OutboundQCService -> allocationRepository.findByDoId(doId)
    loop each line
      OutboundQCService -> validate pickedQty <= plannedQty
      alt QC Pass
        OutboundQCService -> moveToOutboundStaging(allocation, pickedQty)
      else QC Fail
        OutboundQCService -> quarantineRecordRepository.save(origin=OUTBOUND_QC_FAIL)
        OutboundQCService -> adjustmentRepository.save(type=OUTBOUND_QC_FAIL)
      end
    end
    OutboundQCService -> deliveryOrder.setStatus(QC_PENDING_APPROVAL)
    OutboundQCService -> auditLogService.log(PICKING_QC_EXECUTED, before, after)
  OutboundQCService --> OutboundQCController : DeliveryOrderResponse
OutboundQCController --> Nhan vien kho : 200 OK
```

#### d. Database Queries

```sql
-- 1/ Cap nhat allocation sau pick+QC
UPDATE delivery_order_item_allocations
SET picked_qty = ?, qc_pass_qty = ?, qc_fail_qty = ?, status = 'PICKED'
WHERE id = ? AND do_item_id = ?;

-- 2/ Tao quarantine record cho hang fail
INSERT INTO quarantine_records (warehouse_id, product_id, quantity, origin, do_id, created_at)
VALUES (?, ?, ?, 'OUTBOUND_QC_FAIL', ?, NOW());

-- 3/ Chuyen trang thai DO
UPDATE delivery_orders SET status = 'QC_PENDING_APPROVAL', updated_at = NOW() WHERE id = ?;
```

---

### 4.4 Warehouse Approval (DO)

#### a. Class Diagram

```
DeliveryOrderApprovalController ──uses──► DeliveryOrderApprovalService ──uses──► DeliveryOrderRepository
                                                  │
                                                  └──uses──► DeliveryOrderItemAllocationRepository (reject -> return to bin)
```

#### b. Class Specifications

**DeliveryOrderApprovalService Class**

| No  | Method                                                       | Description                                                                                                                                                                                                                                                                                    |
| --- | ------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `approveDo(Long doId, User actor)`                                 | **Input**: doId (`QC_COMPLETED`), actor (`WAREHOUSE_MANAGER`). **Xu ly noi bo**: cap nhat `status = WAREHOUSE_APPROVED`; ghi audit `DO_APPROVED`. **Output**: `DeliveryOrderResponse`.                                                                                                          |
| 02  | `rejectDo(Long doId, RejectRequest request, User actor)`           | **Input**: doId, request.reason (bat buoc), actor. **Xu ly noi bo**: tra toan bo hang QC-pass o outbound staging ve bin goc (ghi `PICKED_GOODS_RETURN_TO_BIN`), release allocation, `status = REJECTED`; ghi audit `DO_REJECTED`. **Output**: `DeliveryOrderResponse`.                          |

#### c. Sequence Diagram(s)

```
Truong kho -> DeliveryOrderApprovalController.approve(doId)
  DeliveryOrderApprovalController -> DeliveryOrderApprovalService.approveDo(doId, actor)
    DeliveryOrderApprovalService -> deliveryOrderRepository.findById(doId)   -> DO (must be QC_COMPLETED)
    DeliveryOrderApprovalService -> deliveryOrder.setStatus(WAREHOUSE_APPROVED)
    DeliveryOrderApprovalService -> auditLogService.log(DO_APPROVED, before, after)
  DeliveryOrderApprovalService --> DeliveryOrderApprovalController : DeliveryOrderResponse
DeliveryOrderApprovalController --> Truong kho : 200 OK
```

#### d. Database Queries

```sql
UPDATE delivery_orders SET status = 'WAREHOUSE_APPROVED', approved_by = ?, approved_at = NOW()
WHERE id = ? AND status = 'QC_COMPLETED';

-- Reject: tra hang pass ve bin goc
UPDATE inventories SET total_qty = total_qty + ?, version = version + 1
WHERE warehouse_id = ? AND product_id = ? AND batch_id = ? AND location_id = ?;

UPDATE delivery_orders SET status = 'REJECTED', reject_reason = ? WHERE id = ?;
```

---

### 4.5 Trip Dispatch (Delivery)

#### a. Class Diagram

```
TripDispatchController ──uses──► TripDispatchService ──uses──► TripRepository ──maps──► Trip (entity)
                                       │
                                       ├──uses──► VehicleRepository / DriverService (eligible drivers)
                                       └──uses──► DeliveryOrderRepository
```

#### b. Class Specifications

**TripDispatchService Class**

| No  | Method                                                             | Description                                                                                                                                                                                                                                                                                                              |
| --- | -------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 01  | `createDeliveryTrip(CreateTripRequest request, User actor)`               | **Input**: request (warehouseId, doIds[] tat ca `WAREHOUSE_APPROVED` cung kho, vehicleId, driverId), actor (`DISPATCHER`). **Xu ly noi bo**: `requireDispatcher` -> tinh `totalWeight = Sum(doItem.qty * product.weightKg)` -> kiem tra `totalWeight <= vehicle.maxWeightKg` (else `OVER_WEIGHT`) -> neu `vehicle.maxVolumeM3` khac null, kiem tra tuong tu the tich -> validate driver thuoc warehouse scope -> luu `Trip` (`trip_type=DELIVERY`, `status=PLANNED`) -> link DO[] -> ghi audit `TRIP_CREATED`. **Output**: `TripResponse`. |

#### c. Sequence Diagram(s)

```
Dispatcher -> TripDispatchController.createTrip(request)
  TripDispatchController -> TripDispatchService.createDeliveryTrip(request, actor)
    TripDispatchService -> requireDispatcher(actor)
    TripDispatchService -> calculate totalWeight(doIds)
    TripDispatchService -> validate totalWeight <= vehicle.maxWeightKg
    TripDispatchService -> validate totalVolume <= vehicle.maxVolumeM3 (neu co)
    TripDispatchService -> validate driver in warehouse scope
    TripDispatchService -> tripRepository.save(trip{trip_type=DELIVERY, status=PLANNED})
    TripDispatchService -> linkDeliveryOrders(trip, doIds)
    TripDispatchService -> auditLogService.log(TRIP_CREATED, before=null, after=snapshot)
  TripDispatchService --> TripDispatchController : TripResponse
TripDispatchController --> Dispatcher : 201 Created
```

#### d. Database Queries

```sql
-- 1/ Tinh tong tai trong cac DO
SELECT SUM(doi.requested_qty * p.weight_kg) AS total_weight
FROM delivery_order_items doi
JOIN products p ON p.id = doi.product_id
WHERE doi.do_id = ANY(?);

-- 2/ Tao trip
INSERT INTO trips (trip_type, warehouse_id, vehicle_id, driver_id, status, created_at)
VALUES ('DELIVERY', ?, ?, ?, 'PLANNED', NOW()) RETURNING id;

-- 3/ Gan DO vao trip
UPDATE delivery_orders SET trip_id = ? WHERE id = ANY(?);
```

---

### 4.6 Driver Mobile POD + OTP

#### a. Class Diagram

```
DriverDeliveryController ──uses──► DriverDeliveryServiceImpl ──uses──► DeliveryRepository ──maps──► Delivery (entity)
                                       │
                                       └──uses──► DeliveryOtpService ──uses──► DeliveryOtpAttemptRepository
```

#### b. Class Specifications

**DriverDeliveryServiceImpl Class**

| No  | Method                                                             | Description                                                                                                                                                                                                                                                                                    |
| --- | ------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `uploadPod(Long deliveryId, PodUploadRequest request, User actor)`         | **Input**: deliveryId, request (goodsImageRef, signDocumentImageRef - da upload multipart truoc), actor (`DRIVER`, phai dung driver duoc gan). **Xu ly noi bo**: validate ca 2 anh da co -> luu vao `deliveries.goods_image_ref`/`sign_document_image_ref`. **Output**: `DeliveryResponse`. |

**DeliveryOtpService Class**

| No  | Method                                                       | Description                                                                                                                                                                                                                                                                                                                                       |
| --- | ------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `requestOtp(Long deliveryId, User actor)`                          | **Input**: deliveryId (da co du POD anh). **Xu ly noi bo**: kiem tra khong co OTP con hieu luc (else `OTP_ALREADY_ACTIVE`) -> sinh ma 6 so random -> hash (SHA-256) -> luu `delivery_otp_attempts` (`expires_at = NOW()+5m`, `status=PENDING`) -> gui email toi `dealer.email`. **Output**: void.                                                          |
| 02  | `verifyOtp(Long deliveryId, String otpPlain, User actor)`           | **Input**: deliveryId, otpPlain (tai xe nhap). **Xu ly noi bo**: hash input, so khop voi `delivery_otp_attempts.otp_hash` con `PENDING` va `expires_at > NOW()` -> neu dung: `status=VERIFIED`, `consumed_at=NOW()`, `deliveries.status=DELIVERED` -> trigger `AutoInvoiceService` -> neu sai: `attempt_count += 1`, neu `>= 3` thi `status=LOCKED`. **Output**: `DeliveryResponse`. |

#### c. Sequence Diagram(s)

```
Tai xe -> DriverDeliveryController.requestOtp(deliveryId)
  DriverDeliveryController -> DeliveryOtpService.requestOtp(deliveryId, actor)
    DeliveryOtpService -> validate no active OTP
    DeliveryOtpService -> generate 6-digit code
    DeliveryOtpService -> hash(code) -> SHA-256
    DeliveryOtpService -> otpAttemptRepository.save(hash, expires_at=+5m, status=PENDING)
    DeliveryOtpService -> mailService.sendOtp(dealer.email, code)
  DeliveryOtpService --> DriverDeliveryController : void
DriverDeliveryController --> Tai xe : 200 OK

Tai xe -> DriverDeliveryController.verifyOtp(deliveryId, otpPlain)
  DriverDeliveryController -> DeliveryOtpService.verifyOtp(deliveryId, otpPlain, actor)
    DeliveryOtpService -> hash(otpPlain)
    DeliveryOtpService -> otpAttemptRepository.findPending(deliveryId)
    alt hash matches AND not expired
      DeliveryOtpService -> otpAttempt.setStatus(VERIFIED)
      DeliveryOtpService -> delivery.setStatus(DELIVERED)
      DeliveryOtpService -> eventPublisher.publish(DeliveryDeliveredEvent)   [-> AutoInvoiceService]
    else
      DeliveryOtpService -> otpAttempt.incrementAttemptCount()
      alt attemptCount >= 3
        DeliveryOtpService -> otpAttempt.setStatus(LOCKED)
      end
    end
  DeliveryOtpService --> DriverDeliveryController : DeliveryResponse
DriverDeliveryController --> Tai xe : 200 OK
```

#### d. Database Queries

```sql
-- 1/ Luu OTP hash
INSERT INTO delivery_otp_attempts (delivery_id, recipient_email, otp_hash, expires_at, attempt_count, status)
VALUES (?, ?, ?, NOW() + INTERVAL '5 minutes', 0, 'PENDING');

-- 2/ Verify OTP
SELECT id, otp_hash, expires_at, attempt_count
FROM delivery_otp_attempts
WHERE delivery_id = ? AND status = 'PENDING';

UPDATE delivery_otp_attempts SET status = 'VERIFIED', consumed_at = NOW() WHERE id = ?;
UPDATE deliveries SET status = 'DELIVERED', otp_verified_at = NOW() WHERE id = ?;

-- 3/ Sai OTP: tang attempt_count, khoa neu >=3
UPDATE delivery_otp_attempts
SET attempt_count = attempt_count + 1,
    status = CASE WHEN attempt_count + 1 >= 3 THEN 'LOCKED' ELSE status END
WHERE id = ?;
```

---

### 4.7 Auto-Invoice Creation

#### a. Class Diagram

```
DeliveryDeliveredEvent ──triggers──► AutoInvoiceService (listener) ──uses──► InvoiceRepository / DealerRepository
```

#### b. Class Specifications

**AutoInvoiceService Class**

| No  | Method                                                | Description                                                                                                                                                                                                                                                                              |
| --- | ---------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 01  | `onDeliveryDelivered(DeliveryDeliveredEvent event)`         | **Input**: event chua `deliveryId`/`doId`. **Xu ly noi bo**: idempotent check (`invoiceRepository.existsByDoId`) -> tinh `total_amount = Sum(doItem.qcPassQty * doItem.unitPrice)` -> tao `invoices` (`issue_date=TODAY`, `due_date=TODAY+30`, `status=UNPAID`) -> cong `dealers.current_balance` -> cap nhat DO `status=COMPLETED` -> ghi audit `INVOICE_CREATED`. **Output**: void (event-driven, khong HTTP response). |

#### c. Sequence Diagram(s)

```
[Event] DeliveryDeliveredEvent -> AutoInvoiceService.onDeliveryDelivered(event)
  AutoInvoiceService -> invoiceRepository.existsByDoId(doId)   [idempotent check]
  AutoInvoiceService -> deliveryOrderItemRepository.findByDoId(doId)
  AutoInvoiceService -> calculate totalAmount = Sum(qcPassQty * unitPrice)
  AutoInvoiceService -> invoiceRepository.save(invoice{status=UNPAID, due_date=+30d})
  AutoInvoiceService -> dealerRepository.increaseBalance(dealerId, totalAmount)
  AutoInvoiceService -> deliveryOrder.setStatus(COMPLETED)
  AutoInvoiceService -> auditLogService.log(INVOICE_CREATED, before=null, after=snapshot)
```

#### d. Database Queries

```sql
SELECT EXISTS (SELECT 1 FROM invoices WHERE do_id = ?);

INSERT INTO invoices (do_id, dealer_id, total_amount, issue_date, due_date, status, created_at)
SELECT ?, do.dealer_id, SUM(doi.qc_pass_qty * doi.unit_price), CURRENT_DATE, CURRENT_DATE + 30, 'UNPAID', NOW()
FROM delivery_orders do
JOIN delivery_order_items doi ON doi.do_id = do.id
WHERE do.id = ?
GROUP BY do.dealer_id;

UPDATE dealers SET current_balance = current_balance + ? WHERE id = ?;
UPDATE delivery_orders SET status = 'COMPLETED' WHERE id = ?;
```

---

## 5. Inter-Warehouse Transfer (Spec 005)

### 5.1 Cross-Warehouse View & Transfer Request

#### a. Class Diagram

```
CrossWarehouseStockController ──uses──► CrossWarehouseStockService (read-only)
TransferRequestController ──uses──► TransferRequestService ──uses──► TransferRequestRepository
```

#### b. Class Specifications

**TransferRequestService Class**

| No  | Method                                                       | Description                                                                                                                                                                                                                                                    |
| --- | ------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `getCrossWarehouseStock(User actor)`                                | **Input**: actor (`WAREHOUSE_MANAGER`). **Xu ly noi bo**: query ton kha dung cua 3 kho vat ly (loai `IN_TRANSIT`, `Quarantine`), read-only, khong reserve. **Output**: `List<CrossWarehouseStockResponse>`.                                                       |
| 02  | `createTransferRequest(CreateTransferRequestRequest request, User actor)` | **Input**: request (sourceWarehouseId, destWarehouseId, items[]), actor. **Xu ly noi bo**: validate `source != dest` -> luu `transfer_requests` (`status=DRAFT`) -> ghi audit `TRANSFER_REQUEST_CREATED`. **Output**: `TransferRequestResponse`.                    |
| 03  | `approveTransferRequest(Long id, User ceo)`                          | CEO duyet: `status = APPROVED`, khong reserve, khong sinh bien dong inventory.                                                                                                                                                                                     |

#### c. Sequence Diagram(s)

```
Truong kho -> TransferRequestController.createRequest(request)
  TransferRequestController -> TransferRequestService.createTransferRequest(request, actor)
    TransferRequestService -> validate sourceWarehouseId != destWarehouseId
    TransferRequestService -> transferRequestRepository.save(request{status=DRAFT})
    TransferRequestService -> auditLogService.log(TRANSFER_REQUEST_CREATED)
  TransferRequestService --> TransferRequestController : TransferRequestResponse
TransferRequestController --> Truong kho : 201 Created

CEO -> TransferRequestController.approve(id)
  TransferRequestController -> TransferRequestService.approveTransferRequest(id, ceo)
    TransferRequestService -> transferRequest.setStatus(APPROVED)
    TransferRequestService -> auditLogService.log(TRANSFER_REQUEST_APPROVED)
  TransferRequestService --> TransferRequestController : TransferRequestResponse
TransferRequestController --> CEO : 200 OK
```

#### d. Database Queries

```sql
-- 1/ Ton kha dung lien kho (read-only)
SELECT w.id AS warehouse_id, w.code, i.product_id,
       SUM(i.total_qty - i.reserved_qty) AS available_qty
FROM inventories i
JOIN warehouses w ON w.id = i.warehouse_id
JOIN warehouse_locations wl ON wl.id = i.location_id
WHERE w.type = 'PHYSICAL' AND wl.is_quarantine = false
GROUP BY w.id, w.code, i.product_id;

-- 2/ Tao transfer request
INSERT INTO transfer_requests (source_warehouse_id, dest_warehouse_id, status, created_by, created_at)
VALUES (?, ?, 'DRAFT', ?, NOW()) RETURNING id;

-- 3/ CEO duyet
UPDATE transfer_requests SET status = 'APPROVED', approved_by = ?, approved_at = NOW() WHERE id = ?;
```

---

### 5.2 Transfer Order Creation

#### a. Class Diagram

```
TransferController ──uses──► InterWarehouseTransferService ──uses──► TransferRepository ──maps──► Transfer (entity)
                                       └──► TransferItem
```

#### b. Class Specifications

**InterWarehouseTransferService Class**

| No  | Method                                                     | Description                                                                                                                                                                                                                                       |
| --- | ---------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `createTransfer(CreateTransferRequest request, User actor)`        | **Input**: request (sourceWarehouseId, destWarehouseId, items[]), actor (`PLANNER`). **Xu ly noi bo**: validate `source != dest`, SKU ton tai -> luu `transfers` (`status=NEW`, ma `TRF-*`) + `transfer_items` (`sent_qty=0`) -> ghi audit `TRANSFER_CREATED`. Chua reserve. **Output**: `TransferResponse`. |

#### c. Sequence Diagram(s)

```
Planner -> TransferController.createTransfer(request)
  TransferController -> InterWarehouseTransferService.createTransfer(request, actor)
    InterWarehouseTransferService -> validate sourceWarehouseId != destWarehouseId
    InterWarehouseTransferService -> transferRepository.save(transfer{status=NEW, code=TRF-*})
    InterWarehouseTransferService -> transferItemRepository.saveAll(items{sent_qty=0})
    InterWarehouseTransferService -> auditLogService.log(TRANSFER_CREATED, before=null, after=snapshot)
  InterWarehouseTransferService --> TransferController : TransferResponse
TransferController --> Planner : 201 Created
```

#### d. Database Queries

```sql
INSERT INTO transfers (code, source_warehouse_id, dest_warehouse_id, status, created_by, created_at)
VALUES (?, ?, ?, 'NEW', ?, NOW()) RETURNING id;

INSERT INTO transfer_items (transfer_id, product_id, planned_qty, sent_qty, received_qty)
VALUES (?, ?, ?, 0, 0);
```

---

### 5.3 Transfer Approval & Reserve Stock

#### a. Class Diagram

```
TransferApprovalController ──uses──► TransferApprovalService ──uses──► TransferRepository
                                              │
                                              └──uses──► FIFOSelector / WarehouseProductReservationRepository
```

#### b. Class Specifications

**TransferApprovalService Class**

| No  | Method                                                   | Description                                                                                                                                                                                                                                                        |
| --- | --------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `approveTransfer(Long transferId, User actor)`                    | **Input**: transferId (`NEW`), actor (`WAREHOUSE_MANAGER` kho nguon). **Xu ly noi bo**: kiem tra ton FIFO-eligible du cho moi item (else `INSUFFICIENT_STOCK`) -> reserve theo `fifoSelector` -> `status=APPROVED` -> ghi audit `TRANSFER_APPROVED`. **Output**: `TransferResponse`. |
| 02  | `rejectTransfer(Long transferId, String reason, User actor)`       | **Input**: transferId, reason bat buoc. **Xu ly noi bo**: `status=REJECTED`, khong reserve; ghi audit `TRANSFER_REJECTED`. **Output**: `TransferResponse`.                                                                                                          |

#### c. Sequence Diagram(s)

```
Truong kho nguon -> TransferApprovalController.approve(transferId)
  TransferApprovalController -> TransferApprovalService.approveTransfer(transferId, actor)
    TransferApprovalService -> transferRepository.findById(transferId)    -> Transfer (must be NEW)
    loop each item
      TransferApprovalService -> fifoSelector.checkAvailable(warehouseId, productId, qty)  [throw neu thieu]
      TransferApprovalService -> reservationRepository.reserve(warehouseId, productId, qty)
    end
    TransferApprovalService -> transfer.setStatus(APPROVED)
    TransferApprovalService -> auditLogService.log(TRANSFER_APPROVED, before, after)
  TransferApprovalService --> TransferApprovalController : TransferResponse
TransferApprovalController --> Truong kho nguon : 200 OK
```

#### d. Database Queries

```sql
UPDATE warehouse_product_reservations
SET reserved_qty = reserved_qty + ?, version = version + 1
WHERE warehouse_id = ? AND product_id = ? AND version = ?;

UPDATE transfers SET status = 'APPROVED', approved_by = ?, approved_at = NOW() WHERE id = ? AND status = 'NEW';
UPDATE transfers SET status = 'REJECTED', reject_reason = ? WHERE id = ? AND status = 'NEW';
```

---

### 5.4 Transfer Ship (Dispatch + Outbound QC + Handover)

#### a. Class Diagram

```
TransferShipmentController ──uses──► TransferShipmentService ──uses──► TripRepository (trip_type=TRANSFER)
                                              │
                                              └──uses──► InventoryRepository (tru nguon, cong IN_TRANSIT)
```

#### b. Class Specifications

**TransferShipmentService Class**

| No  | Method                                                            | Description                                                                                                                                                                                                                                                                                                            |
| --- | ------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `dispatchTransferTrip(CreateTransferTripRequest request, User actor)`      | **Input**: request (transferId `APPROVED`, vehicleId, driverId thuoc kho nguon). **Xu ly noi bo**: tuong tu `TripDispatchService` (weight/volume check) -> tao `trips` (`trip_type=TRANSFER`, ma `TTR-*`). **Output**: `TripResponse`.                                                                                    |
| 02  | `confirmDeparture(Long transferId, User actor)`                            | **Input**: transferId, actor (`DRIVER`). **Xu ly noi bo**: kiem tra anh handover da upload -> tru `inventories` kho nguon, cong `inventories` kho ao `IN_TRANSIT` -> `transfer.status=IN_TRANSIT`; ghi audit `TRANSFER_SHIPPED`. **Output**: `TransferResponse`.                                                          |

#### c. Sequence Diagram(s)

```
Tai xe -> TransferShipmentController.confirmDeparture(transferId)
  TransferShipmentController -> TransferShipmentService.confirmDeparture(transferId, actor)
    TransferShipmentService -> validate handoverPhotoRef da co
    TransferShipmentService -> inventoryRepository.decrease(sourceWarehouseId, productId, qty)
    TransferShipmentService -> inventoryRepository.increase(IN_TRANSIT_WAREHOUSE_ID, productId, qty)
    TransferShipmentService -> transfer.setStatus(IN_TRANSIT)
    TransferShipmentService -> auditLogService.log(TRANSFER_SHIPPED, before, after)
  TransferShipmentService --> TransferShipmentController : TransferResponse
TransferShipmentController --> Tai xe : 200 OK
```

#### d. Database Queries

```sql
INSERT INTO trips (trip_type, warehouse_id, vehicle_id, driver_id, status, created_at)
VALUES ('TRANSFER', ?, ?, ?, 'PLANNED', NOW()) RETURNING id;

UPDATE inventories SET total_qty = total_qty - ?, version = version + 1
WHERE warehouse_id = ? AND product_id = ? AND batch_id = ? AND version = ?;

INSERT INTO inventories (warehouse_id, product_id, batch_id, location_id, total_qty)
VALUES ((SELECT id FROM warehouses WHERE type='IN_TRANSIT'), ?, ?, NULL, ?)
ON CONFLICT (warehouse_id, product_id, batch_id) DO UPDATE SET total_qty = inventories.total_qty + EXCLUDED.total_qty;

UPDATE transfers SET status = 'IN_TRANSIT', updated_at = NOW() WHERE id = ?;
```

---

### 5.5 Transfer Receive (Count + QC + Final Approval)

#### a. Class Diagram

```
TransferReceiveController ──uses──► TransferReceiveService ──uses──► TransferDiscrepancyService
                                              │
                                              └──uses──► InventoryRepository / QuarantineRecordRepository
```

#### b. Class Specifications

**TransferReceiveService Class**

| No  | Method                                                              | Description                                                                                                                                                                                                                                                                                                                                                        |
| --- | -------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `recordBlindCount(Long transferId, BlindCountRequest request, User actor)`    | **Input**: transferId (`IN_TRANSIT`), request.receivedQty[] theo item, actor (`WAREHOUSE_STAFF`). **Xu ly noi bo**: luu received draft (chua cap nhat inventory). **Output**: `TransferReceiveDraftResponse`.                                                                                                                                                       |
| 02  | `confirmFinalReceive(Long transferId, FinalReceiveRequest request, User actor)` | **Input**: transferId, request (QC result tung item, bin chon cho hang dat), actor (`WAREHOUSE_MANAGER` kho dich). **Xu ly noi bo**: `transferDiscrepancyService.calculate(sent_qty, received_qty)` -> neu khop: tru `IN_TRANSIT`, cong kho dich, `status=COMPLETED`; neu thieu: tao `adjustments(TRANSFER_DISCREPANCY)`, `status=COMPLETED_WITH_DISCREPANCY`; neu QC fail: vao Quarantine (`origin=INTERNAL_TRANSFER`). **Output**: `TransferResponse`. |

#### c. Sequence Diagram(s)

```
Truong kho dich -> TransferReceiveController.confirmFinalReceive(transferId, request)
  TransferReceiveController -> TransferReceiveService.confirmFinalReceive(transferId, request, actor)
    TransferReceiveService -> transferDiscrepancyService.calculate(sentQty, receivedQty)
    alt received == sent AND QC pass
      TransferReceiveService -> inventoryRepository.decrease(IN_TRANSIT, productId, qty)
      TransferReceiveService -> inventoryRepository.increase(destWarehouseId, productId, batchId, binId, qty)
      TransferReceiveService -> transfer.setStatus(COMPLETED)
    else received < sent
      TransferReceiveService -> adjustmentRepository.save(type=TRANSFER_DISCREPANCY)
      TransferReceiveService -> transfer.setStatus(COMPLETED_WITH_DISCREPANCY)
    else QC fail
      TransferReceiveService -> quarantineRecordRepository.save(origin=INTERNAL_TRANSFER)
    end
    TransferReceiveService -> auditLogService.log(TRANSFER_RECEIVED, before, after)
  TransferReceiveService --> TransferReceiveController : TransferResponse
TransferReceiveController --> Truong kho dich : 200 OK
```

#### d. Database Queries

```sql
-- 1/ Tinh chenh lech
SELECT ti.planned_qty AS sent_qty, ?::int AS received_qty, (?::int - ti.planned_qty) AS variance_qty
FROM transfer_items ti WHERE ti.id = ?;

-- 2/ Khop: tru IN_TRANSIT, cong kho dich
UPDATE inventories SET total_qty = total_qty - ? WHERE warehouse_id = (SELECT id FROM warehouses WHERE type='IN_TRANSIT') AND product_id = ?;
UPDATE inventories SET total_qty = total_qty + ?, version = version + 1 WHERE warehouse_id = ? AND product_id = ? AND location_id = ?;
UPDATE transfers SET status = 'COMPLETED', updated_at = NOW() WHERE id = ?;

-- 3/ Chenh lech: tao adjustment
INSERT INTO adjustments (transfer_id, product_id, type, quantity_adjustment, created_at)
VALUES (?, ?, 'TRANSFER_DISCREPANCY', ?, NOW());
UPDATE transfers SET status = 'COMPLETED_WITH_DISCREPANCY' WHERE id = ?;

-- 4/ QC fail: Quarantine
INSERT INTO quarantine_records (warehouse_id, product_id, quantity, origin, transfer_id, created_at)
VALUES (?, ?, ?, 'INTERNAL_TRANSFER', ?, NOW());
```

---

## 6. Stocktake & Adjustment (Spec 006)

### 6.1 Stocktake Creation & Count

#### a. Class Diagram

```
StocktakeController ──uses──► StocktakeService ──uses──► StocktakeRepository ──maps──► Stocktake (entity)
                                       │                                                       └──► StocktakeItem
                                       └──uses──► LocationLockService (lock/unlock bin)
```

#### b. Class Specifications

**StocktakeService Class**

| No  | Method                                                     | Description                                                                                                                                                                                                                                                       |
| --- | ------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `createStocktake(Long warehouseId, User actor)`                  | **Input**: warehouseId, actor (`STOREKEEPER`). **Xu ly noi bo**: kiem tra khong co stocktake `IN_PROGRESS` khac cho kho (else `WAREHOUSE_LOCKED`) -> snapshot `system_qty` tu `inventories` cho tung product/location -> luu `stocktakes` (`status=IN_PROGRESS`) + `stocktake_items` -> `locationLockService.lockLocationsForStocktake(warehouseId)` -> ghi audit `STOCKTAKE_CREATED`. **Output**: `StocktakeResponse`.                                       |
| 02  | `recordCount(Long stocktakeId, RecordCountRequest request, User actor)` | **Input**: stocktakeId, request.counts[] (product/location/receivedQty), actor (`WAREHOUSE_STAFF`). **Xu ly noi bo**: cap nhat `stocktake_items.received_qty` -> tinh `variance_qty = system_qty - received_qty`. **Output**: `StocktakeResponse`.                    |

#### c. Sequence Diagram(s)

```
Thu kho -> StocktakeController.createStocktake(warehouseId)
  StocktakeController -> StocktakeService.createStocktake(warehouseId, actor)
    StocktakeService -> validate no other stocktake IN_PROGRESS for warehouse
    StocktakeService -> snapshot inventories AS system_qty
    StocktakeService -> stocktakeRepository.save(stocktake{status=IN_PROGRESS})
    StocktakeService -> locationLockService.lockLocationsForStocktake(warehouseId)
    StocktakeService -> auditLogService.log(STOCKTAKE_CREATED, before=null, after=snapshot)
  StocktakeService --> StocktakeController : StocktakeResponse
StocktakeController --> Thu kho : 201 Created
```

#### d. Database Queries

```sql
SELECT EXISTS (SELECT 1 FROM stocktakes WHERE warehouse_id = ? AND status = 'IN_PROGRESS');

INSERT INTO stocktakes (warehouse_id, status, created_by, created_at) VALUES (?, 'IN_PROGRESS', ?, NOW()) RETURNING id;

INSERT INTO stocktake_items (stocktake_id, product_id, location_id, system_qty)
SELECT ?, i.product_id, i.location_id, i.total_qty FROM inventories i WHERE i.warehouse_id = ?;

UPDATE warehouse_locations SET is_locked = true WHERE warehouse_id = ? AND type = 'BIN' AND is_quarantine = false;

UPDATE stocktake_items SET received_qty = ?, variance_qty = system_qty - ? WHERE id = ?;
```

---

### 6.2 Adjustment Approval

#### a. Class Diagram

```
StocktakeApprovalController ──uses──► StocktakeApprovalService ──uses──► InventoryRepository
                                              │
                                              └──uses──► AdjustmentRepository / LocationLockService (unlock)
```

#### b. Class Specifications

**StocktakeApprovalService Class**

| No  | Method                                                     | Description                                                                                                                                                                                                                                                              |
| --- | ------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 01  | `approveAdjustment(Long stocktakeId, User actor)`                | **Input**: stocktakeId, actor (`WAREHOUSE_MANAGER`). **Xu ly noi bo**: voi moi `stocktake_item` co `variance_qty != 0`: `inventoryRepository.setTotalQty(receivedQty)` + tao `adjustments` (`type=STOCKTAKE`) -> `locationLockService.unlock(warehouseId)` -> `stocktake.status=CLOSED` -> ghi audit `STOCKTAKE_APPROVED`. **Output**: `StocktakeResponse`. Flat approval, khong phan cap gia tri. |
| 02  | `rejectAdjustment(Long stocktakeId, User actor)`                 | **Input**: stocktakeId. **Xu ly noi bo**: `status` quay lai `IN_PROGRESS`, kho van lock, yeu cau dem lai. **Output**: `StocktakeResponse`.                                                                                                                                 |

#### c. Sequence Diagram(s)

```
Truong kho -> StocktakeApprovalController.approve(stocktakeId)
  StocktakeApprovalController -> StocktakeApprovalService.approveAdjustment(stocktakeId, actor)
    loop each stocktake_item where variance_qty != 0
      StocktakeApprovalService -> inventoryRepository.setTotalQty(productId, locationId, receivedQty)
      StocktakeApprovalService -> adjustmentRepository.save(type=STOCKTAKE, quantity=varianceQty)
    end
    StocktakeApprovalService -> locationLockService.unlock(warehouseId)
    StocktakeApprovalService -> stocktake.setStatus(CLOSED)
    StocktakeApprovalService -> auditLogService.log(STOCKTAKE_APPROVED, before, after)
  StocktakeApprovalService --> StocktakeApprovalController : StocktakeResponse
StocktakeApprovalController --> Truong kho : 200 OK
```

#### d. Database Queries

```sql
UPDATE inventories SET total_qty = ?, version = version + 1 WHERE product_id = ? AND location_id = ? AND warehouse_id = ?;

INSERT INTO adjustments (stocktake_id, product_id, type, quantity_adjustment, created_at)
VALUES (?, ?, 'STOCKTAKE', ?, NOW());

UPDATE warehouse_locations SET is_locked = false WHERE warehouse_id = ? AND is_locked = true;

UPDATE stocktakes SET status = 'CLOSED', approved_by = ?, approved_at = NOW() WHERE id = ?;
```

---

## 7. Pricing & COGS Management (Spec 007)

### 7.1 Price List Creation

#### a. Class Diagram

```
PriceListController ──uses──► PricingService ──uses──► PriceHistoryRepository ──maps──► PriceHistory (entity)
```

#### b. Class Specifications

**PricingService Class**

| No  | Method                                                        | Description                                                                                                                                                                                                                        |
| --- | ---------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `createPriceList(CreatePriceListRequest request, User actor)`          | **Input**: request.items[] (productId, costPrice, sellingPrice, effectiveDate), actor (`ACCOUNTANT`). **Xu ly noi bo**: `validatePriceRange` (>0) -> luu `price_history` (`status=DRAFT`) -> ghi audit `PRICE_LIST_CREATED`. **Output**: `PriceListResponse`. |
| 02  | `importExcel(MultipartFile file, User actor)`                          | Parse Excel theo template, map tung dong -> goi `createPriceList` hang loat.                                                                                                                                                            |

#### c. Sequence Diagram(s)

```
Ke toan vien -> PriceListController.createPriceList(request)
  PriceListController -> PricingService.createPriceList(request, actor)
    loop each item
      PricingService -> validatePriceRange(costPrice, sellingPrice)
      PricingService -> priceHistoryRepository.save(item{status=DRAFT})
    end
    PricingService -> auditLogService.log(PRICE_LIST_CREATED, before=null, after=snapshot)
  PricingService --> PriceListController : PriceListResponse
PriceListController --> Ke toan vien : 201 Created
```

#### d. Database Queries

```sql
INSERT INTO price_history (product_id, cost_price, selling_price, effective_date, end_date, status, created_at)
VALUES (?, ?, ?, ?, NULL, 'DRAFT', NOW());
```

---

### 7.2 Price Approval

#### a. Class Diagram

```
PriceApprovalController ──uses──► PriceApprovalService ──uses──► PriceHistoryRepository
```

#### b. Class Specifications

**PriceApprovalService Class**

| No  | Method                                                | Description                                                                                                                                     |
| --- | ------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `approvePriceList(Long priceListId, User actor)`                | **Input**: priceListId (`DRAFT`), actor (`ACCOUNTANT_MANAGER`). **Xu ly noi bo**: `status=ACTIVE`; ghi audit `PRICE_LIST_APPROVED`. **Output**: `PriceListResponse`. |

#### c. Sequence Diagram(s)

```
Ke toan truong -> PriceApprovalController.approve(priceListId)
  PriceApprovalController -> PriceApprovalService.approvePriceList(priceListId, actor)
    PriceApprovalService -> priceHistoryRepository.updateStatus(priceListId, ACTIVE)
    PriceApprovalService -> auditLogService.log(PRICE_LIST_APPROVED, before, after)
  PriceApprovalService --> PriceApprovalController : PriceListResponse
PriceApprovalController --> Ke toan truong : 200 OK
```

#### d. Database Queries

```sql
UPDATE price_history SET status = 'ACTIVE', approved_by = ?, approved_at = NOW() WHERE id = ? AND status = 'DRAFT';
```

---

### 7.3 COGS Auto-Calculation (Snapshot)

#### a. Class Diagram

```
DeliveryOrderService ──uses──► PriceHistoryRepository.findEffectivePrice(productId, date)
```

#### b. Class Specifications

**PriceHistoryRepository (query method)**

| No  | Method                                                          | Description                                                                                                                                                       |
| --- | ---------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `findEffectivePrice(Long productId, LocalDate date)`                     | **Input**: productId, date (ngay tao DO). **Xu ly noi bo**: `WHERE product_id=? AND effective_date <= date AND (end_date IS NULL OR end_date > date) ORDER BY effective_date DESC LIMIT 1`. **Output**: `PriceHistory{costPrice, sellingPrice}`, snapshot co dinh vao `delivery_order_items.unit_price`. |

#### c. Sequence Diagram(s)

```
DeliveryOrderService -> priceHistoryRepository.findEffectivePrice(productId, today)
  priceHistoryRepository --> DeliveryOrderService : PriceHistory (or throw NO_ACTIVE_PRICE)
DeliveryOrderService -> deliveryOrderItem.setUnitPrice(priceHistory.sellingPrice)
```

#### d. Database Queries

```sql
SELECT cost_price, selling_price FROM price_history
WHERE product_id = ? AND status = 'ACTIVE'
  AND effective_date <= ? AND (end_date IS NULL OR end_date > ?)
ORDER BY effective_date DESC LIMIT 1;
```

---

## 8. Finance & Billing & Closing (Spec 008)

### 8.1 Invoice Reconciliation

#### a. Class Diagram

```
BillingNotificationController ──uses──► InvoiceServiceImpl ──uses──► BillingNotificationRepository / InvoiceRepository
```

#### b. Class Specifications

**InvoiceServiceImpl Class**

| No  | Method                                                    | Description                                                                                                                                                          |
| --- | ---------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `getBillingWorklist(User actor)`                                    | Tra danh sach `billing_notifications` chua confirm, join `invoices`.                                                                                                    |
| 02  | `confirmReconciliation(Long billingNotificationId, User actor)`      | **Input**: id, actor (`ACCOUNTANT`). **Xu ly noi bo**: mark `is_confirmed=true`; ghi audit `INVOICE_RECONCILED`. **Output**: void.                                       |

#### c. Sequence Diagram(s)

```
Ke toan vien -> BillingNotificationController.confirm(id)
  BillingNotificationController -> InvoiceServiceImpl.confirmReconciliation(id, actor)
    InvoiceServiceImpl -> billingNotificationRepository.markConfirmed(id)
    InvoiceServiceImpl -> auditLogService.log(INVOICE_RECONCILED, before, after)
  InvoiceServiceImpl --> BillingNotificationController : void
BillingNotificationController --> Ke toan vien : 200 OK
```

#### d. Database Queries

```sql
SELECT bn.id, bn.do_id, i.id AS invoice_id, i.total_amount
FROM billing_notifications bn
JOIN invoices i ON i.do_id = bn.do_id
WHERE bn.is_confirmed = false;

UPDATE billing_notifications SET is_confirmed = true, confirmed_at = NOW() WHERE id = ?;
```

---

### 8.2 Payment Receipt Recording

#### a. Class Diagram

```
PaymentReceiptController ──uses──► PaymentReceiptServiceImpl ──uses──► PaymentReceiptRepository / InvoiceRepository / DealerRepository
```

#### b. Class Specifications

**PaymentReceiptServiceImpl Class**

| No  | Method                                                          | Description                                                                                                                                                                                                                                                                                                    |
| --- | ------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `recordPayment(RecordPaymentRequest request, User actor)`                  | **Input**: request (dealerId, amount, invoiceIds[] can tru), actor (`ACCOUNTANT`). **Xu ly noi bo**: validate `amount > 0` -> luu `payment_receipts` -> voi moi invoice: cap nhat `status` (`PAID` neu du, `PARTIALLY_PAID` neu thieu) -> `dealers.current_balance -= amount` -> `creditLimitService.reEvaluate(dealerId)` (neu `balance < limit*0.8` va khong overdue -> `ACTIVE`) -> ghi audit `PAYMENT_RECEIVED`. **Output**: `PaymentReceiptResponse`. |

#### c. Sequence Diagram(s)

```
Ke toan vien -> PaymentReceiptController.recordPayment(request)
  PaymentReceiptController -> PaymentReceiptServiceImpl.recordPayment(request, actor)
    PaymentReceiptServiceImpl -> validate amount > 0
    PaymentReceiptServiceImpl -> paymentReceiptRepository.save(receipt)
    loop each invoiceId
      PaymentReceiptServiceImpl -> invoiceRepository.applyPayment(invoiceId, allocatedAmount)
    end
    PaymentReceiptServiceImpl -> dealerRepository.decreaseBalance(dealerId, amount)
    PaymentReceiptServiceImpl -> creditLimitService.reEvaluate(dealerId)
    PaymentReceiptServiceImpl -> auditLogService.log(PAYMENT_RECEIVED, before, after)
  PaymentReceiptServiceImpl --> PaymentReceiptController : PaymentReceiptResponse
PaymentReceiptController --> Ke toan vien : 201 Created
```

#### d. Database Queries

```sql
INSERT INTO payment_receipts (dealer_id, amount, payment_date, created_at) VALUES (?, ?, NOW(), NOW()) RETURNING id;

UPDATE invoices SET status = CASE WHEN paid_amount + ? >= total_amount THEN 'PAID' ELSE 'PARTIALLY_PAID' END,
                    paid_amount = paid_amount + ?
WHERE id = ?;

UPDATE dealers SET current_balance = current_balance - ? WHERE id = ?;

UPDATE dealers SET credit_status = 'ACTIVE'
WHERE id = ? AND current_balance < credit_limit * 0.8
  AND NOT EXISTS (SELECT 1 FROM invoices WHERE dealer_id = dealers.id AND status IN ('UNPAID','PARTIALLY_PAID') AND due_date < NOW() - INTERVAL '30 days');
```

---

### 8.3 Aging Report

#### a. Class Diagram

```
ReportController ──uses──► ReportServiceImpl ──uses──► InvoiceRepository (aggregate query)
```

#### b. Class Specifications

**ReportServiceImpl Class**

| No  | Method                                    | Description                                                                                                                                                                       |
| --- | ---------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `getAgingReport(User actor)`                     | **Input**: actor (`ACCOUNTANT_MANAGER`/`CEO`). **Xu ly noi bo**: query `invoices` chua `CLOSED`, nhom theo `dealer_id` + bucket (`Not due`/`1-30`/`31-60`/`>60`) theo `due_date`. **Output**: `AgingReportResponse`, read-only. |

#### c. Sequence Diagram(s)

```
Ke toan truong -> ReportController.getAgingReport()
  ReportController -> ReportServiceImpl.getAgingReport(actor)
    ReportServiceImpl -> invoiceRepository.aggregateAging()
  ReportServiceImpl --> ReportController : AgingReportResponse
ReportController --> Ke toan truong : 200 OK
```

#### d. Database Queries

```sql
SELECT dealer_id,
       CASE WHEN due_date >= CURRENT_DATE THEN 'Not due'
            WHEN CURRENT_DATE - due_date <= 30 THEN '1-30 days'
            WHEN CURRENT_DATE - due_date <= 60 THEN '31-60 days'
            ELSE '>60 days' END AS aging_bucket,
       SUM(total_amount - paid_amount) AS balance
FROM invoices
WHERE status NOT IN ('CLOSED', 'PAID')
GROUP BY dealer_id, aging_bucket;
```

---

### 8.4 Accounting Period Closing

#### a. Class Diagram

```
AccountingPeriodController ──uses──► AccountingPeriodServiceImpl ──uses──► AccountingPeriodRepository
```

#### b. Class Specifications

**AccountingPeriodServiceImpl Class**

| No  | Method                                       | Description                                                                                                                                                                        |
| --- | ------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `closePeriod(Long periodId, User actor)`             | **Input**: periodId, actor (`ACCOUNTANT_MANAGER`). **Xu ly noi bo**: validate moi ky truoc do da `CLOSED` (else `PERIOD_SEQUENCE_ERROR`) -> `status=CLOSED`; ghi audit `PERIOD_CLOSED`. **Output**: `AccountingPeriodResponse`. |

#### c. Sequence Diagram(s)

```
Ke toan truong -> AccountingPeriodController.close(periodId)
  AccountingPeriodController -> AccountingPeriodServiceImpl.closePeriod(periodId, actor)
    AccountingPeriodServiceImpl -> validate all prior periods CLOSED
    AccountingPeriodServiceImpl -> accountingPeriodRepository.updateStatus(periodId, CLOSED)
    AccountingPeriodServiceImpl -> auditLogService.log(PERIOD_CLOSED, before, after)
  AccountingPeriodServiceImpl --> AccountingPeriodController : AccountingPeriodResponse
AccountingPeriodController --> Ke toan truong : 200 OK
```

#### d. Database Queries

```sql
SELECT EXISTS (SELECT 1 FROM accounting_periods WHERE period_end < (SELECT period_start FROM accounting_periods WHERE id = ?) AND status != 'CLOSED');

UPDATE accounting_periods SET status = 'CLOSED', closed_by = ?, closed_at = NOW() WHERE id = ?;
```

---

## 9. Returns, Scrap & Disposal (Spec 009)

### 9.1 Dealer Return & Credit Note

#### a. Class Diagram

```
ReturnController ──uses──► ReturnService ──uses──► ReceiptRepository (type=RETURN)
                                   └──uses──► CreditNoteService ──uses──► CreditNoteRepository
```

#### b. Class Specifications

**ReturnService Class**

| No  | Method                                                | Description                                                                                                                                                                                                                          |
| --- | ------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `receiveDealerReturn(CreateReturnRequest request, User actor)`  | **Input**: request (dealerId, doId goc, items[]), actor (`STOREKEEPER`). **Xu ly noi bo**: tao `receipts` (`type=RETURN`) -> QC hang hoan -> neu dat: cong ton kho bin; neu fail: vao Quarantine. **Output**: `ReceiptResponse`.        |

**CreditNoteService Class**

| No  | Method                                                | Description                                                                                                                                                                     |
| --- | ------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `createCreditNote(Long returnReceiptId, User actor)`             | **Input**: returnReceiptId (hang QC dat), actor (`ACCOUNTANT`). **Xu ly noi bo**: tinh `amount = returnQty * original_unit_price` (tu DO goc) -> luu `credit_notes` -> tru `dealers.current_balance`. **Output**: `CreditNoteResponse`. |

#### c. Sequence Diagram(s)

```
Thu kho -> ReturnController.receiveDealerReturn(request)
  ReturnController -> ReturnService.receiveDealerReturn(request, actor)
    ReturnService -> receiptRepository.save(receipt{type=RETURN})
    alt QC pass
      ReturnService -> inventoryRepository.increase(warehouseId, productId, binId, qty)
    else QC fail
      ReturnService -> quarantineRecordRepository.save(origin=CUSTOMER_RETURN_QC_FAIL)
    end
  ReturnService --> ReturnController : ReceiptResponse
ReturnController --> Thu kho : 201 Created

Ke toan vien -> CreditNoteController.createCreditNote(returnReceiptId)
  CreditNoteController -> CreditNoteService.createCreditNote(returnReceiptId, actor)
    CreditNoteService -> calculate amount = returnQty * originalUnitPrice
    CreditNoteService -> creditNoteRepository.save(creditNote)
    CreditNoteService -> dealerRepository.decreaseBalance(dealerId, amount)
    CreditNoteService -> auditLogService.log(CREDIT_NOTE_CREATED, before, after)
  CreditNoteService --> CreditNoteController : CreditNoteResponse
CreditNoteController --> Ke toan vien : 201 Created
```

#### d. Database Queries

```sql
INSERT INTO receipts (type, dealer_id, warehouse_id, status, created_by, created_at)
VALUES ('RETURN', ?, ?, 'PENDING_RECEIPT', ?, NOW()) RETURNING id;

UPDATE inventories SET total_qty = total_qty + ?, version = version + 1
WHERE warehouse_id = ? AND product_id = ? AND location_id = ?;

INSERT INTO credit_notes (dealer_id, receipt_id, amount, reason, created_at)
VALUES (?, ?, ?, 'CUSTOMER_RETURN', NOW());

UPDATE dealers SET current_balance = current_balance - ? WHERE id = ?;
```

---

### 9.2 Disposal Approval & Execution

#### a. Class Diagram

```
DisposalController ──uses──► DisposalService ──uses──► DamageReportRepository / AdjustmentRepository / QuarantineRecordRepository
```

#### b. Class Specifications

**DisposalService Class**

| No  | Method                                                     | Description                                                                                                                                                                                                                                       |
| --- | ------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `createDamageReport(CreateDamageReportRequest request, User actor)` | **Input**: request (quarantineRecordId, quantity, origin, reason), actor (`STOREKEEPER`). **Xu ly noi bo**: luu `damage_reports` (`status=PENDING`). **Output**: `DamageReportResponse`.                                                            |
| 02  | `approveDisposal(Long damageReportId, User actor)`                  | **Input**: damageReportId, actor (`WAREHOUSE_MANAGER`, flat approval). **Xu ly noi bo**: `status=APPROVED`; ghi audit `DISPOSAL_APPROVED`. **Output**: `DamageReportResponse`.                                                                        |
| 03  | `executeDisposal(Long damageReportId, User actor)`                  | **Input**: damageReportId (`APPROVED`), actor (`STOREKEEPER`). **Xu ly noi bo**: tru `quarantine_records`, tao `adjustments` (`type=DISPOSAL`), `status=EXECUTED`; ghi audit `DISPOSAL_EXECUTED`. **Output**: `DamageReportResponse`.                     |

#### c. Sequence Diagram(s)

```
Truong kho -> DisposalController.approve(damageReportId)
  DisposalController -> DisposalService.approveDisposal(damageReportId, actor)
    DisposalService -> damageReport.setStatus(APPROVED)
    DisposalService -> auditLogService.log(DISPOSAL_APPROVED, before, after)
  DisposalService --> DisposalController : DamageReportResponse
DisposalController --> Truong kho : 200 OK

Thu kho -> DisposalController.execute(damageReportId)
  DisposalController -> DisposalService.executeDisposal(damageReportId, actor)
    DisposalService -> quarantineRecordRepository.decrease(quantity)
    DisposalService -> adjustmentRepository.save(type=DISPOSAL)
    DisposalService -> damageReport.setStatus(EXECUTED)
    DisposalService -> auditLogService.log(DISPOSAL_EXECUTED, before, after)
  DisposalService --> DisposalController : DamageReportResponse
DisposalController --> Thu kho : 200 OK
```

#### d. Database Queries

```sql
INSERT INTO damage_reports (quarantine_record_id, quantity, origin, reason, status, created_at)
VALUES (?, ?, ?, ?, 'PENDING', NOW()) RETURNING id;

UPDATE damage_reports SET status = 'APPROVED', approved_by = ?, approved_at = NOW() WHERE id = ?;

UPDATE quarantine_records SET quantity = quantity - ? WHERE id = ?;

INSERT INTO adjustments (product_id, type, quantity_adjustment, created_at)
VALUES (?, 'DISPOSAL', ?, NOW());

UPDATE damage_reports SET status = 'EXECUTED', executed_at = NOW() WHERE id = ?;
```

---

## 10. Reports, Dashboards & Alerts (Spec 010)

### 10.1 CEO Dashboard

#### a. Class Diagram

```
DashboardController ──uses──► ReportServiceImpl ──uses──► (aggregate queries across inventories/invoices/deliveries/receipt_items)
```

#### b. Class Specifications

**ReportServiceImpl Class**

| No  | Method                             | Description                                                                                                                                                                                       |
| --- | --------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `getDashboardKpi(User actor)`             | **Input**: actor (`CEO`). **Xu ly noi bo**: tong hop Inventory KPI (total value, low stock count), Credit KPI (A/R, aging), P&L (revenue YTD, COGS, margin%), QC KPI (accept/defect rate), OTD (on-time %). Read-only. **Output**: `DashboardResponse`. |

#### c. Sequence Diagram(s)

```
CEO -> DashboardController.getDashboardKpi()
  DashboardController -> ReportServiceImpl.getDashboardKpi(actor)
    ReportServiceImpl -> inventoryRepository.aggregateValue()
    ReportServiceImpl -> invoiceRepository.aggregateAging()
    ReportServiceImpl -> deliveryRepository.aggregateOtdRate()
    ReportServiceImpl -> receiptItemRepository.aggregateQcRate()
  ReportServiceImpl --> DashboardController : DashboardResponse
DashboardController --> CEO : 200 OK
```

#### d. Database Queries

```sql
-- Inventory value
SELECT SUM(i.total_qty * ph.cost_price) AS total_value
FROM inventories i
JOIN price_history ph ON ph.product_id = i.product_id AND ph.status = 'ACTIVE';

-- OTD rate
SELECT COUNT(*) FILTER (WHERE delivered_at <= requested_date)::float / COUNT(*) AS otd_rate
FROM delivery_orders WHERE status = 'COMPLETED';

-- QC accept rate
SELECT COUNT(*) FILTER (WHERE qc_result = 'PASS')::float / COUNT(*) AS accept_rate
FROM receipt_items WHERE qc_result IS NOT NULL;
```

---

### 10.2 Low Stock Alert Trigger/Resolve

#### a. Class Diagram

```
[InventoryChangeEvent] ──triggers──► StockAlertServiceImpl ──uses──► StockAlertRepository ──maps──► StockAlert (entity)
```

#### b. Class Specifications

**StockAlertServiceImpl Class**

| No  | Method                                                   | Description                                                                                                                                                                                                                                            |
| --- | --------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `onInventoryChanged(InventoryChangeEvent event)`                   | **Input**: event (warehouseId, productId). **Xu ly noi bo**: tinh `available_qty = total_qty - reserved_qty` -> so voi `product.reorderPoint` -> neu duoi nguong va chua co alert active: tao `stock_alerts` (`is_resolved=false`); neu hoi phuc va co alert active: `is_resolved=true`. **Output**: void (event-driven). |
| 02  | `resolveAlert(Long alertId, User actor)`                            | Planner mark resolved thu cong (VD sau khi lap DO nhap hang).                                                                                                                                                                                             |

#### c. Sequence Diagram(s)

```
[Event] InventoryChangeEvent -> StockAlertServiceImpl.onInventoryChanged(event)
  StockAlertServiceImpl -> calculate availableQty = total_qty - reserved_qty
  StockAlertServiceImpl -> productRepository.getReorderPoint(productId)
  alt availableQty < reorderPoint AND no active alert
    StockAlertServiceImpl -> stockAlertRepository.save(alert{is_resolved=false})
  else availableQty >= reorderPoint AND active alert exists
    StockAlertServiceImpl -> stockAlertRepository.markResolved(alertId)
  end
```

#### d. Database Queries

```sql
SELECT COALESCE(SUM(total_qty - reserved_qty), 0) AS available_qty
FROM inventories WHERE warehouse_id = ? AND product_id = ?;

INSERT INTO stock_alerts (warehouse_id, product_id, alert_type, is_resolved, created_at)
VALUES (?, ?, 'LOW_STOCK', false, NOW())
ON CONFLICT (warehouse_id, product_id, alert_type) WHERE is_resolved = false DO NOTHING;

UPDATE stock_alerts SET is_resolved = true, resolved_at = NOW()
WHERE warehouse_id = ? AND product_id = ? AND is_resolved = false;
```

---

### 10.3 Productivity Report

#### a. Class Diagram

```
ProductivityReportController ──uses──► ReportServiceImpl ──uses──► AuditLogRepository / DeliveryOrderItemRepository
```

#### b. Class Specifications

**ReportServiceImpl Class (Productivity)**

| No  | Method                                            | Description                                                                                                                                                                        |
| --- | -------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `getProductivityReport(Long warehouseId, DateRange range, User actor)` | **Input**: warehouseId, date range, actor (`WAREHOUSE_MANAGER`). **Xu ly noi bo**: dem so phieu xu ly + item picked/packed tu `audit_logs`/`delivery_order_item_allocations` theo staff, tinh efficiency = items/gio (uoc luong tu state-change timestamps). **Output**: `ProductivityReportResponse`, ho tro export Excel. |

#### c. Sequence Diagram(s)

```
Truong kho -> ProductivityReportController.getReport(warehouseId, range)
  ProductivityReportController -> ReportServiceImpl.getProductivityReport(warehouseId, range, actor)
    ReportServiceImpl -> auditLogRepository.countActionsByActor(warehouseId, range)
    ReportServiceImpl -> allocationRepository.sumPickedQtyByActor(warehouseId, range)
    ReportServiceImpl -> calculate efficiency = totalItems / totalHours
  ReportServiceImpl --> ProductivityReportController : ProductivityReportResponse
ProductivityReportController --> Truong kho : 200 OK
```

#### d. Database Queries

```sql
SELECT al.actor_id, COUNT(*) AS actions_count
FROM audit_logs al
WHERE al.warehouse_id = ? AND al.created_at BETWEEN ? AND ?
GROUP BY al.actor_id;

SELECT picked_by AS actor_id, SUM(picked_qty) AS total_picked
FROM delivery_order_item_allocations
WHERE warehouse_id = ? AND updated_at BETWEEN ? AND ?
GROUP BY picked_by;
```

---

## Schema Reference Summary

**Core Tables (All Specs):**
```
users ←→ user_warehouse_assignments ←→ warehouses
warehouse_locations (zones/bins with is_locked, is_quarantine)
products (SKU, unit, no serial/expiry/grade)
batches (product + received_date + source receipt)
inventories (warehouse + product + batch + location; optimistic locking via version)

receipts, receipt_items, batches ← Spec 003
delivery_orders, delivery_order_items, delivery_order_item_allocations, deliveries ← Spec 004
transfers, transfer_items, reception_records ← Spec 005
stock_takes, stock_take_items ← Spec 006
product_price_history ← Spec 007
invoices, payment_receipts, accounting_periods ← Spec 008
quarantine_records, damage_reports, adjustments (type in STOCK_TAKE, TRANSFER_DISCREPANCY, DISPOSAL, RTV...) ← Spec 009
audit_logs (append-only, action = *, entity_type = *, REPORT_VIEW for dashboards) ← All specs
```

**Constraint Summary:**
- Inventory: total_qty ≥ 0, reserved_qty ≥ 0, total_qty - reserved_qty ≥ 0 (CHECK + app validation)
- Warehouse: unique code, manager_id FK to user with MANAGER role
- Location: unique code per warehouse, parent_id for hierarchy (ZONE → BIN only)
- Product: unique SKU, no serial/expiry/grade for household goods
- DO/Transfer: credit check, FIFO allocation, optimistic locking, soft-cancel (status = CANCELLED)
- Stocktake/Adjustment: location locking, flat approval (Trưởng kho)
- Invoice: auto-create on delivery DELIVERED, unit_price snapshot from picking plan
- Audit: append-only, ghi all mutations + REPORT_VIEW for confidential reports
