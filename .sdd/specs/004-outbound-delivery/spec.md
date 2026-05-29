# Feature Specification: Xuất hàng & Giao hàng (Outbound & Delivery)

**Spec ID**: 004-outbound-delivery
**Created**: 2026-05-30
**Status**: Draft
**Features**: US-WMS-06, US-WMS-07, US-WMS-08, US-WMS-09, US-WMS-10

---

## 1. Context and Goal

Xuất hàng là quy trình tạo doanh thu cho Phúc Anh. Planner nhận yêu cầu từ Công ty mẹ,
kiểm tra credit + tồn kho, lập Đơn xuất. Thủ kho soạn hàng, QC Outbound kiểm tra đóng gói,
Dispatcher lập chuyến xe nội bộ, Tài xế giao hàng và ký POD. Kế toán lập hóa đơn.

**Goal:** Xây dựng luồng xuất hàng hoàn chỉnh từ lập đơn đến giao hàng và lập hóa đơn,
tích hợp credit check tự động, reserve inventory, QC Outbound, POD, và tracking chuyến xe.

## 2. Actors

| Actor | Vai trò |
|-------|---------|
| Planner | Lập đơn xuất, credit check, reserve |
| Thủ kho | Soạn hàng từ Bin (Picking) |
| Nhân viên kho (QC) | QC Outbound đóng gói |
| Dispatcher | Lập chuyến xe, gán tài xế |
| Tài xế | Nhận chuyến, giao hàng, ký POD |
| Kế toán viên | Lập hóa đơn từ đơn Delivered |

## 3. Functional Requirements (EARS)

**Ubiquitous:**
- The system SHALL always perform an automatic credit check BEFORE creating a
  delivery order. IF the dealer's status is CREDIT_HOLD, the system SHALL block
  order creation with a clear reason.
- The system SHALL always reserve inventory (`reserved_qty += quantity`) upon
  successful delivery order creation.
- The system SHALL always release reserved inventory when a delivery order is
  CANCELLED or transitions to IN_TRANSIT.

**Event-driven:**
- WHEN a Planner creates a delivery order, the system SHALL:
  - Validate: `available_qty = total_qty - reserved_qty ≥ requested_qty`
  - IF insufficient stock, the system SHALL suggest alternative warehouses
  - Apply FEFO/FIFO to select batch for each item
  - Reserve inventory: `reserved_qty += quantity`
- WHEN a Thủ kho completes picking, the system SHALL update status to PICKING.
- WHEN a Nhân viên kho confirms QC Outbound passed, the system SHALL allow the
  Thủ kho to set status to READY_TO_SHIP.
- WHEN a Dispatcher creates a trip, the system SHALL:
  - Validate that all selected DOs are in READY_TO_SHIP status
  - Validate total weight/volume against vehicle capacity
- WHEN a Tài xế confirms goods loaded (vehicle departs), the system SHALL:
  - Deduct `total_qty -= picked_qty`, release `reserved_qty -= picked_qty`
  - Update trip status to IN_TRANSIT, DO status to IN_TRANSIT
- WHEN a Tài xế confirms delivery (POD signed), the system SHALL:
  - Update DO status to DELIVERED
  - Save POD image + signature + timestamp
  - Notify Kế toán viên to create invoice
- WHEN a Tài xế reports delivery failure, the system SHALL:
  - Update DO status to RETURNED
  - Create a quarantine receipt for returned goods

## 4. Non-functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-001 | Credit check + reserve transaction | ≤ 1s |
| NFR-002 | POD image upload | ≤ 5s for 5MB image |
| NFR-003 | Trip creation with 10+ DOs | ≤ 2s |
| NFR-004 | Concurrent order creation for same product | No oversell (optimistic locking) |

## 5. Data Model

### DeliveryOrder
- `id`, `do_code` (UNIQUE), `warehouse_id` (FK), `dealer_id` (FK), `created_by` (FK),
  `status` (NEW → PICKING → READY_TO_SHIP → IN_TRANSIT → DELIVERED / RETURNED / CANCELLED),
  `total_amount`, `credit_check_status`, `version`

### DeliveryOrderItem
- `id`, `delivery_order_id` (FK), `product_id` (FK), `batch_id` (FK),
  `quantity`, `picked_qty`, `price`, `subtotal`

### Trip
- `id`, `trip_code` (UNIQUE), `warehouse_id` (FK), `driver_id` (FK→User),
  `vehicle_plate`, `status`, `stop_order` (JSON array of DO ids),
  `departed_at`, `completed_at`

### POD (Proof of Delivery)
- Embedded in DO: `pod_signature` (image/text), `pod_image`, `pod_notes`,
  `pod_confirmed_at`, `pod_confirmed_by`

## 6. API Spec

### Delivery Orders
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | /api/v1/delivery-orders | Bearer | List DOs (filterable) |
| POST | /api/v1/delivery-orders | PLANNER | Create DO with credit check |
| GET | /api/v1/delivery-orders/{id} | Bearer | Get DO detail |
| PUT | /api/v1/delivery-orders/{id}/pick | STORE_KEEPER | Pick items, set PICKING |
| PUT | /api/v1/delivery-orders/{id}/qc-outbound | WAREHOUSE_STAFF | QC check |
| PUT | /api/v1/delivery-orders/{id}/ship | STORE_KEEPER | Mark READY_TO_SHIP |
| PUT | /api/v1/delivery-orders/{id}/cancel | PLANNER | Cancel DO, release reserve |

### Trips
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | /api/v1/trips | Bearer | List trips |
| POST | /api/v1/trips | DISPATCHER | Create trip |
| PUT | /api/v1/trips/{id}/depart | DRIVER | Confirm departure, deduct stock |
| PUT | /api/v1/trips/{id}/confirm-delivery | DRIVER | POD confirmation |
| PUT | /api/v1/trips/{id}/fail-delivery | DRIVER | Report delivery failure |

## 7. Error Handling

| Error | HTTP | Condition |
|-------|------|-----------|
| CREDIT_HOLD | 422 | Dealer credit limit exceeded or overdue |
| INSUFFICIENT_STOCK | 422 | available_qty < requested_qty |
| VEHICLE_OVERLOAD | 422 | Trip exceeds vehicle capacity |
| DO_NOT_READY | 400 | DO not in READY_TO_SHIP status |
| MISSING_POD | 400 | POD signature/image required |
| INVENTORY_VERSION_CONFLICT | 409 | Concurrent inventory update |

## 8. Acceptance Criteria

1. Given a dealer with balance 480M and credit_limit 500M,
   when Planner creates a DO worth 30M,
   then system SHALL block with CREDIT_HOLD (480M + 30M ≥ 500M).
2. Given a product with total_qty = 100, reserved_qty = 30,
   when Planner creates a DO with qty = 80,
   then system SHALL allow (available = 70 < 80 → block or suggest alternative).
3. Given a DO with items picked and QC passed,
   when Tài xế confirms departure,
   then total_qty SHALL decrease by picked_qty, reserved_qty SHALL decrease by picked_qty.
4. Given a DO marked DELIVERED,
   when system checks,
   then Kế toán viên SHALL receive a notification to create invoice.

## 9. Out of Scope

- Real-time GPS tracking of delivery vehicles
- Route optimization algorithm (manual stop ordering for Sprint 1)
- Electronic signature capture hardware integration
- Multi-leg / hub-and-spoke delivery model
- COD (Cash on Delivery) payment processing
