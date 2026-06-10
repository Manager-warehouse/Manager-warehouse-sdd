# Feature: Trưởng kho Xử lý Hàng lỗi trong Quarantine Zone (US-WMS-04)

## 1. Context and Goal
Trưởng kho xử lý hàng lỗi đang nằm trong khu vực cách ly (Quarantine Zone) bằng phương án trả về nhà cung cấp (RTV) kèm Debit Note. Sprint 1 không xử lý tiêu hủy hàng lỗi trong inbound receipt.

## 2. Actors
* **Trưởng kho**: Xác nhận trả hàng lỗi về nhà cung cấp.
* **Storekeeper (STOREKEEPER)**: Rà soát tình trạng hàng lỗi tại Quarantine và đề xuất trả nhà cung cấp.
* **Kế toán viên**: Nhận thông báo trả hàng NCC, lập Debit Note tương ứng.

## 3. Functional Requirements (EARS)
* **Event-driven:**
  * WHEN a receipt is confirmed as `QC_FAILED`, the system SHALL create quarantine inventory for the whole failed lot and exclude it from available selling inventory.
  * WHEN a Trưởng kho selects "Trả NCC" (RTV), the system SHALL:
    * Create a `debit_notes` record for the supplier.
    * Create an `adjustments` record with type `'RETURN_TO_VENDOR'`.
    * Decrease quarantine inventories.
* **State-driven:**
  * WHILE return-to-vendor documentation is pending, the system SHALL NOT deduct quarantine inventories.

## 4. API Endpoints
* `POST /api/v1/receipts/{id}/rtv` - Lập phiếu trả hàng NCC và sinh Debit Note.

## 5. Acceptance Criteria

**Scenario: Trưởng kho trả hàng lỗi về nhà cung cấp**
* Given 20 units of product X in quarantine after `QC_FAILED`
* When Trưởng kho selects "Trả NCC"
* Then the system SHALL create a `RETURN_TO_VENDOR` adjustment, create a Debit Note, and deduct quarantine stock.
