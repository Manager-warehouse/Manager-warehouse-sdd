# Feature: Trưởng kho & Planner Cảnh báo tự động Tồn kho dưới định mức (US-WMS-26)

## 1. Context and Goal
Trưởng kho và Planner cần được cảnh báo kịp thời khi tồn kho khả dụng của sản phẩm chạm ngưỡng tối thiểu để lên kế hoạch tái nhập hoặc điều chuyển kho.

## 2. Actors
* **Trưởng kho**: Nhận thông báo in-app (High Priority) khi kho mình phụ trách bị thiếu hàng.
* **Planner**: Nhận thông báo và xem đánh dấu đỏ trên Planning Dashboard để điều phối hàng.

## 3. Functional Requirements (EARS)
* **Event-driven:**
  * WHEN available inventory of a product at a warehouse drops below its `reorder_point` (or falls below the default system configuration `MIN_INVENTORY_WARNING_THRESHOLD` if the product's `reorder_point` is null), the system SHALL:
    * Create or update an active record in `stock_alerts` table.
    * Send an in-app notification (High Priority) to the assigned Trưởng kho and Planner.
    * Highlight the product in red on the Dashboard.
  * WHEN a low stock situation is resolved (`available stock >= reorder_point` or `>= MIN_INVENTORY_WARNING_THRESHOLD` fallback), the system SHALL set `is_resolved = true` and update `resolved_at` in the `stock_alerts` record.

## 4. API Endpoints
* `GET /api/v1/alerts/low-stock` - Xem danh sách cảnh báo tồn kho thấp.

## 5. Acceptance Criteria
* **Scenario: Low stock notification**
  * Given a product has `reorder_point = 100` and current available inventory HN = 110
  * When a DO is departing and HN inventory drops to 80
  * Then the system SHALL create an active alert in `stock_alerts` and send a High Priority notification to the HN Trưởng kho and Planner.
