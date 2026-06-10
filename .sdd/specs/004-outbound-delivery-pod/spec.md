# Feature Specification: Xuất hàng & Giao hàng (Outbound & Delivery)

**Spec ID**: 004-outbound-delivery-pod
**Created**: 2026-05-30
**Status**: Draft
**Features**: US-WMS-06, US-WMS-07, US-WMS-08, US-WMS-09, US-WMS-10

---

## 1. Context and Goal

Xuất hàng là quy trình tạo doanh thu cho Phúc Anh. Planner nhận yêu cầu từ Công ty mẹ, kiểm tra credit + tồn kho, lập Đơn xuất. Thủ kho soạn hàng và kiểm QC Outbound, Dispatcher lập chuyến xe nội bộ, Tài xế giao hàng và xác nhận bằng OTP tại điểm giao. Kế toán lập hóa đơn.

### Features List
* [US-WMS-06: Lập Đơn xuất hàng & Tự động Kiểm tra Công nợ](./features/feature-planner-delivery-order.md)
* [Thủ kho Soạn hàng tại Kệ](./features/feature-storekeeper-picking.md)
* [US-WMS-07: Nhân viên QC Kiểm tra Đóng gói Outbound](./features/feature-qc-outbound-inspection.md)
* [US-WMS-08: Lập Chuyến xe & Vận chuyển Nội bộ](./features/feature-dispatcher-trip-dispatch.md)
* [US-WMS-09: Tài xế Xác nhận Giao hàng bằng OTP](./features/feature-driver-mobile-pod.md)
* [US-WMS-10: Kế toán Tiếp nhận Thông báo Lập Hóa đơn](./features/feature-accountant-billing-notification.md)

### Cross-Spec Mapping Notes
- US-WMS-10 trong spec này chỉ bao phủ notification sau khi Delivery Order chuyển sang `DELIVERED`. Nghiệp vụ tạo invoice, cộng công nợ, khóa/mở `CREDIT_HOLD`, và cảnh báo nợ quá hạn được đặc tả tại [008-finance-billing-closing](../008-finance-billing-closing/spec.md).
- US-WMS-09 sử dụng nền tảng xác thực JWT và RBAC theo kho/role từ [001-security-auth-rbac-audit](../001-security-auth-rbac-audit/spec.md); driver mobile endpoints không được bypass authentication.

## 2. Actors

| Actor | Vai trò | Nghiệp vụ liên quan |
|-------|---------|---------------------|
| Planner | Maker | Lập Đơn xuất hàng (Delivery Order), kiểm tra tồn kho khả dụng và trạng thái công nợ Đại lý |
| Thủ kho kiêm QC | Maker | Nhận đơn xuất, soạn hàng từ các vị trí kệ (Picking), kiểm QC Outbound và cập nhật trạng thái đơn |
| Nhân viên kho | Maker | Hỗ trợ bốc xếp, di chuyển hàng hóa theo chỉ dẫn của Thủ kho |
| Dispatcher | Maker | Lập Chuyến xe nội bộ, gán xe và tài xế rảnh, sắp xếp thứ tự giao hàng |
| Tài xế | Maker | Sử dụng smartphone xem chuyến xe, xác nhận nhận hàng (xe rời kho), xác nhận giao bằng OTP, báo cáo giao thất bại |
| Kế toán viên | Maker | Nhận thông báo đơn hàng Delivered, lập Hóa đơn bán hàng |
| Kế toán trưởng | Checker | Phê duyệt Credit Limit cho Đại lý |
| Trưởng kho | Checker | Ký duyệt xuất kho (giai đoạn Warehouse Approval) |

## 3. Functional Requirements (EARS)
*Vui lòng xem chi tiết yêu cầu chức năng EARS tại các tài liệu đặc tả tính năng:*
* [EARS - Delivery Order](./features/feature-planner-delivery-order.md#3-functional-requirements-ears)
* [EARS - Picking](./features/feature-storekeeper-picking.md#3-functional-requirements-ears)
* [EARS - Outbound QC](./features/feature-qc-outbound-inspection.md#3-functional-requirements-ears)
* [EARS - Trip Dispatch](./features/feature-dispatcher-trip-dispatch.md#3-functional-requirements-ears)
* [EARS - Driver Mobile & OTP](./features/feature-driver-mobile-pod.md#3-functional-requirements-ears)
* [EARS - Billing Notification](./features/feature-accountant-billing-notification.md#3-functional-requirements-ears)

## 4. Non-functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-001 | Credit check + reserve transaction | ≤ 1s |
| NFR-002 | OTP delivery confirmation | ≤ 5s for OTP request/verify |
| NFR-003 | Trip creation with 10+ DOs | ≤ 2s |
| NFR-004 | Concurrent order creation for same product | No oversell (optimistic locking) |

## 5. Data Model

### delivery_orders
- `id` (BIGSERIAL, PK)
- `do_number` (VARCHAR(50), UNIQUE, NOT NULL)
- `dealer_id` (BIGINT, FK→dealers, NOT NULL)
- `warehouse_id` (BIGINT, FK→warehouses, NOT NULL)
- `type` (VARCHAR(30), CHECK IN ('SALE','DELIVERY','ADJUSTMENT'), NOT NULL)
- `expected_delivery_date` (DATE)
- `status` (VARCHAR(30), DEFAULT 'NEW', CHECK IN ('NEW','PICKING','READY_TO_SHIP','IN_TRANSIT','DELIVERED','RETURNED','CANCELLED'))
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
- `batch_id` (BIGINT, FK→batches) -- set khi picking/FEFO/FIFO
- `location_id` (BIGINT, FK→warehouse_locations)
- `requested_qty` (DECIMAL(10,2), NOT NULL)
- `reserved_qty` (DECIMAL(10,2), DEFAULT 0)
- `issued_qty` (DECIMAL(10,2), DEFAULT 0)
- `unit_price` (DECIMAL(18,2)) -- Tra cứu từ price_history tại ngày giao
- `serial_number` (VARCHAR(100)) -- bắt buộc nếu product.has_serial = true

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
- `status` (VARCHAR(30), DEFAULT 'PENDING', CHECK IN ('PENDING','IN_TRANSIT','DELIVERED','RETURNED'))
- `otp_code_hash` (VARCHAR(255))
- `otp_requested_at` (TIMESTAMPTZ)
- `otp_expires_at` (TIMESTAMPTZ)
- `otp_verified_at` (TIMESTAMPTZ)
- `otp_attempt_count` (INTEGER, DEFAULT 0)
- `otp_recipient_phone` (VARCHAR(20))
- `failure_reason` (TEXT)
- `delivered_at` (TIMESTAMPTZ)
- `created_at` (TIMESTAMPTZ)
- `updated_at` (TIMESTAMPTZ)

### inventories (shared)
- `id` (BIGSERIAL, PK)
- `warehouse_id` (BIGINT, FK→warehouses, NOT NULL)
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

## 6. API Spec
*Vui lòng xem chi tiết API endpoints tại các tài liệu đặc tả tính năng:*
* [APIs - Delivery Order](./features/feature-planner-delivery-order.md#4-api-endpoints)
* [APIs - Picking](./features/feature-storekeeper-picking.md#4-api-endpoints)
* [APIs - Outbound QC](./features/feature-qc-outbound-inspection.md#4-api-endpoints)
* [APIs - Trip Dispatch](./features/feature-dispatcher-trip-dispatch.md#4-api-endpoints)
* [APIs - Driver Mobile & OTP](./features/feature-driver-mobile-pod.md#4-api-endpoints)
* [APIs - Billing Notification](./features/feature-accountant-billing-notification.md#4-api-endpoints)

## 7. Error Handling

| Error | HTTP | Condition |
|-------|------|-----------|
| CREDIT_HOLD | 422 | Dealer credit limit exceeded or overdue |
| INSUFFICIENT_STOCK | 422 | available_qty < requested_qty |
| VEHICLE_OVERLOAD | 422 | Trip exceeds vehicle capacity |
| DO_NOT_READY | 400 | DO not in READY_TO_SHIP status |
| OTP_REQUIRED | 400 | OTP verification required |
| OTP_INVALID | 422 | OTP code is incorrect |
| OTP_EXPIRED | 422 | OTP code expired |
| OTP_ATTEMPT_LIMIT_EXCEEDED | 429 | Too many OTP attempts |
| INVENTORY_VERSION_CONFLICT | 409 | Concurrent inventory update |

### Audit Trail
- Every outbound mutation SHALL create an audit log with `actor`, `action`, `entity_type`, `entity_id`, `entity_code`, `timestamp`, `before`, and `after`.
- `DELIVERY_ORDER_CREATE`: create DO, select FEFO/FIFO batch/location, and reserve inventory.
- `DELIVERY_ORDER_CANCEL`: cancel DO and release reserved inventory.
- `DELIVERY_ORDER_PICK_START`: move DO to `PICKING`.
- `DELIVERY_ORDER_PICK_COMPLETE`: mark picked items ready for outbound QC.
- `DELIVERY_ORDER_QC_CONFIRM`: record outbound QC result and package verification.
- `DELIVERY_ORDER_WAREHOUSE_APPROVE`: move DO to `READY_TO_SHIP`.
- `TRIP_CREATE`: create trip, assign vehicle/driver, and store stop order.
- `TRIP_DEPART`: move trip and DOs to `IN_TRANSIT`, decrease `total_qty`, and release `reserved_qty`.
- `OTP_REQUEST`: generate and send OTP to the dealer/receiver.
- `OTP_CONFIRM`: verify OTP, store verification timestamp, and move DO to `DELIVERED`.
- `DELIVERY_FAIL`: store failure reason, move DO to `RETURNED`, and create quarantine return receipt.
- `BILLING_NOTIFICATION_CREATE`: notify accounting that a delivered DO is ready for invoicing.

## 8. Acceptance Criteria
*Vui lòng xem chi tiết kịch bản kiểm thử tại các tài liệu đặc tả tính năng:*
* [Acceptance - Delivery Order](./features/feature-planner-delivery-order.md#5-acceptance-criteria)
* [Acceptance - Picking](./features/feature-storekeeper-picking.md#5-acceptance-criteria)
* [Acceptance - Outbound QC](./features/feature-qc-outbound-inspection.md#5-acceptance-criteria)
* [Acceptance - Trip Dispatch](./features/feature-dispatcher-trip-dispatch.md#5-acceptance-criteria)
* [Acceptance - Driver Mobile & OTP](./features/feature-driver-mobile-pod.md#5-acceptance-criteria)
* [Acceptance - Billing Notification](./features/feature-accountant-billing-notification.md#5-acceptance-criteria)

## 9. Out of Scope

- Real-time GPS tracking of delivery vehicles
- Route optimization algorithm (manual stop ordering for Sprint 1)
- Electronic signature capture hardware integration
- Multi-leg / hub-and-spoke delivery model
- COD (Cash on Delivery) payment processing
