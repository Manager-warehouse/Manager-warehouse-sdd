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
    * Require `destination_location_id` for QC-passed quantity; the selected bin SHALL NOT be a quarantine bin (`QC_PASSED_BIN_MUST_NOT_BE_QUARANTINE`).
    * When `qcFailedQty > 0`, validate at this step that the target warehouse has at least one active quarantine bin (`QUARANTINE_LOCATION_NOT_CONFIGURED`); do not defer this check to finalReceive.
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
      * SHALL NOT create quarantine inventory or a disposal candidate for the missing quantity because it is not physically present.
    * IF there is a final-level material issue unrelated to QC failure, the system SHALL require `discrepancy_reason` before confirmation.
    * IF QC failed quantity is greater than zero and no active quarantine location exists in the destination warehouse, the system SHALL reject confirmation with `QUARANTINE_LOCATION_REQUIRED`.
    * Create a `TRANSFER_RECEIVE_CONFIRM` audit log entry.
    * Create a `TRANSFER_DISCREPANCY_CREATE` audit log entry when a shortage adjustment is created.
  * WHEN a Thủ kho đích or Trưởng kho đích triggers quarantine rejection (QC lỗi - Từ chối & Cách ly toàn bộ), the system SHALL:
    * Allow the action only while the transfer is `IN_TRANSIT`.
    * Require a non-blank `rejectionReason`.
    * Set status of the transfer to `QUARANTINED`.
    * Record the actor and timestamp under `rejectedBy` and `rejectedAt`, and store the rejection reason under `rejectionReason`.
    * Update all transfer items: set `receivedQty` to `sentQty`, `qcPassedQty` to `0`, `qcFailedQty` to `sentQty`. Set `checkerNote` and `qcFailureReason` of all items to the rejection reason.
    * Move all transit stock of this transfer from the `IN_TRANSIT` virtual location and add it to the destination warehouse's active quarantine bin (or source warehouse's active quarantine bin if `is_returned = true`).
    * Mark the quarantined stock as `INTERNAL_TRANSFER` origin with transfer and transfer-item traceability for disposal under [spec 009](../../009-returns-scrap-disposal/features/feature-manager-scrap-disposal.md).
    * Block supplier RTV and supplier Debit Note actions for this stock.
    * Release the vehicle, driver, and trip: set driver and vehicle status back to `AVAILABLE` and the trip status to `COMPLETED`.
    * Create a `TRANSFER_QUARANTINE_REJECT` audit log entry containing the rejection reason.
  * WHEN destination Storekeeper identifies an intact wrong SKU, the system SHALL:
    * Require expected SKU, actual SKU, affected quantity, and a non-blank reason.
    * Create a `WRONG_SKU` return report while the transfer remains `IN_TRANSIT`.
    * Keep the physical stock in In-Transit and SHALL NOT move it to regular inventory or Quarantine.
    * Notify the destination Warehouse Manager for decision.
    * Create a `TRANSFER_RETURN_REQUEST` audit log entry.
  * WHEN destination Warehouse Manager approves a `WRONG_SKU` return report, the system SHALL:
    * Require destination warehouse scope and an assigned driver/trip.
    * Set `is_returned = true`, retain the same transfer, trip, vehicle, driver, and In-Transit stock, and direct the assigned driver back to the source warehouse.
    * Flip receiving responsibility to source Staff, source Storekeeper, and source Warehouse Manager.
    * Create a `TRANSFER_RETURN_APPROVE` audit log entry.
  * WHEN returned goods arrive at the source warehouse, the system SHALL repeat receive-count, receive-check/QC, and final-receive with source-scoped actors.
  * WHEN a transfer shortage is finalized, the system SHALL import and calculate value only for physically received and accepted quantity; missing quantity SHALL remain a quantity-only discrepancy and SHALL NOT be included in destination receipt value or billing totals.
* **Authorization and warehouse scope:**
  * Nhân viên kho/Công nhân kho đích SHALL record initial counts only for transfers whose destination warehouse is in the actor's assigned warehouse scope.
  * When `is_returned = true` (Return to Source triggered), the receiving scope flips to the source warehouse; destination-side actors SHALL be blocked from all receive actions.
  * Thủ kho đích SHALL approve receive check only for transfers whose destination warehouse is in the actor's assigned warehouse scope (or source warehouse when `is_returned = true`).
  * Trưởng kho đích SHALL confirm final receipt only for transfers whose destination warehouse is in the actor's assigned warehouse scope (or source warehouse when `is_returned = true`).
* **UI behavior:**
  * When the storekeeper enters `qcFailedQty > 0` in the Kiểm tra count/QC form, the system SHALL display a read-only quarantine destination hint below the QC lỗi input: e.g., `"2 sp lỗi → WH-HCM-Q01 (tự động)"`.
  * If the destination warehouse has no active quarantine bin, the hint SHALL display: `"⚠ Kho đích chưa có Quarantine Bin!"`
  * The storekeeper CANNOT select or override the quarantine bin; it is system-assigned only.
  * The bin dropdown for QC-passed stock SHALL filter out quarantine bins (client-side pre-filtering), with backend enforcement as the authoritative guard.

## 4. API Endpoints
* `PUT /api/v1/transfers/{id}/receive-count` - Nhân viên kho/Công nhân kho đích nhập hoặc sửa số lượng thực nhận ban đầu khi phiếu còn `IN_TRANSIT` và chưa được Thủ kho duyệt receive check.
* `PUT /api/v1/transfers/{id}/receive-check` - Thủ kho đích kiểm tra lại số lượng, nhập/chốt QC, chọn vị trí nhập kho cho hàng đạt và duyệt kết quả nhận.
* `POST /api/v1/transfers/{id}/final-receive` - Trưởng kho đích xác nhận nhận hàng tại kho đích và báo cáo chênh lệch nếu có.
* `POST /api/v1/transfers/{id}/quarantine-reject` - Thủ kho đích hoặc Trưởng kho đích từ chối toàn bộ và đưa vào quarantine.
* `POST /api/v1/transfers/{id}/return-request` - Thủ kho đích báo cáo gửi nhầm SKU còn nguyên.
* `POST /api/v1/transfers/{id}/return-request/approve` - Trưởng kho đích duyệt cho tài xế quay đầu về kho nguồn.
* `POST /api/v1/transfers/{id}/return-request/reject` - Trưởng kho đích từ chối yêu cầu quay đầu với lý do.

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

### `POST /api/v1/transfers/{id}/quarantine-reject` Request
```json
{
  "rejectionReason": "Toàn bộ kiện hàng bị ướt sũng nước, không thể nhập kho"
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
* `QC_PASSED_BIN_MUST_NOT_BE_QUARANTINE` (HTTP 400): the selected `destinationLocationId` is a quarantine bin; QC-passed stock must go to a regular storage bin.
* `INVALID_DESTINATION_LOCATION` (HTTP 400): the selected `destinationLocationId` does not belong to the target warehouse or is inactive.
* `QUARANTINE_LOCATION_NOT_CONFIGURED` (HTTP 422): QC-failed quantity exists but the target warehouse has no active quarantine bin — validated at `receiveCheck` time.
* `DISCREPANCY_REQUIRES_REASON` (HTTP 400): shortage or final-level material issue outside normal QC failure exists without a reason.
* `TRANSFER_SPLIT_RECEIVE_NOT_SUPPORTED` (HTTP 409): actor attempts to finalize the same transfer through multiple independent receipt cycles.
* `RETURN_REQUEST_NOT_ALLOWED` (HTTP 409): wrong-SKU report is not allowed in the current transfer state.
* `WRONG_SKU_REASON_REQUIRED` (HTTP 400): expected/actual SKU, quantity, or reason is missing.
* `RETURN_REQUEST_REQUIRED` (HTTP 409): manager decision attempted before a Storekeeper report.
* `RETURN_APPROVAL_NOT_ALLOWED` (HTTP 403): actor is not the destination Warehouse Manager or lacks destination warehouse scope.

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
    * Calculate destination inventory quantity and value for 28 units only; the 2 missing units SHALL carry no amount in the destination receipt total.
    * Create no invoice, revenue, dealer receivable, supplier payable, or supplier Debit Note.

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
  * And the 2 quarantine units SHALL retain internal-transfer origin and be eligible only for disposal under spec 009.

* **Scenario: Shortage is not quarantine stock**
  * Given 30 units were sent and only 28 units physically arrived
  * When the destination completes receiving with the required shortage reason
  * Then the system SHALL create a `TRANSFER_DISCREPANCY` adjustment for -2 and SHALL NOT create two quarantine units or a disposal candidate.

* **Scenario: Physically damaged transfer goods map to disposal**
  * Given transfer goods physically arrive but are damaged and fail QC
  * When they are moved into the destination Quarantine bin
  * Then the system SHALL preserve transfer-item traceability, block RTV, and expose the physical damaged quantity to the spec 009 disposal workflow.

* **Scenario: Intact wrong SKU may return to source**
  * Given the wrong SKU arrives but remains intact and safe to transport
  * When destination Storekeeper reports `WRONG_SKU` with expected/actual SKU and destination Warehouse Manager approves
  * Then the assigned driver SHALL turn back with the same transfer/trip and the goods SHALL remain In-Transit.
  * And source Staff SHALL count, source Storekeeper SHALL check/QC, and source Warehouse Manager SHALL final-confirm the return.
  * And QC-passed quantity SHALL return to source regular inventory; any newly discovered damaged quantity SHALL enter source Quarantine; any shortage SHALL create a discrepancy adjustment.

* **Scenario: Block Storekeeper from self-approving wrong-SKU return**
  * Given destination Storekeeper submitted a `WRONG_SKU` return report
  * When the same Storekeeper attempts to approve vehicle return
  * Then the system SHALL reject the action because destination Warehouse Manager approval is required.

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

* **Scenario: Reject quarantine bin as destination for QC-passed stock**
  * Given a transfer is in `IN_TRANSIT` status and initial received counts exist
  * When Thủ kho HCM records `qcPassedQty = 8`, `qcFailedQty = 2`, and selects `WH-HCM-Q01` (a quarantine bin) as `destinationLocationId`
  * Then the system SHALL reject the receive-check request with `QC_PASSED_BIN_MUST_NOT_BE_QUARANTINE`.

* **Scenario: Reject receive-check when destination warehouse has no quarantine bin and QC failed**
  * Given a transfer is in `IN_TRANSIT` status, initial received counts exist, and the destination warehouse has no active quarantine bin
  * When Thủ kho đích records `qcFailedQty > 0`
  * Then the system SHALL reject the receive-check request with `QUARANTINE_LOCATION_NOT_CONFIGURED`.

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

## 7. Success Criteria

- 100% finalized shortages calculate destination inventory quantity and value only from physically received and accepted quantity.
- 0 internal-transfer shortages create invoices, receivables, payables, supplier Debit Notes, or automatic employee charges.
- 100% wrong-SKU returns require both destination Storekeeper reporting and destination Warehouse Manager approval.
- 100% approved wrong-SKU returns remain traceable through the original transfer, trip, vehicle, driver, and source receiving audit chain.
- Source users can complete the returned-goods count/check/QC/final-confirm sequence without creating a second transfer document.

## 8. Assumptions

- Destination inventory valuation applies only to physically received and accepted quantity.
- Employee or driver liability is decided only by a separate investigation and accounting approval process.
- A wrong-SKU return uses the currently assigned driver and vehicle; driver replacement after departure remains out of Sprint 1 scope.
- The wrong SKU is intact and safe to transport. Damaged goods use Quarantine and spec 009 disposal instead.
