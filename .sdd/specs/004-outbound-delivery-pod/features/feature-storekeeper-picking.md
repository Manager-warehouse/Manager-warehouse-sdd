# Feature: Thủ kho Soạn hàng tại Kệ (US-WMS-07)

## 1. Context and Goal
Thủ kho lấy hàng từ kệ theo chỉ định (Picking) được sinh bởi hệ thống dựa trên FIFO theo ngày nhận hàng, đảm bảo soạn đúng sản phẩm và số lượng từ các vị trí Bin Location chính xác.

## 2. Actors
* **Thủ kho**: Soạn hàng tại kệ, lấy đúng SKU và số lượng, cập nhật trạng thái soạn hàng.

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * The system SHALL create `DELIVERY_ORDER_PICK_START` and `DELIVERY_ORDER_PICK_COMPLETE` audit log entries for picking state changes and picked quantities.
* **Event-driven:**
  * WHEN a Thủ kho starts picking, the system SHALL update the DO status to `PICKING`.
  * WHEN a Thủ kho finishes picking all items, the system SHALL mark the items as picked and wait for QC inspection.

## 4. API Endpoints
* `PUT /api/v1/delivery-orders/{id}/pick` - Bắt đầu soạn hàng (STORE_KEEPER).
* `PUT /api/v1/delivery-orders/{id}/picked` - Xác nhận đã soạn hàng xong (STORE_KEEPER).

## 5. Acceptance Criteria
* **Scenario: Start picking order**
  * Given a delivery order in `NEW` status
  * When Thủ kho clicks "Bắt đầu soạn hàng"
  * Then the system SHALL change DO status to `PICKING` and lock the items for picking.
