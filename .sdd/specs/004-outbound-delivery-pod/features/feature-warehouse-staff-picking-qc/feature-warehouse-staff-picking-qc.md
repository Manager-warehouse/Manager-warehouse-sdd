# Feature: Nhân viên kho Nhập kết quả lấy hàng & QC Outbound (US-WMS-07B)

## 1. Context and Goal

Nhân viên kho lấy hàng thực tế theo kế hoạch của Thủ kho, kiểm tra chất lượng từng sản phẩm và nhập số lượng đã lấy, số lượng đạt QC, số lượng không đạt QC theo từng Delivery Order item, allocation, batch, location và zone đã được lập kế hoạch. Feature này không dùng trạng thái `PICKING`; Delivery Order ở `WAITING_PICKING` trong lúc chờ nhân viên kho nhập kết quả lấy hàng/QC.

Khi nhân viên kho lưu kết quả QC, hàng đạt QC được chuyển nội bộ từ bin gốc sang outbound staging trong cùng kho và tiếp tục bị giữ cho Delivery Order, chưa được xuất bán. Với `qcPassQty`, hệ thống giảm `total_qty` và `reserved_qty` ở source inventory, sau đó cộng `total_qty` và `reserved_qty` vào staging inventory cùng product/batch. Hàng không đạt QC được chuyển sang quarantine, tạo quarantine record, tạo inventory adjustment record loại `QC_FAIL_OUTBOUND` cho phần fail, trừ khỏi tồn kho hợp lệ và không được tính vào available inventory. Việc trừ tồn kho xuất bán thực tế chỉ xảy ra sau này khi tài xế xác nhận nhận hàng và Delivery Order chuyển sang `IN_TRANSIT`.

Sau khi Nhân viên kho nhập kết quả lấy hàng/QC một lần duy nhất cho toàn bộ kế hoạch hiện tại, Delivery Order chuyển sang `QC_PENDING_APPROVAL` kể cả khi số lượng đạt QC chưa đủ. Nếu có hàng fail QC, Thủ kho xem kết quả ở trạng thái chờ duyệt và chọn hàng thay thế ở feature picking plan; khi replacement plan được lưu, Delivery Order quay lại `WAITING_PICKING` để Nhân viên kho lấy và QC phần hàng thay thế. Khi Delivery Order quay lại `WAITING_PICKING` do replacement, `pick-qc-result` chỉ yêu cầu các allocation replacement hoặc allocation còn `PLANNED`/chưa có QC record; các allocation cũ đã QC pass và đang nằm ở outbound staging không phải nhập lại.

## 2. Actors

* **Nhân viên kho**: Lấy hàng từ bin/zone theo kế hoạch trong kho được gán, kiểm tra chất lượng sản phẩm và nhập số lượng đã lấy, đạt QC, không đạt QC theo từng Delivery Order item, allocation, batch, location và zone.
* **Thủ kho**: Phê duyệt kết quả chất lượng outbound và chọn hàng thay thế khi có QC fail.
* **Trưởng kho**: Phê duyệt hoặc từ chối xuất kho sau khi Delivery Order đã `QC_COMPLETED`.

## 3. Functional Requirements (EARS)

* **Ubiquitous:**
  * The system SHALL allow warehouse staff to record picking/QC results only for Delivery Orders belonging to warehouses assigned to their user account.
  * The system SHALL create `DELIVERY_ORDER_PICK_COMPLETE`, `OUTBOUND_QC_FAIL_QUARANTINE`, `DELIVERY_ORDER_QC_APPROVE`, `DELIVERY_ORDER_WAREHOUSE_APPROVE`, `DELIVERY_ORDER_WAREHOUSE_REJECT`, and `PICKED_GOODS_RETURN_TO_BIN` audit log entries for outbound picking, QC, quarantine, warehouse approval, rejection, and return-to-bin actions.
  * The system SHALL NOT use a `PICKING` Delivery Order status; warehouse staff records picking/QC results while the Delivery Order is in `WAITING_PICKING`.
  * QC result records SHALL be tracked by `doItemId`, `allocationId`, `batchId`, `locationId`, and `zoneId`.
  * The system SHALL validate every QC result row belongs to the Delivery Order warehouse and to an existing planned allocation.
  * Warehouse staff SHALL record picking/QC results exactly once for the complete currently planned allocation set; partial submission is not allowed.
  * When a Delivery Order returns to `WAITING_PICKING` because of replacement planning, warehouse staff SHALL record picking/QC results only for replacement allocations or allocations that are still `PLANNED` and do not yet have QC records.
  * The system SHALL reject duplicate picking/QC submission for any allocation that already has an `outbound_qc_records` row, unless the request uses the same idempotency key and exact same payload as a previously completed request.
  * For every QC result row, `pickedQty`, `qcPassQty`, and `qcFailQty` SHALL be non-negative.
  * For every QC result row, `pickedQty = qcPassQty + qcFailQty`.
  * For every allocation, total recorded `pickedQty` SHALL NOT exceed the allocation's remaining planned quantity.
  * For every Delivery Order item, cumulative QC-passed quantity SHALL NOT exceed the requested quantity.
  * For the submitted plan, total `pickedQty` across all rows for each Delivery Order item SHALL equal the item's currently planned quantity.
  * `qcFailReason` SHALL be required when `qcFailQty > 0`.
  * QC-passed quantity SHALL remain in outbound staging until warehouse manager approval, trip departure, or warehouse rejection.
  * Failed QC quantity SHALL be moved to quarantine, recorded by an inventory adjustment, deducted from valid regular inventory, and excluded from available inventory.
* **Event-driven:**
  * WHEN warehouse staff saves picked and QC quantities for a Delivery Order in `WAITING_PICKING`, the system SHALL:
    * Validate the warehouse staff user is assigned to the Delivery Order warehouse.
    * Store picked quantity, QC pass quantity, and QC fail quantity for each `doItemId` + `allocationId` + `batchId` + `locationId` + `zoneId`.
    * Move QC-passed quantity from the planned batch/bin/location/zone to outbound staging in the same warehouse by decreasing source inventory `total_qty` and `reserved_qty`, then increasing staging inventory `total_qty` and `reserved_qty` for the same product/batch.
    * Move QC-failed quantity from the planned batch/bin/location/zone to quarantine in the same warehouse by decreasing source inventory `total_qty` and `reserved_qty`, then increasing quarantine inventory `total_qty` for the same product/batch with `reserved_qty = 0`.
    * Create a quarantine record and an inventory adjustment record with type `QC_FAIL_OUTBOUND`, negative quantity against the regular source inventory, and references to the Delivery Order, allocation, outbound QC record, and quarantine record for every QC-failed quantity.
    * Decrease regular valid inventory for the failed quantity, remove the failed quantity from the original concrete reservation, keep failed goods out of available inventory, and preserve version checks on every affected inventory row.
    * Keep QC-passed goods reserved for the Delivery Order in outbound staging.
    * Move the Delivery Order to `QC_PENDING_APPROVAL`.
  * WHEN any Delivery Order item has cumulative QC-passed quantity lower than requested quantity because of QC fail, the system SHALL keep the Delivery Order in `QC_PENDING_APPROVAL` so the Storekeeper can review the fail result and select replacement goods, and SHALL block Storekeeper quality approval until replacement goods have been planned, picked, and QC-passed.
  * WHEN the Storekeeper saves replacement allocation for QC fail quantity, the system SHALL move the Delivery Order back to `WAITING_PICKING`.
  * WHEN the Storekeeper approves quality for a `QC_PENDING_APPROVAL` Delivery Order, the system SHALL move the Delivery Order to `QC_COMPLETED` and create an audit log with before/after state and optional notes.
  * WHEN warehouse manager approves a `QC_COMPLETED` Delivery Order, the system SHALL move the Delivery Order to `WAREHOUSE_APPROVED` and create an audit log with before/after state and optional notes.
  * WHEN warehouse manager rejects a `QC_COMPLETED` Delivery Order, the system SHALL:
    * Store the rejection reason.
    * Move QC-passed goods from outbound staging back to their original batch/bin/location/zone.
    * Increase available regular inventory at the original bin/location for returned QC-passed goods.
    * Release the Delivery Order's remaining reserved quantity for returned QC-passed goods.
    * Create `PICKED_GOODS_RETURN_TO_BIN` audit log entries for the returned QC-passed goods.
    * Keep QC-failed goods in quarantine.
    * Move the Delivery Order to `REJECTED` and end the outbound flow.

## 4. API Endpoints

* `PUT /api/v1/delivery-orders/{id}/pick-qc-result` - Nhân viên kho lưu số lượng đã lấy, đạt QC và không đạt QC theo từng Delivery Order item, allocation, batch, location và zone.
* `PUT /api/v1/delivery-orders/{id}/quality-approval` - Thủ kho phê duyệt chất lượng outbound sau khi đủ số lượng đạt QC.
* `PUT /api/v1/delivery-orders/{id}/warehouse-approval` - Trưởng kho phê duyệt xuất kho sau `QC_COMPLETED`.
* `PUT /api/v1/delivery-orders/{id}/warehouse-reject` - Trưởng kho từ chối xuất kho sau `QC_COMPLETED`.

### Pick/QC result request payload

`PUT /api/v1/delivery-orders/{id}/pick-qc-result` SHALL accept:

* `idempotencyKey` - Optional client-generated key for safe retry of the same pick/QC submission after timeout.
* `results[]` - Kết quả lấy hàng/QC theo từng dòng kế hoạch:
  * `doItemId` - Delivery Order item được lấy hàng.
  * `allocationId` - Allocation đã được Thủ kho lập kế hoạch.
  * `batchId` - Batch gốc của allocation.
  * `locationId` - Bin/location gốc của allocation.
  * `zoneId` - Zone chứa bin/location gốc của allocation.
  * `pickedQty` - Số lượng nhân viên kho đã lấy thực tế từ location này.
  * `qcPassQty` - Số lượng đạt QC.
  * `qcFailQty` - Số lượng không đạt QC.
  * `qcFailReason` - Lý do fail QC, bắt buộc khi `qcFailQty > 0`.
  * `stagingLocationId` - Outbound staging location nhận hàng đạt QC trong cùng kho.
  * `quarantineLocationId` - Quarantine location nhận hàng fail QC trong cùng kho, bắt buộc khi `qcFailQty > 0`.
  * `notes` - Ghi chú kiểm tra QC.

Validation rules:

* Mỗi `results[]` row SHALL map đúng `doItemId`, `allocationId`, `batchId`, `locationId`, `zoneId` của Delivery Order hiện tại.
* `batchId`, `locationId`, và `zoneId` trong payload SHALL khớp với batch/location/zone của allocation đã được Thủ kho lập kế hoạch.
* `results[]` SHALL include every allocation that is currently `PLANNED` and has no QC record in the active pick/QC cycle; allocation đã QC pass ở cycle trước và đang nằm tại outbound staging SHALL NOT be submitted again.
* If any allocation in `results[]` already has an `outbound_qc_records` row and the request does not match a previously completed request with the same `idempotencyKey`, the system SHALL reject the request with `QC_RESULT_ALREADY_RECORDED`.
* `pickedQty`, `qcPassQty`, `qcFailQty` SHALL be `>= 0`.
* `pickedQty` SHALL equal `qcPassQty + qcFailQty`.
* Tổng `pickedQty` theo `allocationId` SHALL bằng `plannedQty` của allocation trong lần nhập QC đó; hệ thống không cho nhập một phần rồi bổ sung sau.
* Tổng `qcPassQty` theo `doItemId` SHALL NOT vượt quá `requestedQty` của Delivery Order item.
* `stagingLocationId` SHALL thuộc outbound staging zone trong cùng kho. If the warehouse has exactly one default outbound staging location, the backend MAY resolve it automatically when `stagingLocationId` is omitted.
* `quarantineLocationId` SHALL thuộc quarantine zone trong cùng kho khi có `qcFailQty > 0`. If the warehouse has exactly one default quarantine location, the backend MAY resolve it automatically when `quarantineLocationId` is omitted.
* If `stagingLocationId` or `quarantineLocationId` is provided, the backend SHALL validate the location belongs to the Delivery Order warehouse and has the required zone type before applying inventory movement.

Inventory movement rules:

* For `qcPassQty`, source inventory SHALL decrease `total_qty` and `reserved_qty`; staging inventory for the same product/batch SHALL increase `total_qty` and `reserved_qty`.
* For `qcFailQty`, source inventory SHALL decrease `total_qty` and `reserved_qty`; quarantine inventory for the same product/batch SHALL increase `total_qty` and SHALL keep `reserved_qty = 0`.
* For `qcFailQty`, the system SHALL create an inventory adjustment with type `QC_FAIL_OUTBOUND`, negative quantity against the regular source inventory, and references to Delivery Order, allocation, outbound QC record, and quarantine record.
* Quarantine inventory SHALL NOT be counted as available regular inventory.

Duplicate handling:

* The system SHALL enforce at most one successful `outbound_qc_records` row per allocation per pick/QC cycle.
* If the same `idempotencyKey` and exact same payload are retried after a previous successful request, the system SHALL return the previous successful result without applying inventory movement again.
* If the same `idempotencyKey` is reused with a different payload, the system SHALL reject the request with `IDEMPOTENCY_KEY_CONFLICT`.
* If no `idempotencyKey` is provided and any submitted allocation already has a QC record, the system SHALL reject the request with `QC_RESULT_ALREADY_RECORDED`.

### Quality approval request payload

`PUT /api/v1/delivery-orders/{id}/quality-approval` SHALL accept:

* `notes` - Optional Storekeeper notes for quality approval.

Validation rules:

* Delivery Order SHALL be in `QC_PENDING_APPROVAL`.
* All requested quantities SHALL have QC-passed goods available in outbound staging after any required replacement.
* The system SHALL create `DELIVERY_ORDER_QC_APPROVE` audit log with actor, role, warehouse, before state, after state, and `notes`.

### Warehouse approval request payload

`PUT /api/v1/delivery-orders/{id}/warehouse-approval` SHALL accept:

* `notes` - Optional warehouse manager notes for outbound approval.

Validation rules:

* Delivery Order SHALL be in `QC_COMPLETED`.
* The system SHALL create `DELIVERY_ORDER_WAREHOUSE_APPROVE` audit log with actor, role, warehouse, before state, after state, and `notes`.

### Warehouse reject request payload

`PUT /api/v1/delivery-orders/{id}/warehouse-reject` SHALL accept:

* `reason` - Lý do Trưởng kho từ chối xuất kho.
* `returnToBinRecords[]` - Các dòng hàng đạt QC được trả từ outbound staging về bin gốc:
  * `doItemId` - Delivery Order item được trả hàng.
  * `allocationId` - Allocation gốc của hàng được trả.
  * `batchId` - Batch gốc của hàng được trả.
  * `returnedQty` - Số lượng trả về bin gốc.
  * `sourceLocationId` - Outbound staging location hiện tại.
  * `originalLocationId` - Bin/location gốc cần cộng lại tồn hợp lệ.
  * `originalZoneId` - Zone gốc chứa bin/location cần cộng lại tồn hợp lệ.
  * `reason` - Lý do trả hàng, mặc định theo lý do reject nếu không nhập riêng.

Validation rules:

* `returnToBinRecords[]` SHALL be required when the Delivery Order has QC-passed goods in outbound staging.
* Tổng `returnedQty` theo từng `allocationId` SHALL bằng số lượng QC-passed còn ở outbound staging của allocation đó.
* Tổng `returnedQty` của toàn bộ request SHALL bằng tổng số lượng hàng pass trong outbound staging của Delivery Order.

## 5. Acceptance Criteria

* **Scenario: Warehouse staff records QC result**
  * Given a delivery order is in `WAITING_PICKING` with a saved picking plan
  * And the warehouse staff user is assigned to the delivery order warehouse
  * When warehouse staff records picked, QC pass, and QC fail quantities by `doItemId`, `allocationId`, `batchId`, `locationId`, and `zoneId`
  * Then the system SHALL validate quantities, move QC-passed goods to outbound staging, move failed goods to quarantine, create inventory adjustment records, create QC/quarantine audit logs, deduct failed goods from valid regular inventory, remove failed quantity from concrete reservation, and move the order to `QC_PENDING_APPROVAL`.

* **Scenario: Block invalid QC quantity**
  * Given an allocation has planned quantity 10
  * When warehouse staff submits `pickedQty = 8`, `qcPassQty = 7`, and `qcFailQty = 2`
  * Then the system SHALL reject the request because `pickedQty` must equal `qcPassQty + qcFailQty`.

* **Scenario: Block partial QC submission**
  * Given an allocation has planned quantity 10
  * When warehouse staff submits `pickedQty = 8`, `qcPassQty = 8`, and `qcFailQty = 0`
  * Then the system SHALL reject the request because each planned allocation must be fully picked and QC checked in one submission.

* **Scenario: Block duplicate QC submission**
  * Given allocation A already has a successful outbound QC record
  * When warehouse staff submits another pick/QC result for allocation A without a matching `idempotencyKey`
  * Then the system SHALL reject the request with `QC_RESULT_ALREADY_RECORDED` and SHALL NOT apply inventory movement again.

* **Scenario: Retry same QC submission safely**
  * Given warehouse staff submitted a pick/QC result with `idempotencyKey = K1` and the request succeeded
  * When the same user retries the exact same payload with `idempotencyKey = K1`
  * Then the system SHALL return the previous successful result and SHALL NOT create new QC records or apply inventory movement again.

* **Scenario: Replacement QC submits only new allocations**
  * Given a Delivery Order has QC-passed allocation A already in outbound staging
  * And the Storekeeper created replacement allocation B for failed quantity
  * When warehouse staff submits pick/QC result after the Delivery Order returns to `WAITING_PICKING`
  * Then the request SHALL include allocation B and SHALL NOT require allocation A to be submitted again.

* **Scenario: Block warehouse staff outside assigned warehouse**
  * Given a Delivery Order belongs to warehouse HN
  * And the warehouse staff user is assigned only to warehouse HP
  * When the warehouse staff user submits picking/QC results
  * Then the system SHALL reject the action with `WAREHOUSE_SCOPE_FORBIDDEN`.

* **Scenario: Replacement returns order to waiting picking**
  * Given requested quantity is 10 and warehouse staff records 8 QC pass and 2 QC fail
  * And the Delivery Order is in `QC_PENDING_APPROVAL`
  * When the Storekeeper saves replacement allocation for the 2 failed units
  * Then the system SHALL move the Delivery Order back to `WAITING_PICKING` so warehouse staff can record picking/QC result for the replacement goods.

* **Scenario: Replacement required before quality approval**
  * Given requested quantity is 10 and warehouse staff records 8 QC pass and 2 QC fail
  * When the Storekeeper tries to approve quality before replacement goods pass QC
  * Then the system SHALL reject the action with `QC_REPLACEMENT_REQUIRED`.

* **Scenario: Storekeeper approves quality**
  * Given all requested quantities have QC-passed goods in outbound staging and the Delivery Order is in `QC_PENDING_APPROVAL`
  * When the Storekeeper approves quality
  * Then the system SHALL move the Delivery Order to `QC_COMPLETED`.

* **Scenario: Warehouse manager approves outbound**
  * Given a Delivery Order is in `QC_COMPLETED`
  * When the warehouse manager approves outbound release
  * Then the system SHALL move the Delivery Order to `WAREHOUSE_APPROVED`.

* **Scenario: Warehouse manager rejects outbound**
  * Given a Delivery Order is in `QC_COMPLETED`
  * When the warehouse manager rejects outbound release with a reason
  * Then the system SHALL require total returned quantity to equal all QC-passed goods in outbound staging, move QC-passed goods back to original batch/bin/location/zone, increase available regular inventory, release reservations for returned goods, keep failed goods in quarantine, create `PICKED_GOODS_RETURN_TO_BIN` and `DELIVERY_ORDER_WAREHOUSE_REJECT` audit logs, store the reason, and move the order to `REJECTED`.
