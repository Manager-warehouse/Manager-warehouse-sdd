# Feature: Trưởng kho / CEO Phê duyệt Điều chỉnh Chênh lệch Kiểm kê (US-WMS-13)

## 1. Context and Goal

Sau khi Thủ kho hoàn tất đếm, hệ thống tự động định tuyến yêu cầu phê duyệt đến đúng cấp duyệt dựa trên **giá trị tuyệt đối** của tổng chênh lệch và cờ lỗi nhân viên. Khi được phê duyệt, hệ thống cập nhật tồn kho về số đếm thực tế, ghi audit trail và giải phóng khóa vị trí. Khi bị từ chối, phiếu trở về DRAFT để Thủ kho điều chỉnh và nộp lại.

## 2. Actors

- **Trưởng kho**: Duyệt hoặc từ chối khi `5,000,000 ≤ |total_variance_value| ≤ 100,000,000` VND **và** `is_employee_fault = false`.
- **CEO**: Duyệt hoặc từ chối khi `|total_variance_value| > 100,000,000` VND **hoặc** `is_employee_fault = true`.

> **Lưu ý**: Ngưỡng áp dụng theo **giá trị tuyệt đối** của `total_variance_value` (có thể âm hoặc dương).

## 3. Functional Requirements (EARS)

### Routing phê duyệt

- WHEN a stocktake transitions to `PENDING_APPROVAL`, the system SHALL compute `approval_level` as follows:
  - `|total_variance_value| < 5,000,000` → `approval_level = AUTO`
  - `5,000,000 ≤ |total_variance_value| ≤ 100,000,000` AND `is_employee_fault = false` → `approval_level = MANAGER`
  - `|total_variance_value| > 100,000,000` OR `is_employee_fault = true` → `approval_level = CEO`
- WHEN `approval_level = AUTO`, the system SHALL immediately approve the stocktake without human intervention (xem EARS phê duyệt bên dưới).

### Kiểm soát cấp duyệt

- WHEN a Trưởng kho calls the approve or reject endpoint on a stocktake where `approval_level = CEO`, the system SHALL reject with `APPROVAL_LEVEL_MISMATCH (403)`.
- WHEN a CEO calls the approve or reject endpoint on a stocktake where `approval_level = MANAGER`, the system SHALL allow the action (CEO có thể duyệt thay Trưởng kho nếu cần).

### Phê duyệt

- WHEN a stocktake is approved (by Trưởng kho, CEO, or AUTO), the system SHALL:
  1. Validate the `accounting_period` of the stocktake is still `OPEN`; reject with `ACCOUNTING_PERIOD_CLOSED` if not.
  2. For each `stock_take_item` where `variance_qty ≠ 0`, update `inventory.quantity` to match `actual_qty` using optimistic locking.
  3. If an optimistic lock version conflict occurs during inventory update, abort and retry up to 3 times; if still failing, return `INVENTORY_VERSION_CONFLICT (409)`.
  4. Create one `adjustments` record per affected item with `type = 'STOCK_TAKE'`, `reference_id = stock_take.id`, `reference_type = 'STOCK_TAKE'`.
  5. Set `stock_takes.status = 'APPROVED'`, `approved_by = current_user.id`, `approved_at = NOW()`.
  6. Release the lock on all warehouse locations referenced in `stock_take_items`.

### Từ chối

- WHEN a Trưởng kho or CEO rejects a stocktake, the system SHALL:
  - Require a non-empty `rejection_reason`; return `400` if empty.
  - Set `stock_takes.status = 'REJECTED'` and store `rejection_reason`.
  - Release the lock on all warehouse locations referenced in `stock_take_items`.
- AFTER a stocktake is `REJECTED`, the system SHALL allow the Thủ kho to update `stock_take_items.actual_qty` and re-submit (`PUT .../complete`) which transitions status back to `PENDING_APPROVAL` with recalculated `total_variance_value` and `approval_level`.

### Phê duyệt AUTO (chênh lệch nhỏ)

- WHEN `approval_level = AUTO`, the system SHALL execute the same approval steps as above immediately upon `complete`, without requiring human action, and set `approved_by = NULL` (system-generated).

## 4. API Endpoints

| Method | Endpoint                                     | Role         | Mô tả                                                              |
| ------ | -------------------------------------------- | ------------ | ------------------------------------------------------------------ |
| `GET`  | `/api/v1/stocktakes?status=PENDING_APPROVAL` | MANAGER, CEO | Danh sách phiếu chờ duyệt (filter theo approval_level)             |
| `PUT`  | `/api/v1/stocktakes/{id}/approve`            | MANAGER      | Phê duyệt chênh lệch (approval_level = MANAGER)                    |
| `PUT`  | `/api/v1/stocktakes/{id}/reject`             | MANAGER      | Từ chối, bắt buộc nhập rejection_reason (approval_level = MANAGER) |
| `PUT`  | `/api/v1/stocktakes/{id}/approve-ceo`        | CEO          | Phê duyệt chênh lệch (approval_level = CEO)                        |
| `PUT`  | `/api/v1/stocktakes/{id}/reject-ceo`         | CEO          | Từ chối, bắt buộc nhập rejection_reason (approval_level = CEO)     |

> CEO cũng có thể gọi `PUT .../approve` khi `approval_level = MANAGER` nếu cần duyệt thay.

## 5. Acceptance Criteria

**Scenario 1: Routing đến Trưởng kho**

- Given stocktake có `total_variance_value = -60,000,000` VND (|60M| ≤ 100M) và `is_employee_fault = false`
- When Thủ kho hoàn tất đếm (`complete`)
- Then `approval_level = MANAGER` và status = `PENDING_APPROVAL`, Trưởng kho được phép duyệt.

**Scenario 2: Routing đến CEO vì vượt ngưỡng**

- Given stocktake có `total_variance_value = -120,000,000` VND (|120M| > 100M)
- When Thủ kho hoàn tất đếm
- Then `approval_level = CEO` và status = `PENDING_APPROVAL`; nếu Trưởng kho gọi approve thì hệ thống trả về `APPROVAL_LEVEL_MISMATCH (403)`.

**Scenario 3: Routing đến CEO vì lỗi nhân viên**

- Given stocktake có `total_variance_value = -30,000,000` VND (|30M| ≤ 100M) nhưng `is_employee_fault = true`
- When Thủ kho hoàn tất đếm
- Then `approval_level = CEO` mặc dù giá trị dưới ngưỡng 100M.

**Scenario 4: Auto-approve khi chênh lệch nhỏ**

- Given stocktake có `total_variance_value = -2,000,000` VND (|2M| < 5M)
- When Thủ kho gọi `complete`
- Then hệ thống tự động phê duyệt, cập nhật inventory, ghi adjustments, status = `APPROVED`, không cần action của Trưởng kho hay CEO.

**Scenario 5: Phê duyệt bởi Trưởng kho**

- Given stocktake `approval_level = MANAGER` đang `PENDING_APPROVAL`, gồm item A variance_qty = -10 (cost_price = 50,000)
- When Trưởng kho gọi `PUT .../approve`
- Then:
  - `inventory.quantity` của item A giảm 10.
  - Một bản ghi `adjustments` được tạo với `type = 'STOCK_TAKE'`, `quantity_adjustment = -10`.
  - status = `APPROVED`, location lock được giải phóng.

**Scenario 6: Từ chối và Thủ kho nộp lại**

- Given stocktake đang `PENDING_APPROVAL`
- When Trưởng kho gọi reject với `rejection_reason = "Số đếm sai vị trí A.01"`
- Then status = `REJECTED`, location lock giải phóng.
- When Thủ kho cập nhật actual_qty và gọi lại `complete`
- Then status = `PENDING_APPROVAL` với `total_variance_value` và `approval_level` được tính lại.

**Scenario 7: Từ chối thiếu lý do**

- Given stocktake đang `PENDING_APPROVAL`
- When Trưởng kho gọi reject với `rejection_reason` rỗng
- Then hệ thống trả về `400 BAD_REQUEST`.

**Scenario 8: Chặn duyệt khi kỳ kế toán đã đóng**

- Given stocktake đang `PENDING_APPROVAL` với `accounting_period` đã chuyển sang `CLOSED` trong khi chờ duyệt
- When Trưởng kho gọi approve
- Then hệ thống từ chối với `ACCOUNTING_PERIOD_CLOSED (422)`.
