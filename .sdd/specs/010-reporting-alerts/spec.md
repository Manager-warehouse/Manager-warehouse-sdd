# Feature Specification: Báo cáo & Cảnh báo (Reporting & Alerts)

**Spec ID**: 010-reporting-alerts
**Created**: 2026-05-30
**Status**: Draft
**Features**: US-WMS-18, US-WMS-26, US-WMS-25

---

## 1. Context and Goal

Lãnh đạo cần dashboard trực quan để đưa ra quyết định kinh doanh. Trưởng kho cần biết
khi nào hàng sắp hết để kịp thời tái nhập. Nhân viên cần báo cáo năng suất.

**Goal:** Xây dựng dashboard quản trị (CEO), cảnh báo tồn kho dưới định mức, và báo cáo
năng suất nhân viên kho.

## 2. Actors

| Actor | Vai trò |
|-------|---------|
| CEO | Xem Dashboard chiến lược |
| Kế toán trưởng | Xem Inventory Valuation, P&L |
| Trưởng kho | Nhận cảnh báo tồn kho thấp |
| Planner | Xem cảnh báo trên Planning Dashboard |

## 3. Functional Requirements (EARS)

**Ubiquitous:**
- The system SHALL always log every report view (who, when, filters applied).

**Event-driven:**
- WHEN a CEO views the Dashboard, the system SHALL display:
  - Total inventory value (cost basis) across 3 warehouses
  - Top 5 dealers with highest overdue amounts + days
  - P&L summary (Revenue - COGS - Operating Costs = Net Profit)
  - QC failure rate for current month
  - On-Time Delivery rate
- WHEN available inventory of a product at a warehouse drops below minimum
  threshold, the system SHALL:
  - Send an in-app notification (High Priority) to Trưởng kho and Planner
  - Highlight the product in red on the Dashboard
- WHEN a Trưởng kho requests a productivity report, the system SHALL export
  an Excel file with: number of orders processed per employee, QC throughput,
  trips completed per driver.

**Optional:**
- WHERE the report is Inventory Valuation, the system SHALL calculate total
  value as `SUM(quantity × cost_price at period-end)`.

## 4. Non-functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-001 | Dashboard load time | ≤ 3s |
| NFR-002 | Report data freshness | Near real-time (≤ 5min) |
| NFR-003 | Alert delivery latency | ≤ 30s from threshold breach |
| NFR-004 | Excel export | ≤ 5s for full report |

## 5. API Spec

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | /api/v1/dashboard/ceo | CEO, ACCOUNTANT_MANAGER | CEO dashboard |
| GET | /api/v1/reports/inventory-summary | Bearer | Inventory summary by warehouse |
| GET | /api/v1/reports/inventory-valuation | ACCOUNTANT_MANAGER | Valuation report |
| GET | /api/v1/reports/daily-transactions | Bearer | Daily transaction summary |
| GET | /api/v1/reports/dealer-aging | ACCOUNTANT_MANAGER | Aging report |
| GET | /api/v1/reports/productivity | WAREHOUSE_MANAGER | Employee productivity |
| GET | /api/v1/alerts/low-stock | Bearer | Low stock alerts |

## 6. Error Handling

| Error | HTTP | Condition |
|-------|------|-----------|
| ACCESS_DENIED | 403 | User lacks role for report |

## 7. Acceptance Criteria

1. Given a CEO user,
   when they access the Dashboard,
   then all 5 metrics (inventory value, top debtors, P&L, QC rate, OTD) SHALL display.
2. Given a product with min_threshold = 100 and current stock = 80,
   when the system runs its stock check,
   then an alert SHALL be sent to Trưởng kho and Planner.
3. Given a date range,
   when a productivity report is requested,
   then the system SHALL export an Excel file with per-employee metrics.

## 8. Out of Scope

- Real-time push notifications (email/SMS)
- Custom report builder
- Drill-down interactive charts (static dashboard)
- Scheduled email reports
