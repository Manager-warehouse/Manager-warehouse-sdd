# Feature: Điều phối viên Quản lý Xe tải & Tài xế Nội bộ (US-WMS-23)

## 1. Context and Goal
Quản lý đội xe tải nội bộ của Phúc Anh và danh sách tài xế, theo dõi trạng thái rảnh/bận/bảo trì để phục vụ công tác điều phối chuyến xe giao hàng và điều chuyển kho.

## 2. Actors
* **Dispatcher (Người điều phối)**: Quản lý thông tin phương tiện, tài xế và theo dõi trạng thái phân công.

## 3. Functional Requirements (EARS)
* **State-driven:**
  * WHILE a vehicle status is `'MAINTENANCE'` or `'ON_TRIP'`, the system SHALL exclude it from trip assignment dropdowns.
  * WHILE a driver status is `'MAINTENANCE'` or `'ON_DELIVERY'`, the system SHALL exclude them from trip assignment dropdowns.

## 4. API Endpoints
* `GET /api/v1/vehicles` - Xem danh sách xe.
* `POST /api/v1/vehicles` - Thêm mới xe.
* `GET /api/v1/drivers` - Xem danh sách tài xế.
* `POST /api/v1/drivers` - Thêm mới tài xế.

## 5. Acceptance Criteria
* **Scenario: Exclude busy or maintenance vehicle**
  * Given a vehicle with status `MAINTENANCE`
  * When a Dispatcher creates a trip and looks up the vehicle selection
  * Then the system SHALL NOT list this vehicle in the options.
