# Feature: Nhân viên kho Tiếp nhận & Đếm hàng Thực tế (US-WMS-03)

## 1. Context and Goal
Nhân viên kho (WAREHOUSE_STAFF) chịu trách nhiệm tiếp nhận hàng hóa thực tế giao đến kho, kiểm đếm số lượng thực tế nhận được và cập nhật vào lệnh nhập nháp để chuẩn bị cho quy trình kiểm tra chất lượng (QC). Storekeeper xác nhận kết quả QC sang `QC_COMPLETED`; Trưởng kho là người duyệt hoặc từ chối phiếu nhập kho.

## 2. Actors
* **Nhân viên kho (WAREHOUSE_STAFF) kiêm QC Staff**: Tiếp nhận hàng thực tế, kiểm đếm và cập nhật số lượng thực tế để tạo bản nháp `DRAFT`.
* **Storekeeper (STOREKEEPER)**: Rà soát và kết luận kết quả QC, xác nhận phiếu sang `QC_COMPLETED`; không duyệt phiếu nhập kho chính thức.

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * The system SHALL create a `RECEIPT_RECEIVE` audit log entry whenever actual received quantities are recorded.
  * The system SHALL create a `RECEIPT_PUTAWAY_COMPLETE` audit log entry whenever putaway location assignment is completed.
* **Event-driven:**
  * WHEN a Warehouse Staff enters the actual received quantity, the system SHALL update the receipt status to `DRAFT` (indicating actual counts are updated and ready for QC inspection).
  * WHEN a Storekeeper completes the putaway process after final approval, the system SHALL verify: `current_volume_m3 + incoming_volume_m3 <= capacity_m3` AND `current_weight_kg + incoming_weight_kg <= capacity_kg` before updating locations.

## 4. API Endpoints
* `PUT /api/v1/receipts/{id}/receive` - Nhân viên kho cập nhật số đếm thực tế của lô hàng.
* `PUT /api/v1/receipts/{id}/complete` - Xác nhận cất hàng vào Bin Location (Putaway).

## 5. Acceptance Criteria
* **Scenario: Warehouse Staff records receipt counts**
  * Given a receipt in `PENDING_RECEIPT` state with expected quantity of 100
  * When Nhân viên kho counts 98 units physically and submits the actual count
  * Then the system SHALL save the count (98) and update the receipt status to `DRAFT`.
