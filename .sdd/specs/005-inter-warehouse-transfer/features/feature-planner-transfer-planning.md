# Feature: Planner Lập Kế hoạch Điều chuyển kho tự động (US-WMS-11)

## 1. Context and Goal
Planner sử dụng Planning Dashboard để hệ thống tự động quét tồn khả dụng của 3 kho vật lý và gợi ý các lệnh điều chuyển tối ưu nhằm tránh đứt gãy hàng hóa. Planner chọn gợi ý phù hợp để tạo nhanh phiếu điều chuyển; quyền phê duyệt phiếu điều chuyển thuộc Trưởng kho nguồn.

## 2. Actors
* **Planner (Người lập kế hoạch)**: Xem gợi ý điều chuyển và nhấn nút tạo nhanh phiếu điều chuyển.

## 3. Functional Requirements (EARS)
* **Optional:**
  * WHERE the Planning Dashboard is enabled, the system SHALL compare available inventory of each product at each warehouse against the configured minimum stock threshold and display replenishment suggestions.
* **Ubiquitous:**
  * The system SHALL return each transfer suggestion with product, source warehouse, destination warehouse, suggested quantity, priority, and reason.
  * The system SHALL calculate suggested quantity from the destination warehouse shortage and source warehouse available surplus.
* **Event-driven:**
  * WHEN a Planner creates a transfer from a suggestion, the system SHALL copy product, source warehouse, destination warehouse, and suggested quantity into the new transfer draft.
  * WHEN a Planner creates a transfer manually, the system SHALL require source warehouse, destination warehouse, product, planned quantity, and planned date.

## 4. API Endpoints
* `GET /api/v1/planning/suggestions` - Quét và xem danh sách gợi ý điều chuyển.
* `POST /api/v1/transfers` - Lập phiếu điều chuyển dựa trên gợi ý hoặc tự tạo.

## 5. Acceptance Criteria
* **Scenario: Stock replenishment suggestion**
  * Given warehouse HP has 80 units of product A (min stock threshold = 100) and warehouse HN has 300 units of product A
  * When Planner runs the suggestions check
  * Then the system SHALL suggest transferring 20 units of product A from warehouse HN to warehouse HP with priority and shortage reason.

* **Scenario: Create transfer from suggestion**
  * Given a transfer suggestion recommends moving 20 units of product A from warehouse HN to warehouse HP
  * When Planner creates a transfer from the suggestion
  * Then the system SHALL create a `NEW` transfer using the suggested product, source warehouse, destination warehouse, and quantity.
