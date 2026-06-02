# Feature: System Admin & Thủ kho Cấu hình Vị trí Kho & Sức chứa Kệ (US-WMS-20)

## 1. Context and Goal
Cấu hình vị trí kho theo cấu trúc phân cấp: Zone -> Rack -> Shelf -> Bin và thiết lập sức chứa (thể tích m3, khối lượng kg) để tối ưu không gian lưu trữ và kiểm soát tải trọng kệ khi cất hàng (Putaway).

## 2. Actors
* **Thủ kho**: Cấu hình vị trí kho, Bin location.
* **System Admin**: Quản trị hệ thống, phê duyệt cấu hình kho.

## 3. Functional Requirements (EARS)
* **Event-driven:**
  * WHEN a user creates a Bin Location, the system SHALL auto-generate the bin_code from format: `{warehouse_code}.{zone}.{rack}.{shelf}.{bin}`.
  * WHEN a user attempts to put away goods into a Bin, the system SHALL verify: `current_volume_m3 + incoming_volume_m3 ≤ capacity_m3` AND `current_weight_kg + incoming_weight_kg ≤ capacity_kg`.

## 4. API Endpoints
* `GET /api/v1/bin-locations` - Danh sách Bin location.
* `POST /api/v1/bin-locations` - Tạo mới Bin location.
* `PUT /api/v1/bin-locations/{id}` - Cập nhật Bin location.
* `GET /api/v1/bin-locations/{id}/capacity` - Xem sức chứa còn lại của Bin.

## 5. Acceptance Criteria
* **Scenario: Bin capacity constraint check**
  * Given a Bin with volume capacity 100m3 and current occupied volume 80m3
  * When putting away items with total volume 30m3
  * Then the system SHALL reject the operation with error `BIN_OVER_CAPACITY`.
