# Feature: Kế toán viên Quản lý Bản giá Đơn lẻ (US-WMS-14a)

## 1. Context and Goal

Kế toán viên tạo, sửa và hủy từng bản giá (`price_history`) cho một sản phẩm trong một kỳ hiệu lực xác định. Bản giá mới được tạo ở trạng thái `PENDING` và chỉ có hiệu lực sau khi Kế toán trưởng duyệt (feature riêng).

---

## 2. Actor

**Kế toán viên (`ACCOUNTANT`)** — Maker.

---

## 3. Functional Requirements (EARS)

### 3.1 Ubiquitous

- The system SHALL require `product_id`, `effective_date`, `end_date`, `cost_price`, `selling_price` for every price entry.
- The system SHALL enforce `effective_date <= end_date`; violations return `INVALID_DATE_RANGE` (400).
- The system SHALL enforce `cost_price > 0` and `selling_price > 0`.
- The system SHALL check for overlap with any `APPROVED` entry for the same `product_id` on every create and update; violations return `OVERLAPPING_EFFECTIVE_DATE` (409). `PENDING` and `CANCELLED` entries are excluded from the overlap check.
- Every mutation SHALL create an audit log entry.

### 3.2 Event-driven

**Tạo bản giá**
- WHEN Kế toán viên submits `POST /api/v1/price-history`:
  - Validate required fields and constraints above.
  - Create `price_history` with `status = PENDING`, `created_by = authenticated user`.
  - Create an in-app notification for all users with role `ACCOUNTANT_MANAGER` (`type = PRICE_PENDING_APPROVAL`).
  - Return HTTP 201 with the created record.

**Sửa bản giá**
- WHEN Kế toán viên submits `PUT /api/v1/price-history/{id}`:
  - Reject with `PRICE_ALREADY_APPROVED` (409) if `status = APPROVED`.
  - Reject with `PRICE_ALREADY_CANCELLED` (409) if `status = CANCELLED`.
  - Reject with HTTP 403 if `created_by != authenticated user`.
  - Re-run all validations (date range, positive values, overlap) against the updated values.
  - Update the record and return HTTP 200.

**Hủy bản giá**
- WHEN Kế toán viên submits `DELETE /api/v1/price-history/{id}`:
  - Reject with `PRICE_ALREADY_APPROVED` (409) if `status = APPROVED`.
  - Reject with `PRICE_ALREADY_CANCELLED` (409) if `status = CANCELLED`.
  - Reject with HTTP 403 if `created_by != authenticated user`.
  - Set `status = CANCELLED`, `cancelled_by`, `cancelled_at`. Do not delete the row.
  - Return HTTP 200.

### 3.3 State-driven

- WHILE `status = PENDING`: creator may edit or cancel; no one may use it for transactions.
- WHILE `status = APPROVED`: immutable — no edit, no cancel.
- WHILE `status = CANCELLED`: immutable — no edit, no cancel, excluded from all lookups.

---

## 4. API Endpoints

| Method | Path | Mô tả |
|--------|------|-------|
| `POST` | `/api/v1/price-history` | Tạo bản giá mới |
| `PUT` | `/api/v1/price-history/{id}` | Sửa bản giá PENDING |
| `DELETE` | `/api/v1/price-history/{id}` | Hủy bản giá PENDING (soft cancel) |
| `GET` | `/api/v1/price-history` | Danh sách bản giá (filter: product_id, status, date range) |
| `GET` | `/api/v1/products/{id}/price-history` | Lịch sử tất cả bản giá của một sản phẩm |

### Request body — `POST /api/v1/price-history`

```json
{
  "product_id": 42,
  "effective_date": "2026-07-01",
  "end_date": "2026-07-31",
  "cost_price": 85000.00,
  "selling_price": 120000.00,
  "notes": "Giá tháng 7 — NCC tăng nguyên liệu"
}
```

### Response — `GET /api/v1/products/{id}/price-history`

```json
{
  "product_id": 42,
  "product_sku": "POT-001",
  "entries": [
    {
      "id": 15,
      "effective_date": "2026-07-01",
      "end_date": "2026-07-31",
      "cost_price": 85000.00,
      "selling_price": 120000.00,
      "status": "PENDING",
      "created_by": { "id": 8, "full_name": "Trần Thị Lan" },
      "approved_by": null,
      "approved_at": null
    },
    {
      "id": 10,
      "effective_date": "2026-06-01",
      "end_date": "2026-06-30",
      "cost_price": 80000.00,
      "selling_price": 115000.00,
      "status": "APPROVED",
      "created_by": { "id": 8, "full_name": "Trần Thị Lan" },
      "approved_by": { "id": 3, "full_name": "Nguyễn Văn An" },
      "approved_at": "2026-05-28T14:00:00Z"
    }
  ]
}
```

---

## 5. Acceptance Criteria

**Scenario 1: Tạo bản giá hợp lệ**
- Given product P không có bản giá APPROVED nào trong tháng 7/2026
- When Kế toán viên tạo bản giá effective 01/07, end 31/07, cost 85.000, sell 120.000
- Then hệ thống tạo bản ghi `status = PENDING`
- And tạo in-app notification cho tất cả ACCOUNTANT_MANAGER
- And trả HTTP 201

**Scenario 2: Từ chối vì overlap với APPROVED**
- Given product P có APPROVED: 01/06 – 30/06
- When Kế toán viên tạo bản giá 15/06 – 15/07
- Then HTTP 409 `OVERLAPPING_EFFECTIVE_DATE`
- And không tạo bản ghi nào

**Scenario 3: PENDING không cản overlap check**
- Given product P có PENDING cho tháng 7, không có APPROVED nào cho tháng 7
- When Kế toán viên tạo bản giá khác cũng cho tháng 7 cho cùng product P
- Then hệ thống cho phép tạo (hai PENDING cùng tháng được phép tồn tại)
- And khi Kế toán trưởng cố duyệt bản thứ hai sau khi bản thứ nhất đã được duyệt → bị chặn ở bước duyệt

**Scenario 4: Sửa bản giá PENDING**
- Given bản giá ID=15 ở PENDING, tạo bởi Kế toán viên A
- When Kế toán viên A sửa selling_price
- Then hệ thống cập nhật, re-validate overlap, trả HTTP 200

**Scenario 5: Không thể sửa bản giá APPROVED**
- Given bản giá ID=10 đã APPROVED
- When Kế toán viên gửi PUT /price-history/10
- Then HTTP 409 `PRICE_ALREADY_APPROVED`

**Scenario 6: Hủy bản giá PENDING**
- Given bản giá ID=15 ở PENDING
- When Kế toán viên (người tạo) gọi DELETE /price-history/15
- Then `status = CANCELLED`, bản ghi vẫn còn trong DB
- And HTTP 200

**Scenario 7: Ngày không hợp lệ**
- Given effective_date = 2026-07-31, end_date = 2026-07-01
- When submit POST
- Then HTTP 400 `INVALID_DATE_RANGE`
