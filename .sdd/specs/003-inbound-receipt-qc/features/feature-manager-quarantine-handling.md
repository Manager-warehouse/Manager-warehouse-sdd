# Feature: Trưởng kho Xử lý Hàng lỗi trong Quarantine Zone (US-WMS-04)

## 1. Context and Goal
Trưởng kho xử lý hàng lỗi đang nằm trong khu vực cách ly (Quarantine Zone) bằng phương án trả về nhà cung cấp (RTV) kèm Debit Note. Hàng gia dụng lỗi không được phân loại lại thành cấp chất lượng khác để bán tiếp trong Sprint 1. Feature 003 chỉ hiển thị và xử lý nút "Trả NCC"; luồng tiêu hủy hàng lỗi không hiển thị trong feature này và được đặc tả tại [009 returns & disposal](../../009-returns-scrap-disposal/spec.md).

## Clarifications

### Session 2026-06-11
- Q: Should RTV deduct quarantine inventory when Trưởng kho creates the RTV request? -> A: No; deduct only when Storekeeper confirms physical return to supplier.
- Q: Who creates the Debit Note for RTV? -> A: The system creates it automatically when Trưởng kho creates the RTV request.
- Q: What is the receipt status after RTV completion? -> A: The receipt remains `QC_FAILED`; RTV completion is represented by the `RETURN_TO_VENDOR` adjustment.
- Q: Should feature 003 show a Disposal action? -> A: No; only show "Trả NCC" in this feature.
- Q: Can a Trưởng kho create RTV for another warehouse? -> A: No; RTV actions are limited to receipts in warehouses assigned to the authenticated Trưởng kho.
- Q: Can a receipt have multiple pending RTV requests? -> A: No; duplicate RTV creation is rejected while a pending or confirmed RTV already exists for the receipt.
- Q: Can Storekeeper confirm a partial RTV quantity? -> A: No; RTV confirmation must return the full quarantined quantity for the receipt.

## 2. Actors
* **Trưởng kho**: Tạo RTV request cho hàng lỗi trong Quarantine.
* **Storekeeper (STOREKEEPER)**: Rà soát tình trạng hàng lỗi tại Quarantine, đóng gói hàng trả NCC, và xác nhận đã giao trả NCC.
* **Kế toán viên**: Theo dõi Debit Note do hệ thống tự tạo khi RTV request được lập.

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * The system SHALL create `QUARANTINE_RTV_CREATE` and `QUARANTINE_RTV_CONFIRM` audit log entries for RTV documentation and quarantine stock deduction.
  * The system SHALL expose only the "Trả NCC" action in feature 003 quarantine handling; disposal actions SHALL be handled only by spec 009.
  * The system SHALL NOT support quality-tier reclassification for QC-failed household goods; failed goods remain quarantine stock until RTV or disposal.
  * The system SHALL allow RTV creation only for `QC_FAILED` receipts belonging to warehouses assigned to the authenticated Trưởng kho.
* **Event-driven:**
  * WHEN a receipt is confirmed as `QC_FAILED`, the system SHALL create quarantine inventory for the whole failed lot and exclude it from available selling inventory.
  * WHEN a Trưởng kho selects "Trả NCC" (RTV) for a `QC_FAILED` receipt with quarantine inventory, the system SHALL:
    * Create a `debit_notes` record for the supplier.
    * Create an `adjustments` record with type `'RETURN_TO_VENDOR'` to serve as the RTV document.
    * Keep the adjustment pending physical return confirmation.
    * The system SHALL NOT decrease quarantine inventories at this step.
  * WHEN Storekeeper confirms physical return to supplier for a pending `RETURN_TO_VENDOR` adjustment, the system SHALL:
    * Require the confirmed returned quantity to equal the full quarantined quantity for the receipt.
    * Mark the `RETURN_TO_VENDOR` adjustment as confirmed by setting approval/confirmation fields.
    * Decrease quarantine inventories by the full quarantined quantity.
    * Keep the source receipt status as `QC_FAILED`.
* **State-driven:**
  * WHILE return-to-vendor documentation is pending, the system SHALL NOT deduct quarantine inventories.
  * WHILE the confirmed returned quantity is less than or greater than the full quarantined quantity, the system SHALL reject RTV confirmation with HTTP 422.
  * WHILE a `RETURN_TO_VENDOR` adjustment already exists for the receipt, duplicate RTV creation attempts SHALL be rejected with HTTP 409.
  * WHILE a `RETURN_TO_VENDOR` adjustment has already been confirmed, duplicate confirmation attempts SHALL be rejected with HTTP 409.

## 4. API Endpoints
* `POST /api/v1/receipts/{id}/rtv` - Lập phiếu trả hàng NCC và sinh Debit Note.
* `PUT /api/v1/receipts/{id}/rtv/confirm` - Storekeeper xác nhận đã giao trả NCC và trừ tồn Quarantine.

All write requests in this feature SHALL include `expectedVersion` so stale concurrent RTV create/confirm actions return HTTP 409. RTV create is restricted to an assigned Trưởng kho, while RTV physical return confirmation is restricted to Storekeeper for the receipt warehouse. RTV confirmation SHALL provide `returnedQty`, and the backend SHALL reject any value different from the full quarantined quantity with HTTP 422.

## 5. Acceptance Criteria

**Scenario: Trưởng kho trả hàng lỗi về nhà cung cấp**
* Given 20 units of product X in quarantine after `QC_FAILED`
* When Trưởng kho selects "Trả NCC"
* Then the system SHALL create a pending `RETURN_TO_VENDOR` adjustment, create a Debit Note, and SHALL NOT deduct quarantine stock yet.

**Scenario: Storekeeper confirms physical return to supplier**
* Given a pending `RETURN_TO_VENDOR` adjustment for 20 units of product X
* When Storekeeper confirms the goods were handed back to the supplier
* Then the system SHALL deduct 20 units from quarantine inventory and keep the receipt status as `QC_FAILED`.

**Scenario: Block partial RTV confirmation**
* Given 20 units of product X are in quarantine for a pending `RETURN_TO_VENDOR` adjustment
* When Storekeeper confirms only 18 units were handed back to the supplier
* Then the system SHALL reject the confirmation with HTTP 422 and SHALL NOT deduct quarantine inventory.

**Scenario: Feature 003 does not show disposal action**
* Given a receipt has status `QC_FAILED`
* When Trưởng kho opens quarantine handling in feature 003
* Then the system SHALL show "Trả NCC" and SHALL NOT show "Tiêu hủy".

**Scenario: Block duplicate RTV creation**
* Given a receipt already has a pending or confirmed `RETURN_TO_VENDOR` adjustment
* When Trưởng kho selects "Trả NCC" again
* Then the system SHALL reject the request with HTTP 409 and SHALL NOT create another Debit Note.
