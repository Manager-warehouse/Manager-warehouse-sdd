# Feature Specification: Xuất hàng & Giao hàng (Outbound & Delivery)

**Spec ID**: 004-outbound-delivery-pod
**Created**: 2026-05-30
**Status**: Draft
**Features**: 6 feature files covering US-WMS-06, US-WMS-07A, US-WMS-07B, US-WMS-08, US-WMS-09, US-WMS-10

---

## 1. Context and Goal

Xuất hàng là quy trình tạo doanh thu cho Phúc Anh. Planner nhận yêu cầu từ Công ty mẹ, hệ thống kiểm tra công nợ và tồn kho trước khi tạo Đơn xuất. Sau khi tạo thành công, Thủ kho lập kế hoạch lấy hàng theo danh sách batch/bin hợp lệ trong kho, sắp theo FIFO cho domain đồ gia dụng không quản lý hạn sử dụng; Nhân viên kho lấy hàng thực tế, kiểm tra chất lượng từng sản phẩm và nhập số lượng đạt/không đạt. Thủ kho phê duyệt chất lượng, xử lý hàng không đạt vào quarantine và chọn hàng thay thế nếu cần. Trưởng kho duyệt xuất kho trước khi Dispatcher xếp xe và tài xế. Khi giao thành công bằng POD + OTP, hệ thống tự động tạo invoice và ghi nhận công nợ; Kế toán theo dõi thanh toán từng đợt cho tới khi công nợ đơn hàng được tất toán.

### Features List

- [US-WMS-06: Lập Đơn xuất hàng & Tự động Kiểm tra Công nợ](features/feature-planner-delivery-order/feature-planner-delivery-order.md)
- [US-WMS-07A: Thủ kho Lập kế hoạch lấy hàng](features/feature-storekeeper-picking-plan/feature-storekeeper-picking-plan.md)
- [US-WMS-07B: Nhân viên kho Lấy hàng & QC Outbound](features/feature-warehouse-staff-picking-qc/feature-warehouse-staff-picking-qc.md)
- [US-WMS-08: Lập Chuyến xe & Vận chuyển Nội bộ](features/feature-dispatcher-trip-dispatch/feature-dispatcher-trip-dispatch.md)
- [US-WMS-09: Tài xế Xác nhận Giao hàng & Chữ ký Điện tử POD](features/feature-driver-mobile-pod/feature-driver-mobile-pod.md)
- [US-WMS-10: Kế toán Quản lý Invoice, Công nợ & Thanh toán](features/feature-accountant-receivable-payment/feature-accountant-receivable-payment.md)

## 2. Actors

| Actor           | Vai trò | Nghiệp vụ liên quan                                                                                                |
| --------------- | ------- | ------------------------------------------------------------------------------------------------------------------ |
| Planner         | Maker   | Lập Đơn xuất hàng (Delivery Order), kiểm tra tồn kho khả dụng và trạng thái công nợ Đại lý                         |
| Thủ kho         | Maker   | Lập kế hoạch lấy hàng, chọn một hoặc nhiều batch/bin/zone từ danh sách FIFO trong kho được gán, phê duyệt chất lượng outbound và chọn hàng thay thế khi QC fail |
| Nhân viên kho   | Maker   | Lấy hàng thực tế theo kế hoạch, kiểm tra chất lượng từng sản phẩm, nhập số lượng đã lấy, đạt QC và không đạt QC     |
| Dispatcher      | Maker   | Lập Chuyến xe nội bộ, gán xe và tài xế rảnh, sắp xếp thứ tự giao hàng                                              |
| Tài xế          | Maker   | Sử dụng smartphone xem chuyến xe, xác nhận nhận hàng (xe rời kho), giao hàng và ký nhận POD, báo cáo giao thất bại |
| Kế toán viên    | Maker   | Quản lý invoice/công nợ được tạo tự động sau giao hàng, ghi nhận từng đợt thanh toán kèm ảnh giao dịch             |
| Kế toán trưởng  | Checker | Phê duyệt Credit Limit cho Đại lý                                                                                  |
| Trưởng kho      | Checker | Ký duyệt xuất kho (giai đoạn Warehouse Approval)                                                                   |

## 3. Functional Requirements (EARS)

_Vui lòng xem chi tiết yêu cầu chức năng EARS tại các tài liệu đặc tả tính năng:_

- [EARS - Delivery Order](features/feature-planner-delivery-order/feature-planner-delivery-order.md#3-functional-requirements-ears)
- [EARS - Storekeeper Picking Plan](features/feature-storekeeper-picking-plan/feature-storekeeper-picking-plan.md#3-functional-requirements-ears)
- [EARS - Warehouse Staff Picking & QC](features/feature-warehouse-staff-picking-qc/feature-warehouse-staff-picking-qc.md#3-functional-requirements-ears)
- [EARS - Trip Dispatch](features/feature-dispatcher-trip-dispatch/feature-dispatcher-trip-dispatch.md#3-functional-requirements-ears)
- [EARS - Driver Mobile & POD](features/feature-driver-mobile-pod/feature-driver-mobile-pod.md#3-functional-requirements-ears)
- [EARS - Invoice & Receivable Payment](features/feature-accountant-receivable-payment/feature-accountant-receivable-payment.md#3-functional-requirements-ears)

## 4. Non-functional Requirements

| ID      | Requirement                                | Target                                       |
| ------- | ------------------------------------------ | -------------------------------------------- |
| NFR-001 | Credit check + reserve transaction         | ≤ 1s                                         |
| NFR-002 | POD image upload                           | ≤ 5s for 5MB image                           |
| NFR-003 | Trip creation with 10+ DOs                 | ≤ 2s                                         |
| NFR-004 | Concurrent order creation for same product | No oversell (optimistic locking)             |
| NFR-005 | Delivery OTP email delivery                | ≤ 30s under normal mail service availability |

## 5. Data Model

### delivery_orders

- `id` (BIGSERIAL, PK)
- `do_number` (VARCHAR(50), UNIQUE, NOT NULL)
- `dealer_id` (BIGINT, FK→dealers, NOT NULL)
- `warehouse_id` (BIGINT, FK→warehouses, NOT NULL)
- `type` (VARCHAR(30), CHECK IN ('SALE','DELIVERY'), NOT NULL)
- `expected_delivery_date` (DATE)
- `status` (VARCHAR(30), DEFAULT 'NEW', CHECK IN ('NEW','WAITING_PICKING','PICKING','QC_PENDING_APPROVAL','QC_COMPLETED','WAREHOUSE_APPROVED','IN_TRANSIT','RETURNED','DELIVERY_FAILED','COMPLETED','CLOSED','REJECTED','CANCELLED'))
- `created_by` (BIGINT, FK→users, NOT NULL)
- `cancel_reason` (TEXT)
- `rejection_reason` (TEXT)
- `closed_at` (TIMESTAMPTZ)
- `document_date` (DATE, NOT NULL)
- `accounting_period_id` (BIGINT, FK→accounting_periods)
- `notes` (TEXT)
- `created_at` (TIMESTAMPTZ)
- `updated_at` (TIMESTAMPTZ)

### delivery_order_items

- `id` (BIGSERIAL, PK)
- `do_id` (BIGINT, FK→delivery_orders, NOT NULL)
- `product_id` (BIGINT, FK→products, NOT NULL)
- `batch_id` (BIGINT, FK→batches) -- nullable summary of planned batch when a line is planned from one source; detailed multi-bin allocations live in `delivery_order_item_allocations`
- `location_id` (BIGINT, FK→warehouse_locations) -- nullable summary of planned bin when a line is planned from one source
- `zone_id` (BIGINT, FK→warehouse_zones) -- nullable summary of planned zone when a line is planned from one source
- `requested_qty` (DECIMAL(10,2), NOT NULL)
- `reserved_qty` (DECIMAL(10,2), DEFAULT 0)
- `planned_qty` (DECIMAL(10,2), DEFAULT 0)
- `picked_qty` (DECIMAL(10,2), DEFAULT 0)
- `qc_pass_qty` (DECIMAL(10,2), DEFAULT 0)
- `qc_fail_qty` (DECIMAL(10,2), DEFAULT 0)
- `issued_qty` (DECIMAL(10,2), DEFAULT 0)
- `unit_price` (DECIMAL(18,2)) -- Tra cứu từ price_history tại ngày giao

### delivery_order_item_allocations

- `id` (BIGSERIAL, PK)
- `do_item_id` (BIGINT, FK→delivery_order_items, NOT NULL)
- `inventory_id` (BIGINT, FK→inventories, NOT NULL)
- `batch_id` (BIGINT, FK→batches, NOT NULL)
- `location_id` (BIGINT, FK→warehouse_locations, NOT NULL)
- `zone_id` (BIGINT, FK→warehouse_zones, NOT NULL)
- `planned_qty` (DECIMAL(10,2), NOT NULL)
- `picked_qty` (DECIMAL(10,2), DEFAULT 0)
- `is_replacement` (BOOLEAN, DEFAULT false)
- `replaced_allocation_id` (BIGINT, FK→delivery_order_item_allocations)
- `created_by` (BIGINT, FK→users, NOT NULL)
- `created_at` (TIMESTAMPTZ)
- `updated_at` (TIMESTAMPTZ)
- `CHECK(planned_qty > 0)`

### delivery_order_item_return_to_bin_records

- `id` (BIGSERIAL, PK)
- `do_item_id` (BIGINT, FK→delivery_order_items, NOT NULL)
- `allocation_id` (BIGINT, FK→delivery_order_item_allocations, NOT NULL)
- `product_id` (BIGINT, FK→products, NOT NULL)
- `batch_id` (BIGINT, FK→batches, NOT NULL)
- `original_location_id` (BIGINT, FK→warehouse_locations, NOT NULL)
- `original_zone_id` (BIGINT, FK→warehouse_zones, NOT NULL)
- `source_location_id` (BIGINT, FK→warehouse_locations) -- outbound staging or current picked state when applicable
- `returned_qty` (DECIMAL(10,2), NOT NULL)
- `reason` (TEXT)
- `created_by` (BIGINT, FK→users, NOT NULL)
- `created_at` (TIMESTAMPTZ)
- `CHECK(returned_qty > 0)`

### warehouse_product_reservations

- `id` (BIGSERIAL, PK)
- `warehouse_id` (BIGINT, FK→warehouses, NOT NULL)
- `product_id` (BIGINT, FK→products, NOT NULL)
- `reserved_qty` (DECIMAL(10,2), NOT NULL, DEFAULT 0)
- `version` (INTEGER, NOT NULL, DEFAULT 0)
- `created_at` (TIMESTAMPTZ, NOT NULL)
- `updated_at` (TIMESTAMPTZ, NOT NULL)
- `UNIQUE(warehouse_id, product_id)`
- `CHECK(reserved_qty >= 0)`

### delivery_order_item_replacements

- `id` (BIGSERIAL, PK)
- `do_item_id` (BIGINT, FK→delivery_order_items, NOT NULL)
- `failed_inventory_id` (BIGINT, FK→inventories, NOT NULL)
- `replacement_inventory_id` (BIGINT, FK→inventories, NOT NULL)
- `failed_batch_id` (BIGINT, FK→batches, NOT NULL)
- `failed_location_id` (BIGINT, FK→warehouse_locations, NOT NULL)
- `replacement_batch_id` (BIGINT, FK→batches, NOT NULL)
- `replacement_location_id` (BIGINT, FK→warehouse_locations, NOT NULL)
- `quantity` (DECIMAL(10,2), NOT NULL)
- `reason` (TEXT, NOT NULL)
- `created_by` (BIGINT, FK→users, NOT NULL)
- `created_at` (TIMESTAMPTZ)

### outbound_qc_records

- `id` (BIGSERIAL, PK)
- `do_item_id` (BIGINT, FK→delivery_order_items, NOT NULL)
- `inspector_id` (BIGINT, FK→users, NOT NULL) -- Nhân viên kho trực tiếp kiểm tra sản phẩm
- `picked_qty` (DECIMAL(10,2), NOT NULL)
- `qc_pass_qty` (DECIMAL(10,2), NOT NULL)
- `qc_fail_qty` (DECIMAL(10,2), NOT NULL)
- `quarantine_record_id` (BIGINT, FK→quarantine_records)
- `notes` (TEXT)
- `created_at` (TIMESTAMPTZ)

### delivery_order_approvals

- `id` (BIGSERIAL, PK)
- `do_id` (BIGINT, FK→delivery_orders, NOT NULL)
- `approver_id` (BIGINT, FK→users, NOT NULL)
- `result` (VARCHAR(20), CHECK IN ('APPROVED','REJECTED'), NOT NULL)
- `contract_image_url` (VARCHAR(500))
- `rejection_reason` (TEXT)
- `approved_at` (TIMESTAMPTZ)

### delivery_order_warehouse_approvals

- `id` (BIGSERIAL, PK)
- `do_id` (BIGINT, FK→delivery_orders, NOT NULL)
- `approver_id` (BIGINT, FK→users, NOT NULL)
- `result` (VARCHAR(20), CHECK IN ('APPROVED','REJECTED'), NOT NULL)
- `notes` (TEXT)
- `approved_at` (TIMESTAMPTZ)

### invoices

- `id` (BIGSERIAL, PK)
- `do_id` (BIGINT, FK→delivery_orders, UNIQUE, NOT NULL)
- `invoice_number` (VARCHAR(50), UNIQUE, NOT NULL)
- `dealer_id` (BIGINT, FK→dealers, NOT NULL)
- `total_amount` (DECIMAL(18,2), NOT NULL)
- `outstanding_amount` (DECIMAL(18,2), NOT NULL)
- `status` (VARCHAR(30), CHECK IN ('OPEN','PARTIALLY_PAID','PAID'), NOT NULL)
- `created_at` (TIMESTAMPTZ)
- `updated_at` (TIMESTAMPTZ)

### invoice_payments

- `id` (BIGSERIAL, PK)
- `invoice_id` (BIGINT, FK→invoices, NOT NULL)
- `amount` (DECIMAL(18,2), NOT NULL)
- `transaction_image_url` (VARCHAR(500), NOT NULL)
- `status` (VARCHAR(30), CHECK IN ('PENDING_APPROVAL','APPROVED','REJECTED'), NOT NULL)
- `submitted_by` (BIGINT, FK→users, NOT NULL)
- `approved_by` (BIGINT, FK→users)
- `approved_at` (TIMESTAMPTZ)
- `rejection_reason` (TEXT)
- `created_at` (TIMESTAMPTZ)

### trips

- `id` (BIGSERIAL, PK)
- `trip_number` (VARCHAR(50), UNIQUE, NOT NULL)
- `vehicle_id` (BIGINT, FK→vehicles, NOT NULL)
- `driver_id` (BIGINT, FK→drivers, NOT NULL)
- `dispatcher_id` (BIGINT, FK→users, NOT NULL)
- `planned_date` (DATE, NOT NULL)
- `status` (VARCHAR(20), DEFAULT 'PLANNED', CHECK IN ('PLANNED','IN_TRANSIT','COMPLETED'))
- `total_weight_kg` (DECIMAL(10,2))
- `total_volume_m3` (DECIMAL(10,3))
- `created_at` (TIMESTAMPTZ)
- `updated_at` (TIMESTAMPTZ)

### trip_delivery_orders

- `id` (BIGSERIAL, PK)
- `trip_id` (BIGINT, FK→trips, NOT NULL)
- `do_id` (BIGINT, FK→delivery_orders, UNIQUE, NOT NULL)
- `stop_order` (INTEGER, NOT NULL)
- `UNIQUE(trip_id, stop_order)`

### deliveries (Proof of Delivery)

- `id` (BIGSERIAL, PK)
- `delivery_number` (VARCHAR(50), UNIQUE, NOT NULL)
- `do_id` (BIGINT, FK→delivery_orders, NOT NULL)
- `trip_id` (BIGINT, FK→trips)
- `vehicle_id` (BIGINT, FK→vehicles, NOT NULL)
- `driver_id` (BIGINT, FK→drivers, NOT NULL)
- `attempt_number` (INTEGER, NOT NULL)
- `status` (VARCHAR(30), DEFAULT 'PENDING', CHECK IN ('PENDING','IN_TRANSIT','OUT_FOR_DELIVERY','DELIVERED','FAILED','RETURNED'))
- `pod_image_url` (VARCHAR(500))
- `pod_signature_url` (VARCHAR(500))
- `pod_timestamp` (TIMESTAMPTZ)
- `otp_verified_at` (TIMESTAMPTZ) -- chỉ lưu thời điểm xác thực thành công; không lưu raw OTP trong bản ghi POD
- `failure_reason` (TEXT)
- `dispatched_at` (TIMESTAMPTZ)
- `delivered_at` (TIMESTAMPTZ)
- `created_at` (TIMESTAMPTZ)
- `updated_at` (TIMESTAMPTZ)
- `UNIQUE(do_id, attempt_number)`

### delivery_otp_attempts

- `id` (BIGSERIAL, PK)
- `delivery_id` (BIGINT, FK→deliveries, NOT NULL)
- `otp_hash` (VARCHAR(255), NOT NULL) -- hash/verifier only; raw OTP is never stored
- `recipient_email` (VARCHAR(255), NOT NULL)
- `expires_at` (TIMESTAMPTZ, NOT NULL)
- `consumed_at` (TIMESTAMPTZ)
- `attempt_count` (INTEGER, DEFAULT 0)
- `created_at` (TIMESTAMPTZ)

### inventories (shared)

- `id` (BIGSERIAL, PK)
- `warehouse_id` (BIGINT, FK→warehouses, NOT NULL) -- kho vật lý hoặc kho ảo `IN_TRANSIT`
- `product_id` (BIGINT, FK→products, NOT NULL)
- `batch_id` (BIGINT, FK→batches, NOT NULL)
- `location_id` (BIGINT, FK→warehouse_locations, NOT NULL)
- `total_qty` (DECIMAL(10,2), NOT NULL)
- `reserved_qty` (DECIMAL(10,2), NOT NULL)
- `cost_price` (DECIMAL(18,2), NOT NULL)
- `version` (INTEGER, NOT NULL)
- `CHECK(total_qty >= 0)`
- `CHECK(reserved_qty >= 0)`
- `CHECK(total_qty - reserved_qty >= 0)`

### Inventory Versioning Rules

All outbound mutations that update `inventories.total_qty`, `inventories.reserved_qty`, or `warehouse_product_reservations.reserved_qty` SHALL validate and increment the corresponding `version` using optimistic locking.

- Delivery order creation SHALL calculate warehouse-level availability from valid regular quality-passed inventory as `sum(inventories.total_qty - inventories.reserved_qty) - warehouse_product_reservations.reserved_qty` for each requested warehouse/product pair.
- Delivery order creation SHALL reserve requested product quantity on `delivery_order_items.reserved_qty` and `warehouse_product_reservations.reserved_qty` at the selected warehouse and SHALL NOT update `inventories.reserved_qty`, `batch_id`, or `location_id`.
- Delivery order creation SHALL create or update its `warehouse_product_reservations` rows in the same transaction as the availability check to prevent oversell across concurrent Planner requests.
- Storekeeper picking plan SHALL assign the Delivery Order item reserved quantities to one or more concrete batch/bin/zone rows from the FIFO-ranked valid regular inventory list, decrease the matching `warehouse_product_reservations.reserved_qty`, and increase affected `inventories.reserved_qty` with version checks before moving the Delivery Order from `NEW` to `WAITING_PICKING`.
- Storekeeper picking plan SHALL validate that total allocation quantity for each Delivery Order item equals the requested quantity before saving.
- Storekeeper picking plan changes while the Delivery Order is `WAITING_PICKING` SHALL release removed concrete reservations, reserve newly selected concrete inventory, and keep every item fully allocated to its requested quantity.
- Picking plan changes after physical picking has started SHALL use the same picking-plan update request with `returnToBinRecords` payload, return picked goods from changed picked allocations to their original batch/bin/location, and create `PICKED_GOODS_RETURN_TO_BIN` audit entries before the revised plan is saved.
- Picking plan changes SHALL require `returnToBinRecords` only for picked allocations that are removed or reduced by the revised plan; unchanged picked allocations SHALL remain in place and SHALL NOT require return records.
- Delivery order cancellation before warehouse approval SHALL release any remaining `warehouse_product_reservations.reserved_qty` and any concrete `inventories.reserved_qty` already assigned by picking plan, depending on the Delivery Order's current lifecycle status.
- Warehouse staff picking SHALL move picked quantity from the planned bin to an outbound staging location inside the same warehouse; all affected inventory rows SHALL pass version checks.
- Outbound QC fail SHALL move failed quantity from outbound staging to quarantine, create a quarantine record, create an inventory adjustment record for the failed quantity, decrease regular valid inventory, and keep failed quantity out of available inventory.
- Replacement picking SHALL update the Delivery Order item plan, create replacement history, and require the replacement goods to go through warehouse staff picking and QC again.
- Warehouse manager rejection SHALL move QC-passed goods from outbound staging back to their original bin/location, release reservations, keep failed goods in quarantine, and end the Delivery Order as `REJECTED`.
- Trip departure SHALL move QC-approved goods from outbound staging to virtual In-Transit inventory in one transaction; all affected inventory rows SHALL pass version checks.
- Delivery confirmation SHALL decrease virtual In-Transit inventory in the same transaction that marks the delivery order as `COMPLETED` and creates the invoice/receivable.
- Delivery failure SHALL NOT change inventory quantity; goods remain in virtual In-Transit inventory until handled by the separate return flow.
- Billing/payment mutations SHALL NOT change inventory quantity and do not require an inventory version check.
- On any version conflict, the system SHALL rollback the whole mutation and return `409 INVENTORY_VERSION_CONFLICT`.

### Delivery Order Lifecycle Rules

- Happy path SHALL be: `NEW` → `WAITING_PICKING` → `PICKING` → `QC_PENDING_APPROVAL` → `QC_COMPLETED` → `WAREHOUSE_APPROVED` → `IN_TRANSIT` → `COMPLETED` → `CLOSED`.
- A newly created Delivery Order SHALL start in `NEW`.
- Storekeeper planning SHALL choose one or more batch/bin/zone allocations from valid regular quality-passed stock in the assigned warehouse, ranked by oldest received date first using FIFO.
- Storekeeper planning SHALL NOT require expiry date or FEFO selection because the current household-goods domain does not track expiry.
- After storekeeper saves a complete picking plan from `NEW`, the Delivery Order SHALL move directly to `WAITING_PICKING`.
- While a Delivery Order is `WAITING_PICKING`, the storekeeper MAY change the picking plan if each item remains fully allocated and no physical pick has started.
- If physical picking has started, the system SHALL require the picking-plan update payload to include valid `returnToBinRecords` only for picked allocations being removed or reduced; the revised picking plan SHALL be saved only after those returned goods are recorded back to their original batch/bin/location.
- Warehouse staff SHALL move the Delivery Order to `PICKING` while physically taking goods and checking product quality.
- Warehouse staff SHALL enter picked quantity, QC pass quantity, and QC fail quantity before the Delivery Order can move to `QC_PENDING_APPROVAL`.
- Storekeeper quality approval SHALL move the Delivery Order to `QC_COMPLETED` only when all requested quantities have QC-passed goods available after any required replacements.
- Warehouse manager approval SHALL move the Delivery Order to `WAREHOUSE_APPROVED`, making it eligible for dispatcher trip planning.
- Warehouse manager rejection SHALL move the Delivery Order to `REJECTED`; the flow ends and any later outbound attempt for the same business request must use a new Delivery Order.
- Dealer refusal at delivery SHALL move the Delivery Order to `RETURNED`; the goods remain in virtual In-Transit inventory until the separate return flow receives them.
- When the separate return flow confirms returned goods back into warehouse custody, the Delivery Order SHALL move to `DELIVERY_FAILED`.
- After successful POD + OTP confirmation, the system SHALL auto-create invoice/receivable and move the Delivery Order directly to `COMPLETED`.
- The Delivery Order SHALL move to `CLOSED` only after the invoice receivable has been fully paid and approved by accounting.

### Delivery Lifecycle Rules

- Each `deliveries` record represents one physical delivery attempt for one Delivery Order.
- A Delivery Order MAY have multiple `deliveries` records only if it is dispatched again before being closed as `DELIVERY_FAILED`; Sprint 1 normally uses one active delivery attempt per dispatched Delivery Order.
- The system SHALL create a new `deliveries` record whenever goods are dispatched for another delivery attempt.
- POD upload and delivery confirmation SHALL update only the current attempt's `deliveries` record and SHALL NOT overwrite previous `FAILED`, `RETURNED`, or `DELIVERED` attempts.
- If a dealer refuses receipt or delivery fails at the delivery point, the current `deliveries` record SHALL be closed with status `FAILED` and the Delivery Order SHALL move to `RETURNED` while goods remain tracked in virtual In-Transit inventory.
- If the goods are later returned to a warehouse, the same `deliveries` record MAY be marked `RETURNED` or linked to the separate return record created by the return flow.
- After a returned Delivery Order is closed as `DELIVERY_FAILED`, any later outbound attempt for the same business request SHALL use a new Delivery Order.

### Trip Completion Rules

- A trip SHALL move to `COMPLETED` only when every Delivery Order assigned to the trip has reached a terminal delivery outcome.
- Terminal delivery outcomes for Sprint 1 are `COMPLETED` and `RETURNED`.
- A trip with mixed successful and failed delivery orders MAY still be `COMPLETED` when all assigned orders have either been completed or recorded as returned for follow-up.

### Delivery Order Status Semantics

- `WAREHOUSE_APPROVED`: the warehouse manager approved outbound release and the Delivery Order is eligible for dispatcher trip planning.
- `RETURNED`: the dealer refused or the delivery attempt failed; goods remain in virtual In-Transit inventory until the separate return flow receives them.
- `DELIVERY_FAILED`: returned goods have been received back by the separate return flow and the outbound order is closed as unsuccessful.
- `COMPLETED`: the dealer received goods, POD + OTP verification succeeded, invoice was auto-created, and receivable was recognized.
- `CLOSED`: the invoice receivable for the order has been fully paid and approved by accounting.

### Authorization and Warehouse Scope Rules

- Every outbound API SHALL enforce both role permission and warehouse assignment.
- Planner, Thủ kho, Nhân viên kho, Trưởng kho, and Dispatcher users SHALL only create, view, approve, pick, QC, dispatch, or mutate Delivery Orders for warehouses assigned to their user account.
- Drivers SHALL only view and update trips and delivery attempts assigned to their driver profile.
- Accountant users SHALL only view invoices, receivables, and payments for warehouses assigned to their user account, unless their role is explicitly configured for company-wide accounting access.
- System Admin and CEO roles MAY have cross-warehouse visibility, but every mutation SHALL still write audit logs with actor, role, warehouse, entity, before state, and after state.

## 6. API Spec

_Vui lòng xem chi tiết API endpoints tại các tài liệu đặc tả tính năng:_

- [APIs - Delivery Order](features/feature-planner-delivery-order/feature-planner-delivery-order.md#4-api-endpoints)
- [APIs - Storekeeper Picking Plan](features/feature-storekeeper-picking-plan/feature-storekeeper-picking-plan.md#4-api-endpoints)
- [APIs - Warehouse Staff Picking & QC](features/feature-warehouse-staff-picking-qc/feature-warehouse-staff-picking-qc.md#4-api-endpoints)
- [APIs - Trip Dispatch](features/feature-dispatcher-trip-dispatch/feature-dispatcher-trip-dispatch.md#4-api-endpoints)
- [APIs - Driver Mobile & POD](features/feature-driver-mobile-pod/feature-driver-mobile-pod.md#4-api-endpoints)
- [APIs - Invoice & Receivable Payment](features/feature-accountant-receivable-payment/feature-accountant-receivable-payment.md#4-api-endpoints)

## 7. Error Handling

| Error                      | HTTP | Condition                               |
| -------------------------- | ---- | --------------------------------------- |
| CREDIT_HOLD                | 422  | Dealer credit limit exceeded or overdue |
| INSUFFICIENT_STOCK         | 422  | available_qty < requested_qty           |
| VEHICLE_OVERLOAD           | 422  | Trip exceeds vehicle capacity           |
| DO_NOT_READY               | 400  | DO is not in the required status for the requested transition |
| DO_NOT_WAREHOUSE_APPROVED  | 400  | DO not in WAREHOUSE_APPROVED status for trip planning |
| QC_REPLACEMENT_REQUIRED    | 422  | QC pass quantity is lower than requested quantity and replacement is not completed |
| PICKING_PLAN_QTY_MISMATCH  | 422  | Planned allocation quantity does not equal requested quantity |
| PICKED_GOODS_RETURN_REQUIRED | 422 | Picking plan cannot remove or reduce a picked allocation until the request includes valid return-to-bin records for that changed allocation |
| WAREHOUSE_REJECTED         | 422  | Warehouse manager rejected outbound release |
| PAYMENT_APPROVAL_REQUIRED  | 422  | Payment cannot reduce receivable before Chief Accountant approval |
| MISSING_POD                | 400  | POD signature/image required            |
| DELIVERY_OTP_INVALID       | 400  | OTP is incorrect or not issued for this delivery order |
| DELIVERY_OTP_EXPIRED       | 400  | OTP has expired                         |
| INVENTORY_VERSION_CONFLICT | 409  | Concurrent inventory update             |
| WAREHOUSE_SCOPE_FORBIDDEN  | 403  | User role is valid but user is not assigned to the target warehouse, trip, or delivery attempt |

### Audit Trail

- Every outbound mutation SHALL create an audit log with `actor`, `role`, `warehouse_id`, `action`, `entity_type`, `entity_id`, `entity_code`, `timestamp`, `before`, and `after`.
- `DELIVERY_ORDER_CREATE`: create DO and reserve requested product quantity on Delivery Order items plus `warehouse_product_reservations` at warehouse level; final batch/bin/location is selected by Storekeeper during picking planning.
- `DELIVERY_ORDER_CANCEL`: cancel DO before warehouse approval and release aggregate and/or concrete inventory reservation according to current lifecycle status.
- `PICKING_PLAN_SAVE`: storekeeper selects one or more batch/bin/zone allocations from FIFO-ranked valid regular inventory and moves DO from `NEW` to `WAITING_PICKING`, or changes the plan while DO remains `WAITING_PICKING`.
- `PICKED_GOODS_RETURN_TO_BIN`: storekeeper changes a picking plan after physical picking has started; the system records picked goods from removed or reduced allocations returned to their original batch/bin/location before applying the revised plan.
- `DELIVERY_ORDER_PICK_START`: warehouse staff moves DO to `PICKING`.
- `DELIVERY_ORDER_PICK_COMPLETE`: warehouse staff records picked, QC pass, and QC fail quantities.
- `OUTBOUND_QC_FAIL_QUARANTINE`: move failed quantity to quarantine, create quarantine record, create inventory adjustment record, and decrease valid regular inventory.
- `PICKING_REPLACEMENT_SAVE`: store replacement batch/bin/zone and replacement history when QC fail requires substitute goods.
- `DELIVERY_ORDER_QC_APPROVE`: storekeeper approves quality and moves DO to `QC_COMPLETED`.
- `DELIVERY_ORDER_WAREHOUSE_APPROVE`: warehouse manager moves DO to `WAREHOUSE_APPROVED`.
- `DELIVERY_ORDER_WAREHOUSE_REJECT`: store warehouse rejection reason, release QC-passed goods back to bin, keep failed goods in quarantine, and move DO to `REJECTED`.
- `TRIP_CREATE`: create trip, assign vehicle/driver, and store stop order.
- `TRIP_DEPART`: move trip and DOs to `IN_TRANSIT`, move goods from outbound staging to virtual In-Transit, and release `reserved_qty`.
- `DELIVERY_ATTEMPT_CREATE`: create a new physical delivery attempt record for a dispatched Delivery Order.
- `OTP_REQUEST`: generate raw OTP, send it to the dealer/receiver email, and store only the hashed verifier in `delivery_otp_attempts` with expiry metadata.
- `OTP_CONFIRM`: verify OTP against the active `delivery_otp_attempts` record, mark the attempt consumed, store verification timestamp, auto-create invoice/receivable, and move DO to `COMPLETED`.
- `DELIVERY_FAIL`: store failure reason, close the current delivery attempt as `FAILED`, and move DO to `RETURNED`; returned goods remain tracked in virtual In-Transit inventory until a separate return flow receives and classifies them.
- `RETURN_FLOW_CONFIRMED`: separate return flow confirms returned goods back into warehouse custody and moves DO to `DELIVERY_FAILED`.
- `TRIP_COMPLETE`: mark trip `COMPLETED` only after all assigned DOs are `COMPLETED` or `RETURNED`.
- `INVOICE_AUTO_CREATE_FROM_DO`: create invoice and receivable automatically after successful POD + OTP.
- `PAYMENT_SUBMIT`: accountant records a payment attempt with transaction image and `PENDING_APPROVAL` status.
- `PAYMENT_APPROVE`: chief accountant approves payment, decreases outstanding receivable, and moves invoice/DO to `PAID`/`CLOSED` if fully settled.
- `PAYMENT_REJECT`: chief accountant rejects a payment attempt with reason; receivable remains unchanged.

## 8. Acceptance Criteria

_Vui lòng xem chi tiết kịch bản kiểm thử tại các tài liệu đặc tả tính năng:_

- [Acceptance - Delivery Order](features/feature-planner-delivery-order/feature-planner-delivery-order.md#5-acceptance-criteria)
- [Acceptance - Storekeeper Picking Plan](features/feature-storekeeper-picking-plan/feature-storekeeper-picking-plan.md#5-acceptance-criteria)
- [Acceptance - Warehouse Staff Picking & QC](features/feature-warehouse-staff-picking-qc/feature-warehouse-staff-picking-qc.md#5-acceptance-criteria)
- [Acceptance - Trip Dispatch](features/feature-dispatcher-trip-dispatch/feature-dispatcher-trip-dispatch.md#5-acceptance-criteria)
- [Acceptance - Driver Mobile & POD](features/feature-driver-mobile-pod/feature-driver-mobile-pod.md#5-acceptance-criteria)
- [Acceptance - Invoice & Receivable Payment](features/feature-accountant-receivable-payment/feature-accountant-receivable-payment.md#5-acceptance-criteria)

## 9. Out of Scope

- Real-time GPS tracking of delivery vehicles
- Route optimization algorithm (manual stop ordering for Sprint 1)
- Electronic signature capture hardware integration
- Multi-leg / hub-and-spoke delivery model
- COD (Cash on Delivery) payment processing
