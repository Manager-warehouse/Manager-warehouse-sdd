# Feature: Kế toán viên Import Bảng giá từ Excel (US-WMS-14b)

## 1. Context and Goal

Khi cần cập nhật giá cho hàng trăm SKU cùng lúc (ví dụ đầu tháng, đầu quý), việc nhập từng bản giá một là không khả thi. Feature này cho phép Kế toán viên upload file Excel với nhiều dòng sản phẩm, hệ thống tạo hàng loạt bản giá `PENDING` và trả về báo cáo dòng nào thành công / thất bại.

Import không tự động duyệt. Tất cả bản ghi tạo ra đều ở `PENDING` và chờ Kế toán trưởng xét duyệt (theo feature `feature-accountant-manager-price-approval.md`).

---

## 2. Actor

**Kế toán viên (`ACCOUNTANT`)** — Maker.

---

## 3. Functional Requirements (EARS)

### 3.1 Ubiquitous

- The system SHALL only accept `.xlsx` files.
- The system SHALL validate each row independently; a failed row SHALL NOT block valid rows from being created.
- The system SHALL return HTTP 201 when all rows are valid and all records are created.
- The system SHALL return HTTP 207 when the file is structurally valid but at least one row fails data validation (including the case where all data rows fail).
- The system SHALL return HTTP 400 when the file itself is invalid: not `.xlsx`, unreadable, or missing required columns A–E in the header.
- The system SHALL apply the same validation rules as single-entry creation: required fields, positive prices, `selling_price > cost_price` (Session 2026-07-18, `SELLING_BELOW_COST`), `effective_date` in an `OPEN` accounting period (Session 2026-07-18, `PERIOD_CLOSED`), product existence, warehouse existence, no conflicting non-`CANCELLED` (`PENDING` or `APPROVED`) entry with the same `(product_id, warehouse_id, effective_date)` (Session 2026-07-12 của `spec.md`). There is no `end_date` and no date-range validation.
- The system SHALL also treat two rows within the same uploaded file as conflicting if they target the same `(product_id, warehouse_id, effective_date)` (Session 2026-07-18 của `spec.md`): the first such row is created normally, the second and any subsequent one fail with `OVERLAPPING_EFFECTIVE_DATE` — this is checked in addition to, not instead of, the pre-existing-DB-row check above, since a not-yet-committed row created earlier in the same batch would not otherwise be visible to a plain DB lookup for a later row.
- The 1.000-row limit (`EXCEL_TOO_MANY_ROWS`) is counted over actual non-blank data rows, excluding any trailing blank/formatted rows the spreadsheet file may contain after the last real row of data.
- Every successfully created record SHALL trigger a single aggregated in-app notification to all `ACCOUNTANT_MANAGER` users (not one notification per row).
- Every created price entry SHALL have `created_by = authenticated user` and `status = PENDING`.
- Every mutation SHALL create an audit log entry per created record.

### 3.2 Event-driven

**Upload file**
- WHEN Kế toán viên submits `POST /api/v1/price-history/import` with multipart file:
  - Reject if file is not `.xlsx` → HTTP 400 `EXCEL_FORMAT_INVALID`.
  - Reject if row count > 1.000 (excluding header) → HTTP 400 `EXCEL_TOO_MANY_ROWS`.
  - Parse rows starting from row 2 (row 1 = header).
  - For each row:
    - Validate presence of required columns.
    - Validate `product_sku` exists in `products` and `is_active = true`.
    - Validate positive prices.
    - Check for a conflicting non-`CANCELLED` entry with the same `(product_id, warehouse_id, effective_date)`.
    - If valid: create `price_history` record with `status = PENDING`.
    - If invalid: record row number and reason in failure list.
  - After processing all rows:
    - If at least one record was created: send one aggregated notification to `ACCOUNTANT_MANAGER` users.
    - Return HTTP 207 (or 201 if all rows passed) with result payload.

### 3.3 Khuôn dạng file Excel

File `.xlsx` phải có header ở **dòng 1** chính xác như sau (case-insensitive, trim whitespace). Không còn cột `end_date` (xem Session 2026-07-09 của `spec.md`):

| Cột | Header | Kiểu | Bắt buộc | Ghi chú |
|-----|--------|------|----------|---------|
| A | `product_sku` | TEXT | Có | Phải khớp `products.sku` đang active |
| B | `warehouse_code` | TEXT | Có | Mã kho (`warehouses.code`), ví dụ: `HP`, `HN`, `HCM` |
| C | `effective_date` | DATE | Có | Định dạng `DD/MM/YYYY` hoặc Excel date serial |
| D | `cost_price` | DECIMAL | Có | > 0, không có dấu phẩy ngăn cách hàng nghìn |
| E | `selling_price` | DECIMAL | Có | > 0 |
| F | `notes` | TEXT | Không | Bỏ trống nếu không có |

Cột thừa ngoài A–F: bỏ qua. Cột thiếu trong A–E: toàn bộ file bị reject với `EXCEL_FORMAT_INVALID`.

> **Session 2026-07-18**: `GET /price-history/export` (mục 4) PHẢI xuất đúng header và giá trị cột B là `warehouse_code`/mã kho (`HP`/`HN`/`HCM`) — trước đó xuất nhầm header `warehouse_name` kèm tên đầy đủ (`Kho Hải Phòng`), khiến file export không bao giờ import lại được (bị `EXCEL_FORMAT_INVALID` ngay ở bước đọc header, kể cả khi header trùng khớp thì giá trị tên kho cũng không khớp `warehouses.code`). Cột A–F của export và import PHẢI đồng nhất tuyệt đối để hỗ trợ đúng luồng "xuất → sửa giá → import lại".

### 3.4 Mã lỗi per-row

| Mã lỗi dòng | Nguyên nhân |
|-------------|-------------|
| `MISSING_REQUIRED_FIELD` | Thiếu product_sku, warehouse_code, effective_date, cost_price, hoặc selling_price |
| `PRODUCT_NOT_FOUND` | product_sku không tồn tại hoặc `is_active = false` |
| `WAREHOUSE_NOT_FOUND` | warehouse_code không tồn tại hoặc không active |
| `INVALID_DATE_FORMAT` | Ngày không parse được |
| `INVALID_PRICE` | `cost_price` hoặc `selling_price` <= 0 |
| `SELLING_BELOW_COST` | `selling_price <= cost_price` |
| `PERIOD_CLOSED` | `effective_date` nằm trong một kỳ kế toán đã CLOSED |
| `OVERLAPPING_EFFECTIVE_DATE` | Đã tồn tại bản giá PENDING hoặc APPROVED khác cùng `(product_id, warehouse_id, effective_date)` — kể cả khi bản trùng đó nằm ở một dòng khác trong cùng file đang import |

---

## 4. API Endpoints

| Method | Path | Mô tả |
|--------|------|-------|
| `POST` | `/api/v1/price-history/import` | Upload file Excel, tạo hàng loạt bản giá PENDING |
| `GET` | `/api/v1/price-history/import/template` | Download file Excel mẫu với header đúng chuẩn |
| `GET` | `/api/v1/price-history/export` | Xuất danh sách bản giá hiện có ra Excel (filter: `productId`, `warehouseId`, `status`) — cùng khuôn dạng cột A–F ở mục 3.3, để có thể sửa rồi import lại (Session 2026-07-18 của `spec.md`) |

### Request — `POST /api/v1/price-history/import`

`Content-Type: multipart/form-data`

| Field | Kiểu |
|-------|------|
| `file` | File `.xlsx` |

### Response — HTTP 207 (partial success)

```json
{
  "total_rows": 5,
  "created_count": 3,
  "failed_count": 2,
  "created": [
    { "row": 2, "product_sku": "POT-001", "price_history_id": 20 },
    { "row": 3, "product_sku": "PAN-002", "price_history_id": 21 },
    { "row": 5, "product_sku": "CUP-004", "price_history_id": 22 }
  ],
  "failed": [
    { "row": 4, "product_sku": "XXXX-999", "error_code": "PRODUCT_NOT_FOUND", "message": "SKU không tồn tại" },
    { "row": 6, "product_sku": "POT-001", "error_code": "OVERLAPPING_EFFECTIVE_DATE", "message": "Đã có bản giá PENDING khác cùng effective_date 01/07 cho sản phẩm/kho này" }
  ]
}
```

---

## 5. Acceptance Criteria

**Scenario 1: Import thành công toàn bộ**
- Given file Excel 3 dòng, tất cả hợp lệ, không overlap APPROVED nào
- When upload
- Then HTTP 201, tạo 3 bản ghi PENDING
- And 1 notification tổng hợp gửi đến ACCOUNTANT_MANAGER

**Scenario 2: Import thành công một phần**
- Given file Excel 5 dòng: 3 hợp lệ, 1 product_sku không tồn tại, 1 overlap APPROVED
- When upload
- Then HTTP 207, tạo 3 bản ghi PENDING
- And response liệt kê 2 dòng lỗi với mã lỗi và lý do cụ thể

**Scenario 3: File sai định dạng cột**
- Given file Excel thiếu cột `cost_price`
- When upload
- Then HTTP 400 `EXCEL_FORMAT_INVALID`, không tạo bản ghi nào

**Scenario 4: Vượt giới hạn 1.000 dòng**
- Given file Excel có 1.001 dòng dữ liệu
- When upload
- Then HTTP 400 `EXCEL_TOO_MANY_ROWS`, không tạo bản ghi nào

**Scenario 5: File không phải .xlsx**
- Given upload file `.csv`
- Then HTTP 400 `EXCEL_FORMAT_INVALID`

**Scenario 6: Tất cả dòng đều lỗi**
- Given file 3 dòng dữ liệu hợp lệ về format, nhưng tất cả overlap với APPROVED đã có
- When upload
- Then HTTP 207 với `created_count = 0`, `failed_count = 3`
- And không tạo bản ghi nào
- And không tạo notification (vì không có bản ghi nào được tạo)

> Lý do dùng 207 thay vì 422 khi all rows fail: file hợp lệ về mặt cấu trúc, đây là lỗi data từng dòng, không phải lỗi request. Caller cần danh sách lỗi per-row để sửa, không phải reject toàn bộ không rõ lý do.

**Scenario 7: Hai dòng trong cùng file trùng effective_date**
- Given file Excel 2 dòng cùng `product_sku`, cùng `warehouse_code`, cùng `effective_date`, không có bản ghi APPROVED/PENDING nào trong DB trùng ngày đó trước khi upload
- When upload
- Then dòng đầu tiên được tạo `PENDING` thành công
- And dòng thứ hai bị từ chối với `OVERLAPPING_EFFECTIVE_DATE`
- And HTTP 207, `created_count = 1`, `failed_count = 1`

**Scenario 8: Dòng có selling_price <= cost_price**
- Given file Excel có một dòng với `cost_price = 100000`, `selling_price = 95000`
- When upload
- Then dòng đó bị từ chối với `SELLING_BELOW_COST`, các dòng hợp lệ khác vẫn được tạo

**Scenario 9: Dòng có effective_date thuộc kỳ kế toán đã CLOSED**
- Given kỳ kế toán tháng 01/2026 đã CLOSED, file Excel có một dòng với `effective_date = 15/01/2026`
- When upload
- Then dòng đó bị từ chối với `PERIOD_CLOSED`, các dòng hợp lệ khác (effective_date trong kỳ đang mở) vẫn được tạo
