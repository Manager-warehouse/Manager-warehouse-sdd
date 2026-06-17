# Feature Specification: Điều chuyển Kho Nội bộ (Internal Transfer)

**Spec ID**: 005-inter-warehouse-transfer
**Created**: 2026-05-30
**Status**: Draft
**Features**: US-WMS-11, US-WMS-12

---

## 1. Context and Goal

Phúc Anh vận hành 3 kho vật lý (Hải Phòng, Hà Nội, Hồ Chí Minh). Hàng hóa cần được điều chuyển giữa các kho để cân bằng tồn kho, tránh đứt gãy nguồn cung. Hệ thống sử dụng kho ảo In-Transit để track hàng đang trên đường vận chuyển bằng xe nội bộ. Mỗi phiếu điều chuyển luôn gắn với đúng một chuyến xe nội bộ riêng, gồm xe, tài xế và lịch vận chuyển.

### Features List
* [US-WMS-11: Planner Nhập Lệnh Điều chuyển kho từ Công ty mẹ](./features/feature-planner-transfer-planning.md)
* [Thủ kho Nguồn Soạn & Xuất hàng Điều chuyển](./features/feature-storekeeper-transfer-ship.md)
* [US-WMS-12: Thủ kho Đích Tiếp nhận & Xử lý Chênh lệch Điều chuyển](./features/feature-storekeeper-transfer-receive.md)

## 2. Actors

| Actor | Vai trò | Nghiệp vụ liên quan |
|-------|---------|---------------------|
| Planner | Maker | Nhập phiếu điều chuyển kho nội bộ theo lệnh từ Công ty mẹ hoặc bộ phận điều phối trung tâm |
| Trưởng kho (Kho nguồn) | Checker | Kiểm tra tồn kho khả dụng tại kho nguồn và phê duyệt phiếu điều chuyển |
| Dispatcher | Maker | Lập chuyến xe nội bộ riêng cho phiếu điều chuyển, gán xe và tài xế khả dụng |
| Thủ kho (Kho nguồn) | Maker | Nhận lệnh xuất điều chuyển, soạn hàng và xác nhận xuất hàng lên xe |
| Trưởng kho (Kho đích) | Checker | Tiếp nhận hàng điều chuyển đến, kiểm đếm số lượng thực tế, ghi nhận chênh lệch và xác nhận hoàn tất nhận hàng |
| Nhân viên kho | Maker | Bốc xếp hàng hóa lên xuống xe nội bộ tại cả hai đầu kho |
| Tài xế | Maker | Xác nhận đã nhận hàng, xe rời kho nguồn và vận chuyển hàng hóa giữa các kho bằng xe nội bộ của Phúc Anh |

## 3. Functional Requirements (EARS)
*Vui lòng xem chi tiết yêu cầu chức năng EARS tại các tài liệu đặc tả tính năng:*
* [EARS - Transfer Creation](./features/feature-planner-transfer-planning.md#3-functional-requirements-ears)
* [EARS - Transfer Shipment](./features/feature-storekeeper-transfer-ship.md#3-functional-requirements-ears)
* [EARS - Transfer Receipt](./features/feature-storekeeper-transfer-receive.md#3-functional-requirements-ears)

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
- `status` (VARCHAR(40), DEFAULT 'NEW', CHECK IN ('NEW','APPROVED','REJECTED','IN_TRANSIT','COMPLETED','COMPLETED_WITH_DISCREPANCY','CANCELLED'))
- `created_by` (BIGINT, FK→users, NOT NULL)
- `external_instruction_code` (VARCHAR(80)) -- mã lệnh điều chuyển từ Công ty mẹ/bộ phận điều phối nếu có
- `approved_by` (BIGINT, FK→users)
- `approved_at` (TIMESTAMPTZ)
- `rejected_by` (BIGINT, FK→users)
- `rejected_at` (TIMESTAMPTZ)
- `rejection_reason` (TEXT)
- `confirmed_by` (BIGINT, FK→users) -- người nhận hàng tại kho đích
- `confirmed_at` (TIMESTAMPTZ)
- `planned_date` (DATE)
- `actual_received_date` (DATE)
- `discrepancy_reason` (TEXT)
- `trip_id` (BIGINT, FK→trips, UNIQUE) -- mỗi phiếu điều chuyển gắn đúng một chuyến xe nội bộ
- `document_date` (DATE, NOT NULL)
- `accounting_period_id` (BIGINT, FK→accounting_periods)
- `notes` (TEXT)
- `created_at` (TIMESTAMPTZ)
- `updated_at` (TIMESTAMPTZ)

### transfer_items
- `id` (BIGSERIAL, PK)
- `transfer_id` (BIGINT, FK→transfers, NOT NULL)
- `product_id` (BIGINT, FK→products, NOT NULL)
- `batch_id` (BIGINT, FK→batches) -- nullable while transfer status is NEW; resolved later by FIFO during approval/shipment
- `source_location_id` (BIGINT, FK→warehouse_locations)
- `destination_location_id` (BIGINT, FK→warehouse_locations)
- `planned_qty` (DECIMAL(10,2), NOT NULL)
- `sent_qty` (DECIMAL(10,2))
- `received_qty` (DECIMAL(10,2))
- `variance_qty` (DECIMAL(10,2)) -- received_qty - sent_qty (âm = thiếu)
- `qc_passed_qty` (DECIMAL(10,2))
- `qc_failed_qty` (DECIMAL(10,2))
- `qc_result` (VARCHAR(20), CHECK IN ('PENDING','PASSED','FAILED','PARTIAL'))
- `qc_failure_reason` (TEXT)

### trips (transfer usage)
- Transfer trips reuse the shared `trips` entity with `trip_type = 'TRANSFER'`.
- Each transfer SHALL have exactly one transfer trip before shipment can move to `IN_TRANSIT`.
- A transfer trip SHALL be linked to exactly one transfer; multi-transfer trips are out of scope for Sprint 1.

### inventories (shared)
- Tồn kho tại kho ảo In-Transit sử dụng cùng thực thể với `warehouse_id` liên kết đến kho In-Transit (type = 'IN_TRANSIT').

## 6. API Spec
*Vui lòng xem chi tiết API endpoints tại các tài liệu đặc tả tính năng:*
* [APIs - Transfer Creation](./features/feature-planner-transfer-planning.md#4-api-endpoints)
* [APIs - Transfer Shipment](./features/feature-storekeeper-transfer-ship.md#4-api-endpoints)
* [APIs - Transfer Receipt](./features/feature-storekeeper-transfer-receive.md#4-api-endpoints)

## 7. Error Handling

| Error | HTTP | Condition |
|-------|------|-----------|
| SAME_WAREHOUSE | 422 | source = dest |
| TRANSFER_ITEMS_REQUIRED | 400 | Planner submits transfer without item lines |
| INVALID_TRANSFER_QTY | 400 | planned_qty <= 0 |
| PRODUCT_INACTIVE | 422 | Product is inactive or unavailable for transfer |
| WAREHOUSE_INACTIVE | 422 | Source or destination warehouse is inactive |
| ACCOUNTING_PERIOD_CLOSED | 409 | document_date belongs to a closed accounting period |
| TRANSFER_UPDATE_NOT_ALLOWED | 409 | Planner attempts to edit a transfer after status is no longer NEW |
| INSUFFICIENT_TRANSFER_STOCK | 422 | Source warehouse lacks available qty |
| TRANSFER_ALREADY_APPROVED | 409 | Duplicate approval |
| REJECTION_REASON_REQUIRED | 400 | Trưởng kho nguồn rejects transfer without reason |
| TRANSFER_TRIP_REQUIRED | 400 | Shipment attempted before assigning vehicle and driver |
| RECEIVED_QTY_EXCEEDS_SENT | 422 | received_qty > sent_qty |
| DISCREPANCY_REQUIRES_REASON | 400 | Quantity mismatch without reason |
| TRANSFER_CANCEL_NOT_ALLOWED | 409 | Actor or current status is not allowed to cancel the transfer |
| INVENTORY_VERSION_CONFLICT | 409 | Concurrent inventory update |

### Transfer Business Rules
- Sprint 1 SHALL NOT generate transfer suggestions or automatically decide source/destination/quantity for inter-warehouse transfers.
- Planner SHALL create transfers only from explicit external transfer instructions from Công ty mẹ or a central coordination team.
- Công ty mẹ SHALL NOT be modeled as a WMS user in Sprint 1; Planner is the system actor who enters the external instruction.
- Sprint 1 SHALL NOT require uploading attachment files for external transfer instructions.
- Each transfer MAY contain multiple item lines.
- Planner SHALL NOT choose batch during transfer creation; batch allocation SHALL be resolved by FIFO during the approval/shipment flow.
- Planner MAY edit transfer header fields and add/update/remove transfer item lines only while status is `NEW`.
- Transfers SHALL NOT be editable after status changes to `APPROVED`, `REJECTED`, `IN_TRANSIT`, `COMPLETED`, `COMPLETED_WITH_DISCREPANCY`, or `CANCELLED`.
- Trưởng kho nguồn approval SHALL reserve the planned quantity immediately to prevent competing outbound or transfer operations from consuming the same stock.
- Trưởng kho nguồn MAY reject a `NEW` transfer; rejection SHALL require `rejection_reason` and set status to `REJECTED`.
- A `REJECTED` transfer SHALL be immutable and SHALL NOT be resubmitted; Planner MUST create a new transfer if the external instruction still needs execution.
- Planner MAY cancel only `NEW` transfers.
- Trưởng kho nguồn or an authorized manager MAY cancel `APPROVED` transfers; cancellation SHALL release reserved quantity.
- Each transfer SHALL use a dedicated internal-fleet trip (`trip_type = 'TRANSFER'`) with one vehicle and one driver; one transfer cannot share a trip with another transfer.
- Shipment to `IN_TRANSIT` SHALL occur only after the assigned driver confirms received goods and vehicle departure.
- Destination receiving SHALL be split by responsibility: Thủ kho đích records actual counts and QC results; Trưởng kho đích performs the final confirmation.
- Destination QC SHALL check both received quantity and product quality. QC-failed quantities SHALL be moved to quarantine inventory and excluded from available inventory.
- `received_qty > sent_qty` SHALL be blocked; the system SHALL NOT create positive transfer discrepancy adjustments for over-receipt.
- If `received_qty < sent_qty`, the system SHALL require `discrepancy_reason`, create a `TRANSFER_DISCREPANCY` adjustment for the shortage, and set status to `COMPLETED_WITH_DISCREPANCY`.
- Transfer cancellation SHALL be rejected for `REJECTED`, `IN_TRANSIT`, `COMPLETED`, `COMPLETED_WITH_DISCREPANCY`, or `CANCELLED` transfers.

### Audit Trail
- Every transfer mutation SHALL create an audit log with `actor`, `action`, `entity_type`, `entity_id`, `entity_code`, `timestamp`, `before`, and `after`.
- `TRANSFER_CREATE`: Planner creates transfer with status `NEW`.
- `TRANSFER_UPDATE`: Planner edits header or item lines while transfer status is `NEW`.
- `TRANSFER_APPROVE`: Trưởng kho nguồn approves transfer, reserves source inventory, and changes status to `APPROVED`.
- `TRANSFER_REJECT`: Trưởng kho nguồn rejects a `NEW` transfer with a required reason and changes status to `REJECTED`.
- `TRANSFER_TRIP_ASSIGN`: Dispatcher assigns dedicated vehicle and driver trip for the transfer.
- `TRANSFER_SHIP`: Thủ kho nguồn records sent quantities and loading details.
- `TRANSFER_DEPART`: Driver confirms goods received and vehicle departure; system moves inventory from source to In-Transit and changes status to `IN_TRANSIT`.
- `TRANSFER_RECEIVE_COUNT`: Thủ kho đích records received quantities and QC result.
- `TRANSFER_RECEIVE_CONFIRM`: Trưởng kho đích confirms receipt, moves passed quantity to destination inventory, failed quantity to quarantine inventory, clears In-Transit, and completes the transfer.
- `TRANSFER_DISCREPANCY_CREATE`: System creates shortage adjustment when received quantity is lower than sent quantity.
- `TRANSFER_CANCEL`: Planner cancels a `NEW` transfer without inventory changes, or Trưởng kho nguồn/manager cancels an `APPROVED` transfer and releases reserved quantity.

## 8. Acceptance Criteria
*Vui lòng xem chi tiết kịch bản kiểm thử tại các tài liệu đặc tả tính năng:*
* [Acceptance - Transfer Creation](./features/feature-planner-transfer-planning.md#6-acceptance-criteria)
* [Acceptance - Transfer Shipment](./features/feature-storekeeper-transfer-ship.md#5-acceptance-criteria)
* [Acceptance - Transfer Receipt](./features/feature-storekeeper-transfer-receive.md#5-acceptance-criteria)

## 9. Out of Scope

- Automated replenishment suggestions and transfer decision algorithms
- Multi-warehouse transfer optimization
- Transfer cost tracking
- Third-party logistics (3PL) — internal fleet only
