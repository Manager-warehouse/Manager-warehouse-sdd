# Feature: Trưởng kho Duyệt Nhập kho Chính thức (US-WMS-05)

## 1. Context and Goal
Trưởng kho ký duyệt phiếu nhập kho chính thức dựa trên số lượng đếm thực tế và kết quả QC Đạt để ghi nhận tăng tồn kho khả dụng bán hàng trên hệ thống.

## 2. Actors
* **Trưởng kho kiêm Trưởng QC**: Đối chiếu biên bản QC và ký duyệt.

## 3. Functional Requirements (EARS)
* **Event-driven:**
  * WHEN a Trưởng kho approves a receipt, the system SHALL:
    * Create or update a Batch record (each batch MUST have exactly one quality grade A/B/C).
    * Increase inventories: `total_qty += qc_passed_qty` for the target product, batch, and location.
    * Update receipt status to `APPROVED`.
* **State-driven:**
  * WHILE a receipt status is `'PENDING_RECEIPT'`, `'DRAFT'`, or `'QC_COMPLETED'`, the system SHALL NOT increase available stock.
  * WHILE goods are in Quarantine locations (`is_quarantine = true`), the system SHALL exclude them from available selling inventory calculations.

## 4. API Endpoints
* `PUT /api/v1/receipts/{id}/approve` - Phê duyệt nhập kho (Trưởng kho).
* `PUT /api/v1/receipts/{id}/reject` - Từ chối nhập kho.

## 5. Acceptance Criteria
* **Scenario: Successful receipt approval**
  * Given a receipt with 80 units that passed QC (`QC_COMPLETED`)
  * When the Trưởng kho clicks "Duyệt nhập"
  * Then the system SHALL change status to `APPROVED`, update the batch record, and increase `inventories.total_qty` by exactly 80.
