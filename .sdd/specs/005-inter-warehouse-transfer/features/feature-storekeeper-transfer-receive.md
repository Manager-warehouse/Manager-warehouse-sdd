# Feature: Thủ kho Đích Tiếp nhận & Xử lý Chênh lệch Điều chuyển (US-WMS-12)

## 1. Context and Goal
Thủ kho tại kho đích tiếp nhận xe hàng điều chuyển đến, thực hiện kiểm đếm thực tế, đối chiếu số lượng và xác nhận nhập kho. Bất kỳ chênh lệch nào so với số lượng gửi đi đều phải được ghi nhận và sinh phiếu điều chỉnh tự động.

## 2. Actors
* **Thủ kho (Kho đích)**: Đếm hàng thực tế, cập nhật số lượng nhận.
* **Trưởng kho đích**: Đối chiếu, xác nhận chênh lệch và duyệt hoàn tất nhập kho.

## 3. Functional Requirements (EARS)
* **Event-driven:**
  * WHEN a Trưởng kho đích confirms receipt:
    * IF `received_qty == sent_qty`:
      * Decrease In-Transit inventories: `total_qty -= sent_qty` for the target product and batch.
      * Increase destination warehouse inventories: `total_qty += received_qty` at the target bin location.
      * Set status to `COMPLETED`.
    * IF `received_qty ≠ sent_qty`:
      * Decrease In-Transit inventories: `total_qty -= sent_qty` (deducts the full sent quantity to clear the virtual warehouse).
      * Increase destination warehouse inventories: `total_qty += received_qty` at the target bin location.
      * Create an `adjustments` record for the discrepancy (`variance_qty = received_qty - sent_qty`, type = `'TRANSFER_DISCREPANCY'`).
      * Log the reason in the audit trail (`discrepancy_reason`).
      * Set status to `COMPLETED_WITH_DISCREPANCY`.

## 4. API Endpoints
* `PUT /api/v1/transfers/{id}/receive` - Xác nhận nhận hàng tại kho đích và báo cáo chênh lệch nếu có.

## 5. Acceptance Criteria
* **Scenario: Receive with quantity discrepancy**
  * Given a transfer of 30 units in `IN_TRANSIT` status
  * When Trưởng kho HN confirms receipt of 28 units (2 units short) and inputs the reason
  * Then the system SHALL:
    * Add 28 units to inventory HN.
    * Deduct the full 30 units from virtual In-Transit inventory (clearing it to 0).
    * Create a `TRANSFER_DISCREPANCY` adjustment for -2 units.
    * Set status to `COMPLETED_WITH_DISCREPANCY`.
