# Feature: Nhân viên kho Tiếp nhận & Đếm hàng Thực tế (US-WMS-03)

## 1. Context and Goal
Nhân viên kho (WAREHOUSE_STAFF) chịu trách nhiệm tiếp nhận hàng hóa thực tế giao đến kho, kiểm đếm số lượng thực tế nhận được và cập nhật vào lệnh nhập để chuẩn bị cho quy trình kiểm tra chất lượng (QC). Một phiếu nhập kho chỉ ghi nhận các sản phẩm cần nhập vào kho trong một lần nhận hàng cụ thể; không dùng dòng hàng có số lượng bằng 0 để biểu diễn sản phẩm không giao trong lần này. Feature này chỉ bao phủ bước ghi nhận số lượng thực tế và chuyển phiếu từ `PENDING_RECEIPT` sang `DRAFT`; việc nhập kết quả QC, xác nhận `QC_COMPLETED`/`QC_FAILED`, phê duyệt nhập kho và putaway thuộc các feature tiếp theo.

## 2. Actors
* **Nhân viên kho (WAREHOUSE_STAFF)**: Tiếp nhận hàng thực tế, kiểm đếm và cập nhật số lượng thực tế để chuyển phiếu sang `DRAFT`.

## 3. Functional Requirements (EARS)
* **Event-driven:**
  * WHEN a Warehouse Staff records physical counts, the receive request SHALL submit `counted_qty` for each counted receipt item.
  * WHEN any submitted receipt item has invalid count data, the system SHALL reject the entire receive request and SHALL NOT save any partial changes from that request.
  * WHEN the receive request has `complete_receiving = false`, the system SHALL derive and save `actual_qty` and `over_received_qty` from the provided valid `counted_qty` values and keep the receipt status as `PENDING_RECEIPT`.
  * WHEN the receive request has `complete_receiving = true`, the system SHALL validate that every receipt item has a valid count before completing receiving.
  * WHEN a Warehouse Staff records physical counts for only some receipt items, the system SHALL derive and save `actual_qty` and `over_received_qty` for the provided valid counts and keep the receipt status as `PENDING_RECEIPT`.
  * WHEN a Warehouse Staff completes receiving for a receipt in `PENDING_RECEIPT`, the system SHALL require a valid `counted_qty` for every receipt item before calculating `actual_qty` and updating the receipt status to `DRAFT`.
  * WHEN receiving is submitted, the system SHALL require every receipt item to represent goods physically received in this receipt.
  * WHEN a Warehouse Staff records physical counts, the system SHALL reject any item with `counted_qty <= 0`.
  * WHEN `complete_receiving = true` and every receipt item has a valid `counted_qty > 0`, the system SHALL calculate and save `actual_qty`/`over_received_qty`, then update the receipt status to `DRAFT`.
  * WHEN counted quantity differs from expected quantity, the system SHALL preserve `expected_qty`, store `actual_qty` as the quantity accepted into this receipt, and retain the variance for Storekeeper review and manager approval.
  * WHEN counted quantity exceeds `expected_qty`, the system SHALL cap `actual_qty` at `expected_qty` and record the excess as `over_received_qty` for separate holding and return-to-supplier handling.
  * WHEN counted quantity is less than `expected_qty`, the system SHALL store the counted quantity as `actual_qty`, preserve the shortage for Storekeeper review, and allow a supplemental receipt to be created against the original receipt for the missing quantity.
  * WHEN `over_received_qty > 0`, the system SHALL keep the excess quantity recorded on the receipt item as evidence for a later over-receipt return-to-supplier issue document.
  * WHEN a Warehouse Staff corrects previous counts, the correction request SHALL also submit `counted_qty`, and the system SHALL recalculate `actual_qty` and `over_received_qty`.
* **State-driven:**
  * WHILE a receipt is `PENDING_RECEIPT`, the system SHALL allow Warehouse Staff to update physical counts through `counted_qty`.
  * WHILE a receipt is `DRAFT`, the system SHALL treat actual received quantities as counted-but-not-QC-confirmed goods.
  * WHILE a receipt has not been approved, rejected, confirmed for quarantine intake, or included in an RTV flow, the system SHALL allow Warehouse Staff to correct actual received quantities when counting mistakes are found.
  * WHILE physical counts are corrected after QC data has been recorded or confirmed, the system SHALL invalidate the previous QC result by clearing `qc_result`, `sample_qty`, `sample_passed_qty`, `sample_failed_qty`, `qc_failure_reason`, and `qc_by`, then return the receipt to `DRAFT` so QC must be performed again.
  * WHILE receiving is being recorded, the system SHALL NOT create or update batch records, regular inventory, quarantine inventory, or bin locations.
  * WHILE the Trưởng kho has not made the final approve/reject decision for the receipt, the system SHALL NOT create quarantine inventory or assign a quarantine/holding location for any `over_received_qty`.
  * WHILE goods are later rejected by QC, quarantine inventory SHALL only be created outside this receive feature after Trưởng kho confirms the quarantine handling decision.

## 4. API Endpoints
* `PUT /api/v1/receipts/{id}/receive` - Nhân viên kho cập nhật số đếm thực tế của lô hàng.

## 5. Acceptance Criteria
* **Scenario: Warehouse Staff records receipt counts**
  * Given a receipt in `PENDING_RECEIPT` state with expected quantity of 100
  * When Nhân viên kho counts 98 units physically and submits `counted_qty = 98`
  * Then the system SHALL save `actual_qty = 98`, preserve `expected_qty = 100`, and update the receipt status to `DRAFT`.
  * And the system SHALL NOT create batch records, increase regular inventory, increase quarantine inventory, or assign bin locations.

* **Scenario: Save partial receipt counts**
  * Given a receipt in `PENDING_RECEIPT` state with multiple receipt items
  * When Nhân viên kho saves `counted_qty` for only some receipt items with `complete_receiving = false`
  * Then the system SHALL derive and save `actual_qty` and `over_received_qty` for the provided valid counts and keep the receipt status as `PENDING_RECEIPT`.
  * And the system SHALL NOT move the receipt to `DRAFT` until `complete_receiving = true` and every receipt item has a valid `counted_qty > 0`.

* **Scenario: Reject completion when receipt counts are incomplete**
  * Given a receipt in `PENDING_RECEIPT` state with multiple receipt items
  * When Nhân viên kho submits `complete_receiving = true` without a valid count for every receipt item
  * Then the system SHALL reject the request and keep the receipt status as `PENDING_RECEIPT`.

* **Scenario: Reject non-positive receipt counts**
  * Given a receipt in `PENDING_RECEIPT` state
  * When Nhân viên kho submits `counted_qty = 0` or a negative quantity for any item
  * Then the system SHALL reject the entire request, save no submitted line changes from that request, and keep the receipt status as `PENDING_RECEIPT`.

* **Scenario: Correct receipt counts before inventory-affecting manager decision**
  * Given a receipt has not been approved, rejected, confirmed for quarantine intake, or included in an RTV flow
  * When Nhân viên kho discovers a counting mistake and submits corrected `counted_qty` values
  * Then the system SHALL recalculate and save the corrected `actual_qty` and `over_received_qty` values and keep or return the receipt status to `DRAFT`.

* **Scenario: Correct receipt counts after QC was recorded**
  * Given a receipt has QC data recorded or confirmed and has not been approved, rejected, confirmed for quarantine intake, or included in an RTV flow
  * When Nhân viên kho submits corrected `counted_qty` values
  * Then the system SHALL recalculate and save the corrected `actual_qty` and `over_received_qty` values, clear the previous QC fields, and return the receipt status to `DRAFT`.

* **Scenario: Prevent count correction after quarantine intake**
  * Given a receipt is `QC_FAILED` and Trưởng kho has confirmed quarantine intake
  * When Nhân viên kho tries to correct `counted_qty`
  * Then the system SHALL reject the request because quarantine inventory has already been created.

* **Scenario: Handle shortage against expected quantity**
  * Given a receipt item has `expected_qty = 100`
  * When Nhân viên kho counts 90 units and completes receiving
  * Then the system SHALL save `actual_qty = 90`, preserve `expected_qty = 100`, and flag a shortage of 10 units for Storekeeper review.
  * And the missing 10 units SHALL be handled by a supplemental receipt linked to the original receipt when they arrive later.

* **Scenario: Handle over-received quantity**
  * Given a receipt item has `expected_qty = 100`
  * When Nhân viên kho counts 120 units and completes receiving
  * Then the system SHALL save `actual_qty = 100` for the receipt item and record `over_received_qty = 20` for separate handling.
  * And the system SHALL NOT create quarantine inventory or assign a quarantine/holding location for the excess 20 units until Trưởng kho makes the final approve/reject decision for the receipt.
