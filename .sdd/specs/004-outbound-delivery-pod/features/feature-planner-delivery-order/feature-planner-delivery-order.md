# Feature: Planner Lập đơn xuất hàng & Tự động kiểm tra công nợ (US-WMS-06)

## 1. Context and Goal

Planner tiếp nhận yêu cầu xuất hàng từ Công ty mẹ cho kho mà Planner được gán. Trước khi tạo Delivery Order, hệ thống bắt buộc kiểm tra công nợ đại lý và tồn kho khả dụng tại kho xuất. Nếu công nợ không hợp lệ, Planner không có quyền trên kho, hoặc tồn kho không đủ, hệ thống không tạo phiếu và trả lời rõ lý do cho Planner. Nếu tạo thành công, hệ thống reserve tổng số lượng hàng cần xuất tại kho và tạo Delivery Order ở trạng thái `NEW` để Thủ kho lập danh sách lấy hàng theo vị trí cụ thể trong kho.

## 2. Actors

* **Planner**: Lập Delivery Order từ yêu cầu xuất hàng cho kho được gán và nhận thông báo lỗi nếu credit/stock/warehouse scope không đạt điều kiện.
* **Warehouse Manager**: Là actor duy nhất được hủy Delivery Order trước khi phiếu đã được phê duyệt xuất kho.

## 3. Functional Requirements (EARS)

* **Ubiquitous:**
  * Hệ thống SHALL luôn thực hiện automatic credit check trước khi tạo Delivery Order.
  * Hệ thống SHALL cho phép tạo đơn khi `current_balance + order_value <= credit_limit`, bao gồm trường hợp số dư sau tạo đơn bằng đúng credit limit.
  * IF dealer status là `CREDIT_HOLD`, hệ thống SHALL chặn tạo đơn và hiển thị lý do rõ ràng.
  * Hệ thống SHALL chặn tạo đơn khi `current_balance + order_value > credit_limit`.
  * Hệ thống SHALL chặn tạo đơn khi đại lý có bất kỳ invoice chưa thanh toán nào quá hạn quá số ngày nợ tối đa được cấu hình cho đại lý đó trên một đơn hàng (`dealers.payment_term_days`).
  * Hệ thống SHALL chỉ cho phép Planner tạo Delivery Order mới cho đại lý sau khi toàn bộ invoice quá hạn quá `dealers.payment_term_days` đã được thanh toán hoặc tất toán.
  * Hệ thống SHALL chặn tạo đơn khi Planner không được gán vào kho đã chọn.
  * Hệ thống SHALL reserve số lượng sản phẩm yêu cầu tại kho đã chọn trên Delivery Order items sau khi tạo Delivery Order thành công.
  * Hệ thống SHALL NOT tăng `inventories.reserved_qty` hoặc gán batch, bin, zone cuối cùng khi tạo Delivery Order; Storekeeper SHALL tạo picking list với batch/bin/zone cụ thể và số lượng theo từng vị trí trong feature picking-plan.
  * Hệ thống SHALL duy trì một dòng tổng hợp `warehouse_product_reservations` cho mỗi warehouse/product để theo dõi reservation cấp Planner trước khi Storekeeper gán batch/bin/zone cụ thể.
  * Hệ thống SHALL tính warehouse-level availability từ tồn kho hợp lệ đã đạt chất lượng theo công thức `available_qty = sum(inventories.total_qty - inventories.reserved_qty) - warehouse_product_reservations.reserved_qty` cho cùng warehouse và product.
  * Hệ thống SHALL cập nhật `warehouse_product_reservations.reserved_qty` bằng optimistic locking trong cùng transaction với thao tác tạo và hủy Delivery Order.
  * Hệ thống SHALL giải phóng reservation của Delivery Order item khi Warehouse Manager hủy Delivery Order trước bước phê duyệt xuất kho.
  * Hệ thống SHALL chặn hủy khi Delivery Order đã ở trạng thái `WAREHOUSE_APPROVED` hoặc các trạng thái sau đó.
  * Hệ thống SHALL chặn thao tác hủy từ bất kỳ actor nào không phải Warehouse Manager.
  * Hệ thống SHALL tạo audit log `DELIVERY_ORDER_CREATE` và `DELIVERY_ORDER_CANCEL` cho các thao tác tạo và hủy thành công, bao gồm kết quả credit-check và reservation delta của Delivery Order item.
* **Event-driven:**
  * WHEN Planner tạo Delivery Order, hệ thống SHALL:
    * Validate `available_qty >= requested_qty` bằng tồn kho hợp lệ đã đạt chất lượng tại kho, sau khi trừ `warehouse_product_reservations.reserved_qty`.
    * IF tồn kho không đủ, chặn tạo đơn và hiển thị lý do `INSUFFICIENT_STOCK` hoặc thông báo tương đương rằng tồn kho không đủ.
    * Hệ thống SHALL NOT gợi ý, liệt kê, hoặc đề xuất các kho khác có đủ hàng khi tồn kho tại kho đã chọn không đủ.
    * Với domain hiện tại là hàng gia dụng, áp dụng FIFO bằng cách xếp hạng hàng nhập kho cũ hơn trước hàng nhập kho mới hơn.
    * Không yêu cầu expiry date hoặc FEFO selection vì hàng gia dụng hiện tại như nồi, chảo, đồ nhựa không quản lý hạn sử dụng.
    * Tăng `delivery_order_items.reserved_qty` theo số lượng yêu cầu.
    * Tăng `warehouse_product_reservations.reserved_qty` theo số lượng yêu cầu cho từng cặp warehouse/product.
    * Tạo Delivery Order ở trạng thái `NEW`.
* **State-driven:**
  * WHILE dealer status là `CREDIT_HOLD`, hệ thống SHALL chặn tạo Delivery Order mới cho đại lý đó.
  * WHILE Delivery Order status là `WAREHOUSE_APPROVED` hoặc trạng thái sau đó, hệ thống SHALL chặn hủy qua feature này.

## 4. API Endpoints

* `POST /api/v1/delivery-orders` - Tạo Delivery Order mới sau khi automatic credit check và stock reservation đạt điều kiện.
* `PUT /api/v1/delivery-orders/{id}/cancel` - Warehouse Manager hủy Delivery Order trước bước phê duyệt xuất kho và giải phóng reservation của Delivery Order item.

## 5. Acceptance Criteria

**Scenario 1: Chặn tạo đơn do vượt hạn mức công nợ**
* Given đại lý có `current_balance = 480M` và `credit_limit = 500M`
* When Planner tạo Delivery Order trị giá `30M`
* Then hệ thống SHALL chặn tạo đơn và hiển thị lỗi credit check.

**Scenario 1b: Chặn tạo đơn do invoice quá hạn vượt số ngày nợ tối đa của đại lý**
* Given đại lý có `payment_term_days = N`
* And đại lý có ít nhất một invoice chưa thanh toán quá hạn hơn `N` ngày
* When Planner tạo Delivery Order
* Then hệ thống SHALL chặn tạo đơn và hiển thị lý do invoice quá hạn vượt số ngày nợ tối đa của đại lý.

**Scenario 1c: Cho phép tạo đơn sau khi đại lý thanh toán hết invoice quá hạn**
* Given đại lý từng có invoice quá hạn vượt số ngày nợ tối đa
* And các invoice quá hạn đó đã được thanh toán hoặc tất toán
* And hạn mức công nợ và tồn kho đều hợp lệ
* When Planner tạo Delivery Order
* Then hệ thống SHALL cho phép tạo đơn.

**Scenario 1d: Cho phép tạo đơn khi vừa đúng hạn mức công nợ**
* Given đại lý có `current_balance = 480M` và `credit_limit = 500M`
* When Planner tạo Delivery Order trị giá `20M`
* Then hệ thống SHALL cho phép tạo đơn vì `current_balance + order_value = credit_limit`.

**Scenario 2: Chặn tạo đơn khi thiếu tồn kho tại kho đã chọn**
* Given product X có `total_qty = 100` và `reserved_qty = 30` tại warehouse HP
* When Planner tạo Delivery Order cho `80` đơn vị tại warehouse HP
* Then hệ thống SHALL chặn tạo đơn và hiển thị lý do tồn kho không đủ.
* And hệ thống SHALL NOT gợi ý kho khác có đủ available stock.

**Scenario 2b: Chặn Planner thao tác ngoài kho được gán**
* Given Planner chỉ được gán vào warehouse HP
* When Planner tạo Delivery Order cho warehouse HN
* Then hệ thống SHALL chặn tạo đơn với `WAREHOUSE_SCOPE_FORBIDDEN`.

**Scenario 3: Delivery Order tạo thành công bắt đầu ở trạng thái NEW**
* Given công nợ đại lý hợp lệ và tồn kho yêu cầu còn khả dụng
* When Planner tạo Delivery Order thành công
* Then hệ thống SHALL reserve số lượng sản phẩm yêu cầu trên Delivery Order items và `warehouse_product_reservations`, tạo audit log `DELIVERY_ORDER_CREATE`, và tạo Delivery Order ở trạng thái `NEW` mà không thay đổi `inventories.reserved_qty` hoặc gán batch/bin/zone cuối cùng.

**Scenario 4: Warehouse Manager hủy đơn trước phê duyệt**
* Given Delivery Order chưa ở trạng thái `WAREHOUSE_APPROVED`
* When Warehouse Manager hủy Delivery Order kèm lý do
* Then hệ thống SHALL giải phóng reservation của Delivery Order item, tạo audit log `DELIVERY_ORDER_CANCEL`, và chuyển Delivery Order sang `CANCELLED`.

**Scenario 5: Chặn hủy sau khi đã phê duyệt xuất kho**
* Given Delivery Order đã ở trạng thái `WAREHOUSE_APPROVED`
* When Warehouse Manager hủy Delivery Order
* Then hệ thống SHALL chặn hủy vì Delivery Order đã được phê duyệt xuất kho không thể bị hủy bằng feature này.

**Scenario 6: Chặn actor không phải manager hủy đơn**
* Given Delivery Order chưa ở trạng thái `WAREHOUSE_APPROVED`
* When Planner hoặc Storekeeper hủy Delivery Order
* Then hệ thống SHALL chặn hủy vì chỉ Warehouse Manager được hủy Delivery Order.
