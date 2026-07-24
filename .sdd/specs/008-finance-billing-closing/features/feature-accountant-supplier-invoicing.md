# Feature: Kế toán Lập Hóa đơn Mua hàng & Ghi nhận Công nợ Nhà cung cấp (US-WMS-28)

## 1. Context and Goal
Khi phiếu nhập kho (`receipts`) được Trưởng kho duyệt xong sau kiểm đếm và QC (chuyển trạng thái sang `APPROVED`, mở khóa putaway theo Spec 003 — `receipts` không có trạng thái `COMPLETED` riêng, `APPROVED` là trạng thái cuối cùng sau khi duyệt), hệ thống **tự động** tạo thông báo lập hóa đơn mua hàng (`supplier_billing_notifications`). 

Kế toán viên (`ACCOUNTANT`) tiếp nhận thông báo, kiểm tra đối chiếu phiếu nhập kho thực tế với hóa đơn tài chính do Nhà cung cấp (NCC) phát hành, và thực hiện lập Hóa đơn Mua hàng (`supplier_invoices`). Việc lập hóa đơn ghi nhận dư nợ phải trả cho Nhà cung cấp (`suppliers.current_balance`), gán hạn thanh toán, và thuộc về một Kỳ kế toán đang mở (`accounting_periods.status = 'OPEN'`).

Bên cạnh đó, Kế toán viên có thể tạo Phiếu chi (`supplier_payments`) để ghi nhận các khoản thanh toán thực tế cho Nhà cung cấp, cấn trừ dư nợ tương ứng trên từng Hóa đơn mua hàng.

## 2. Actors
* **Hệ thống**: Tự động sinh thông báo `supplier_billing_notifications` khi phiếu nhập kho (`receipts`) chuyển sang trạng thái `APPROVED`.
* **Kế toán viên (`ACCOUNTANT`)**: Tiếp nhận thông báo, kiểm tra phiếu nhập kho, lập Hóa đơn mua hàng (`supplier_invoices`) và lập Phiếu chi thanh toán (`supplier_payments`) cho NCC.
* **Kế toán trưởng (`ACCOUNTANT_MANAGER`)**: Xem danh sách hóa đơn mua hàng, theo dõi dư nợ phải trả NCC, và phê duyệt các chứng từ điều chỉnh (nếu có).

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * Hệ thống luôn duy trì dư nợ phải trả Nhà cung cấp `suppliers.current_balance`, tự động cộng tăng khi lập Hóa đơn mua hàng và trừ giảm khi ghi nhận Phiếu chi thanh toán.
  * Mọi chứng từ Hóa đơn mua hàng và Phiếu chi thanh toán phải gắn liền với một Kỳ kế toán `accounting_period_id` đang ở trạng thái `OPEN`.
* **Event-driven:**
  * **WHEN** phiếu nhập kho (`receipts`) chuyển sang trạng thái `APPROVED` theo nghiệp vụ tại Spec 003, hệ thống **SHALL** trong cùng giao dịch:
    * Tạo một bản ghi `supplier_billing_notifications` ở trạng thái `status = 'ACTIVE'` và `invoice_status = 'NOT_INVOICED'` để Kế toán viên tiếp nhận trên worklist.
  * **WHEN** Kế toán viên gửi yêu cầu lập hóa đơn mua hàng qua `POST /api/v1/supplier-invoices` với `receiptId` hợp lệ, hệ thống **SHALL**:
    * Kiểm tra phiếu nhập kho `receipts` phải ở trạng thái `APPROVED` và chưa từng lập hóa đơn mua hàng trước đó (`supplier_invoices.receipt_id` chưa tồn tại).
    * Kiểm tra ngày hạch toán `documentDate` thuộc một kỳ kế toán có trạng thái `OPEN`.
    * Sinh mã hóa đơn mua hàng nội bộ (`invoice_number`, ví dụ: `SINV-202607-0001`).
    * Lưu số hóa đơn gốc do Nhà cung cấp phát hành (`supplier_invoice_number`).
    * Tính toán tổng tiền hóa đơn `total_amount` dựa trên số lượng thực nhập × đơn giá nhập hợp lệ.
    * Tạo bản ghi `supplier_invoices` ở trạng thái `status = 'UNPAID'`.
    * Cộng dồn dư nợ phải trả NCC: `suppliers.current_balance = current_balance + total_amount`.
    * Cập nhật bản ghi `supplier_billing_notifications` tương ứng sang `invoice_status = 'INVOICED'` và `status = 'ARCHIVED'`.
    * Tạo bản ghi audit log hành động `CREATE_SUPPLIER_INVOICE`.
  * **WHEN** Kế toán viên gửi yêu cầu tạo Phiếu chi qua `POST /api/v1/supplier-payments` với `supplierInvoiceId` và `amount` hợp lệ, hệ thống **SHALL**:
    * Kiểm tra hóa đơn mua hàng `supplier_invoices` chưa ở trạng thái `PAID` và số tiền thanh toán `amount <= remaining_balance` của hóa đơn đó.
    * Tạo bản ghi `supplier_payments`.
    * Trừ số dư nợ của hóa đơn mua hàng. Nếu số tiền đã thanh toán đủ `total_amount`, cập nhật hóa đơn sang `PAID`, ngược lại cập nhật `PARTIALLY_PAID`.
    * Giảm dư nợ phải trả NCC: `suppliers.current_balance = current_balance - amount`.
    * Tạo bản ghi audit log hành động `CREATE_SUPPLIER_PAYMENT`.
* **State-driven:**
  * **WHILE** phiếu nhập kho gốc chưa ở trạng thái `APPROVED`, hệ thống **SHALL** từ chối lập hóa đơn mua hàng (trả về lỗi `RECEIPT_NOT_APPROVED` với HTTP 400).
  * **WHILE** phiếu nhập kho đã được lập hóa đơn trước đó, hệ thống **SHALL** từ chối tạo hóa đơn trùng lặp (trả về lỗi `SUPPLIER_INVOICE_ALREADY_EXISTS` với HTTP 409).
  * **WHILE** kỳ kế toán tương ứng với ngày hạch toán chứng từ đã ở trạng thái `CLOSED`, hệ thống **SHALL** chặn việc tạo Hóa đơn mua hàng hoặc Phiếu chi (trả về lỗi `PERIOD_CLOSED` với HTTP 422).

## 4. API Endpoints

### 4.1 Lấy danh sách thông báo lập hóa đơn mua hàng
* **Protocol & Path**: `GET /api/v1/supplier-billing-notifications`
* **Query Params**: `status`, `invoiceStatus`, `supplierId`
* **Response 200 OK**:
  ```json
  [
    {
      "id": 1,
      "receiptId": 10,
      "receiptNumber": "RO-20260710-001",
      "supplierId": 5,
      "supplierName": "Công ty TNHH Gia Dụng Phúng",
      "warehouseId": 1,
      "completedAt": "2026-07-20T14:30:00Z",
      "totalAmountEstimate": 45000000.00,
      "invoiceStatus": "NOT_INVOICED",
      "status": "ACTIVE"
    }
  ]
  ```

### 4.2 Lập hóa đơn mua hàng từ Phiếu nhập kho
* **Protocol & Path**: `POST /api/v1/supplier-invoices`
* **Request Body**:
  ```json
  {
    "receiptId": 10,
    "supplierInvoiceNumber": "VAT-NCC-88392",
    "documentDate": "2026-07-21",
    "dueDate": "2026-08-20",
    "notes": "Nhập kho lô gia dụng nồi chảo"
  }
  ```
* **Response 201 Created**:
  ```json
  {
    "id": 50,
    "invoiceNumber": "SINV-202607-0001",
    "supplierInvoiceNumber": "VAT-NCC-88392",
    "receiptId": 10,
    "supplierId": 5,
    "totalAmount": 45000000.00,
    "issueDate": "2026-07-21",
    "dueDate": "2026-08-20",
    "status": "UNPAID",
    "accountingPeriodId": 3,
    "documentDate": "2026-07-21",
    "createdAt": "2026-07-21T09:00:00Z"
  }
  ```

### 4.3 Xem danh sách và chi tiết hóa đơn mua hàng
* **GET** `/api/v1/supplier-invoices`: Danh sách hóa đơn mua hàng (hỗ trợ lọc theo `supplierId`, `status`, `accountingPeriodId`).
* **GET** `/api/v1/supplier-invoices/{id}`: Xem chi tiết hóa đơn mua hàng kèm danh sách mặt hàng thực nhập.

### 4.4 Lập phiếu chi thanh toán cho Nhà cung cấp
* **Protocol & Path**: `POST /api/v1/supplier-payments`
* **Request Body**:
  ```json
  {
    "supplierId": 5,
    "supplierInvoiceId": 50,
    "amount": 20000000.00,
    "paymentDate": "2026-07-22",
    "paymentMethod": "BANK_TRANSFER",
    "documentDate": "2026-07-22",
    "notes": "Thanh toán đợt 1 hóa đơn VAT-NCC-88392"
  }
  ```
* **Response 201 Created**:
  ```json
  {
    "id": 30,
    "paymentNumber": "SPAY-202607-0001",
    "supplierId": 5,
    "supplierInvoiceId": 50,
    "amount": 20000000.00,
    "paymentDate": "2026-07-22",
    "paymentMethod": "BANK_TRANSFER",
    "accountingPeriodId": 3,
    "documentDate": "2026-07-22",
    "createdAt": "2026-07-22T10:15:00Z"
  }
  ```

### 4.5 Upload và quét OCR Ủy nhiệm chi thanh toán cho NCC
* **Protocol & Path**: `POST /api/v1/supplier-payments/ocr`
* **Request Header**: `Content-Type: multipart/form-data`
* **Request Body**: `file` (Ảnh UNC / Giấy báo Nợ ngân hàng, max 5MB)
* **Response 200 OK**:
  ```json
  {
    "amount": 20000000.00,
    "paymentDate": "2026-07-22",
    "supplierId": 5,
    "supplierInvoiceId": 50,
    "notes": "UNC CHI TIEN HANG - NCC GIA DUNG PHUNG - GD 88392",
    "confidenceScore": 0.94
  }
  ```

## 5. Acceptance Criteria

* **Scenario: Lập Hóa đơn Mua hàng ghi nhận nợ NCC thành công**
  * **Given**: Phiếu nhập kho `RO-20260710-001` đã ở trạng thái `APPROVED` với tổng giá trị `45,000,000` VNĐ, dư nợ hiện tại của NCC là `0` VNĐ.
  * **When**: Kế toán viên gửi yêu cầu lập hóa đơn mua hàng với số hóa đơn NCC `VAT-NCC-88392`.
  * **Then**: Hệ thống tạo hóa đơn `SINV-202607-0001` ở trạng thái `UNPAID`, dư nợ NCC `suppliers.current_balance` tăng lên `45,000,000` VNĐ, thông báo lập hóa đơn đổi thành `INVOICED` / `ARCHIVED`, và audit log được tạo.

* **Scenario: Quét OCR Ủy nhiệm chi NCC tự động điền form phiếu chi**
  * **Given**: Kế toán viên tải lên ảnh chụp Ủy nhiệm chi ngân hàng thanh toán cho Nhà cung cấp Gia Dụng Phúng số tiền `20,000,000` VNĐ.
  * **When**: Kế toán viên gửi file ảnh tới `POST /api/v1/supplier-payments/ocr`.
  * **Then**: Hệ thống trả về `amount = 20000000.00`, map `supplierId = 5`, và tự động điền thông tin vào form tạo phiếu chi.

* **Scenario: Thanh toán phiếu chi giảm nợ NCC**
  * **Given**: Hóa đơn mua hàng `SINV-202607-0001` có tổng tiền `45,000,000` VNĐ ở trạng thái `UNPAID`, dư nợ NCC là `45,000,000` VNĐ.
  * **When**: Kế toán viên lập phiếu chi thanh toán `20,000,000` VNĐ cho hóa đơn này.
  * **Then**: Hệ thống tạo phiếu chi `SPAY-202607-0001`, trạng thái hóa đơn mua hàng chuyển thành `PARTIALLY_PAID`, dư nợ NCC giảm còn `25,000,000` VNĐ.

* **Scenario: Chặn lập hóa đơn cho phiếu nhập chưa APPROVED**
  * **Given**: Phiếu nhập kho `RO-20260710-002` đang ở trạng thái `QC_COMPLETED` (chưa duyệt putaway xong).
  * **When**: Kế toán viên cố gắng lập hóa đơn mua hàng cho phiếu nhập kho này.
  * **Then**: Hệ thống từ chối yêu cầu và trả về lỗi `RECEIPT_NOT_APPROVED` (HTTP 400).
