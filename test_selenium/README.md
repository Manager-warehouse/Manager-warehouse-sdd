# Test Selenium Automation Framework (WMS System Test - Round 2)

## 1. Overview
Thư mục `test_selenium` chứa bộ kịch bản kiểm thử tự động End-to-End (E2E System Test) sử dụng **Selenium WebDriver** cho hệ thống **Warehouse Management System (WMS)**.

## 2. Mục đích
- Thực thi **System Test (Round 2)** tự động trên môi trường Staging/Local.
- Tự động mở trình duyệt Chrome, mô phỏng thao tác người dùng thật (Đăng nhập, Quản lý danh mục, Nhập kho, Xuất kho, Điều chuyển, Kiểm kê, Báo cáo).
- Tự động xuất báo cáo kết quả (Passed/Failed) để điền vào file `docs/test/test_final.xlsx` (Cột Round 2).

## 3. Cấu trúc Thư mục
```text
test_selenium/
├── config/              # config.py: APP_URL/API_URL, headless flag, role credentials (env-overridable)
├── pages/               # Page Object Model (POM)
│   ├── base_page.py     # BasePage: find/click/type/is_visible helpers
│   └── wms_pages.py      # LoginPage + generic ModulePage.check_page_loaded()
├── run_selenium_round2.py  # Runner: logs in per module's required role, verifies
│                           # the module landing page isn't bounced to /login or /forbidden
├── utils/               # excel_reporter.py (writes test_final.xlsx/result_test.md),
│                         # error_tracer.py (selenium_error_report.md/.xlsx)
└── requirements.txt     # selenium, webdriver-manager, pytest, openpyxl, requests
```

## 4. Hướng dẫn chạy Test
```bash
pip install -r test_selenium/requirements.txt
python test_selenium/run_selenium_round2.py
```

Mặc định dùng `admin@phucanh.vn` cho module Auth/RBAC và `ceo@phucanh.vn`
cho 9 module còn lại (CEO có quyền truy cập gần như mọi route theo
`AppRoutes.jsx`). Có thể override bằng biến môi trường
`WMS_ADMIN_EMAIL`/`WMS_ADMIN_PASSWORD`/`WMS_CEO_EMAIL`/`WMS_CEO_PASSWORD`
nếu tài khoản thật khác.

Đây là smoke test ở mức trang (đăng nhập đúng role, vào được trang module
mà không bị bounce về `/login` hoặc `/forbidden`) — không thực thi từng
assertion nghiệp vụ chi tiết của mỗi dòng test case trong `test_final.xlsx`.
