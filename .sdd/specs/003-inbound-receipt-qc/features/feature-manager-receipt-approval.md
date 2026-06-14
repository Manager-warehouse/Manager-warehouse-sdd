# Feature: Trưởng kho Duyệt Nhập kho Chính thức (US-WMS-06)

## 1. Context and Goal
Trưởng kho duyệt hoặc từ chối phiếu nhập kho chính thức dựa trên số lượng đếm thực tế và kết quả QC mẫu đã được Storekeeper xác nhận ở trạng thái `QC_COMPLETED`.

## 2. Actors
* **Trưởng kho**: Đối chiếu biên bản QC đã `QC_COMPLETED` và duyệt hoặc từ chối phiếu nhập kho chính thức.

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * The system SHALL create `RECEIPT_APPROVE` or `RECEIPT_REJECT` audit log entries for every official receipt approval decision, including before/after status and inventory delta when applicable.
* **Event-driven:**
  * WHEN a Trưởng kho approves a receipt in `QC_COMPLETED` status, the system SHALL:
    * Create or update a Batch record for the received lot.
    * Set `receipt_items.batch_id` after the Batch record is created or resolved.
    * Increase inventories: `total_qty += actual_qty` for the target product, batch, and regular location.
    * For any `over_received_qty > 0`, create quarantine/holding inventory for the excess quantity and exclude it from available selling inventory.
    * Update receipt status to `APPROVED`.
  * WHEN a Trưởng kho rejects a receipt in `QC_COMPLETED` status, the system SHALL:
    * Update receipt status to `REJECTED`.
    * Store the rejection reason.
    * Create or resolve Batch records for the physically received rejected goods.
    * Move the rejected `actual_qty` and any `over_received_qty` into quarantine/holding inventory for return-to-supplier handling.
    * The system SHALL NOT increase regular inventory or available selling inventory.
* **State-driven:**
  * WHILE a receipt status is `'PENDING_RECEIPT'`, `'DRAFT'`, or `'QC_COMPLETED'`, the system SHALL NOT increase available stock.
  * WHILE a receipt with `over_received_qty > 0` has not been approved or rejected by the Trưởng kho, the system SHALL NOT create quarantine/holding inventory for the excess quantity.
  * WHILE a receipt status is not `QC_COMPLETED`, the system SHALL reject approve/reject actions for the official receipt.
  * WHILE a receipt status is `QC_FAILED`, the system SHALL NOT allow official receipt approval and SHALL require Trưởng kho quarantine/RTV handling before any quarantine inventory is created or deducted.
  * WHILE goods are in Quarantine locations (`is_quarantine = true`), the system SHALL exclude them from available selling inventory calculations.

## 4. API Endpoints
* `PUT /api/v1/receipts/{id}/approve` - Phê duyệt nhập kho (Trưởng kho).
* `PUT /api/v1/receipts/{id}/reject` - Từ chối nhập kho.

## 5. Acceptance Criteria
* **Scenario: Successful receipt approval after QC completion**
  * Given a receipt with 100 units that passed sample QC (`QC_COMPLETED`)
  * When the Trưởng kho clicks "Duyệt nhập"
  * Then the system SHALL:
    * Change receipt status to `APPROVED`.
    * Create or update the batch record for the full received lot.
    * Set `receipt_items.batch_id`.
    * Increase regular `inventories.total_qty` by exactly 100.

* **Scenario: Receipt approval with over-received quantity**
  * Given a receipt item has `actual_qty = 100`, `over_received_qty = 20`, and passed sample QC (`QC_COMPLETED`)
  * When the Trưởng kho clicks "Duyệt nhập"
  * Then the system SHALL increase regular `inventories.total_qty` by exactly 100.
  * And the system SHALL create quarantine/holding inventory for the excess 20 units and exclude it from available selling inventory.

* **Scenario: Receipt rejection after QC completion**
  * Given a receipt item has `actual_qty = 100`, `over_received_qty = 20`, and passed sample QC (`QC_COMPLETED`)
  * When the Trưởng kho clicks "Từ chối nhập"
  * Then the system SHALL change receipt status to `REJECTED`.
  * And the system SHALL move 120 physical units into quarantine/holding inventory for return-to-supplier handling.
  * And the system SHALL NOT increase regular inventory or available selling inventory.
