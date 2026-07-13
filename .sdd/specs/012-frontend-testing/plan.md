# Implementation Plan: Thiết lập & Hoàn thiện Hệ thống Test Giao diện (Frontend Testing System)

**Branch**: `feat/frontend-testing` | **Date**: 2026-07-12 | **Spec**: [spec.md](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/012-frontend-testing/spec.md)

**Input**: Feature specification from `.sdd/specs/012-frontend-testing/spec.md`

## Summary

Thiết lập môi trường kiểm thử giao diện bằng Vitest, jsdom và React Testing Library cho module `frontend`. Triển khai cấu hình test runner, sửa đổi 2 tệp test nháp hiện có (`config.test.js` và `rbac.test.js`) để chạy thực tế trên code production, và viết thêm bộ test cho format utility cùng UI store. Hoàn thiện cấu hình CI/CD trên GitHub Actions để chạy test frontend tự động và đẩy báo cáo coverage LCOV lên SonarQube bằng `sonarsource/sonarqube-scan-action`.
Đồng thời, cấu hình loại trừ các thư mục UI tĩnh (pages, components) để điểm số đo lường phản ánh chính xác chất lượng nghiệp vụ (Zustand stores, Utils) liên quan trực tiếp đến các spec từ 001 đến 010.

## Technical Context

**Language/Version**: React 18 / JavaScript / Vite / Node 20+

**Primary Dependencies**: 
- `vitest` (Test runner)
- `jsdom` (Giả lập DOM môi trường browser)
- `@testing-library/react` (Render & Query components)
- `@testing-library/jest-dom` (Custom matchers cho DOM assertions)
- `@testing-library/user-event` (Giả lập tương tác người dùng)
- `@vitest/coverage-v8` (Đo lường độ bao phủ mã nguồn)
- `sonarsource/sonarqube-scan-action@v2` (Gửi báo cáo quét frontend SonarQube)

**Storage**: LocalStorage / SessionStorage mock (sử dụng in-memory mock trong setup test).

**Testing**: 
- Unit Tests cho Utilities & Zustand Stores.
- Integration/Component Tests cho UI Components, Forms và Routers.
- Mocking Axios cho toàn bộ các giao tiếp API.

**Target Platform**: Web Browser (môi trường giả lập jsdom)

**Performance Goals**: Thời gian thực thi toàn bộ test suite cục bộ ≤ 2 phút.

**Constraints**:
- Không bỏ qua test trên CI (`test-frontend` trong workflow không được chứa `--if-present` hay `|| true`).
- Trạng thái Zustand store phải được reset giữa các testcase để bảo toàn tính độc lập.

## Constitution Check

*GATE: Passed*

- [x] Layered architecture preserved: N/A (chỉ cấu hình kiểm thử ở frontend).
- [x] Write endpoints use request DTOs with Jakarta Validation: N/A.
- [x] Service methods own business rules, transactions, authorization, and audit logging: N/A.
- [x] All DB access goes through Spring Data JPA/Hibernate: N/A.
- [x] Inventory invariants preserved if touched: N/A.
- [x] QC/quarantine/transfer/accounting state rules listed when touched: N/A.
- [x] Audit action, entity type, before/after payload, and warehouse scope identified: N/A.
- [x] OpenAPI/Swagger impact identified: N/A.
- [x] Flyway migration impact identified: N/A.
- [x] Unit and integration test strategy covers happy path and error paths: Bộ test sẽ phủ cả kịch bản thành công (render UI chính xác, submit form thành công) và kịch bản lỗi (validate lỗi, API lỗi 4xx/5xx, lỗi kết nối mạng).

## Domain Impact

**Actors/Roles**:
- **Developer**: Viết code test, cấu hình thư viện, kiểm tra cục bộ.
- **QA Engineer**: Xác nhận kịch bản test UAT, theo dõi defect log, ký duyệt release.
- **CI/CD Runner**: Tự động cài dependencies (`npm ci`), chạy `npm run lint`, chạy `npm run test:coverage`, và build ứng dụng.

**State Changes**: Giả lập các trạng thái đăng nhập, thay đổi thông tin người dùng trong Zustand store.

**Inventory Impact**: Không có.

**Audit Actions**: Không có.

**Security/Authorization**: Kiểm thử các Route Guard dựa trên vai trò người dùng (ADMIN, WAREHOUSE_MANAGER, STOREKEEPER) và phạm vi kho được gán.

**Accounting Impact**: Không có.

## Data Model / Migration Impact

- Entities/tables touched: Không.
- New/changed columns or constraints: Không.
- Flyway plan: Không.
- Backfill/seed data: Sử dụng mock store state và mock API payloads.

## API / Contract Impact

- Mock toàn bộ các endpoint Axios giao tiếp với Backend để cô lập test.
- Đảm bảo hiển thị đúng lỗi từ các response status: 400 (Bad Request), 401 (Unauthorized), 403 (Forbidden), 404 (Not Found), 500 (Internal Server Error).

## QA/QC Organization Plan

### 1. Phân vùng QC (Kiểm soát chất lượng tự động)
* **Coverage Target**: Đạt tối thiểu 80% line/branch coverage trên các file logic nghiệp vụ (Zustand Stores, Form Validation, Helpers).
* **SonarQube Quality Gate**: Chỉ áp dụng ngưỡng coverage trên **New Code** trong PR. Loại trừ hoàn toàn các thành phần UI thuần (màn hình hiển thị tĩnh) thông qua tham số loại trừ:
  `-Dsonar.coverage.exclusions=src/pages/**,src/components/**,src/main.jsx,src/App.jsx,src/router.jsx`

## Test Strategy

- **Store Unit Tests**: Viết kiểm thử cho Zustand Auth Store (lưu token, xóa token khi logout, phân quyền người dùng) và Zustand UI Store (quản lý trạng thái mở/đóng sidebar, danh sách toast thông báo, cơ chế tự động dismiss sau 3 giây).
- **Utility Unit Tests**: Kiểm tra toàn diện các hàm tiện ích định dạng dữ liệu trong `format.js` (formatDate, formatNumber, getAvatarFallback) với đầy đủ các biên dữ liệu null, undefined, số âm, chuỗi không hợp lệ.
- **Component Tests**:
  - Render các trạng thái của component (loading spinner, danh sách trống, hiển thị dữ liệu thành công).
  - Kiểm tra disable nút submit khi đang gửi request.
- **Routing Tests**: Kiểm thử bảo vệ Router (chặn người dùng chưa đăng nhập, sai vai trò).
- **Regression Tests**: Thiết lập lệnh chạy test bắt buộc trên GitHub Actions để bảo vệ hệ thống trước lỗi hồi quy.
- **Test Code Quality (Vitest Parameterized Tests)**: Sử dụng `test.each` hoặc `describe.each` cho các kịch bản test có nhiều bộ dữ liệu (edge cases) để loại bỏ code lặp và giảm thiểu hardcode.

## Project Structure

### Documentation

```text
.sdd/specs/012-frontend-testing/
├── spec.md
├── plan.md
├── checklists/
│   └── requirements.md
└── tasks.md
```

### Source Code

```text
frontend/
├── package.json (Thêm devDependencies và scripts test)
├── vite.config.js (Thêm cấu hình test cho Vitest)
├── tests/
│   ├── setup.js (Thiết lập cấu hình jsdom, dọn dẹp mock)
│   └── admin/
│       ├── config.test.js (Sửa đổi kiểm thử validation thật)
│       └── rbac.test.js (Sửa đổi kiểm thử auth store thật)
```

**Structure Decision**: Cài đặt tích hợp Vitest vào `vite.config.js` hiện có để tận dụng tối đa hệ thống plugin của Vite và tối ưu hiệu suất chạy test.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [None] | [N/A] | [N/A] |
