# Feature: Bút toán Điều chỉnh Sai sót Kỳ đã Chốt (Correction Voucher) (US-WMS-29)

## 1. Context and Goal

Kỳ kế toán đã `CLOSED` khóa cứng mọi chứng từ có `document_date` thuộc kỳ đó (US-WMS-17) — không ai được sửa/xóa trực tiếp `invoices`, `payment_receipts`, `supplier_invoices`, `supplier_payments` trong kỳ đã đóng. Khóa kỳ không có nghĩa là sai sót phát hiện sau đó không bao giờ được sửa — nó chỉ có nghĩa là sai sót không được sửa **tại chỗ** trên chứng từ gốc.

Tính năng này lấp khoảng trống đó bằng cách kích hoạt giá trị `type = 'CORRECTION_VOUCHER'` vốn đã tồn tại sẵn trong `adjustments.type` từ Spec 006 nhưng chưa từng được dùng cho chứng từ tài chính thuần túy (không có hàng hóa liên quan). Khi Kế toán trưởng phát hiện một `invoices`, `payment_receipts`, `supplier_invoices`, hoặc `supplier_payments` đã tồn tại thuộc kỳ đã `CLOSED` bị ghi sai, họ tạo trực tiếp một bản ghi `adjustments` loại `CORRECTION_VOUCHER` với `document_date` thuộc kỳ đang `OPEN`, tham chiếu ngược tới chứng từ gốc qua `reference_type`/`reference_id` (cột đã có sẵn, dùng chung với `STOCK_TAKE`/`TRANSFER_DISCREPANCY`/`DISPOSAL`/`RETURN_TO_VENDOR`). Chứng từ gốc trong kỳ đã đóng không bao giờ bị `UPDATE`/`DELETE` — chỉ số dư hiện tại (`dealers.current_balance` / `suppliers.current_balance`) thay đổi, cùng cơ chế mà `payment_receipts` và `credit_notes` đã dùng để điều chỉnh số dư mà không sửa hóa đơn gốc.

Đây KHÔNG phải một chứng từ/nghiệp vụ riêng biệt về mặt hạ tầng — nó là một cách dùng mới của bảng `adjustments` đã có, giới hạn cho một actor duy nhất (`ACCOUNTANT_MANAGER`, cùng người có thẩm quyền khóa kỳ), một bước tạo duy nhất (không qua vòng duyệt riêng, vì `ACCOUNTANT_MANAGER` là thẩm quyền cao nhất trong luồng tài chính của Spec 008 — giống mọi hành động AR/AP khác trong spec này, `ACCOUNTANT` tạo `payment_receipts`/`supplier_invoices`/`supplier_payments` và số dư thay đổi ngay lập tức, không qua bước duyệt thứ hai).

Tính năng KHÔNG thay thế `credit_notes` (US-WMS-24, Spec 009) — `credit_notes` vẫn là cơ chế duy nhất cho hàng hoàn trả vật lý. Correction Voucher chỉ xử lý sai sót **ghi nhận** (không có hàng hóa liên quan) trên chứng từ tài chính đã tồn tại của cả hai luồng AR và AP.

## 2. Actors

* **Kế toán trưởng (`ACCOUNTANT_MANAGER`)**: Người duy nhất có thẩm quyền tạo bút toán điều chỉnh — cùng vai trò đã sở hữu quyền khóa kỳ kế toán (US-WMS-17), nên không cần thêm một cấp duyệt riêng cho việc sửa sai sót của chính kỳ mà họ quản lý.

## 3. Functional Requirements (EARS)

* **Ubiquitous:**
  * Hệ thống luôn giữ nguyên (không `UPDATE`/`DELETE`) chứng từ gốc (`invoices`, `payment_receipts`, `supplier_invoices`, `supplier_payments`) khi xử lý một bút toán điều chỉnh — chỉ `dealers.current_balance` hoặc `suppliers.current_balance` bị thay đổi.
* **Event-driven:**
  * **WHEN** Kế toán trưởng gửi yêu cầu `POST /api/v1/correction-vouchers` với `referenceType`, `referenceId`, `amountDelta`, `reason`, `documentDate`, hệ thống **SHALL**:
    * Xác định chứng từ gốc theo `referenceType` (`INVOICE` → `invoices`, `PAYMENT_RECEIPT` → `payment_receipts`, `SUPPLIER_INVOICE` → `supplier_invoices`, `SUPPLIER_PAYMENT` → `supplier_payments`) và kiểm tra chứng từ tồn tại (`REFERENCE_DOCUMENT_NOT_FOUND` nếu không tồn tại).
    * Kiểm tra kỳ kế toán của chứng từ gốc đang ở trạng thái `CLOSED` — điều kiện tiên quyết để dùng Correction Voucher thay vì luồng sửa thông thường.
    * Kiểm tra `documentDate` thuộc một kỳ kế toán có trạng thái `OPEN`, gán `accounting_period_id` tương ứng.
    * Tạo một bản ghi `adjustments` với `type = 'CORRECTION_VOUCHER'`, `warehouse_id = NULL`, `product_id = NULL`, `quantity_adjustment = NULL`, `amount_delta` = giá trị điều chỉnh (có dấu), `reference_type`/`reference_id` trỏ về chứng từ gốc, `reason`, `document_date`, `accounting_period_id`, `created_by` = actor, `approved_by` = actor, `approved_at` = thời điểm tạo (tạo và duyệt cùng một bước, vì actor đã là thẩm quyền cao nhất).
    * **IF** `referenceType IN ('INVOICE', 'PAYMENT_RECEIPT')`: `dealers.current_balance = current_balance + amount_delta`, sau đó áp dụng lại đúng logic kiểm tra tín dụng đang dùng cho `payment_receipts` (US-WMS-15): nếu `current_balance > credit_limit` → `credit_status = 'CREDIT_HOLD'`; nếu `current_balance < credit_limit * CREDIT_UNLOCK_BUFFER_PCT` → `credit_status = 'ACTIVE'`.
    * **IF** `referenceType IN ('SUPPLIER_INVOICE', 'SUPPLIER_PAYMENT')`: `suppliers.current_balance = current_balance + amount_delta`.
    * Tạo bản ghi audit log `CORRECTION_VOUCHER_CREATE` (bao gồm chứng từ gốc, kỳ gốc, kỳ hạch toán mới, số tiền, lý do, số dư trước/sau).
* **State-driven:**
  * **WHILE** kỳ kế toán của chứng từ gốc chưa `CLOSED`, hệ thống **SHALL** từ chối tạo bút toán điều chỉnh (trả về lỗi `ORIGINAL_PERIOD_NOT_CLOSED` với HTTP 422) — sai sót trên chứng từ thuộc kỳ còn `OPEN` không thuộc phạm vi tính năng này.
  * **WHILE** kỳ kế toán tương ứng với `documentDate` đã `CLOSED`, hệ thống **SHALL** từ chối tạo bút toán điều chỉnh (trả về lỗi `PERIOD_CLOSED` với HTTP 422) — bút toán điều chỉnh luôn phải nằm ở kỳ đang mở.

## 4. Data Model

Không tạo bảng mới. Mở rộng bảng `adjustments` đã có (Spec 006):

* Đổi ràng buộc `warehouse_id NOT NULL` và `product_id NOT NULL` thành ràng buộc có điều kiện: `CHECK (type = 'CORRECTION_VOUCHER' OR (warehouse_id IS NOT NULL AND product_id IS NOT NULL))` — giữ nguyên đảm bảo bắt buộc cho 4 loại điều chỉnh tồn kho hiện có (`STOCK_TAKE`, `TRANSFER_DISCREPANCY`, `DISPOSAL`, `RETURN_TO_VENDOR`), chỉ nới lỏng cho `CORRECTION_VOUCHER`.
* Tương tự với `quantity_adjustment NOT NULL` → `CHECK (type = 'CORRECTION_VOUCHER' OR quantity_adjustment IS NOT NULL)`.
* Thêm cột mới `amount_delta` (DECIMAL(18,2), NULL) — chỉ dùng khi `type = 'CORRECTION_VOUCHER'`; có dấu, dương = tăng công nợ, âm = giảm công nợ. Không tái sử dụng `quantity_adjustment` cho việc này vì đơn vị/ý nghĩa khác nhau (số lượng vs. tiền tệ).
* `reference_type`/`reference_id` (đã có sẵn, không có `CHECK` ràng buộc giá trị) mở rộng nhận thêm `'INVOICE'`, `'PAYMENT_RECEIPT'`, `'SUPPLIER_INVOICE'`, `'SUPPLIER_PAYMENT'` bên cạnh các giá trị hiện có.
* `dealer_id`/`supplier_id` không lưu trực tiếp trên `adjustments` — được suy ra khi đọc bằng cách join `reference_id`/`reference_type` với bảng chứng từ gốc tương ứng, tránh trùng lặp dữ liệu.
* `approved_by`/`approved_at` được set ngay tại thời điểm tạo (không có trạng thái chờ duyệt riêng cho `CORRECTION_VOUCHER`) — phù hợp với truy vấn hiện có trong `AccountingPeriodServiceImpl` vốn coi `approved_by IS NULL` là dấu hiệu "còn dở dang" khi kiểm tra điều kiện khóa kỳ, bất kể `type`.

Migration cần: nới ràng buộc trên 3 cột, thêm 1 cột `amount_delta`, cập nhật entity `Adjustment.java` (bỏ `nullable = false` trên `@JoinColumn` của `warehouse`/`product`, thêm field `amountDelta`). Không cần sửa `AdjustmentRepository` (4 query hiện có không tham chiếu `warehouse`/`product`) và không ảnh hưởng `DisposalService` (luồng duy nhất có đọc `adjustment.getWarehouse()`/`getProduct()` đã lọc cứng theo `type = DISPOSAL`).

## 5. API Endpoints

### 5.1 Lập bút toán điều chỉnh
* **Protocol & Path**: `POST /api/v1/correction-vouchers`
* **Request Body**:
  ```json
  {
    "reference_type": "INVOICE",
    "reference_id": 101,
    "amount_delta": -2000000.00,
    "reason": "Hóa đơn INV-202606-0005 ghi nhầm đơn giá, kỳ 2026-06 đã chốt sổ",
    "document_date": "2026-07-24"
  }
  ```
* **Response 201 Created**:
  ```json
  {
    "id": 5,
    "adjustment_number": "ADJ-202607-0012",
    "reference_type": "INVOICE",
    "reference_id": 101,
    "dealer_id": 3,
    "amount_delta": -2000000.00,
    "reason": "Hóa đơn INV-202606-0005 ghi nhầm đơn giá, kỳ 2026-06 đã chốt sổ",
    "document_date": "2026-07-24",
    "accounting_period_id": 3,
    "original_period_id": 2,
    "original_period_name": "2026-06",
    "approved_by": 6,
    "approved_by_name": "Kế toán trưởng",
    "approved_at": "2026-07-24T09:00:00Z",
    "created_at": "2026-07-24T09:00:00Z"
  }
  ```

### 5.2 Danh sách bút toán điều chỉnh
* **Protocol & Path**: `GET /api/v1/correction-vouchers`
* **Query Params**: `referenceType`, `dealerId`, `supplierId` (Optional)
* **Actor**: `ACCOUNTANT`, `ACCOUNTANT_MANAGER` (đọc để đối chiếu; chỉ `ACCOUNTANT_MANAGER` được tạo)

## 6. Error Handling

| Error Code | HTTP Status | Điều kiện kích hoạt |
|------------|-------------|---------------------|
| `ORIGINAL_PERIOD_NOT_CLOSED` | 422 Unprocessable Entity | Chứng từ gốc thuộc kỳ kế toán chưa `CLOSED` — không thuộc phạm vi Correction Voucher |
| `PERIOD_CLOSED` | 422 Unprocessable Entity | `documentDate` của bút toán điều chỉnh thuộc kỳ kế toán đã `CLOSED` |
| `REFERENCE_DOCUMENT_NOT_FOUND` | 404 Not Found | `referenceId` không tồn tại với `referenceType` tương ứng |

## 7. Acceptance Criteria

* **Scenario: Lập bút toán điều chỉnh cho hóa đơn thuộc kỳ đã chốt**
  * **Given**: Hóa đơn `INV-202606-0005` thuộc kỳ `2026-06` đã `CLOSED`, ghi nhầm `total_amount` cao hơn thực tế `2,000,000` VNĐ. Dư nợ đại lý hiện tại là `50,000,000` VNĐ.
  * **When**: Kế toán trưởng tạo bút toán điều chỉnh với `amountDelta = -2,000,000`, `documentDate` thuộc kỳ `2026-07` đang `OPEN`.
  * **Then**: Hệ thống tạo bản ghi `adjustments` (`type = CORRECTION_VOUCHER`, `warehouse_id`/`product_id` = NULL) đã `approved_by`/`approved_at` ngay lúc tạo; hóa đơn gốc `INV-202606-0005` không bị thay đổi bất kỳ trường nào; dư nợ đại lý giảm ngay còn `48,000,000` VNĐ; audit log `CORRECTION_VOUCHER_CREATE` được ghi kèm số dư trước/sau.

* **Scenario: Bút toán điều chỉnh đưa dư nợ vượt hạn mức tín dụng**
  * **Given**: Đại lý có `credit_limit = 500,000,000` VNĐ, `current_balance = 480,000,000` VNĐ, `credit_status = 'ACTIVE'`.
  * **When**: Kế toán trưởng tạo bút toán điều chỉnh `amountDelta = +30,000,000` cho hóa đơn của đại lý này (phát hiện thiếu sót ghi nhận từ kỳ trước đã đóng).
  * **Then**: Dư nợ cập nhật thành `510,000,000` VNĐ, hệ thống tự động chuyển `credit_status = 'CREDIT_HOLD'` — dùng đúng cổng kiểm tra tín dụng như khi lập hóa đơn/phiếu thu.

* **Scenario: Từ chối tạo bút toán điều chỉnh cho chứng từ thuộc kỳ còn mở**
  * **Given**: Hóa đơn `INV-202607-0012` thuộc kỳ `2026-07` đang `OPEN`.
  * **When**: Kế toán trưởng cố gắng tạo bút toán điều chỉnh cho hóa đơn này.
  * **Then**: Hệ thống từ chối yêu cầu, trả về lỗi `ORIGINAL_PERIOD_NOT_CLOSED` (HTTP 422).

* **Scenario: Bút toán điều chỉnh không ảnh hưởng luồng tiêu hủy (Disposal)**
  * **Given**: Danh sách chờ tiêu hủy (`GET /api/v1/disposals/pending`) chỉ truy vấn `adjustments` với `type = DISPOSAL`.
  * **When**: Một bút toán điều chỉnh `type = CORRECTION_VOUCHER` (`warehouse_id`/`product_id` = NULL) được tạo.
  * **Then**: Danh sách chờ tiêu hủy không bị ảnh hưởng — bút toán mới không xuất hiện và không gây lỗi ở luồng tiêu hủy.
