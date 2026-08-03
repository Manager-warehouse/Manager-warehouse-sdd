# Feature: Planner Lập đơn xuất hàng & Tự động kiểm tra công nợ (US-WMS-06)

## Change Note: Planner update/cancel before Storekeeper planning

- Planner may update Delivery Order header fields and item quantities only while the Delivery Order status is `NEW`.
- Planner may cancel a Delivery Order only while the Delivery Order status is `NEW`.
- `NEW` means the order has been created but Storekeeper has not saved the first picking plan yet.
- After Storekeeper saves the first picking plan and the order moves to `WAITING_PICKING`, Planner update and Planner cancel are blocked.
- Planner update must re-run credit, overdue invoice, warehouse scope, product, price, accounting period, and selected-warehouse stock validation.
- Planner create and update must reject a Delivery Order whose total goods weight exceeds the combined maximum payload of every active vehicle assigned to the selected warehouse, regardless of whether those vehicles are ready, busy, on trip, or under maintenance.
- Planner update must apply planner-level reservation deltas in one transaction and must not assign batch, bin, zone, or concrete inventory rows.
- Planner cancel must release planner-level reservations, persist cancel reason, write `DELIVERY_ORDER_CANCEL`, and move the Delivery Order to `CANCELLED`.
- Successful Planner update must write `DELIVERY_ORDER_UPDATE` with before/after state and reservation deltas.
- This note supersedes older wording that blocked Planner cancellation in every state; Storekeeper cancellation remains blocked.

## 1. Context and Goal

Planner tiếp nhận yêu cầu xuất hàng từ Công ty mẹ cho kho mà Planner được gán. Trước khi tạo Delivery Order, hệ thống bắt buộc kiểm tra công nợ đại lý và tồn kho khả dụng tại kho xuất. Nếu công nợ không hợp lệ, Planner không có quyền trên kho, hoặc tồn kho không đủ, hệ thống không tạo phiếu và trả lời rõ lý do cho Planner. Nếu tạo thành công, hệ thống reserve tổng số lượng hàng cần xuất tại kho và tạo Delivery Order ở trạng thái `NEW` để Thủ kho lập danh sách lấy hàng theo vị trí cụ thể trong kho.

Planner update/cancel extension: While the Delivery Order is still `NEW` and no Storekeeper picking plan exists, Planner may correct the order details or cancel the order. Once Storekeeper saves the first picking plan and the Delivery Order leaves `NEW`, Planner update and Planner cancel are no longer allowed.

Để bảo đảm một Delivery Order có thể được giao trong một đợt điều phối đồng thời, hệ thống kiểm tra tổng trọng lượng hàng của đơn với tổng tải trọng tối đa của toàn bộ xe active thuộc kho đã chọn. Phép tính sức chứa đội xe bao gồm cả xe đang sẵn sàng, đang bận, đang chạy chuyến hoặc đang bảo trì; trạng thái hiện tại chỉ ảnh hưởng thời điểm lập kế hoạch, không làm thay đổi giới hạn năng lực tối đa của kho. Nếu đơn vượt giới hạn này, Planner phải chia nhu cầu thành nhiều Delivery Order/phiếu xuất kho nhỏ hơn.

## 2. Actors

- **Planner**: Lập Delivery Order từ yêu cầu xuất hàng cho kho được gán và nhận thông báo lỗi nếu credit/stock/warehouse scope không đạt điều kiện.
- **Warehouse Manager**: Là actor duy nhất được hủy Delivery Order trước khi phiếu đã được phê duyệt xuất kho.

Additional Planner permission for this feature:

- **Planner update/cancel scope**: Planner may update or cancel a Delivery Order only while the order is `NEW`, before Storekeeper saves the first picking plan.

## 3. Functional Requirements (EARS)

- **Ubiquitous:**
  - Hệ thống SHALL luôn thực hiện automatic credit check trước khi tạo Delivery Order.
  - Hệ thống SHALL cho phép tạo đơn khi `current_balance + order_value <= credit_limit`, bao gồm trường hợp số dư sau tạo đơn bằng đúng credit limit.
  - IF dealer status là `CREDIT_HOLD`, hệ thống SHALL chặn tạo đơn và hiển thị lý do rõ ràng.
  - Hệ thống SHALL chặn tạo đơn khi `current_balance + order_value > credit_limit`.
  - Hệ thống SHALL chặn tạo đơn khi đại lý có bất kỳ invoice chưa thanh toán nào quá hạn quá số ngày nợ tối đa được cấu hình cho đại lý đó trên một đơn hàng (`dealers.payment_term_days`).
  - Hệ thống SHALL chỉ cho phép Planner tạo Delivery Order mới cho đại lý sau khi toàn bộ invoice quá hạn quá `dealers.payment_term_days` đã được thanh toán hoặc tất toán.
  - Hệ thống SHALL chặn tạo đơn khi Planner không được gán vào kho đã chọn.
  - Hệ thống SHALL tính `delivery_order_total_weight_kg = sum(requested_qty * products.weight_kg)` từ toàn bộ dòng hàng của Delivery Order.
  - Hệ thống SHALL tính `warehouse_fleet_max_payload_kg = sum(vehicles.max_weight_kg)` từ toàn bộ Vehicle còn active và được gán cho kho đã chọn, không phụ thuộc trạng thái hiện tại của xe, bao gồm xe sẵn sàng, đang bận, đang chạy chuyến và đang bảo trì.
  - Hệ thống SHALL loại khỏi tổng tải trọng đội xe các Vehicle đã inactive hoặc không thuộc kho đã chọn.
  - Hệ thống SHALL chặn tạo Delivery Order khi `delivery_order_total_weight_kg > warehouse_fleet_max_payload_kg`; không tạo đơn, không tạo reservation và không ghi audit thành công.
  - Khi chặn do vượt tổng tải trọng đội xe, hệ thống SHALL hiển thị thông báo: `Tải trọng quá lớn để giao trong 1 lần, vui lòng chia nhỏ đơn thành nhiều phiếu xuất kho để có thể giao hàng.`
  - Hệ thống SHALL chặn tạo hoặc cập nhật Delivery Order với `PRODUCT_WEIGHT_MISSING` nếu bất kỳ sản phẩm nào trong đơn chưa có `weight_kg` hợp lệ; hệ thống SHALL NOT xem trọng lượng thiếu là `0`.
  - Hệ thống SHALL reserve số lượng sản phẩm yêu cầu tại kho đã chọn trên Delivery Order items sau khi tạo Delivery Order thành công.
  - Hệ thống SHALL NOT tăng `inventories.reserved_qty` hoặc gán batch, bin, zone cuối cùng khi tạo Delivery Order; Storekeeper SHALL tạo picking list với batch/bin/zone cụ thể và số lượng theo từng vị trí trong feature picking-plan.
  - Hệ thống SHALL duy trì một dòng tổng hợp `warehouse_product_reservations` cho mỗi warehouse/product để theo dõi reservation cấp Planner trước khi Storekeeper gán batch/bin/zone cụ thể.
  - Hệ thống SHALL tính warehouse-level availability từ tồn kho hợp lệ đã đạt chất lượng theo công thức `available_qty = sum(inventories.total_qty - inventories.reserved_qty) - warehouse_product_reservations.reserved_qty` cho cùng warehouse và product.
  - Hệ thống SHALL cập nhật `warehouse_product_reservations.reserved_qty` bằng optimistic locking trong cùng transaction với thao tác tạo và hủy Delivery Order.
  - Hệ thống SHALL giải phóng reservation của Delivery Order item khi Warehouse Manager hủy Delivery Order trước bước phê duyệt xuất kho.
  - Hệ thống SHALL chặn hủy khi Delivery Order đã ở trạng thái `WAREHOUSE_APPROVED` hoặc các trạng thái sau đó.
  - Hệ thống SHALL chặn thao tác hủy từ bất kỳ actor nào không phải Warehouse Manager.
  - Hệ thống SHALL tạo audit log `DELIVERY_ORDER_CREATE` và `DELIVERY_ORDER_CANCEL` cho các thao tác tạo và hủy thành công, bao gồm kết quả credit-check và reservation delta của Delivery Order item.
- **Event-driven:**
  - WHEN Planner tạo Delivery Order, hệ thống SHALL:
    - Validate `available_qty >= requested_qty` bằng tồn kho hợp lệ đã đạt chất lượng tại kho, sau khi trừ `warehouse_product_reservations.reserved_qty`.
    - IF tồn kho không đủ, chặn tạo đơn và hiển thị lý do `INSUFFICIENT_STOCK` hoặc thông báo tương đương rằng tồn kho không đủ.
    - Validate tổng trọng lượng đơn không vượt `warehouse_fleet_max_payload_kg` trước khi tạo Delivery Order hoặc reservation.
    - Hệ thống SHALL NOT gợi ý, liệt kê, hoặc đề xuất các kho khác có đủ hàng khi tồn kho tại kho đã chọn không đủ.
    - Với domain hiện tại là hàng gia dụng, danh sách tồn kho được xếp hàng nhập kho cũ hơn trước hàng nhập kho mới hơn để hiển thị; thứ tự này không bắt buộc Thủ kho phải xuất lô cũ trước.
    - Không yêu cầu expiry date hoặc FEFO selection vì hàng gia dụng hiện tại như nồi, chảo, đồ nhựa không quản lý hạn sử dụng.
    - Tăng `delivery_order_items.reserved_qty` theo số lượng yêu cầu.
    - Tăng `warehouse_product_reservations.reserved_qty` theo số lượng yêu cầu cho từng cặp warehouse/product.
    - Tạo Delivery Order ở trạng thái `NEW`.
- **State-driven:**
  - WHILE dealer status là `CREDIT_HOLD`, hệ thống SHALL chặn tạo Delivery Order mới cho đại lý đó.
  - WHILE Delivery Order status là `WAREHOUSE_APPROVED` hoặc trạng thái sau đó, hệ thống SHALL chặn hủy qua feature này.

### Planner update/cancel before picking plan

- The system SHALL allow Planner to update Delivery Order header fields and item quantities only while the Delivery Order status is `NEW`.
- The system SHALL block Planner update when the Delivery Order status is not `NEW`, including `WAITING_PICKING` after Storekeeper saves the first picking plan.
- The system SHALL re-run dealer credit, overdue invoice, warehouse scope, product, price, accounting period, and selected-warehouse stock validation before saving a Planner update.
- The system SHALL re-run the warehouse fleet maximum-payload validation before saving a Planner update and SHALL reject an update that would make the Delivery Order exceed the combined payload of all active vehicles assigned to the warehouse.
- The Planner create/update form SHALL display `Tổng trọng lượng đơn hàng` and `Tổng tải trọng tất cả xe trong kho` as read-only values that cannot be entered or overridden manually.
- The displayed order weight SHALL update from `sum(requested_qty * product.weight_kg)` whenever Planner changes a product or quantity.
- The displayed warehouse fleet payload SHALL include all active vehicles assigned to the selected warehouse regardless of operational status, and SHALL exclude inactive vehicles and vehicles assigned to other warehouses.
- The system SHALL apply planner-level reservation deltas for Planner update in the same transaction: release quantities removed from the old order, reserve quantities added by the new order, and keep `warehouse_product_reservations.reserved_qty >= 0`.
- The system SHALL NOT assign batch, bin, zone, or concrete inventory rows during Planner update.
- The system SHALL allow Planner to cancel a Delivery Order only while the Delivery Order status is `NEW`.
- The system SHALL release planner-level reservation quantities when Planner cancels a `NEW` Delivery Order.
- The system SHALL block Planner cancel when the Delivery Order status is not `NEW`.
- The system SHALL create `DELIVERY_ORDER_UPDATE` audit for successful Planner update and `DELIVERY_ORDER_CANCEL` audit for successful Planner cancel, including before/after state and reservation deltas.

## 4. API Endpoints

- `POST /api/v1/delivery-orders` - Tạo Delivery Order mới sau khi automatic credit check và stock reservation đạt điều kiện.
- `PUT /api/v1/delivery-orders/{id}/cancel` - Warehouse Manager hủy Delivery Order trước bước phê duyệt xuất kho và giải phóng reservation của Delivery Order item.

Additional Planner endpoint:

- `PUT /api/v1/delivery-orders/{id}` - Planner update Delivery Order when the order is still `NEW`; the system re-runs credit/stock checks and applies planner-level reservation deltas.

### Exception Handling

- `400 VALIDATION_ERROR` or `INVALID_REQUEST_BODY` when update/cancel payload is malformed or required fields are missing.
- `400 INVALID_DELIVERY_DATE` when expected delivery date is before document date.
- `403 WAREHOUSE_SCOPE_FORBIDDEN` when actor has no allowed role or is not assigned to the Delivery Order warehouse.
- `404 RESOURCE_NOT_FOUND` when the Delivery Order or referenced master data does not exist.
- `409 INVENTORY_VERSION_CONFLICT` or `WAREHOUSE_PRODUCT_RESERVATION_CONFLICT` when reservation/inventory rows changed concurrently.
- `409 RESERVATION_NOT_FOUND` when cancellation cannot find the planner-level reservation row that must be released.
- `422 DELIVERY_ORDER_UPDATE_FORBIDDEN` when Planner updates a Delivery Order after it leaves `NEW`.
- `422 DELIVERY_ORDER_CANCEL_FORBIDDEN` when Planner cancels after `NEW` or Warehouse Manager cancels after warehouse approval.
- `422 PICKED_GOODS_RETURN_REQUIRED` when picked/QC-processed goods must complete return-to-bin before cancellation.
- `422 CREDIT_HOLD`, `MISSING_PRICE`, `PERIOD_CLOSED`, or `INSUFFICIENT_STOCK` when Planner update revalidation fails.
- `422 DELIVERY_ORDER_EXCEEDS_WAREHOUSE_FLEET_CAPACITY` when the total goods weight is greater than the combined `max_weight_kg` of all active vehicles assigned to the selected warehouse, regardless of their current operational status.
- `422 PRODUCT_WEIGHT_MISSING` when at least one requested product has no valid positive `weight_kg`, so fleet-capacity validation cannot be completed safely.

## 5. Acceptance Criteria

**Scenario 1: Chặn tạo đơn do vượt hạn mức công nợ**

- Given đại lý có `current_balance = 480M` và `credit_limit = 500M`
- When Planner tạo Delivery Order trị giá `30M`
- Then hệ thống SHALL chặn tạo đơn và hiển thị lỗi credit check.

**Scenario 1b: Chặn tạo đơn do invoice quá hạn vượt số ngày nợ tối đa của đại lý**

- Given đại lý có `payment_term_days = N`
- And đại lý có ít nhất một invoice chưa thanh toán quá hạn hơn `N` ngày
- When Planner tạo Delivery Order
- Then hệ thống SHALL chặn tạo đơn và hiển thị lý do invoice quá hạn vượt số ngày nợ tối đa của đại lý.

**Scenario 1c: Cho phép tạo đơn sau khi đại lý thanh toán hết invoice quá hạn**

- Given đại lý từng có invoice quá hạn vượt số ngày nợ tối đa
- And các invoice quá hạn đó đã được thanh toán hoặc tất toán
- And hạn mức công nợ và tồn kho đều hợp lệ
- When Planner tạo Delivery Order
- Then hệ thống SHALL cho phép tạo đơn.

**Scenario 1d: Cho phép tạo đơn khi vừa đúng hạn mức công nợ**

- Given đại lý có `current_balance = 480M` và `credit_limit = 500M`
- When Planner tạo Delivery Order trị giá `20M`
- Then hệ thống SHALL cho phép tạo đơn vì `current_balance + order_value = credit_limit`.

**Scenario 2: Chặn tạo đơn khi thiếu tồn kho tại kho đã chọn**

- Given product X có `total_qty = 100` và `reserved_qty = 30` tại warehouse HP
- When Planner tạo Delivery Order cho `80` đơn vị tại warehouse HP
- Then hệ thống SHALL chặn tạo đơn và hiển thị lý do tồn kho không đủ.
- And hệ thống SHALL NOT gợi ý kho khác có đủ available stock.

**Scenario 2b: Chặn Planner thao tác ngoài kho được gán**

- Given Planner chỉ được gán vào warehouse HP
- When Planner tạo Delivery Order cho warehouse HN
- Then hệ thống SHALL chặn tạo đơn với `WAREHOUSE_SCOPE_FORBIDDEN`.

**Scenario 2c: Chặn tạo đơn khi tổng hàng vượt tải trọng của toàn bộ đội xe trong kho**

- Given warehouse HP có các Vehicle active với tổng `max_weight_kg = 12,000 kg`, bao gồm cả xe đang sẵn sàng, đang bận và đang bảo trì
- And Delivery Order có `delivery_order_total_weight_kg = 12,001 kg`
- When Planner tạo Delivery Order cho warehouse HP
- Then hệ thống SHALL chặn tạo đơn với `DELIVERY_ORDER_EXCEEDS_WAREHOUSE_FLEET_CAPACITY`.
- And hệ thống SHALL hiển thị `Tải trọng quá lớn để giao trong 1 lần, vui lòng chia nhỏ đơn thành nhiều phiếu xuất kho để có thể giao hàng.`
- And hệ thống SHALL NOT tạo Delivery Order, reservation hoặc audit thành công.

**Scenario 2d: Cho phép đơn bằng đúng tổng tải trọng đội xe**

- Given warehouse HP có các Vehicle active với tổng `max_weight_kg = 12,000 kg`
- And Delivery Order có `delivery_order_total_weight_kg = 12,000 kg`
- And công nợ, tồn kho và các điều kiện tạo đơn khác đều hợp lệ
- When Planner tạo Delivery Order cho warehouse HP
- Then hệ thống SHALL cho phép tạo đơn vì tổng trọng lượng không vượt tổng tải trọng đội xe.

**Scenario 2e: Không cho phép thiếu trọng lượng sản phẩm làm sai kiểm tra tải trọng**

- Given ít nhất một sản phẩm trong Delivery Order chưa có `weight_kg` hợp lệ
- When Planner tạo hoặc cập nhật Delivery Order
- Then hệ thống SHALL chặn thao tác với `PRODUCT_WEIGHT_MISSING` và SHALL NOT xem trọng lượng sản phẩm đó là `0`.

**Scenario 2f: Hiển thị hai tổng trọng lượng chỉ đọc trên form Planner**

- Given Planner đang tạo hoặc cập nhật Delivery Order tại warehouse HP
- When Planner chọn sản phẩm hoặc thay đổi số lượng
- Then UI SHALL tự động hiển thị tổng trọng lượng đơn hàng theo đơn vị kg.
- And UI SHALL hiển thị tổng `max_weight_kg` của tất cả Vehicle active thuộc warehouse HP, không phụ thuộc trạng thái vận hành.
- And cả hai giá trị SHALL là chỉ đọc và Planner SHALL NOT được nhập hoặc sửa thủ công.

**Scenario 3: Delivery Order tạo thành công bắt đầu ở trạng thái NEW**

- Given công nợ đại lý hợp lệ và tồn kho yêu cầu còn khả dụng
- When Planner tạo Delivery Order thành công
- Then hệ thống SHALL reserve số lượng sản phẩm yêu cầu trên Delivery Order items và `warehouse_product_reservations`, tạo audit log `DELIVERY_ORDER_CREATE`, và tạo Delivery Order ở trạng thái `NEW` mà không thay đổi `inventories.reserved_qty` hoặc gán batch/bin/zone cuối cùng.

**Scenario 4: Warehouse Manager hủy đơn trước phê duyệt**

- Given Delivery Order chưa ở trạng thái `WAREHOUSE_APPROVED`
- When Warehouse Manager hủy Delivery Order kèm lý do
- Then hệ thống SHALL giải phóng reservation của Delivery Order item, tạo audit log `DELIVERY_ORDER_CANCEL`, và chuyển Delivery Order sang `CANCELLED`.

**Scenario 5: Chặn hủy sau khi đã phê duyệt xuất kho**

- Given Delivery Order đã ở trạng thái `WAREHOUSE_APPROVED`
- When Warehouse Manager hủy Delivery Order
- Then hệ thống SHALL chặn hủy vì Delivery Order đã được phê duyệt xuất kho không thể bị hủy bằng feature này.

**Scenario 6: Block unauthorized cancellation actor**

- Given Delivery Order chưa ở trạng thái `WAREHOUSE_APPROVED`
- When Storekeeper cancels Delivery Order through this feature
- Then the system SHALL reject cancellation because Storekeeper is not a valid cancellation actor in this feature.
- When Planner cancels Delivery Order that is not `NEW`
- Then the system SHALL reject cancellation because Planner cancellation is allowed only before Storekeeper saves the first picking plan.
