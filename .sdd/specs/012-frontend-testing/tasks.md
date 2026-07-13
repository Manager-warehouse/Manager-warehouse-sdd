# Tasks: Thiết lập & Hoàn thiện Hệ thống Test Giao diện (Frontend Testing System)

**Input**: Design documents from `.sdd/specs/012-frontend-testing/`

**Prerequisites**: `spec.md`, `plan.md`

**Tests**: Yêu cầu cài đặt thành công Vitest, chạy 2 file test giao diện (config, rbac) trên code thật, sinh báo cáo coverage và hoàn thiện CI/CD pipeline.

---

## Phase 1: Setup & Design Alignment

- [x] T001 Đọc kỹ tài liệu đặc tả [spec.md](./spec.md) và kế hoạch triển khai [plan.md](./plan.md).
- [x] T002 Xác nhận trạng thái git (`git status`) để đảm bảo không ghi đè bất kỳ thay đổi chưa commit nào.
- [x] T003 Kiểm tra sự hiện diện của thư mục [frontend/tests/admin/](file:///d:/swp/Manager-warehouse-sdd/frontend/tests/admin/) và 2 file test nháp.

---

## Phase 2: Setup Frontend Testing Infrastructure

**Goal**: Cài đặt các dependencies và cấu hình môi trường chạy test Vitest + jsdom ở frontend.

- [x] T010 Cài đặt các thư viện kiểm thử `vitest`, `jsdom`, `@testing-library/react`, `@testing-library/jest-dom`, `@testing-library/user-event`, `@vitest/coverage-v8` vào `devDependencies` của [frontend/package.json](file:///d:/swp/Manager-warehouse-sdd/frontend/package.json), đồng thời định nghĩa các test scripts.
- [x] T011 Cấu hình phần `test` cho Vitest trong [frontend/vite.config.js](file:///d:/swp/Manager-warehouse-sdd/frontend/vite.config.js).
- [x] T012 Tạo tệp [frontend/tests/setup.js](file:///d:/swp/Manager-warehouse-sdd/frontend/tests/setup.js) để giả lập môi trường browser (localStorage, jsdom cleanup) và thiết lập custom matchers.

**Checkpoint**: Chạy thử lệnh `npx vitest --run` phát hiện được các file test mà không gặp lỗi cấu hình môi trường.

---

## Phase 3: Sửa đổi và Hoàn thiện Test cases

**Goal**: Chuyển đổi 2 file test nháp thành các bài test thực tế chạy trên code production và stores.

- [x] T020 Sửa đổi [frontend/tests/admin/config.test.js](file:///d:/swp/Manager-warehouse-sdd/frontend/tests/admin/config.test.js) để import các hàm kiểm tra validation thực tế từ source code và viết các trường hợp test dữ liệu hợp lệ/lỗi biên.
- [x] T021 Sửa đổi [frontend/tests/admin/rbac.test.js](file:///d:/swp/Manager-warehouse-sdd/frontend/tests/admin/rbac.test.js) để kiểm tra hoạt động thực tế của auth store (Zustand) và phân quyền kho bãi (Warehouse Scope).
- [x] T023 Tạo mới bộ test [frontend/tests/utils/format.test.js](file:///d:/swp/Manager-warehouse-sdd/frontend/tests/utils/format.test.js) kiểm thử toàn diện các hàm format ngày tháng, số lượng.
- [x] T024 Tạo mới bộ test [frontend/tests/stores/ui.test.js](file:///d:/swp/Manager-warehouse-sdd/frontend/tests/stores/ui.test.js) kiểm thử toàn bộ logic đóng mở sidebar, auto-dismiss toast.
- [x] T022 Chạy kiểm thử cục bộ `npm run test` và `npm run test:coverage` để đảm bảo 100% test cases vượt qua và sinh báo cáo LCOV/HTML.

**Checkpoint**: Toàn bộ test frontend vượt qua, có báo cáo coverage được tạo thành công tại `frontend/coverage/lcov.info`.

---

## Phase 4: CI/CD Integration

**Goal**: Cấu hình pipeline để chạy test frontend bắt buộc trên môi trường GitHub Actions CI.

- [x] T030 Cập nhật tệp workflow [.github/workflows/test.yml](file:///d:/swp/Manager-warehouse-sdd/.github/workflows/test.yml) để thay thế `npm test --if-present` bằng lệnh chạy test frontend bắt buộc, đồng thời tích hợp `sonarsource/sonarqube-scan-action` đẩy LCOV lên SonarQube.

**Checkpoint**: Pipeline CI chạy thành công cả frontend/backend test mà không bị skip.

---

## Phase 6: Cấu hình SonarQube Exclusions & Áp dụng Parameterized Tests

**Goal**: Loại bỏ UI pages/components khỏi coverage và chuẩn hóa việc viết test cho utilities/stores.

- [x] T040 Thêm cấu hình `-Dsonar.coverage.exclusions` vào tệp workflow [.github/workflows/test.yml](file:///d:/swp/Manager-warehouse-sdd/.github/workflows/test.yml) ở bước quét SonarQube Frontend để loại bỏ các thư mục UI: `src/pages/**,src/components/**,src/main.jsx,src/App.jsx,src/router.jsx`.
- [x] T041 Cải tiến các test case của frontend sử dụng Vitest Parameterized Tests (`test.each`) đối với các utilities và stores xử lý nhiều kịch bản đầu vào.

---

## Phase 5: Cross-Cutting Verification

- [x] T900 Chạy lệnh `npm run lint` ở frontend.
- [x] T901 Chạy lệnh `npm run test` ở frontend.
- [x] T902 Chạy lệnh `npm run test:coverage` ở frontend.
- [x] T903 Chạy lệnh `npm run build` ở frontend.
- [x] T904 Xác nhận không lưu credentials hoặc TODO comments trong các file test.
