# Feature: Thủ kho Quản lý SKU & Danh mục Sản phẩm (US-WMS-19)

## 1. Context and Goal
Quản lý danh mục sản phẩm (SKU) tập trung làm nền tảng đồng bộ dữ liệu hàng hóa trên toàn hệ thống 3 kho, hỗ trợ một quy đổi cố định từ thùng sang cái. Domain Sprint 1 là hàng gia dụng nên không quản lý serial từng sản phẩm và không quản lý hạn sử dụng.

## 2. Actors
* **Thủ kho**: Tạo mới, cập nhật và quản lý danh mục sản phẩm.
* **System Admin**: Phê duyệt vô hiệu hóa sản phẩm (`is_active = false`) khi cần kiểm soát thay đổi danh mục.

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * The system SHALL always validate that a product SKU is unique before creation.
* **Event-driven:**
  * WHEN a user creates a product, the system SHALL require: SKU, name, unit, and optional fields: description, weight_kg, volume_m3, unit_per_pack, reorder_point.
  * WHEN a user configures package conversion, the system SHALL support only `thùng → cái` using `unit_per_pack` as the number of `cái` in one `thùng`.
* **State-driven:**
  * WHILE a product is `is_active = false`, the system SHALL prevent new transactions (receipt, issue, transfer) referencing that product.
* **Optional:**
  * WHERE a product has `reorder_point` set and available inventory drops below it, the system SHALL trigger a low-stock alert.

## 4. API Endpoints
* `GET /api/v1/products` - Xem danh sách sản phẩm.
* `POST /api/v1/products` - Tạo mới sản phẩm.
* `GET /api/v1/products/{id}` - Chi tiết sản phẩm.
* `PUT /api/v1/products/{id}` - Cập nhật sản phẩm.
* `DELETE /api/v1/products/{id}` - Vô hiệu hóa sản phẩm (soft-delete).

## 5. Acceptance Criteria
* **Scenario: Product does not require serial or expiry fields**
  * Given a user creates a household-goods product
  * When they submit SKU, name, unit, and optional packaging/capacity fields
  * Then the system SHALL create the product without requiring serial tracking or expiry fields.
