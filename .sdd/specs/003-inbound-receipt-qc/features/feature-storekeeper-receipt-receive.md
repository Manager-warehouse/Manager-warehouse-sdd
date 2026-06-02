# Feature: Thủ kho Tiếp nhận & Đếm hàng Thực tế (US-WMS-03)

## 1. Context and Goal
Thủ kho chịu trách nhiệm tiếp nhận hàng hóa thực tế giao đến kho, đếm số lượng thực tế nhận được và cập nhật vào lệnh nhập nháp để chuẩn bị cho quy trình kiểm tra chất lượng (QC).

## 2. Actors
* **Thủ kho**: Tiếp nhận hàng thực tế, thực hiện đếm và cập nhật số lượng thực tế.

## 3. Functional Requirements (EARS)
* **Event-driven:**
  * WHEN a Thủ kho enters the actual received quantity, the system SHALL update the receipt status to `DRAFT` (indicating actual counts are updated and ready for QC inspection).
  * WHEN a Thủ kho completes the putaway process after final approval, the system SHALL verify: `current_volume_m3 + incoming_volume_m3 <= capacity_m3` AND `current_weight_kg + incoming_weight_kg <= capacity_kg` before updating locations.

## 4. API Endpoints
* `PUT /api/v1/receipts/{id}/receive` - Thủ kho cập nhật số đếm thực tế của lô hàng.
* `PUT /api/v1/receipts/{id}/complete` - Xác nhận cất hàng vào Bin Location (Putaway).

## 5. Acceptance Criteria
* **Scenario: Storekeeper records receipt counts**
  * Given a receipt in `PENDING_RECEIPT` state with expected quantity of 100
  * When Thủ kho counts 98 units physically and submits the actual count
  * Then the system SHALL save the count (98) and update the receipt status to `DRAFT`.
