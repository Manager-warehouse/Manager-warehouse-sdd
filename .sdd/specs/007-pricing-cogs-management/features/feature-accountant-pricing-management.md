# Feature: Kế toán Thiết lập Bảng giá & Lịch sử Biến động giá (US-WMS-14)

## 1. Context and Goal
Quản lý bảng giá bán cho Đại lý và giá vốn hàng hóa theo kỳ kinh doanh có ngày hiệu lực. Kế toán viên nhập bảng giá và Kế toán trưởng duyệt để lưu vết lịch sử giá trong `price_history` làm căn cứ tính toán tài chính.

## 2. Actors
* **Kế toán viên (Maker)**: Nhập bảng giá vốn + giá bán, gửi duyệt.
* **Kế toán trưởng (Checker)**: Phê duyệt bảng giá chính thức.

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * The system SHALL always record price changes in the `price_history` table with `effective_date`, `end_date`, `cost_price`, `selling_price`, `status`, and actor info.
* **Event-driven:**
  * WHEN a Kế toán viên creates a price list entry, the system SHALL require: product_id, cost_price, selling_price, effective_date, and end_date. Status is set to `PENDING`.
  * WHEN a Kế toán viên submits a price entry for approval, the system SHALL notify Kế toán trưởng.
  * WHEN a Kế toán trưởng approves a price entry, the system SHALL:
    * Set status to `APPROVED`.
    * Set approved_by and approved_at.
* **State-driven:**
  * WHILE a price history record is `PENDING`, the system SHALL NOT use it for any transaction.

## 4. API Endpoints
* `POST /api/v1/price-lists` - Tạo bảng giá mới (Kế toán viên).
* `PUT /api/v1/price-lists/{id}/approve` - Duyệt bảng giá (Kế toán trưởng).
* `GET /api/v1/products/{id}/price-history` - Xem lịch sử giá của sản phẩm.

## 5. Acceptance Criteria

**Scenario: Price List Approval**
* Given a price list in `PENDING` status submitted by Kế toán viên
* When Kế toán trưởng approves it
* Then the system SHALL change status to `APPROVED`, update `price_history`, and make it available for transactions.
