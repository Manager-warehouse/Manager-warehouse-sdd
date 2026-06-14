# Feature: Nhân viên kho Tiếp nhận & Đếm số lượng hàng thực tế (US-WMS-03)

## 1. Context and Goal
Nhân viên kho (WAREHOUSE_STAFF) tiếp nhận hàng hóa thực tế giao đến kho, đếm số lượng nhận được cho từng dòng hàng trong phiếu nhập, và nhập số lượng đếm được vào phiếu kiểm tra nhận hàng. Feature này chỉ bao phủ nghiệp vụ đếm số lượng và cập nhật `actual_qty`/`over_received_qty`; không kiểm tra chất lượng, không lấy mẫu QC, không ghi kết quả đạt/lỗi, không tạo batch, không tăng tồn kho, không đưa hàng vào quarantine, và không putaway.

Một phiếu nhận hàng chỉ được hoàn tất khi Nhân viên kho đã nhập `counted_qty > 0` cho tất cả dòng hàng có trong phiếu. Nếu sản phẩm không được giao trong lần nhận này thì không tạo dòng hàng có số lượng 0; Planner hoặc nghiệp vụ bổ sung sẽ tạo phiếu nhận riêng khi hàng đến sau. Sau khi đếm đủ, phiếu chuyển từ `PENDING_RECEIPT` sang `DRAFT` để feature QC inbound thực hiện kiểm tra chất lượng.

Nhân viên kho được sửa số lượng đã đếm khi phát hiện sai sót nếu trạng thái phiếu nhập chưa phải `APPROVED` hoặc `REJECTED`. Khi sửa số lượng sau khi đã có dữ liệu QC, hệ thống chỉ vô hiệu hóa dữ liệu QC cũ và đưa phiếu về `DRAFT`; việc kiểm tra chất lượng lại thuộc feature QC inbound.

## 2. Actors
* **Nhân viên kho (WAREHOUSE_STAFF)**: Tiếp nhận hàng thực tế, đếm đủ số lượng từng dòng hàng, nhập số lượng đếm được, và sửa sai số đếm trước khi Trưởng kho ra quyết định nhập kho.

## 3. Functional Requirements (EARS)
* **Event-driven:**
  * WHEN a Warehouse Staff submits physical counts for a receipt, the request SHALL include `counted_qty` for every receipt item in that receipt.
  * WHEN any receipt item is missing from the count request, the system SHALL reject the entire request and SHALL NOT save partial count changes.
  * WHEN any submitted receipt item has non-integer `counted_qty` or `counted_qty <= 0`, the system SHALL reject the entire request and SHALL NOT save partial count changes.
  * WHEN any submitted receipt item does not belong to the target receipt, the system SHALL reject the entire request.
  * WHEN a Warehouse Staff completes counting for a receipt in `PENDING_RECEIPT`, the system SHALL calculate and save `actual_qty` and `over_received_qty` for every receipt item, then update the receipt status to `DRAFT`.
  * WHEN `counted_qty` is equal to or less than `expected_qty`, the system SHALL store `actual_qty = counted_qty` and `over_received_qty = 0`.
  * WHEN `counted_qty` exceeds `expected_qty`, the system SHALL cap `actual_qty` at `expected_qty` and record the excess as `over_received_qty`.
  * WHEN counted quantity differs from expected quantity, the system SHALL preserve `expected_qty` and retain the variance for Storekeeper review and Trưởng kho approval.
  * WHEN a Warehouse Staff corrects previous counts before Trưởng kho approval/rejection, the correction request SHALL include `counted_qty` for every receipt item, and the system SHALL recalculate `actual_qty` and `over_received_qty`.
  * WHEN count correction changes a receipt that already has QC data, the system SHALL clear `qc_result`, `sample_qty`, `sample_passed_qty`, `sample_failed_qty`, `qc_sampling_method`, and `qc_failure_reason`, then return the receipt to `DRAFT` so QC must be performed again.
  * WHEN physical counts are saved or corrected, the system SHALL create a `RECEIPT_RECEIVE` audit log with actor, action, entity type, entity id/code, timestamp, before state, and after state.
* **State-driven:**
  * WHILE a receipt is `PENDING_RECEIPT`, the system SHALL allow Warehouse Staff to submit complete physical counts.
  * WHILE a receipt is `DRAFT`, `QC_COMPLETED`, or `QC_FAILED`, the system SHALL allow Warehouse Staff to correct physical counts.
  * WHILE a receipt is `APPROVED` or `REJECTED`, the system SHALL reject count changes.
  * WHILE receiving/counting is being recorded, the system SHALL NOT create or update batch records, regular inventory, quarantine inventory, or bin locations.
  * WHILE goods require quality inspection, that inspection SHALL be handled by the QC inbound feature, not by this counting feature.

## 4. API Endpoints
* `PUT /api/v1/receipts/{id}/receive` - Nhân viên kho nhập hoặc sửa số lượng đếm thực tế cho toàn bộ dòng hàng của phiếu nhận.

### Request Payload
```json
{
  "items": [
    {
      "receipt_item_id": 101,
      "counted_qty": 98
    },
    {
      "receipt_item_id": 102,
      "counted_qty": 120
    }
  ]
}
```

### Payload Rules
* `items` SHALL contain exactly one entry for every receipt item in the target receipt.
* `receipt_item_id` SHALL belong to the target receipt.
* `counted_qty` SHALL be an integer greater than 0.
* Duplicate `receipt_item_id` values SHALL be rejected.

## 5. Error Handling
| Error | HTTP | Condition |
|-------|------|-----------|
| INVALID_RECEIPT_COUNT | 422 | Any non-integer `counted_qty`, `counted_qty <= 0`, duplicate item, or item not belonging to the receipt |
| RECEIPT_COUNT_INCOMPLETE | 422 | The request does not include every receipt item in the target receipt |
| INVALID_RECEIPT_STATUS | 409 | Receipt status does not allow receiving/count correction |
| RECEIPT_ALREADY_FINALIZED | 409 | Receipt status is already `APPROVED` or `REJECTED` |

## 6. Acceptance Criteria
* **Scenario: Warehouse Staff records complete receipt counts**
  * Given a receipt in `PENDING_RECEIPT` state with two receipt items
  * When Nhân viên kho submits `counted_qty > 0` for every receipt item
  * Then the system SHALL save `actual_qty` and `over_received_qty` for every item and update the receipt status to `DRAFT`.
  * And the system SHALL create a `RECEIPT_RECEIVE` audit log.
  * And the system SHALL NOT create batch records, increase regular inventory, increase quarantine inventory, or assign bin locations.

* **Scenario: Reject incomplete receipt counts**
  * Given a receipt in `PENDING_RECEIPT` state with multiple receipt items
  * When Nhân viên kho submits counts for only some receipt items
  * Then the system SHALL reject the request with `RECEIPT_COUNT_INCOMPLETE`, save no submitted line changes, and keep the receipt status as `PENDING_RECEIPT`.

* **Scenario: Reject non-positive receipt counts**
  * Given a receipt in `PENDING_RECEIPT` state
  * When Nhân viên kho submits `counted_qty = 0` or a negative quantity for any item
  * Then the system SHALL reject the request with `INVALID_RECEIPT_COUNT`, save no submitted line changes, and keep the receipt status as `PENDING_RECEIPT`.

* **Scenario: Handle shortage against expected quantity**
  * Given a receipt item has `expected_qty = 100`
  * When Nhân viên kho counts 90 units and submits the complete receipt count
  * Then the system SHALL save `actual_qty = 90`, `over_received_qty = 0`, preserve `expected_qty = 100`, and retain a shortage variance of 10 units for Storekeeper review.

* **Scenario: Handle over-received quantity**
  * Given a receipt item has `expected_qty = 100`
  * When Nhân viên kho counts 120 units and submits the complete receipt count
  * Then the system SHALL save `actual_qty = 100`, record `over_received_qty = 20`, and preserve `expected_qty = 100`.
  * And the system SHALL NOT create quarantine inventory or assign a quarantine/holding location for the excess 20 units until Trưởng kho makes the final approve/reject decision for the receipt.

* **Scenario: Correct receipt counts before Trưởng kho decision**
  * Given a receipt is `DRAFT`, `QC_COMPLETED`, or `QC_FAILED`
  * And the receipt status is not `APPROVED` or `REJECTED`
  * When Nhân viên kho submits corrected `counted_qty` values for every receipt item
  * Then the system SHALL recalculate and save `actual_qty` and `over_received_qty`, create a `RECEIPT_RECEIVE` audit log, and keep or return the receipt status to `DRAFT`.

* **Scenario: Correct receipt counts after QC data exists**
  * Given a receipt has QC sample data recorded or confirmed
  * And Trưởng kho has not approved/rejected the receipt
  * When Nhân viên kho submits corrected `counted_qty` values for every receipt item
  * Then the system SHALL recalculate the count quantities, clear the previous QC fields, return the receipt status to `DRAFT`, and require QC to be performed again by the QC inbound feature.

* **Scenario: Prevent count correction after final manager decision**
  * Given a receipt is `APPROVED` or `REJECTED`
  * When Nhân viên kho tries to correct `counted_qty`
  * Then the system SHALL reject the request with `RECEIPT_ALREADY_FINALIZED`.
