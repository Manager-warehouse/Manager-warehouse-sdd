# Feature: Kế toán viên Quản lý Bản giá Đơn lẻ (US-WMS-14a)

## 1. Context and Goal

Kế toán viên tạo, sửa và hủy từng bản giá (`price_history`) cho một sản phẩm, bắt đầu hiệu lực từ một ngày xác định (`effective_date`). Bản giá mới được tạo ở trạng thái `PENDING` và chỉ có hiệu lực sau khi Kế toán trưởng duyệt (feature riêng).

> **Effective-date-only model** (Session 2026-07-09 của `spec.md`): không còn `end_date`. Một bản giá `APPROVED` có hiệu lực từ `effective_date` cho đến khi có bản `APPROVED` khác của cùng `(product_id, warehouse_id)` với `effective_date` mới hơn. Sửa một bản giá đã `APPROVED` bị nhập sai không cần đụng đến bản ghi cũ — chỉ cần tạo và duyệt một bản ghi mới với `effective_date = hôm nay`.

---

## 2. Actor

**Kế toán viên (`ACCOUNTANT`)** — Maker.

---

## 3. Functional Requirements (EARS)

### 3.1 Ubiquitous

- The system SHALL require `product_id`, `warehouse_id`, `effective_date`, `cost_price`, `selling_price` for every price entry. There is no `end_date`.
- The system SHALL enforce `cost_price > 0` and `selling_price > 0`.
- The system SHALL check for another non-`CANCELLED` entry (`PENDING` or `APPROVED`) with the exact same `(product_id, warehouse_id, effective_date)` on every create and update; violations return `OVERLAPPING_EFFECTIVE_DATE` (409) (Session 2026-07-12 của `spec.md` — broadened from "APPROVED only"). A `PENDING` entry already occupying a date blocks creating a second one for that date: the correct way to fix a wrong `PENDING` entry is to edit it (`PUT`), not create a duplicate — the edit action already exists for exactly this reason. `CANCELLED` entries are excluded from the check. Entries for the same product but a different warehouse never conflict.
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
  - Re-run all validations (positive values, same-effective-date conflict) against the updated values.
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
| `GET` | `/api/v1/price-history` | Danh sách bản giá (filter: product_id, warehouse_id, status, effective_date range) |
| `GET` | `/api/v1/products/{id}/price-history` | Lịch sử tất cả bản giá của một sản phẩm (filter tùy chọn: warehouse_id) |

### Request body — `POST /api/v1/price-history`

```json
{
  "product_id": 42,
  "warehouse_id": 1,
  "effective_date": "2026-07-01",
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
      "warehouse_id": 1,
      "warehouse_name": "Kho Hải Phòng",
      "effective_date": "2026-07-01",
      "cost_price": 85000.00,
      "selling_price": 120000.00,
      "status": "PENDING",
      "created_by": { "id": 8, "full_name": "Trần Thị Lan" },
      "approved_by": null,
      "approved_at": null
    },
    {
      "id": 10,
      "warehouse_id": 1,
      "warehouse_name": "Kho Hải Phòng",
      "effective_date": "2026-06-01",
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

> Danh sách trên được sắp xếp theo `effective_date DESC`. Không có trường `end_date`; "hiệu lực đến khi nào" của một dòng được suy ra là ngay trước `effective_date` của dòng liền trước nó trong danh sách (dòng mới nhất còn hiệu lực vô thời hạn). UI hiển thị dạng suy luận này, không lưu trong DB.

---

## 5. Acceptance Criteria

**Scenario 1: Tạo bản giá hợp lệ**
- Given product P tại kho Hải Phòng không có bản giá APPROVED nào với effective_date = 01/07/2026
- When Kế toán viên tạo bản giá warehouse_id=1 (Hải Phòng), effective 01/07, cost 85.000, sell 120.000
- Then hệ thống tạo bản ghi `status = PENDING`
- And tạo in-app notification cho tất cả ACCOUNTANT_MANAGER
- And trả HTTP 201

**Scenario 2: Từ chối vì trùng effective_date với APPROVED cùng kho**
- Given product P tại kho Hải Phòng có APPROVED: effective 01/06
- When Kế toán viên tạo bản giá khác cho Hải Phòng cũng effective 01/06
- Then HTTP 409 `OVERLAPPING_EFFECTIVE_DATE`
- And không tạo bản ghi nào

**Scenario 2b: Cho phép cùng effective_date nhưng khác kho**
- Given product P tại kho Hải Phòng có APPROVED: effective 01/06
- When Kế toán viên tạo bản giá cho kho Hà Nội (warehouse_id=2) cùng effective_date 01/06
- Then hệ thống cho phép tạo (khác warehouse_id, không xung đột)
- And trả HTTP 201

**Scenario 3: Từ chối vì trùng effective_date với PENDING cùng kho**
- Given product P tại kho Hải Phòng có PENDING với effective 01/07, không có APPROVED nào effective 01/07
- When Kế toán viên tạo bản giá khác cũng effective 01/07 tại cùng kho Hải Phòng
- Then HTTP 409 `OVERLAPPING_EFFECTIVE_DATE`
- And không tạo bản ghi nào — nếu bản PENDING hiện tại sai, phải sửa nó qua `PUT /price-history/{id}` thay vì tạo bản mới

**Scenario 3b: Bản giá mới supersede bản cũ, không cần sửa bản cũ**
- Given product P tại kho Hải Phòng có APPROVED: effective 01/06, sell 115.000 (nhập sai, đáng lẽ 150.000)
- When Kế toán viên tạo bản giá mới effective 09/07 (hôm nay), sell 150.000, và Kế toán trưởng duyệt ngay
- Then bản giá effective 01/06 vẫn giữ nguyên `APPROVED`, không bị sửa/hủy
- And từ ngày 09/07 trở đi, tra cứu giá trả về bản effective 09/07 (sell 150.000)
- And tại bất kỳ thời điểm nào trong quá trình trên, luôn có ít nhất một bản `APPROVED` khả dụng cho product P tại kho Hải Phòng — không có khoảng trống giá

**Scenario 4: Sửa bản giá PENDING**
- Given bản giá ID=15 ở PENDING, tạo bởi Kế toán viên A
- When Kế toán viên A sửa selling_price
- Then hệ thống cập nhật, re-validate same-effective-date conflict, trả HTTP 200

**Scenario 5: Không thể sửa bản giá APPROVED**
- Given bản giá ID=10 đã APPROVED
- When Kế toán viên gửi PUT /price-history/10
- Then HTTP 409 `PRICE_ALREADY_APPROVED`

**Scenario 6: Hủy bản giá PENDING**
- Given bản giá ID=15 ở PENDING
- When Kế toán viên (người tạo) gọi DELETE /price-history/15
- Then `status = CANCELLED`, bản ghi vẫn còn trong DB
- And HTTP 200
