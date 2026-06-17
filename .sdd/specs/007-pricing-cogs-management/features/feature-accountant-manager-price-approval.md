# Feature: Kế toán trưởng Duyệt Bản giá (US-WMS-14c)

## 1. Context and Goal

Kế toán trưởng xem xét các bản giá đang ở trạng thái `PENDING`, so sánh với giá hiện hành (bản giá `APPROVED` gần nhất của cùng sản phẩm), và đưa ra quyết định duyệt để kích hoạt bản giá mới.

Không có luồng "từ chối" tường minh trong Sprint 1: Kế toán trưởng thông báo qua kênh nội bộ (Zalo/chat) nếu cần điều chỉnh, Kế toán viên sửa bản giá PENDING hoặc hủy và tạo lại. Lý do: không muốn thêm trạng thái `REJECTED` khi workflow chỉ có 2 người, lượng giao dịch nhỏ, và feedback loop đủ ngắn.

---

## 2. Actor

**Kế toán trưởng (`ACCOUNTANT_MANAGER`)** — Checker.

---

## 3. Functional Requirements (EARS)

### 3.1 Ubiquitous

- The system SHALL re-validate APPROVED overlap at the moment of approval, not only at creation time. If a concurrent approval has occurred since the entry was created, the system SHALL reject with `OVERLAPPING_EFFECTIVE_DATE` (409).
- The system SHALL enforce Maker-Checker: the user who created the entry (`created_by`) SHALL NOT be the same user who approves it. Since `ACCOUNTANT` creates and `ACCOUNTANT_MANAGER` approves, RBAC enforces this automatically; no additional check is needed at the service layer.
- Every approval action SHALL create an audit log entry.

### 3.2 Event-driven

**Xem danh sách PENDING**
- WHEN Kế toán trưởng opens the price approval screen, the system SHALL display all `price_history` entries with `status = PENDING`, sorted by `created_at ASC` (oldest first).
- For each PENDING entry, the system SHALL display the most recent `APPROVED` entry for the same product alongside (for comparison), or indicate "Chưa có giá hiệu lực" if none exists.

**Duyệt bản giá**
- WHEN Kế toán trưởng submits `PUT /api/v1/price-history/{id}/approve`:
  - Reject with `PRICE_ALREADY_APPROVED` (409) if `status = APPROVED`.
  - Reject with `PRICE_ALREADY_CANCELLED` (409) if `status = CANCELLED`.
  - Re-check APPROVED overlap for the same `product_id` and date range. If overlap found → HTTP 409 `OVERLAPPING_EFFECTIVE_DATE`.
  - Set `status = APPROVED`, `approved_by = authenticated user`, `approved_at = NOW()`.
  - Return HTTP 200 with the updated entry.
  - The approved entry is now immediately available for price lookups in transactions.

### 3.3 State-driven

- WHILE viewing the approval list, the system SHALL show the delta between `cost_price`/`selling_price` of the PENDING entry and the most recent APPROVED entry for the same product (absolute difference and percentage).
- WHILE a PENDING entry's date range now overlaps with a newly approved entry (race condition), the system SHALL surface `OVERLAPPING_EFFECTIVE_DATE` at approval time to prevent silent data corruption.

---

## 4. API Endpoints

| Method | Path | Mô tả |
|--------|------|-------|
| `GET` | `/api/v1/price-history?status=PENDING` | Danh sách bản giá chờ duyệt — endpoint dùng chung, filter `status=PENDING` |
| `GET` | `/api/v1/price-history/{id}` | Chi tiết một bản giá; response bổ sung `previous_approved` khi caller là `ACCOUNTANT_MANAGER` — endpoint dùng chung, owned bởi feature 14a |
| `PUT` | `/api/v1/price-history/{id}/approve` | Duyệt bản giá — endpoint riêng của feature 14c |

### Response — `GET /api/v1/price-history/{id}` (approval view)

```json
{
  "id": 15,
  "product_id": 42,
  "product_sku": "POT-001",
  "product_name": "Nồi inox 20cm",
  "effective_date": "2026-07-01",
  "end_date": "2026-07-31",
  "cost_price": 85000.00,
  "selling_price": 120000.00,
  "status": "PENDING",
  "notes": "NCC tăng nguyên liệu",
  "created_by": { "id": 8, "full_name": "Trần Thị Lan" },
  "created_at": "2026-06-25T10:00:00Z",
  "previous_approved": {
    "id": 10,
    "effective_date": "2026-06-01",
    "end_date": "2026-06-30",
    "cost_price": 80000.00,
    "selling_price": 115000.00,
    "cost_price_delta": 5000.00,
    "cost_price_delta_pct": 6.25,
    "selling_price_delta": 5000.00,
    "selling_price_delta_pct": 4.35
  }
}
```

### Response — `PUT /api/v1/price-history/{id}/approve`

```json
{
  "id": 15,
  "status": "APPROVED",
  "approved_by": { "id": 3, "full_name": "Nguyễn Văn An" },
  "approved_at": "2026-06-26T09:15:00Z"
}
```

---

## 5. Acceptance Criteria

**Scenario 1: Duyệt thành công**
- Given bản giá ID=15 đang PENDING, effective 01/07–31/07
- And không có APPROVED nào cho cùng product trong tháng 7
- When Kế toán trưởng gọi PUT /price-history/15/approve
- Then `status = APPROVED`, `approved_by` và `approved_at` được ghi
- And bản giá có hiệu lực ngay lập tức cho tra cứu giao dịch
- And audit log ghi nhận action APPROVE

**Scenario 2: Từ chối vì overlap phát sinh sau khi tạo (race condition)**
- Given bản giá ID=15 (tháng 7) ở PENDING
- And trong thời gian chờ, bản giá ID=16 cùng product cùng tháng 7 đã được APPROVED trước
- When Kế toán trưởng cố duyệt ID=15
- Then HTTP 409 `OVERLAPPING_EFFECTIVE_DATE`
- And ID=15 vẫn ở PENDING

**Scenario 3: Từ chối vì bản giá đã APPROVED**
- Given bản giá ID=10 đã APPROVED
- When Kế toán trưởng gọi PUT /price-history/10/approve lần thứ hai
- Then HTTP 409 `PRICE_ALREADY_APPROVED`

**Scenario 4: Hiển thị delta so với giá cũ**
- Given GET /price-history/15 cho bản giá PENDING
- And product đó có bản giá APPROVED gần nhất: cost 80.000, sell 115.000
- Then response chứa `previous_approved.cost_price_delta = 5000`, `cost_price_delta_pct = 6.25`

**Scenario 5: Không có bản giá cũ**
- Given product mới tạo lần đầu có bản giá PENDING, chưa có APPROVED nào
- When GET /price-history/{id}
- Then `previous_approved = null`

**Scenario 6: Từ chối vì bản giá đã CANCELLED**
- Given bản giá ID=12 đã CANCELLED
- When Kế toán trưởng gọi PUT /price-history/12/approve
- Then HTTP 409 `PRICE_ALREADY_CANCELLED`
