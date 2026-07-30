# Feature: Kế toán Ghi nhận Thanh toán & Quản lý Công nợ (US-WMS-15)

## 1. Context and Goal

Kế toán viên (`ACCOUNTANT`) thực hiện ghi nhận các khoản thanh toán từ đại lý thông qua phiếu thu (`payment_receipts`), thực hiện cấn trừ cho các hóa đơn bán hàng (`invoices`) và giảm dư nợ hiện tại (`dealers.current_balance`).
Hệ thống tự động kiểm tra và mở khóa tín dụng (chuyển sang trạng thái `credit_status = 'ACTIVE'`) khi dư nợ giảm xuống dưới ngưỡng an toàn (mặc định là 80% hạn mức tín dụng). Ngoài ra, hệ thống chạy Job định kỳ quét cuối ngày để tự động khóa công nợ các đại lý có hóa đơn nợ quá hạn lâu ngày.

## 2. Actors

- **Kế toán viên (`ACCOUNTANT`)**: Maker ghi nhận phiếu thu nộp tiền của đại lý trên hệ thống.
- **Hệ thống (Daily Job)**: Định kỳ quét các hóa đơn quá hạn thanh toán để khóa tín dụng đại lý.

## 3. Functional Requirements (EARS)

- **Ubiquitous:**
  - Hệ thống luôn cập nhật và trừ dư nợ đại lý `dealers.current_balance` tương ứng với số tiền nhận được trên mỗi phiếu thu hợp lệ.
- **State-driven:**
  - **WHILE** hóa đơn liên kết đã ở trạng thái `PAID`, hệ thống **SHALL** từ chối tạo thêm phiếu thu cho hóa đơn đó (trả về lỗi `INVOICE_ALREADY_PAID` với HTTP 409).
  - **WHILE** số tiền phiếu thu vượt quá dư nợ còn lại của hóa đơn (`total_amount - Σ payment_receipts.amount` đã ghi nhận trước đó), hệ thống **SHALL** từ chối tạo phiếu thu (trả về lỗi `OVERPAYMENT_EXCEEDS_INVOICE` với HTTP 422) — không cho phép một hóa đơn nhận thừa tiền qua nhiều phiếu thu cộng dồn.
- **Event-driven:**
  - **WHEN** Kế toán viên ghi nhận một phiếu thu nộp tiền thành công, hệ thống **SHALL**:
    - Tạo bản ghi phiếu thu trong bảng `payment_receipts` với kỳ kế toán `accounting_period_id` tương ứng với Ngày Nhập Hàng `document_date` (phải thuộc một kỳ kế toán có trạng thái `OPEN`).
    - Trừ số dư nợ đại lý: `dealers.current_balance = current_balance - amount`.
    - Tính toán số tiền đã cấn trừ cho hóa đơn được liên kết:
      - Cập nhật số tiền đã thanh toán của hóa đơn.
      - Cập nhật trạng thái hóa đơn sang `PAID` nếu đã thanh toán đủ tổng giá trị hóa đơn, hoặc sang `PARTIALLY_PAID` nếu thanh toán một phần.
    - Đọc giá trị cấu hình `CREDIT_UNLOCK_BUFFER_PCT` (mặc định `0.8`) từ bảng `system_configs`.
    - **IF** `current_balance < credit_limit * CREDIT_UNLOCK_BUFFER_PCT`: Hệ thống tự động mở khóa tín dụng đại lý, cập nhật trạng thái `dealers.credit_status = 'ACTIVE'`.
  - **WHEN** Daily Job quét cuối ngày được kích hoạt, hệ thống **SHALL**:
    - Đọc giá trị cấu hình số ngày quá hạn tối đa `CREDIT_HOLD_OVERDUE_DAYS` (mặc định `30` ngày) từ bảng `system_configs`.
    - Quét toàn bộ hóa đơn có trạng thái `UNPAID` hoặc `PARTIALLY_PAID` có ngày đến hạn thanh toán `due_date` quá hạn vượt quá số ngày cấu hình so với ngày hiện tại.
    - **IF** phát hiện đại lý có ít nhất một hóa đơn quá hạn vượt mốc cấu hình: Hệ thống tự động cập nhật trạng thái đại lý sang `dealers.credit_status = 'CREDIT_HOLD'`.

## 4. API Endpoints

### 4.1 Tạo phiếu thu cấn trừ công nợ

- **Protocol & Path**: `POST /api/v1/payment-receipts`
- **Request Body**:
  ```json
  {
    "dealer_id": 3,
    "invoice_id": 101,
    "amount": 10000000.0,
    "payment_date": "2026-06-17",
    "payment_method": "BANK_TRANSFER",
    "notes": "Đại lý Minh Trí chuyển khoản thanh toán đợt 1"
  }
  ```
- **Response 201 Created**:
  ```json
  {
    "id": 201,
    "payment_number": "PAY-202606-0003",
    "dealer_id": 3,
    "invoice_id": 101,
    "amount": 10000000.0,
    "payment_date": "2026-06-17",
    "payment_method": "BANK_TRANSFER",
    "accounting_period_id": 2,
    "document_date": "2026-06-17",
    "notes": "Đại lý Minh Trí chuyển khoản thanh toán đợt 1",
    "created_at": "2026-06-17T01:00:00Z"
  }
  ```

### 4.2 Lấy danh sách phiếu thu

- **Protocol & Path**: `GET /api/v1/payment-receipts`
- **Query Params**:
  - `dealerId`: BIGINT (Optional)
  - `accountingPeriodId`: BIGINT (Optional)
- **Response 200 OK**: Trả về danh sách phiếu thu công nợ phù hợp điều kiện lọc.

## 5. Acceptance Criteria

- **Scenario 1: Ghi nhận thanh toán nhưng chưa đạt ngưỡng mở khóa**
  - **Given**: Đại lý có `credit_limit = 500,000,000` VNĐ, dư nợ `current_balance = 600,000,000` VNĐ, đang có trạng thái `credit_status = 'CREDIT_HOLD'`. Cấu hình `CREDIT_UNLOCK_BUFFER_PCT = 0.8`. Ngưỡng mở khóa là `400,000,000` VNĐ.
  - **When**: Kế toán viên tạo phiếu thu thanh toán `200,000,000` VNĐ (dư nợ mới cập nhật là `400,000,000` VNĐ).
  - **Then**: Trạng thái tín dụng đại lý vẫn được giữ nguyên là `CREDIT_HOLD`. Hệ thống đưa ra cảnh báo cho kế toán biết cần thanh toán thêm để mở khóa.

- **Scenario 2: Ghi nhận thanh toán vượt ngưỡng mở khóa thành công**
  - **Given**: Đại lý có `credit_limit = 500,000,000` VNĐ, dư nợ `current_balance = 600,000,000` VNĐ, trạng thái `credit_status = 'CREDIT_HOLD'`. Cấu hình `CREDIT_UNLOCK_BUFFER_PCT = 0.8`. Ngưỡng mở khóa là `400,000,000` VNĐ.
  - **When**: Kế toán viên tạo phiếu thu thanh toán `201,000,000` VNĐ (dư nợ mới cập nhật là `399,000,000` VNĐ, nhỏ hơn `400,000,000` VNĐ).
  - **Then**: Trạng thái tín dụng của đại lý tự động chuyển đổi thành `ACTIVE`.

- **Scenario 3: Từ chối phiếu thu vượt quá dư nợ hóa đơn**
  - **Given**: Hóa đơn `INV-202606-0005` có `total_amount = 17,000,000` VNĐ, đã thu `10,000,000` VNĐ qua một phiếu thu trước đó (còn lại `7,000,000` VNĐ).
  - **When**: Kế toán viên tạo phiếu thu mới với `amount = 8,000,000` VNĐ cho cùng hóa đơn.
  - **Then**: HTTP 422 `OVERPAYMENT_EXCEEDS_INVOICE`. Không tạo phiếu thu, không thay đổi `dealers.current_balance`.

- **Scenario 4: Từ chối phiếu thu cho hóa đơn đã PAID**
  - **Given**: Hóa đơn `INV-202606-0004` đã ở trạng thái `PAID`.
  - **When**: Kế toán viên cố tạo thêm phiếu thu cho hóa đơn này.
  - **Then**: HTTP 409 `INVOICE_ALREADY_PAID`.

> **Lưu ý liên quan Credit Note**: `credit_notes` (Spec 009, hàng hoàn trả) cũng làm giảm `dealers.current_balance` nhưng đi qua một cơ chế hoàn toàn khác (`ReturnsService.createCreditNote()`), không phải phiếu thu và không kiểm tra `OVERPAYMENT_EXCEEDS_INVOICE` vì Credit Note không gắn với một hóa đơn cụ thể để so sánh — xem `spec.md` Session 2026-07-25 về khả năng `current_balance` xuống âm qua đường này.
