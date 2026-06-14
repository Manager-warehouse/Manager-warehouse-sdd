# Feature Specification: Xuất hàng & Giao hàng (Outbound & Delivery)

**Spec ID**: 004-outbound-delivery-pod
**Created**: 2026-05-30
**Status**: Draft
**Features**: 6 feature files covering US-WMS-06, US-WMS-07, US-WMS-08, US-WMS-09, US-WMS-10

---

## 1. Context and Goal

Xuất hàng là quy trình tạo doanh thu cho Phúc Anh. Planner nhận yêu cầu từ Công ty mẹ, kiểm tra credit + tồn kho, lập Đơn xuất. Thủ kho soạn hàng và kiểm QC Outbound, Dispatcher lập chuyến xe nội bộ, Tài xế giao hàng, upload POD images và xác thực OTP email Đại lý. Kế toán lập hóa đơn.

### Features List
* [US-WMS-06: Lập Đơn xuất hàng & Tự động Kiểm tra Công nợ](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-planner-delivery-order/feature-planner-delivery-order.md)
* [Thủ kho Soạn hàng tại Bin](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-storekeeper-picking/feature-storekeeper-picking.md)
* [US-WMS-07: Nhân viên QC Kiểm tra Đóng gói Outbound](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-qc-outbound-inspection/feature-qc-outbound-inspection.md)
* [US-WMS-08: Lập Chuyến xe & Vận chuyển Nội bộ](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-dispatcher-trip-dispatch/feature-dispatcher-trip-dispatch.md)
* [US-WMS-09: Tài xế Xác nhận Giao hàng & Chữ ký Điện tử POD](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-driver-mobile-pod/feature-driver-mobile-pod.md)
* [US-WMS-10: Kế toán Tiếp nhận Thông báo Lập Hóa đơn](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-accountant-billing-notification/feature-accountant-billing-notification.md)

## 2. Actors

| Actor | Vai trò | Nghiệp vụ liên quan |
|-------|---------|---------------------|
| Planner | Maker | Lập Đơn xuất hàng (Delivery Order), kiểm tra tồn kho khả dụng và trạng thái công nợ Đại lý |
| Thủ kho kiêm QC | Maker | Nhận đơn xuất, soạn hàng từ Bin theo batch đã reserve (Picking), kiểm QC Outbound và cập nhật trạng thái đơn |
| Nhân viên kho | Maker | Hỗ trợ bốc xếp, di chuyển hàng hóa theo chỉ dẫn của Thủ kho |
| Dispatcher | Maker | Lập Chuyến xe nội bộ, gán xe và tài xế rảnh, sắp xếp thứ tự giao hàng |
| Tài xế | Maker | Sử dụng smartphone xem chuyến xe, xác nhận nhận hàng (xe rời kho), giao hàng, upload POD images, nhập OTP Đại lý đọc từ email, báo cáo giao thất bại |
| Kế toán viên | Maker | Nhận thông báo đơn hàng Delivered, lập Hóa đơn bán hàng |
| Kế toán trưởng | Checker | Phê duyệt Credit Limit cho Đại lý |
| Trưởng kho | Checker | Ký duyệt xuất kho (giai đoạn Warehouse Approval) |

## 3. Functional Requirements (EARS)
*Vui lòng xem chi tiết yêu cầu chức năng EARS tại các tài liệu đặc tả tính năng:*
* [EARS - Delivery Order](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-planner-delivery-order/feature-planner-delivery-order.md#3-functional-requirements-ears)
* [EARS - Picking](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-storekeeper-picking/feature-storekeeper-picking.md#3-functional-requirements-ears)
* [EARS - Outbound QC](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-qc-outbound-inspection/feature-qc-outbound-inspection.md#3-functional-requirements-ears)
* [EARS - Trip Dispatch](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-dispatcher-trip-dispatch/feature-dispatcher-trip-dispatch.md#3-functional-requirements-ears)
* [EARS - Driver Mobile & POD](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-driver-mobile-pod/feature-driver-mobile-pod.md#3-functional-requirements-ears)
* [EARS - Billing Notification](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-accountant-billing-notification/feature-accountant-billing-notification.md#3-functional-requirements-ears)

## 4. Non-functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-001 | Credit check + reserve transaction | ≤ 1s |
| NFR-002 | POD image upload | ≤ 5s for each image up to 5MB |
| NFR-003 | Trip creation with 10+ DOs | ≤ 2s |
| NFR-004 | Concurrent order creation for same product | No oversell (optimistic locking) |
| NFR-005 | Delivery OTP email delivery | ≤ 30s under normal mail service availability |

## 5. Data Model

### delivery_orders
- `id` (BIGSERIAL, PK)
- `do_number` (VARCHAR(50), UNIQUE, NOT NULL)
- `dealer_id` (BIGINT, FK→dealers, NOT NULL)
- `warehouse_id` (BIGINT, FK→warehouses, NOT NULL)
- `type` (VARCHAR(30), CHECK IN ('SALE','DELIVERY','ADJUSTMENT'), NOT NULL)
- `expected_delivery_date` (DATE)
- `status` (VARCHAR(40), DEFAULT 'NEW', CHECK IN ('NEW','PICKING','PENDING_WAREHOUSE_APPROVAL','READY_TO_SHIP','IN_TRANSIT','DELIVERED','RETURNED','COMPLETED','CLOSED','CANCELLED'))
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
- `pod_image_url` (VARCHAR(500)) -- ảnh hàng hóa bàn giao
- `pod_signature_url` (VARCHAR(500)) -- ảnh chữ ký/biên nhận của Đại lý xác nhận đã nhận hàng
- `pod_timestamp` (TIMESTAMPTZ)
- `otp_recipient_email` (VARCHAR(255))
- `otp_sent_at` (TIMESTAMPTZ)
- `otp_verified_at` (TIMESTAMPTZ)
- `failure_reason` (TEXT)
- `delivered_at` (TIMESTAMPTZ)
- `created_at` (TIMESTAMPTZ)
- `updated_at` (TIMESTAMPTZ)

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

## 6. API Spec
*Vui lòng xem chi tiết API endpoints tại các tài liệu đặc tả tính năng:*
* [APIs - Delivery Order](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-planner-delivery-order/feature-planner-delivery-order.md#4-api-endpoints)
* [APIs - Picking](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-storekeeper-picking/feature-storekeeper-picking.md#4-api-endpoints)
* [APIs - Outbound QC](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-qc-outbound-inspection/feature-qc-outbound-inspection.md#4-api-endpoints)
* [APIs - Trip Dispatch](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-dispatcher-trip-dispatch/feature-dispatcher-trip-dispatch.md#4-api-endpoints)
* [APIs - Driver Mobile & POD](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-driver-mobile-pod/feature-driver-mobile-pod.md#4-api-endpoints)
* [APIs - Billing Notification](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-accountant-billing-notification/feature-accountant-billing-notification.md#4-api-endpoints)

## 7. Error Handling

| Error | HTTP | Condition |
|-------|------|-----------|
| CREDIT_HOLD | 422 | Dealer credit limit exceeded or overdue |
| INSUFFICIENT_STOCK | 422 | available_qty < requested_qty |
| VEHICLE_OVERLOAD | 422 | Trip exceeds vehicle capacity |
| DO_NOT_READY | 400 | DO not in READY_TO_SHIP status |
| WAREHOUSE_APPROVAL_REQUIRED | 422 | DO passed outbound QC but has not been approved by Trưởng kho |
| MISSING_POD | 400 | POD goods image and signature/receipt image are required |
| DELIVERY_OTP_REQUIRED | 400 | Delivery success confirmation requires dealer OTP verification |
| DELIVERY_OTP_INVALID | 422 | Submitted OTP does not match the latest active delivery OTP |
| DELIVERY_OTP_EXPIRED | 422 | Delivery OTP expired before confirmation |
| INVENTORY_VERSION_CONFLICT | 409 | Concurrent inventory update |

## 8. Acceptance Criteria
*Vui lòng xem chi tiết kịch bản kiểm thử tại các tài liệu đặc tả tính năng:*
* [Acceptance - Delivery Order](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-planner-delivery-order/feature-planner-delivery-order.md#5-acceptance-criteria)
* [Acceptance - Picking](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-storekeeper-picking/feature-storekeeper-picking.md#5-acceptance-criteria)
* [Acceptance - Outbound QC](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-qc-outbound-inspection/feature-qc-outbound-inspection.md#5-acceptance-criteria)
* [Acceptance - Trip Dispatch](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-dispatcher-trip-dispatch/feature-dispatcher-trip-dispatch.md#5-acceptance-criteria)
* [Acceptance - Driver Mobile & POD](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-driver-mobile-pod/feature-driver-mobile-pod.md#5-acceptance-criteria)
* [Acceptance - Billing Notification](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-accountant-billing-notification/feature-accountant-billing-notification.md#5-acceptance-criteria)

## 9. Out of Scope

- Real-time GPS tracking of delivery vehicles
- Route optimization algorithm (manual stop ordering for Sprint 1)
- Electronic signature capture hardware integration
- Multi-leg / hub-and-spoke delivery model
- COD (Cash on Delivery) payment processing

