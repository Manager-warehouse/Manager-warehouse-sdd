# Feature Specification: Xuất hàng & Giao hàng (Outbound & Delivery)

**Spec ID**: 004-outbound-delivery-pod
**Created**: 2026-05-30
**Status**: Draft
**Features**: 6 feature files covering US-WMS-06, US-WMS-07, US-WMS-08, US-WMS-09, US-WMS-10

---

## 1. Context and Goal

Xuất hàng là quy trình tạo doanh thu cho Phúc Anh. Planner nhận yêu cầu từ Công ty mẹ, kiểm tra credit + tồn kho, lập Đơn xuất. Thủ kho soạn hàng và kiểm QC Outbound, Dispatcher lập chuyến xe nội bộ, Tài xế giao hàng và ký POD. Kế toán lập hóa đơn.

### Features List

- [US-WMS-06: Lập Đơn xuất hàng & Tự động Kiểm tra Công nợ](features/feature-planner-delivery-order/feature-planner-delivery-order.md)
- [Thủ kho Soạn hàng tại Kệ](features/feature-storekeeper-picking/feature-storekeeper-picking.md)
- [US-WMS-07: Nhân viên QC Kiểm tra Đóng gói Outbound](features/feature-qc-outbound-inspection/feature-qc-outbound-inspection.md)
- [US-WMS-08: Lập Chuyến xe & Vận chuyển Nội bộ](features/feature-dispatcher-trip-dispatch/feature-dispatcher-trip-dispatch.md)
- [US-WMS-09: Tài xế Xác nhận Giao hàng & Chữ ký Điện tử POD](features/feature-driver-mobile-pod/feature-driver-mobile-pod.md)
- [US-WMS-10: Kế toán Tiếp nhận Thông báo Lập Hóa đơn](features/feature-accountant-billing-notification/feature-accountant-billing-notification.md)

## 2. Actors

| Actor           | Vai trò | Nghiệp vụ liên quan                                                                                                |
| --------------- | ------- | ------------------------------------------------------------------------------------------------------------------ |
| Planner         | Maker   | Lập Đơn xuất hàng (Delivery Order), kiểm tra tồn kho khả dụng và trạng thái công nợ Đại lý                         |
| Thủ kho kiêm QC | Maker   | Nhận đơn xuất, soạn hàng từ Bin theo batch đã reserve (Picking), kiểm QC Outbound và cập nhật trạng thái đơn       |
| Nhân viên kho   | Maker   | Hỗ trợ bốc xếp, di chuyển hàng hóa theo chỉ dẫn của Thủ kho                                                        |
| Dispatcher      | Maker   | Lập Chuyến xe nội bộ, gán xe và tài xế rảnh, sắp xếp thứ tự giao hàng                                              |
| Tài xế          | Maker   | Sử dụng smartphone xem chuyến xe, xác nhận nhận hàng (xe rời kho), giao hàng và ký nhận POD, báo cáo giao thất bại |
| Kế toán viên    | Maker   | Nhận thông báo đơn hàng Delivered, lập Hóa đơn bán hàng                                                            |
| Kế toán trưởng  | Checker | Phê duyệt Credit Limit cho Đại lý                                                                                  |
| Trưởng kho      | Checker | Ký duyệt xuất kho (giai đoạn Warehouse Approval)                                                                   |

## 3. Functional Requirements (EARS)

_Vui lòng xem chi tiết yêu cầu chức năng EARS tại các tài liệu đặc tả tính năng:_

- [EARS - Delivery Order](features/feature-planner-delivery-order/feature-planner-delivery-order.md#3-functional-requirements-ears)
- [EARS - Picking](features/feature-storekeeper-picking/feature-storekeeper-picking.md#3-functional-requirements-ears)
- [EARS - Outbound QC](features/feature-qc-outbound-inspection/feature-qc-outbound-inspection.md#3-functional-requirements-ears)
- [EARS - Trip Dispatch](features/feature-dispatcher-trip-dispatch/feature-dispatcher-trip-dispatch.md#3-functional-requirements-ears)
- [EARS - Driver Mobile & POD](features/feature-driver-mobile-pod/feature-driver-mobile-pod.md#3-functional-requirements-ears)
- [EARS - Billing Notification](features/feature-accountant-billing-notification/feature-accountant-billing-notification.md#3-functional-requirements-ears)

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
- `type` (VARCHAR(30), CHECK IN ('SALE','DELIVERY','ADJUSTMENT'), NOT NULL)
- `expected_delivery_date` (DATE)
- `status` (VARCHAR(30), DEFAULT 'NEW', CHECK IN ('NEW','PICKING','PENDING_WAREHOUSE_APPROVAL','READY_TO_SHIP','IN_TRANSIT','DELIVERED','RETURNED','COMPLETED','CLOSED','CANCELLED'))
- `created_by` (BIGINT, FK→users, NOT NULL)
- `cancel_reason` (TEXT)
- `document_date` (DATE, NOT NULL)
- `accounting_period_id` (BIGINT, FK→accounting_periods)
- `notes` (TEXT)
- `created_at` (TIMESTAMPTZ)
- `updated_at` (TIMESTAMPTZ)

### delivery_order_items

- `id` (BIGSERIAL, PK)
- `do_id` (BIGINT, FK→delivery_orders, NOT NULL)
- `product_id` (BIGINT, FK→products, NOT NULL)
- `batch_id` (BIGINT, FK→batches) -- lô tồn kho được reserve; FIFO mặc định, FEFO chỉ áp dụng cho sản phẩm có expiry/được cấu hình
- `location_id` (BIGINT, FK→warehouse_locations) -- Bin nhỏ nhất nơi lấy hàng
- `requested_qty` (DECIMAL(10,2), NOT NULL)
- `reserved_qty` (DECIMAL(10,2), DEFAULT 0)
- `issued_qty` (DECIMAL(10,2), DEFAULT 0)
- `unit_price` (DECIMAL(18,2)) -- Tra cứu từ price_history tại ngày giao

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

All outbound mutations that update `inventories.total_qty` or `inventories.reserved_qty` SHALL validate and increment `inventories.version` using optimistic locking.

- Delivery order creation SHALL reserve inventory only if the selected inventory rows still match the expected version.
- Delivery order cancellation SHALL release reserved inventory only if the reserved inventory rows still match the expected version.
- Trip departure SHALL update source warehouse inventory and virtual In-Transit inventory in one transaction; all affected inventory rows SHALL pass version checks.
- Delivery confirmation SHALL decrease virtual In-Transit inventory in the same transaction that marks the delivery order as `DELIVERED`.
- Delivery failure SHALL NOT change inventory quantity; goods remain in virtual In-Transit inventory until handled by the separate return flow.
- Billing notification SHALL NOT change inventory quantity and does not require an inventory version check.
- On any version conflict, the system SHALL rollback the whole mutation and return `409 INVENTORY_VERSION_CONFLICT`.

### Delivery Lifecycle Rules

- Each `deliveries` record represents one physical delivery attempt for one Delivery Order.
- A Delivery Order MAY have multiple `deliveries` records over its lifecycle.
- The system SHALL create a new `deliveries` record whenever goods are dispatched for another delivery attempt.
- POD upload and delivery confirmation SHALL update only the current attempt's `deliveries` record and SHALL NOT overwrite previous `FAILED`, `RETURNED`, or `DELIVERED` attempts.
- If a delivery attempt fails at the delivery point, the current `deliveries` record SHALL be closed with status `FAILED` and the Delivery Order SHALL move to `RETURNED` while goods remain tracked in virtual In-Transit inventory.
- If the goods are later returned to a warehouse, the same `deliveries` record MAY be marked `RETURNED` or linked to the separate return record created by the return flow.
- If the same Delivery Order is delivered again later, the system SHALL create a new `deliveries` record with the next `attempt_number`.

### Trip Completion Rules

- A trip SHALL move to `COMPLETED` only when every Delivery Order assigned to the trip has reached a terminal delivery outcome.
- Terminal delivery outcomes for Sprint 1 are `DELIVERED` and `RETURNED`.
- A trip with mixed successful and failed delivery orders MAY still be `COMPLETED` when all assigned orders have either been delivered or recorded as returned/failed for follow-up.

### Delivery Order Status Semantics

- `DELIVERED`: the dealer received goods and POD + OTP verification succeeded.
- `COMPLETED`: accounting has created the sales invoice for the delivered order and receivable has been recognized.
- `CLOSED`: the receivable/payment cycle for the order is settled or locked by accounting period closing rules.

### Authorization and Warehouse Scope Rules

- Every outbound API SHALL enforce both role permission and warehouse assignment.
- Planner, Thủ kho, QC, Trưởng kho, and Dispatcher users SHALL only create, view, approve, pick, QC, dispatch, or mutate Delivery Orders for warehouses assigned to their user account.
- Drivers SHALL only view and update trips and delivery attempts assigned to their driver profile.
- Accountant users SHALL only view and invoice delivered orders for warehouses assigned to their user account, unless their role is explicitly configured for company-wide accounting access.
- System Admin and CEO roles MAY have cross-warehouse visibility, but every mutation SHALL still write audit logs with actor, role, warehouse, entity, before state, and after state.

## 6. API Spec

_Vui lòng xem chi tiết API endpoints tại các tài liệu đặc tả tính năng:_

- [APIs - Delivery Order](features/feature-planner-delivery-order/feature-planner-delivery-order.md#4-api-endpoints)
- [APIs - Picking](features/feature-storekeeper-picking/feature-storekeeper-picking.md#4-api-endpoints)
- [APIs - Outbound QC](features/feature-qc-outbound-inspection/feature-qc-outbound-inspection.md#4-api-endpoints)
- [APIs - Trip Dispatch](features/feature-dispatcher-trip-dispatch/feature-dispatcher-trip-dispatch.md#4-api-endpoints)
- [APIs - Driver Mobile & POD](features/feature-driver-mobile-pod/feature-driver-mobile-pod.md#4-api-endpoints)
- [APIs - Billing Notification](features/feature-accountant-billing-notification/feature-accountant-billing-notification.md#4-api-endpoints)

## 7. Error Handling

| Error                      | HTTP | Condition                               |
| -------------------------- | ---- | --------------------------------------- |
| CREDIT_HOLD                | 422  | Dealer credit limit exceeded or overdue |
| INSUFFICIENT_STOCK         | 422  | available_qty < requested_qty           |
| VEHICLE_OVERLOAD           | 422  | Trip exceeds vehicle capacity           |
| DO_NOT_READY               | 400  | DO not in READY_TO_SHIP status          |
| MISSING_POD                | 400  | POD signature/image required            |
| DELIVERY_OTP_INVALID       | 400  | OTP is incorrect or not issued for this delivery order |
| DELIVERY_OTP_EXPIRED       | 400  | OTP has expired                         |
| INVENTORY_VERSION_CONFLICT | 409  | Concurrent inventory update             |
| WAREHOUSE_SCOPE_FORBIDDEN  | 403  | User role is valid but user is not assigned to the target warehouse, trip, or delivery attempt |

### Audit Trail

- Every outbound mutation SHALL create an audit log with `actor`, `action`, `entity_type`, `entity_id`, `entity_code`, `timestamp`, `before`, and `after`.
- `DELIVERY_ORDER_CREATE`: create DO, select FEFO/FIFO batch/location, and reserve inventory.
- `DELIVERY_ORDER_CANCEL`: cancel DO and release reserved inventory.
- `DELIVERY_ORDER_PICK_START`: move DO to `PICKING`.
- `DELIVERY_ORDER_PICK_COMPLETE`: mark picked items ready for outbound QC.
- `DELIVERY_ORDER_QC_CONFIRM`: record outbound QC result and package verification.
- `DELIVERY_ORDER_WAREHOUSE_APPROVE`: move DO to `READY_TO_SHIP`.
- `DELIVERY_ORDER_WAREHOUSE_REJECT`: store warehouse rejection reason and move DO back to `PICKING`.
- `TRIP_CREATE`: create trip, assign vehicle/driver, and store stop order.
- `TRIP_DEPART`: move trip and DOs to `IN_TRANSIT`, decrease `total_qty`, and release `reserved_qty`.
- `DELIVERY_ATTEMPT_CREATE`: create a new physical delivery attempt record for a dispatched Delivery Order.
- `OTP_REQUEST`: generate raw OTP, send it to the dealer/receiver email, and store only the hashed verifier in `delivery_otp_attempts` with expiry metadata.
- `OTP_CONFIRM`: verify OTP against the active `delivery_otp_attempts` record, mark the attempt consumed, store verification timestamp, and move DO to `DELIVERED`.
- `DELIVERY_FAIL`: store failure reason, close the current delivery attempt as `FAILED`, and move DO to `RETURNED`; returned goods remain tracked in virtual In-Transit inventory until a separate return flow receives and classifies them.
- `TRIP_COMPLETE`: mark trip `COMPLETED` only after all assigned DOs are `DELIVERED` or `RETURNED`.
- `BILLING_NOTIFICATION_CREATE`: notify accounting that a delivered DO is ready for invoicing.
- `INVOICE_CREATE_FROM_DO`: create invoice from a delivered DO and move the DO to `COMPLETED`.

## 8. Acceptance Criteria

_Vui lòng xem chi tiết kịch bản kiểm thử tại các tài liệu đặc tả tính năng:_

- [Acceptance - Delivery Order](features/feature-planner-delivery-order/feature-planner-delivery-order.md#5-acceptance-criteria)
- [Acceptance - Picking](features/feature-storekeeper-picking/feature-storekeeper-picking.md#5-acceptance-criteria)
- [Acceptance - Outbound QC](features/feature-qc-outbound-inspection/feature-qc-outbound-inspection.md#5-acceptance-criteria)
- [Acceptance - Trip Dispatch](features/feature-dispatcher-trip-dispatch/feature-dispatcher-trip-dispatch.md#5-acceptance-criteria)
- [Acceptance - Driver Mobile & POD](features/feature-driver-mobile-pod/feature-driver-mobile-pod.md#5-acceptance-criteria)
- [Acceptance - Billing Notification](features/feature-accountant-billing-notification/feature-accountant-billing-notification.md#5-acceptance-criteria)

## 9. Out of Scope

- Real-time GPS tracking of delivery vehicles
- Route optimization algorithm (manual stop ordering for Sprint 1)
- Electronic signature capture hardware integration
- Multi-leg / hub-and-spoke delivery model
- COD (Cash on Delivery) payment processing
