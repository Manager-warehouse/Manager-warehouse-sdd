# Backend Module Refactor Plan

Tài liệu này ghi lại kế hoạch làm sạch cấu trúc backend WMS theo hướng ít rủi ro nhất: vẫn giữ layered architecture quen thuộc, nhưng chia nhỏ file theo module nghiệp vụ bên trong từng layer.

Nói ngắn gọn: không dùng kiểu `feature/masterdata/product/controller`. Cấu trúc đúng của dự án này là `controller/product_catalog`, `service/product_catalog`, `dto/request/product_catalog`, `dto/response/product_catalog`, `mapper/supplier_management`...

## Vấn Đề Hiện Tại

Backend hiện đang tổ chức chủ yếu theo layer lớn:

| Layer hiện tại | Vấn đề khi dự án lớn lên |
| --- | --- |
| `controller` | Tất cả controller nằm chung một chỗ, khó nhìn nhanh module nào thuộc nghiệp vụ nào |
| `service` | Service interface và implementation dễ bị trộn giữa nhiều phân hệ |
| `dto/request` | Request DTO của product, supplier, dealer, driver... bị gom chung |
| `dto/response` | Response DTO nhiều nghiệp vụ nằm lẫn nhau |
| `mapper` | Mapper của từng module chưa được nhóm rõ |
| `repository` | Repository của module nhỏ có thể chia theo module, nhưng cần cẩn thận vì nhiều service nghiệp vụ dùng chéo |

Layered architecture vẫn đúng và vẫn phải giữ: `Controller -> Service -> Repository -> Entity`. Vấn đề cần sửa chỉ là cách nhóm file bên trong từng layer.

## Mục Tiêu Cấu Trúc Mới

Cấu trúc mục tiêu:

```text
com.wms
├── WmsApplication.java
├── controller
│   ├── product_catalog
│   ├── supplier_management
│   ├── dealer_management
│   ├── driver_management
│   ├── fleet_management
│   ├── warehouse_location
│   ├── stock_control
│   ├── stock_receiving
│   ├── order_fulfillment
│   ├── warehouse_transfer
│   ├── stock_counting
│   ├── billing_payment
│   └── reporting_alerting
├── service
│   ├── product_catalog
│   │   └── impl
│   ├── supplier_management
│   │   └── impl
│   ├── dealer_management
│   │   └── impl
│   ├── driver_management
│   │   └── impl
│   └── ...
├── dto
│   ├── request
│   │   ├── product_catalog
│   │   ├── supplier_management
│   │   ├── dealer_management
│   │   ├── driver_management
│   │   └── ...
│   └── response
│       ├── product_catalog
│       ├── supplier_management
│       ├── dealer_management
│       ├── driver_management
│       └── ...
├── mapper
│   ├── product_catalog
│   ├── supplier_management
│   ├── dealer_management
│   └── ...
├── repository
│   ├── product_catalog
│   ├── supplier_management
│   ├── dealer_management
│   ├── driver_management
│   └── ...
├── entity
│   ├── product_catalog
│   ├── stock_control
│   ├── stock_receiving
│   └── ...
├── enums
│   ├── access_control
│   ├── stock_control
│   ├── order_fulfillment
│   └── ...
├── exception
├── config
├── security
└── util
```

Không bắt buộc module nào cũng phải có đủ tất cả thư mục. Module nhỏ chỉ cần các phần đang dùng.

## Quy Ước Quan Trọng

- Giữ thư mục chính theo layer: `controller`, `service`, `dto`, `mapper`, `repository`, `entity`.
- Chỉ chia module bên trong layer chính.
- `service/<module>/impl` chứa implementation.
- `impl` không phải module riêng; nó là phần implementation nằm bên trong cùng module nghiệp vụ.
- `dto/request/<module>` chứa request DTO của module đó.
- `dto/response/<module>` chứa response DTO của module đó.
- `mapper/<module>` chứa mapper của module đó.
- `repository/<module>` chứa repository chính của module đó, ví dụ `repository/product_catalog/ProductRepository`.
- `entity/<module>` chứa entity theo nghiệp vụ chính của bảng.
- `enums/<module>` chứa enum theo nghiệp vụ sử dụng chính.
- Tên module phải nói được nghiệp vụ/hành động, tránh quá chung chung như `finance`, `driver`, `inventory`.

## Bản Đồ Module Đề Xuất

| Module | Package mục tiêu |
| --- | --- |
| Product Catalog | `controller/product_catalog`, `service/product_catalog`, `service/product_catalog/impl`, `repository/product_catalog`, `dto/request/product_catalog`, `dto/response/product_catalog`, `entity/product_catalog` |
| Supplier Management | `controller/supplier_management`, `service/supplier_management`, `service/supplier_management/impl`, `repository/supplier_management`, `dto/request/supplier_management`, `dto/response/supplier_management`, `mapper/supplier_management`, `entity/supplier_management` |
| Dealer Management | `controller/dealer_management`, `service/dealer_management`, `service/dealer_management/impl`, `repository/dealer_management`, `dto/request/dealer_management`, `dto/response/dealer_management`, `mapper/dealer_management`, `entity/dealer_management` |
| Driver Management | `controller/driver_management`, `service/driver_management`, `service/driver_management/impl`, `repository/driver_management`, `dto/request/driver_management`, `dto/response/driver_management`, `entity/driver_management` |
| Fleet Management | `controller/fleet_management`, `service/fleet_management`, `service/fleet_management/impl`, `entity/fleet_management` |
| Warehouse Location | `controller/warehouse_location`, `service/warehouse_location`, `service/warehouse_location/impl`, `entity/warehouse_location` |
| Stock Control | `controller/stock_control`, `service/stock_control`, `service/stock_control/impl`, `entity/stock_control`, `enums/stock_control` |
| Stock Receiving | `controller/stock_receiving`, `service/stock_receiving`, `entity/stock_receiving`, `enums/stock_receiving` |
| Order Fulfillment | `controller/order_fulfillment`, `service/order_fulfillment`, `service/order_fulfillment/impl`, `entity/order_fulfillment`, `enums/order_fulfillment` |
| Warehouse Transfer | `controller/warehouse_transfer`, `service/warehouse_transfer`, `service/warehouse_transfer/impl`, `repository/warehouse_transfer`, `dto/request/warehouse_transfer`, `dto/response/warehouse_transfer` |
| Stock Counting | `controller/stock_counting`, `service/stock_counting`, `entity/stock_counting`, `enums/stock_counting` |
| Billing Payment | `controller/billing_payment`, `service/billing_payment`, `service/billing_payment/impl`, `entity/billing_payment`, `enums/billing_payment` |
| Price Management | `controller/price_management`, `service/price_management`, `service/price_management/impl`, `entity/price_management`, `enums/price_management` |
| Return Disposal | `controller/return_disposal`, `service/return_disposal` |
| Reporting Alerting | `controller/reporting_alerting`, `service/reporting_alerting`, `service/reporting_alerting/impl` |
| Notification Delivery | `controller/notification_delivery`, `service/notification_delivery`, `service/notification_delivery/impl`, `entity/notification_delivery`, `enums/notification_delivery` |
| Access Control | `controller/access_control`, `service/access_control`, `entity/access_control`, `enums/access_control` |
| User Configuration | `controller/user_configuration`, `service/user_configuration`, `service/user_configuration/impl`, `entity/user_configuration`, `enums/user_configuration` |
| Audit Trail | `controller/audit_trail`, `service/audit_trail`, `entity/audit_trail`, `enums/audit_trail` |

## Lộ Trình Refactor An Toàn

### Phase 0 — Chuẩn bị

- Giữ nguyên behavior/API endpoint.
- Không đổi tên class public nếu không cần.
- Không sửa logic nghiệp vụ trong cùng bước move package.
- Chạy GitNexus impact analysis trước khi di chuyển từng nhóm symbol nếu tooling khả dụng.
- Sau mỗi module chạy backend compile, targeted tests và frontend build.

### Phase 1 — Module ít rủi ro

Di chuyển nhóm CRUD/master data đơn giản trước:

1. `product_catalog`
2. `supplier_management`
3. `dealer_management`
4. `driver_management`
5. `fleet_management`
6. `warehouse_location`

Lý do: các module này tương đối độc lập hơn inbound/outbound/warehouse_transfer/inventory, nên phù hợp để tạo pattern chuẩn.

### Phase 2 — Shared và cross-cutting

Sau khi module nhỏ ổn định, mới xử lý nhóm dùng chung:

1. `user_context`
2. `audit_trail`
3. `exception`
4. `config`
5. `document_numbering`

Nhóm này có nhiều dependency chéo nên phải làm cẩn thận hơn.

### Phase 3 — Core warehouse operations

Di chuyển từng nghiệp vụ kho chính:

1. `stock_control`
2. `stock_receiving`
3. `stock_counting`
4. `warehouse_transfer`
5. `order_fulfillment`

Các phase này phải chạy test kỹ vì liên quan invariant tồn kho, FIFO, QC, reservation, warehouse transfer in-transit và audit log.

### Phase 4 — Finance, returns, reporting

Di chuyển các module còn lại:

1. `price_management`
2. `billing_payment`
3. `return_disposal`
4. `reporting_alerting`

## Quy Tắc Khi Di Chuyển File

- Mỗi bước chỉ nên đổi một module hoặc một nhóm nhỏ.
- Không thay đổi endpoint URL.
- Không thay đổi request/response schema nếu mục tiêu chỉ là dọn package.
- Test package trong `backend/src/test/java` phải di chuyển tương ứng với package main.
- Sau khi move phải kiểm tra:
  - import compile được;
  - endpoint giữ nguyên URL;
  - Spring component scan vẫn thấy bean;
  - test liên quan pass;
  - frontend service vẫn gọi đúng endpoint;
  - frontend build pass;
  - GitNexus detect changes chỉ ra đúng scope mong muốn nếu tooling khả dụng.

## Checklist Xác Nhận Frontend ↔ Backend Sau Mỗi Module

Sau mỗi bước refactor backend, phải kiểm tra tối thiểu:

1. Backend compile:

   ```bash
   JAVA_HOME=/Users/haison/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -q -DskipTests compile
   ```

2. Backend targeted tests cho module vừa refactor:

   ```bash
   JAVA_HOME=/Users/haison/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -q -Dtest=<ModuleTests> test
   ```

3. Backend full tests nếu thay đổi không quá lớn:

   ```bash
   JAVA_HOME=/Users/haison/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -q test
   ```

4. Rà frontend service/page đang gọi endpoint của module đó.

5. Frontend build:

   ```bash
   cd frontend
   npm run build
   ```

Nếu module có thay đổi URL, request DTO hoặc response DTO thì phải test thêm luồng UI tương ứng, không chỉ build.

## Module Đã Áp Dụng Pattern

Các module đầu tiên đang được đưa về đúng pattern layer-first:

- `product_catalog`
- `supplier_management`
- `dealer_management`
- `driver_management`

Pattern đúng:

```text
controller/<module>
service/<module>
service/<module>/impl
dto/request/<module>
dto/response/<module>
mapper/<module>
repository/<module>
```

Không dùng:

```text
feature/masterdata/<module>/controller
feature/masterdata/<module>/service
```
