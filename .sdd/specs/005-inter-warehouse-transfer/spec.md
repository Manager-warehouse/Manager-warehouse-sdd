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
* [US-WMS-11: Planner Nhập Lệnh Điều chuyển kho từ Công ty mẹ](./features/feature-planner-transfer-planning.md)
* [Thủ kho Nguồn Soạn & Xuất hàng Điều chuyển](./features/feature-storekeeper-transfer-ship.md)
* [US-WMS-12: Kho Đích Tiếp nhận & Xử lý Chênh lệch Điều chuyển](./features/feature-storekeeper-transfer-receive.md)

## Clarifications

### Session 2026-06-16
- Q: Does Sprint 1 include automatic transfer suggestions? → A: No; Planner manually enters transfers from Công ty mẹ/central coordination instructions.
- Q: Can transfers be edited or cancelled after creation? → A: Planner may edit/cancel only `NEW`; source warehouse manager/authorized manager may cancel only unshipped `APPROVED` transfers and must release reservations; if goods were already loaded, unship/unload is required before cancellation; no cancellation is allowed from `IN_TRANSIT` onward.
- Q: Is `external_instruction_code` optional? → A: No; every transfer must include a non-blank external instruction code for traceability.
- Q: How should duplicate external instructions be handled? → A: Reject duplicate active transfers with the same external instruction code, source warehouse, destination warehouse, and document date; rejected/cancelled transfers do not block corrected re-entry.
- Q: Who performs destination receiving steps? → A: Worker records initial quantity; destination storekeeper checks quantity, records QC, selects destination location and approves receive check; destination warehouse manager confirms final receipt.
- Q: How are destination locations and receiving reasons handled? → A: Storekeeper selects destination location for QC-passed stock; QC-failed stock uses the active destination quarantine location; shortage, QC failure, or any material issue requires a reason.
- Q: How do transfer edit and receive correction work? → A: Planner edits a loaded existing transfer list and saves the current state; worker shortage/over-count requires an issue reason; destination storekeeper may correct worker count but must add a checker note when values differ.

## 2. Actors

| Actor | Vai trò | Nghiệp vụ liên quan |
|-------|---------|---------------------|
| Planner | Maker | Nhập phiếu điều chuyển kho nội bộ theo lệnh từ Công ty mẹ hoặc bộ phận điều phối trung tâm |
| Planner | Maker | Nhập phiếu điều chuyển kho nội bộ theo lệnh từ Công ty mẹ hoặc bộ phận điều phối trung tâm |
| Trưởng kho (Kho nguồn) | Checker | Kiểm tra tồn kho khả dụng tại kho nguồn và phê duyệt phiếu điều chuyển |
| Dispatcher | Maker | Lập chuyến xe nội bộ riêng cho phiếu điều chuyển, gán xe và tài xế khả dụng |
| Thủ kho (Kho nguồn) | Maker | Nhận lệnh xuất điều chuyển, soạn hàng và xác nhận xuất hàng lên xe |
| Nhân viên kho/Công nhân kho đích | Maker | Kiểm tra mặt số lượng khi xe điều chuyển đến và nhập số lượng thực nhận ban đầu |
| Thủ kho (Kho đích) | Checker | Kiểm tra lại số lượng công nhân nhập, nhập/chốt QC, chọn vị trí nhập kho cho hàng đạt và duyệt kết quả nhận |
| Trưởng kho (Kho đích) | Checker | Xác nhận cuối cùng, xử lý chênh lệch hoặc vấn đề phát sinh và duyệt hoàn tất nhập kho |
| Nhân viên kho | Maker | Bốc xếp hàng hóa lên xuống xe nội bộ tại cả hai đầu kho |
| Tài xế | Maker | Xác nhận đã nhận hàng, xe rời kho nguồn và vận chuyển hàng hóa giữa các kho bằng xe nội bộ của Phúc Anh |

## 3. Functional Requirements (EARS)
*Vui lòng xem chi tiết yêu cầu chức năng EARS tại các tài liệu đặc tả tính năng:*
* [EARS - Transfer Creation](./features/feature-planner-transfer-planning.md#3-functional-requirements-ears)
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
- `status` (VARCHAR(40), DEFAULT 'NEW', CHECK IN ('NEW','APPROVED','REJECTED','IN_TRANSIT','COMPLETED','COMPLETED_WITH_DISCREPANCY','CANCELLED'))
- `created_by` (BIGINT, FK→users, NOT NULL)
- `external_instruction_code` (VARCHAR(80), NOT NULL) -- mã lệnh điều chuyển từ Công ty mẹ/bộ phận điều phối, bắt buộc để truy vết
- `approved_by` (BIGINT, FK→users)
- `approved_at` (TIMESTAMPTZ)
- `rejected_by` (BIGINT, FK→users)
- `rejected_at` (TIMESTAMPTZ)
- `rejection_reason` (TEXT)
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
- `notes` (TEXT)
- `created_at` (TIMESTAMPTZ)
- `updated_at` (TIMESTAMPTZ)

### transfer_items
- `id` (BIGSERIAL, PK)
- `transfer_id` (BIGINT, FK→transfers, NOT NULL)
- `product_id` (BIGINT, FK→products, NOT NULL)
- `source_location_id` (BIGINT, FK→warehouse_locations)
- `destination_location_id` (BIGINT, FK→warehouse_locations)
- `planned_qty` (DECIMAL(10,2), NOT NULL)
- `sent_qty` (DECIMAL(10,2))
- `received_qty` (DECIMAL(10,2)) -- worker-entered count before receive-check approval; storekeeper-confirmed effective count after receive-check approval
- `variance_qty` (DECIMAL(10,2)) -- received_qty - sent_qty (âm = thiếu)
- `qc_passed_qty` (DECIMAL(10,2))
- `qc_failed_qty` (DECIMAL(10,2))
- `qc_result` (VARCHAR(20), CHECK IN ('PENDING','PASSED','FAILED','PARTIAL'))
- `qc_failure_reason` (TEXT)
- `receive_issue_reason` (TEXT) -- lý do công nhân báo thiếu hoặc vấn đề khi nhập số lượng ban đầu
- `receive_checked_by` (BIGINT, FK→users) -- Thủ kho đích duyệt lại số lượng/QC
- `receive_checked_at` (TIMESTAMPTZ)
- `receive_checker_note` (TEXT)

### trips (transfer usage)
- Transfer trips reuse the shared `trips` entity with `trip_type = 'TRANSFER'`.
- Each transfer SHALL have exactly one transfer trip before shipment can move to `IN_TRANSIT`.
- A transfer trip SHALL be linked to exactly one transfer; multi-transfer trips are out of scope for Sprint 1.

### inventories (shared)
- Tồn kho tại kho ảo In-Transit sử dụng cùng thực thể với `warehouse_id` liên kết đến kho In-Transit (type = 'IN_TRANSIT').

## 6. API Spec
*Vui lòng xem chi tiết API endpoints tại các tài liệu đặc tả tính năng:*
* [APIs - Transfer Creation](./features/feature-planner-transfer-planning.md#4-api-endpoints)
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
| EXTERNAL_INSTRUCTION_CODE_REQUIRED | 400 | external_instruction_code is blank or missing |
| DUPLICATE_EXTERNAL_INSTRUCTION | 409 | Another active transfer already uses the same external_instruction_code, source warehouse, destination warehouse, and document_date |
| INSUFFICIENT_TRANSFER_STOCK | 422 | Source warehouse lacks available qty |
| TRANSFER_ALREADY_APPROVED | 409 | Duplicate approval |
| REJECTION_REASON_REQUIRED | 400 | Trưởng kho nguồn rejects transfer without reason |
| TRANSFER_TRIP_REQUIRED | 400 | Departure attempted before assigning exactly one transfer trip |
| TRANSFER_TRIP_NOT_AVAILABLE | 409 | Selected vehicle or driver is unavailable or already assigned to an overlapping trip |
| TRANSFER_SHIP_NOT_ALLOWED | 409 | Shipment attempted in invalid state, by actor outside source scope, or after shipment already recorded |
| SENT_QTY_MISMATCH | 400 | sent_qty differs from approved planned_qty |
| TRANSFER_UNSHIP_NOT_ALLOWED | 409 | Unship attempted before shipment, after departure, or by unauthorized actor |
| TRANSFER_DEPART_NOT_ALLOWED | 409 | Departure attempted without APPROVED status, assigned trip, sent quantities, or assigned driver |
| TRANSFER_RECEIVE_NOT_ALLOWED | 409 | Actor attempts receive-count or receive-check in an invalid status/step |
| RECEIVE_ISSUE_REASON_REQUIRED | 400 | An item has received_qty different from sent_qty or a reported issue without item-level receive issue reason |
| RECEIVED_QTY_EXCEEDS_SENT | 422 | received_qty > sent_qty |
| RECEIVE_CHECK_REQUIRED | 409 | Trưởng kho đích confirms receipt before Thủ kho đích approves receive check |
| CHECKER_NOTE_REQUIRED | 400 | Confirmed received quantity differs from worker-entered received quantity without checker note; checker note is optional when quantities match |
| QC_TOTAL_MISMATCH | 400 | qc_passed_qty + qc_failed_qty != confirmed received quantity |
| QC_FAILURE_REASON_REQUIRED | 400 | qc_failed_qty > 0 without qc_failure_reason |
| DESTINATION_LOCATION_REQUIRED | 400 | qc_passed_qty > 0 without destination_location_id |
| QUARANTINE_LOCATION_REQUIRED | 422 | qc_failed_qty > 0 but destination warehouse has no active quarantine location |
| DISCREPANCY_REQUIRES_REASON | 400 | Shortage or final-level material issue outside normal QC failure without reason |
| TRANSFER_CANCEL_NOT_ALLOWED | 409 | Actor or current status is not allowed to cancel the transfer |
| INVENTORY_VERSION_CONFLICT | 409 | Concurrent inventory update |

### Transfer Business Rules
- Sprint 1 SHALL NOT generate transfer suggestions or automatically decide source/destination/quantity for inter-warehouse transfers.
- Planner SHALL create transfers only from explicit external transfer instructions from Công ty mẹ or a central coordination team.
- Every transfer SHALL store a non-blank `external_instruction_code` for traceability to the external transfer instruction.
- Active transfers SHALL be unique by `external_instruction_code`, source warehouse, destination warehouse, and `document_date`; transfers in `REJECTED` or `CANCELLED` status SHALL NOT block creating a corrected transfer for the same external instruction.
- Công ty mẹ SHALL NOT be modeled as a WMS user in Sprint 1; Planner is the system actor who enters the external instruction.
- Sprint 1 SHALL NOT require uploading attachment files for external transfer instructions.
- Each transfer MAY contain multiple item lines.
- Planner MAY edit transfer header fields and add/update/remove transfer item lines only while status is `NEW`; the edit screen SHALL load the existing transfer and item list before the Planner saves the current intended state.
- During a `NEW` transfer update, the submitted item list SHALL be treated as the full current state; existing transfer item lines omitted from the payload SHALL be removed.
- Transfers SHALL NOT be editable after status changes to `APPROVED`, `REJECTED`, `IN_TRANSIT`, `COMPLETED`, `COMPLETED_WITH_DISCREPANCY`, or `CANCELLED`.
- Trưởng kho nguồn approval SHALL reserve the planned quantity immediately to prevent competing outbound or transfer operations from consuming the same stock, and SHALL require source warehouse scope unless the actor has an authorized manager override role.
- Trưởng kho nguồn MAY reject a `NEW` transfer; rejection SHALL require `rejection_reason` and set status to `REJECTED`.
- A `REJECTED` transfer SHALL be immutable and SHALL NOT be resubmitted; Planner MUST create a new transfer if the external instruction still needs execution.
- Planner MAY cancel only `NEW` transfers.
- Trưởng kho nguồn or an authorized manager MAY cancel `APPROVED` transfers only when shipment has not been recorded; cancellation SHALL release reserved quantity.
- Each transfer SHALL use a dedicated internal-fleet trip (`trip_type = 'TRANSFER'`) with one vehicle and one driver; one transfer cannot share a trip with another transfer.
- Dispatcher SHALL create or assign the dedicated transfer trip; Planner SHALL NOT create or assign transfer trips.
- Dispatcher SHALL verify the selected vehicle and driver are available and not already assigned to an overlapping trip.
- Thủ kho nguồn SHALL record shipment only for transfers in `APPROVED` status and only when assigned to the transfer source warehouse.
- `sent_qty` SHALL equal approved `planned_qty` for every transfer item; sending less or more than the approved quantity SHALL be rejected.
- If `sent_qty` has been recorded but the driver has not departed, cancellation SHALL be blocked until Thủ kho nguồn or an authorized manager performs unship/unload; unship/unload clears recorded `sent_qty`, keeps status `APPROVED`, and creates an audit entry.
- Shipment to `IN_TRANSIT` SHALL occur only after the transfer is `APPROVED`, exactly one `TRANSFER` trip is assigned, `sent_qty` has been recorded for every item, and the assigned driver confirms goods received and vehicle departure.
- Destination receiving SHALL be split by responsibility: Nhân viên kho/Công nhân kho đích records initial received counts; Thủ kho đích checks those counts, records/approves QC, and selects destination location for QC-passed stock; Trưởng kho đích performs the final confirmation.
- Nhân viên kho/Công nhân kho đích MAY edit initial received counts until Thủ kho đích approves receive check; after receive check approval, worker edits SHALL be rejected.
- If an item's initial `received_qty` is lower or higher than `sent_qty`, or the worker reports an issue for that item, Nhân viên kho/Công nhân kho đích SHALL provide item-level `receive_issue_reason`; matching items without reported issues SHALL NOT require a reason. Over-receipt SHALL still be rejected after reason validation.
- Thủ kho đích MAY correct the worker-entered received quantity during receive check. If the checked quantity equals the worker-entered quantity, `receive_checker_note` MAY be provided but is optional; if the checked quantity differs, `receive_checker_note` SHALL be required.
- After receive check approval, the storekeeper-confirmed quantity SHALL become the effective `received_qty` used by final confirmation and inventory settlement; audit before/after values SHALL preserve the worker-entered count and the storekeeper correction.
- `receive_checked_at IS NOT NULL` SHALL mean the receive check has been approved; no separate receive-check status field is required.
- Destination QC SHALL check both received quantity and product quality. QC-failed quantities SHALL be moved to quarantine inventory and excluded from available inventory.
- Thủ kho đích SHALL select `destination_location_id` for QC-passed quantity. QC-failed quantity SHALL be moved automatically to the destination warehouse active quarantine location (`warehouse_locations.is_quarantine = true`); if none exists, confirmation SHALL be rejected.
- `received_qty > sent_qty` SHALL be blocked; the system SHALL NOT create positive transfer discrepancy adjustments for over-receipt.
- If `received_qty < sent_qty` or any final-level material issue outside normal QC failure is reported during receiving, the system SHALL require `discrepancy_reason` before final confirmation; Trưởng kho đích uses `discrepancy_reason` itself to report that final-level issue, with no extra boolean or field.
- QC failure alone SHALL require `qc_failure_reason` during receive-check and SHALL NOT require duplicate `discrepancy_reason` unless Trưởng kho đích reports another final-level issue.
- If `received_qty < sent_qty`, the system SHALL require `discrepancy_reason`, create a `TRANSFER_DISCREPANCY` adjustment for the shortage, and set status to `COMPLETED_WITH_DISCREPANCY`.
- Transfer cancellation SHALL be rejected for `REJECTED`, `IN_TRANSIT`, `COMPLETED`, `COMPLETED_WITH_DISCREPANCY`, or `CANCELLED` transfers.
- Transfer cancellation SHALL be rejected for `REJECTED`, `IN_TRANSIT`, `COMPLETED`, `COMPLETED_WITH_DISCREPANCY`, or `CANCELLED` transfers.

### Audit Trail
- Every transfer mutation SHALL create an audit log with `actor`, `action`, `entity_type`, `entity_id`, `entity_code`, `timestamp`, `before`, and `after`.
- `TRANSFER_CREATE`: Planner creates transfer with status `NEW`.
- `TRANSFER_UPDATE`: Planner edits header or item lines while transfer status is `NEW`.
- `TRANSFER_UPDATE`: Planner edits header or item lines while transfer status is `NEW`.
- `TRANSFER_APPROVE`: Trưởng kho nguồn approves transfer, reserves source inventory, and changes status to `APPROVED`.
- `TRANSFER_REJECT`: Trưởng kho nguồn rejects a `NEW` transfer with a required reason and changes status to `REJECTED`.
- `TRANSFER_REJECT`: Trưởng kho nguồn rejects a `NEW` transfer with a required reason and changes status to `REJECTED`.
- `TRANSFER_TRIP_ASSIGN`: Dispatcher assigns dedicated vehicle and driver trip for the transfer.
- `TRANSFER_SHIP`: Thủ kho nguồn records sent quantities and loading details.
- `TRANSFER_UNSHIP`: Thủ kho nguồn or authorized manager unloads goods before departure and clears recorded sent quantities.
- `TRANSFER_DEPART`: Driver confirms goods received and vehicle departure; system moves inventory from source to In-Transit and changes status to `IN_TRANSIT`.
- `TRANSFER_RECEIVE_COUNT`: Nhân viên kho/Công nhân kho đích records initial received quantities.
- `TRANSFER_RECEIVE_CHECK`: Thủ kho đích checks received counts, records/approves QC, and selects destination location for QC-passed stock.
- `TRANSFER_RECEIVE_CONFIRM`: Trưởng kho đích confirms receipt, moves passed quantity to destination inventory, failed quantity to quarantine inventory, clears In-Transit, and completes the transfer.
- `TRANSFER_DISCREPANCY_CREATE`: System creates shortage adjustment when received quantity is lower than sent quantity.
- `TRANSFER_CANCEL`: Planner cancels a `NEW` transfer without inventory changes, or Trưởng kho nguồn/manager cancels an unshipped `APPROVED` transfer and releases reserved quantity.

## 8. Acceptance Criteria
*Vui lòng xem chi tiết kịch bản kiểm thử tại các tài liệu đặc tả tính năng:*
* [Acceptance - Transfer Creation](./features/feature-planner-transfer-planning.md#6-acceptance-criteria)
* [Acceptance - Transfer Shipment](./features/feature-storekeeper-transfer-ship.md#7-acceptance-criteria)
* [Acceptance - Transfer Receipt](./features/feature-storekeeper-transfer-receive.md#6-acceptance-criteria)

## 9. Out of Scope

- Automated replenishment suggestions and transfer decision algorithms
- Automated replenishment suggestions and transfer decision algorithms
- Multi-warehouse transfer optimization
- Transfer cost tracking
- Third-party logistics (3PL) — internal fleet only
