# Feature Specification: Hàng hoàn trả & Tiêu hủy (Returns & Disposal)

**Spec ID**: 009-returns-scrap-disposal
**Created**: 2026-05-30
**Status**: Draft
**Features**: US-WMS-24, US-WMS-04 (Disposal sub-flow)

---

## 1. Context and Goal

Đại lý có thể hoàn trả hàng (hàng lỗi, sai quy cách, tồn kho) và hàng trong Quarantine Zone cần được xử lý tiêu hủy khi không thể nhập lại kho hoặc trả NCC theo luồng inbound. Các quy trình này ảnh hưởng đến inventory và công nợ, cần kiểm soát chặt chẽ. RTV của hàng lỗi inbound được xử lý tại Spec 003.

### Features List
* [US-WMS-24: Xử lý Hàng hoàn trả từ Đại lý](./features/feature-storekeeper-customer-returns.md)
* [US-WMS-04 (Disposal Sub-flow): Tiêu hủy Hàng lỗi từ Quarantine](./features/feature-manager-scrap-disposal.md)

## 2. Actors

| Actor | Vai trò | Nghiệp vụ liên quan |
|-------|---------|---------------------|
| Thủ kho kiêm QC | Maker | Lập phiếu nhận hàng hoàn, kiểm QC hàng hoàn trả, phân loại hàng Đạt (nhập lại kho thường) / Lỗi (chuyển sang Quarantine) |
| Nhân viên kho | Maker | Hỗ trợ bốc xếp, di chuyển hàng hoàn và hàng lỗi theo chỉ dẫn của Thủ kho |
| Trưởng kho | Checker | Phê duyệt biên bản hàng lỗi tại Quarantine Zone và quyết định Tiêu hủy (DISPOSAL) theo thẩm quyền |
| Kế toán viên | Maker | Tạo Credit Note ghi nhận giảm công nợ cho Đại lý tương ứng với hàng hoàn |

## 3. Functional Requirements (EARS)
*Vui lòng xem chi tiết yêu cầu chức năng EARS tại các tài liệu đặc tả tính năng:*
* [EARS - Customer Returns](./features/feature-storekeeper-customer-returns.md#3-functional-requirements-ears)
* [EARS - Scrap Disposal](./features/feature-manager-scrap-disposal.md#3-functional-requirements-ears)

## 4. Non-functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-001 | Credit Note creation + balance update | ≤ 1s |

## 5. Data Model

### receipts (type = 'RETURN')
- `id` (BIGSERIAL, PK)
- `receipt_number` (VARCHAR(50), UNIQUE, NOT NULL)
- `source_order_code` (VARCHAR(100)) -- DO code gốc
- `type` (VARCHAR(20), DEFAULT 'RETURN', CHECK IN ('PURCHASE','RETURN'), NOT NULL)
- `warehouse_id` (BIGINT, FK→warehouses, NOT NULL)
- `dealer_id` (BIGINT, FK→dealers)
- `contact_person` (VARCHAR(255))
- `status` (VARCHAR(30), CHECK IN ('PENDING_RECEIPT','DRAFT','QC_COMPLETED','APPROVED','REJECTED'))
- `document_date` (DATE, NOT NULL)
- `accounting_period_id` (BIGINT, FK→accounting_periods)
- `created_by` (BIGINT, FK→users, NOT NULL)
- `created_at` (TIMESTAMPTZ)

### credit_notes
- `id` (BIGSERIAL, PK)
- `credit_note_number` (VARCHAR(50), UNIQUE, NOT NULL)
- `dealer_id` (BIGINT, FK→dealers, NOT NULL)
- `receipt_id` (BIGINT, FK→receipts) -- reference tới return receipt
- `amount` (DECIMAL(18,2), NOT NULL)
- `reason` (TEXT, NOT NULL)
- `created_by` (BIGINT, FK→users, NOT NULL)
- `document_date` (DATE, NOT NULL)
- `accounting_period_id` (BIGINT, FK→accounting_periods)
- `created_at` (TIMESTAMPTZ)

### adjustments (type = 'DISPOSAL' hoặc 'RETURN_TO_VENDOR')
- `id` (BIGSERIAL, PK)
- `adjustment_number` (VARCHAR(50), UNIQUE, NOT NULL)
- `warehouse_id` (BIGINT, FK→warehouses, NOT NULL)
- `product_id` (BIGINT, FK→products, NOT NULL)
- `batch_id` (BIGINT, FK→batches)
- `location_id` (BIGINT, FK→warehouse_locations) -- Quarantine location
- `quantity_adjustment` (DECIMAL(10,2), NOT NULL) -- âm khi tiêu hủy/trả NCC
- `type` (VARCHAR(30), CHECK IN ('STOCK_TAKE','TRANSFER_DISCREPANCY','DISPOSAL','RETURN_TO_VENDOR','CORRECTION_VOUCHER'), NOT NULL)
- `reference_id` (BIGINT)
- `reference_type` (VARCHAR(50))
- `reason` (TEXT, NOT NULL)
- `approved_by` (BIGINT, FK→users)
- `approved_at` (TIMESTAMPTZ)
- `document_date` (DATE, NOT NULL)
- `accounting_period_id` (BIGINT, FK→accounting_periods)
- `created_by` (BIGINT, FK→users, NOT NULL)
- `created_at` (TIMESTAMPTZ)

### damage_reports (for disposal flow)
- `id` (BIGSERIAL, PK)
- `report_number` (VARCHAR(50), UNIQUE, NOT NULL)
- `warehouse_id` (BIGINT, FK→warehouses, NOT NULL)
- `product_id` (BIGINT, FK→products, NOT NULL)
- `batch_id` (BIGINT, FK→batches)
- `quantity` (DECIMAL(10,2), NOT NULL)
- `cause` (TEXT, NOT NULL)
- `image_url` (VARCHAR(500))
- `reported_by` (BIGINT, FK→users, NOT NULL)
- `report_date` (DATE, NOT NULL)
- `created_at` (TIMESTAMPTZ)

## 6. API Spec
*Vui lòng xem chi tiết API endpoints tại các tài liệu đặc tả tính năng:*
* [APIs - Customer Returns](./features/feature-storekeeper-customer-returns.md#4-api-endpoints)
* [APIs - Scrap Disposal](./features/feature-manager-scrap-disposal.md#4-api-endpoints)

## 7. Error Handling

| Error | HTTP | Condition |
|-------|------|-----------|
| RETURN_EXCEEDS_ORIGINAL_SALE | 422 | Return qty > original DO qty |
| MISSING_CREDIT_NOTE_REASON | 400 | No reason for credit |

## 8. Acceptance Criteria
*Vui lòng xem chi tiết kịch bản kiểm thử tại các tài liệu đặc tả tính năng:*
* [Acceptance - Customer Returns](./features/feature-storekeeper-customer-returns.md#5-acceptance-criteria)
* [Acceptance - Scrap Disposal](./features/feature-manager-scrap-disposal.md#5-acceptance-criteria)

## 9. Out of Scope

- Return shipping logistics
- Restocking fees
- Automated return authorization
