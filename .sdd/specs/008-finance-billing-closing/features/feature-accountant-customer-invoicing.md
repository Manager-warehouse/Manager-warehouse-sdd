# Feature: Kế toán Lập Hóa đơn Bán hàng & Ghi nhận Công nợ (US-WMS-10)

## 1. Context and Goal
Khi đơn xuất kho (`delivery_orders`) được giao thành công cho Đại lý thông qua xác nhận mã OTP và tải lên ảnh chụp bàn giao thực tế (Proof of Delivery - POD) từ tài xế (được xử lý ở Spec 004), hệ thống **tự động** tạo hóa đơn bán hàng (`invoices`) và chuyển đơn xuất kho gốc sang `COMPLETED` trong cùng giao dịch — không chờ thao tác thủ công của Kế toán viên (CLAUDE.md swimlane Outbound & Delivery; business.md ACC-01: "Invoice được tạo khi DO chuyển Delivered — tự động cộng công nợ").

Bảng `billing_notifications` là **worklist đối chiếu chỉ đọc** để Kế toán viên xác minh hóa đơn đã tự động sinh khớp với chứng từ giao nhận (`otp_verified_at`, `pod_image_url`, `pod_signature_url`, `pod_timestamp`) — đây KHÔNG phải bước bắt buộc để tạo hóa đơn.

Hệ thống tự động kiểm tra hạn mức tín dụng và khóa tín dụng đại lý (`dealers.credit_status = 'CREDIT_HOLD'`) nếu dư nợ mới vượt quá hạn mức (`credit_limit`).

## 2. Actors
* **Hệ thống**: Tự động tạo hóa đơn ngay khi đơn xuất kho hoàn tất giao hàng (POD + OTP hợp lệ).
* **Kế toán viên (`ACCOUNTANT`)**: Đối chiếu hóa đơn đã tự động sinh với chứng từ giao nhận qua worklist `billing_notifications`; có thể tạo hóa đơn thủ công chỉ trong trường hợp khắc phục lỗi (backfill) khi tự động hóa thất bại.
* **Nhân viên kế hoạch (`PLANNER`)**: Bị chặn không cho phép tạo đơn xuất kho mới cho đại lý nếu trạng thái tín dụng là `CREDIT_HOLD`.

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * Hệ thống luôn duy trì số dư nợ hiện tại `current_balance` của mỗi đại lý, cập nhật tức thời sau mỗi nghiệp vụ phát sinh hóa đơn hoặc thanh toán.
  * Hệ thống luôn kiểm tra trạng thái tín dụng `credit_status` của đại lý trước khi cho phép tạo đơn xuất kho mới.
* **Event-driven:**
  * **WHEN** tài xế xác nhận OTP thành công và tải lên đủ ảnh chụp bàn giao cho đơn xuất kho (`delivery_orders`) theo Spec 004, hệ thống **SHALL** trong cùng giao dịch:
    * Tính toán tổng tiền hóa đơn `total_amount` bằng cách tổng hợp số lượng thực tế đã xuất giao của các mặt hàng trong đơn nhân với đơn giá **snapshot tại thời điểm Thủ kho lập/hoàn tất picking plan** (lấy từ bảng `price_history` tại thời điểm đó, không phải giá hiện hành lúc giao hàng).
    * Thiết lập hạn thanh toán `due_date` = `issue_date` + `payment_term_days` (lấy từ cấu hình của đại lý).
    * Xác định kỳ kế toán hạch toán `accounting_period_id` từ ngày hạch toán chứng từ `document_date` (phải thuộc một kỳ kế toán có trạng thái `OPEN` trong bảng `accounting_periods`).
    * Tạo hóa đơn `invoices` mới với trạng thái ban đầu là `UNPAID`.
    * Cộng dồn dư nợ đại lý: `dealers.current_balance = current_balance + total_amount`.
    * **IF** `current_balance > credit_limit`: Cập nhật trạng thái tín dụng đại lý sang `credit_status = 'CREDIT_HOLD'`.
    * Cập nhật trạng thái đơn xuất kho gốc (`delivery_orders`) sang `COMPLETED`.
    * Tạo bản ghi `billing_notifications` ở trạng thái `invoice_status = 'INVOICED'` và `status = 'ARCHIVED'` để Kế toán viên đối chiếu sau (không phải để kích hoạt tạo hóa đơn).
  * **WHEN** Kế toán viên gọi `POST /api/v1/invoices` cho một đơn xuất kho đã có hóa đơn tự động, hệ thống **SHALL** từ chối với `INVOICE_ALREADY_EXISTS` (409) — endpoint này chỉ dùng để backfill khi tự động hóa thất bại.
* **State-driven:**
  * **WHILE** trạng thái tín dụng của đại lý là `CREDIT_HOLD`, hệ thống **SHALL** chặn tất cả các yêu cầu tạo mới đơn xuất kho cho đại lý đó (trả về lỗi `CREDIT_HOLD` với HTTP 422).
  * **WHILE** đơn xuất kho gốc chưa hoàn tất OTP + POD, hệ thống **SHALL** từ chối yêu cầu tạo hóa đơn thủ công (trả về lỗi `DELIVERY_ORDER_NOT_DELIVERED` với HTTP 400).
  * **WHILE** đơn xuất kho đã được lập hóa đơn trước đó hoặc ở trạng thái `COMPLETED`, hệ thống **SHALL** từ chối tạo hóa đơn trùng lặp (trả về lỗi `INVOICE_ALREADY_EXISTS` with HTTP 409).

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

### 4.2 Lập hóa đơn thủ công (chỉ dùng để backfill khi tự động hóa thất bại)
* **Protocol & Path**: `POST /api/v1/invoices`
* Endpoint này KHÔNG phải luồng tạo hóa đơn chính; hóa đơn được hệ thống tự động tạo ngay khi OTP + POD hợp lệ (mục 3, Event-driven). Chỉ dùng endpoint này khi tự động hóa thất bại và cần khôi phục thủ công.
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
    "issue_date": "2026-06-17",
    "due_date": "2026-07-17",
    "status": "UNPAID",
    "accounting_period_id": 2,
    "document_date": "2026-06-17",
    "created_at": "2026-06-17T00:30:00Z"
  }
  ```

### 4.3 Xem chi tiết hóa đơn
* **Protocol & Path**: `GET /api/v1/invoices/{id}`
* **Response 200 OK**: Trả về thông tin chi tiết hóa đơn kèm theo thông tin đại lý, danh sách mặt hàng của đơn giao hàng gốc và các bằng chứng đối chứng bàn giao (xác thực OTP, chữ ký, ảnh chụp POD thực tế).

## 5. Acceptance Criteria

* **Scenario: Khóa hạn mức công nợ khi hóa đơn tự động vượt giới hạn**
  * **Given**: Đại lý có `credit_limit = 500,000,000` VNĐ, dư nợ `current_balance = 0` VNĐ, và trạng thái `credit_status = 'ACTIVE'`.
  * **When**: Tài xế xác nhận OTP + POD hợp lệ cho đơn xuất kho trị giá `600,000,000` VNĐ của đại lý này.
  * **Then**: Hệ thống tự động tạo hóa đơn, dư nợ của đại lý cập nhật thành `600,000,000` VNĐ, và trạng thái tín dụng tự động đổi sang `CREDIT_HOLD`. Hệ thống ghi nhận log audit tương ứng.

* **Scenario: Hoàn thành đơn hàng ngay khi hóa đơn được tự động tạo**
  * **Given**: Đơn xuất kho `DO-20260612-003` đã lập kế hoạch lấy hàng và đang `IN_TRANSIT`.
  * **When**: Tài xế xác nhận OTP + POD hợp lệ cho đơn xuất kho đó.
  * **Then**: Hệ thống tự động tạo hóa đơn trong cùng giao dịch, chuyển trạng thái đơn xuất kho sang `COMPLETED`, và tạo bản ghi `billing_notifications` với `invoice_status = 'INVOICED'` và `status = 'ARCHIVED'` để Kế toán viên đối chiếu.
