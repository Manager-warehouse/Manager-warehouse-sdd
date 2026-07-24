# Test Selenium Automation Framework (WMS System Test)

## 1. Overview
Thư mục `test_selenium` chứa bộ kịch bản kiểm thử tự động End-to-End (E2E System Test) sử dụng **Selenium WebDriver** cho hệ thống **Warehouse Management System (WMS)**.

## 2. Mục đích
- Thực thi **System Test Round 2** (page-level smoke check) và **Round 3** (real create/mutate business flows) tự động trên môi trường Local/Staging.
- Tự động mở trình duyệt Chrome, mô phỏng thao tác người dùng thật theo đúng role được gán cho từng module (Đăng nhập, Quản lý danh mục, Nhập kho, Xuất kho, Điều chuyển, Kiểm kê, Báo cáo).
- Tự động xuất báo cáo kết quả (Passed/Failed) vào `docs/test/test_final.xlsx` (cột Round 2 / Round 3) và `docs/test/result_test.md`.

## 3. Cấu trúc Thư mục
```text
test_selenium/
├── config/              # config.py: APP_URL/API_URL, headless flag, role credentials (env-overridable)
├── pages/               # Page Object Model (POM)
│   ├── base_page.py     # BasePage: find/click/type/is_visible/wait_for/wait_network_idle/label-driven form helpers
│   ├── wms_pages.py      # LoginPage + generic ModulePage.check_page_loaded()
│   └── round3_flows.py   # One real create/mutate-and-verify flow per module
├── run_selenium_round2.py  # Runner: logs in per module's required role, verifies
│                           # the module landing page isn't bounced to /login or /forbidden
├── run_selenium_round3.py  # Runner: executes round3_flows per module, writes Round 3 columns
├── utils/               # excel_reporter.py (writes test_final.xlsx/result_test.md),
│                         # error_tracer.py (selenium_error_report.md/.xlsx)
└── requirements.txt     # selenium, webdriver-manager, pytest, openpyxl, requests
```

## 4. Hướng dẫn chạy Test
```bash
pip install -r test_selenium/requirements.txt
python test_selenium/run_selenium_round2.py
python test_selenium/run_selenium_round3.py
```

Mặc định dùng 10 tài khoản seed thật theo role (Hải Phòng), cùng mật khẩu `password123`:
`admin@phucanh.vn` (ADMIN), `ceo@phucanh.vn` (CEO), `storekeeperHP@gmail.com` (STOREKEEPER),
`manager.hp@phucanh.vn` (WAREHOUSE_MANAGER), `hpwhstaff@gmail.com` (WAREHOUSE_STAFF),
`planer@gmail.com` (PLANNER), `dispatcher-HP@gmail.com` (DISPATCHER), `driverHP@gmail.com` (DRIVER),
`accountantHP@phucanh.vn` (ACCOUNTANT), `acc_managerHP@phucanh.vn` (ACCOUNTANT_MANAGER).
Mỗi tài khoản có thể override qua cặp biến môi trường riêng (`WMS_<ROLE>_EMAIL`/`WMS_<ROLE>_PASSWORD`)
nếu tài khoản thật thay đổi.

**Round 2** là smoke test ở mức trang (đăng nhập đúng role, vào được trang module mà không bị
bounce về `/login`/`/forbidden`, và không có error toast) — không thực thi từng assertion nghiệp vụ
chi tiết của mỗi dòng test case trong `test_final.xlsx`.

**Round 3** thực thi hành động nghiệp vụ thật (tạo user, tạo sản phẩm, lập lệnh nhập kho, lập đơn
xuất, tạo yêu cầu điều chuyển, tạo phiếu kiểm kê, tạo bản giá, ghi nhận phiếu thu, lập phiếu trả
hàng, kiểm tra dashboard KPI) rồi xác minh kết quả thật sự xuất hiện -- gần với UAT thủ công hơn
Round 2, nhưng vẫn không thay thế toàn bộ ~924 test case chi tiết. Một vài flow phụ thuộc dữ liệu
sẵn có trong hệ thống (VD: cần một hóa đơn chưa thanh toán để thu tiền, cần một DO đã giao để trả
hàng) -- nếu dữ liệu đó không tồn tại, flow được ghi nhận "Skipped" kèm lý do cụ thể thay vì báo
Pass/Fail giả.
