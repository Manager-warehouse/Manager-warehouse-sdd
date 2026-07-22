# Tasks: Thiết lập & Hoàn thiện Hệ thống Test Giao diện (Frontend Testing System)

**Input**: Design documents from `.sdd/specs/012-frontend-testing/`
**Prerequisites**: `spec.md`, `plan.md`

---

## Phase 1: Setup & Testing Infrastructure

- [x] T001 Đọc kỹ tài liệu đặc tả [spec.md](./spec.md) và kế hoạch triển khai [plan.md](./plan.md).
- [x] T002 Cài đặt các thư viện kiểm thử `vitest`, `jsdom`, `@testing-library/react`, `@testing-library/jest-dom`, `@testing-library/user-event`, `@vitest/coverage-v8` trong [frontend/package.json](file:///d:/swp/Manager-warehouse-sdd/frontend/package.json).
- [x] T003 Cấu hình test runner trong [frontend/vite.config.js](file:///d:/swp/Manager-warehouse-sdd/frontend/vite.config.js) và setup mock environment tại [frontend/tests/setup.js](file:///d:/swp/Manager-warehouse-sdd/frontend/tests/setup.js).

---

## Phase 2: Foundational Utilities & Store Tests

- [x] T010 [P] Cập nhật bộ test validation [frontend/tests/admin/config.test.js](file:///d:/swp/Manager-warehouse-sdd/frontend/tests/admin/config.test.js).
- [x] T011 [P] Cập nhật bộ test auth & rbac store [frontend/tests/admin/rbac.test.js](file:///d:/swp/Manager-warehouse-sdd/frontend/tests/admin/rbac.test.js).
- [x] T012 [P] Tạo bộ test định dạng dữ liệu [frontend/tests/utils/format.test.js](file:///d:/swp/Manager-warehouse-sdd/frontend/tests/utils/format.test.js).
- [x] T013 [P] Tạo bộ test UI notification store [frontend/tests/stores/ui.test.js](file:///d:/swp/Manager-warehouse-sdd/frontend/tests/stores/ui.test.js).

---

## Phase 3: User Story 1 - Inbound & Stocktake Component Tests (Priority: P1)

- [x] T020 [P] [US1] Tạo bộ test component Nhập kho & QC [frontend/src/pages/Inbound/ReceiptList.test.jsx](file:///d:/swp/Manager-warehouse-sdd/frontend/src/pages/Inbound/ReceiptList.test.jsx) kiểm tra render bảng phiếu nhập, loading state và filter theo trạng thái.
- [x] T021 [P] [US1] Tạo bộ test component Kho cách ly [frontend/src/pages/Inbound/QuarantineWorkspace.test.jsx](file:///d:/swp/Manager-warehouse-sdd/frontend/src/pages/Inbound/QuarantineWorkspace.test.jsx) kiểm tra form xử lý hàng lỗi QC và nộp đơn tiêu hủy/RTV.
- [x] T022 [P] [US1] Tạo bộ test component Kiểm kê tồn kho [frontend/src/pages/Stocktake/StocktakeWorkspace.test.jsx](file:///d:/swp/Manager-warehouse-sdd/frontend/src/pages/Stocktake/StocktakeList.test.jsx) kiểm tra nhập số lượng thực tế, tính chênh lệch variance và disable double submit.

---

## Phase 4: User Story 2 - Outbound & Logistics Component Tests (Priority: P1)

- [x] T030 [P] [US2] Kiểm thử component Chuyến hàng tài xế [frontend/src/pages/Outbound/DriverTrip.test.jsx](file:///d:/swp/Manager-warehouse-sdd/frontend/src/pages/Outbound/DriverTrip.test.jsx).
- [x] T031 [P] [US2] Tạo bộ test component QC Xuất kho [frontend/src/pages/Outbound/QCOutbound.test.jsx](file:///d:/swp/Manager-warehouse-sdd/frontend/src/pages/Outbound/QCOutbound.test.jsx) kiểm tra xác nhận số lượng picking và phê duyệt DO.
- [x] T032 [P] [US2] Tạo bộ test component Quản lý Lệnh xuất kho [frontend/src/pages/Outbound/DeliveryOrders.test.jsx](file:///d:/swp/Manager-warehouse-sdd/frontend/src/pages/Outbound/DeliveryOrders.test.jsx) kiểm tra tạo mới phiếu DO và validate thông tin khách hàng/đại lý.

---

## Phase 5: User Story 3 - Inter-Warehouse Transfer & Master Data Component Tests (Priority: P1)

- [x] T040 [P] [US3] Kiểm thử validation chuyển kho [frontend/tests/inter-warehouse-transfer/validation.test.js](file:///d:/swp/Manager-warehouse-sdd/frontend/tests/inter-warehouse-transfer/validation.test.js).
- [x] T041 [P] [US3] Tạo bộ test component Phiếu chuyển kho nội bộ [frontend/src/pages/InterWarehouseTransfer/TransferRequestWorkspace.test.jsx](file:///d:/swp/Manager-warehouse-sdd/frontend/src/pages/InterWarehouseTransfer/TransferRequestWorkspace.test.jsx) kiểm tra chọn kho xuất/kho nhập và danh sách vật tư.
- [x] T042 [P] [US3] Tạo bộ test component Quản lý Sản phẩm [frontend/src/pages/Admin/ProductList.test.jsx](file:///d:/swp/Manager-warehouse-sdd/frontend/src/pages/Admin/ProductManagement.test.jsx) kiểm tra bảng sản phẩm và dialog thêm mới/chỉnh sửa sản phẩm.

---

## Phase 6: User Story 4 - Finance & Billing Component Tests (Priority: P1)

- [x] T050 [P] [US4] Tạo bộ test component Hóa đơn & Công nợ Đại lý [frontend/src/pages/Finance/DealerDebtInvoice.test.jsx](file:///d:/swp/Manager-warehouse-sdd/frontend/src/pages/Finance/DealerDebtInvoice.test.jsx) kiểm tra tính toán tổng nợ, hạn thanh toán và tạo mới invoice.
- [x] T051 [P] [US4] Tạo bộ test component Phiếu thu tiền [frontend/src/pages/Finance/PaymentReceipts.test.jsx](file:///d:/swp/Manager-warehouse-sdd/frontend/src/pages/Finance/PaymentReceipts.test.jsx) kiểm tra ghi nhận thanh toán và gạch nợ.

---

## Phase 6.5: User Story 5 - Core API Client & Full Service Coverage (Priority: P1)

- [x] T052 [P] Tạo bộ test Unit cho Axios Interceptor [frontend/test_frontend/services/apiClient.test.js](file:///d:/swp/Manager-warehouse-sdd/frontend/test_frontend/services/apiClient.test.js) kiểm tra gắn Bearer JWT token, xử lý refresh token 401, redirect và catch lỗi network.
- [x] T053 [P] Tạo bộ test API Service & Component cho các phân vùng còn thiếu: Pricing/COGS (`pricing.service.js`), Returns/Scrap (`returns.service.js`), Reports/Alerts (`report.service.js`), Master Data Suppliers/Dealers/Warehouses/Vehicles (`masterData.service.js`), và Accounting Period.

---

## Phase 7: Polish & CI/CD Pipeline Alignment

- [x] T060 Cập nhật workflow [.github/workflows/test.yml](file:///d:/swp/Manager-warehouse-sdd/.github/workflows/test.yml) để bỏ loại trừ `src/pages/**,src/components/**` trong bước quét SonarQube Frontend.
- [x] T061 Thẩm định toàn bộ test suite frontend bằng lệnh `npm run test` và `npm run test:coverage` đạt line/branch coverage >= 80% trên New Code.
