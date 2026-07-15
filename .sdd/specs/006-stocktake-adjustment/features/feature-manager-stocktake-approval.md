# Feature: Trưởng kho Phê duyệt Điều chỉnh Chênh lệch Kiểm kê (US-WMS-13)

## 1. Context and Goal

Sau khi Thủ kho hoàn tất đếm, mọi phiếu kiểm kê chuyển sang `PENDING_APPROVAL` và được gửi trực tiếp đến Trưởng kho phê duyệt — không phân cấp theo giá trị chênh lệch. Khi được phê duyệt, hệ thống cập nhật tồn kho về số đếm thực tế, ghi audit trail và giải phóng khóa vị trí. Khi bị từ chối, phiếu trở về DRAFT để Thủ kho điều chỉnh và nộp lại.

## 2. Actors

- **Trưởng kho**: Duyệt hoặc từ chối mọi chênh lệch kiểm kê thuộc kho được gán, không phân cấp theo giá trị.

## 3. Functional Requirements (EARS)

### Phê duyệt

- WHEN a stocktake transitions to `PENDING_APPROVAL`, the system SHALL route it to the Trưởng kho of the stocktake's `warehouse_id`.
- WHEN a Trưởng kho calls the approve endpoint on a stocktake whose `warehouse_id` is not in that Trưởng kho's assigned warehouses, the system SHALL reject with `403 FORBIDDEN_WAREHOUSE` — role check alone (`MANAGER`) is not sufficient.
- WHEN a stocktake is approved by Trưởng kho, the system SHALL:
  1. Validate the `accounting_period` of the stocktake is still `OPEN`; reject with `ACCOUNTING_PERIOD_CLOSED` if not.
  2. For each `stock_take_item` where `variance_qty ≠ 0`, update `inventory.quantity` to match `actual_qty` using optimistic locking.
  3. If an optimistic lock version conflict occurs during inventory update, abort and retry up to 3 times; if still failing, return `INVENTORY_VERSION_CONFLICT (409)`.
  4. Create one `adjustments` record per affected item with `type = 'STOCK_TAKE'`, `reference_id = stock_take.id`, `reference_type = 'STOCK_TAKE'`.
  5. Set `stock_takes.status = 'APPROVED'`, `approved_by = current_user.id`, `approved_at = NOW()`.
  6. Release the lock on all warehouse locations referenced in `stock_take_items`.

### Từ chối

- WHEN a Trưởng kho rejects a stocktake, the system SHALL:
  - Require a non-empty `rejection_reason`; return `400` if empty.
  - Set `stock_takes.status = 'REJECTED'` and store `rejection_reason`.
  - Release the lock on all warehouse locations referenced in `stock_take_items`.
- AFTER a stocktake is `REJECTED`, the system SHALL allow the Thủ kho to update `stock_take_items.actual_qty` and re-submit (`PUT .../complete`) which transitions status back to `PENDING_APPROVAL`.

### Ghi log truy cập báo cáo

- WHEN a user views the pending-approval list or a stocktake detail via `GET /api/v1/stocktakes`, the system SHALL create a `REPORT_VIEW` audit log entry with viewer, timestamp, and applied filters (business.md AUD-04).

## 4. API Endpoints

| Method | Endpoint                                     | Role    | Mô tả                                          |
| ------ | --------------------------------------------- | ------- | ----------------------------------------------- |
| `GET`  | `/api/v1/stocktakes?status=PENDING_APPROVAL`  | MANAGER | Danh sách phiếu chờ duyệt thuộc kho được gán     |
| `PUT`  | `/api/v1/stocktakes/{id}/approve`             | MANAGER | Phê duyệt chênh lệch                             |
| `PUT`  | `/api/v1/stocktakes/{id}/reject`              | MANAGER | Từ chối, bắt buộc nhập rejection_reason          |

## 5. Acceptance Criteria

**Scenario 1: Phê duyệt bởi Trưởng kho**

- Given stocktake `PENDING_APPROVAL`, gồm item A variance_qty = -10 (cost_price = 50,000)
- When Trưởng kho của kho đó gọi `PUT .../approve`
- Then:
  - `inventory.quantity` của item A giảm 10.
  - Một bản ghi `adjustments` được tạo với `type = 'STOCK_TAKE'`, `quantity_adjustment = -10`.
  - status = `APPROVED`, location lock được giải phóng.

**Scenario 2: Từ chối và Thủ kho nộp lại**

- Given stocktake đang `PENDING_APPROVAL`
- When Trưởng kho gọi reject với `rejection_reason = "Số đếm sai vị trí A.01"`
- Then status = `REJECTED`, location lock giải phóng.
- When Thủ kho cập nhật actual_qty và gọi lại `complete`
- Then status = `PENDING_APPROVAL`.

**Scenario 3: Từ chối thiếu lý do**

- Given stocktake đang `PENDING_APPROVAL`
- When Trưởng kho gọi reject với `rejection_reason` rỗng
- Then hệ thống trả về `400 BAD_REQUEST`.

**Scenario 4: Chặn duyệt khi kỳ kế toán đã đóng**

- Given stocktake đang `PENDING_APPROVAL` với `accounting_period` đã chuyển sang `CLOSED` trong khi chờ duyệt
- When Trưởng kho gọi approve
- Then hệ thống từ chối với `ACCOUNTING_PERIOD_CLOSED (422)`.

**Scenario 5: Chặn duyệt sai kho**

- Given stocktake `PENDING_APPROVAL` thuộc kho Hải Phòng
- When một Trưởng kho chỉ được gán kho Hà Nội gọi `PUT .../approve`
- Then hệ thống trả về `403 FORBIDDEN_WAREHOUSE`.
