# Feature Specification: Hàng hoàn trả & Tiêu hủy (Returns & Disposal)

**Spec ID**: 009-returns-scrap-disposal
**Created**: 2026-05-30
**Updated**: 2026-06-17
**Status**: Draft
**Features**: US-WMS-24, US-WMS-04 (Disposal sub-flow)

---

## 1. Context and Goal

Đại lý có thể hoàn trả hàng (hàng lỗi, sai quy cách, thừa) và hàng trong Quarantine Zone cần được xử lý tiêu hủy khi không thể nhập lại kho hoặc trả NCC theo luồng inbound. Các quy trình này ảnh hưởng đến tồn kho (inventory) và công nợ (balance), cần kiểm soát chặt chẽ. RTV (Return To Vendor) của hàng lỗi inbound được xử lý tại Spec 003.

### Features List
* [US-WMS-24: Xử lý Hàng hoàn trả từ Đại lý](./features/feature-storekeeper-customer-returns.md)
* [US-WMS-04 (Disposal Sub-flow): Tiêu hủy Hàng lỗi từ Quarantine](./features/feature-manager-scrap-disposal.md)

## 2. Actors

| Actor | Vai trò | Nghiệp vụ liên quan |
|-------|---------|---------------------|
| Thủ kho kiêm QC | Maker | Lập phiếu nhận hàng hoàn, kiểm QC hàng hoàn trả, phân loại hàng Đạt (nhập lại kho thường) / Lỗi (chuyển sang Quarantine) |
| Nhân viên kho | Maker | Hỗ trợ bốc xếp, di chuyển hàng hoàn và hàng lỗi theo chỉ dẫn của Thủ kho |
| Trưởng kho | Checker | Phê duyệt biên bản hàng lỗi tại Quarantine Zone và quyết định Tiêu hủy (DISPOSAL) theo thẩm quyền hạn mức |
| Kế toán viên | Maker | Tạo Credit Note ghi nhận giảm công nợ cho Đại lý tương ứng với hàng hoàn |
| CEO | Checker | Phê duyệt tiêu hủy (DISPOSAL) đối với các biên bản vượt hạn mức của Trưởng kho |

## 3. Functional Requirements (EARS)
*Vui lòng xem chi tiết yêu cầu chức năng EARS tại các tài liệu đặc tả tính năng:*
* [EARS - Customer Returns](./features/feature-storekeeper-customer-returns.md#3-functional-requirements-ears)
* [EARS - Scrap Disposal](./features/feature-manager-scrap-disposal.md#3-functional-requirements-ears)

## 4. Non-functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-001 | Credit Note creation + balance update | ≤ 1s |
| NFR-002 | Disposal stock deduction processing | ≤ 1s |

## 5. Data Model (Đồng bộ với DB Migration V1, V8, V17)

### receipts (type = 'RETURN')
- `id` (BIGSERIAL, PK)
- `receipt_number` (VARCHAR(50), UNIQUE, NOT NULL)
- `source_order_code` (VARCHAR(100)) -- DO code gốc
- `delivery_order_id` (BIGINT, FK→delivery_orders) -- Link DO gốc
- `type` (VARCHAR(20), CHECK IN ('PURCHASE','RETURN'), NOT NULL)
- `warehouse_id` (BIGINT, FK→warehouses, NOT NULL)
- `dealer_id` (BIGINT, FK→dealers)
- `contact_person` (VARCHAR(255))
- `status` (VARCHAR(30), NOT NULL, CHECK IN ('PENDING_RECEIPT','DRAFT','QC_COMPLETED','QC_FAILED','APPROVED','RETURN_TO_SUPPLIER_PENDING','RETURNED_TO_SUPPLIER'))
- `document_date` (DATE, NOT NULL)
- `accounting_period_id` (BIGINT, FK→accounting_periods)
- `created_by` (BIGINT, FK→users, NOT NULL)
- `approved_by` (BIGINT, FK→users)
- `approved_at` (TIMESTAMPTZ)
- `rejection_reason` (TEXT)
- `notes` (TEXT)
- `created_at` (TIMESTAMPTZ)
- `updated_at` (TIMESTAMPTZ)

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

### adjustments (type = 'DISPOSAL')
- `id` (BIGSERIAL, PK)
- `adjustment_number` (VARCHAR(50), UNIQUE, NOT NULL)
- `warehouse_id` (BIGINT, FK→warehouses, NOT NULL)
- `product_id` (BIGINT, FK→products, NOT NULL)
- `batch_id` (BIGINT, FK→batches)
- `location_id` (BIGINT, FK→warehouse_locations) -- Quarantine location
- `quantity_adjustment` (DECIMAL(10,2), NOT NULL) -- âm khi tiêu hủy (-quantity)
- `type` (VARCHAR(30), CHECK IN ('STOCK_TAKE','TRANSFER_DISCREPANCY','DISPOSAL','RETURN_TO_VENDOR','CORRECTION_VOUCHER'), NOT NULL)
- `reference_id` (BIGINT) -- reference tới damage_report_id
- `reference_type` (VARCHAR(50)) -- 'DAMAGE_REPORT'
- `reason` (TEXT, NOT NULL)
- `approved_by` (BIGINT, FK→users) -- Trưởng kho hoặc CEO duyệt
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
- `reported_by` (BIGINT, FK→users, NOT NULL) -- Người đề xuất (WAREHOUSE_MANAGER)
- `report_date` (DATE, NOT NULL)
- `created_at` (TIMESTAMPTZ)

## 6. API Spec
*Vui lòng xem chi tiết API endpoints tại các tài liệu đặc tả tính năng:*
* [APIs - Customer Returns](./features/feature-storekeeper-customer-returns.md#4-api-endpoints)
* [APIs - Scrap Disposal](./features/feature-manager-scrap-disposal.md#4-api-endpoints)

## 7. Error Handling

| Error | HTTP | Condition |
|-------|------|-----------|
| RETURN_EXCEEDS_ORIGINAL_SALE | 422 | Return qty > original DO issued qty |
| MISSING_CREDIT_NOTE_REASON | 400 | No reason for credit |
| DISPOSAL_EXCEEDS_QUARANTINE_STOCK | 422 | Disposal qty > current quarantine total_qty |
| DISPOSAL_LIMIT_EXCEEDED | 403 | Trưởng kho duyệt vượt hạn mức 100M VND |

## 8. Acceptance Criteria
*Vui lòng xem chi tiết kịch bản kiểm thử tại các tài liệu đặc tả tính năng:*
* [Acceptance - Customer Returns](./features/feature-storekeeper-customer-returns.md#5-acceptance-criteria)
* [Acceptance - Scrap Disposal](./features/feature-manager-scrap-disposal.md#5-acceptance-criteria)

## 9. Out of Scope

- Lọc và vận chuyển hàng hoàn từ đại lý về kho (Logistics)
- Phí lưu kho hoặc phí hoàn hàng (Restocking fee)
- Tự động hoàn tiền trực tiếp qua ngân hàng (chỉ ghi giảm công nợ qua Credit Note)
