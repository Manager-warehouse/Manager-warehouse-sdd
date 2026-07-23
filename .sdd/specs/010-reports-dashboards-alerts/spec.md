# Feature Specification: Báo cáo & Cảnh báo (Reporting & Alerts)

**Spec ID**: 010-reports-dashboards-alerts
**Created**: 2026-05-30
**Updated**: 2026-06-29
**Status**: Draft
**Features**: US-WMS-18, US-WMS-26

---

## 1. Context and Goal

Ban giám đốc (CEO) và Kế toán trưởng cần các chỉ số tài chính và vận hành trực quan (Dashboard) để theo dõi hiệu quả kinh doanh của toàn bộ 3 kho vật lý (Hải Phòng, Hà Nội, Hồ Chí Minh). Đồng thời, Trưởng kho và bộ phận Planner cần nhận cảnh báo tự động về mức tồn kho khả dụng sắp hết để chủ động tái nhập hàng.

### Features List
* [US-WMS-18: Dashboard Báo cáo Quản trị Cấp cao](./features/feature-ceo-management-dashboard.md)
* [US-WMS-26: Cảnh báo tự động Tồn kho dưới định mức](./features/feature-manager-low-stock-alerts.md)

---

## 2. Clarifications

### Session 2026-06-29

**Q: Công thức tính các KPI trên CEO Dashboard được định nghĩa như thế nào?**
A: Các chỉ số KPI được tổng hợp thời gian thực hoặc gần thời gian thực (freshness ≤ 5 phút) theo các logic sau:
- **Tổng giá trị tồn kho (cost basis)**: Bằng `SUM(inventories.total_qty * price_history.cost_price)` tại thời điểm hiện tại của các kho vật lý. Giá vốn lấy từ bảng giá `APPROVED` đang có hiệu lực tại ngày truy vấn.
- **Top 5 đại lý nợ quá hạn**: Lấy từ bảng `invoices` có `status != 'PAID'` và `due_date < CURRENT_DATE`, sắp xếp theo số tiền quá hạn giảm dần, hiển thị tên đại lý, số tiền và số ngày quá hạn lớn nhất.
- **Báo cáo Lãi/Lỗ tóm tắt (P&L)**: `Net Profit = Revenue - COGS - Operating Costs` trong kỳ báo cáo (ví dụ: tháng hiện tại).
  - *Revenue (Doanh thu)*: `SUM(invoices.total_amount)` của tất cả hóa đơn được phát hành trong kỳ.
  - *COGS (Giá vốn)*: `SUM(delivery_order_items.unit_cost * delivery_order_items.qc_pass_qty)` từ các đơn xuất hàng có trạng thái `COMPLETED` trong kỳ.
  - *Operating Costs (Chi phí vận hành)*: Tổng chi phí vận chuyển của các chuyến đi (`trips.actual_cost` hoặc chi phí phát sinh thực tế nếu có) trong kỳ.
- **Tỷ lệ lỗi QC trong tháng**: Bằng `Tổng số lượng hàng fail QC / Tổng số lượng hàng được kiểm QC` của tất cả các bản ghi QC (`inbound_qc_records` và `outbound_qc_records`) trong tháng.
- **Tỷ lệ OTD (On-Time Delivery)**: Bằng `Số đơn hàng giao đúng hạn / Tổng số đơn hàng đã giao (COMPLETED)` trong kỳ. Đơn giao đúng hạn khi ngày giao hàng thực tế (`deliveries.delivered_at::date`) nhỏ hơn hoặc bằng ngày cam kết giao (`delivery_orders.expected_delivery_date`).

**Q: Việc ghi Audit Log xem báo cáo có vi phạm quy định cấm log read-only trong Spec 001 không?**
A: Có. Spec 010 không định nghĩa ngoại lệ audit cho thao tác xem báo cáo read-only; các màn hình dashboard/report/alert chỉ đọc không tự tạo audit log.

**Q: Khi nào thì cảnh báo tồn kho thấp tự động được kích hoạt và tự động giải quyết?**
A: Hệ thống sẽ tự động tính toán tồn kho khả dụng (`total_qty - reserved_qty`) của sản phẩm tại kho vật lý mỗi khi có biến động tồn kho (tạo DO mới làm tăng reserved, hoàn thành picking làm giảm total, nhập hàng làm tăng total, hoặc adjustment):
- **Kích hoạt (Trigger)**: Khi tồn kho khả dụng giảm xuống dưới ngưỡng `reorder_point` cấu hình cho sản phẩm tại kho đó (nếu null thì dùng cấu hình hệ thống `MIN_INVENTORY_WARNING_THRESHOLD`). Một bản ghi mới ở trạng thái `is_resolved = false` được thêm vào bảng `stock_alerts`, đồng thời gửi in-app notification (High Priority) cho Trưởng kho phụ trách và Planner.
- **Giải quyết (Resolve)**: Khi tồn kho khả dụng tăng trở lại bằng hoặc vượt ngưỡng cảnh báo (do nhập kho hoặc nhận điều chuyển). Hệ thống tự động cập nhật bản ghi alert đó thành `is_resolved = true`, `resolved_at = NOW()`.
- Để tránh spam, hệ thống áp dụng ràng buộc UNIQUE `(warehouse_id, product_id, alert_type, is_resolved)` trên bảng `stock_alerts` ở trạng thái chưa giải quyết (`is_resolved = false`).

---

## 3. Actors

| Actor | Role | Quyền hạn và Nghiệp vụ liên quan |
|-------|------|----------------------------------|
| **CEO** | Strategic Viewer | Xem CEO Dashboard cấp cao (tồn kho, công nợ, P&L, QC, OTD). Không được xem dữ liệu thô hoặc Audit Log hệ thống. |
| **Kế toán trưởng** | Financial Viewer | Xem báo cáo tài chính P&L, Inventory Valuation, Aging Report và thực hiện chốt sổ tháng. |
| **Trưởng kho** | Operations Checker | Xem cảnh báo tồn kho thấp của kho được gán phụ trách. |
| **Planner** | Planning Maker | Xem cảnh báo tồn kho thấp của tất cả các kho để điều phối hàng hóa hoặc lập kế hoạch tái nhập. |
| **System** | Background Trigger | Tự động giám sát biến động tồn kho để kích hoạt/giải quyết cảnh báo. |

---

## 4. Functional Requirements (EARS)

*Vui lòng xem chi tiết tại:*
* [EARS - CEO Dashboard](./features/feature-ceo-management-dashboard.md#3-functional-requirements-ears)
* [EARS - Low Stock Alerts](./features/feature-manager-low-stock-alerts.md#3-functional-requirements-ears)

---

## 5. Non-functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| **NFR-001** | Thời gian phản hồi load CEO Dashboard (p95) | ≤ 2.0 giây |
| **NFR-002** | Độ trễ dữ liệu của báo cáo (Data Freshness) | ≤ 5 phút (Near Real-time) |
| **NFR-003** | Độ trễ kích hoạt cảnh báo tồn kho thấp và gửi notification | ≤ 30 giây kể từ khi tồn kho khả dụng chạm ngưỡng |
| **NFR-004** | Khả năng chịu tải đồng thời khi truy vấn báo cáo | Hỗ trợ tối thiểu 20 truy vấn báo cáo đồng thời không giảm hiệu năng |

---

## 6. Data Model

### stock_alerts

Bảng này dùng để quản lý các cảnh báo tồn kho thấp.

| Field | Type | Notes |
|-------|------|-------|
| `id` | BIGSERIAL (PK) | |
| `warehouse_id` | BIGINT (FK→warehouses, NOT NULL) | Kho vật lý bị cảnh báo |
| `product_id` | BIGINT (FK→products, NOT NULL) | Sản phẩm bị thiếu |
| `current_qty` | DECIMAL(10,2) (NOT NULL) | Tồn kho khả dụng thực tế tại thời điểm cảnh báo |
| `reorder_point` | DECIMAL(10,2) (NOT NULL) | Ngưỡng cảnh báo được áp dụng |
| `alert_type` | VARCHAR(20) (NOT NULL) | CHECK IN ('LOW_STOCK', 'OUT_OF_STOCK'); DEFAULT 'LOW_STOCK' |
| `is_resolved` | BOOLEAN (NOT NULL) | DEFAULT false; true khi tồn kho đã được bổ sung đầy đủ |
| `resolved_at` | TIMESTAMPTZ | NULL cho đến khi `is_resolved = true` |
| `created_at` | TIMESTAMPTZ (DEFAULT NOW()) | Thời điểm kích hoạt cảnh báo |
| `updated_at` | TIMESTAMPTZ (DEFAULT NOW()) | |

**Constraints:**
- `UNIQUE(warehouse_id, product_id, alert_type, is_resolved)` khi `is_resolved = false` (chỉ cho phép tối đa 1 alert chưa xử lý cho mỗi sản phẩm/kho/loại).

**Indexes:**
- `idx_stock_alerts_lookup` on `(warehouse_id, product_id, is_resolved)`
- `idx_stock_alerts_created` on `(created_at DESC)`

## 7. API Spec

*Vui lòng xem chi tiết API endpoints tại các tài liệu đặc tả tính năng:*
* [APIs - CEO Dashboard](./features/feature-ceo-management-dashboard.md#4-api-endpoints)
* [APIs - Low Stock Alerts](./features/feature-manager-low-stock-alerts.md#4-api-endpoints)

---

## 8. Error Handling

| Error Code | HTTP | Điều kiện |
|------------|------|-----------|
| `ACCESS_DENIED` | 403 | Người dùng không có vai trò phù hợp (ví dụ: nhân viên kho cố truy cập CEO Dashboard). |
| `WAREHOUSE_SCOPE_FORBIDDEN` | 403 | Trưởng kho yêu cầu xem cảnh báo của kho mà mình không được gán quyền phụ trách. |
| `INVALID_DATE_RANGE` | 400 | Ngày bắt đầu lớn hơn ngày kết thúc hoặc định dạng ngày không hợp lệ (`YYYY-MM-DD`). |
| `REPORT_NOT_FOUND` | 404 | Báo cáo yêu cầu không tồn tại hoặc đã bị xóa. |

---

## 9. Acceptance Criteria

*Vui lòng xem chi tiết tại:*
* [Acceptance - CEO Dashboard](./features/feature-ceo-management-dashboard.md#5-acceptance-criteria)
* [Acceptance - Low Stock Alerts](./features/feature-manager-low-stock-alerts.md#5-acceptance-criteria)

---

## 10. Out of Scope

- Tích hợp gửi cảnh báo qua các kênh bên ngoài (Email, SMS, Zalo, Slack...) trong Sprint 1. Chỉ hỗ trợ in-app notification.
- Giao diện thiết kế báo cáo động (Custom Report Builder) cho phép người dùng tự kéo thả trường thông tin.
- Các biểu đồ trực quan có tính năng tương tác sâu (Drill-down interactive charts). Sprint 1 chỉ cung cấp số liệu tổng hợp tĩnh trên UI và file Excel thô.
- Tự động lập lịch gửi báo cáo theo định kỳ (Scheduled email reports).
- Hạch toán kế toán kép (Double-entry) và báo cáo thuế/VAT chi tiết (hệ thống chỉ quản lý P&L và công nợ nội bộ).
