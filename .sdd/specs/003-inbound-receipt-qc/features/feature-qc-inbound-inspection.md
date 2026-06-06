# Feature: Thủ kho Kiểm tra Chất lượng Inbound (US-WMS-03)

## 1. Context and Goal
Thủ kho kiêm QC kiểm tra ngoại quan và chất lượng từng sản phẩm nhập về. Hàng hóa đạt chuẩn được giữ để Trưởng kho phê duyệt nhập kho, hàng hóa lỗi QC bắt buộc phải tách riêng và chuyển vào khu cách ly (Quarantine Zone).

## 2. Actors
* **Thủ kho kiêm QC**: Thực hiện kiểm QC và ghi nhận số lượng đạt/lỗi cùng lý do.

## 3. Functional Requirements (EARS)
* **Event-driven:**
  * WHEN a QC staff submits QC results on a `DRAFT` receipt, the system SHALL:
    * Split quantity into: `qc_passed_qty` (ready for approval) and `qc_failed_qty` (to quarantine zone).
    * Set `qc_result` status: `PENDING`/`PASSED`/`FAILED`/`PARTIAL`.
    * IF `qc_failed_qty > 0`, the system SHALL flag the receipt and automatically route the failed quantity to a Quarantine location (`is_quarantine = true`).

## 4. API Endpoints
* `PUT /api/v1/receipts/{id}/qc` - Nhập kết quả QC Đạt/Lỗi và lý do cụ thể cho hàng lỗi.

## 5. Acceptance Criteria
* **Scenario: Split passed and failed goods**
  * Given a receipt in `DRAFT` state with actual count 100 units
  * When QC staff updates QC with 80 units passed and 20 units failed (móp méo)
  * Then the system SHALL route 20 failed units to the Quarantine Zone and hold 80 passed units under status `QC_COMPLETED` for approval.
