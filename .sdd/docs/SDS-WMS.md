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

## 4. Spec 004+005: Outbound, Delivery & Transfer Classes

### 4.1 Outbound/Delivery Core Services

**Service Layer:**
- `DeliveryOrderService.java` – CRUD DO, Credit Check, reserve + release, status transitions
- `PickingPlanService.java` – FIFO batch selection, allocation strategy, snapshot unit_price
- `OutboundQCService.java` – Record QC pass/fail, quarantine + adjustments for failures
- `TripDispatchService.java` – Create trips, assign vehicle/driver, capacity validation (weight/volume)
- `DriverDeliveryService.java` – Mobile API for driver: accept trip, depart, deliver, POD, OTP
- `DeliveryOtpService.java` – Generate OTP (6-digit, TTL 5min), hash storage, verification (max 3 fail)
- `AutoInvoiceService.java` – Event listener: WHEN delivery attempt = DELIVERED → create invoice + receivable

**Key SQL: DO + Credit Check + Reservation**

```sql
-- Credit Check before DO create
SELECT SUM(invoices.total_amount - COALESCE(payment_receipts.amount_paid, 0)) AS current_balance
FROM invoices
LEFT JOIN payment_receipts ON invoices.id = payment_receipts.invoice_id
WHERE invoices.dealer_id = ? AND invoices.status NOT IN ('CANCELLED','CLOSED')
GROUP BY invoices.dealer_id;
-- IF current_balance + DO_value > credit_limit → BLOCK with CREDIT_HOLD

-- Reserve warehouse-level on DO create
UPDATE warehouse_product_reservations
SET reserved_qty = reserved_qty + ?, version = version + 1
WHERE warehouse_id = ? AND product_id = ?
  AND version = ? -- Optimistic locking
RETURNING reserved_qty;

-- Create allocation on picking plan (FIFO)
INSERT INTO delivery_order_item_allocations 
  (do_item_id, inventory_id, batch_id, location_id, zone_id, planned_qty)
SELECT ?, i.id, i.batch_id, i.location_id, wl_zone.id, ?
FROM inventories i
INNER JOIN batches b ON i.batch_id = b.id
INNER JOIN warehouse_locations wl_zone ON wl_zone.id = wl.zone_id
WHERE i.warehouse_id = ? AND i.product_id = ? 
  AND i.total_qty - i.reserved_qty > 0
  AND wl.is_quarantine = false
  AND wl.type = 'BIN'
ORDER BY b.received_date ASC -- FIFO
LIMIT 1;
```

**SQL: Delivery & OTP**

```sql
-- Record OTP for delivery attempt (hash only, never plain)
INSERT INTO delivery_otp_attempts 
  (delivery_attempt_id, recipient_email, otp_hash, expires_at, status)
VALUES (?, ?, SHA256(?), NOW() + INTERVAL '5 minutes', 'PENDING');

-- Verify OTP (max 3 attempts)
SELECT otp_hash, expires_at, attempt_count
FROM delivery_otp_attempts
WHERE delivery_attempt_id = ? AND status = 'PENDING'
  AND attempt_count < 3;

-- Mark OTP used after successful verification
UPDATE delivery_otp_attempts
SET status = 'VERIFIED', consumed_at = NOW()
WHERE delivery_attempt_id = ? AND otp_hash = ?;

-- Auto-create invoice on delivery DELIVERED
INSERT INTO invoices (dealer_id, total_amount, issue_date, due_date, status)
SELECT do.dealer_id, 
       SUM(doi.qc_pass_qty * doi.unit_price),
       DATE(NOW()),
       DATE(NOW()) + INTERVAL '30 days',
       'UNPAID'
FROM delivery_attempts da
INNER JOIN delivery_orders do ON da.do_id = do.id
INNER JOIN delivery_order_items doi ON do.id = doi.do_id
WHERE da.status = 'DELIVERED' AND da.id = ?
GROUP BY do.dealer_id;
```

---

### 4.2 Transfer Core Services

**Service Layer:**
- `InterWarehouseTransferService.java` – CRUD transfer, reserve FIFO, status transitions
- `TransferShipmentService.java` – Outbound: pick + QC + handover
- `TransferReceiveService.java` – Inbound: blind count + receive QC + bin-capacity + discrepancy
- `TransferLocationLockService.java` – Lock/unlock during stocktake
- `TransferDiscrepancyService.java` – Calculate discrepancy, create adjustment records

**Key SQL: Transfer + Discrepancy**

```sql
-- Reserve FIFO for transfer on approval (source warehouse)
UPDATE warehouse_product_reservations
SET reserved_qty = reserved_qty + ?, version = version + 1
WHERE warehouse_id = ? AND product_id = ?;

-- Move qty to In-Transit on departure
UPDATE inventories
SET total_qty = total_qty - ?
WHERE warehouse_id = ? AND product_id = ? AND version = ?;

INSERT INTO inventories (warehouse_id, product_id, batch_id, location_id, total_qty)
SELECT ?, ?, batch_id, in_transit_location_id, ?
FROM inventories
WHERE warehouse_id = ? AND product_id = ?;

-- Calculate discrepancy on dest receive
SELECT 
  trf_items.planned_qty,
  reception_records.received_qty,
  (reception_records.received_qty - trf_items.planned_qty) AS variance_qty
FROM transfer_items trf_items
INNER JOIN reception_records ON trf_items.id = reception_records.transfer_item_id
WHERE trf_items.transfer_id = ?;

-- Create TRANSFER_DISCREPANCY adjustment if variance ≠ 0
INSERT INTO adjustments 
  (transfer_id, product_id, type, quantity_adjustment, variance_value)
VALUES (?, ?, 'TRANSFER_DISCREPANCY', ?, ? * cost_price);
```

---

## 5. Spec 006+007: Stocktake & Pricing Classes

**Services:** `StocktakeService`, `PricingService` (price_history lookup for COGS on invoice create)

**SQL: Stocktake + Location Lock**

```sql
-- Lock locations during IN_PROGRESS
UPDATE warehouse_locations
SET is_locked = true WHERE warehouse_id = ? AND type = 'BIN' AND is_quarantine = false;

-- Update inventory on approval + optimistic locking
UPDATE inventories SET total_qty = ?, version = version + 1
WHERE id = ? AND version = ?;

-- Price history COGS lookup (at issue_date)
SELECT cost_price FROM product_price_history
WHERE product_id = ? AND effective_date <= ? AND (end_date IS NULL OR end_date > ?)
ORDER BY effective_date DESC LIMIT 1;
```

---

## 6. Spec 008+009+010: Finance, Returns & Reporting Classes

**Services:**
- `InvoiceService` (auto-create on delivery DELIVERED)
- `AccountingPeriodService` (close, lock, late-doc handling)
- `ReturnService` (RTV Debit Note for supplier; Credit Note for customer)
- `ReportService` (Dashboard KPI, Aging, P&L)

**SQL: Auto-Invoice & Accounting Period**

```sql
-- Auto-create invoice trigger (after delivery DELIVERED)
INSERT INTO invoices (dealer_id, total_amount, issue_date, due_date, status, document_reference)
SELECT do.dealer_id, SUM(doi.qc_pass_qty * doi.unit_price), 
       CURRENT_DATE, CURRENT_DATE + INTERVAL '30 days', 'UNPAID', 'DO-' || do.do_number
FROM deliveries d
INNER JOIN delivery_orders do ON d.do_id = do.id
INNER JOIN delivery_order_items doi ON do.id = doi.do_id
WHERE d.status = 'DELIVERED' AND d.id = ?
GROUP BY do.dealer_id;

-- Monthly closing lock
UPDATE accounting_periods SET status = 'CLOSED' WHERE id = ?;
-- Block new transactions in closed period; allow Correction Vouchers in open period

-- Aging Report
SELECT dealer_id, due_date,
       CASE WHEN due_date < CURRENT_DATE AND CURRENT_DATE - due_date > 60 THEN '>60 days'
            WHEN due_date < CURRENT_DATE AND CURRENT_DATE - due_date > 30 THEN '31-60 days'
            WHEN due_date < CURRENT_DATE THEN '1-30 days'
            ELSE 'Not due' END AS aging_bucket,
       SUM(total_amount - COALESCE(paid_amount, 0)) AS balance
FROM invoices
WHERE status != 'CLOSED'
GROUP BY dealer_id, aging_bucket;
```

---


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
