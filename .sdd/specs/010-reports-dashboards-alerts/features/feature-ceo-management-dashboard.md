# Feature: Dashboard Báo cáo Quản trị Cấp cao (US-WMS-18)

## 1. Context and Goal

CEO và Kế toán trưởng cần theo dõi liên tục sức khỏe hoạt động của doanh nghiệp thông qua một màn hình Dashboard tập trung. Dashboard này tổng hợp các số liệu tài chính nhạy cảm (Lãi/Lỗ, Công nợ quá hạn, Giá trị tồn kho) và các chỉ số vận hành (Tỷ lệ lỗi QC, Giao hàng đúng hạn - OTD). Ngoài ra, Kế toán trưởng cần xuất chi tiết báo cáo Giá trị tồn kho cuối kỳ (Inventory Valuation) để đối chiếu kế toán.

---

## 2. Actor

- **CEO (`CEO`)** — Viewer cấp cao: Xem toàn bộ các chỉ số trên dashboard quản trị.
- **Kế toán trưởng (`ACCOUNTANT_MANAGER`)** — Viewer tài chính: Xem dashboard quản trị và xem/xuất báo cáo Giá trị tồn kho.

---

## 3. Functional Requirements (EARS)

### 3.1 Ubiquitous

- The system SHALL restrict access to CEO Dashboard APIs only to users with role `CEO` or `ACCOUNTANT_MANAGER`; unauthorized access requests SHALL be rejected with `ACCESS_DENIED` (403).
- The system SHALL log every successful report view and export action by creating an entry in the `audit_logs` table with `action = 'VIEW'`, `entity_type = 'REPORT'`, and parameters in `new_value`.
- The system SHALL calculate all metrics near real-time with data freshness of no more than 5 minutes.
- The system SHALL use the `cost_price` from the active `APPROVED` price history matching the query date to compute inventory values and COGS.

### 3.2 Event-driven

**Tải dữ liệu CEO Dashboard**
- WHEN a user with `CEO` or `ACCOUNTANT_MANAGER` role requests `GET /api/v1/dashboard/ceo`:
  - Calculate `total_inventory_value` across all physical warehouses by summing up the product of `total_qty` and active approved `cost_price`.
  - Query top 5 dealers with overdue invoices (`status != 'PAID'` and `due_date < CURRENT_DATE`), sorted by total overdue amount descending.
  - Compile the P&L summary for the current month: `Revenue` (total amount from invoices issued in the month), `COGS` (total cost from completed delivery orders in the month), and `Operating Costs` (total shipping cost from completed trips in the month).
  - Calculate the QC failure rate for the current month: `qc_fail_qty / (qc_pass_qty + qc_fail_qty)` from all QC records in the month.
  - Calculate the OTD rate for the current month: count of completed delivery orders where actual delivery date <= expected delivery date, divided by total completed delivery orders.
  - Return HTTP 200 with the compiled JSON.

**Tải Báo cáo Giá trị Tồn kho**
- WHEN a user with `ACCOUNTANT_MANAGER` role requests `GET /api/v1/reports/inventory-valuation`:
  - Support optional query filter `warehouse_id` to scope the report.
  - Retrieve current available regular stock from `inventories` grouped by warehouse, product, and batch.
  - Multiply the quantity by the active approved `cost_price` of the product for that warehouse.
  - Return HTTP 200 with the detailed list and aggregate sum.

---

## 4. API Endpoints

| Method | Path | Mô tả |
|--------|------|-------|
| `GET` | `/api/v1/dashboard/ceo` | Xem 5 chỉ số KPI chiến lược trên Dashboard |
| `GET` | `/api/v1/reports/inventory-valuation` | Lấy chi tiết báo cáo giá trị tồn kho |

### Response — `GET /api/v1/dashboard/ceo`

```json
{
  "as_of_time": "2026-06-29T10:45:00Z",
  "kpis": {
    "total_inventory_value": 4850900000.00,
    "p_and_l": {
      "period": "2026-06",
      "revenue": 1250000000.00,
      "cogs": 850000000.00,
      "operating_costs": 75000000.00,
      "net_profit": 325000000.00
    },
    "qc_failure_rate": 0.024,
    "on_time_delivery_rate": 0.945
  },
  "top_debtors": [
    {
      "dealer_id": 12,
      "dealer_name": "Đại lý Phúc Hưng",
      "overdue_amount": 150000000.00,
      "max_overdue_days": 18
    },
    {
      "dealer_id": 7,
      "dealer_name": "Đại lý Minh Anh",
      "overdue_amount": 90000000.00,
      "max_overdue_days": 12
    }
  ]
}
```

### Response — `GET /api/v1/reports/inventory-valuation`

```json
{
  "generated_at": "2026-06-29T10:45:00Z",
  "filters": {
    "warehouse_id": 1
  },
  "summary": {
    "total_items": 120,
    "total_qty": 15450.00,
    "total_valuation": 1236000000.00
  },
  "records": [
    {
      "warehouse_id": 1,
      "warehouse_name": "Kho Hải Phòng",
      "product_id": 42,
      "product_sku": "POT-001",
      "product_name": "Nồi inox 3 đáy Supor",
      "batch_number": "BAT-20260601-HP",
      "total_qty": 500.00,
      "unit_cost": 85000.00,
      "valuation_amount": 42500000.00
    },
    {
      "warehouse_id": 1,
      "warehouse_name": "Kho Hải Phòng",
      "product_id": 15,
      "product_sku": "PAN-002",
      "product_name": "Chảo chống dính Sunhouse 26cm",
      "batch_number": "BAT-20260515-HP",
      "total_qty": 300.00,
      "unit_cost": 92000.00,
      "valuation_amount": 27600000.00
    }
  ]
}
```

---

## 5. Acceptance Criteria

**Scenario 1: CEO truy cập thành công Dashboard**
- Given một tài khoản người dùng có vai trò `CEO`
- When người dùng gửi yêu cầu `GET /api/v1/dashboard/ceo`
- Then hệ thống phản hồi HTTP 200
- And hiển thị đầy đủ 5 chỉ số KPI bao gồm tổng giá trị tồn kho, P&L, tỷ lệ lỗi QC, tỷ lệ OTD, và danh sách top đại lý nợ quá hạn
- And hệ thống chèn một bản ghi Audit Log với `action = 'VIEW'`, `entity_type = 'REPORT'`, và `new_value` chứa thông tin loại dashboard vừa xem.

**Scenario 2: Người dùng vai trò không hợp lệ bị chặn**
- Given một tài khoản người dùng có vai trò `STOREKEEPER` (Thủ kho) hoặc `WAREHOUSE_STAFF`
- When người dùng cố gửi yêu cầu `GET /api/v1/dashboard/ceo`
- Then hệ thống từ chối yêu cầu và trả về lỗi HTTP 403 `ACCESS_DENIED`
- And không ghi nhận bất kỳ audit log hay trả về thông tin tài chính nào.

**Scenario 3: Kế toán trưởng xem báo cáo Giá trị Tồn kho có lọc theo kho**
- Given một tài khoản người dùng có vai trò `ACCOUNTANT_MANAGER`
- When người dùng yêu cầu Báo cáo Giá trị Tồn kho với tham số `warehouse_id = 1` (Kho Hải Phòng)
- Then hệ thống phản hồi HTTP 200
- And trả về danh sách các sản phẩm và lô hàng thuộc Kho Hải Phòng kèm giá trị định giá tương ứng
- And tổng định giá của báo cáo khớp với tổng giá trị của các lô hàng tại kho đó nhân với giá vốn hiệu lực
- And hệ thống chèn một bản ghi Audit Log ghi nhận việc xem báo cáo của kho này.

**Scenario 4: Tính toán P&L chính xác dựa trên snapshot giá vốn**
- Given trong tháng 6/2026 hệ thống ghi nhận:
  - 1 hóa đơn phát hành trị giá 100.000.000 VND (doanh thu)
  - 1 đơn xuất hàng DO đã hoàn thành (`COMPLETED`) có chứa 100 sản phẩm, mỗi sản phẩm có giá vốn snapshot lúc lập picking plan (`unit_cost`) là 600.000 VND (tổng giá vốn COGS là 60.000.000 VND)
  - 1 chuyến xe Trip hoàn thành có chi phí vận chuyển là 5.000.000 VND
- When CEO tải Dashboard quản trị cho tháng 6/2026
- Then hệ thống hiển thị P&L với: Doanh thu = 100.000.000 VND, COGS = 60.000.000 VND, Operating Costs = 5.000.000 VND, Lợi nhuận ròng = 35.000.000 VND.
