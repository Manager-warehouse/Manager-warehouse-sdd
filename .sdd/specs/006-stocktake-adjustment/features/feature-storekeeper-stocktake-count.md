# Feature: Thủ kho Kiểm kê kho & Đếm hàng Thực tế (US-WMS-13)

## 1. Context and Goal
Thủ kho tạo phiếu kiểm kê định kỳ, hệ thống khóa sổ vị trí ô kệ để tránh giao dịch phát sinh trong lúc đếm. Khi nhập kết quả đếm, hệ thống tự tính chênh lệch để chuẩn bị trình duyệt.

## 2. Actors
* **Thủ kho**: Lập phiếu kiểm kê, bắt đầu kiểm kê (khóa kệ) và nhập số đếm thực tế.

## 3. Functional Requirements (EARS)
* **Event-driven:**
  * WHEN a Thủ kho creates a stocktake document and starts it, the system SHALL lock the target warehouse locations from other transactions.
  * WHEN a Thủ kho enters counted quantities, the system SHALL auto-calculate the variance (`Variance = counted_qty - system_qty`) and total variance value (`variance_value = variance_qty × cost_price`).
* **State-driven:**
  * WHILE a stocktake is `IN_PROGRESS`, the system SHALL prevent any receipt, delivery order, or transfer transactions on the locked warehouse locations.

## 4. API Endpoints
* `POST /api/v1/stocktakes` - Lập phiếu kiểm kê (STORE_KEEPER).
* `PUT /api/v1/stocktakes/{id}/start` - Bắt đầu kiểm kê (khóa vị trí kệ).
* `PUT /api/v1/stocktakes/{id}/count` - Nhập kết quả đếm thực tế.
* `PUT /api/v1/stocktakes/{id}/complete` - Hoàn tất đếm, trình duyệt.

## 5. Acceptance Criteria

**Scenario 1: Auto-calculation of variance**
* Given product A has system quantity 100 and cost price 50,000 VND
* When Thủ kho enters actual counted quantity 90
* Then the system SHALL calculate variance = -10 and variance_value = -500,000 VND.

**Scenario 2: Lock warehouse locations during count**
* Given a stocktake is `IN_PROGRESS` on location `WH-HP.A.01.1.01`
* When a user attempts to create a DO picking from this location
* Then the system SHALL block the transaction with location locked error.
