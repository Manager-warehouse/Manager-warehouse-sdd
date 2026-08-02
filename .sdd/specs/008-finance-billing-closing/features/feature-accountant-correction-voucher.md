# Feature: Bút toán Điều chỉnh Chứng từ Tài chính Đã Lưu (Correction Voucher) (US-WMS-29)

## 1. Context and Goal

`invoices`, `payment_receipts`, `supplier_invoices`, `supplier_payments`, và `debit_notes` (khi đã `APPLIED`) không có endpoint `UPDATE`/`DELETE` ở bất kỳ thời điểm nào — kể cả khi kỳ kế toán chứa chúng vẫn còn `OPEN`. Một sai sót gõ nhầm số tiền bị phát hiện ngay sau khi lưu, trong cùng kỳ đang mở, trước đây không có cơ chế nào xử lý trong hệ thống (phải xử lý ngoài luồng qua Zalo/Email, không có audit trail, không phản ánh đúng số dư). Khi kỳ đã `CLOSED` (US-WMS-17), việc khóa cứng càng rõ ràng hơn — không ai được sửa/xóa trực tiếp chứng từ thuộc kỳ đã đóng.

Tính năng này lấp cả hai khoảng trống bằng một cơ chế duy nhất: kích hoạt giá trị `type = 'CORRECTION_VOUCHER'` vốn đã tồn tại sẵn trong `adjustments.type` từ Spec 006 nhưng chưa từng được dùng cho chứng từ tài chính thuần túy (không có hàng hóa liên quan). Khi Kế toán trưởng phát hiện một `invoices`, `payment_receipts`, `supplier_invoices`, `supplier_payments`, hoặc `debit_notes` đã tồn tại bị ghi sai — **bất kể kỳ kế toán của chứng từ gốc đang `OPEN` hay đã `CLOSED`** — họ tạo trực tiếp một bản ghi `adjustments` loại `CORRECTION_VOUCHER` với `document_date` thuộc kỳ đang `OPEN`, tham chiếu ngược tới chứng từ gốc qua `reference_type`/`reference_id` (cột đã có sẵn, dùng chung với `STOCK_TAKE`/`TRANSFER_DISCREPANCY`/`DISPOSAL`/`RETURN_TO_VENDOR`). Chứng từ gốc không bao giờ bị `UPDATE`/`DELETE`, dù kỳ của nó mở hay đóng — chỉ số dư hiện tại (`dealers.current_balance` / `suppliers.current_balance`) thay đổi, cùng cơ chế mà `payment_receipts` và `credit_notes` đã dùng để điều chỉnh số dư mà không sửa hóa đơn gốc. Dùng một cơ chế duy nhất cho cả hai trường hợp (kỳ mở và kỳ đóng) tránh việc phải xây hai luồng sửa lỗi song song với hai bộ quy tắc audit khác nhau.


## 2. Actors

* **Kế toán trưởng (`ACCOUNTANT_MANAGER`)**: Người duy nhất có thẩm quyền tạo bút toán điều chỉnh — cùng vai trò đã sở hữu quyền khóa kỳ kế toán (US-WMS-17), nên không cần thêm một cấp duyệt riêng cho việc sửa sai sót của chính kỳ mà họ quản lý.

## 3. Functional Requirements (EARS)

* **Ubiquitous:**
  * Hệ thống luôn giữ nguyên (không `UPDATE`/`DELETE`) chứng từ gốc (`invoices`, `payment_receipts`, `supplier_invoices`, `supplier_payments`, `debit_notes`) khi xử lý một bút toán điều chỉnh — chỉ `dealers.current_balance` hoặc `suppliers.current_balance` bị thay đổi.
* **Event-driven:**
  * **WHEN** Kế toán trưởng gửi yêu cầu `POST /api/v1/correction-vouchers` với `referenceType`, `referenceId`, `amountDelta`, `reason`, `documentDate`, hệ thống **SHALL**:
    * Xác định chứng từ gốc theo `referenceType` (`INVOICE` → `invoices`, `PAYMENT_RECEIPT` → `payment_receipts`, `SUPPLIER_INVOICE` → `supplier_invoices`, `SUPPLIER_PAYMENT` → `supplier_payments`, `DEBIT_NOTE` → `debit_notes`) và kiểm tra chứng từ tồn tại (`REFERENCE_DOCUMENT_NOT_FOUND` nếu không tồn tại).
    * **IF** `referenceType = 'DEBIT_NOTE'`: kiểm tra `debit_notes.status = 'APPLIED'` (trả về lỗi `DEBIT_NOTE_NOT_APPLIED` với HTTP 422 nếu còn `PENDING`) — một Debit Note chưa `APPLIED` chưa từng thay đổi `suppliers.current_balance` ([US-WMS-06 Feature 06](../../003-inbound-receipt-qc/features/feature-06-quarantine-rtv/spec.md)), nên phải sửa/hủy trực tiếp bản ghi `PENDING` đó thay vì tạo bút toán điều chỉnh ngược.
    * Không còn yêu cầu kỳ kế toán của chứng từ gốc phải `CLOSED` — Correction Voucher là cơ chế sửa lỗi duy nhất cho các chứng từ không có endpoint UPDATE/DELETE, áp dụng cho cả chứng từ gốc thuộc kỳ đang `OPEN` (ví dụ gõ nhầm số tiền, phát hiện ngay sau khi lưu) lẫn kỳ đã `CLOSED`.
    * Kiểm tra `documentDate` thuộc một kỳ kế toán có trạng thái `OPEN`, gán `accounting_period_id` tương ứng.
    * Tạo một bản ghi `adjustments` với `type = 'CORRECTION_VOUCHER'`, `warehouse_id = NULL`, `product_id = NULL`, `quantity_adjustment = NULL`, `amount_delta` = giá trị điều chỉnh (có dấu), `reference_type`/`reference_id` trỏ về chứng từ gốc, `reason`, `document_date`, `accounting_period_id`, `created_by` = actor, `approved_by` = actor, `approved_at` = thời điểm tạo (tạo và duyệt cùng một bước, vì actor đã là thẩm quyền cao nhất).
    * **IF** `referenceType IN ('INVOICE', 'PAYMENT_RECEIPT')`: `dealers.current_balance = current_balance + amount_delta`, sau đó áp dụng lại đúng logic kiểm tra tín dụng đang dùng cho `payment_receipts` (US-WMS-15): nếu `current_balance > credit_limit` → `credit_status = 'CREDIT_HOLD'`; nếu `current_balance < credit_limit * CREDIT_UNLOCK_BUFFER_PCT` → `credit_status = 'ACTIVE'`.
    * **IF** `referenceType IN ('SUPPLIER_INVOICE', 'SUPPLIER_PAYMENT', 'DEBIT_NOTE')`: `suppliers.current_balance = current_balance + amount_delta`.
    * Tạo bản ghi audit log `CORRECTION_VOUCHER_CREATE` (bao gồm chứng từ gốc, kỳ gốc, kỳ hạch toán mới, số tiền, lý do, số dư trước/sau).
* **State-driven:**
  * **WHILE** kỳ kế toán tương ứng với `documentDate` đã `CLOSED`, hệ thống **SHALL** từ chối tạo bút toán điều chỉnh (trả về lỗi `PERIOD_CLOSED` với HTTP 422) — bút toán điều chỉnh luôn phải nằm ở kỳ đang mở.

## 4. Data Model

Không tạo bảng mới. Mở rộng bảng `adjustments` đã có (Spec 006):

* Đổi ràng buộc `warehouse_id NOT NULL` và `product_id NOT NULL` thành ràng buộc có điều kiện: `CHECK (type = 'CORRECTION_VOUCHER' OR (warehouse_id IS NOT NULL AND product_id IS NOT NULL))` — giữ nguyên đảm bảo bắt buộc cho 4 loại điều chỉnh tồn kho hiện có (`STOCK_TAKE`, `TRANSFER_DISCREPANCY`, `DISPOSAL`, `RETURN_TO_VENDOR`), chỉ nới lỏng cho `CORRECTION_VOUCHER`.
* Tương tự với `quantity_adjustment NOT NULL` → `CHECK (type = 'CORRECTION_VOUCHER' OR quantity_adjustment IS NOT NULL)`.
* Thêm cột mới `amount_delta` (DECIMAL(18,2), NULL) — chỉ dùng khi `type = 'CORRECTION_VOUCHER'`; có dấu, dương = tăng công nợ, âm = giảm công nợ. Không tái sử dụng `quantity_adjustment` cho việc này vì đơn vị/ý nghĩa khác nhau (số lượng vs. tiền tệ).
* `reference_type`/`reference_id` (đã có sẵn, không có `CHECK` ràng buộc giá trị) mở rộng nhận thêm `'INVOICE'`, `'PAYMENT_RECEIPT'`, `'SUPPLIER_INVOICE'`, `'SUPPLIER_PAYMENT'`, `'DEBIT_NOTE'` bên cạnh các giá trị hiện có.
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

> `original_period_id`/`original_period_name` trong response luôn phản ánh kỳ kế toán thực tế của chứng từ gốc — có thể trùng với `accounting_period_id` của chính bút toán điều chỉnh khi chứng từ gốc và điều chỉnh cùng nằm trong kỳ đang `OPEN` (sửa lỗi cùng ngày), hoặc khác nhau khi chứng từ gốc thuộc một kỳ đã `CLOSED` trước đó.

### 5.2 Danh sách bút toán điều chỉnh
* **Protocol & Path**: `GET /api/v1/correction-vouchers`
* **Query Params**: `referenceType` (bao gồm `DEBIT_NOTE`), `dealerId`, `supplierId` (Optional)
* **Actor**: `ACCOUNTANT`, `ACCOUNTANT_MANAGER` (đọc để đối chiếu; chỉ `ACCOUNTANT_MANAGER` được tạo)

## 6. Error Handling

| Error Code | HTTP Status | Điều kiện kích hoạt |
|------------|-------------|---------------------|
| `PERIOD_CLOSED` | 422 Unprocessable Entity | `documentDate` của bút toán điều chỉnh thuộc kỳ kế toán đã `CLOSED` |
| `REFERENCE_DOCUMENT_NOT_FOUND` | 404 Not Found | `referenceId` không tồn tại với `referenceType` tương ứng |
| `DEBIT_NOTE_NOT_APPLIED` | 422 Unprocessable Entity | `referenceType = 'DEBIT_NOTE'` nhưng `debit_notes.status` vẫn là `PENDING` — chưa từng ảnh hưởng `suppliers.current_balance` nên không có gì để điều chỉnh ngược |

## 7. Acceptance Criteria

* **Scenario: Lập bút toán điều chỉnh cho hóa đơn thuộc kỳ đã chốt**
  * **Given**: Hóa đơn `INV-202606-0005` thuộc kỳ `2026-06` đã `CLOSED`, ghi nhầm `total_amount` cao hơn thực tế `2,000,000` VNĐ. Dư nợ đại lý hiện tại là `50,000,000` VNĐ.
  * **When**: Kế toán trưởng tạo bút toán điều chỉnh với `amountDelta = -2,000,000`, `documentDate` thuộc kỳ `2026-07` đang `OPEN`.
  * **Then**: Hệ thống tạo bản ghi `adjustments` (`type = CORRECTION_VOUCHER`, `warehouse_id`/`product_id` = NULL) đã `approved_by`/`approved_at` ngay lúc tạo; hóa đơn gốc `INV-202606-0005` không bị thay đổi bất kỳ trường nào; dư nợ đại lý giảm ngay còn `48,000,000` VNĐ; audit log `CORRECTION_VOUCHER_CREATE` được ghi kèm số dư trước/sau.

* **Scenario: Bút toán điều chỉnh đưa dư nợ vượt hạn mức tín dụng**
  * **Given**: Đại lý có `credit_limit = 500,000,000` VNĐ, `current_balance = 480,000,000` VNĐ, `credit_status = 'ACTIVE'`.
  * **When**: Kế toán trưởng tạo bút toán điều chỉnh `amountDelta = +30,000,000` cho hóa đơn của đại lý này (phát hiện thiếu sót ghi nhận từ kỳ trước đã đóng).
  * **Then**: Dư nợ cập nhật thành `510,000,000` VNĐ, hệ thống tự động chuyển `credit_status = 'CREDIT_HOLD'` — dùng đúng cổng kiểm tra tín dụng như khi lập hóa đơn/phiếu thu.

* **Scenario: Lập bút toán điều chỉnh cho hóa đơn gõ nhầm số tiền, phát hiện ngay trong kỳ đang mở**
  * **Given**: Hóa đơn `INV-202607-0012` thuộc kỳ `2026-07` đang `OPEN`, kế toán viên vừa lưu xong thì phát hiện gõ nhầm `total_amount` cao hơn thực tế `500,000` VNĐ. Không có endpoint sửa hóa đơn trực tiếp.
  * **When**: Kế toán trưởng tạo bút toán điều chỉnh với `referenceType = INVOICE`, `referenceId` trỏ tới `INV-202607-0012`, `amountDelta = -500,000`, `documentDate` cùng thuộc kỳ `2026-07` đang `OPEN`.
  * **Then**: Hệ thống chấp nhận yêu cầu (không có kiểm tra kỳ gốc phải `CLOSED`); tạo bản ghi `adjustments` (`type = CORRECTION_VOUCHER`) tham chiếu `INV-202607-0012`; hóa đơn gốc không bị sửa bất kỳ trường nào; dư nợ đại lý giảm đúng `500,000` VNĐ; audit log `CORRECTION_VOUCHER_CREATE` được ghi.

* **Scenario: Lập bút toán điều chỉnh cho Debit Note đã Applied bị tính sai số tiền**
  * **Given**: Debit Note `DN-202606-0003` đã `APPLIED` với `amount = 5,000,000` VNĐ (đã trừ vào `suppliers.current_balance`), sau đó phát hiện đơn giá QC-fail tính sai, số tiền đúng phải là `4,500,000` VNĐ.
  * **When**: Kế toán trưởng tạo bút toán điều chỉnh với `referenceType = DEBIT_NOTE`, `referenceId` trỏ tới `DN-202606-0003`, `amountDelta = +500,000` (khôi phục lại phần đã trừ thừa vào công nợ phải trả NCC).
  * **Then**: Hệ thống tạo bản ghi `adjustments` (`type = CORRECTION_VOUCHER`, `reference_type = DEBIT_NOTE`); `suppliers.current_balance` tăng đúng `500,000` VNĐ; Debit Note gốc không bị sửa.

* **Scenario: Từ chối điều chỉnh Debit Note còn PENDING**
  * **Given**: Debit Note `DN-202607-0010` vừa được tạo từ RTV, `status = PENDING`, chưa được ACCOUNTANT áp dụng, chưa từng ảnh hưởng `suppliers.current_balance`.
  * **When**: Kế toán trưởng cố gắng tạo bút toán điều chỉnh cho Debit Note này.
  * **Then**: Hệ thống từ chối yêu cầu, trả về lỗi `DEBIT_NOTE_NOT_APPLIED` (HTTP 422) — Debit Note `PENDING` phải được sửa/hủy trực tiếp, không đi qua Correction Voucher.

* **Scenario: Bút toán điều chỉnh không ảnh hưởng luồng tiêu hủy (Disposal)**
  * **Given**: Danh sách chờ tiêu hủy (`GET /api/v1/disposals/pending`) chỉ truy vấn `adjustments` với `type = DISPOSAL`.
  * **When**: Một bút toán điều chỉnh `type = CORRECTION_VOUCHER` (`warehouse_id`/`product_id` = NULL) được tạo.
  * **Then**: Danh sách chờ tiêu hủy không bị ảnh hưởng — bút toán mới không xuất hiện và không gây lỗi ở luồng tiêu hủy.
