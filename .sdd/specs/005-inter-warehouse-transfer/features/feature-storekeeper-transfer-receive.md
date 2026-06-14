# Feature: Thủ kho Đích Tiếp nhận & Xử lý Chênh lệch Điều chuyển (US-WMS-12)

## 1. Context and Goal
Thủ kho tại kho đích tiếp nhận xe hàng điều chuyển đến, thực hiện kiểm đếm thực tế và kiểm QC số lượng/chất lượng. Trưởng kho đích là người xác nhận cuối cùng. Bất kỳ thiếu hụt nào so với số lượng gửi đi đều phải được ghi nhận và sinh phiếu điều chỉnh tự động; nhận thừa so với số lượng gửi đi bị chặn.

## 2. Actors
* **Thủ kho (Kho đích)**: Đếm hàng thực tế, cập nhật số lượng nhận, kiểm QC chất lượng/số lượng và ghi nhận phần đạt/lỗi.
* **Trưởng kho đích**: Đối chiếu số nhận, kết quả QC, xác nhận chênh lệch và duyệt hoàn tất nhập kho.

## 3. Functional Requirements (EARS)
* **Event-driven:**
  * WHEN a Thủ kho đích records received counts and QC results, the system SHALL:
    * Require `received_qty <= sent_qty`.
    * Require `qc_passed_qty + qc_failed_qty = received_qty`.
    * Require a QC failure reason when `qc_failed_qty > 0`.
    * Store the received counts and QC result without completing the transfer.
  * WHEN a Trưởng kho đích confirms receipt:
    * IF `received_qty > sent_qty`, the system SHALL reject the confirmation.
    * IF `received_qty == sent_qty`:
      * Decrease In-Transit inventories: `total_qty -= sent_qty` for the target product and batch.
      * Increase destination warehouse inventories: `total_qty += qc_passed_qty` at the target bin location.
      * Increase quarantine inventory by `qc_failed_qty` when QC failed quantity is greater than zero.
      * Set status to `COMPLETED`.
    * IF `received_qty < sent_qty`:
      * Decrease In-Transit inventories: `total_qty -= sent_qty` (deducts the full sent quantity to clear the virtual warehouse).
      * Increase destination warehouse inventories: `total_qty += qc_passed_qty` at the target bin location.
      * Increase quarantine inventory by `qc_failed_qty` when QC failed quantity is greater than zero.
      * Create an `adjustments` record for the discrepancy (`variance_qty = received_qty - sent_qty`, type = `'TRANSFER_DISCREPANCY'`).
      * Log the reason in the audit trail (`discrepancy_reason`).
      * Set status to `COMPLETED_WITH_DISCREPANCY`.

## 4. API Endpoints
* `PUT /api/v1/transfers/{id}/receive-count` - Thủ kho đích nhập số nhận thực tế và kết quả QC.
* `PUT /api/v1/transfers/{id}/receive` - Trưởng kho đích xác nhận nhận hàng tại kho đích và báo cáo chênh lệch nếu có.

## 5. Acceptance Criteria
* **Scenario: Receive with quantity discrepancy**
  * Given a transfer of 30 units in `IN_TRANSIT` status
  * When Thủ kho HN records receipt of 28 passed units (2 units short)
  * And Trưởng kho HN confirms the receipt with a shortage reason
  * Then the system SHALL:
    * Add 28 units to inventory HN.
    * Deduct the full 30 units from virtual In-Transit inventory (clearing it to 0).
    * Create a `TRANSFER_DISCREPANCY` adjustment for -2 units.
    * Set status to `COMPLETED_WITH_DISCREPANCY`.

* **Scenario: Reject over-receipt**
  * Given a transfer of 30 units in `IN_TRANSIT` status
  * When Thủ kho HN records receipt of 32 units
  * Then the system SHALL reject the receive-count request because received quantity cannot exceed sent quantity.

* **Scenario: Receive with QC failed quantity**
  * Given a transfer of 30 units in `IN_TRANSIT` status
  * When Thủ kho HN records 28 passed units and 2 failed units with a QC failure reason
  * And Trưởng kho HN confirms the receipt
  * Then the system SHALL add 28 units to regular inventory, add 2 units to quarantine inventory, and exclude the failed quantity from available inventory.
