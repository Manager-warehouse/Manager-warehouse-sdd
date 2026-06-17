# Feature Specification: Tài chính & Công nợ Đại lý (Finance & Credit)

**Spec ID**: 008-finance-billing-closing
**Created**: 2026-05-30
**Status**: Draft
**Features**: US-WMS-10, US-WMS-15, US-WMS-16, US-WMS-17

---

## 1. Context and Goal

Sau khi đơn hàng được giao thành công bằng POD + OTP, delivery attempt chuyển `DELIVERED`; hệ thống tự động tạo invoice/công nợ và chuyển Delivery Order sang `COMPLETED`. Kế toán viên theo dõi danh sách invoice/công nợ đã tự động tạo để thu tiền, đối soát và không bỏ sót khoản phải thu. Hệ thống tự động kiểm tra hạn mức tín dụng (Credit Limit) và khóa/chặn đơn mới nếu vi phạm. Cuối tháng, Kế toán trưởng chốt sổ, khóa cứng kỳ quá khứ.

### Features List
* [US-WMS-10: Lập Hóa đơn Bán hàng & Ghi nhận Công nợ](./features/feature-accountant-customer-invoicing.md)
* [US-WMS-15: Ghi nhận Thanh toán & Quản lý Công nợ](./features/feature-accountant-payment-collection.md)
* [US-WMS-16: Báo cáo Công nợ Phân kỳ (Aging Report)](./features/feature-accountant-credit-aging-report.md)
* [US-WMS-17: Chốt sổ Kế toán & Khóa Kỳ](./features/feature-accountant-period-closing.md)

## 2. Actors

| Actor | Vai trò | Nghiệp vụ liên quan |
|-------|---------|---------------------|
| Kế toán viên | Maker | Theo dõi invoice/công nợ tự động tạo từ DO hoàn tất, tạo Phiếu thu (Payment Receipt) cấn trừ hóa đơn, cấn trừ công nợ |
| Kế toán trưởng | Checker | Phê duyệt Credit Limit cho Đại lý, xem Aging Report và báo cáo Lãi/Lỗ, thực hiện chốt sổ tháng |
| Hệ thống (Daily Job) | System | Chạy Job định kỳ quét hóa đơn quá hạn >30 ngày để tự động khóa công nợ (CREDIT_HOLD) |

## 3. Functional Requirements (EARS)
*Vui lòng xem chi tiết yêu cầu chức năng EARS tại các tài liệu đặc tả tính năng:*
* [EARS - Customer Invoicing](./features/feature-accountant-customer-invoicing.md#3-functional-requirements-ears)
* [EARS - Payment Collection](./features/feature-accountant-payment-collection.md#3-functional-requirements-ears)
* [EARS - Credit Aging Report](./features/feature-accountant-credit-aging-report.md#3-functional-requirements-ears)
* [EARS - Period Closing](./features/feature-accountant-period-closing.md#3-functional-requirements-ears)

## 4. Non-functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-001 | Auto invoice creation from completed DO | ≤ 1s |
| NFR-002 | Payment recording + balance update | ≤ 1s |
| NFR-003 | Aging report generation | ≤ 3s for 50+ dealers |
| NFR-004 | Monthly closing process | ≤ 30s |
| NFR-005 | Daily credit check batch job | ≤ 5s |

## 5. Data Model

### invoices
- `id` (BIGSERIAL, PK)
- `invoice_number` (VARCHAR(50), UNIQUE, NOT NULL)
- `do_id` (BIGINT, FK→delivery_orders, NOT NULL)
- `dealer_id` (BIGINT, FK→dealers, NOT NULL)
- `total_amount` (DECIMAL(18,2), NOT NULL)
- `issue_date` (DATE, NOT NULL)
- `due_date` (DATE, NOT NULL)
- `status` (VARCHAR(20), DEFAULT 'UNPAID', CHECK IN ('UNPAID','PARTIALLY_PAID','PAID'), NOT NULL)
- `created_by` (BIGINT, FK→users, NOT NULL)
- `document_date` (DATE, NOT NULL)
- `accounting_period_id` (BIGINT, FK→accounting_periods)
- `created_at` (TIMESTAMPTZ)
- `updated_at` (TIMESTAMPTZ)

### payment_receipts
- `id` (BIGSERIAL, PK)
- `payment_number` (VARCHAR(50), UNIQUE, NOT NULL)
- `dealer_id` (BIGINT, FK→dealers, NOT NULL)
- `invoice_id` (BIGINT, FK→invoices, NOT NULL)
- `amount` (DECIMAL(18,2), NOT NULL)
- `payment_date` (DATE, NOT NULL)
- `payment_method` (VARCHAR(30), CHECK IN ('BANK_TRANSFER','CASH'), NOT NULL)
- `created_by` (BIGINT, FK→users, NOT NULL)
- `document_date` (DATE, NOT NULL)
- `accounting_period_id` (BIGINT, FK→accounting_periods)
- `notes` (TEXT)
- `created_at` (TIMESTAMPTZ)

### accounting_periods
- `id` (BIGSERIAL, PK)
- `period_name` (VARCHAR(20), UNIQUE, NOT NULL) -- YYYY-MM format
- `start_date` (DATE, NOT NULL)
- `end_date` (DATE, NOT NULL)
- `status` (VARCHAR(10), DEFAULT 'OPEN', CHECK IN ('OPEN','CLOSED'), NOT NULL)
- `closed_by` (BIGINT, FK→users)
- `closed_at` (TIMESTAMPTZ)
- `notes` (TEXT)
- `created_at` (TIMESTAMPTZ)
- `CHECK(end_date >= start_date)`

## 6. API Spec
*Vui lòng xem chi tiết API endpoints tại các tài liệu đặc tả tính năng:*
* [APIs - Customer Invoicing](./features/feature-accountant-customer-invoicing.md#4-api-endpoints)
* [APIs - Payment Collection](./features/feature-accountant-payment-collection.md#4-api-endpoints)
* [APIs - Credit Aging Report](./features/feature-accountant-credit-aging-report.md#4-api-endpoints)
* [APIs - Period Closing](./features/feature-accountant-period-closing.md#4-api-endpoints)

## 7. Error Handling

| Error | HTTP | Condition |
|-------|------|-----------|
| CREDIT_HOLD | 422 | Dealer is CREDIT_HOLD, blocked |
| PERIOD_CLOSED | 422 | Cannot modify closed period |
| INVOICE_ALREADY_PAID | 409 | Payment on already paid invoice |
| OVERPAYMENT_EXCEEDS_INVOICE | 422 | Payment > invoice remaining balance |
| EXCESSIVE_CREDIT_CHANGE | 422 | Credit limit change exceeds policy |
| WAREHOUSE_SCOPE_FORBIDDEN | 403 | Accountant is not assigned to the requested warehouse/invoice candidate |

## 8. Acceptance Criteria
*Vui lòng xem chi tiết kịch bản kiểm thử tại các tài liệu đặc tả tính năng:*
* [Acceptance - Customer Invoicing](./features/feature-accountant-customer-invoicing.md#5-acceptance-criteria)
* [Acceptance - Payment Collection](./features/feature-accountant-payment-collection.md#5-acceptance-criteria)
* [Acceptance - Credit Aging Report](./features/feature-accountant-credit-aging-report.md#5-acceptance-criteria)
* [Acceptance - Period Closing](./features/feature-accountant-period-closing.md#5-acceptance-criteria)

## 9. Out of Scope

- Full General Ledger (GL) double-entry accounting
- VAT / Tax reporting
- Bank reconciliation
- Automated payment collection
- Credit scoring / credit rating models
