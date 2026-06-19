# Feature: Kho Đích Tiếp nhận & Xử lý Chênh lệch Điều chuyển (US-WMS-12)

## 1. Context and Goal
Công nhân/Nhân viên kho tại kho đích kiểm tra mặt số lượng khi xe điều chuyển đến và nhập số lượng thực nhận ban đầu. Thủ kho đích kiểm tra lại số lượng, nhập/chốt QC, chọn vị trí nhập kho cho hàng đạt và duyệt kết quả nhận. Trưởng kho đích là người xác nhận cuối cùng, xử lý chênh lệch nếu có và hoàn tất phiếu. Bất kỳ thiếu hụt hoặc vấn đề nào so với số lượng gửi đi đều phải được ghi nhận lý do; nhận thừa so với số lượng gửi đi bị chặn.

Luong nay duoc xu ly trong man **Dieu chuyen noi bo** cho ma `TRF-*`. No khong di vao danh sach phieu nhap `RN-*` tu nha cung cap.
Sprint 1 gia dinh moi phieu `TRF` co mot lan ship va mot lan final receive. Cac truong hop nhieu dot nhan dang do, split receive, hoac chia thanh nhieu lan final receive khong nam trong scope chuan cua feature nay.

## 2. Actors
* **Nhân viên kho/Công nhân kho đích**: Kiểm tra mặt số lượng khi xe đến và nhập số lượng thực nhận ban đầu.
* **Thủ kho (Kho đích)**: Kiểm tra lại số lượng công nhân nhập, nhập/chốt QC, chọn `destinationLocationId` cho hàng đạt QC và duyệt kết quả nhận.
* **Trưởng kho đích**: Đối chiếu kết quả đã được Thủ kho duyệt, xác nhận chênh lệch nếu có và duyệt hoàn tất nhập kho.

## 3. Functional Requirements (EARS)
* **Event-driven:**
  * WHEN a Nhân viên kho/Công nhân kho đích records initial received counts, the system SHALL:
    * Allow the action only while the transfer is `IN_TRANSIT`.
    * Require `issueReason` per item when that item's `receivedQty < sentQty`, `receivedQty > sentQty`, or the worker reports an issue for that item; items with `receivedQty == sentQty` and no reported issue SHALL NOT require `issueReason`.
    * Reject `received_qty > sent_qty` after validating that an issue reason was provided.
    * Store the worker-entered counts as the current `received_qty` without completing the transfer.
    * Allow the worker to edit the received counts until Thủ kho đích approves the receive check.
    * Create a `TRANSFER_RECEIVE_COUNT` audit log entry.
    * Treat repeated worker saves before receive-check approval as draft overwrite of the same receiving cycle, not as separate partial receives.
  * WHEN a Thủ kho đích checks and approves received counts and QC results, the system SHALL:
    * Allow the action only after initial received counts exist and before receive check approval.
    * Require `received_qty <= sent_qty`.
    * Allow Thủ kho đích to adjust the confirmed received quantity after checking the worker-entered count.
    * Allow optional `checkerNote` when `confirmedReceivedQty` equals the worker-entered `receivedQty`.
    * Require `checkerNote` when `confirmedReceivedQty` differs from the worker-entered `receivedQty`.
    * Require `qc_passed_qty + qc_failed_qty = confirmedReceivedQty`.
    * Require a QC failure reason when `qc_failed_qty > 0`.
    * Require `destination_location_id` for QC-passed quantity.
    * Save `confirmedReceivedQty` as the effective `received_qty` for final confirmation and inventory settlement.
    * Store the approved received counts and QC result without completing the transfer.
    * Treat `receive_checked_at IS NOT NULL` as the receive-check approval marker; no separate receive-check status field is required.
    * Block Nhân viên kho/Công nhân kho đích from editing received counts after this approval.
    * Create a `TRANSFER_RECEIVE_CHECK` audit log entry.
  * WHEN a Trưởng kho đích confirms receipt:
    * Require the transfer to be `IN_TRANSIT` and require receive check approval by Thủ kho đích.
    * IF `received_qty > sent_qty`, the system SHALL reject the confirmation.
    * IF `received_qty == sent_qty`:
      * Decrease In-Transit inventories: `total_qty -= sent_qty` for the target product.
      * Increase destination warehouse inventories: `total_qty += qc_passed_qty` at the target bin location.
      * Increase quarantine inventory by `qc_failed_qty` when QC failed quantity is greater than zero; the system SHALL automatically use the destination warehouse quarantine location where `is_quarantine = true`.
      * Set status to `COMPLETED`.
    * IF `received_qty < sent_qty`:
      * Require `discrepancy_reason` before confirmation.
      * Decrease In-Transit inventories: `total_qty -= sent_qty` (deducts the full sent quantity to clear the virtual warehouse).
      * Increase destination warehouse inventories: `total_qty += qc_passed_qty` at the target bin location.
      * Increase quarantine inventory by `qc_failed_qty` when QC failed quantity is greater than zero; the system SHALL automatically use the destination warehouse quarantine location where `is_quarantine = true`.
      * Create an `adjustments` record for the discrepancy (`variance_qty = received_qty - sent_qty`, type = `'TRANSFER_DISCREPANCY'`).
      * Log the reason in the audit trail (`discrepancy_reason`).
      * Set status to `COMPLETED_WITH_DISCREPANCY`.
    * IF there is a final-level material issue unrelated to QC failure, the system SHALL require `discrepancy_reason` before confirmation.
    * IF QC failed quantity is greater than zero and no active quarantine location exists in the destination warehouse, the system SHALL reject confirmation with `QUARANTINE_LOCATION_REQUIRED`.
    * Create a `TRANSFER_RECEIVE_CONFIRM` audit log entry.
    * Create a `TRANSFER_DISCREPANCY_CREATE` audit log entry when a shortage adjustment is created.
* **Authorization and warehouse scope:**
  * Nhân viên kho/Công nhân kho đích SHALL record initial counts only for transfers whose destination warehouse is in the actor's assigned warehouse scope.
  * Thủ kho đích SHALL approve receive check only for transfers whose destination warehouse is in the actor's assigned warehouse scope.
  * Trưởng kho đích SHALL confirm final receipt only for transfers whose destination warehouse is in the actor's assigned warehouse scope.

## 4. API Endpoints
* `PUT /api/v1/transfers/{id}/receive-count` - Nhân viên kho/Công nhân kho đích nhập hoặc sửa số lượng thực nhận ban đầu khi phiếu còn `IN_TRANSIT` và chưa được Thủ kho duyệt receive check.
* `PUT /api/v1/transfers/{id}/receive-check` - Thủ kho đích kiểm tra lại số lượng, nhập/chốt QC, chọn vị trí nhập kho cho hàng đạt và duyệt kết quả nhận.
* `POST /api/v1/transfers/{id}/final-receive` - Trưởng kho đích xác nhận nhận hàng tại kho đích và báo cáo chênh lệch nếu có.

### `PUT /api/v1/transfers/{id}/receive-count` Request
```json
{
  "items": [
    {
      "transferItemId": 1001,
      "receivedQty": 28,
      "issueReason": "Thieu 2 san pham khi kiem dem ban dau"
    }
  ]
}
```

### `PUT /api/v1/transfers/{id}/receive-check` Request
```json
{
  "items": [
    {
      "transferItemId": 1001,
      "confirmedReceivedQty": 28,
      "qcPassedQty": 26,
      "qcFailedQty": 2,
      "destinationLocationId": 201,
      "qcFailureReason": "2 san pham bi mop vo hop"
    }
  ],
  "checkerNote": "Da doi chieu so luong voi cong nhan nhap ban dau"
}
```

### `POST /api/v1/transfers/{id}/final-receive` Request
```json
{
  "discrepancyReason": "Thieu 2 san pham so voi so luong da gui tu kho nguon"
}
```

`discrepancyReason` is required when any item has `receivedQty < sentQty` or the final confirmer reports a final-level material issue with the transfer. Trưởng kho đích uses `discrepancyReason` itself to report that final-level issue; no separate boolean or new field is required. It MAY be omitted when all received quantities match sent quantities and no issue is reported. QC failure alone uses `qcFailureReason` from receive-check and does not require a duplicate `discrepancyReason` unless the Trưởng kho đích reports another final-level issue.

## 5. Validation and Error Handling
* `TRANSFER_RECEIVE_NOT_ALLOWED` (HTTP 409): transfer is not `IN_TRANSIT` or the current receive step is not allowed.
* `RECEIVE_ISSUE_REASON_REQUIRED` (HTTP 400): an item has `receivedQty < sentQty`, `receivedQty > sentQty`, or a reported issue without item-level `issueReason`.
* `RECEIVED_QTY_EXCEEDS_SENT` (HTTP 422): `receivedQty > sentQty`.
* `RECEIVE_CHECK_REQUIRED` (HTTP 409): Trưởng kho đích attempts final confirmation before Thủ kho đích approves receive check.
* `CHECKER_NOTE_REQUIRED` (HTTP 400): `confirmedReceivedQty` differs from the worker-entered `receivedQty` without `checkerNote`; `checkerNote` is optional when the quantities match.
* `QC_TOTAL_MISMATCH` (HTTP 400): `qcPassedQty + qcFailedQty != confirmedReceivedQty`.
* `QC_FAILURE_REASON_REQUIRED` (HTTP 400): `qcFailedQty > 0` without `qcFailureReason`.
* `DESTINATION_LOCATION_REQUIRED` (HTTP 400): QC-passed quantity exists without `destinationLocationId`.
* `QUARANTINE_LOCATION_REQUIRED` (HTTP 422): QC-failed quantity exists but destination warehouse has no active quarantine location.
* `DISCREPANCY_REQUIRES_REASON` (HTTP 400): shortage or final-level material issue outside normal QC failure exists without a reason.
* `TRANSFER_SPLIT_RECEIVE_NOT_SUPPORTED` (HTTP 409): actor attempts to finalize the same transfer through multiple independent receipt cycles.

## 6. Acceptance Criteria
* **Scenario: Receive with quantity discrepancy**
  * Given a transfer of 30 units in `IN_TRANSIT` status
  * When Nhân viên kho HN records initial receipt of 28 units with a shortage reason
  * And Thủ kho HN checks the count, records 28 passed units, selects a destination location, and approves receive check
  * And Trưởng kho HN confirms the receipt with a shortage reason
  * Then the system SHALL:
    * Add 28 units to inventory HN.
    * Deduct the full 30 units from virtual In-Transit inventory (clearing it to 0).
    * Create a `TRANSFER_DISCREPANCY` adjustment for -2 units.
    * Set status to `COMPLETED_WITH_DISCREPANCY`.

* **Scenario: Reject over-receipt**
  * Given a transfer of 30 units in `IN_TRANSIT` status
  * When Nhân viên kho HN records receipt of 32 units with an issue reason
  * Then the system SHALL reject the receive-count request because received quantity cannot exceed sent quantity.

* **Scenario: Reject shortage without issue reason**
  * Given a transfer of 30 units in `IN_TRANSIT` status
  * When Nhân viên kho HN records receipt of 28 units without an issue reason
  * Then the system SHALL reject the receive-count request with `RECEIVE_ISSUE_REASON_REQUIRED`.

* **Scenario: Receive with QC failed quantity**
  * Given a transfer of 30 units in `IN_TRANSIT` status
  * When Nhân viên kho HN records initial receipt of 30 units
  * And Thủ kho HN records 28 passed units and 2 failed units with a QC failure reason, then approves receive check
  * And Trưởng kho HN confirms the receipt
  * Then the system SHALL add 28 units to regular inventory, add 2 units to quarantine inventory, and exclude the failed quantity from available inventory.

* **Scenario: Block worker edit after receive check approval**
  * Given Nhân viên kho HN recorded initial receipt counts
  * And Thủ kho HN approved the receive check
  * When Nhân viên kho HN attempts to edit the received quantity
  * Then the system SHALL reject the edit with `TRANSFER_RECEIVE_NOT_ALLOWED`.

* **Scenario: Storekeeper corrects worker count**
  * Given Nhân viên kho HN recorded initial receipt of 28 units
  * When Thủ kho HN confirms the checked received quantity as 27 units with a checker note
  * Then the system SHALL save the checked quantity and create a `TRANSFER_RECEIVE_CHECK` audit entry.

* **Scenario: Reject checked count change without checker note**
  * Given Nhân viên kho HN recorded initial receipt of 28 units
  * When Thủ kho HN confirms the checked received quantity as 27 units without `checkerNote`
  * Then the system SHALL reject the receive-check request with `CHECKER_NOTE_REQUIRED`.

* **Scenario: Reject QC failed quantity without reason**
  * Given a transfer is in `IN_TRANSIT` status and initial received counts exist
  * When Thủ kho HN records `qcFailedQty > 0` without `qcFailureReason`
  * Then the system SHALL reject the receive-check request with `QC_FAILURE_REASON_REQUIRED`.

* **Scenario: Reject final confirmation before receive check approval**
  * Given Nhân viên kho HN recorded initial receipt counts
  * When Trưởng kho HN attempts to confirm final receipt before Thủ kho HN approves receive check
  * Then the system SHALL reject the request with `RECEIVE_CHECK_REQUIRED`.

* **Scenario: Worker updates draft before receive-check approval**
  * Given transfer `TRF-*` is `IN_TRANSIT`
  * And worker saved an initial received draft
  * When the same worker updates the counts again before storekeeper approve receive-check
  * Then the system SHALL overwrite the worker draft for the same receiving cycle and keep the transfer in the pre-check state.

* **Scenario: Split final receive is out of scope**
  * Given transfer `TRF-*` is in one active receiving cycle
  * When a user attempts to complete half of the quantity now and half later as separate final confirmations
  * Then the system SHALL reject the flow because split final receive is not supported in Sprint 1.
