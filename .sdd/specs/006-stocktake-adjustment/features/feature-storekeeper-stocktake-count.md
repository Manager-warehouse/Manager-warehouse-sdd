# Feature: Thủ kho Kiểm kê kho & Đếm hàng Thực tế (US-WMS-13)

## 1. Context and Goal
Thủ kho tạo phiếu kiểm kê định kỳ, hệ thống khóa sổ vị trí ô kệ (trừ Quarantine) để tránh giao dịch phát sinh trong lúc đếm. Khi nhập kết quả đếm, hệ thống tự tính chênh lệch để chuẩn bị trình duyệt.

## 2. Actors
* **Thủ kho**: Lập phiếu kiểm kê, bắt đầu kiểm kê (khóa kệ), nhập số đếm thực tế, hủy phiếu khi còn DRAFT hoặc IN_PROGRESS.

## 3. Functional Requirements (EARS)

### Tạo phiếu kiểm kê
* WHEN a Thủ kho creates a stocktake document, the system SHALL validate that the selected `accounting_period` is in status `OPEN`; if CLOSED, the system SHALL reject with `ACCOUNTING_PERIOD_CLOSED`.
* WHEN a Thủ kho creates a stocktake document, the system SHALL set status to `DRAFT` and NOT lock any locations yet.
* WHEN a Thủ kho creates a stocktake document, the system SHALL populate `stock_take_items` using current inventory quantities (`system_qty`) **excluding** any locations within zones of type `QUARANTINE`.

### Bắt đầu kiểm kê (khóa kệ)
* WHEN a Thủ kho starts a stocktake (`PUT .../start`), the system SHALL transition status from `DRAFT` to `IN_PROGRESS` and lock all warehouse locations referenced in `stock_take_items`.
* WHILE a stocktake is `IN_PROGRESS`, the system SHALL prevent any receipt, delivery order, transfer inbound, or transfer outbound transactions on the locked warehouse locations, returning `LOCATION_LOCKED`.

### Nhập số đếm thực tế
* WHEN a Thủ kho enters counted quantities, the system SHALL reject any `actual_qty` < 0 with `INVALID_COUNT_QTY`.
* WHEN a Thủ kho enters counted quantities, the system SHALL auto-calculate:
  * `variance_qty = actual_qty − system_qty`
  * `variance_value = variance_qty × cost_price` (cost_price tại thời điểm kiểm kê)
* WHEN a Thủ kho marks `is_employee_fault = true`, the system SHALL require a non-empty `notes` value on the affected `stock_take_items` row, returning `EMPLOYEE_FAULT_REASON_REQUIRED` otherwise.

### Hoàn tất đếm & trình duyệt
* WHEN a Thủ kho completes counting (`PUT .../complete`), the system SHALL:
  * Calculate `total_variance_value = SUM(variance_value)` across all items.
  * Determine `approval_level` theo bảng routing trong spec.md mục 4.
  * Transition status to `PENDING_APPROVAL`.
  * If `approval_level = AUTO`: immediately approve (xem EARS approval tại feature-manager).
* WHEN `complete` is called but any `stock_take_item` has no `actual_qty` recorded, the system SHALL reject with `400 INCOMPLETE_COUNT`.

### Hủy phiếu
* WHEN a Thủ kho cancels a stocktake (`PUT .../cancel`), the system SHALL:
  * Allow cancellation only when status is `DRAFT` or `IN_PROGRESS`.
  * Transition status to `CANCELLED`.
  * Release all location locks (if IN_PROGRESS).
* IF status is `PENDING_APPROVAL`, `APPROVED`, or `REJECTED`, the system SHALL reject cancellation with `STOCK_TAKE_NOT_CANCELLABLE`.

## 4. API Endpoints

| Method | Endpoint | Role | Mô tả |
|--------|----------|------|-------|
| `POST` | `/api/v1/stocktakes` | STORE_KEEPER | Tạo phiếu kiểm kê mới (status = DRAFT) |
| `GET` | `/api/v1/stocktakes` | STORE_KEEPER, MANAGER | Danh sách phiếu kiểm kê (filter by warehouse, status) |
| `GET` | `/api/v1/stocktakes/{id}` | STORE_KEEPER, MANAGER | Chi tiết phiếu kiểm kê kèm items |
| `PUT` | `/api/v1/stocktakes/{id}/start` | STORE_KEEPER | Bắt đầu kiểm kê, khóa vị trí kệ |
| `PUT` | `/api/v1/stocktakes/{id}/count` | STORE_KEEPER | Nhập kết quả đếm thực tế cho từng item |
| `PUT` | `/api/v1/stocktakes/{id}/complete` | STORE_KEEPER | Hoàn tất đếm, tính tổng chênh lệch, trình duyệt |
| `PUT` | `/api/v1/stocktakes/{id}/cancel` | STORE_KEEPER | Hủy phiếu (chỉ khi DRAFT hoặc IN_PROGRESS) |

## 5. Acceptance Criteria

**Scenario 1: Tự tính chênh lệch**
* Given sản phẩm A có `system_qty = 100` và `cost_price = 50,000 VND`
* When Thủ kho nhập `actual_qty = 90`
* Then hệ thống tính `variance_qty = -10` và `variance_value = -500,000 VND`.

**Scenario 2: Khóa vị trí trong lúc đếm**
* Given stocktake đang `IN_PROGRESS` trên location `WH-HP.A.01.1.01`
* When người dùng tạo DO picking từ location này
* Then hệ thống chặn giao dịch với lỗi `LOCATION_LOCKED`.

**Scenario 3: Loại trừ hàng Quarantine**
* Given location `WH-HP.QC.01` thuộc zone `QUARANTINE`
* When Thủ kho tạo phiếu kiểm kê cho kho Hải Phòng
* Then `stock_take_items` không chứa bất kỳ dòng nào có `location_id` thuộc zone QUARANTINE.

**Scenario 4: Chặn tạo phiếu vào kỳ đã đóng**
* Given `accounting_period` tháng 5/2026 có status `CLOSED`
* When Thủ kho tạo phiếu kiểm kê với `accounting_period_id` trỏ vào kỳ đó
* Then hệ thống từ chối với lỗi `ACCOUNTING_PERIOD_CLOSED`.

**Scenario 5: Hủy phiếu đang IN_PROGRESS**
* Given stocktake `ST-2026-001` đang `IN_PROGRESS`, location `WH-HP.A.01.1.01` đang bị khóa
* When Thủ kho gọi `PUT /api/v1/stocktakes/1/cancel`
* Then status chuyển sang `CANCELLED` và location lock được giải phóng.

**Scenario 6: Chặn hủy phiếu đã PENDING_APPROVAL**
* Given stocktake đang `PENDING_APPROVAL`
* When Thủ kho gọi cancel
* Then hệ thống từ chối với lỗi `STOCK_TAKE_NOT_CANCELLABLE`.

**Scenario 7: Lỗi nhân viên phải có ghi chú**
* Given Thủ kho đánh dấu `is_employee_fault = true` cho một item
* When không nhập `notes`
* Then hệ thống từ chối với lỗi `EMPLOYEE_FAULT_REASON_REQUIRED`.
