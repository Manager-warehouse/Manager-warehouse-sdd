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
├── config/              # Cấu hình môi trường (staging_url, credentials, browser options)
├── drivers/             # ChromeDriver / Selenium Manager
├── pages/               # Page Object Model (POM) cho từng màn hình WMS
│   ├── LoginPage.py
│   ├── ProductPage.py
│   ├── ReceiptPage.py
│   ├── DeliveryPage.py
│   └── TransferPage.py
├── tests/               # Các kịch bản test theo 10 Module WMS
│   ├── test_001_auth.py
│   ├── test_002_mdm.py
│   ├── test_003_receipt.py
│   └── ...
├── utils/               # Excel report writer, screenshot capturer, logger
└── requirements.txt     # Dependencies (selenium, pytest, openpyxl, webdriver-manager)
```

## 4. Hướng dẫn chạy Test
```bash
pip install -r test_selenium/requirements.txt
pytest test_selenium/tests/ --html=test_selenium/report.html
```
