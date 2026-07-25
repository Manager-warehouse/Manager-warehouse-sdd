# Feature: Kế toán Lập Hóa đơn Bán hàng & Ghi nhận Công nợ (US-WMS-10)

## 1. Context and Goal


Khi đơn xuất kho (`delivery_orders`) được giao thành công cho Đại lý thông qua xác nhận mã OTP và tải lên ảnh chụp bàn giao thực tế (Proof of Delivery - POD) từ tài xế (được xử lý ở Spec 004), hệ thống tạo một bản ghi `billing_notifications` ở trạng thái `status = 'ACTIVE'`, `invoice_status = 'NOT_INVOICED'` — đây là **worklist duy nhất kích hoạt việc lập hóa đơn**, không phải một bước đối chiếu sau khi hóa đơn đã tồn tại.

Kế toán viên (`ACCOUNTANT`) tiếp nhận thông báo trên worklist, đối chiếu với chứng từ giao nhận (`otp_verified_at`, `pod_image_url`, `pod_signature_url`, `pod_timestamp`), và chủ động gọi `POST /api/v1/invoices` để lập hóa đơn bán hàng ghi nhận công nợ đại lý. Hệ thống tự động kiểm tra hạn mức tín dụng và khóa tín dụng đại lý (`dealers.credit_status = 'CREDIT_HOLD'`) **tại thời điểm lập hóa đơn** nếu dư nợ mới vượt quá hạn mức (`credit_limit`) — không còn xảy ra tại thời điểm giao hàng.



## 2. Actors
* **Hệ thống**: Tự động tạo `billing_notifications` ngay khi đơn xuất kho hoàn tất giao hàng (POD + OTP hợp lệ). Không tự tạo `invoices`.
* **Kế toán viên (`ACCOUNTANT`)**: Tiếp nhận thông báo trên worklist `billing_notifications`, đối chiếu với chứng từ giao nhận, và **chủ động** gọi `POST /api/v1/invoices` để lập hóa đơn bán hàng ghi nhận công nợ đại lý.
* **Nhân viên kế hoạch (`PLANNER`)**: Bị chặn không cho phép tạo đơn xuất kho mới cho đại lý nếu trạng thái tín dụng là `CREDIT_HOLD`.

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * Hệ thống luôn duy trì số dư nợ hiện tại `current_balance` của mỗi đại lý, cập nhật tức thời sau mỗi nghiệp vụ phát sinh hóa đơn hoặc thanh toán.
  * Hệ thống luôn kiểm tra trạng thái tín dụng `credit_status` của đại lý trước khi cho phép tạo đơn xuất kho mới.
* **Event-driven:**
  * **WHEN** tài xế xác nhận OTP thành công và tải lên đủ ảnh chụp bàn giao cho đơn xuất kho (`delivery_orders`) theo Spec 004, hệ thống **SHALL** trong cùng giao dịch:
    * Trừ tồn kho ảo In-Transit cho đúng DO đó.
    * Cập nhật trạng thái đơn xuất kho gốc (`delivery_orders`) sang `COMPLETED`.
    * Tạo bản ghi `billing_notifications` với `invoice_status = 'NOT_INVOICED'`, `status = 'ACTIVE'` — đây là **hành động tự động duy nhất** của bước này; hệ thống KHÔNG tạo `invoices` tại đây.
  * **WHEN** Kế toán viên gọi `POST /api/v1/invoices` cho một đơn xuất kho đã `COMPLETED` và chưa có hóa đơn, hệ thống **SHALL**:
    * Tính toán tổng tiền hóa đơn `total_amount` bằng cách tổng hợp số lượng thực tế đã xuất giao của các mặt hàng trong đơn nhân với đơn giá **snapshot tại thời điểm Thủ kho lập/hoàn tất picking plan** (lấy từ bảng `price_history` tại thời điểm đó, không phải giá hiện hành lúc lập hóa đơn).
    * Thiết lập hạn thanh toán `due_date` = `issue_date` + `payment_term_days` (lấy từ cấu hình của đại lý).
    * Xác định kỳ kế toán hạch toán `accounting_period_id` từ ngày hạch toán chứng từ `document_date` (phải thuộc một kỳ kế toán có trạng thái `OPEN` trong bảng `accounting_periods`).
    * Tạo hóa đơn `invoices` mới với trạng thái ban đầu là `UNPAID`.
    * Cộng dồn dư nợ đại lý: `dealers.current_balance = current_balance + total_amount`.
    * **IF** `current_balance > credit_limit`: Cập nhật trạng thái tín dụng đại lý sang `credit_status = 'CREDIT_HOLD'`.
    * Cập nhật bản ghi `billing_notifications` tương ứng (nếu còn tồn tại) sang `invoice_status = 'INVOICED'`, `status = 'ARCHIVED'`.
  * **WHEN** Kế toán viên gọi `POST /api/v1/invoices` cho một đơn xuất kho đã có hóa đơn, hệ thống **SHALL** từ chối với `INVOICE_ALREADY_EXISTS` (409).
* **State-driven:**
  * **WHILE** trạng thái tín dụng của đại lý là `CREDIT_HOLD`, hệ thống **SHALL** chặn tất cả các yêu cầu tạo mới đơn xuất kho cho đại lý đó (trả về lỗi `CREDIT_HOLD` với HTTP 422).
  * **WHILE** đơn xuất kho gốc chưa hoàn tất OTP + POD (chưa `COMPLETED`), hệ thống **SHALL** từ chối yêu cầu tạo hóa đơn (trả về lỗi `DELIVERY_ORDER_NOT_DELIVERED` với HTTP 400).
  * **WHILE** đơn xuất kho đã được lập hóa đơn trước đó, hệ thống **SHALL** từ chối tạo hóa đơn trùng lặp (trả về lỗi `INVOICE_ALREADY_EXISTS` với HTTP 409).

## 4. API Endpoints

### 4.1 Lấy danh sách thông báo lập hóa đơn cần xử lý
* **Protocol & Path**: `GET /api/v1/billing-notifications`
* **Query Params**:
  * `status`: String (Mặc định: `'ACTIVE'`)
  * `invoiceStatus`: String (Mặc định: `'NOT_INVOICED'`)
* **Response 200 OK**:
  ```json
  [
    {
      "id": 12,
      "do_id": 45,
      "do_number": "DO-20260612-003",
      "dealer_id": 3,
      "dealer_name": "Đại lý Minh Trí",
      "warehouse_id": 1,
      "delivered_at": "2026-06-16T10:00:00Z",
      "total_amount_estimate": 17000000.00,
      "invoice_status": "NOT_INVOICED",
      "status": "ACTIVE",
      "otp_verified_at": "2026-06-16T09:58:30Z",
      "pod_image_url": "https://storage.phucanh.vn/pod/photos/DO-20260612-003_delivered.jpg",
      "pod_signature_url": "https://storage.phucanh.vn/pod/signatures/DO-20260612-003_sig.png",
      "pod_timestamp": "2026-06-16T10:00:00Z"
    }
  ]
  ```

### 4.2 Lập hóa đơn bán hàng từ đơn xuất kho (luồng chính, thủ công)
* **Protocol & Path**: `POST /api/v1/invoices`
* Đây là luồng tạo hóa đơn **duy nhất**. Kế toán viên chọn một thông báo `NOT_INVOICED` trên worklist và gửi yêu cầu này để lập hóa đơn tương ứng.
* **Request Body**:
  ```json
  {
    "do_id": 45,
    "document_date": "2026-06-17",
    "notes": "Lập hóa đơn cho đơn giao hàng Minh Trí"
  }
  ```
* **Response 201 Created**:
  ```json
  {
    "id": 101,
    "invoice_number": "INV-202606-0005",
    "do_id": 45,
    "dealer_id": 3,
    "total_amount": 17000000.00,
    "paid_amount": 0.00,
    "issue_date": "2026-06-17",
    "due_date": "2026-07-17",
    "status": "UNPAID",
    "accounting_period_id": 2,
    "document_date": "2026-06-17",
    "created_at": "2026-06-17T00:30:00Z"
  }
  ```
  > `paid_amount` (thêm 2026-07-25) là trường tính toán tại thời điểm trả response — tổng `payment_receipts.amount` đã áp dụng cho hóa đơn này — không phải cột lưu trong DB. Dùng để tính dư nợ còn lại (`total_amount - paid_amount`) khi lập Phiếu thu tiếp theo cho hóa đơn này.

### 4.3 Xem chi tiết hóa đơn
* **Protocol & Path**: `GET /api/v1/invoices/{id}`
* **Response 200 OK**: Trả về thông tin chi tiết hóa đơn (bao gồm `paid_amount`, xem 4.2) kèm theo thông tin đại lý, danh sách mặt hàng của đơn giao hàng gốc và các bằng chứng đối chứng bàn giao (xác thực OTP, chữ ký, ảnh chụp POD thực tế).

## 5. Acceptance Criteria

* **Scenario: Xác nhận giao hàng chỉ tạo thông báo, không tự tạo hóa đơn**
  * **Given**: Đơn xuất kho `DO-20260612-003` đã lập kế hoạch lấy hàng và đang `IN_TRANSIT`.
  * **When**: Tài xế xác nhận OTP + POD hợp lệ cho đơn xuất kho đó.
  * **Then**: Hệ thống chuyển trạng thái đơn xuất kho sang `COMPLETED` và tạo bản ghi `billing_notifications` với `invoice_status = 'NOT_INVOICED'`, `status = 'ACTIVE'`.
  * **And**: KHÔNG có bản ghi `invoices` nào được tạo tại bước này; `dealers.current_balance` của đại lý không đổi.

* **Scenario: Kế toán viên lập hóa đơn từ thông báo, vượt hạn mức tín dụng**
  * **Given**: Đại lý có `credit_limit = 500,000,000` VNĐ, dư nợ `current_balance = 0` VNĐ, và trạng thái `credit_status = 'ACTIVE'`. Có thông báo `NOT_INVOICED` cho đơn xuất kho trị giá `600,000,000` VNĐ của đại lý này.
  * **When**: Kế toán viên gọi `POST /api/v1/invoices` cho đơn xuất kho đó.
  * **Then**: Hệ thống tạo hóa đơn, dư nợ của đại lý cập nhật thành `600,000,000` VNĐ, trạng thái tín dụng tự động đổi sang `CREDIT_HOLD`, và thông báo tương ứng chuyển `invoice_status = 'INVOICED'`, `status = 'ARCHIVED'`. Hệ thống ghi nhận log audit tương ứng.

* **Scenario: Từ chối lập hóa đơn trùng lặp**
  * **Given**: Đơn xuất kho `DO-20260612-003` đã có hóa đơn `INV-202606-0005`.
  * **When**: Kế toán viên gọi lại `POST /api/v1/invoices` cho cùng `do_id = 45`.
  * **Then**: HTTP 409 `INVOICE_ALREADY_EXISTS`. Không tạo thêm hóa đơn nào.

* **Scenario: Từ chối lập hóa đơn khi DO chưa giao xong**
  * **Given**: Đơn xuất kho đang `IN_TRANSIT`, chưa xác nhận OTP + POD.
  * **When**: Kế toán viên gọi `POST /api/v1/invoices` cho DO đó.
  * **Then**: HTTP 400 `DELIVERY_ORDER_NOT_DELIVERED`.
