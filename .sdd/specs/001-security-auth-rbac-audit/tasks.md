# Tasks: Phân Quyền & Cách Ly Kho (User Warehouse Assignment & Isolation)

**Input**: Design documents from `.sdd/specs/001-security-auth-rbac-audit/`

**Prerequisites**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `quickstart.md`

**Tests**: Yêu cầu viết unit test cho các service backend và store frontend bị ảnh hưởng.

**Organization**: Các nhiệm vụ được nhóm theo từng User Story để có thể triển khai và kiểm thử độc lập.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Có thể chạy song song (parallel) do không phụ thuộc dữ liệu hoặc chạm vào cùng một file.
- **[Story]**: Nhãn User Story tương ứng như US1, US2, US3.
- Đường dẫn file chính xác và rõ ràng.

## Path Conventions

- Backend code: `backend/src/main/java/com/wms/`
- Backend tests: `backend/src/test/java/com/wms/`
- Frontend code: `frontend/src/`
- Frontend tests: `frontend/tests/`
- Feature docs: `.sdd/specs/001-security-auth-rbac-audit/`

---

## Phase 1: Setup & Design Alignment

**Purpose**: Đảm bảo kế hoạch và môi trường sẵn sàng, không vi phạm hiến pháp WMS.

- [x] T001 Read `.specify/memory/constitution.md`, `AGENTS.md`, and the feature `spec.md`.
- [x] T002 Verify `plan.md` lists affected entities, validation, and test strategy.
- [x] T003 [P] Verify the current `UserServiceImpl.java` and `WarehouseServiceImpl.java` structure.


---

## Phase 2: Foundational Backend

**Purpose**: Đăng ký các mã lỗi chung cho Validation.

- [x] T004 Add exception message mappings/keys (`WAREHOUSE_REQUIRED`, `MULTIPLE_WAREHOUSES_NOT_ALLOWED`, `WAREHOUSE_SCOPE_FORBIDDEN`) to central error definitions in `backend/src/main/java/com/wms/exception/GlobalExceptionHandler.java`.

**Checkpoint**: Mã nguồn biên dịch thành công.

---

## Phase 3: User Story 1 - Ràng buộc gán kho phía Backend & Login Payload (Priority: P1)

**Goal**: Thực hiện kiểm tra validate danh sách kho truyền lên khi tạo/cập nhật user và bypass gán kho cho ADMIN/CEO.

**Independent Test**: Chạy unit test `UserServiceImplTest` để kiểm định logic validation và lưu trữ.

### Tests for User Story 1

- [x] T005 [P] [US1] Create/update unit tests in `backend/src/test/java/com/wms/service/impl/UserServiceImplTest.java` to verify validation constraints: ném lỗi khi user thường có 0 hoặc >1 kho, và thành công khi ADMIN/CEO gán 0 kho.

### Implementation for User Story 1

- [x] T006 [US1] Implement validation constraints inside `createUser` and `updateUser` methods of `backend/src/main/java/com/wms/service/impl/UserServiceImpl.java`:
  - Nếu role là `ADMIN` hoặc `CEO`, tự động bỏ qua gán kho.
  - Nếu role khác, kiểm tra `request.getWarehouses()`. Nếu rỗng hoặc có size > 1, ném `IllegalArgumentException`.
- [x] T007 [US1] Implement `buildWarehouseInfoList` and `buildMeWarehouseInfoList` in `backend/src/main/java/com/wms/service/AuthService.java` to load all active warehouses for ADMIN/CEO and only assigned warehouse for other roles.

**Checkpoint**: US1 hoàn thành độc lập và tất cả các test của US1 vượt qua.

---

## Phase 4: User Story 2 - Lọc danh sách kho và Phân quyền Trưởng kho (Priority: P1)

**Goal**: Bộ lọc API `/api/v1/admin/warehouses` trả về danh sách kho phù hợp theo vai trò, và chặn Trưởng kho xem báo cáo năng suất của kho khác.

**Independent Test**: Chạy tích hợp hoặc unit test của `WarehouseControllerTest` và `ReportServiceImplTest`.

### Tests for User Story 2

- [x] T008 [P] [US2] Add unit tests in `backend/src/test/java/com/wms/service/impl/ReportServiceImplTest.java` to verify that `getProductivityReport` throws `IllegalArgumentException` when a WAREHOUSE_MANAGER tries to access a warehouse different from their assigned one.
- [x] T009 [P] [US2] Update/create unit tests for `WarehouseServiceImpl.java` to verify that `getAllWarehouses` filters out other warehouses for restricted users, but returns all for ADMIN/CEO/WAREHOUSE_MANAGER.

### Implementation for User Story 2

- [x] T010 [US2] Modify `getAllWarehouses` interface in `backend/src/main/java/com/wms/service/WarehouseService.java` and implementation in `WarehouseServiceImpl.java` to accept `Long userId` and filter the returned list according to user roles.
- [x] T011 [US2] Update `getAllWarehouses` endpoint in `backend/src/main/java/com/wms/controller/WarehouseController.java` to retrieve the current logged-in user's ID and pass it to the service method.
- [x] T012 [US2] Update `getProductivityReport` and `exportProductivityReportExcel` in `backend/src/main/java/com/wms/service/impl/ReportServiceImpl.java` to enforce that a `WAREHOUSE_MANAGER` can only request productivity details for their assigned warehouse.

**Checkpoint**: US2 hoạt động và vượt qua tất cả kiểm thử liên quan.

---

## Phase 5: User Story 3 - Global Warehouse Selector & Lock Logic Frontend (Priority: P1)

**Goal**: Hiển thị dropdown bộ chọn kho ở Header dựa trên role, và reset kho làm việc của Trưởng kho khi rời khỏi Dashboard.

**Independent Test**: Chạy `npm run test` để kiểm thử logic store.

### Tests for User Story 3

- [x] T013 [P] [US3] Update test cases in `frontend/tests/admin/rbac.test.js` to verify `hasWarehouseAccess` method in `auth.store.js` behaves correctly for ADMIN, CEO, and restricted roles.

### Implementation for User Story 3

- [x] T014 [US3] Update `hasWarehouseAccess` implementation in `frontend/src/stores/auth.store.js` to return `true` for ADMIN/CEO, and match user's assigned warehouses for other roles.
- [x] T015 [US3] Update `frontend/src/components/layout/Header.jsx` to render the Global Warehouse Selector:
  - Nếu user là `ADMIN` hoặc `CEO`, hiển thị bộ chọn cho tất cả các kho trên mọi trang.
  - Nếu user là `WAREHOUSE_MANAGER`, chỉ cho phép thay đổi kho khi đang ở trang `/dashboard` (hoặc `/reports/low-stock`, v.v. nếu cần xem tồn kho). Trên các trang khác, vô hiệu hóa dropdown hoặc khóa về kho được gán.
  - Nếu user là các vai trò khác, ẩn chevron và khóa dropdown về kho được gán duy nhất.
- [x] T016 [US3] Implement auto-reset in `frontend/src/routes/ProtectedRoute.jsx` (hoặc một wrapper hook): Khi route thay đổi sang một trang tác vụ (không phải `/dashboard` hay trang xem tồn kho), nếu activeWarehouse khác với kho gán duy nhất của `WAREHOUSE_MANAGER`, tự động gọi `setActiveWarehouse` reset về kho được gán ban đầu.

**Checkpoint**: Giao diện chọn kho hoạt động chính xác theo vai trò, tự động khóa/mở khóa và reset đúng quy tắc.

---

## Phase 6: Cross-Cutting Verification

- [x] T900 Run `mvn clean test` from `backend/` to verify all backend tests pass.
- [x] T901 Run `npm run test` from `frontend/` to verify all frontend tests pass.
- [x] T902 Verify no hardcoded secrets, debug logging (`System.out` or `console.log`), or TODO comments remain.
- [x] T903 Manually verify all user roles (Admin, CEO, Manager, Storekeeper) behave correctly according to the 3 quickstart scenarios.

## Dependency Rules

- Phase 3 (US1) và Phase 4 (US2) có thể làm song song nhưng phải hoàn thành trước khi tích hợp Frontend.
- T014 (auth.store.js) phải hoàn tất trước khi chỉnh sửa T015 (Header.jsx) và T016 (ProtectedRoute.jsx).
- Việc chạy toàn bộ kiểm thử xác thực (Phase 6) chỉ được thực hiện sau khi hoàn tất cả backend và frontend.
