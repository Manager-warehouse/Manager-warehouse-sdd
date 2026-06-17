# Feature: Thủ kho Lập kế hoạch lấy hàng (US-WMS-07A)

## 1. Context and Goal

Thủ kho nhận Delivery Order mới và lập kế hoạch lấy hàng trước khi nhân viên kho thao tác thực tế. Delivery Order giữ trạng thái `NEW` cho đến khi Thủ kho lưu kế hoạch lấy hàng đầu tiên. Hệ thống hiển thị danh sách tồn kho hợp lệ trong kho được gán cho Thủ kho, theo product, batch, bin, zone, và ưu tiên hàng nhập kho sớm hơn lên đầu danh sách theo FIFO. Domain hiện tại là đồ gia dụng không quản lý hạn sử dụng, vì vậy feature này không áp dụng FEFO và không yêu cầu expiry date.

Thủ kho có thể chọn hàng của một dòng Delivery Order từ nhiều bin khác nhau. Tổng số lượng planned cho mỗi dòng hàng bắt buộc bằng số lượng yêu cầu trên phiếu xuất kho trước khi hệ thống chuyển phiếu sang trạng thái chờ nhân viên kho lấy hàng.

## 2. Actors

* **Thủ kho**: Chọn batch/bin/zone từ danh sách FIFO trong kho được gán, lưu hoặc điều chỉnh kế hoạch lấy hàng, và chọn hàng thay thế khi có hàng fail QC.

## 3. Functional Requirements (EARS)

* **Ubiquitous:**
  * The system SHALL allow storekeepers to plan picking only for Delivery Orders belonging to warehouses assigned to their user account.
  * The system SHALL show storekeepers a ranked inventory list by product, batch, bin, and zone for each Delivery Order item.
  * The ranked inventory list SHALL include only valid regular stock inside the selected warehouse's quality-passed storage/picking zones.
  * The ranked inventory list SHALL exclude quarantine, outbound staging, In-Transit, inactive locations, and any inventory row where `total_qty - reserved_qty <= 0`.
  * The ranked inventory list SHALL place oldest received inventory first using FIFO because the current household-goods domain does not track expiry dates.
  * The system SHALL allow a single Delivery Order item to be planned from multiple batch/bin/zone rows.
  * The total planned quantity for each Delivery Order item SHALL equal the requested quantity on that item before the picking plan can be saved.
  * The system SHALL create `PICKING_PLAN_SAVE` audit log entries whenever a storekeeper saves or changes the picking plan.
  * The system SHALL create `PICKED_GOODS_RETURN_TO_BIN` audit log entries whenever picked goods are returned to their original batch/bin/zone as part of a picking-plan change.
  * The system SHALL create `PICKING_REPLACEMENT_SAVE` audit log entries whenever failed QC goods require replacement goods from another batch/bin/zone.
* **Event-driven:**
  * WHEN a Delivery Order is created successfully, the system SHALL keep it in `NEW` status until a storekeeper saves the initial picking plan.
  * WHEN a storekeeper saves the initial picking plan for a Delivery Order in `NEW`, the system SHALL:
    * Validate the storekeeper is assigned to the Delivery Order warehouse.
    * Validate every planned inventory row belongs to the Delivery Order warehouse and is valid regular quality-passed stock.
    * Store one or more planned batch, bin, zone, and quantity allocations for each item.
    * Validate each item's total planned quantity equals its requested quantity.
    * Decrease the matching `warehouse_product_reservations.reserved_qty` and increase `inventories.reserved_qty` for the selected batch/bin/zone rows in the same transaction.
    * Use optimistic locking on all affected `warehouse_product_reservations` and `inventories` rows.
    * Keep the selected concrete inventory quantities reserved.
    * Move the Delivery Order to `WAITING_PICKING`.
  * WHEN a storekeeper changes a picking plan while the Delivery Order is in `WAITING_PICKING`, the system SHALL:
    * Release concrete reservations from removed or reduced allocations.
    * Reserve concrete inventory for added or increased allocations.
    * Keep each Delivery Order item fully planned to its requested quantity.
    * Keep the Delivery Order in `WAITING_PICKING`.
  * WHEN a storekeeper changes a picking plan after warehouse staff has already recorded picked/QC results for the planned batch/bin/zone allocation, the same `PUT /api/v1/delivery-orders/{id}/picking-plan` request SHALL require return-to-bin records in the payload before any new allocation is saved.
  * WHEN only some picked allocations are changed, the system SHALL require `returnToBinRecords` only for the picked allocations being removed or reduced, and SHALL NOT require unchanged allocations to be returned.
  * WHEN a picking-plan change payload includes return-to-bin records, the system SHALL:
    * Validate each returned quantity references the original allocation and does not exceed the picked quantity.
    * Validate goods are returned to the original batch/bin/zone.
    * Move the returned quantity from outbound staging or picked state back to the original inventory row.
    * Create a `PICKED_GOODS_RETURN_TO_BIN` audit log with actor, quantity, product, original allocation, source state/location, original batch/bin/zone, before inventory state, and after inventory state.
    * Apply the revised picking plan only after all required returns are valid and recorded.
  * WHEN QC fail quantity requires replacement and the Delivery Order is in `QC_PENDING_APPROVAL`, the system SHALL allow the storekeeper to select replacement goods from the same FIFO-ranked valid regular inventory list.
  * WHEN the storekeeper saves replacement goods, the system SHALL:
    * Update the Delivery Order item plan with replacement batch/bin/zone details.
    * Store replacement history including failed source, replacement source, quantity, reason, and actor.
    * Reserve the replacement goods as concrete inventory.
    * Move the Delivery Order back to `WAITING_PICKING`.
    * Require warehouse staff to pick and QC the replacement goods before quality approval can complete.
* **State-driven:**
  * WHILE a Delivery Order has recorded picked/QC results, the system SHALL block direct picking-plan changes for changed picked allocations unless the picked goods are first returned to their original locations through a recorded return-to-bin action.

## 4. API Endpoints

* `PUT /api/v1/delivery-orders/{id}/picking-plan` - Thủ kho lưu hoặc điều chỉnh kế hoạch lấy hàng; nếu hàng đã được nhân viên kho ghi nhận lấy/QC, payload phải có `returnToBinRecords` cho các allocation đã pick và bị remove/reduce để ghi nhận trả hàng về đúng vị trí gốc trước khi lưu plan mới.
* `PUT /api/v1/delivery-orders/{id}/replacement-plan` - Thủ kho lưu kế hoạch lấy hàng thay thế cho số lượng fail QC và đưa Delivery Order về `WAITING_PICKING`.

### Picking plan request payload

`PUT /api/v1/delivery-orders/{id}/picking-plan` SHALL accept:

* `allocations[]` - The full revised picking plan:
  * `doItemId` - Delivery Order item being planned.
  * `inventoryId` - Concrete inventory row selected for picking.
  * `batchId` - Batch selected for this allocation.
  * `locationId` - Bin/location selected for this allocation.
  * `zoneId` - Zone containing the selected bin/location.
  * `plannedQty` - Quantity planned from this allocation.
* `returnToBinRecords[]` - Required only for picked allocations that are removed or reduced by the revised plan:
  * `allocationId` - Existing allocation whose picked goods are being returned.
  * `returnedQty` - Quantity returned to the original batch/bin/zone.
  * `sourceLocationId` - Current source location/state before return, such as outbound staging.
  * `reason` - Reason for returning goods and changing the plan.

For each `doItemId`, the sum of `allocations[].plannedQty` SHALL equal the Delivery Order item's requested quantity. If an allocation was already picked but remains unchanged in the revised plan, the request SHALL NOT require a `returnToBinRecords[]` entry for that allocation.

## 5. Acceptance Criteria

* **Scenario: Save initial picking plan from NEW**
  * Given a delivery order in `NEW` status
  * And the storekeeper is assigned to the delivery order warehouse
  * When the storekeeper selects FIFO-ranked valid inventory from one or more bins for each item
  * And each item's total planned quantity equals its requested quantity
  * Then the system SHALL save the picking plan, move reservation from `warehouse_product_reservations` to concrete `inventories.reserved_qty`, create a `PICKING_PLAN_SAVE` audit log, and move the order to `WAITING_PICKING`.

* **Scenario: Block incomplete picking plan**
  * Given a delivery order item has requested quantity 10
  * When the storekeeper plans only 8 units across selected bins
  * Then the system SHALL reject the save because planned quantity must equal requested quantity.

* **Scenario: Block storekeeper outside assigned warehouse**
  * Given a delivery order belongs to warehouse HN
  * And the storekeeper is assigned only to warehouse HP
  * When the storekeeper attempts to save or change the picking plan
  * Then the system SHALL reject the action with `WAREHOUSE_SCOPE_FORBIDDEN`.

* **Scenario: Change plan while waiting picking**
  * Given a delivery order is in `WAITING_PICKING`
  * And warehouse staff has not recorded picked/QC results for the changed allocations
  * When the storekeeper changes selected bins while keeping each item fully planned
  * Then the system SHALL release old concrete reservations, reserve the new concrete inventory rows, create a `PICKING_PLAN_SAVE` audit log, and keep the order in `WAITING_PICKING`.

* **Scenario: Require return to original bin before changing picked goods**
  * Given warehouse staff has already recorded picked/QC results for an allocation
  * And the revised plan removes or reduces that allocation
  * When the storekeeper submits a picking-plan change with valid `returnToBinRecords`
  * Then the system SHALL record the return to the original batch/bin/zone, create a `PICKED_GOODS_RETURN_TO_BIN` audit log, and then save the revised picking plan.

* **Scenario: Block plan change without return records after picking**
  * Given warehouse staff has already recorded picked/QC results for an allocation
  * And the revised plan removes or reduces that allocation
  * When the storekeeper submits a picking-plan change without the required `returnToBinRecords`
  * Then the system SHALL reject the action with `PICKED_GOODS_RETURN_REQUIRED`.

* **Scenario: Keep unchanged picked allocation without return**
  * Given warehouse staff has already recorded picked/QC results for allocation A
  * And allocation A remains unchanged in the revised picking plan
  * And allocation B has not been picked and is changed
  * When the storekeeper submits the revised picking plan
  * Then the system SHALL NOT require a `returnToBinRecords` entry for allocation A.

* **Scenario: Save replacement after QC fail**
  * Given a delivery order has QC fail quantity recorded and failed goods moved to quarantine with an inventory adjustment record
  * When the storekeeper selects replacement goods from another available bin
  * Then the system SHALL update the picking plan, store replacement history, reserve replacement goods, create a `PICKING_REPLACEMENT_SAVE` audit log, move the Delivery Order back to `WAITING_PICKING`, and require the replacement goods to be picked and QC checked.
