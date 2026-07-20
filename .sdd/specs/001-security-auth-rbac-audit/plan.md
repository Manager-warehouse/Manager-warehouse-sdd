# Implementation Plan: Phân Quyền & Cách Ly Kho (User Warehouse Assignment & Isolation)

**Branch**: `feat/warehouse-isolation` | **Date**: 2026-07-19 | **Spec**: [.sdd/specs/001-security-auth-rbac-audit/spec.md](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/001-security-auth-rbac-audit/spec.md)

## Summary

Đặc tả các quy tắc và triển khai nghiệp vụ cách ly dữ liệu giữa 3 kho vật lý (Hải Phòng, Hà Nội, Hồ Chí Minh) cho từng vai trò người dùng (Role):
1. **ADMIN & CEO**: Toàn quyền xem và thao tác trên mọi kho. Không lưu thông tin phân bổ kho trong bảng `user_warehouse_assignments` (bypass kiểm tra ở mức code).
2. **WAREHOUSE_MANAGER**: Chỉ được gán vào chính xác 1 kho hoạt động. Được xem Dashboard tổng quan và tồn kho khả dụng (Available Qty) của các kho khác (Read-only) để lên kế hoạch và yêu cầu điều chuyển; chặn xem chi tiết nội bộ và chặn mọi thao tác ghi dữ liệu tại kho khác.
3. **Các vai trò còn lại (Storekeeper, Warehouse Staff, Accountant, Dispatcher...)**: Chỉ được gán vào chính xác 1 kho hoạt động. Locked hoàn toàn dữ liệu (cả Đọc và Ghi) theo kho được gán.
4. **Backend Validation**: Enforce ràng buộc số lượng kho được gán khi tạo/cập nhật tài khoản ở tầng Service.
5. **Frontend UX**: Tích hợp Global Warehouse Selector ở Header. Tự động lock/unlock và reset kho hoạt động dựa trên vai trò hiện tại của user và trang đang xem.

## Technical Context

**Language/Version**: Java 21 / Spring Boot 3.4.5; React 18 + JavaScript

**Primary Dependencies**: Spring Web, Spring Data JPA, Hibernate, Spring Security, Lombok, Springdoc OpenAPI, React, Tailwind CSS

**Storage**: PostgreSQL 18 via Flyway migrations and Spring Data JPA

**Testing**: JUnit 5 + Mockito for backend; Jest + React Testing Library for frontend

**Target Platform**: Full-stack WMS web application and REST API

**Constraints**: Bắt buộc tuân thủ Hiến pháp WMS: chặn tồn kho âm, bắt buộc ghi Audit Log, kiểm soát quyền truy cập theo vai trò + chi nhánh kho.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] Layered architecture preserved: Controller -> Service -> Repository -> Entity.
- [x] Write endpoints use request DTOs with Jakarta Validation.
- [x] Service methods own business rules, transactions, authorization, and audit logging.
- [x] All DB access goes through Spring Data JPA/Hibernate; no raw SQL in application code.
- [x] QC/quarantine/transfer/accounting state rules listed when touched.
- [x] Audit action, entity type, before/after payload, and warehouse scope identified.
- [x] OpenAPI/Swagger impact identified for every new or changed endpoint.
- [x] Flyway migration impact identified; no duplicate migration version in runnable history.
- [x] Unit and integration test strategy covers happy path and error paths.

## Domain Impact

**Actors/Roles**:
* **ADMIN / CEO**: Có quyền chuyển đổi và thao tác trên tất cả các kho.
* **WAREHOUSE_MANAGER**: Mặc định làm việc tại kho được gán. Tại `/dashboard` hoặc `/inventory-availability` (nếu có), có thể đổi bộ lọc kho sang kho khác để xem tồn kho khả dụng phục vụ xin điều chuyển.
* **Các vai trò khác**: Bị khóa cứng dữ liệu theo kho duy nhất được gán.

**State Changes**:
* Gán kho cho người dùng: `ASSIGN` / `UNASSIGN` audit logs.

**Inventory Impact**:
* Bất kỳ đột biến tồn kho nào (nhập, xuất, điều chuyển, kiểm kê) phải validate xem người thực hiện có quyền ghi tại kho đó hay không.

**Audit Actions**:
* Ghi nhận audit log khi thực hiện gán kho cho tài khoản thông qua `/api/v1/admin/users`.

**Security/Authorization**:
* Xác thực API check role và warehouse scope. ADMIN và CEO bypass hoàn toàn kiểm tra warehouse.

## Data Model / Migration Impact

* Không thay đổi schema database. Bảng `user_warehouse_assignments` và thực thể `UserWarehouseAssignment` được giữ nguyên.
* Mọi tài khoản không phải ADMIN/CEO bắt buộc phải có đúng 1 bản ghi gán kho trong bảng `user_warehouse_assignments`.
* Tài khoản ADMIN/CEO không có bản ghi nào trong bảng này.

## API / Contract Impact

* **`POST /api/v1/admin/users` & `PUT /api/v1/admin/users/{id}`**:
  * Khi tạo/cập nhật user có vai trò không phải ADMIN/CEO, kiểm tra danh sách `warehouses` trong `UserRequest`. Nếu danh sách rỗng hoặc có nhiều hơn 1 phần tử, trả về lỗi `400 Bad Request` kèm mã lỗi tương ứng (`WAREHOUSE_REQUIRED` hoặc `MULTIPLE_WAREHOUSES_NOT_ALLOWED`).
  * Nếu vai trò là ADMIN/CEO, hệ thống tự động loại bỏ (hoặc bỏ qua) các kho truyền lên và không lưu vào bảng gán kho.
* **`GET /api/v1/admin/warehouses`**:
  * Thêm tham số lọc danh sách kho dựa trên tài khoản đăng nhập.
  * ADMIN/CEO/WAREHOUSE_MANAGER: Trả về toàn bộ danh sách kho hoạt động.
  * Các vai trò còn lại: Trả về danh sách chỉ chứa duy nhất kho được gán của họ.
* **`GET /api/v1/reports/productivity`**:
  * Phải kiểm tra: Nếu user là WAREHOUSE_MANAGER, họ chỉ được xem báo cáo năng suất của kho họ được gán. Nếu truyền `warehouseId` của kho khác lên, trả về lỗi `400 Bad Request` / `WAREHOUSE_SCOPE_FORBIDDEN`.

## Test Strategy

### Backend Tests
* **`UserServiceImplTest.java`**:
  * Test tạo/cập nhật user vai trò giới hạn gán 0 kho -> Ném lỗi `IllegalArgumentException("WAREHOUSE_REQUIRED")`.
  * Test tạo/cập nhật user vai trò giới hạn gán từ 2 kho trở lên -> Ném lỗi `IllegalArgumentException("MULTIPLE_WAREHOUSES_NOT_ALLOWED")`.
  * Test tạo/cập nhật ADMIN/CEO -> Bảng gán kho trống, không lưu bản ghi nào.
* **`WarehouseServiceImplTest.java`**:
  * Test `getAllWarehouses` với ADMIN/CEO/WAREHOUSE_MANAGER -> Trả về tất cả các kho.
  * Test `getAllWarehouses` với STOREKEEPER -> Chỉ trả về kho được gán.
* **`ReportServiceImplTest.java`**:
  * Test `getProductivityReport` với WAREHOUSE_MANAGER truy cập kho khác -> Ném lỗi `IllegalArgumentException`.

### Frontend Tests
* Cập nhật test trong `frontend/tests/admin/rbac.test.js` để kiểm tra hàm `hasWarehouseAccess` trong auth store:
  * ADMIN/CEO -> Trả về `true` cho mọi kho.
  * Các role khác -> Chỉ trả về `true` cho kho nằm trong `user.warehouses`.
* Test component `Header.jsx` hiển thị bộ chọn kho (Warehouse Selector):
  * Trạng thái hiển thị dropdown đối với ADMIN/CEO trên mọi trang.
  * Trạng thái hiển thị dropdown đối với WAREHOUSE_MANAGER chỉ trên Dashboard.
  * Trạng thái ẩn/khóa đối với các vai trò khác.

## Project Structure

### Documentation

```text
.sdd/specs/001-security-auth-rbac-audit/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── tasks.md
```

### Source Code Touched

#### Backend
* `com.wms.service.impl.UserServiceImpl`
* `com.wms.service.impl.WarehouseServiceImpl`
* `com.wms.service.impl.ReportServiceImpl`
* `com.wms.controller.WarehouseController`
* `com.wms.service.AuthService`

#### Frontend
* `frontend/src/stores/auth.store.js`
* `frontend/src/components/layout/Header.jsx`
* `frontend/src/routes/ProtectedRoute.jsx`
