# Feature Specification: Tài chính & Công nợ Đại lý (Finance & Credit)

**Spec ID**: 008-finance-billing-closing
**Created**: 2026-05-30
**Updated**: 2026-07-23
**Status**: Approved
**Features**: US-WMS-10, US-WMS-15, US-WMS-16, US-WMS-17, US-WMS-18, US-WMS-28, US-WMS-29

---

## 1. Context and Goal

Sau khi đơn hàng được giao thành công (trạng thái `DELIVERED`), hệ thống tạo một thông báo lập hóa đơn (`billing_notifications`). Kế toán viên lập hóa đơn bán hàng dựa trên thông tin này, ghi nhận và cộng dồn công nợ Đại lý. Hệ thống tự động kiểm tra hạn mức tín dụng (Credit Limit) và khóa/chặn việc tạo đơn mới nếu vi phạm. Bên cạnh luồng xuất hàng, hệ thống hỗ trợ ghi nhận Hóa đơn mua hàng & Công nợ Nhà cung cấp (Supplier Invoicing & AP) khi nhập hàng thành công. Cuối kỳ kế toán, Kế toán trưởng thực hiện chốt sổ tháng, khóa kỳ kế toán để tránh sửa đổi dữ liệu quá khứ.

### Features List
* [US-WMS-10: Lập Hóa đơn Bán hàng & Ghi nhận Công nợ](./features/feature-accountant-customer-invoicing.md)
* [US-WMS-15: Ghi nhận Thanh toán & Quản lý Công nợ](./features/feature-accountant-payment-collection.md)
* [US-WMS-16: Báo cáo Công nợ Phân kỳ (Aging Report)](./features/feature-accountant-credit-aging-report.md)
* [US-WMS-17: Chốt sổ Kế toán & Khóa Kỳ](./features/feature-accountant-period-closing.md)
* [US-WMS-18: Quét hóa đơn chuyển khoản bằng OCR](./features/feature-ocr-payment-receipt-scanning.md)
* [US-WMS-28: Lập Hóa đơn Mua hàng & Ghi nhận Công nợ Nhà cung cấp](./features/feature-accountant-supplier-invoicing.md)
* [US-WMS-29: Bút toán Điều chỉnh Sai sót Kỳ đã Chốt (Correction Voucher)](./features/feature-accountant-correction-voucher.md)

## Clarifications

### Session 2026-07-23
- Q: Quy định ghi nhận hóa đơn kế toán khi nhập hàng (Hóa đơn Mua hàng / Công nợ NCC - AP) như thế nào trong Spec 008? → A: Bổ sung đặc tả tính năng ghi nhận Hóa đơn Mua hàng & Công nợ Nhà cung cấp (Supplier Invoicing & AP) trực tiếp vào Spec 008.
- Q: Quy định tách 2 chế độ nhận dạng OCR chứng từ ngân hàng cho Đại lý (Thu) và Nhà cung cấp (Chi) như thế nào? → A: Tách riêng 2 endpoint OCR: POST /api/v1/payment-receipts/ocr cho Phiếu thu Đại lý và POST /api/v1/supplier-payments/ocr cho Ủy nhiệm chi thanh toán NCC.
- Q: Quy chuẩn tổ chức kiến trúc giao diện UX/UI và phân nhóm menu Sidebar cho phân hệ Tài chính (Spec 008) như thế nào để tránh rối và chồng chéo? → A: Phân nhóm Sidebar thành 4 sub-module rõ ràng (Quản lý Bảng giá, Phải thu AR, Phải trả AP, Kỳ kế toán & Báo cáo), đồng thời chuẩn hóa giao diện Unified Tab View đồng nhất cho cả Phải thu (AR) và Phải trả (AP).
- Q: Quy định hợp nhất các màn hình Phải thu Đại lý (AR) trên Frontend (`DealerDebtInvoice.jsx` và `Payments.jsx`) như thế nào? → A: Gộp thành một trang Unified AR View duy nhất tại đường dẫn `/finance/invoices` với 3 Tab (`Thông báo HĐ Bán`, `Hóa đơn Bán (SINV)`, `Phiếu thu AR & Quét OCR`). Nút menu "Thu nợ Đại lý (AR)" trỏ về `/finance/invoices?tab=payments`.
- Q: Quy định vị trí điều hướng cho tính năng "Chốt sổ & Khóa Kỳ Kế toán" và "Báo cáo Phân kỳ Công nợ (Aging Report)" trên Menu Sidebar như thế nào? → A: Đặt trực tiếp trong sub-group "Kế toán tổng hợp & Khóa kỳ" thuộc khối menu "Tài chính & Bảng giá" trên Sidebar (`/finance/periods` và `/reports/credit-aging`) cho vai trò ACCOUNTANT_MANAGER và ADMIN.

### Session 2026-07-24
- Q: US-WMS-17 (period-closing) yêu cầu hệ thống tạo bản ghi `adjustments` với `type = 'CORRECTION_VOUCHER'` để xử lý sai sót phát hiện sau khi kỳ đã `CLOSED`, nhưng mục 9 (Out of Scope) lại nói rõ tính năng này chưa được xây dựng vì bảng `adjustments` bắt buộc `warehouse_id`/`product_id` (không phù hợp cho sai sót tài chính thuần túy không có hàng hóa liên quan). Xử lý mâu thuẫn này như thế nào? → A: Bổ sung tính năng US-WMS-29 (Correction Voucher), nhưng KHÔNG tạo bảng mới — kích hoạt trực tiếp giá trị `CORRECTION_VOUCHER` đã có sẵn trong `adjustments.type`, chỉ nới lỏng `warehouse_id`/`product_id`/`quantity_adjustment` thành ràng buộc có điều kiện (`CHECK` theo `type`, vẫn bắt buộc cho 4 loại điều chỉnh tồn kho hiện có) và thêm cột `amount_delta`. Đã xác minh không ảnh hưởng `AdjustmentRepository` (không truy vấn theo `warehouse`/`product`) và không ảnh hưởng `DisposalService` (nơi duy nhất đọc `adjustment.getWarehouse()`/`getProduct()`, đã lọc cứng `type = DISPOSAL`). Một actor duy nhất (`ACCOUNTANT_MANAGER`, cùng người khóa kỳ), một bước tạo duy nhất — không có vòng duyệt riêng, khớp với cách mọi hành động AR/AP khác trong Spec 008 vận hành (một actor, hiệu lực ngay). `credit_notes` (US-WMS-24, Spec 009) không đổi và vẫn là cơ chế riêng cho hàng hoàn trả vật lý.

## 2. Actors

| Actor | Vai trò hệ thống | Nghiệp vụ liên quan |
|-------|------------------|---------------------|
| Kế toán viên | `ACCOUNTANT` | Maker: Tiếp nhận thông báo lập hóa đơn bán/mua hàng, lập Hóa đơn bán hàng (`invoices`), Hóa đơn mua hàng (`supplier_invoices`), tạo Phiếu thu cấn trừ công nợ đại lý và Phiếu chi thanh toán cho NCC. |
| Kế toán trưởng | `ACCOUNTANT_MANAGER` | Checker: Phê duyệt Credit Limit cho Đại lý, xem báo cáo phân kỳ công nợ, xem công nợ NCC, thực hiện chốt sổ tháng và khóa kỳ kế toán. |
| Hệ thống (Daily Job) | System | Quét định kỳ cuối ngày các hóa đơn quá hạn quá số ngày quy định để tự động chuyển trạng thái đại lý sang `CREDIT_HOLD`. |

## 3. UX/UI Navigation & Layout Standards

1. **Cấu trúc Sidebar (Tài chính & Bảng giá)**:
   - **Quản lý Bảng giá**: Bảng giá (`/finance/price-list`), Duyệt bảng giá (`/finance/price-approval`) (Spec 007)
   - **Phải thu Đại lý (AR)**: Gom toàn bộ luồng Hóa đơn Bán & Thu nợ đại lý vào màn hình Unified AR View (`/finance/invoices`) với 3 tab (`Thông báo HĐ Bán`, `Hóa đơn Bán (SINV)`, `Phiếu thu AR & Quét OCR`).
   - **Phải trả Nhà cung cấp (AP)**: Gom toàn bộ luồng Hóa đơn Mua & Chi trả NCC vào màn hình Unified AP View (`/finance/supplier-invoices`) với 3 tab (`Thông báo HĐ Mua`, `Hóa đơn Mua (SINV)`, `Phiếu chi AP & Quét OCR`).
   - **Kế toán tổng hợp & Báo cáo**: Quản lý Kỳ kế toán / Khóa sổ (`/finance/periods`) và Báo cáo tuổi nợ / Phân kỳ công nợ (`/reports/credit-aging`).

2. **Quy chuẩn Giao diện Tab nhất quán**:
   - Mọi trang quản lý chứng từ Tài chính (AR/AP) đều có chung layout: Header có nút khởi tạo nhanh chứng từ kèm OCR + 3 Tab theo dõi thứ tự theo nghiệp vụ (Thông báo chờ → Sổ Hóa đơn → Nhật ký Thanh toán).
   - Trang AR (`/finance/invoices`) hợp nhất `DealerDebtInvoice.jsx` và `Payments.jsx`, truy cập tab Phiếu thu qua query string `?tab=payments`.

3. **Vị trí giao diện Bút toán Điều chỉnh (Correction Voucher, US-WMS-29)**: không có trang/route riêng.
   - Nút "Điều chỉnh" xuất hiện trực tiếp trên từng dòng chứng từ tại các tab `Hóa đơn Bán (SINV)`, `Phiếu thu AR & Quét OCR` (`/finance/invoices`), `Hóa đơn Mua (SINV)`, và `Phiếu chi AP & Quét OCR` (`/finance/supplier-invoices`) — chỉ hiển thị cho `ACCOUNTANT_MANAGER` và chỉ trên dòng có `accounting_period_id` đã `CLOSED`.
   - Bấm nút mở modal nhỏ (không điều hướng trang) đã có sẵn `referenceType`/`referenceId` từ dòng vừa chọn; chỉ yêu cầu nhập `amountDelta`, `reason`, `documentDate`.
   - Lịch sử bút toán điều chỉnh (`GET /api/v1/correction-vouchers`) hiển thị dưới dạng mục con khi xem chi tiết một kỳ kế toán đã `CLOSED` tại `/finance/periods` — không thêm mục Sidebar mới.

## 4. Functional Requirements (EARS)
*Vui lòng xem chi tiết yêu cầu chức năng EARS tại các tài liệu đặc tả tính năng:*
* [EARS - Customer Invoicing](./features/feature-accountant-customer-invoicing.md#3-functional-requirements-ears)
* [EARS - Payment Collection](./features/feature-accountant-payment-collection.md#3-functional-requirements-ears)
* [EARS - Credit Aging Report](./features/feature-accountant-credit-aging-report.md#3-functional-requirements-ears)
* [EARS - Period Closing](./features/feature-accountant-period-closing.md#3-functional-requirements-ears)
* [EARS - OCR Payment Receipt Scanning](./features/feature-ocr-payment-receipt-scanning.md#3-functional-requirements-ears)
* [EARS - Supplier Invoicing & AP](./features/feature-accountant-supplier-invoicing.md#3-functional-requirements-ears)
* [EARS - Correction Voucher](./features/feature-accountant-correction-voucher.md#3-functional-requirements-ears)

## 5. Non-functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-001 | Invoice creation from DO notification | ≤ 1s |
| NFR-002 | Payment recording + balance update | ≤ 1s |
| NFR-003 | Aging report generation | ≤ 3s for 50+ dealers |
| NFR-004 | Monthly closing validation & execution | ≤ 10s |
| NFR-005 | Daily credit check batch job | Run off-peak, duration ≤ 30s |

## 6. Data Model (Đồng bộ với DB Migration V1 & V17)

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

### supplier_invoices (Hóa đơn mua hàng từ Nhà cung cấp)
* `id` (BIGSERIAL, PK)
* `invoice_number` (VARCHAR(50), UNIQUE, NOT NULL) - Số hóa đơn mua hàng nội bộ
* `supplier_invoice_number` (VARCHAR(100), NOT NULL) - Số hóa đơn gốc từ NCC
* `receipt_id` (BIGINT, FK→receipts, NOT NULL) - Liên kết phiếu nhập kho `APPROVED` (Spec 003 không có trạng thái `COMPLETED` riêng — `APPROVED` là trạng thái cuối sau khi Trưởng kho duyệt, mở khóa putaway)
* `supplier_id` (BIGINT, FK→suppliers, NOT NULL) - Nhà cung cấp
* `total_amount` (DECIMAL(18,2), NOT NULL) - Tổng tiền hóa đơn mua hàng
* `issue_date` (DATE, NOT NULL) - Ngày phát hành hóa đơn
* `due_date` (DATE, NOT NULL) - Hạn thanh toán
* `status` (VARCHAR(20), DEFAULT 'UNPAID', CHECK IN ('UNPAID','PARTIALLY_PAID','PAID'), NOT NULL)
* `created_by` (BIGINT, FK→users, NOT NULL)
* `document_date` (DATE, NOT NULL) - Ngày hạch toán chứng từ
* `accounting_period_id` (BIGINT, FK→accounting_periods, NOT NULL) - Kỳ kế toán phát sinh
* `created_at` (TIMESTAMPTZ, DEFAULT NOW())
* `updated_at` (TIMESTAMPTZ, DEFAULT NOW())

### supplier_payments (Phiếu chi thanh toán cho NCC)
* `id` (BIGSERIAL, PK)
* `payment_number` (VARCHAR(50), UNIQUE, NOT NULL) - Số phiếu chi tự sinh
* `supplier_id` (BIGINT, FK→suppliers, NOT NULL) - Nhà cung cấp
* `supplier_invoice_id` (BIGINT, FK→supplier_invoices, NOT NULL) - Hóa đơn mua hàng được cấn trừ
* `amount` (DECIMAL(18,2), NOT NULL) - Số tiền chi thanh toán
* `payment_date` (DATE, NOT NULL) - Ngày chi tiền
* `payment_method` (VARCHAR(30), CHECK IN ('BANK_TRANSFER','CASH'), NOT NULL)
* `created_by` (BIGINT, FK→users, NOT NULL)
* `document_date` (DATE, NOT NULL) - Ngày chứng từ
* `accounting_period_id` (BIGINT, FK→accounting_periods, NOT NULL) - Kỳ kế toán phát sinh
* `notes` (TEXT)
* `created_at` (TIMESTAMPTZ, DEFAULT NOW())

### adjustments — mở rộng cho Correction Voucher (Bút toán điều chỉnh sai sót kỳ đã chốt — chi tiết tại [feature-accountant-correction-voucher.md](./features/feature-accountant-correction-voucher.md#4-data-model))
Không tạo bảng mới. Bảng `adjustments` (Spec 006) được mở rộng để nhận `type = 'CORRECTION_VOUCHER'` cho chứng từ tài chính (không có hàng hóa liên quan):
* `warehouse_id`, `product_id`, `quantity_adjustment`: đổi từ `NOT NULL` sang `CHECK (type = 'CORRECTION_VOUCHER' OR <cột> IS NOT NULL)` — vẫn bắt buộc cho 4 loại điều chỉnh tồn kho hiện có, chỉ nới cho `CORRECTION_VOUCHER`.
* `amount_delta` (DECIMAL(18,2), NULL) — cột mới; chỉ dùng khi `type = 'CORRECTION_VOUCHER'`, có dấu, điều chỉnh `dealers.current_balance`/`suppliers.current_balance`.
* `reference_type`/`reference_id` (đã có sẵn) nhận thêm giá trị `'INVOICE'`, `'PAYMENT_RECEIPT'`, `'SUPPLIER_INVOICE'`, `'SUPPLIER_PAYMENT'` để trỏ về chứng từ tài chính gốc.
* `approved_by`/`approved_at` (đã có sẵn) được set ngay tại thời điểm tạo bởi `ACCOUNTANT_MANAGER` — không có bước duyệt riêng.

## 7. API Spec (Đồng bộ Frontend - Backend)

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
| **GET** | `/api/v1/supplier-billing-notifications` | `ACCOUNTANT` | Danh sách thông báo lập hóa đơn mua hàng từ phiếu nhập `APPROVED` |
| **POST** | `/api/v1/supplier-invoices` | `ACCOUNTANT` | Lập hóa đơn mua hàng từ phiếu nhập kho `APPROVED` |
| **GET** | `/api/v1/supplier-invoices` | `ACCOUNTANT`, `ACCOUNTANT_MANAGER` | Danh sách hóa đơn mua hàng từ Nhà cung cấp |
| **GET** | `/api/v1/supplier-invoices/{id}` | `ACCOUNTANT`, `ACCOUNTANT_MANAGER` | Chi tiết hóa đơn mua hàng |
| **POST** | `/api/v1/supplier-payments` | `ACCOUNTANT` | Ghi nhận phiếu chi thanh toán cho Nhà cung cấp |
| **POST** | `/api/v1/supplier-payments/ocr` | `ACCOUNTANT` | Upload và quét OCR Ủy nhiệm chi thanh toán cho Nhà cung cấp |
| **GET** | `/api/v1/supplier-payments` | `ACCOUNTANT`, `ACCOUNTANT_MANAGER` | Danh sách phiếu chi thanh toán NCC |
| **GET** | `/api/v1/credit/aging-report` | `ACCOUNTANT`, `ACCOUNTANT_MANAGER` | Báo cáo công nợ phân kỳ của đại lý |
| **GET** | `/api/v1/accounting-periods` | `ACCOUNTANT`, `ACCOUNTANT_MANAGER` | Danh sách kỳ kế toán |
| **PUT** | `/api/v1/accounting-periods/{id}/close` | `ACCOUNTANT_MANAGER` | Thực hiện khóa kỳ kế toán |
| **POST** | `/api/v1/correction-vouchers` | `ACCOUNTANT_MANAGER` | Lập bút toán điều chỉnh cho chứng từ thuộc kỳ đã `CLOSED`, cập nhật số dư công nợ ngay lập tức |
| **GET** | `/api/v1/correction-vouchers` | `ACCOUNTANT`, `ACCOUNTANT_MANAGER` | Danh sách bút toán điều chỉnh |

## 8. Error Handling

| Error Code | HTTP Status | Điều kiện kích hoạt |
|------------|-------------|---------------------|
| `CREDIT_HOLD` | 422 Unprocessable Entity | Đại lý đang bị khóa tín dụng, không cho phép tạo DO mới |
| `PERIOD_CLOSED` | 422 Unprocessable Entity | Kỳ kế toán của ngày chứng từ đã bị đóng, không cho phép ghi nhận |
| `INVOICE_ALREADY_PAID` | 409 Conflict | Thực hiện thanh toán cho hóa đơn đã có trạng thái `PAID` |
| `OVERPAYMENT_EXCEEDS_INVOICE` | 422 Unprocessable Entity | Số tiền thanh toán cấn trừ lớn hơn dư nợ còn lại của hóa đơn |
| `DELIVERY_ORDER_NOT_DELIVERED` | 400 Bad Request | Lập hóa đơn từ DO chưa chuyển sang trạng thái `DELIVERED` |
| `RECEIPT_NOT_APPROVED` | 400 Bad Request | Lập hóa đơn mua hàng từ phiếu nhập chưa ở trạng thái `APPROVED` |
| `SUPPLIER_INVOICE_ALREADY_EXISTS` | 409 Conflict | Lập hóa đơn mua hàng cho phiếu nhập đã có hóa đơn |
| `ORIGINAL_PERIOD_NOT_CLOSED` | 422 Unprocessable Entity | Lập bút toán điều chỉnh cho chứng từ thuộc kỳ kế toán chưa `CLOSED` |

## 9. Acceptance Criteria
*Vui lòng xem chi tiết kịch bản kiểm thử tại các tài liệu đặc tả tính năng:*
* [Acceptance - Customer Invoicing](./features/feature-accountant-customer-invoicing.md#5-acceptance-criteria)
* [Acceptance - Payment Collection](./features/feature-accountant-payment-collection.md#5-acceptance-criteria)
* [Acceptance - Credit Aging Report](./features/feature-accountant-credit-aging-report.md#5-acceptance-criteria)
* [Acceptance - Period Closing](./features/feature-accountant-period-closing.md#5-acceptance-criteria)
* [Acceptance - OCR Payment Receipt Scanning](./features/feature-ocr-payment-receipt-scanning.md#5-acceptance-criteria)
* [Acceptance - Supplier Invoicing & AP](./features/feature-accountant-supplier-invoicing.md#5-acceptance-criteria)
* [Acceptance - Correction Voucher](./features/feature-accountant-correction-voucher.md#7-acceptance-criteria)

## 10. Out of Scope

- Hạch toán kép General Ledger (GL) kế toán tổng hợp.
- Kê khai và tính toán thuế VAT chi tiết (chỉ lưu tổng tiền hóa đơn).
- Đối soát tự động với tài khoản ngân hàng (Bank Statement).
- Tự động trừ tiền qua các cổng thanh toán online.
- Tạo `credit_notes` (Phiếu ghi giảm công nợ cho hàng hoàn trả): thuộc luồng US-WMS-24 tại Spec 009 (`feature-storekeeper-customer-returns.md`). Spec 008 chỉ áp dụng khoản trừ `current_balance` mà `credit_notes` đã tạo, không sở hữu bước tạo Credit Note.
- Giao diện thủ công lập lại hóa đơn cho các DO đã `COMPLETED` nhưng thiếu hóa đơn (do dữ liệu lịch sử/di trú hoặc can thiệp thủ công vào DB): hóa đơn luôn được hệ thống tự động tạo ngay khi giao hàng thành công (US-WMS-10), tình huống này không phát sinh trong vận hành bình thường. Endpoint `POST /api/v1/invoices` vẫn tồn tại để hỗ trợ khôi phục dữ liệu ngoại lệ nhưng không có giao diện thao tác.
- Chỉnh sửa (edit) hóa đơn sau khi phát hành: hóa đơn là chứng từ bất biến sau khi tạo, chỉ `status` được cập nhật gián tiếp qua Phiếu thu. Đối với hàng hoàn trả, điều chỉnh công nợ đại lý đi qua Credit Note (Spec 009) thay vì sửa trực tiếp `total_amount`/`due_date` của `invoices` đã tồn tại; Credit Note chỉ áp dụng cho luồng hàng hoàn trả (yêu cầu Return Receipt đã `APPROVED`) và không phải là cơ chế chỉnh sửa hóa đơn nói chung.
- Xử lý tranh chấp/khiếu nại liên quan tới bằng chứng giao hàng (POD) khi Kế toán viên đối chiếu hóa đơn tự động qua danh sách hóa đơn: hệ thống không cung cấp cơ chế hủy hóa đơn, đánh dấu tranh chấp, hay điều chỉnh tài chính trong ứng dụng cho tình huống này. Việc xử lý (nếu có) diễn ra ngoài hệ thống, tương tự cách phối hợp qua Zalo/Email đã áp dụng ở luồng nhập hàng.
- Sửa/xóa trực tiếp chứng từ tài chính thuộc kỳ đã `CLOSED`: xem cơ chế thay thế (kích hoạt `adjustments.type = 'CORRECTION_VOUCHER'` đã có sẵn) tại US-WMS-29 (Session 2026-07-24, mục Clarifications).
