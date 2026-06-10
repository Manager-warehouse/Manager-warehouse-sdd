# Feature: Trưởng kho Xử lý Hàng lỗi QC và Quarantine Zone (US-WMS-04)

## 1. Context and Goal
Trưởng kho xử lý lô hàng inbound đã bị xác nhận `QC_FAILED`. Chỉ Trưởng kho mới được xác nhận đưa lô lỗi vào tồn kho cách ly (Quarantine inventory) hoặc hoàn tất trả về nhà cung cấp (RTV) kèm Debit Note. Sprint 1 không xử lý tiêu hủy hàng lỗi trong inbound receipt.

## 2. Actors
* **Trưởng kho**: Xác nhận đưa hàng lỗi vào Quarantine inventory và/hoặc xác nhận trả hàng lỗi về nhà cung cấp.
* **Storekeeper (STOREKEEPER)**: Rà soát tình trạng hàng lỗi đã `QC_FAILED` và đề xuất xử lý quarantine/RTV.
* **Kế toán viên**: Nhận thông báo trả hàng NCC, lập Debit Note tương ứng.

## 3. Functional Requirements (EARS)
* **Event-driven:**
  * WHEN a receipt is confirmed as `QC_FAILED`, the system SHALL NOT create or increase quarantine inventory until Trưởng kho confirms the quarantine handling decision.
  * WHEN a Trưởng kho confirms quarantine intake for a `QC_FAILED` receipt, the system SHALL:
    * Create or resolve a Batch record for the failed lot.
    * Set `receipt_items.batch_id` for the failed lot.
    * Assign the failed lot to a Quarantine location (`warehouse_locations.is_quarantine = true`).
    * Increase `inventories.total_qty` by `actual_qty` for the product, batch, warehouse, and quarantine location.
    * Exclude the quarantine inventory from available selling inventory.
  * WHEN a Trưởng kho creates a "Trả NCC" (RTV) request for quarantined goods, the system SHALL:
    * Create a `debit_notes` record for the supplier.
    * Keep quarantine inventory unchanged while RTV documentation is pending.
  * WHEN a Trưởng kho confirms RTV completion for quarantined goods, the system SHALL:
    * Create an `adjustments` record with type `'RETURN_TO_VENDOR'`.
    * Decrease quarantine inventories by the confirmed RTV quantity.
* **State-driven:**
  * WHILE a receipt is `QC_FAILED` but quarantine intake has not been confirmed by Trưởng kho, the system SHALL keep inventory unchanged.
  * WHILE quarantine inventory has not been created for the failed lot, the system SHALL reject RTV inventory deduction for that lot.
  * WHILE return-to-vendor documentation is pending, the system SHALL NOT deduct quarantine inventories.
  * WHILE goods are stored in Quarantine locations (`is_quarantine = true`), the system SHALL NOT count them as available selling inventory.

## 4. API Endpoints
* `POST /api/v1/receipts/{id}/quarantine` - Trưởng kho xác nhận đưa lô lỗi QC vào Quarantine inventory.
* `POST /api/v1/receipts/{id}/rtv` - Lập hồ sơ trả hàng NCC và sinh Debit Note, chưa trừ quarantine inventory.
* `POST /api/v1/receipts/{id}/rtv/confirm` - Xác nhận hoàn tất trả hàng NCC và trừ quarantine inventory.

## 5. Acceptance Criteria

**Scenario: Trưởng kho xác nhận đưa hàng lỗi vào Quarantine inventory**
* Given a receipt is in `QC_FAILED` status with `actual_qty = 20`
* When Trưởng kho confirms quarantine intake
* Then the system SHALL create or resolve the failed lot batch, set `receipt_items.batch_id`, assign a quarantine location, and increase quarantine `inventories.total_qty` by 20.
* And the 20 quarantined units SHALL be excluded from available selling inventory.

**Scenario: Trưởng kho trả hàng lỗi về nhà cung cấp**
* Given 20 units of product X are already recorded in quarantine inventory after Trưởng kho confirmed quarantine intake
* When Trưởng kho selects "Trả NCC"
* Then the system SHALL create a Debit Note and keep quarantine inventory unchanged while RTV documentation is pending.

**Scenario: Trưởng kho xác nhận hoàn tất RTV**
* Given 20 units of product X are in quarantine inventory and RTV documentation has been created
* When Trưởng kho confirms RTV completion
* Then the system SHALL create a `RETURN_TO_VENDOR` adjustment and deduct 20 units from quarantine stock.
