# Feature Specification: Điều chuyển Kho Nội bộ (Internal Transfer)

**Spec ID**: 005-inter-warehouse-transfer
**Created**: 2026-05-30
**Status**: Draft
**Features**: US-WMS-11, US-WMS-12

---

## 1. Context and Goal

Phúc Anh vận hành 3 kho vật lý (Hải Phòng, Hà Nội, Hồ Chí Minh). Hàng hóa cần được điều chuyển giữa các kho để cân bằng tồn kho, tránh đứt gãy nguồn cung. Hệ thống sử dụng kho ảo In-Transit để track hàng đang trên đường vận chuyển bằng xe nội bộ.

### Features List
* [US-WMS-11: Planning Dashboard & Gợi ý Điều chuyển kho tự động](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/005-inter-warehouse-transfer/features/feature-planner-transfer-planning.md)
* [Thủ kho Nguồn Soạn & Xuất hàng Điều chuyển](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/005-inter-warehouse-transfer/features/feature-storekeeper-transfer-ship.md)
* [US-WMS-12: Thủ kho Đích Tiếp nhận & Xử lý Chênh lệch Điều chuyển](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/005-inter-warehouse-transfer/features/feature-storekeeper-transfer-receive.md)

## 2. Actors

| Actor | Vai trò | Nghiệp vụ liên quan |
|-------|---------|---------------------|
| Planner | Maker | Xem Planning Dashboard, lập phiếu điều chuyển kho nội bộ |
| Trưởng kho (Kho nguồn) | Checker | Kiểm tra tồn kho khả dụng tại kho nguồn và phê duyệt phiếu điều chuyển |
| Thủ kho (Kho nguồn) | Maker | Nhận lệnh xuất điều chuyển, soạn hàng và xác nhận xuất hàng lên xe |
| Trưởng kho (Kho đích) | Checker | Tiếp nhận hàng điều chuyển đến, kiểm đếm số lượng thực tế, ghi nhận chênh lệch và xác nhận hoàn tất nhận hàng |
| Nhân viên kho | Maker | Bốc xếp hàng hóa lên xuống xe nội bộ tại cả hai đầu kho |
| Tài xế | Maker | Vận chuyển hàng hóa giữa các kho bằng xe nội bộ của Phúc Anh |

## 3. Functional Requirements (EARS)
*Vui lòng xem chi tiết yêu cầu chức năng EARS tại các tài liệu đặc tả tính năng:*
* [EARS - Transfer Suggestions](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/005-inter-warehouse-transfer/features/feature-planner-transfer-planning.md#3-functional-requirements-ears)
* [EARS - Transfer Shipment](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/005-inter-warehouse-transfer/features/feature-storekeeper-transfer-ship.md#3-functional-requirements-ears)
* [EARS - Transfer Receipt](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/005-inter-warehouse-transfer/features/feature-storekeeper-transfer-receive.md#3-functional-requirements-ears)

## 4. Non-functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-001 | Transfer creation + inventory update | ≤ 2s |
| NFR-002 | In-Transit inventory query | Real-time (≤ 500ms) |
| NFR-003 | Discrepancy adjustment record | Must be immutable after creation |

## 5. Data Model

### transfers
- `id` (BIGSERIAL, PK)
- `transfer_number` (VARCHAR(50), UNIQUE, NOT NULL)
- `source_warehouse_id` (BIGINT, FK→warehouses, NOT NULL)
- `destination_warehouse_id` (BIGINT, FK→warehouses, NOT NULL)
- `status` (VARCHAR(40), DEFAULT 'NEW', CHECK IN ('NEW','APPROVED','IN_TRANSIT','COMPLETED','COMPLETED_WITH_DISCREPANCY','CANCELLED'))
- `created_by` (BIGINT, FK→users, NOT NULL)
- `approved_by` (BIGINT, FK→users)
- `approved_at` (TIMESTAMPTZ)
- `confirmed_by` (BIGINT, FK→users) -- người nhận hàng tại kho đích
- `confirmed_at` (TIMESTAMPTZ)
- `planned_date` (DATE)
- `actual_received_date` (DATE)
- `discrepancy_reason` (TEXT)
- `document_date` (DATE, NOT NULL)
- `accounting_period_id` (BIGINT, FK→accounting_periods)
- `created_at` (TIMESTAMPTZ)
- `updated_at` (TIMESTAMPTZ)

### transfer_items
- `id` (BIGSERIAL, PK)
- `transfer_id` (BIGINT, FK→transfers, NOT NULL)
- `product_id` (BIGINT, FK→products, NOT NULL)
- `batch_id` (BIGINT, FK→batches, NOT NULL)
- `source_location_id` (BIGINT, FK→warehouse_locations)
- `destination_location_id` (BIGINT, FK→warehouse_locations)
- `planned_qty` (DECIMAL(10,2), NOT NULL)
- `sent_qty` (DECIMAL(10,2))
- `received_qty` (DECIMAL(10,2))
- `variance_qty` (DECIMAL(10,2)) -- received_qty - sent_qty (âm = thiếu)

### inventories (shared)
- Tồn kho tại kho ảo In-Transit sử dụng cùng thực thể với `warehouse_id` liên kết đến kho In-Transit (type = 'IN_TRANSIT').

## 6. API Spec
*Vui lòng xem chi tiết API endpoints tại các tài liệu đặc tả tính năng:*
* [APIs - Transfer Planning](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/005-inter-warehouse-transfer/features/feature-planner-transfer-planning.md#4-api-endpoints)
* [APIs - Transfer Shipment](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/005-inter-warehouse-transfer/features/feature-storekeeper-transfer-ship.md#4-api-endpoints)
* [APIs - Transfer Receipt](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/005-inter-warehouse-transfer/features/feature-storekeeper-transfer-receive.md#4-api-endpoints)

## 7. Error Handling

| Error | HTTP | Condition |
|-------|------|-----------|
| SAME_WAREHOUSE | 422 | source = dest |
| INSUFFICIENT_TRANSFER_STOCK | 422 | Source warehouse lacks available qty |
| TRANSFER_ALREADY_APPROVED | 409 | Duplicate approval |
| DISCREPANCY_REQUIRES_REASON | 400 | Quantity mismatch without reason |
| INVENTORY_VERSION_CONFLICT | 409 | Concurrent inventory update |

## 8. Acceptance Criteria
*Vui lòng xem chi tiết kịch bản kiểm thử tại các tài liệu đặc tả tính năng:*
* [Acceptance - Transfer Planning](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/005-inter-warehouse-transfer/features/feature-planner-transfer-planning.md#5-acceptance-criteria)
* [Acceptance - Transfer Shipment](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/005-inter-warehouse-transfer/features/feature-storekeeper-transfer-ship.md#5-acceptance-criteria)
* [Acceptance - Transfer Receipt](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/005-inter-warehouse-transfer/features/feature-storekeeper-transfer-receive.md#5-acceptance-criteria)

## 9. Out of Scope

- Automated replenishment algorithms (batch job suggestion only)
- Multi-warehouse transfer optimization
- Transfer cost tracking
- Third-party logistics (3PL) — internal fleet only
