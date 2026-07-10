# Feature: Hệ thống Tra cứu Giá & Snapshot COGS vào Giao dịch (US-WMS-14d)

## 1. Context and Goal

Khi Planner tạo Đơn xuất hàng (Delivery Order), hệ thống phải tự động tra cứu giá `APPROVED` có hiệu lực tại ngày hôm nay và snapshot `unit_price` (giá bán) + `unit_cost` (giá vốn) vào từng dòng của `delivery_order_items`. Nếu bất kỳ dòng nào thiếu giá, toàn bộ DO bị từ chối.

> **Effective-date-only model** (Session 2026-07-09 của `spec.md`): `price_history` không còn `end_date`. "Giá có hiệu lực tại ngày D" là bản `APPROVED` của `(product_id, warehouse_id)` có `effective_date <= D`, lớn nhất (gần D nhất). Vì bản giá cũ không bao giờ cần sửa để nhường chỗ cho bản mới, tại mọi thời điểm luôn tồn tại đúng một giá hiệu lực (hoặc không có, nếu chưa từng tạo giá nào) — không có khoảng trống giữa lúc một bản giá "hết hạn" và bản kế tiếp "bắt đầu" như mô hình `end_date` cũ.

Sprint 1 dùng **standard cost** (giá kỳ từ `price_history.cost_price`) — không phải actual FIFO batch cost từ `receipt_items.unit_cost`. Giá được snapshot tại thời điểm tạo DO và không thay đổi dù bảng giá sau đó bị thay thế.

---

## 2. Actor

**System** — tự động; không có UI riêng.  
**Planner (`PLANNER`)** — người kích hoạt gián tiếp khi tạo DO; nhận lỗi nếu thiếu giá.

---

## 3. Functional Requirements (EARS)

### 3.1 Ubiquitous

- The system SHALL always look up `price_history` WHERE `status = 'APPROVED' AND warehouse_id = :doWarehouseId AND effective_date <= :today`, ordered by `effective_date DESC`, taking the first row, for each product line when a DO is created.
- The system SHALL snapshot `selling_price` → `delivery_order_items.unit_price` and `cost_price` → `delivery_order_items.unit_cost` at DO creation time.
- The system SHALL NOT recalculate or allow overriding these snapshotted values after DO creation.
- The system SHALL NOT use `PENDING` or `CANCELLED` price entries for any transaction lookup.

### 3.2 Event-driven

**Khi tạo Delivery Order (`POST /api/v1/delivery-orders`, định nghĩa đầy đủ tại spec 004)**

- WHEN Planner creates a DO with one or more product lines:
  - For each `delivery_order_item`, query: `SELECT * FROM price_history WHERE product_id = ? AND warehouse_id = :doWarehouseId AND status = 'APPROVED' AND effective_date <= CURRENT_DATE ORDER BY effective_date DESC LIMIT 1`.
  - IF no matching row found for ANY item → reject the entire DO with HTTP 422 `MISSING_PRICE`, listing which `product_id`(s) are missing a price.
  - IF all items have a matching row → snapshot `selling_price` into `unit_price` and `cost_price` into `unit_cost` on each `delivery_order_items` row.
  - Continue with DO creation (credit check, inventory reservation) as defined in spec 004.

**Khi lập Invoice (`POST /api/v1/invoices`, định nghĩa tại spec 008)**

- WHEN Kế toán viên creates an invoice for a Delivered DO:
  - Use the already-snapshotted `unit_price` from `delivery_order_items` to compute `line_total` and `total_amount`.
  - SHALL NOT re-query `price_history`. Price was locked at DO creation.

**Khi DO chuyển sang IN_TRANSIT**

- No price recalculation. Snapshotted values remain authoritative.

### 3.3 State-driven

- WHILE no `APPROVED` price entry exists for a product at the DO's warehouse on `CURRENT_DATE`, any DO line containing that product SHALL be blocked.
- WHILE a price entry is `PENDING` or `CANCELLED`, it SHALL be invisible to all transaction-time lookups.

### 3.4 COGS trong báo cáo P&L (scope spec 010, ghi nhận ở đây để tránh mơ hồ)

- COGS per DO = Σ (`delivery_order_items.unit_cost × quantity`)
- Gross margin per DO = Σ (`unit_price × quantity`) − COGS
- Sprint 1 không điều chỉnh COGS theo actual batch purchase cost. Standard cost từ `price_history` là nguồn duy nhất.

---

## 4. API Endpoints

Không có endpoint độc lập. Logic tích hợp vào:

| Endpoint (spec khác) | Điểm tích hợp |
|----------------------|---------------|
| `POST /api/v1/delivery-orders` (spec 004) | Lookup + snapshot `unit_price`, `unit_cost`; block nếu `MISSING_PRICE` |
| `POST /api/v1/invoices` (spec 008) | Dùng `unit_price` đã snapshot, không re-lookup |

**Endpoint tra cứu giá tiện ích (optional, dùng cho UI preview)**

`GET /api/v1/price-history/lookup?product_id={id}&warehouse_id={warehouseId}&date={YYYY-MM-DD}`

- Trả về bản giá `APPROVED` có `effective_date` lớn nhất không vượt quá `date`, cho kho `warehouse_id`, hoặc HTTP 404 nếu không tồn tại bản `APPROVED` nào với `effective_date <= date`.
- Dùng để hiển thị preview giá trước khi Planner tạo DO.

---

## 5. Acceptance Criteria

**Scenario 1: DO tạo thành công khi có giá hợp lệ**
- Given product P có APPROVED tại kho Hải Phòng: effective 01/06, cost 80.000, sell 115.000
- When Planner tạo DO xuất từ kho Hải Phòng chứa P ngày 15/06
- Then `delivery_order_items.unit_price = 115.000`, `unit_cost = 80.000`
- And DO được tạo thành công

**Scenario 1b: Giá kho khác không được dùng**
- Given product P có APPROVED tại kho Hà Nội ngày 15/06, nhưng không có APPROVED tại kho Hải Phòng ngày 15/06
- When Planner tạo DO xuất từ kho Hải Phòng chứa P ngày 15/06
- Then HTTP 422 `MISSING_PRICE` — giá tại kho Hà Nội không áp dụng cho DO của Hải Phòng

**Scenario 2: DO bị chặn vì thiếu giá tại kho**
- Given product P không có APPROVED nào tại kho DO có hiệu lực ngày 15/07
- When Planner tạo DO chứa P ngày 15/07
- Then HTTP 422 `MISSING_PRICE` chỉ rõ product P bị thiếu giá
- And DO không được tạo

**Scenario 3: DO chứa nhiều sản phẩm — một thiếu giá**
- Given DO tại kho Hải Phòng có 3 dòng: product A và B có giá APPROVED tại Hải Phòng, product C không có giá tại Hải Phòng
- When Planner tạo DO
- Then HTTP 422 `MISSING_PRICE` chỉ rõ product C
- And toàn bộ DO bị từ chối (không tạo dòng nào)

**Scenario 4: Giá không thay đổi sau khi bảng giá được cập nhật**
- Given DO-001 tạo ngày 15/06 với unit_price = 115.000 (giá tháng 6)
- And ngày 01/07 bảng giá tháng 7 được APPROVED với selling_price = 120.000
- When Kế toán viên lập Invoice cho DO-001
- Then Invoice tính theo unit_price = 115.000 (snapshot tại ngày tạo DO)
- And KHÔNG dùng 120.000

**Scenario 5: PENDING price không được dùng khi tạo DO**
- Given product P có PENDING với effective_date 01/07 nhưng không có APPROVED nào với effective_date <= 01/07
- When Planner tạo DO ngày 01/07
- Then HTTP 422 `MISSING_PRICE` (PENDING không được tính)

**Scenario 6: Lookup tiện ích — giá cũ tiếp tục hiệu lực cho đến khi có bản mới hơn**
- Given product P có APPROVED tại kho Hải Phòng: effective 01/06, chưa có bản `APPROVED` nào mới hơn
- When GET /price-history/lookup?product_id=P&warehouse_id=1&date=2026-06-15
- Then trả về bản effective 01/06 với cost/selling price tương ứng
- When GET với date=2026-07-01 (vẫn chưa có bản `APPROVED` nào mới hơn 01/06)
- Then vẫn trả về bản effective 01/06 (giá cũ tiếp tục hiệu lực vô thời hạn cho đến khi có bản kế tiếp)
- When GET với date=2026-05-01 (trước effective_date của bản giá đầu tiên của product P tại kho này)
- Then HTTP 404 — chưa từng có bản `APPROVED` nào hiệu lực trước ngày đó
