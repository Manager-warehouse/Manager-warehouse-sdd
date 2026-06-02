# Feature: CEO Dashboard Báo cáo Quản trị Cấp cao (US-WMS-18)

## 1. Context and Goal
CEO và Kế toán trưởng cần màn hình Dashboard trực quan để theo dõi giá trị tồn kho 3 miền, doanh số nợ đại lý, kết quả QC, hiệu suất giao hàng và báo cáo Lãi/Lỗ (P&L = Doanh thu - COGS - Chi phí) phục vụ điều tiết kinh doanh.

## 2. Actors
* **CEO**: Xem dashboard chiến lược cấp cao.
* **Kế toán trưởng**: Xem báo cáo tài chính P&L, báo cáo giá trị tồn kho.

## 3. Functional Requirements (EARS)
* **Ubiquitous:**
  * The system SHALL always log every report view by creating an entry in `audit_logs` with `action = 'VIEW'` and `entity_type = 'REPORT'`.
* **Event-driven:**
  * WHEN a CEO views the Dashboard, the system SHALL display:
    * Total inventory value (cost basis) across 3 physical warehouses.
    * Top 5 dealers with highest overdue amounts + number of overdue days.
    * P&L summary (`Revenue - COGS - Operating Costs = Net Profit`).
    * QC failure rate for the current month.
    * On-Time Delivery (OTD) rate.
* **Optional:**
  * WHERE the report is Inventory Valuation, the system SHALL calculate total value as `SUM(inventories.total_qty × price_history.cost_price at period-end)`.

## 4. API Endpoints
* `GET /api/v1/dashboard/ceo` - Xem dashboard quản trị.
* `GET /api/v1/reports/inventory-valuation` - Xuất báo cáo giá trị tồn kho.

## 5. Acceptance Criteria
* **Scenario: Access CEO Dashboard**
  * Given a user with `CEO` role
  * When they access the Dashboard
  * Then the system SHALL render all 5 core metrics (inventory value, top debtors, P&L, QC rate, OTD) and write a VIEW log in the audit logs.
