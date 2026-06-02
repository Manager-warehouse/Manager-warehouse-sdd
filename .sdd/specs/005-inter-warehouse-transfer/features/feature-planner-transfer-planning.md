# Feature: Planner Lập Kế hoạch Điều chuyển kho tự động (US-WMS-11)

## 1. Context and Goal
Planner sử dụng Planning Dashboard để hệ thống tự động quét tồn khả dụng của 3 kho vật lý và gợi ý các lệnh điều chuyển tối ưu nhằm tránh đứt gãy hàng hóa. Planner sau đó phê duyệt gợi ý để tạo nhanh phiếu điều chuyển.

## 2. Actors
* **Planner (Người lập kế hoạch)**: Xem gợi ý điều chuyển và nhấn nút tạo nhanh phiếu điều chuyển.

## 3. Functional Requirements (EARS)
* **Optional:**
  * WHERE the Planning Dashboard is enabled, the system SHALL compare available inventory of each product at each warehouse against the configured minimum stock threshold and display replenishment suggestions.

## 4. API Endpoints
* `GET /api/v1/planning/suggestions` - Quét và xem danh sách gợi ý điều chuyển.
* `POST /api/v1/transfers` - Lập phiếu điều chuyển dựa trên gợi ý hoặc tự tạo.

## 5. Acceptance Criteria
* **Scenario: Stock replenishment suggestion**
  * Given warehouse HP has 80 units of product A (min stock threshold = 100) and warehouse HN has 300 units of product A
  * When Planner runs the suggestions check
  * Then the system SHALL suggest transferring 20 units of product A from warehouse HN to warehouse HP.
