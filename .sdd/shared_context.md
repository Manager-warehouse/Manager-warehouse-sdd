# Shared Context — WMS Phúc Anh

> Nguồn sự thật chung để đồng bộ giữa các AI Agent làm việc trên codebase này.
> Nguồn: AGENTS.md · CLAUDE.md · README.md · constitution.md · database.md

## 1. Tổng quan dự án

| Field         | Giá trị                                        |
| ------------- | ---------------------------------------------- |
| Tên dự án     | Warehouse Management System (WMS)              |
| Công ty       | Phúc Anh                                       |
| Giai đoạn     | Sprint 1 — Core Warehouse Operations           |
| Số kho vật lý | 3: Hải Phòng, Hà Nội, Hồ Chí Minh              |
| Kho ảo        | 1 kho In-Transit (điều chuyển nội bộ)          |
| Zone đặc biệt | Quarantine Zone (hàng lỗi QC)                  |
| Scale         | 1,000+ SKU, 50+ Đại lý, 1,000+ giao dịch/tháng |
| Vận tải       | Chỉ xe nội bộ Phúc Anh — KHÔNG 3PL             |

## 2. Actors & Roles (10 Actors, 3 tầng)

### Tầng Quản trị

| Actor        | Vai trò                                          |
| ------------ | ------------------------------------------------ |
| CEO          | Dashboard chiến lược, duyệt yêu cầu điều chuyển liên kho do Trưởng kho đề xuất |
| System Admin | Quản lý tài khoản, phân quyền, cấu hình hệ thống |

### Tầng Quản lý (Checker)

| Actor                     | Vai trò                                               |
| ------------------------- | ----------------------------------------------------- |
| Trưởng kho kiêm Trưởng QC | Duyệt nhập/xuất/điều chuyển, đề xuất điều chuyển khi kho mình thiếu hàng, xử lý chênh lệch thực tế |
| Kế toán trưởng            | Duyệt bảng giá, thiết lập Credit Limit, chốt sổ       |

### Tầng Nghiệp vụ (Maker)

| Actor                        | Vai trò                                                                                                                   |
| ---------------------------- | ------------------------------------------------------------------------------------------------------------------------- |
| Planner                      | Tiếp nhận đơn, lập lệnh nhập/đơn xuất, credit check                                                                       |
| Dispatcher                   | Lập chuyến xe nội bộ, gán tài xế, tối ưu lộ trình                                                                         |
| Thủ kho                      | Tiếp nhận hàng, soạn hàng, kiểm kê, cất Bin                                                                               |
| Nhân viên kho (Bốc xếp & QC) | Bốc xếp, QC inbound/outbound, di chuyển hàng lỗi                                                                          |
| Kế toán viên                 | Xử lý thanh toán và theo dõi công nợ trong luồng tài chính riêng                                                          |
| Tài xế                       | Nhận chuyến (smartphone), upload `goodsImage`/`signDocumentImage`, nhập OTP Đại lý, báo giao thất bại, xác nhận xe về kho |

## 3. Domain Glossary

| Thuật ngữ         | Định nghĩa                                                                                      |
| ----------------- | ----------------------------------------------------------------------------------------------- |
| **Batch**         | Lô hàng gom theo sản phẩm, nguồn nhập/chứng từ và ngày nhận; domain hiện tại không quản lý hạn dùng hoặc grade |
| **Bin Location**  | Vị trí kệ trong kho — mã hóa WH-Zone.Rack.Shelf.Bin                                             |
| **Putaway**       | Quy trình cất hàng vào Bin sau khi QC đạt                                                       |
| **FEFO**          | Ngoài phạm vi hiện tại vì hàng gia dụng không quản lý hạn sử dụng                               |
| **FIFO**          | First In First Out — ưu tiên xuất batch nhập trước                                              |
| **Quarantine**    | Khu cách ly hàng lỗi QC — không available                                                       |
| **In-Transit**    | Kho ảo — hàng đang vận chuyển giữa 2 kho                                                        |
| **TRQ**           | Yêu cầu điều chuyển do Trưởng kho kho thiếu lập, CEO duyệt trước khi Planner tạo `TRF-*`         |
| **TRF**           | Phiếu điều chuyển nội bộ thực thi, luôn đi qua In-Transit và một chuyến `TTR-*` riêng             |
| **TTR**           | Chuyến xe điều chuyển nội bộ, `trip_type = TRANSFER`, gắn đúng một phiếu `TRF-*` trong Sprint 1  |
| **POD**           | Proof of Delivery — `goodsImage` + `signDocumentImage` + timestamp + OTP email 6 số đã xác thực |
| **COGS**          | Cost of Goods Sold — giá vốn hàng bán                                                           |
| **Credit Hold**   | Trạng thái chặn xuất hàng do nợ quá hạn/vượt hạn mức                                            |
| **RTV**           | Return to Vendor — trả hàng lỗi về NCC                                                          |
| **Maker-Checker** | Nguyên tắc: người tạo ≠ người duyệt                                                             |

## 4. Module Map & Dependencies

```
Master Data ──→ Inbound ──→ Inventory ──→ Outbound ──→ Finance
                    │                        │
                    └── QC ──→ Quarantine ───┘
                              │
                              └── RTV / Tiêu hủy
```

| Module            | Phụ thuộc vào                 | Phụ thuộc vào nó                       |
| ----------------- | ----------------------------- | -------------------------------------- |
| Auth/Users        | —                             | Tất cả                                 |
| Products          | —                             | Inbound, Outbound, Inventory           |
| Warehouses        | —                             | Inbound, Outbound, Transfer, Inventory |
| Inbound (Receipt) | Products, Warehouses, Users   | Inventory                              |
| QC                | Products, Warehouses          | Inbound, Outbound                      |
| Inventory         | Products, Warehouses, Batches | Outbound, Transfer, Stocktake          |
| Outbound (DO)     | Inventory, Dealers, Users     | Finance, Transport                     |
| Transfer          | Inventory, Warehouses         | Inventory                              |
| Stocktake         | Inventory                     | Adjustments                            |
| Finance           | Outbound, Dealers             | —                                      |
| Transport         | Outbound, Vehicles, Drivers   | —                                      |

## 5. Entity Relationships (Core)

```
Warehouse (1) ──→ WarehouseLocation (N)
Product (1) ──→ PriceHistory (N)
Batch (N) ──→ Inventory (N) ──→ Product (1)
Inventory (N) ──→ WarehouseLocation (1)
Receipt (1) ──→ ReceiptItem (N) ──→ Product (1)
DeliveryOrder (1) ──→ DeliveryOrderItem (N) ──→ Inventory (1)
Transfer (1) ──→ TransferItem (N)
Transfer (1) ──→ Trip (1, trip_type = TRANSFER)
Transfer (1) ──→ TransferWrongSkuReport (N)
TransferRequest (1) ──→ TransferRequestItem (N)
TransferRequest (1) ──→ Transfer (0..1)
Trip (1) ──→ TripDeliveryOrder (N) ──→ DeliveryOrder (1)
Invoice (N) ──→ DeliveryOrder (1)
PaymentReceipt (N) ──→ Invoice (1)
Dealer (1) ──→ Invoice (N) ──→ PaymentReceipt (N)
```

## 6. Status Flows

### Receipt (Lệnh nhập)

```
PENDING_RECEIPT → DRAFT → QC_COMPLETED → APPROVED
                                            ↓
                                         REJECTED
```

### Delivery Order (Đơn xuất)

```
NEW → WAITING_PICKING → QC_PENDING_APPROVAL → QC_COMPLETED → WAREHOUSE_APPROVED → IN_TRANSIT → COMPLETED → CLOSED
                                ↓                    ↓
                         QC fail replacement     REJECTED
                                ↓
                         WAITING_PICKING
                                                                    ↓
                                                               RETURNED
                                                                    ↓
                                                            DELIVERY_FAILED
```

Warehouse staff records picked/QC results exactly once for the complete current picking plan while the Delivery Order is `WAITING_PICKING`; the system does not use a separate `PICKING` status. Each QC result line is keyed by `doItemId + allocationId + batchId + locationId + zoneId`, and payload batch/location/zone must match the planned allocation. The QC result always moves the DO to `QC_PENDING_APPROVAL`, even when QC pass is lower than requested quantity, so Storekeeper can decide replacement goods. Picking plan edits use `PUT /api/v1/delivery-orders/{id}/picking-plan`. `allocations[]` represents the full replacement picking plan; `returnToBinRecords[]` is required only for picked allocations that are removed or reduced by the revised plan. Picked allocations that remain unchanged do not require return-to-bin records. Warehouse rejection returns all QC-passed goods from outbound staging to original bins, requires total returned quantity to equal pass quantity in staging, releases reservation, and records `PICKED_GOODS_RETURN_TO_BIN`.

Outbound delivery trips use `trip_type = DELIVERY` and group at least one `WAREHOUSE_APPROVED` Delivery Order from the same warehouse. Dispatcher, vehicle, and driver must belong to that warehouse, and a Delivery Order can belong to only one active trip. Planned trip updates treat `deliveryOrders[]` as the final revised list, may add/remove/reorder DOs before departure, and must re-run validations while ignoring the current trip for active-assignment checks. Cancelled trips keep DOs in `WAREHOUSE_APPROVED`, retain historical vehicle/driver references, and release vehicle/driver from active assignment. At driver departure, goods move from outbound staging to virtual `IN_TRANSIT` inventory and delivery attempts are created with status `IN_TRANSIT`; Sprint 1 does not use `OUT_FOR_DELIVERY`. Drivers can only view/update trips and attempts assigned to their driver profile. POD requires `goodsImage` and `signDocumentImage`, each image under 5MB. Delivery OTP is a random 6-digit code valid for 5 minutes, with exactly one OTP row per delivery attempt. Resend while the OTP is still active is rejected; resend after expiry updates the same OTP row. OTP locks after 3 incorrect submissions and requires Admin reset with reason before a new code can be generated on the same row. Successful verification marks it `VERIFIED`/consumed. Delivery is full DO only; confirming one DO deducts only that DO from In-Transit and auto-creates invoice/receivable for that DO. Dealer refusal moves the DO to `RETURNED` while goods stay in In-Transit for the separate return flow. A trip becomes `COMPLETED` only when the driver confirms the vehicle returns to the source warehouse and all assigned DOs are `COMPLETED` or `RETURNED`.

### Transfer (Điều chuyển)

```
TRANSFER_REQUEST: DRAFT → SUBMITTED → CEO_APPROVED → CONVERTED
                      ↓          ↓
                 CANCELLED   CEO_REJECTED

TRF: NEW → APPROVED → IN_TRANSIT → COMPLETED
      ↓       ↓            ↓            ↓
 REJECTED CANCELLED    COMPLETED_WITH_DISCREPANCY / QUARANTINED
```

Transfer-specific invariants:

- Luồng chuẩn: `TRQ draft -> submit -> CEO approve -> Planner revalidate & convert once -> Source manager reserve FIFO eligible -> Dispatcher capacity/overlap plan -> pick + outbound QC + load/handover -> driver depart -> IN_TRANSIT -> driver arrive/handover -> blind count -> storekeeper count/QC/bin-capacity check -> manager final confirmation`.
- Trưởng kho nguồn approval reserves planned quantity immediately.
- Trưởng kho kho thiếu hàng may view cross-warehouse availability read-only and submit a transfer request to CEO; CEO approval does not reserve or move stock.
- Planner creates `TRF-*` from an external instruction or at most one CEO-approved transfer request.
- Reservation must use FIFO-eligible stock only: active source warehouse/location, non-quarantine, non-locked, positive available quantity.
- Each transfer has exactly one dedicated internal trip; multi-transfer trips are out of scope.
- Transfer trip assignment must calculate weight/volume from transfer lines, check source-scoped vehicle/driver, overlapping assignments, vehicle weight capacity, and volume only when configured.
- Vehicle/driver/trip may be reassigned only before departure; terminal release must not mark a resource available if it has another active assignment.
- Source storekeeper must pass outbound QC and record load/handover before driver departure; confirmation is photo-based, not Barcode/QR-based.
- Driver departure confirmation moves stock from source warehouse to In-Transit.
- Driver arrival and receiving-warehouse handover are required before any receive-count action.
- Receiving starts with blind count, then storekeeper count/QC/bin-capacity check, then manager final confirmation.
- received_qty > sent_qty is blocked.
- QC-passed stock must go to a valid non-quarantine Bin and pass bin capacity checks before inventory is increased.
- QC-failed or physically damaged transfer quantity goes to Quarantine with `INTERNAL_TRANSFER` origin, is excluded from available inventory, and can only enter the Spec 009 disposal path; supplier RTV/Debit Note is blocked.
- Missing transfer quantity creates incident/discrepancy plus `TRANSFER_DISCREPANCY` adjustment/audit; it must not become quarantine stock or a disposal candidate.
- Physical over-receipt is blocked from regular inventory posting and must be captured as discrepancy hold/incident.
- Intact wrong-SKU transfer stock remains In-Transit; destination Storekeeper reports item-level expected SKU, actual SKU, affected quantity, reason, and photo refs when available; destination Warehouse Manager approves/rejects return; the same driver/vehicle returns to the source warehouse for source count/check/QC/final receive.
- Approved wrong-SKU or overdue return requires return departure, source arrival/handover, and source-side receiving; merely toggling `is_returned` is not sufficient.
- Overdue trips still in `IN_TRANSIT` block destination receive actions and require Return to Source by an authorized source manager, Admin, CEO, or Planner with reason and photo refs when available.
- Cancellation rules: Planner may cancel NEW; Trưởng kho nguồn/manager may cancel APPROVED and release reserved quantity; cancellation is blocked from REJECTED or IN_TRANSIT onward.
- Transfer, transfer request, trip/resource, and inventory mutations require version/concurrency protection; GET/list/detail endpoints must not mutate persisted business state.
- Applied Flyway migrations are immutable; schema fixes must use the next additive migration.
- Audit snapshots for transfer mutations must include header, items, allocations, QC quantities, wrong-SKU lines, trip/resource state, and inventory movement references.

### Dealer Status

```
ACTIVE ↔ CREDIT_HOLD
```

## 7. Audit Log Structure

| Field          | Bắt buộc | Mô tả                                       |
| -------------- | -------- | ------------------------------------------- |
| `actor_id`     | Có       | ID người thực hiện                          |
| `actor_role`   | Có       | Vai trò                                     |
| `action`       | Có       | CREATE / UPDATE / APPROVE / REJECT / CANCEL |
| `entity_type`  | Có       | Loại đối tượng (Receipt, DO, Invoice...)    |
| `entity_id`    | Có       | ID đối tượng                                |
| `old_value`    | Không    | Giá trị trước (JSON)                        |
| `new_value`    | Có       | Giá trị sau (JSON)                          |
| `warehouse_id` | Không    | Mã kho (nếu có)                             |
| `timestamp`    | Có       | Thời gian (UTC+7)                           |

## 8. Key Domain Constants

| Constant                    | Value           | Ghi chú                                    |
| --------------------------- | --------------- | ------------------------------------------ |
| Monthly closing day         | 25              | Ngày chốt sổ kỳ kế toán hàng tháng         |
| Min stock warning threshold | 10              | Ngưỡng cảnh báo tồn kho tối thiểu mặc định |
| Overdue days                | 30              | Quá hạn > 30 → auto CREDIT_HOLD            |
| bcrypt cost factor          | 12              | Mức tối thiểu                              |
| Net terms                   | 30 hoặc 60 ngày | Theo hồ sơ Đại lý                          |
| Max PR size                 | 400 lines       | Chia nhỏ nếu lớn hơn                       |
| Min test coverage           | 80%             | Services                                   |

## 9. GitHub Branch Naming

| Prefix    | Mục đích           |
| --------- | ------------------ |
| `feat/*`  | Tính năng mới      |
| `fix/*`   | Sửa lỗi            |
| `spec/*`  | Spec/documentation |
| `chore/*` | Maintenance        |
