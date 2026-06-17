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
| CEO          | Dashboard chiến lược                             |
| System Admin | Quản lý tài khoản, phân quyền, cấu hình hệ thống |

### Tầng Quản lý (Checker)

| Actor                     | Vai trò                                               |
| ------------------------- | ----------------------------------------------------- |
| Trưởng kho kiêm Trưởng QC | Duyệt nhập/xuất/điều chuyển, xử lý chênh lệch thực tế |
| Kế toán trưởng            | Duyệt bảng giá, thiết lập Credit Limit, chốt sổ       |

### Tầng Nghiệp vụ (Maker)

| Actor                        | Vai trò                                                                 |
| ---------------------------- | ----------------------------------------------------------------------- |
| Planner                      | Tiếp nhận đơn, lập lệnh nhập/đơn xuất, credit check                     |
| Dispatcher                   | Lập chuyến xe nội bộ, gán tài xế, tối ưu lộ trình                       |
| Thủ kho                      | Tiếp nhận hàng, soạn hàng, kiểm kê, cất Bin                             |
| Nhân viên kho (Bốc xếp & QC) | Bốc xếp, QC inbound/outbound, di chuyển hàng lỗi                        |
| Kế toán viên                 | Lập hóa đơn, ghi nhận thanh toán, cấn trừ công nợ                       |
| Tài xế                       | Nhận chuyến (smartphone), upload `goodsImage`/`signDocumentImage`, nhập OTP Đại lý, báo giao thất bại, xác nhận xe về kho |

## 3. Domain Glossary

| Thuật ngữ         | Định nghĩa                                                                                              |
| ----------------- | ------------------------------------------------------------------------------------------------------- |
| **Batch**         | Lô hàng nhập cùng đợt, cùng grade; domain hiện tại không quản lý hạn dùng                                |
| **Bin Location**  | Vị trí kệ trong kho — mã hóa WH-Zone.Rack.Shelf.Bin                                                     |
| **Putaway**       | Quy trình cất hàng vào Bin sau khi QC đạt                                                               |
| **FEFO**          | Ngoài phạm vi hiện tại vì hàng gia dụng không quản lý hạn sử dụng                                       |
| **FIFO**          | First In First Out — ưu tiên xuất batch nhập trước                                                      |
| **Quarantine**    | Khu cách ly hàng lỗi QC — không available                                                               |
| **In-Transit**    | Kho ảo — hàng đang vận chuyển giữa 2 kho                                                                |
| **POD**           | Proof of Delivery — `goodsImage` + `signDocumentImage` + timestamp + OTP email 6 số đã xác thực |
| **COGS**          | Cost of Goods Sold — giá vốn hàng bán                                                                   |
| **Credit Hold**   | Trạng thái chặn xuất hàng do nợ quá hạn/vượt hạn mức                                                    |
| **RTV**           | Return to Vendor — trả hàng lỗi về NCC                                                                  |
| **Maker-Checker** | Nguyên tắc: người tạo ≠ người duyệt                                                                     |

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
NEW → APPROVED → IN_TRANSIT → COMPLETED
                                ↓
                         COMPLETED_WITH_DISCREPANCY
```

Transfer-specific invariants:

- Trưởng kho nguồn approval reserves planned quantity immediately.
- Each transfer has exactly one dedicated internal trip; multi-transfer trips are out of scope.
- Driver departure confirmation moves stock from source warehouse to In-Transit.
- Thủ kho đích records received counts and QC; Trưởng kho đích confirms final receipt.
- received_qty > sent_qty is blocked.
- QC-failed received quantity goes to Quarantine and is excluded from available inventory.
- Cancellation is allowed only before IN_TRANSIT.

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
