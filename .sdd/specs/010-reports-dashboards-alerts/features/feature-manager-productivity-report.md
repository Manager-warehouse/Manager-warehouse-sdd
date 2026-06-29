# Feature: Báo cáo Năng suất & Sản lượng Nhân viên Kho (US-WMS-25)

## 1. Context and Goal

Để làm căn cứ tính lương theo sản phẩm (KPI) và gửi cho bộ phận nhân sự (HRM) bên ngoài, Trưởng kho và Kế toán trưởng cần theo dõi chi tiết sản lượng hoàn thành công việc của từng nhân viên kho. Báo cáo năng suất cần phân tách rõ 3 nhóm đối tượng: Nhân viên kho bốc xếp (thực hiện picking/putaway), Thủ kho (lập kế hoạch soạn hàng, kiểm QC), và Tài xế (hoàn thành các chuyến đi giao hàng/điều chuyển). Hệ thống cung cấp API xem số liệu trên giao diện và chức năng xuất file Excel báo cáo định kỳ.

---

## 2. Actor

- **Trưởng kho (`WAREHOUSE_MANAGER`)** — Maker xuất báo cáo: Xem và xuất báo cáo năng suất của nhân viên thuộc kho được phân quyền quản lý.
- **Kế toán trưởng (`ACCOUNTANT_MANAGER`)** — Viewer đối soát: Xem và xuất báo cáo năng suất của bất kỳ kho nào trong hệ thống.

---

## 3. Functional Requirements (EARS)

### 3.1 Ubiquitous

- The system SHALL restrict access to productivity report APIs only to `WAREHOUSE_MANAGER` and `ACCOUNTANT_MANAGER` roles.
- The system SHALL log every request to view or export the productivity report in `audit_logs` table.
- The system SHALL require `start_date`, `end_date`, and `warehouse_id` query parameters for every productivity report request.
- The system SHALL enforce that the date range `[start_date, end_date]` does not exceed 31 calendar days; violations SHALL return `INVALID_DATE_RANGE` (400).
- The system SHALL restrict Trưởng kho (`WAREHOUSE_MANAGER`) to only query data for warehouses assigned to their user account; unauthorized warehouse requests SHALL return `WAREHOUSE_SCOPE_FORBIDDEN` (403).

### 3.2 Event-driven

**Tải dữ liệu Báo cáo Năng suất**
- WHEN a user submits `GET /api/v1/reports/productivity` with valid parameters:
  - Query all transactions in the specified date range and warehouse.
  - Compile **Staff Productivity**: count the number of Picking allocations handled and sum of `picked_qty` from `outbound_qc_records` for each user with role `WAREHOUSE_STAFF`.
  - Compile **Storekeeper Productivity**: count the number of Picking plans created (from `delivery_order_item_allocations`) and sum of `qc_pass_qty + qc_fail_qty` checked from `outbound_qc_records` for each user with role `STOREKEEPER`.
  - Compile **Driver Productivity**: count the number of completed Trips (`status = 'COMPLETED'`) and successful deliveries (`status = 'DELIVERED'` in `deliveries`) for each driver.
  - Return HTTP 200 with the compiled JSON structured by actor groups.

**Xuất Excel Báo cáo Năng suất**
- WHEN a user submits `GET /api/v1/reports/productivity/export` with valid parameters:
  - Compile the same productivity data as above.
  - Generate a multi-sheet Excel workbook (`.xlsx`) containing:
    - Sheet 1: `Staff_Productivity` (Mã nhân viên, Tên, Số lượt lấy hàng, Tổng số lượng sản phẩm đã lấy).
    - Sheet 2: `Storekeeper_Productivity` (Mã thủ kho, Tên, Số kế hoạch đã lập, Tổng số lượng sản phẩm đã QC).
    - Sheet 3: `Driver_Productivity` (Mã tài xế, Tên, Số chuyến xe hoàn thành, Số đơn giao thành công).
  - Return HTTP 200 with file download stream (binary output), filename format: `Productivity_Report_WH[ID]_[YYYYMMDD]_[YYYYMMDD].xlsx`.

---

## 4. API Endpoints

| Method | Path | Mô tả |
|--------|------|-------|
| `GET` | `/api/v1/reports/productivity` | Xem số liệu năng suất nhân viên kho (JSON) |
| `GET` | `/api/v1/reports/productivity/export` | Tải báo cáo năng suất dưới dạng file Excel |

### Response — `GET /api/v1/reports/productivity`

```json
{
  "warehouse_id": 1,
  "warehouse_name": "Kho Hải Phòng",
  "start_date": "2026-06-01",
  "end_date": "2026-06-15",
  "staff_productivity": [
    {
      "employee_code": "NV-008",
      "full_name": "Trần Văn Bằng",
      "role": "WAREHOUSE_STAFF",
      "picking_runs_count": 45,
      "total_picked_qty": 2350.00
    },
    {
      "employee_code": "NV-012",
      "full_name": "Lê Văn Cường",
      "role": "WAREHOUSE_STAFF",
      "picking_runs_count": 38,
      "total_picked_qty": 1980.00
    }
  ],
  "storekeeper_productivity": [
    {
      "employee_code": "TK-002",
      "full_name": "Nguyễn Thị Mai",
      "role": "STOREKEEPER",
      "picking_plans_created": 28,
      "total_qc_checked_qty": 4330.00
    }
  ],
  "driver_productivity": [
    {
      "employee_code": "TX-005",
      "full_name": "Phạm Văn Đông",
      "role": "DRIVER",
      "trips_completed": 15,
      "successful_deliveries": 42
    }
  ]
}
```

---

## 5. Acceptance Criteria

**Scenario 1: Xuất báo cáo năng suất thành công với tham số lọc hợp lệ**
- Given một Trưởng kho được phân quyền phụ trách Kho Hải Phòng (id=1)
- When người dùng yêu cầu `GET /api/v1/reports/productivity?warehouse_id=1&start_date=2026-06-01&end_date=2026-06-15`
- Then hệ thống phản hồi HTTP 200
- And trả về cấu trúc dữ liệu JSON chính xác bao gồm 3 mảng năng suất của nhân viên kho, thủ kho và tài xế
- And chèn một bản ghi log trong `audit_logs` với `action = 'VIEW'`, `entity_type = 'REPORT'` và `new_value` chứa thông tin bộ lọc dải ngày của Kho Hải Phòng.

**Scenario 2: Trưởng kho bị chặn xem dữ liệu kho khác**
- Given Trưởng kho A được gán phụ trách Kho Hải Phòng (id=1) và không quản lý Kho Hà Nội (id=2)
- When Trưởng kho A cố truy cập `GET /api/v1/reports/productivity?warehouse_id=2&start_date=2026-06-01&end_date=2026-06-15`
- Then hệ thống từ chối truy cập và trả về lỗi HTTP 403 `WAREHOUSE_SCOPE_FORBIDDEN`
- And không xuất file Excel hay dữ liệu nào.

**Scenario 3: Dải ngày lọc không hợp lệ**
- Given một Kế toán trưởng hợp lệ
- When người dùng yêu cầu báo cáo với dải ngày quá dài: `start_date = 2026-06-01` và `end_date = 2026-07-15` (45 ngày, vượt giới hạn 31 ngày)
- Then hệ thống phản hồi lỗi HTTP 400 `INVALID_DATE_RANGE`
- When người dùng yêu cầu báo cáo với ngày bắt đầu lớn hơn ngày kết thúc: `start_date = 2026-06-15` và `end_date = 2026-06-01`
- Then hệ thống phản hồi lỗi HTTP 400 `INVALID_DATE_RANGE`.

**Scenario 4: Tính toán đúng sản lượng dựa trên giao dịch thực tế**
- Given trong dải ngày từ 01/06 đến 15/06 tại Kho Hải Phòng:
  - Nhân viên kho A thực hiện 2 lượt lấy hàng thành công từ `outbound_qc_records` với tổng `picked_qty` là 150 sản phẩm
  - Thủ kho B tạo lập 3 picking plan và thực hiện kiểm QC 2 lô hàng với tổng số sản phẩm đạt và lỗi là 250 sản phẩm
  - Tài xế C hoàn thành 5 chuyến xe giao hàng và có chữ ký xác nhận POD thành công cho 12 đơn giao
- When Trưởng kho Hải Phòng truy xuất báo cáo năng suất
- Then số liệu của nhân viên A hiển thị: `picking_runs_count = 2`, `total_picked_qty = 150`
- And số liệu của thủ kho B hiển thị: `picking_plans_created = 3`, `total_qc_checked_qty = 250`
- And số liệu của tài xế C hiển thị: `trips_completed = 5`, `successful_deliveries = 12`.
