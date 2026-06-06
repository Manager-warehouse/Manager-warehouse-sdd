# Feature: Quản lý Danh mục Kho & Cấu hình Vị trí, Sức chứa Kệ (US-WMS-20)

## 1. Context and Goal
Quản lý danh mục kho bãi (gồm 3 kho vật lý tại Hải Phòng, Hà Nội, Hồ Chí Minh và 1 kho ảo In-Transit) và cấu hình sơ đồ vị trí lưu trữ trong kho theo cấu trúc phân cấp ZONE -> BIN. Đồng thời, thiết lập thuộc tính và sức chứa tối đa (thể tích m3, khối lượng kg) ở cấp BIN để phục vụ kiểm soát tải trọng khi cất hàng (Putaway) và cách ly hàng lỗi (Quarantine).

## 2. Actors
* **Thủ kho kiêm QC**: Cấu hình vị trí kho, Bin location, theo dõi sức chứa kệ.
* **Trưởng kho (Checker)**: Phê duyệt cấu hình sơ đồ kho vật lý.
* **CEO (Checker cấp cao)**: Phê duyệt các thay đổi cấu hình hệ thống quan trọng (như tạo thêm kho mới).

## 3. Functional Requirements (EARS)
* **State-driven:**
  * WHILE a warehouse status is inactive (`is_active = false`), the system SHALL prevent any new transactions (receipt, issue, transfer, adjustment) referencing that warehouse.
  * WHILE a location (Zone/Bin) status is inactive (`is_active = false`), the system SHALL exclude it from all active operations and dropdowns.
  * WHILE a warehouse or bin location has active stock (occupied volume > 0 or weight > 0 or has active inventory balance), the system SHALL reject any attempt to deactivate it (`is_active = false`).
* **Event-driven:**
  * WHEN a user creates a Warehouse, the system SHALL require: a unique code (e.g., HP, HN, HCM, IN_TRANSIT), name, address, phone, manager_id (FK to users, which MUST be a user with the role of `Trưởng kho`), and type (`PHYSICAL` or `IN_TRANSIT`).
  * WHEN a user creates a Zone, the system SHALL require a unique zone code within that warehouse and auto-generate the globally unique `code` column in format: `{warehouse_code}.{zone_code}` (e.g. `HP.A`).
  * WHEN a user creates a Bin Location, the system SHALL require a parent Zone belonging to the same warehouse and auto-generate the globally unique `code` column in format: `{warehouse_code}.{zone_code}.{bin}` (e.g. `HP.A.01.1.01`).
  * WHEN a user creates or updates a location, the system SHALL reject any hierarchy other than `ZONE -> BIN`.
  * WHEN a user attempts to put away goods into a Bin, the system SHALL verify that: `current_volume_m3 + incoming_volume_m3 <= capacity_m3` AND `current_weight_kg + incoming_weight_kg <= capacity_kg`.
  * WHEN a location is configured with `is_quarantine = true`, the system SHALL flag it as a Quarantine Zone/Bin and restrict its stock from being allocated to normal delivery orders.
* **System-driven:**
  * The system SHALL automatically create a default Zone (type = `'ZONE'`, e.g., code `IN_TRANSIT.ZONE`) and a default Bin (type = `'BIN'`, parent pointing to the default Zone, e.g., code `IN_TRANSIT.BIN`) for any virtual/in-transit warehouse (`type = 'IN_TRANSIT'`), and bypass all zone/bin capacity checks for this location.

## 4. API Endpoints
### Warehouses
* `GET /api/v1/warehouses` - Danh sách kho bãi (hỗ trợ lọc active/inactive).
* `POST /api/v1/warehouses` - Tạo mới kho bãi (Yêu cầu quyền CEO/Trưởng kho).
* `PUT /api/v1/warehouses/{id}` - Cập nhật thông tin kho bãi (kiểm tra phân quyền manager_id).
* `DELETE /api/v1/warehouses/{id}` - Vô hiệu hóa kho bãi (soft-delete, `is_active = false`, chặn nếu còn hàng).

### Bin Locations & Zones
* `GET /api/v1/bin-locations` - Danh sách vị trí lưu trữ (hỗ trợ lọc theo warehouse_id, type, is_quarantine, is_active).
* `POST /api/v1/bin-locations` - Tạo mới Zone hoặc Bin location.
* `PUT /api/v1/bin-locations/{id}` - Cập nhật Zone hoặc Bin location.
* `DELETE /api/v1/bin-locations/{id}` - Vô hiệu hóa vị trí (soft-delete, `is_active = false`, chặn nếu còn hàng).
* `GET /api/v1/bin-locations/{id}/capacity` - Xem sức chứa còn lại và hiện tại của Bin.

## 5. Acceptance Criteria
* **Scenario: Parent zone and bin must belong to the same warehouse**
  * Given a Zone A in Warehouse `WH-HP` and a Zone B in Warehouse `WH-HN`
  * When a user attempts to create a Bin in Warehouse `WH-HP` but sets its parent to Zone B
  * Then the system SHALL reject the operation with a validation error.

* **Scenario: Bin capacity constraint check**
  * Given a Bin with volume capacity 100m3 and current occupied volume 80m3
  * When putting away items with total volume 30m3
  * Then the system SHALL reject the operation with error `BIN_OVER_CAPACITY`.

* **Scenario: Prevent deactivation of bin location with stock**
  * Given a Bin location `WH-HP.ZONEA.BIN01` with occupied volume 10m3
  * When a user attempts to set `is_active = false` for this Bin
  * Then the system SHALL reject the request with a validation error `LOCATION_HAS_STOCK`.
