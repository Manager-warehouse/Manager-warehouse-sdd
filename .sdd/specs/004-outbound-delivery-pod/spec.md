# Feature Specification: Xuất hàng & Giao hàng (Outbound & Delivery)

**Spec ID**: 004-outbound-delivery-pod
**Created**: 2026-05-30
**Status**: Draft
**Features**: US-WMS-06, US-WMS-07, US-WMS-08, US-WMS-09, US-WMS-10

---

## 1. Context and Goal

Xuất hàng là quy trình tạo doanh thu cho Phúc Anh. Planner nhận yêu cầu từ Công ty mẹ, kiểm tra credit + tồn kho, lập Đơn xuất. Thủ kho soạn hàng, QC Outbound kiểm tra đóng gói, Dispatcher lập chuyến xe nội bộ, Tài xế giao hàng và ký POD. Kế toán lập hóa đơn.

### Features List
* [US-WMS-06: Lập Đơn xuất hàng & Tự động Kiểm tra Công nợ](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-planner-delivery-order.md)
* [Thủ kho Soạn hàng tại Kệ](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-storekeeper-picking.md)
* [US-WMS-07: Nhân viên QC Kiểm tra Đóng gói Outbound](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-qc-outbound-inspection.md)
* [US-WMS-08: Lập Chuyến xe & Vận chuyển Nội bộ](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-dispatcher-trip-dispatch.md)
* [US-WMS-09: Tài xế Xác nhận Giao hàng & Chữ ký Điện tử POD](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-driver-mobile-pod.md)
* [US-WMS-10: Kế toán Tiếp nhận Thông báo Lập Hóa đơn](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-accountant-billing-notification.md)

## 2. Actors

| Actor | Vai trò | Nghiệp vụ liên quan |
|-------|---------|---------------------|
| Planner | Maker | Lập Đơn xuất hàng (Delivery Order), kiểm tra tồn kho khả dụng và trạng thái công nợ Đại lý |
| Thủ kho | Maker | Nhận đơn xuất, soạn hàng từ các vị trí kệ (Picking), cập nhật trạng thái đơn |
| Nhân viên kho | Maker | QC Outbound, kiểm tra đóng gói (đúng SKU, số lượng, đóng thùng chống sốc), hỗ trợ bốc xếp |
| Dispatcher | Maker | Lập Chuyến xe nội bộ, gán xe và tài xế rảnh, sắp xếp thứ tự giao hàng |
| Tài xế | Maker | Sử dụng smartphone xem chuyến xe, xác nhận nhận hàng (xe rời kho), giao hàng và ký nhận POD, báo cáo giao thất bại |
| Kế toán viên | Maker | Nhận thông báo đơn hàng Delivered, lập Hóa đơn bán hàng |
| Kế toán trưởng | Checker | Phê duyệt Credit Limit cho Đại lý |
| Trưởng kho kiêm Trưởng QC | Checker | Ký duyệt xuất kho (giai đoạn Warehouse Approval) |

## 3. Functional Requirements (EARS)
*Vui lòng xem chi tiết yêu cầu chức năng EARS tại các tài liệu đặc tả tính năng:*
* [EARS - Delivery Order](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-planner-delivery-order.md#3-functional-requirements-ears)
* [EARS - Picking](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-storekeeper-picking.md#3-functional-requirements-ears)
* [EARS - Outbound QC](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-qc-outbound-inspection.md#3-functional-requirements-ears)
* [EARS - Trip Dispatch](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-dispatcher-trip-dispatch.md#3-functional-requirements-ears)
* [EARS - Driver Mobile & POD](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-driver-mobile-pod.md#3-functional-requirements-ears)
* [EARS - Billing Notification](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-accountant-billing-notification.md#3-functional-requirements-ears)

## 4. Non-functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-001 | Credit check + reserve transaction | ≤ 1s |
| NFR-002 | POD image upload | ≤ 5s for 5MB image |
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
- `status` (VARCHAR(30), DEFAULT 'NEW', CHECK IN ('NEW','PICKING','READY_TO_SHIP','IN_TRANSIT','OUT_FOR_DELIVERY','DELIVERED','COMPLETED','CLOSED','CANCELLED'))
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
- `status` (VARCHAR(30), DEFAULT 'PENDING', CHECK IN ('PENDING','IN_TRANSIT','OUT_FOR_DELIVERY','DELIVERED','RETURNED'))
- `pod_image_url` (VARCHAR(500))
- `pod_signature_url` (VARCHAR(500))
- `pod_timestamp` (TIMESTAMPTZ)
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
* [APIs - Delivery Order](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-planner-delivery-order.md#4-api-endpoints)
* [APIs - Picking](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-storekeeper-picking.md#4-api-endpoints)
* [APIs - Outbound QC](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-qc-outbound-inspection.md#4-api-endpoints)
* [APIs - Trip Dispatch](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-dispatcher-trip-dispatch.md#4-api-endpoints)
* [APIs - Driver Mobile & POD](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-driver-mobile-pod.md#4-api-endpoints)
* [APIs - Billing Notification](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-accountant-billing-notification.md#4-api-endpoints)

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
*Vui lòng xem chi tiết kịch bản kiểm thử tại các tài liệu đặc tả tính năng:*
* [Acceptance - Delivery Order](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-planner-delivery-order.md#5-acceptance-criteria)
* [Acceptance - Picking](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-storekeeper-picking.md#5-acceptance-criteria)
* [Acceptance - Outbound QC](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-qc-outbound-inspection.md#5-acceptance-criteria)
* [Acceptance - Trip Dispatch](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-dispatcher-trip-dispatch.md#5-acceptance-criteria)
* [Acceptance - Driver Mobile & POD](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-driver-mobile-pod.md#5-acceptance-criteria)
* [Acceptance - Billing Notification](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-accountant-billing-notification.md#5-acceptance-criteria)

## 9. Out of Scope

- Real-time GPS tracking of delivery vehicles
- Route optimization algorithm (manual stop ordering for Sprint 1)
- Electronic signature capture hardware integration
- Multi-leg / hub-and-spoke delivery model
- COD (Cash on Delivery) payment processing
