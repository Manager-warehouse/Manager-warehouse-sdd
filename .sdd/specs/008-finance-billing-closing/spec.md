# Feature Specification: Tài chính & Công nợ Đại lý (Finance & Credit)

**Spec ID**: 008-finance-billing-closing
**Created**: 2026-05-30
**Updated**: 2026-06-17
**Status**: Approved
**Features**: US-WMS-10, US-WMS-15, US-WMS-16, US-WMS-17, US-WMS-18

---

## 1. Context and Goal

Sau khi đơn hàng được giao thành công (trạng thái `DELIVERED`), hệ thống tạo một thông báo lập hóa đơn (`billing_notifications`). Kế toán viên lập hóa đơn bán hàng dựa trên thông tin này, ghi nhận và cộng dồn công nợ Đại lý. Hệ thống tự động kiểm tra hạn mức tín dụng (Credit Limit) và khóa/chặn việc tạo đơn mới nếu vi phạm. Cuối kỳ kế toán, Kế toán trưởng thực hiện chốt sổ tháng, khóa kỳ kế toán để tránh sửa đổi dữ liệu quá khứ.

### Features List
* [US-WMS-10: Lập Hóa đơn Bán hàng & Ghi nhận Công nợ](./features/feature-accountant-customer-invoicing.md)
* [US-WMS-15: Ghi nhận Thanh toán & Quản lý Công nợ](./features/feature-accountant-payment-collection.md)
* [US-WMS-16: Báo cáo Công nợ Phân kỳ (Aging Report)](./features/feature-accountant-credit-aging-report.md)
* [US-WMS-17: Chốt sổ Kế toán & Khóa Kỳ](./features/feature-accountant-period-closing.md)
* [US-WMS-18: Quét hóa đơn chuyển khoản bằng OCR](./features/feature-ocr-payment-receipt-scanning.md)

## 2. Actors

| Actor | Vai trò hệ thống | Nghiệp vụ liên quan |
|-------|------------------|---------------------|
| Kế toán viên | `ACCOUNTANT` | Maker: Tiếp nhận thông báo lập hóa đơn, lập Hóa đơn (Invoice), tạo Phiếu thu (Payment Receipt) cấn trừ công nợ đại lý. |
| Kế toán trưởng | `ACCOUNTANT_MANAGER` | Checker: Phê duyệt Credit Limit cho Đại lý, xem báo cáo phân kỳ công nợ (Aging Report), thực hiện chốt sổ tháng và khóa kỳ kế toán. |
| Hệ thống (Daily Job) | System | Quét định kỳ cuối ngày các hóa đơn quá hạn quá số ngày quy định để tự động chuyển trạng thái đại lý sang `CREDIT_HOLD`. |

## 3. Functional Requirements (EARS)
*Vui lòng xem chi tiết yêu cầu chức năng EARS tại các tài liệu đặc tả tính năng:*
* [EARS - Customer Invoicing](./features/feature-accountant-customer-invoicing.md#3-functional-requirements-ears)
* [EARS - Payment Collection](./features/feature-accountant-payment-collection.md#3-functional-requirements-ears)
* [EARS - Credit Aging Report](./features/feature-accountant-credit-aging-report.md#3-functional-requirements-ears)
* [EARS - Period Closing](./features/feature-accountant-period-closing.md#3-functional-requirements-ears)
* [EARS - OCR Payment Receipt Scanning](./features/feature-ocr-payment-receipt-scanning.md#3-functional-requirements-ears)

## 4. Non-functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-001 | Invoice creation from DO notification | ≤ 1s |
| NFR-002 | Payment recording + balance update | ≤ 1s |
| NFR-003 | Aging report generation | ≤ 3s for 50+ dealers |
| NFR-004 | Monthly closing validation & execution | ≤ 10s |
| NFR-005 | Daily credit check batch job | Run off-peak, duration ≤ 30s |

## 5. Data Model (Đồng bộ với DB Migration V1 & V17)

### dealers (Bổ sung thông tin quản lý công nợ)
* `credit_limit` (DECIMAL(18,2), NOT NULL, DEFAULT 0) - Hạn mức nợ tối đa
* `current_balance` (DECIMAL(18,2), NOT NULL, DEFAULT 0) - Dư nợ hiện tại (dương = nợ hệ thống)
* `credit_status` (VARCHAR(20), NOT NULL, DEFAULT 'ACTIVE', CHECK IN ('ACTIVE', 'CREDIT_HOLD')) - Trạng thái tín dụng đại lý

### invoices (Hóa đơn bán hàng)
* `id` (BIGSERIAL, PK)
* `invoice_number` (VARCHAR(50), UNIQUE, NOT NULL) - Số hóa đơn tự sinh
* `do_id` (BIGINT, FK→delivery_orders, NOT NULL) - Liên kết đơn xuất kho giao thành công
* `dealer_id` (BIGINT, FK→dealers, NOT NULL) - Đại lý mua hàng
* `total_amount` (DECIMAL(18,2), NOT NULL) - Tổng tiền hóa đơn (DO items × đơn giá hợp lệ)
* `issue_date` (DATE, NOT NULL) - Ngày phát hành hóa đơn
* `due_date` (DATE, NOT NULL) - Hạn thanh toán (issue_date + dealer.payment_term_days)
* `status` (VARCHAR(20), DEFAULT 'UNPAID', CHECK IN ('UNPAID','PARTIALLY_PAID','PAID'), NOT NULL)
* `created_by` (BIGINT, FK→users, NOT NULL)
* `document_date` (DATE, NOT NULL) - Ngày hạch toán chứng từ
* `accounting_period_id` (BIGINT, FK→accounting_periods, NOT NULL) - Kỳ kế toán phát sinh
* `created_at` (TIMESTAMPTZ, DEFAULT NOW())
* `updated_at` (TIMESTAMPTZ, DEFAULT NOW())

### payment_receipts (Phiếu thu ghi nhận thanh toán)
* `id` (BIGSERIAL, PK)
* `payment_number` (VARCHAR(50), UNIQUE, NOT NULL) - Số phiếu thu tự sinh
* `dealer_id` (BIGINT, FK→dealers, NOT NULL) - Đại lý nộp tiền
* `invoice_id` (BIGINT, FK→invoices, NOT NULL) - Hóa đơn được cấn trừ
* `amount` (DECIMAL(18,2), NOT NULL) - Số tiền thanh toán thực tế
* `payment_date` (DATE, NOT NULL) - Ngày nộp tiền
* `payment_method` (VARCHAR(30), CHECK IN ('BANK_TRANSFER','CASH'), NOT NULL)
* `created_by` (BIGINT, FK→users, NOT NULL)
* `document_date` (DATE, NOT NULL) - Ngày chứng từ
* `accounting_period_id` (BIGINT, FK→accounting_periods, NOT NULL) - Kỳ kế toán phát sinh
* `notes` (TEXT)
* `created_at` (TIMESTAMPTZ, DEFAULT NOW())

### accounting_periods (Kỳ kế toán)
* `id` (BIGSERIAL, PK)
* `period_name` (VARCHAR(20), UNIQUE, NOT NULL) -- Định dạng YYYY-MM
* `start_date` (DATE, NOT NULL)
* `end_date` (DATE, NOT NULL)
* `status` (VARCHAR(10), DEFAULT 'OPEN', CHECK IN ('OPEN','CLOSED'), NOT NULL)
* `closed_by` (BIGINT, FK→users)
* `closed_at` (TIMESTAMPTZ)
* `notes` (TEXT)
* `created_at` (TIMESTAMPTZ, DEFAULT NOW())

### billing_notifications (Thông báo lập hóa đơn - Migration V17)
* `id` (BIGSERIAL, PK)
* `do_id` (BIGINT, FK→delivery_orders, NOT NULL)
* `do_number` (VARCHAR(50), NOT NULL)
* `dealer_id` (BIGINT, FK→dealers, NOT NULL)
* `dealer_name` (VARCHAR(255), NOT NULL)
* `warehouse_id` (BIGINT, FK→warehouses, NOT NULL)
* `delivered_at` (TIMESTAMPTZ, NOT NULL)
* `total_amount_estimate` (DECIMAL(18,2), NOT NULL)
* `invoice_status` (VARCHAR(30), DEFAULT 'NOT_INVOICED', CHECK IN ('NOT_INVOICED', 'INVOICED'), NOT NULL)
* `status` (VARCHAR(20), DEFAULT 'ACTIVE', CHECK IN ('ACTIVE', 'READ', 'ARCHIVED'), NOT NULL)
* `recipient_role` (VARCHAR(50), DEFAULT 'ACCOUNTANT', NOT NULL)
* `read_at` (TIMESTAMPTZ)
* `created_at` (TIMESTAMPTZ, DEFAULT NOW())

## 6. API Spec (Đồng bộ Frontend - Backend)

Các endpoints được cấu hình thông qua base API prefix `/api/v1`:

| Method | Endpoint | Actor | Mô tả |
|--------|----------|-------|-------|
| **GET** | `/api/v1/billing-notifications` | `ACCOUNTANT` | Lấy danh sách thông báo lập hóa đơn cần xử lý |
| **PUT** | `/api/v1/billing-notifications/{id}/read` | `ACCOUNTANT` | Đánh dấu đã đọc thông báo |
| **POST** | `/api/v1/invoices` | `ACCOUNTANT` | Lập hóa đơn từ đơn hàng đã giao (DO) |
| **GET** | `/api/v1/invoices` | `ACCOUNTANT`, `ACCOUNTANT_MANAGER` | Danh sách hóa đơn (lọc theo dealer, status, kỳ) |
| **GET** | `/api/v1/invoices/{id}` | `ACCOUNTANT`, `ACCOUNTANT_MANAGER` | Chi tiết hóa đơn |
| **POST** | `/api/v1/payment-receipts` | `ACCOUNTANT` | Ghi nhận phiếu thu cấn trừ công nợ đại lý |
| **POST** | `/api/v1/payment-receipts/ocr` | `ACCOUNTANT` | Upload và phân tích hóa đơn chuyển khoản qua OCR |
| **GET** | `/api/v1/payment-receipts` | `ACCOUNTANT`, `ACCOUNTANT_MANAGER` | Danh sách phiếu thu công nợ |
| **GET** | `/api/v1/credit/aging-report` | `ACCOUNTANT`, `ACCOUNTANT_MANAGER` | Báo cáo công nợ phân kỳ của đại lý (Kế toán viên xem để đốc thúc thu hồi công nợ; Kế toán trưởng dùng cho quyết định Credit Limit) |
| **GET** | `/api/v1/accounting-periods` | `ACCOUNTANT`, `ACCOUNTANT_MANAGER` | Danh sách kỳ kế toán |
| **PUT** | `/api/v1/accounting-periods/{id}/close` | `ACCOUNTANT_MANAGER` | Thực hiện khóa kỳ kế toán |

## 7. Error Handling

| Error Code | HTTP Status | Điều kiện kích hoạt |
|------------|-------------|---------------------|
| `CREDIT_HOLD` | 422 Unprocessable Entity | Đại lý đang bị khóa tín dụng, không cho phép tạo DO mới |
| `PERIOD_CLOSED` | 422 Unprocessable Entity | Kỳ kế toán của ngày chứng từ đã bị đóng, không cho phép ghi nhận |
| `INVOICE_ALREADY_PAID` | 409 Conflict | Thực hiện thanh toán cho hóa đơn đã có trạng thái `PAID` |
| `OVERPAYMENT_EXCEEDS_INVOICE` | 422 Unprocessable Entity | Số tiền thanh toán cấn trừ lớn hơn dư nợ còn lại của hóa đơn |
| `DELIVERY_ORDER_NOT_DELIVERED` | 400 Bad Request | Lập hóa đơn từ DO chưa chuyển sang trạng thái `DELIVERED` |

## 8. Acceptance Criteria
*Vui lòng xem chi tiết kịch bản kiểm thử tại các tài liệu đặc tả tính năng:*
* [Acceptance - Customer Invoicing](./features/feature-accountant-customer-invoicing.md#5-acceptance-criteria)
* [Acceptance - Payment Collection](./features/feature-accountant-payment-collection.md#5-acceptance-criteria)
* [Acceptance - Credit Aging Report](./features/feature-accountant-credit-aging-report.md#5-acceptance-criteria)
* [Acceptance - Period Closing](./features/feature-accountant-period-closing.md#5-acceptance-criteria)
* [Acceptance - OCR Payment Receipt Scanning](./features/feature-ocr-payment-receipt-scanning.md#5-acceptance-criteria)

## 9. Out of Scope

- Hạch toán kép General Ledger (GL) kế toán tổng hợp.
- Kê khai và tính toán thuế VAT chi tiết (chỉ lưu tổng tiền hóa đơn).
- Đối soát tự động với tài khoản ngân hàng (Bank Statement).
- Tự động trừ tiền qua các cổng thanh toán online.
- Tạo `credit_notes` (Phiếu ghi giảm công nợ cho hàng hoàn trả): thuộc luồng US-WMS-24 tại Spec 009 (`feature-storekeeper-customer-returns.md`). Spec 008 chỉ áp dụng khoản trừ `current_balance` mà `credit_notes` đã tạo, không sở hữu bước tạo Credit Note.
- Giao diện thủ công lập lại hóa đơn cho các DO đã `COMPLETED` nhưng thiếu hóa đơn (do dữ liệu lịch sử/di trú hoặc can thiệp thủ công vào DB): hóa đơn luôn được hệ thống tự động tạo ngay khi giao hàng thành công (US-WMS-10), tình huống này không phát sinh trong vận hành bình thường. Endpoint `POST /api/v1/invoices` vẫn tồn tại để hỗ trợ khôi phục dữ liệu ngoại lệ nhưng không có giao diện thao tác.
- Chỉnh sửa (edit) hóa đơn sau khi phát hành: hóa đơn là chứng từ bất biến sau khi tạo, chỉ `status` được cập nhật gián tiếp qua Phiếu thu. Đối với hàng hoàn trả, điều chỉnh công nợ đại lý đi qua Credit Note (Spec 009) thay vì sửa trực tiếp `total_amount`/`due_date` của `invoices` đã tồn tại; Credit Note chỉ áp dụng cho luồng hàng hoàn trả (yêu cầu Return Receipt đã `APPROVED`) và không phải là cơ chế chỉnh sửa hóa đơn nói chung.
- Xử lý tranh chấp/khiếu nại liên quan tới bằng chứng giao hàng (POD) khi Kế toán viên đối chiếu hóa đơn tự động qua danh sách hóa đơn: hệ thống không cung cấp cơ chế hủy hóa đơn, đánh dấu tranh chấp, hay điều chỉnh tài chính trong ứng dụng cho tình huống này. Việc xử lý (nếu có) diễn ra ngoài hệ thống, tương tự cách phối hợp qua Zalo/Email đã áp dụng ở luồng nhập hàng.
- Tạo `adjustments` loại `CORRECTION_VOUCHER` (ACC-05, business.md) để điều chỉnh sai sót tài chính (hóa đơn/phiếu thu) phát hiện sau khi kỳ kế toán đã `CLOSED`: bảng `adjustments` hiện chỉ mô hình hóa điều chỉnh số lượng tồn kho (`warehouse_id`/`product_id` bắt buộc), không phù hợp để biểu diễn sai sót tài chính. Tính năng này chưa được xây dựng trong Sprint 1; sai sót tài chính phát hiện sau khi đóng kỳ cần được xử lý thủ công ngoài hệ thống.
