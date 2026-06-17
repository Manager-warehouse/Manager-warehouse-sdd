# Feature: Điều phối viên Quản lý Xe tải & Tài xế Nội bộ (US-WMS-23)

## 1. Context and Goal
Quản lý đội xe tải nội bộ của Phúc Anh và danh sách tài xế, theo dõi trạng thái xe (AVAILABLE/ON_TRIP/MAINTENANCE) và tài xế (AVAILABLE/ON_TRIP/UNAVAILABLE) để phục vụ công tác điều phối chuyến xe giao hàng và điều chuyển kho. Đảm bảo loại bỏ xe/tài xế không sẵn sàng hoặc không hợp lệ (như hết hạn bằng lái) và ngăn chặn gán trùng lịch (double booking) trong các chuyến xe chưa hoàn thành.

## 2. Actors
* **Dispatcher (Người điều phối)**: Quản lý thông tin phương tiện, tài xế và theo dõi trạng thái phân công chuyến xe.

## 3. Functional Requirements (EARS)
* **State-driven:**
  * WHILE a vehicle status is `'MAINTENANCE'` or `'ON_TRIP'`, the system SHALL exclude it from trip assignment dropdowns.
  * WHILE a driver status is `'ON_TRIP'` or `'UNAVAILABLE'`, the system SHALL exclude them from trip assignment dropdowns.
  * WHILE a driver's `license_expiry` is in the past, the system SHALL force their status to `'UNAVAILABLE'` and exclude them from trip assignments.
  * WHILE a vehicle or driver has `is_active = false` (soft-deleted), the system SHALL exclude them from all active operations and dropdown selection lists.
  * WHILE a vehicle or driver is already assigned to any incomplete trip (trip status is not `COMPLETED` or `CANCELLED`), the system SHALL exclude them from trip assignment dropdowns to prevent double booking.
* **Event-driven:**
  * WHEN a user creates a Vehicle, the system SHALL require: warehouse_id, plate_number (unique), vehicle_type, and max_weight_kg (positive value). The `max_volume_m3` is optional (nullable), but if provided, it MUST be a positive value.
  * WHEN a user creates a Driver, the system SHALL require: warehouse_id, user_id (unique FK to users, which MUST belong to a user account with role `DRIVER`), full_name, license_number (unique), and license_expiry. The contact `phone` number is optional (nullable) and may inherit/fallback to the phone number of the associated user account.
  * WHEN a vehicle or driver is used for outbound trip planning, the system SHALL validate that its `warehouse_id` matches the trip warehouse.
  * WHEN a trip status changes to `'IN_TRANSIT'`, the system SHALL automatically set the status of the assigned vehicle and driver to `'ON_TRIP'`.
  * WHEN a trip is `COMPLETED` or cancelled, the system SHALL automatically restore the status of the assigned vehicle and driver to `'AVAILABLE'`.
  * WHEN a trip is created, the system SHALL store the trip purpose in `trips.trip_type` with value `DELIVERY` or `TRANSFER` instead of encoding it in vehicle or driver status.
  * WHEN a user deactivates a driver profile (`is_active = false`), the system SHALL automatically deactivate the associated system user account (`users.is_active = false`).

## 4. API Endpoints
### Vehicles
* `GET /api/v1/vehicles` - Xem danh sách xe (hỗ trợ lọc status và is_active).
* `POST /api/v1/vehicles` - Thêm mới xe.
* `PUT /api/v1/vehicles/{id}` - Cập nhật thông tin xe.
* `PATCH /api/v1/vehicles/{id}/status` - Cập nhật nhanh trạng thái xe (ví dụ: chuyển sang MAINTENANCE).
* `DELETE /api/v1/vehicles/{id}` - Vô hiệu hóa xe (soft-delete, `is_active = false`).

### Drivers
* `GET /api/v1/drivers` - Xem danh sách tài xế (hỗ trợ lọc status và is_active).
* `POST /api/v1/drivers` - Thêm mới tài xế (liên kết với user_id).
* `PUT /api/v1/drivers/{id}` - Cập nhật thông tin tài xế.
* `PATCH /api/v1/drivers/{id}/status` - Cập nhật nhanh trạng thái tài xế (ví dụ: chuyển sang UNAVAILABLE).
* `DELETE /api/v1/drivers/{id}` - Vô hiệu hóa tài xế (soft-delete, `is_active = false` và khóa tài khoản users tương ứng).

## 5. Acceptance Criteria
* **Scenario: Exclude busy or maintenance vehicle**
  * Given a vehicle with status `MAINTENANCE`
  * When a Dispatcher creates a trip and looks up the vehicle selection
  * Then the system SHALL NOT list this vehicle in the options.

* **Scenario: Prevent double booking of driver on pending trips**
  * Given a driver `Nguyen Van B` who is assigned to an incomplete trip `TRIP-01` (status `NEW`)
  * When a Dispatcher creates another trip `TRIP-02` and looks up the driver selection list
  * Then the system SHALL NOT list `Nguyen Van B` in the options.

* **Scenario: Exclude driver with expired license**
  * Given a driver whose `license_expiry` date was 5 days ago
  * When the system checks active drivers for a trip assignment
  * Then the system SHALL force the driver status to `UNAVAILABLE` and exclude them from selection.

* **Scenario: Deactivate driver profile also deactivates user account**
  * Given an active driver profile associated with user ID `10`
  * When a Dispatcher deactivates the driver profile (`DELETE /api/v1/drivers/{id}`)
  * Then the system SHALL set `is_active = false` for both the driver profile and the user account with ID `10`.
