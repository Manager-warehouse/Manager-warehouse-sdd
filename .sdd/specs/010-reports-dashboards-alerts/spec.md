# Feature Specification: Báo cáo & Cảnh báo (Reporting & Alerts)

**Spec ID**: 010-reports-dashboards-alerts
**Created**: 2026-05-30
**Status**: Draft
**Features**: US-WMS-18, US-WMS-26, US-WMS-25

---

## 1. Context and Goal

Lãnh đạo cần dashboard trực quan để đưa ra quyết định kinh doanh. Trưởng kho cần biết khi nào hàng sắp hết để kịp thời tái nhập. Nhân viên cần báo cáo năng suất.

### Features List
* [US-WMS-18: Dashboard Báo cáo Quản trị Cấp cao](./features/feature-ceo-management-dashboard.md)
* [US-WMS-26: Cảnh báo tự động Tồn kho dưới định mức](./features/feature-manager-low-stock-alerts.md)
* [US-WMS-25: Báo cáo Năng suất & Sản lượng Nhân viên Kho](./features/feature-manager-productivity-report.md)

## 2. Actors

| Actor | Vai trò | Nghiệp vụ liên quan |
|-------|---------|---------------------|
| CEO | Checker cấp cao | Xem Dashboard quản trị chiến lược (tồn kho, công nợ đại lý, P&L, tỷ lệ QC lỗi, OTD) |
| Kế toán trưởng | Checker | Xem báo cáo tài chính P&L, Aging Report, báo cáo giá trị tồn kho cuối kỳ (Inventory Valuation) |
| Trưởng kho | Checker | Nhận cảnh báo tồn kho khả dụng dưới định mức tối thiểu để kịp thời điều tiết |
| Planner | Maker | Nhận cảnh báo tồn kho thấp trên Planning Dashboard để lập lệnh điều chuyển/nhập kho |

## 3. Functional Requirements (EARS)
*Vui lòng xem chi tiết yêu cầu chức năng EARS tại các tài liệu đặc tả tính năng:*
* [EARS - CEO Dashboard](./features/feature-ceo-management-dashboard.md#3-functional-requirements-ears)
* [EARS - Low Stock Alerts](./features/feature-manager-low-stock-alerts.md#3-functional-requirements-ears)
* [EARS - Employee Productivity](./features/feature-manager-productivity-report.md#3-functional-requirements-ears)

## 4. Non-functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-001 | Dashboard load time | ≤ 3s |
| NFR-002 | Report data freshness | Near real-time (≤ 5min) |
| NFR-003 | Alert delivery latency | ≤ 30s from threshold breach |
| NFR-004 | Excel export | ≤ 5s for full report |

## 5. Data Model

### stock_alerts
- `id` (BIGSERIAL, PK)
- `warehouse_id` (BIGINT, FK→warehouses, NOT NULL)
- `product_id` (BIGINT, FK→products, NOT NULL)
- `current_qty` (DECIMAL(10,2), NOT NULL)
- `reorder_point` (DECIMAL(10,2), NOT NULL)
- `alert_type` (VARCHAR(20), DEFAULT 'LOW_STOCK', CHECK IN ('LOW_STOCK','OUT_OF_STOCK'), NOT NULL)
- `is_resolved` (BOOLEAN, DEFAULT false, NOT NULL)
- `resolved_at` (TIMESTAMPTZ)
- `created_at` (TIMESTAMPTZ)
- `UNIQUE(warehouse_id, product_id, alert_type, is_resolved)`

### audit_logs (shared)
- Sử dụng bảng `audit_logs` để ghi log xem báo cáo:
  - `actor_id` (FK→users)
  - `action` = 'VIEW'
  - `entity_type` = 'REPORT'
  - `entity_id` = 0
  - `new_value` = JSON chứa bộ lọc và thông tin báo cáo đã xem

## 6. API Spec
*Vui lòng xem chi tiết API endpoints tại các tài liệu đặc tả tính năng:*
* [APIs - CEO Dashboard](./features/feature-ceo-management-dashboard.md#4-api-endpoints)
* [APIs - Low Stock Alerts](./features/feature-manager-low-stock-alerts.md#4-api-endpoints)
* [APIs - Employee Productivity](./features/feature-manager-productivity-report.md#4-api-endpoints)

## 7. Error Handling

| Error | HTTP | Condition |
|-------|------|-----------|
| ACCESS_DENIED | 403 | User lacks role for report |

## 8. Acceptance Criteria
*Vui lòng xem chi tiết kịch bản kiểm thử tại các tài liệu đặc tả tính năng:*
* [Acceptance - CEO Dashboard](./features/feature-ceo-management-dashboard.md#5-acceptance-criteria)
* [Acceptance - Low Stock Alerts](./features/feature-manager-low-stock-alerts.md#5-acceptance-criteria)
* [Acceptance - Employee Productivity](./features/feature-manager-productivity-report.md#5-acceptance-criteria)

## 9. Out of Scope

- Real-time push notifications (email/SMS)
- Custom report builder
- Drill-down interactive charts (static dashboard)
- Scheduled email reports
