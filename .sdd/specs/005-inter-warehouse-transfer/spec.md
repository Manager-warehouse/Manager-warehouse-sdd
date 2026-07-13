# Feature Specification: Dieu chuyen Kho Noi bo (Internal Transfer)

**Spec ID**: 005-inter-warehouse-transfer
**Created**: 2026-05-30
**Updated**: 2026-07-12
**Status**: Change requested - harden production transfer flow, fix P0 gaps, and align contracts/tasks with actual implementation
**Features**: US-WMS-11, US-WMS-12

---

## 1. Context and Goal

Phuc Anh van hanh 3 kho vat ly (Hai Phong, Ha Noi, Ho Chi Minh). Hang hoa can duoc dieu chuyen giua cac kho de can bang ton kho, tranh dut gay nguon cung. He thong su dung kho ao `IN_TRANSIT` de theo doi hang dang tren duong van chuyen bang xe noi bo.

Luot nghiep vu nay la **luong rieng** cua dieu chuyen noi bo:
- Ma phieu dieu chuyen: `TRF-YYYYMMDD-####`
- Ma chuyen xe dieu chuyen: `TTR-YYYYMMDD-####`
- Luong nhap hang tu nha cung cap van dung ma `RN-*` va duoc xu ly o man **Phieu nhap & QC**.
- Luong dieu chuyen noi bo `TRF-*` duoc xu ly o man **Dieu chuyen noi bo**, khong tron vao luong `RN`.

Moi phieu dieu chuyen gan voi dung **mot** chuyen xe noi bo rieng, gom xe, tai xe, ngay chuyen va cac buoc ship/receive theo trang thai.

Bo sung luong tien xu ly cho nhu cau can bang ton kho giua cac kho:
- Truong kho co the xem ton kho kha dung cua kho khac o che do read-only de phat hien kho khac con hang ma kho minh dang thieu.
- Truong kho kho dang thieu co the tao **yeu cau dieu chuyen** gui CEO phe duyet, trong do kho dang thieu la kho dich va kho co hang la kho nguon du kien.
- CEO phe duyet hoac tu choi yeu cau. Sau khi CEO phe duyet, he thong phat sinh/dua ra mau de nghi dieu chuyen cho Planner cua kho nguon de tao phieu `TRF-*` va tiep tuc luong dieu chuyen noi bo hien co.
- Luong nay khong tu dong tao `TRF` khi chua duoc CEO duyet va khong tu dong quyet dinh so luong/nguon hang thay con nguoi.

### Features List
- [US-WMS-11: Planner Nhap lenh dieu chuyen kho tu Cong ty me](./features/feature-planner-transfer-planning.md)
- [US-WMS-11A: Truong kho de xuat dieu chuyen tu ton kho kho khac va CEO duyet](./features/feature-warehouse-manager-transfer-request.md)
- [US-WMS-12: Thu kho Nguon Soan & Xuat hang Dieu chuyen](./features/feature-storekeeper-transfer-ship.md)
- [US-WMS-12: Kho Dich Tiep nhan & Xu ly Chenh lech Dieu chuyen](./features/feature-storekeeper-transfer-receive.md)

## Clarifications

### Session 2026-06-16
- Q: Does Sprint 1 include automatic transfer suggestions? -> A: No; Planner manually enters transfers from Cong ty me/central coordination instructions.
- Q: Can transfers be edited or cancelled after creation? -> A: Planner may edit/cancel only `NEW`; source warehouse manager/authorized manager may cancel only unshipped `APPROVED` transfers and must release reservations; if goods were already loaded, unship/unload is required before cancellation; no cancellation is allowed from `IN_TRANSIT` onward.
- Q: Is `external_instruction_code` optional? -> A: No; every transfer must include a non-blank external instruction code for traceability.
- Q: How should duplicate external instructions be handled? -> A: Reject duplicate active transfers with the same external instruction code, source warehouse, destination warehouse, and document date; rejected/cancelled transfers do not block corrected re-entry.
- Q: Who performs destination receiving steps? -> A: Worker records initial quantity; destination storekeeper checks quantity, records QC, selects destination location and approves receive check; destination warehouse manager confirms final receipt.
- Q: How are destination locations and receiving reasons handled? -> A: Storekeeper selects destination location for QC-passed stock; QC-failed stock uses the active destination quarantine location; shortage, QC failure, or any material issue requires a reason.
- Q: How do transfer edit and receive correction work? -> A: Planner edits a loaded existing transfer list and saves the current state; worker shortage/over-count requires an issue reason; destination storekeeper may correct worker count but must add a checker note when values differ.
- Q: Can drivers manually change their own dispatch availability status inside transfer flow? -> A: No; driver dispatch status is managed by dispatcher/admin scheduling and by system auto-transition when a transfer trip starts/ends. Driver only confirms the assigned trip departure inside spec 005.

### Session 2026-06-19
- Q: Who can initiate return-to-source for an overdue trip? -> A: Only WAREHOUSE_MANAGER of the source warehouse, ADMIN, CEO, or PLANNER. Destination storekeeper and destination manager do NOT have this right; only the source manager or authorized role can reverse a transfer direction.
- Q: What happens when trip planned_end_at is in the past while the transfer is still IN_TRANSIT? -> A: The system marks `tripOverdue = true` on the transfer response. The source manager must use the Return to Source action to send goods back. The system blocks receive-count and receive-check actions at the destination and shows an overdue warning in the UI.
- Q: What happens to QC-failed items in internal transfer receiving? -> A: QC-failed quantity is automatically routed to the destination warehouse quarantine bin (is_quarantine = true) at finalReceive. The storekeeper does NOT manually select the quarantine bin; the system selects it automatically. QC-failed items are excluded from available inventory.
- Q: Should the system validate quarantine existence early (at receiveCheck) rather than at finalReceive? -> A: Yes. If qcFailedQty > 0, the system validates at receiveCheck time that the destination warehouse has at least one active quarantine bin. This prevents the storekeeper from completing the check step only to fail at the final confirmation step.
- Q: Can the QC-passed bin selected by the storekeeper be a quarantine bin? -> A: No. The system must reject any attempt to assign a quarantine bin as the destination for QC-passed stock. The storekeeper's bin dropdown already filters out quarantine bins; the backend also enforces this as an invariant.
- Q: What happens after physically damaged internal-transfer goods enter Quarantine? -> A: Spec 005 owns the transfer-to-quarantine handoff; Spec 009 owns the downstream disposal. Transfer-origin quarantine goods cannot use supplier RTV and are not returned to source merely because they are damaged.
- Q: Does a transfer shortage create quarantine stock? -> A: No. Missing quantity is not physically present. It creates a `TRANSFER_DISCREPANCY` adjustment and never becomes a quarantine/disposal quantity.
- Q: What happens when the wrong SKU arrives but remains intact? -> A: It may use Return to Source because the goods still physically exist and can be transported safely. It is not a disposal candidate unless separately confirmed damaged or QC-failed.
- Q: If 30 units are sent and only 28 are received, how much is recognized? -> A: The destination imports and calculates value for exactly 28 physically received units. The missing 2 units are recorded only as a quantity `TRANSFER_DISCREPANCY`; they are excluded from the destination receipt value and all billing totals. Internal transfer creates no sale invoice, revenue, dealer receivable, supplier payable, or supplier Debit Note.
- Q: Who may send an intact wrong-SKU shipment back? -> A: Destination Storekeeper reports `WRONG_SKU`; destination Warehouse Manager approves the return; the assigned driver turns back while stock remains In-Transit. Source Staff then counts, source Storekeeper checks/QC, and source Warehouse Manager final-confirms the same return receiving flow.

### Session 2026-06-24
- Q: Can a warehouse manager inspect stock in another warehouse and request transfer when their own warehouse is short? -> A: Yes. A WAREHOUSE_MANAGER may view cross-warehouse available stock read-only, create a transfer request from their own warehouse need, submit it to CEO, and wait for CEO approval before execution.
- Q: What happens after CEO approval? -> A: The system sends/generates an approved transfer request template for the Planner of the source warehouse. The Planner uses that approved request as the source instruction to create the executable `TRF-*` transfer.
- Q: Does manager-initiated request replace Planner-created transfers from company instructions? -> A: No. It adds a pre-approval request flow. Existing Planner `TRF` creation and source approval/ship/receive flow remain unchanged.

### Session 2026-07-12
- Q: What is the required complete operational flow for internal transfer? -> A: `TRQ draft -> submit -> CEO approve -> Planner revalidate & convert once -> Source manager reserve FIFO eligible -> Dispatcher capacity/overlap plan -> pick + outbound QC + load/handover -> driver depart -> IN_TRANSIT -> driver arrive/handover -> blind count -> storekeeper count/QC/bin-capacity check -> manager final confirmation`.
- Q: Which P0 production gaps must be covered before the flow is accepted? -> A: database schema/code status alignment, nullable transfer-item batch during planning, FIFO eligibility excluding quarantine/inactive locations, outbound QC, destination bin capacity, optimistic locking/concurrency, immutable line-level audit snapshots, and PostgreSQL/Flyway integration tests.
- Q: Is destination receiving allowed immediately after `IN_TRANSIT`? -> A: No. The assigned driver must record arrival and physical handover at the target warehouse before worker blind count, except when an approved return leg flips the target back to the source warehouse.
- Q: How is the return leg controlled? -> A: Wrong-SKU return and overdue return both require explicit reason and photo references when available, driver return departure, return arrival/handover, and source-side count/check/final confirmation. The system must not only toggle `is_returned`.
- Q: What data must a wrong-SKU report contain? -> A: Item-level expected SKU/product, actual SKU/product, affected quantity, reason, and optional photo references/attachments; the report must be reviewable by the destination Warehouse Manager.
- Q: How should trip capacity and resources be handled? -> A: Trip weight/volume must be calculated from transfer lines and checked against vehicle capacity. Driver/vehicle/trip may be changed before departure, but resource release after final receive must only occur if the resource has no other active assignment.
- Q: Does pick/outbound QC/load handover require barcode scanning? -> A: No. The warehouse does not use Barcode/QR in Sprint 1. Source pick, outbound QC, and load/handover are confirmed by selecting the transfer line in the system, entering/confirming quantity, and attaching required photos as operational evidence.
- Q: Can a DRAFT transfer request be changed or deleted by the requesting warehouse manager? -> A: Yes. While `TRQ` is `DRAFT`, the requesting warehouse manager may edit header/item lines or use the UI "Xoa" action, which is implemented as a soft cancellation to `CANCELLED`; submitted/approved/rejected/converted/cancelled requests are immutable except for their allowed workflow actions.
- Q: When do photo-based transfer buttons become available? -> A: Any action requiring photo evidence (outbound QC, load/handover, arrival handover, return handover, and driver POD in outbound delivery) must keep its approve/confirm button disabled until the user has selected an image file or captured a photo from the device camera; manual link entry is not accepted in Sprint 1 UI.

## 2. Actors

| Actor | Vai tro | Nghiep vu lien quan |
|-------|---------|---------------------|
| Planner | Maker | Nhap phieu dieu chuyen kho noi bo theo lenh tu Cong ty me hoac bo phan dieu phoi trung tam |
| Truong kho (Kho dang thieu hang / kho yeu cau) | Maker | Xem ton kho kha dung cua kho khac, tao yeu cau dieu chuyen va gui CEO phe duyet |
| CEO | Approver | Phe duyet hoac tu choi yeu cau dieu chuyen do Truong kho de xuat truoc khi Planner kho nguon tao phieu `TRF` |
| Truong kho (Kho nguon) | Checker | Kiem tra ton kho kha dung tai kho nguon va phe duyet/tu choi phieu dieu chuyen |
| Dispatcher (Kho nguon) | Maker | Lap chuyen xe noi bo rieng cho phieu dieu chuyen, gan xe va tai xe trong pham vi kho nguon |
| Thu kho (Kho nguon) | Maker | Nhan lenh xuat dieu chuyen, soan hang va xac nhan xuat hang len xe |
| Tai xe | Maker | Xac nhan da nhan hang, xe roi kho nguon va van chuyen hang hoa giua cac kho |
| Nhan vien kho/Cong nhan kho dich | Maker | Kiem dem mat so luong thuc nhan ban dau tai kho dich |
| Thu kho (Kho dich) | Checker | Kiem tra lai so luong cong nhan nhap, nhap/chot QC, chon vi tri nhap kho cho hang dat va duyet receive-check |
| Truong kho (Kho dich) | Checker | Xac nhan cuoi cung, xu ly chenh lech hoac van de phat sinh va duyet hoan tat nhap kho noi bo |

## 3. Functional Requirements (EARS)

*Vui long xem chi tiet yeu cau chuc nang EARS tai cac tai lieu dac ta tinh nang:*
- [EARS - Transfer Creation](./features/feature-planner-transfer-planning.md#3-functional-requirements-ears)
- [EARS - Manager Transfer Request and CEO Approval](./features/feature-warehouse-manager-transfer-request.md#3-functional-requirements-ears)
- [EARS - Transfer Shipment](./features/feature-storekeeper-transfer-ship.md#3-functional-requirements-ears)
- [EARS - Transfer Receipt](./features/feature-storekeeper-transfer-receive.md#3-functional-requirements-ears)

### 3.1 Canonical End-to-End Flow

The canonical execution flow SHALL be:

1. Warehouse Manager creates `TRQ` in `DRAFT` from read-only cross-warehouse availability.
2. Warehouse Manager submits `TRQ` to CEO.
3. CEO approves or rejects the `TRQ`.
4. Planner revalidates the approved request, converts it at most once into `TRF`, or creates a manual `TRF` from an external instruction.
5. Source Warehouse Manager approves the `TRF` and reserves FIFO-eligible inventory only.
6. Dispatcher assigns or updates one dedicated `TRANSFER` trip before departure, with capacity and overlap checks.
7. Source Storekeeper picks goods, selects/captures required outbound-QC photo evidence, performs outbound QC, records exact loaded quantities only after QC passes, and records photo-based load/handover evidence before driver departure.
8. Assigned Driver confirms departure; system moves stock to the virtual `IN_TRANSIT` warehouse.
9. Assigned Driver records arrival and the receiving warehouse records physical handover with selected/captured photo evidence before receive-count is enabled.
10. Receiving worker performs blind count.
11. Storekeeper checks count, performs QC, validates destination bin capacity, and approves receive check.
12. Warehouse Manager final-confirms receipt and settlement.

Exception branches SHALL be handled separately:
- Shortage: create incident/discrepancy record plus `TRANSFER_DISCREPANCY` adjustment; missing quantity never becomes quarantine stock.
- Over-receipt: block regular inventory posting and record a discrepancy-hold/incident for the physical excess goods; do not silently ignore physical overage.
- QC failure: move physical failed quantity to Quarantine with internal-transfer origin for Spec 009 disposal.
- Wrong SKU: require line-item report with expected/actual SKU, quantity, reason, and photo references when available; destination manager approves/rejects; approved return requires driver return departure, source arrival/handover with photo evidence, and source receiving.
- Overdue trip: block destination receiving until an authorized return-to-source decision is recorded with reason and photo references when available.

## 4. Non-functional Requirements

| ID | Requirement | Target |
|----|------------|--------|
| NFR-001 | Transfer creation + inventory update | <= 2s |
| NFR-002 | In-Transit inventory query | Real-time (<= 500ms) |
| NFR-003 | Discrepancy adjustment record | Must be immutable after creation |
| NFR-004 | Transfer dashboard for Sprint 1 | Provide role-scoped operational list and current-step panel; advanced KPI/reporting can be deferred |
| NFR-005 | Audit traceability | Every state mutation, trip assignment mutation, and receive/QC mutation must be reconstructable from audit logs |
| NFR-006 | End-to-end verification | Core happy-path and main blocking-path scenarios must be testable from UI through API |
| NFR-007 | Cross-warehouse stock visibility | Read-only cross-warehouse availability lookup should return within <= 1s for normal Sprint 1 data volume |
| NFR-008 | Actual-receipt valuation | Destination inventory quantity and value must be calculated only from physically received and accepted quantity |
| NFR-009 | Database migration safety | Previously applied Flyway migrations must never be edited, renamed, or deleted; transfer fixes require the next additive migration |
| NFR-010 | Concurrency safety | Transfer, transfer request, trip/resource, and inventory state mutations must reject stale writes through optimistic locking or equivalent version checks |
| NFR-011 | Requirement-to-test traceability | Every P0 requirement and exception branch must map to at least one backend integration/service test and, where applicable, one frontend workflow test |

## 5. Data Model

### transfers
- `id` (BIGSERIAL, PK)
- `transfer_number` (VARCHAR(50), UNIQUE, NOT NULL) -- format `TRF-YYYYMMDD-####`
- `source_warehouse_id` (BIGINT, FK->warehouses, NOT NULL)
- `destination_warehouse_id` (BIGINT, FK->warehouses, NOT NULL)
- `status` (VARCHAR(40), DEFAULT 'NEW', CHECK IN ('NEW','APPROVED','REJECTED','IN_TRANSIT','COMPLETED','COMPLETED_WITH_DISCREPANCY','CANCELLED','QUARANTINED'))
- `is_returned` (BOOLEAN, DEFAULT false) -- true after an approved Return to Source; flips receiving scope back to source warehouse
- `return_reason_code` (VARCHAR(30), CHECK IN ('TRIP_OVERDUE','WRONG_SKU','OTHER_APPROVED_REASON'))
- `return_reason` (TEXT)
- `return_requested_by` (BIGINT, FK->users)
- `return_requested_at` (TIMESTAMPTZ)
- `return_approved_by` (BIGINT, FK->users)
- `return_approved_at` (TIMESTAMPTZ)
- `created_by` (BIGINT, FK->users, NOT NULL)
- `external_instruction_code` (VARCHAR(80), NOT NULL)
- `approved_by` (BIGINT, FK->users)
- `approved_at` (TIMESTAMPTZ)
- `rejected_by` (BIGINT, FK->users)
- `rejected_at` (TIMESTAMPTZ)
- `rejection_reason` (TEXT)
- `confirmed_by` (BIGINT, FK->users)
- `confirmed_at` (TIMESTAMPTZ)
- `planned_date` (DATE)
- `actual_received_date` (DATE)
- `discrepancy_reason` (TEXT)
- `trip_id` (BIGINT, FK->trips, UNIQUE)
- `document_date` (DATE, NOT NULL)
- `accounting_period_id` (BIGINT, FK->accounting_periods)
- `notes` (TEXT)
- `created_at` (TIMESTAMPTZ)
- `updated_at` (TIMESTAMPTZ)
- `transfer_request_id` (BIGINT, FK->transfer_requests, NULLABLE) -- present when Planner creates `TRF` from an approved manager request
- `outbound_qc_checked_by` (BIGINT, FK->users)
- `outbound_qc_checked_at` (TIMESTAMPTZ)
- `outbound_qc_result` (VARCHAR(20), CHECK IN ('PASSED','FAILED'))
- `outbound_qc_photo_ref` (TEXT, NOT NULL before outbound QC pass/fail)
- `load_handover_by` (BIGINT, FK->users)
- `load_handover_at` (TIMESTAMPTZ)
- `load_handover_photo_ref` (TEXT, NOT NULL before load handover confirmation)
- `driver_departed_at` (TIMESTAMPTZ)
- `driver_arrived_at` (TIMESTAMPTZ)
- `arrival_handover_at` (TIMESTAMPTZ)
- `return_departed_at` (TIMESTAMPTZ)
- `return_arrived_at` (TIMESTAMPTZ)
- `version` (BIGINT, NOT NULL, DEFAULT 0)

### transfer_requests
- `id` (BIGSERIAL, PK)
- `request_number` (VARCHAR(50), UNIQUE, NOT NULL) -- format `TRQ-YYYYMMDD-####`
- `requesting_warehouse_id` (BIGINT, FK->warehouses, NOT NULL) -- kho dang thieu hang, becomes transfer destination
- `source_warehouse_id` (BIGINT, FK->warehouses, NOT NULL) -- kho co hang, becomes transfer source
- `status` (VARCHAR(40), DEFAULT 'DRAFT', CHECK IN ('DRAFT','SUBMITTED','APPROVED','REJECTED','CONVERTED','CANCELLED'))
- `requested_by` (BIGINT, FK->users, NOT NULL)
- `submitted_at` (TIMESTAMPTZ)
- `approved_by` (BIGINT, FK->users)
- `approved_at` (TIMESTAMPTZ)
- `rejected_by` (BIGINT, FK->users)
- `rejected_at` (TIMESTAMPTZ)
- `rejection_reason` (TEXT)
- `needed_by_date` (DATE)
- `business_reason` (TEXT, NOT NULL)
- `planner_assignee_id` (BIGINT, FK->users) -- Planner of source warehouse or central Planner assigned after CEO approval
- `converted_transfer_id` (BIGINT, FK->transfers)
- `version` (BIGINT, NOT NULL, DEFAULT 0)
- `created_at` (TIMESTAMPTZ)
- `updated_at` (TIMESTAMPTZ)

### transfer_request_items
- `id` (BIGSERIAL, PK)
- `transfer_request_id` (BIGINT, FK->transfer_requests, NOT NULL)
- `product_id` (BIGINT, FK->products, NOT NULL)
- `requested_qty` (DECIMAL(10,2), NOT NULL)
- `observed_source_available_qty` (DECIMAL(10,2), NOT NULL)
- `observed_requesting_available_qty` (DECIMAL(10,2), NOT NULL)
- `shortage_reason` (TEXT)

### transfer_items
- `id` (BIGSERIAL, PK)
- `transfer_id` (BIGINT, FK->transfers, NOT NULL)
- `product_id` (BIGINT, FK->products, NOT NULL)
- `source_location_id` (BIGINT, FK->warehouse_locations)
- `destination_location_id` (BIGINT, FK->warehouse_locations)
- `planned_qty` (DECIMAL(10,2), NOT NULL)
- `sent_qty` (DECIMAL(10,2))
- `received_qty` (DECIMAL(10,2))
- `variance_qty` (DECIMAL(10,2))
- `qc_passed_qty` (DECIMAL(10,2))
- `qc_failed_qty` (DECIMAL(10,2))
- `qc_result` (VARCHAR(20), CHECK IN ('PENDING','PASSED','FAILED','PARTIAL'))
- `qc_failure_reason` (TEXT)
- `receive_issue_reason` (TEXT)
- `receive_checked_by` (BIGINT, FK->users)
- `receive_checked_at` (TIMESTAMPTZ)
- `receive_checker_note` (TEXT)
- `batch_id` (BIGINT, FK->batches, NULLABLE) -- nullable on planned item; batch fidelity is recorded by transfer allocations after FIFO reservation
- `outbound_qc_result` (VARCHAR(20), CHECK IN ('PENDING','PASSED','FAILED'))
- `outbound_qc_note` (TEXT)

### transfer_wrong_sku_reports
- `id` (BIGSERIAL, PK)
- `transfer_id` (BIGINT, FK->transfers, NOT NULL)
- `transfer_item_id` (BIGINT, FK->transfer_items, NOT NULL)
- `expected_product_id` (BIGINT, FK->products, NOT NULL)
- `actual_product_id` (BIGINT, FK->products, NOT NULL)
- `quantity` (DECIMAL(10,2), NOT NULL)
- `reason` (TEXT, NOT NULL)
- `photo_refs` (JSONB, NULLABLE) -- photo URLs/attachment references only; no Barcode/QR scan is required
- `status` (VARCHAR(30), CHECK IN ('REPORTED','APPROVED','REJECTED','RETURN_DEPARTED','RETURN_ARRIVED','CLOSED'))
- `reported_by` (BIGINT, FK->users, NOT NULL)
- `reported_at` (TIMESTAMPTZ, NOT NULL)
- `decided_by` (BIGINT, FK->users)
- `decided_at` (TIMESTAMPTZ)
- `decision_reason` (TEXT)

### trips (transfer usage)
- Transfer trips reuse the shared `trips` entity with `trip_type = 'TRANSFER'`.
- `trip_number` for internal transfer follows `TTR-YYYYMMDD-####`.
- Each transfer SHALL have exactly one transfer trip before shipment can move to `IN_TRANSIT`.
- A transfer trip SHALL be linked to exactly one transfer; multi-transfer trips are out of scope for Sprint 1.
- Driver dispatch status is a master-data scheduling concept, separate from transfer document status. Suggested values for Sprint 1 are `READY`, `ON_TRIP`, and `UNAVAILABLE`.
- Transfer trip `total_weight` and `total_volume` SHALL be calculated from transfer item planned quantities and product package attributes when assigning/reassigning the trip.
- The system SHALL reject trip assignment when calculated weight or volume exceeds selected vehicle capacity.
- Vehicle/driver release at terminal transfer status SHALL check that the resource has no other active trip assignment before setting it back to `AVAILABLE`/`READY`.

### inventories (shared)
- Ton kho tai kho ao In-Transit su dung cung thuc the voi `warehouse_id` lien ket den kho `IN_TRANSIT`.
- Dieu chuyen noi bo khong tao phieu `RN`; nhan hang kho dich xong se cap nhat ton va audit trong luong transfer nay.

## 6. API Spec

*Vui long xem chi tiet API endpoints tai cac tai lieu dac ta tinh nang:*
- [APIs - Transfer Creation](./features/feature-planner-transfer-planning.md#4-api-endpoints)
- [APIs - Manager Transfer Request and CEO Approval](./features/feature-warehouse-manager-transfer-request.md#4-api-endpoints)
- [APIs - Transfer Shipment](./features/feature-storekeeper-transfer-ship.md#4-api-endpoints)
- [APIs - Transfer Receipt](./features/feature-storekeeper-transfer-receive.md#4-api-endpoints)

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
| REJECTION_REASON_REQUIRED | 400 | Truong kho nguon rejects transfer without reason |
| TRANSFER_TRIP_REQUIRED | 400 | Departure attempted before assigning exactly one transfer trip |
| TRANSFER_TRIP_NOT_AVAILABLE | 409 | Selected vehicle or driver is unavailable or already assigned to an overlapping trip |
| TRANSFER_SHIP_NOT_ALLOWED | 409 | Shipment attempted in invalid state, by actor outside source scope, or after shipment already recorded |
| SENT_QTY_MISMATCH | 400 | sent_qty differs from approved planned_qty |
| TRANSFER_UNSHIP_NOT_ALLOWED | 409 | Unship attempted before shipment, after departure, or by unauthorized actor |
| TRANSFER_DEPART_NOT_ALLOWED | 409 | Departure attempted without APPROVED status, assigned trip, sent quantities, or assigned driver |
| ASSIGNED_DRIVER_REQUIRED | 409 | Driver actor is not the driver assigned to the transfer trip |
| RECEIVE_ISSUE_REASON_REQUIRED | 400 | An item has received_qty different from sent_qty or a reported issue without item-level receive issue reason |
| RECEIVED_QTY_EXCEEDS_SENT | 422 | received_qty > sent_qty |
| RECEIVE_CHECK_REQUIRED | 409 | Truong kho dich confirms receipt before Thu kho dich approves receive check |
| CHECKER_NOTE_REQUIRED | 400 | Confirmed received quantity differs from worker-entered received quantity without checker note |
| QC_TOTAL_MISMATCH | 400 | qc_passed_qty + qc_failed_qty != confirmed received quantity |
| QC_FAILURE_REASON_REQUIRED | 400 | qc_failed_qty > 0 without qc_failure_reason |
| DESTINATION_LOCATION_REQUIRED | 400 | qc_passed_qty > 0 without destination_location_id |
| QUARANTINE_LOCATION_REQUIRED | 422 | qc_failed_qty > 0 but destination warehouse has no active quarantine location |
| QUARANTINE_LOCATION_NOT_CONFIGURED | 422 | Same as above — validated eagerly at receiveCheck step before finalReceive |
| QC_PASSED_BIN_MUST_NOT_BE_QUARANTINE | 400 | destinationLocationId provided for QC-passed stock is itself a quarantine bin |
| INVALID_DESTINATION_LOCATION | 400 | Destination bin does not belong to the target warehouse or is inactive |
| DISCREPANCY_REQUIRES_REASON | 400 | Shortage, QC failure follow-up, or another material issue requires a reason |
| TRANSFER_CANCEL_NOT_ALLOWED | 409 | Actor or current status is not allowed to cancel the transfer |
| ONLY_DRAFT_CAN_BE_UPDATED | 409 | Warehouse manager attempts to edit a transfer request after it leaves DRAFT |
| ONLY_DRAFT_CAN_BE_CANCELLED | 409 | Warehouse manager attempts to delete/cancel a transfer request after it leaves DRAFT |
| PHOTO_REF_REQUIRED | 400 | Photo-required transfer action is submitted without selected/captured photo evidence |
| TRANSFER_PHOTO_FILE_INVALID | 400 | Transfer photo upload is missing, not an image, or exceeds the configured size limit |
| TRANSFER_PHOTO_STORAGE_FAILED | 500 | Transfer photo evidence cannot be persisted |
| INVENTORY_VERSION_CONFLICT | 409 | Concurrent inventory update |
| TRANSFER_VERSION_CONFLICT | 409 | Concurrent transfer or transfer-item update |
| TRANSFER_REQUEST_VERSION_CONFLICT | 409 | Concurrent transfer request update or duplicate conversion race |
| TRANSFER_DB_SCHEMA_MISMATCH | 500 | Runtime schema cannot persist a documented transfer status or nullable planned batch field |
| OUTBOUND_QC_REQUIRED | 409 | Shipment/departure attempted before source outbound QC is passed |
| OUTBOUND_QC_FAILED | 409 | Source outbound QC failed and goods cannot be loaded |
| BIN_CAPACITY_EXCEEDED | 422 | QC-passed destination bin does not have enough remaining capacity |
| TRIP_CAPACITY_EXCEEDED | 422 | Calculated transfer weight or volume exceeds vehicle capacity |
| TRANSFER_ARRIVAL_REQUIRED | 409 | Receive-count attempted before driver arrival/handover is recorded |
| RETURN_DEPARTURE_REQUIRED | 409 | Source return receiving attempted before approved return leg departure |
| RETURN_ARRIVAL_REQUIRED | 409 | Source return receiving attempted before approved return arrival/handover |
| RETURN_TO_SOURCE_REASON_REQUIRED | 400 | Overdue return-to-source is requested without reason |
| WAREHOUSE_MANAGER_ROLE_REQUIRED | 403 | Return-to-source action attempted by a non-manager actor without authorized override role |
| RETURN_REQUEST_NOT_ALLOWED | 409 | Return report attempted outside `IN_TRANSIT` or after an existing decision |
| WRONG_SKU_REASON_REQUIRED | 400 | Storekeeper reports wrong SKU without item details/reason |
| RETURN_APPROVAL_NOT_ALLOWED | 403 | Actor is not destination manager for wrong-SKU return approval |
| RETURN_REQUEST_REQUIRED | 409 | Manager attempts wrong-SKU return approval before Storekeeper report |
| TRIP_SCHEDULE_INVALID | 422 | Trip planned_end_at is not after planned_start_at |
| TRIP_START_IN_PAST | 422 | Trip planned_start_at is in the past |
| TRIP_END_IN_PAST | 422 | Trip planned_end_at is in the past |
| TRIP_RESOURCE_OVERLAP | 409 | Vehicle or driver already assigned to another overlapping trip |
| CROSS_WAREHOUSE_STOCK_VIEW_FORBIDDEN | 403 | Actor is not allowed to view cross-warehouse stock availability |
| TRANSFER_REQUEST_ITEMS_REQUIRED | 400 | Warehouse manager submits transfer request without item lines |
| TRANSFER_REQUEST_QTY_EXCEEDS_SOURCE_AVAILABLE | 422 | Requested quantity is greater than observed source available quantity at submit/approval time |
| TRANSFER_REQUEST_REASON_REQUIRED | 400 | Transfer request is missing business reason or shortage reason required for CEO review |
| TRANSFER_REQUEST_APPROVAL_NOT_ALLOWED | 409 | CEO approval/rejection attempted in invalid request status |
| CEO_REJECTION_REASON_REQUIRED | 400 | CEO rejects transfer request without reason |
| TRANSFER_REQUEST_NOT_APPROVED | 409 | Planner attempts to create `TRF` from a request that is not approved |
| TRANSFER_REQUEST_ALREADY_CONVERTED | 409 | Planner attempts to create another `TRF` from a request already converted to a transfer |

### Transfer Business Rules
- Sprint 1 SHALL NOT generate transfer suggestions or automatically decide source/destination/quantity for inter-warehouse transfers.
- Planner SHALL create transfers only from explicit external transfer instructions from Cong ty me, a central coordination team, or an approved manager transfer request.
- A WAREHOUSE_MANAGER MAY view read-only available stock in other active warehouses to support manual replenishment decisions for their assigned warehouse.
- Cross-warehouse stock visibility SHALL expose available quantity (`total_qty - reserved_qty`) and basic product/warehouse identifiers, but SHALL NOT allow the viewing manager to mutate another warehouse's inventory.
- A WAREHOUSE_MANAGER MAY create a transfer request only where the requesting warehouse is within their assigned warehouse scope; that requesting warehouse becomes the destination if the request is later converted to a `TRF`.
- A transfer request SHALL identify the source warehouse, requesting/destination warehouse, product lines, requested quantity, observed source available quantity, observed requesting warehouse available quantity, needed-by date, and business reason.
- A transfer request SHALL be submitted to CEO for approval before Planner can create an executable `TRF` from it.
- CEO MAY approve or reject a submitted transfer request. Rejection SHALL require a reason and preserve the request for audit.
- CEO approval SHALL NOT reserve inventory and SHALL NOT move stock. Inventory reservation remains part of source warehouse manager approval on the later `TRF`.
- After CEO approval, the system SHALL generate or send an approved transfer request template/notification to the Planner assigned to the source warehouse so the Planner can create the executable `TRF`.
- Planner MAY create a `TRF` from an approved transfer request. The resulting `TRF` SHALL copy source warehouse, destination warehouse, item lines, request reference, and traceability code from the approved request.
- An approved transfer request SHALL be converted to at most one active `TRF`; duplicate conversion SHALL be rejected.
- If source availability changes before `TRF` approval, existing source manager stock checks and `INSUFFICIENT_TRANSFER_STOCK` handling still apply.
- Every transfer SHALL store a non-blank `external_instruction_code` for traceability.
- Active transfers SHALL be unique by `external_instruction_code`, source warehouse, destination warehouse, and `document_date`; transfers in `REJECTED` or `CANCELLED` SHALL NOT block creating a corrected transfer.
- Cong ty me SHALL NOT be modeled as a WMS user in Sprint 1; Planner is the system actor who enters the external instruction.
- Sprint 1 SHALL NOT require uploading attachment files for external transfer instructions.
- Each transfer MAY contain multiple item lines.
- Planner MAY edit transfer header fields and add/update/remove transfer item lines only while status is `NEW`.
- During a `NEW` transfer update, the submitted item list SHALL be treated as the full current state.
- Transfers SHALL NOT be editable after status changes to `APPROVED`, `REJECTED`, `IN_TRANSIT`, `COMPLETED`, `COMPLETED_WITH_DISCREPANCY`, `CANCELLED`, or `QUARANTINED`.
- Truong kho nguon approval SHALL reserve the planned quantity immediately and SHALL require source warehouse scope unless the actor has an authorized manager override role.
- Source reservation SHALL allocate only FIFO-eligible stock from active, non-quarantine, source-scoped locations. Quarantine inventory, inactive locations, locked locations, and inventory with non-positive available quantity SHALL be excluded from reservation.
- Truong kho nguon MAY reject a `NEW` transfer; rejection SHALL require `rejection_reason` and set status to `REJECTED`.
- A `REJECTED` transfer SHALL be immutable and SHALL NOT be resubmitted; Planner MUST create a new transfer if needed.
- Planner MAY cancel only `NEW` transfers.
- Truong kho nguon or an authorized manager MAY cancel `APPROVED` transfers only when shipment has not been recorded; cancellation SHALL release reserved quantity.
- Each transfer SHALL use a dedicated internal-fleet trip (`trip_type = 'TRANSFER'`) with one vehicle and one driver.
- Dispatcher SHALL create or assign the dedicated transfer trip and SHALL only operate on transfers whose **source warehouse** is in the dispatcher's assigned warehouse scope.
- Dispatcher SHALL verify the selected vehicle and driver are available and not already assigned to an overlapping trip.
- Dispatcher SHALL calculate transfer trip weight and volume from product/package data and planned quantities, reject overloaded vehicles, and store the calculated totals on the trip.
- Driver candidates for a transfer trip SHALL belong to the transfer source warehouse scope; drivers assigned only to unrelated warehouses SHALL NOT be selectable.
- Drivers SHALL NOT manually change their own dispatch availability status from the transfer flow. Dispatcher/admin manages driver readiness, and the system MAY auto-switch driver status to `ON_TRIP` on departure and back to `READY` when the transfer trip is completed or cancelled without another active assignment.
- Thu kho nguon SHALL record shipment only for transfers in `APPROVED` status and only when assigned to the transfer source warehouse.
- Thu kho nguon SHALL complete outbound QC before shipment can be loaded or departed. Outbound QC SHALL verify correct SKU, physical condition, packaging integrity, and loaded quantity readiness.
- Outbound QC SHALL be confirmed by user-entered result and required photos, not by Barcode/QR scanning.
- Selected/captured transfer photos SHALL be uploaded as multipart files first. The business action payload SHALL store only the returned short `photoRef`; raw base64/data URLs SHALL NOT be sent in `photoRef` because camera images can exceed request timeout and audit payload limits.
- Pick confirmation SHALL use the selected transfer line and quantity in the system; the system SHALL NOT require scanning SKU, bin, carton, or pallet barcodes.
- Load/handover to the assigned driver SHALL be recorded before departure and SHALL require at least one photo of the loaded goods or handover condition.
- `sent_qty` SHALL equal approved `planned_qty` for every transfer item.
- Sprint 1 supports one shipment record and one final receiving cycle per transfer. Split shipment, split receive, or multiple partial departure/arrival legs are not included.
- Driver reassignment or trip reschedule is supported only before departure. After the assigned driver departs and the transfer reaches `IN_TRANSIT`, changing driver or rescheduling the same trip is out of scope for Sprint 1.
- Multiple unfinished receive attempts MAY overwrite the worker draft before receive-check approval, but once receive-check is approved the flow continues toward a single final confirmation.
- If `sent_qty` has been recorded but the driver has not departed, cancellation SHALL be blocked until Thu kho nguon or an authorized manager performs unship/unload.
- Shipment to `IN_TRANSIT` SHALL occur only after the transfer is `APPROVED`, exactly one `TRANSFER` trip is assigned, `sent_qty` has been recorded for every item, and the assigned driver confirms departure.
- Shipment to `IN_TRANSIT` SHALL also require outbound QC passed and source load/handover recorded.
- Destination or source return receiving SHALL be blocked until the assigned driver records arrival and physical handover at the active receiving warehouse.
- Destination receiving SHALL stay inside the **Transfer** module. Internal transfer receiving SHALL NOT be merged into the supplier inbound `RN` workflow.
- Destination receiving SHALL be split by responsibility: worker records initial counts; destination storekeeper checks those counts, records/approves QC, and selects destination location for QC-passed stock; destination warehouse manager performs final confirmation.
- Nhan vien kho/Cong nhan kho dich MAY edit initial received counts until Thu kho dich approves receive check; after that, worker edits SHALL be rejected.
- If an item's initial `received_qty` is different from `sent_qty`, the worker SHALL provide item-level `receive_issue_reason`.
- Thu kho dich MAY correct the worker-entered received quantity during receive check; if the checked quantity differs, `receive_checker_note` SHALL be required.
- After receive check approval, the storekeeper-confirmed quantity SHALL become the effective `received_qty`.
- Destination QC SHALL check both received quantity and product quality. QC-failed quantities SHALL be moved to quarantine inventory and excluded from available inventory.
- Thu kho dich SHALL select `destination_location_id` for QC-passed quantity only. The `destination_location_id` SHALL NOT be a quarantine bin; the system enforces this as an invariant.
- The selected destination bin for QC-passed stock SHALL have enough remaining capacity for the confirmed passed quantity before receive-check or final confirmation can proceed.
- QC-failed quantity SHALL be automatically routed to the destination warehouse quarantine location (`is_quarantine = true`) by the system at finalReceive; Thu kho dich does NOT manually select the quarantine bin.
- QC-failed transfer quantity placed in Quarantine SHALL retain its `transfer_id` and `transfer_item_id` origin and SHALL be handed off to the Spec 009 disposal flow.
- Transfer-origin quarantine goods SHALL NOT be eligible for supplier RTV or supplier Debit Note creation.
- A physical transfer shortage SHALL create only a `TRANSFER_DISCREPANCY` adjustment; the missing quantity SHALL NOT increase quarantine inventory or create a disposal candidate.
- An intact wrong-SKU shipment MAY use Return to Source. Return to Source SHALL NOT be used as the terminal treatment for goods already confirmed physically damaged; those goods SHALL follow Spec 009 disposal.
- Destination Storekeeper SHALL report an intact wrong-SKU shipment with item-level expected SKU, actual SKU, quantity, and reason while the transfer remains `IN_TRANSIT`.
- Destination Storekeeper SHALL attach or reference available photos for a wrong-SKU report when available; photo evidence is optional for wrong-SKU in Sprint 1 but the report schema SHALL support it.
- Destination Warehouse Manager SHALL approve or reject a `WRONG_SKU` return request within destination warehouse scope. Approval SHALL set `is_returned = true`, preserve the same transfer/trip/driver assignment, and flip receiving scope to the source warehouse.
- The assigned driver SHALL turn back with the same physical stock after wrong-SKU return approval. The vehicle, driver, and trip SHALL remain active until source final confirmation.
- The assigned driver SHALL record return departure and source arrival/handover before source receiving can start.
- Source return receiving SHALL repeat the controlled three-step flow: source Staff records count; source Storekeeper checks quantity/QC; source Warehouse Manager final-confirms receipt.
- On source final confirmation, QC-passed returned quantity SHALL increase source regular inventory, damaged quantity SHALL enter source Quarantine, shortages SHALL create `TRANSFER_DISCREPANCY`, In-Transit SHALL be cleared, and the transfer SHALL finish as `COMPLETED` with `is_returned = true`.
- A finalized transfer shortage SHALL calculate destination inventory quantity and value only from `received_qty`. Missing quantity SHALL remain a quantity-only `TRANSFER_DISCREPANCY` and SHALL NOT be included in destination receipt value, invoice, revenue, dealer receivable, supplier payable, or supplier Debit Note.
- The system SHALL NOT automatically charge a driver, employee, or warehouse for transfer loss. Liability/recovery requires a separate approved investigation/accounting process.
- When `qcFailedQty > 0`, the system SHALL validate at receiveCheck time that the destination warehouse has at least one active quarantine bin (`QUARANTINE_LOCATION_NOT_CONFIGURED`); this prevents receiving being blocked at the finalReceive step.
- The system SHALL surface a quarantine destination hint in the UI (e.g., "2 sp lỗi → WH-HCM-Q01 (tự động)") when the storekeeper enters QC-failed quantity greater than zero.
- `received_qty > sent_qty` SHALL be blocked.
- If `received_qty < sent_qty` or any final-level material issue is reported during receiving, the system SHALL require `discrepancy_reason`.
- QC failure alone SHALL require `qc_failure_reason` during receive-check and SHALL NOT require duplicate `discrepancy_reason` unless another final-level issue exists.
- If `received_qty < sent_qty`, the system SHALL create a `TRANSFER_DISCREPANCY` adjustment and set status to `COMPLETED_WITH_DISCREPANCY`.
- Transfer cancellation SHALL be rejected for `REJECTED`, `IN_TRANSIT`, `COMPLETED`, `COMPLETED_WITH_DISCREPANCY`, `CANCELLED`, or `QUARANTINED` transfers.
- **Return to Source**:
  - For `TRIP_OVERDUE`, the source manager (WAREHOUSE_MANAGER scoped to the source warehouse, ADMIN, CEO, or PLANNER) MAY approve return while the transfer remains `IN_TRANSIT` only when the trip is overdue and a non-blank reason is recorded. Photo references SHOULD be attached when available.
  - For `WRONG_SKU`, destination Storekeeper SHALL submit a return report and destination Warehouse Manager SHALL approve or reject it. Destination Staff/Storekeeper SHALL NOT final-receive the wrong SKU into regular inventory before the decision.
  - After approval, `is_returned = true`; the same driver turns back and receiving scope flips to the source warehouse.
  - Source Staff, source Storekeeper, and source Warehouse Manager SHALL execute receive-count, receive-check/QC, and final-receive respectively.
- **Quarantine Rejection**:
  - WHEN a Storekeeper (at receive check stage) or a Warehouse Manager (at final confirmation stage) decides to reject the entire shipment due to severe damage or discrepancy, they SHALL trigger a quarantine rejection.
  - The rejection SHALL require a `rejection_reason`.
  - The system SHALL set the transfer status to `QUARANTINED`.
  - The system SHALL update all transfer items so that `received_qty = sent_qty`, `qc_passed_qty = 0`, and `qc_failed_qty = sent_qty`. The system SHALL record the rejection reason in each item's `checker_note` and `qc_failure_reason`.
  - The system SHALL deduct all transit stock of this transfer from the `IN_TRANSIT` virtual location and add it to the destination warehouse's active quarantine bin (or source warehouse's active quarantine bin if `is_returned = true`). If no quarantine bin exists, the operation SHALL fail with `QUARANTINE_LOCATION_NOT_CONFIGURED`.
  - The system SHALL release the vehicle, driver, and trip, setting the vehicle and driver back to `AVAILABLE` and the trip status to `COMPLETED`.
  - The system SHALL write a `TRANSFER_QUARANTINE_REJECT` audit log containing the rejection reason.
  - The resulting quarantine stock SHALL be marked as internal-transfer origin and made available to the Spec 009 disposal workflow only; RTV SHALL be blocked.
- **Trip date validation**: When assigning a trip, `planned_start_at` SHALL NOT be in the past, `planned_end_at` SHALL NOT be in the past, and `planned_end_at` SHALL be after `planned_start_at`. If any condition is violated, the assignment SHALL be rejected with the appropriate error code (`TRIP_START_IN_PAST`, `TRIP_END_IN_PAST`, `TRIP_SCHEDULE_INVALID`).
- **Read-only operations**: GET/detail/list endpoints SHALL NOT mutate transfer status, trip status, inventory, or audit state. Overdue flags may be computed in the response without changing persisted status.
- If an item's initial `received_qty` is different from `sent_qty`, the worker SHALL provide item-level `receive_issue_reason`.
- Thu kho dich MAY correct the worker-entered received quantity during receive check; if the checked quantity differs, `receive_checker_note` SHALL be required.
- After receive check approval, the storekeeper-confirmed quantity SHALL become the effective `received_qty`.
- Destination QC SHALL check both received quantity and product quality. QC-failed quantities SHALL be moved to quarantine inventory and excluded from available inventory.
- Thu kho dich SHALL select `destination_location_id` for QC-passed quantity.
- `received_qty > sent_qty` SHALL be blocked.
- If `received_qty < sent_qty` or any final-level material issue is reported during receiving, the system SHALL require `discrepancy_reason`.
- QC failure alone SHALL require `qc_failure_reason` during receive-check and SHALL NOT require duplicate `discrepancy_reason` unless another final-level issue exists.
- If `received_qty < sent_qty`, the system SHALL create a `TRANSFER_DISCREPANCY` adjustment and set status to `COMPLETED_WITH_DISCREPANCY`.
- Transfer cancellation SHALL be rejected for `REJECTED`, `IN_TRANSIT`, `COMPLETED`, `COMPLETED_WITH_DISCREPANCY`, or `CANCELLED` transfers.

### Audit Trail
- Every transfer mutation SHALL create an audit log with `actor`, `action`, `entity_type`, `entity_id`, `entity_code`, `timestamp`, `before`, and `after`.
- `TRANSFER_CREATE`: Planner creates transfer with status `NEW`.
- `TRANSFER_REQUEST_CREATE`: Warehouse manager creates transfer request from cross-warehouse stock view.
- `TRANSFER_REQUEST_SUBMIT`: Warehouse manager submits request to CEO.
- `TRANSFER_REQUEST_CEO_APPROVE`: CEO approves request and triggers the approved request template/notification for source Planner.
- `TRANSFER_REQUEST_CEO_REJECT`: CEO rejects request with required reason.
- `TRANSFER_REQUEST_CONVERT`: Planner creates a `TRF` from an approved transfer request.
- `TRANSFER_UPDATE`: Planner edits header or item lines while transfer status is `NEW`.
- `TRANSFER_APPROVE`: Truong kho nguon approves transfer, reserves source inventory, and changes status to `APPROVED`.
- `TRANSFER_REJECT`: Truong kho nguon rejects a `NEW` transfer with a required reason and changes status to `REJECTED`.
- `TRANSFER_TRIP_ASSIGN`: Dispatcher assigns dedicated vehicle and driver trip for the transfer.
- `TRANSFER_TRIP_REASSIGN`: Dispatcher changes vehicle, driver, or schedule before departure.
- `TRANSFER_OUTBOUND_QC`: Source Storekeeper records outbound QC result and photo references before load/departure.
- `TRANSFER_SHIP`: Thu kho nguon records sent quantities and loading details.
- `TRANSFER_LOAD_HANDOVER`: Source Storekeeper hands loaded stock to the assigned driver with required photo confirmation.
- `TRANSFER_UNSHIP`: Thu kho nguon or authorized manager unloads goods before departure and clears recorded sent quantities.
- `TRANSFER_DEPART`: Driver confirms departure; system moves inventory from source to In-Transit and changes status to `IN_TRANSIT`.
- `TRANSFER_ARRIVE`: Driver records arrival at destination/source return warehouse.
- `TRANSFER_ARRIVAL_HANDOVER`: Receiving warehouse accepts physical handover from the assigned driver.
- `TRANSFER_RECEIVE_COUNT`: Nhan vien kho/Cong nhan kho dich records initial received quantities.
- `TRANSFER_RECEIVE_CHECK`: Thu kho dich checks received counts, records/approves QC, and selects destination location for QC-passed stock.
- `TRANSFER_RECEIVE_CONFIRM`: Truong kho dich confirms receipt, moves passed quantity to destination inventory, failed quantity to quarantine inventory, clears In-Transit, and completes the transfer.
- `TRANSFER_DISCREPANCY_CREATE`: System creates shortage adjustment when received quantity is lower than sent quantity.
- `TRANSFER_RETURN_TO_SOURCE`: Source warehouse manager or authorized role triggers return-to-source on an overdue IN_TRANSIT transfer; sets `is_returned = true` and flips receiving scope to source warehouse.
- `TRANSFER_RETURN_REQUEST`: Destination Storekeeper reports intact wrong SKU with item detail and reason.
- `TRANSFER_RETURN_APPROVE`: Destination Warehouse Manager approves wrong-SKU return, keeps stock In-Transit, and flips receiving scope to source warehouse.
- `TRANSFER_RETURN_REJECT`: Destination Warehouse Manager rejects the return request with reason and leaves destination receiving unresolved for corrective action.
- `TRANSFER_RETURN_DEPART`: Assigned driver confirms return-leg departure after approved wrong-SKU or overdue return.
- `TRANSFER_RETURN_ARRIVE`: Assigned driver records arrival back at source warehouse.
- `TRANSFER_RETURN_HANDOVER`: Source warehouse records physical handover for returned goods.
- `TRANSFER_QUARANTINE_REJECT`: Storekeeper or Warehouse Manager rejects the entire transfer order, moving all stock to the target warehouse quarantine location and setting status to `QUARANTINED`.
- `TRANSFER_DISPOSAL_HANDOFF`: Transfer-origin quarantine stock is linked to the Spec 009 disposal workflow without changing inventory until disposal approval.
- `TRANSFER_CANCEL`: Planner cancels a `NEW` transfer without inventory changes, or Truong kho nguon/manager cancels an unshipped `APPROVED` transfer and releases reserved quantity.
- If driver reassignment or trip reschedule is later supported before departure, those changes SHALL also require dedicated audit actions such as `TRANSFER_TRIP_REASSIGN` or `TRANSFER_TRIP_RESCHEDULE`.
- Audit before/after snapshots SHALL include transfer header, transfer items, FIFO allocations, wrong-SKU report lines, QC quantities, trip/resource state, and inventory movement references sufficient to reconstruct the mutation.

## 8. Operational Visibility and Test Scope

### Transfer dashboard scope in Sprint 1
- The transfer workspace SHALL provide role-scoped visibility into:
  - transfer number, route, status, line count
  - assigned trip/vehicle/driver when available
  - current-step guidance for the active role
  - item-level planned/sent/received/QC quantities
- Advanced dashboard/reporting is currently light. The following may be deferred beyond Sprint 1:
  - trend charts by route or warehouse pair
  - SLA/lead-time analytics
  - driver utilization reports
  - discrepancy rate reports
  - cancellation / rejection analytics

### End-to-end test scope expectation
- Minimum end-to-end coverage for spec 005 MUST include:
  - Warehouse Manager creates/submits `TRQ`, CEO approves, and Planner converts it once
  - Planner creates manual `TRF`
  - source manager approves
  - source manager reserves only FIFO-eligible non-quarantine active-location stock
  - dispatcher assigns vehicle + valid source-scoped driver with capacity and overlap validation
  - source storekeeper completes photo-confirmed outbound QC and ships
  - assigned driver departs
  - assigned driver arrives and handover is recorded
  - destination worker counts
  - destination storekeeper checks + QC + destination bin capacity
  - destination manager final-confirms
  - blocking paths for schema mismatch prevention, insufficient stock, invalid driver scope, cancellation after ship without unship, over-receipt, receive before arrival, wrong-SKU return without line details, overloaded trip, and stale concurrent updates
- A requirement-to-test matrix SHALL be maintained in tasks.md or quickstart.md for all P0 requirements and exception branches.
- Frontend-to-backend verification SHALL exercise every role-visible primary action button in the transfer workspace and verify both the UI state transition and the backend state/inventory/audit result.
- Every transfer endpoint SHALL have happy-path, validation-error, authorization/scope-error, invalid-state, and stale-version tests where applicable.
- Every critical frontend action SHALL have tests for visible/enabled state, hidden/disabled state, successful click, failed API response display, and post-success refresh/state update.
- Deploy readiness SHALL require passing backend unit tests, backend controller/integration tests, PostgreSQL/Flyway migration tests, frontend tests, frontend build, backend compile, and at least one full-stack smoke path covering `TRQ -> TRF -> final receive`.
- The following deep scenarios are acknowledged but may remain outside Sprint 1 automated E2E coverage:
  - driver change mid-trip
  - trip reschedule after operational start
  - multiple unfinished receive sessions across shifts
  - split shipment
  - split receive

## 9. Acceptance Criteria

*Vui long xem chi tiet kich ban kiem thu tai cac tai lieu dac ta tinh nang:*
- [Acceptance - Transfer Creation](./features/feature-planner-transfer-planning.md#6-acceptance-criteria)
- [Acceptance - Manager Transfer Request and CEO Approval](./features/feature-warehouse-manager-transfer-request.md#6-acceptance-criteria)
- [Acceptance - Transfer Shipment](./features/feature-storekeeper-transfer-ship.md#7-acceptance-criteria)
- [Acceptance - Transfer Receipt](./features/feature-storekeeper-transfer-receive.md#6-acceptance-criteria)

## 10. Out of Scope

- Automated replenishment suggestions and transfer decision algorithms beyond manager-initiated manual requests
- Multi-warehouse transfer optimization
- Transfer cost tracking
- Third-party logistics (3PL) for transfer flow
- Merging `TRF` internal transfer receive processing into the supplier inbound `RN` module
- Advanced transfer dashboard/report suite beyond the operational workspace
- Driver self-service availability management from the transfer module
- Changing driver after departure
- Rescheduling an in-progress trip
- Split shipment from one transfer into multiple departure events
- Split receive from one transfer into multiple final receipt events
- Execution and approval of disposal for transfer-origin quarantine goods (owned by Spec 009; Spec 005 owns classification, traceability, and handoff)
- QC grade classification for internally transferred goods (household goods domain has no quality tiers)

---

## 11. Implementation Notes (Sprint 1 actuals)

> This section documents decisions and changes discovered during Sprint 1 implementation that deviate from or extend the original spec.

### 11.1 is_returned field and Return-to-Source flow
- Field `is_returned BOOLEAN DEFAULT false` was added to `transfers` table.
- When `is_returned = true`, all receive scope checks use `sourceWarehouseId` instead of `destinationWarehouseId`.
- Overdue Return to Source may be triggered by WAREHOUSE_MANAGER scoped to **source** warehouse, ADMIN, CEO, or PLANNER.
- Wrong-SKU Return to Source requires a destination Storekeeper report followed by destination Warehouse Manager approval.
- After either approval path, the assigned driver returns the goods and the source warehouse repeats count, check/QC, and final confirmation.
- API: `POST /api/v1/inter-warehouse-transfers/{id}/return-to-source`

### 11.2 Trip date validation at assign time
- When assigning a trip (`TRANSFER_TRIP_ASSIGN`), the system validates:
  - `planned_start_at` must not be in the past.
  - `planned_end_at` must not be in the past.
  - `planned_end_at` must be after `planned_start_at`.
- Validation fires with Vietnamese-translated messages via GlobalExceptionHandler.
- UI shows a plain-language alert for past-date submissions instead of silently cancelling.

### 11.3 Quarantine validation moved earlier
- `QUARANTINE_LOCATION_NOT_CONFIGURED` is now validated at `receiveCheck` time (not only at `finalReceive`).
- This ensures the storekeeper is informed of missing quarantine configuration before completing the check step.
- Additionally, the system now rejects (`QC_PASSED_BIN_MUST_NOT_BE_QUARANTINE`) any attempt to assign a quarantine bin as the QC-passed destination.

### 11.4 Quarantine destination UI hint
- When the storekeeper enters `qcFailedQty > 0` in the Kiểm tra count/QC form, the system displays a dynamic hint below the QC lỗi input:
  `"{N} sp lỗi → WH-XYZ-Q01 (tự động)"`
- If no quarantine bin exists, the hint shows a warning: `"⚠ Kho đích chưa có Quarantine Bin!"`
- This is a read-only informational hint; the storekeeper cannot change or override the quarantine destination.

### 11.5 Trip overdue detection
- The transfer response includes a computed field `tripOverdue` which is `true` when the assigned trip's `planned_end_at` < current time and the transfer is still `IN_TRANSIT`.
- When `tripOverdue = true` and `is_returned = false`, the destination UI shows an overdue warning instead of receive actions.
- Return to Source action is surfaced only to the source manager role.

### 11.6 Production hardening delta
- The implementation backlog after 2026-07-12 MUST prioritize P0 production correctness over new UI polish.
- Applied migrations (`V1` through latest deployed version) MUST remain immutable. Schema corrections for transfer status, nullable planned batch, version columns, wrong-SKU reports, arrival/handover, outbound QC, trip capacity, and incident/discrepancy hold data MUST be introduced through the next additive migration.
- The OpenAPI contract MUST use the implemented resource base `/api/v1/inter-warehouse-transfers` for transfer execution endpoints and `/api/v1/transfer-requests` for request endpoints.
