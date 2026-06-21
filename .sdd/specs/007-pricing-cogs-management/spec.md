# Feature Specification: Bảng giá & Giá vốn (Pricing & COGS)

**Spec ID**: 007-pricing-cogs-management
**Created**: 2026-05-30
**Updated**: 2026-06-17
**Status**: Draft
**Features**: US-WMS-14

---

## 1. Context and Goal

Giá bán và giá vốn hàng hóa thay đổi theo từng kỳ kinh doanh. Hệ thống cần duy trì lịch sử giá (`price_history`) để:

1. Tra cứu giá bán đúng kỳ khi lập Đơn xuất (DO) → tính doanh thu và kiểm tra Credit Check.
2. Ghi nhận giá vốn (COGS) tại ngày xuất hàng → phục vụ hạch toán P&L.
3. Cung cấp lịch sử biến động giá để so sánh, kiểm soát nội bộ.

Kế toán viên nhập bảng giá theo kỳ; Kế toán trưởng duyệt để kích hoạt. Chỉ bản giá đã `APPROVED` mới được phép sử dụng trong giao dịch.

### Features List
* [US-WMS-14a: Kế toán viên Quản lý Bản giá Đơn lẻ](./features/feature-accountant-price-entry-management.md)
* [US-WMS-14b: Kế toán viên Import Bảng giá từ Excel](./features/feature-accountant-price-import-excel.md)
* [US-WMS-14c: Kế toán trưởng Duyệt Bản giá](./features/feature-accountant-manager-price-approval.md)
* [US-WMS-14d: Hệ thống Tra cứu Giá & Snapshot COGS vào Giao dịch](./features/feature-system-cogs-calculation.md)

---

## 2. Clarifications

### Session 2026-06-17

**Q: `end_date` bắt buộc hay tùy chọn khi tạo?**
A: Bắt buộc (`NOT NULL`). Không cho phép tạo khoảng giá vô thời hạn. Phải luôn khai báo cả `effective_date` và `end_date`. Ngày `end_date` là ngày cuối cùng bản giá còn hiệu lực (inclusive). Ví dụ: `effective_date = 2026-06-01`, `end_date = 2026-06-30` → bản giá có hiệu lực từ ngày 1 đến hết ngày 30 tháng 6.

> **Lý do**: Bản giá vô thời hạn gây mơ hồ trong tra cứu và báo cáo. Kỳ kinh doanh của Phúc Anh được xác định rõ ràng (tháng/quý), nên không có nhu cầu giá "không có ngày kết thúc".

**Q: Nếu tạo bản giá mới có ngày trùng/giao với bản giá `APPROVED` đang tồn tại thì xử lý thế nào?**
A: Hệ thống từ chối tạo với lỗi `OVERLAPPING_EFFECTIVE_DATE` (HTTP 409). Không tự động đóng/thu hẹp bản giá cũ. Kế toán viên phải đảm bảo khoảng ngày không overlap với bất kỳ bản giá `APPROVED` nào cho cùng `product_id`.

> Overlap kiểm tra logic: `new_effective_date <= existing_end_date AND new_end_date >= existing_effective_date`. Bản giá `PENDING` hoặc `CANCELLED` không tham gia kiểm tra overlap.

**Q: Overlap check áp dụng ở thời điểm tạo hay thời điểm duyệt?**
A: Áp dụng ở **cả hai** thời điểm. Khi tạo (để feedback sớm cho Kế toán viên) và khi duyệt (để đảm bảo tính toàn vẹn — có thể có bản giá `APPROVED` khác được tạo trong khoảng thời gian chờ duyệt).

**Q: Bản giá `PENDING` có được sửa không?**
A: Có. Kế toán viên (người tạo) được sửa bất kỳ trường nào của bản giá `PENDING` trước khi Kế toán trưởng duyệt. Không cần xóa và tạo lại.

**Q: Có thể hủy/rút bản giá `PENDING` không?**
A: Có. Kế toán viên (người tạo) được hủy (cancel) bản giá `PENDING`. Khi hủy, `status` chuyển sang `CANCELLED`. Bản giá `CANCELLED` là immutable và không thể khôi phục.

**Q: Người tạo bản giá có thể tự duyệt không?**
A: Không. Áp dụng nguyên tắc Maker-Checker: Kế toán viên (role `ACCOUNTANT`) tạo; Kế toán trưởng (role `ACCOUNTANT_MANAGER`) duyệt. Đây là hai role khác nhau, RBAC tự động đảm bảo tách biệt này.

**Q: "Thông báo Kế toán trưởng" là cơ chế gì?**
A: In-app notification: hệ thống tạo một bản ghi trong bảng `notifications` cho Kế toán trưởng khi bản giá chuyển sang `PENDING`. Không tích hợp email/Zalo trong Sprint 1.

**Q: Giá bán (`selling_price`) có phân biệt theo Đại lý không?**
A: Không. Sprint 1 không có dealer-tier pricing — một bản giá áp dụng cho tất cả Đại lý trong cùng kho.

**Q: Giá có phân biệt theo kho không?**
A: Có. Mỗi bản giá (`price_history`) gắn với một `warehouse_id` cụ thể. Cùng một sản phẩm có thể có giá khác nhau tại Hải Phòng, Hà Nội và Hồ Chí Minh. Kế toán viên phải chọn kho khi tạo/import bản giá. Overlap check áp dụng trên `(product_id, warehouse_id)` — hai bản giá APPROVED cho cùng sản phẩm nhưng khác kho không bị coi là overlap.

**Q: COGS được lấy từ `price_history.cost_price` hay từ `receipt_items.unit_cost`?**
A: Từ `price_history.cost_price` tại ngày tạo DO, lọc theo `warehouse_id` của DO. Sprint 1 dùng **standard cost** (giá vốn kỳ) thay vì actual purchase cost (giá mua từng lô). `receipt_items.unit_cost` là giá mua ghi nhận trên phiếu nhập (để tính DebitNote, đối chiếu NCC) và là dữ liệu riêng biệt, không ảnh hưởng đến COGS trong module này.

> **Lý do**: Cách tiếp cận standard cost đơn giản hơn trong Sprint 1 và đủ cho nhu cầu P&L nội bộ của Phúc Anh.

**Q: Giá và COGS được snapshot ở thời điểm nào trong luồng DO?**
A: Tại thời điểm **tạo DO** (`POST /api/v1/delivery-orders`). Các trường `unit_price` và `unit_cost` được ghi snapshot vào `delivery_order_items` ngay khi DO được tạo. Nếu bảng giá thay đổi sau đó, DO đã tạo không bị ảnh hưởng.

**Q: DO có thể được tạo nếu không có bản giá `APPROVED` nào cho sản phẩm ở ngày hiện tại?**
A: Không. Hệ thống chặn tạo DO với lỗi `MISSING_PRICE` nếu bất kỳ dòng hàng nào trong DO không tìm thấy bản giá `APPROVED` có hiệu lực tại ngày tạo DO.

**Q: Excel import — scope ra sao?**
A: Kế toán viên có thể import nhiều dòng sản phẩm từ file Excel (`.xlsx`) để tạo hàng loạt bản giá `PENDING`. Mỗi dòng Excel tương ứng một bản ghi `price_history` độc lập. Import không tự duyệt. Khuôn dạng file Excel được quy định trong `feature-accountant-price-import-excel.md`. Validation lỗi trả về danh sách lỗi theo dòng, không chặn toàn bộ file nếu một số dòng hợp lệ.

---

## 3. Actors

| Actor | Role | Nghiệp vụ liên quan |
|-------|------|---------------------|
| Kế toán viên | Maker | Tạo, sửa, hủy bản giá `PENDING`; import Excel |
| Kế toán trưởng | Checker | Xem lịch sử, so sánh giá cũ/mới, duyệt bản giá (không có luồng từ chối tường minh trong Sprint 1) |
| System | Auto | Tra cứu giá tại ngày giao dịch, snapshot giá vào DO item, block DO khi thiếu giá |

---

## 4. Functional Requirements (EARS)

*Vui lòng xem chi tiết tại:*
* [EARS - Price Entry Management](./features/feature-accountant-price-entry-management.md#3-functional-requirements-ears)
* [EARS - Price Import Excel](./features/feature-accountant-price-import-excel.md#3-functional-requirements-ears)
* [EARS - Price Approval](./features/feature-accountant-manager-price-approval.md#3-functional-requirements-ears)
* [EARS - COGS Price Snapshot](./features/feature-system-cogs-calculation.md#3-functional-requirements-ears)

---

## 5. Non-functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-001 | Price lookup tại thời điểm tạo DO (p95) | ≤ 100ms |
| NFR-002 | Truy vấn lịch sử giá của một sản phẩm | ≤ 500ms |
| NFR-003 | Excel import 1000 SKUs | ≤ 10s |
| NFR-004 | Overlap validation khi tạo/duyệt bản giá | ≤ 200ms |

---

## 6. Data Model

### price_history

| Field | Type | Notes |
|-------|------|-------|
| `id` | BIGSERIAL (PK) | |
| `product_id` | BIGINT (FK→products, NOT NULL) | |
| `warehouse_id` | BIGINT (FK→warehouses, NOT NULL) | Kho áp dụng bản giá này |
| `effective_date` | DATE (NOT NULL) | Ngày bắt đầu hiệu lực (inclusive) |
| `end_date` | DATE (NOT NULL) | Ngày kết thúc hiệu lực (inclusive). Bắt buộc, không cho NULL |
| `cost_price` | DECIMAL(18,2) (NOT NULL, > 0) | Giá vốn kỳ — dùng cho COGS khi xuất hàng |
| `selling_price` | DECIMAL(18,2) (NOT NULL, > 0) | Giá bán — dùng cho invoice và Credit Check |
| `status` | VARCHAR(20) (NOT NULL) | CHECK IN ('PENDING', 'APPROVED', 'CANCELLED'); DEFAULT 'PENDING' |
| `created_by` | BIGINT (FK→users, NOT NULL) | Kế toán viên tạo |
| `approved_by` | BIGINT (FK→users) | Kế toán trưởng duyệt; NULL khi còn PENDING/CANCELLED |
| `approved_at` | TIMESTAMPTZ | Null cho đến khi APPROVED |
| `cancelled_by` | BIGINT (FK→users) | Kế toán viên hủy; NULL nếu không bị hủy |
| `cancelled_at` | TIMESTAMPTZ | Null cho đến khi CANCELLED |
| `notes` | TEXT | Ghi chú tùy chọn của Kế toán viên |
| `created_at` | TIMESTAMPTZ (DEFAULT NOW()) | |
| `updated_at` | TIMESTAMPTZ (DEFAULT NOW()) | |

**Constraints:**
- `CHECK (effective_date <= end_date)` — ngày bắt đầu phải trước hoặc bằng ngày kết thúc.
- `CHECK (cost_price > 0 AND selling_price > 0)`
- Không có UNIQUE index đơn giản trên `(product_id, warehouse_id, effective_date)` — thay vào đó dùng application-level overlap check để ngăn overlap giữa các bản `APPROVED`.
- **Overlap invariant** (enforced at service layer): không được tồn tại hai bản `APPROVED` cho cùng `(product_id, warehouse_id)` có khoảng ngày chồng nhau. Hai bản giá `APPROVED` cho cùng product nhưng khác `warehouse_id` không bị coi là overlap.

**Indexes:**
- `idx_price_history_product_warehouse_status` on `(product_id, warehouse_id, status, effective_date, end_date)` — tra cứu giá theo kho và ngày giao dịch.
- `idx_price_history_product_warehouse_created` on `(product_id, warehouse_id, created_at DESC)` — lịch sử giá theo kho và thời gian tạo.

### delivery_order_items (snapshot fields — đã tồn tại trong spec 004)

Hai trường sau được thêm vào `delivery_order_items` tại thời điểm tạo DO, snapshot từ `price_history`:

| Field | Type | Notes |
|-------|------|-------|
| `unit_price` | DECIMAL(18,2) (NOT NULL) | Snapshot từ `price_history.selling_price` tại ngày tạo DO |
| `unit_cost` | DECIMAL(18,2) (NOT NULL) | Snapshot từ `price_history.cost_price` tại ngày tạo DO |

> Nếu spec 004 đã định nghĩa những trường này, spec 007 chỉ xác nhận rằng giá trị phải được điền từ `price_history` (không để trống, không cho phép override thủ công).

### notifications (bảng bổ trợ — nếu chưa tồn tại)

| Field | Type | Notes |
|-------|------|-------|
| `id` | BIGSERIAL (PK) | |
| `recipient_id` | BIGINT (FK→users, NOT NULL) | |
| `type` | VARCHAR(50) (NOT NULL) | Ví dụ: `PRICE_PENDING_APPROVAL` |
| `reference_type` | VARCHAR(50) | `price_history` |
| `reference_id` | BIGINT | ID của bản ghi liên quan |
| `message` | TEXT | |
| `is_read` | BOOLEAN (DEFAULT false) | |
| `created_at` | TIMESTAMPTZ (DEFAULT NOW()) | |

---

## 7. API Spec

*Vui lòng xem chi tiết tại:*
* [APIs - Price Entry Management](./features/feature-accountant-price-entry-management.md#4-api-endpoints)
* [APIs - Price Import Excel](./features/feature-accountant-price-import-excel.md#4-api-endpoints)
* [APIs - Price Approval](./features/feature-accountant-manager-price-approval.md#4-api-endpoints)
* [APIs - COGS Price Snapshot](./features/feature-system-cogs-calculation.md#4-api-endpoints)

---

## 8. Error Handling

| Error Code | HTTP | Điều kiện |
|------------|------|-----------|
| `OVERLAPPING_EFFECTIVE_DATE` | 409 | Khoảng ngày `[effective_date, end_date]` giao với bản giá `APPROVED` đang tồn tại cho cùng `(product_id, warehouse_id)` (áp dụng khi tạo, sửa, và duyệt) |
| `INVALID_DATE_RANGE` | 400 | `effective_date > end_date` |
| `PRICE_ALREADY_APPROVED` | 409 | Thao tác sửa/hủy/duyệt trên bản giá đã ở trạng thái `APPROVED` |
| `PRICE_ALREADY_CANCELLED` | 409 | Thao tác sửa/hủy/duyệt trên bản giá đã ở trạng thái `CANCELLED` |
| `MISSING_PRICE` | 422 | Không tìm thấy bản giá `APPROVED` cho sản phẩm tại ngày tạo DO |
| `PRICE_NOT_FOUND` | 404 | ID bản giá không tồn tại |
| `FORBIDDEN_APPROVE_OWN` | 403 | Người tạo cố tự duyệt (phòng thủ; RBAC đã chặn qua role) |
| `EXCEL_FORMAT_INVALID` | 400 | File không phải `.xlsx`, không đọc được, hoặc thiếu cột bắt buộc (A–E) |
| `EXCEL_TOO_MANY_ROWS` | 400 | File có hơn 1.000 dòng dữ liệu — toàn bộ file bị từ chối |

---

## 9. Acceptance Criteria

*Vui lòng xem chi tiết tại:*
* [Acceptance - Price Entry Management](./features/feature-accountant-price-entry-management.md#5-acceptance-criteria)
* [Acceptance - Price Import Excel](./features/feature-accountant-price-import-excel.md#5-acceptance-criteria)
* [Acceptance - Price Approval](./features/feature-accountant-manager-price-approval.md#5-acceptance-criteria)
* [Acceptance - COGS Price Snapshot](./features/feature-system-cogs-calculation.md#5-acceptance-criteria)

---

## 10. Out of Scope

- Dynamic/promotional pricing hoặc discount tiers theo Đại lý
- Multi-currency pricing
- Dealer-segment giá khác nhau
- Giá theo kênh bán (B2B vs B2C)
- Giá dựa trên batch actual cost (FIFO actual cost COGS) — Sprint 1 dùng standard cost
- Auto-closing bản giá cũ khi duyệt bản giá mới (phải tự đảm bảo khoảng ngày không overlap)
- Approval workflow nhiều bước (hiện tại: 1 Kế toán trưởng duyệt là đủ)
- Email/Zalo notification khi bản giá chờ duyệt
- Price list versioning hoặc audit so sánh diff tự động trong UI
