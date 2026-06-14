# Feature: Điều phối viên Lập Chuyến xe & Vận chuyển Nội bộ (US-WMS-08)

## 1. Context and Goal
Dispatcher gom nhiều đơn hàng và gán xe, tài xế nội bộ để lập Chuyến xe (Trip Log). Khi xe xuất phát rời kho, hệ thống chuyển tồn kho từ kho vật lý sang kho ảo In-Transit và chuyển trạng thái hàng sang In-Transit.

## 2. Actors
* **Dispatcher (Người điều phối)**: Lập chuyến xe, gán xe và tài xế, sắp xếp Stop Order.
* **Tài xế**: Xác nhận nhận hàng và xuất phát rời kho.

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * The system SHALL always release reserved inventory when a delivery order transitions to `IN_TRANSIT`.
* **Event-driven:**
  * WHEN a Dispatcher creates a trip, the system SHALL:
    * Validate that all selected DOs are in `READY_TO_SHIP` status.
    * Validate total weight and volume of DO items against vehicle maximum weight/volume capacity.
  * WHEN a Tài xế confirms departure (goods loaded, vehicle leaves warehouse), the system SHALL:
    * Set `issued_qty = requested_qty` for Sprint 1 full-shipment delivery orders.
    * Update source warehouse inventories: `total_qty -= issued_qty` and `reserved_qty -= issued_qty`.
    * Increase virtual In-Transit warehouse inventories: `total_qty += issued_qty`.
    * Update DO status to `IN_TRANSIT` and Trip status to `IN_TRANSIT`.

## 4. API Endpoints
* `POST /api/v1/trips` - Tạo chuyến xe mới (giao diện Dispatcher).
* `PUT /api/v1/trips/{id}/depart` - Xác nhận xuất phát chuyến xe (Tài xế).

## 5. Acceptance Criteria
* **Scenario: Stock deduction at departure**
  * Given a trip loaded with DO items, where the DO items currently hold `reserved_qty = 50` and `total_qty = 100`
  * When the driver clicks "Xác nhận nhận hàng & Xuất phát"
  * Then the system SHALL change DO status to `IN_TRANSIT`, decrease source warehouse `total_qty` by 50, decrease source warehouse `reserved_qty` by 50, and increase virtual In-Transit inventory by 50.
