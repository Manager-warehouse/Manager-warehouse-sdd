# TỔNG HỢP FEATURE & QUY TẮC NGHIỆP VỤ (WMS PHÚC ANH)

> **Phiên bản:** v1.0.2
> **Dự án:** Warehouse Management System (WMS) - Công ty Phúc Anh
> **Tài liệu nguồn:** [AGENTS.md](file:///d:/swp/Manager-warehouse-sdd/AGENTS.md) · [constitution.md](file:///d:/swp/Manager-warehouse-sdd/.sdd/constitution.md) · [Userstory.md](file:///d:/swp/Manager-warehouse-sdd/Userstory.md) · [shared_context.md](file:///d:/swp/Manager-warehouse-sdd/.sdd/shared_context.md)

---

## 1. Tổng Quan & Kiến Trúc Dự Án

Hệ thống **WMS Phúc Anh** phục vụ quản lý kho tập trung cho 3 kho vật lý: **Hải Phòng, Hà Nội, TP. Hồ Chí Minh**, 1 kho ảo **In-Transit** phục vụ điều chuyển và khu cách ly **Quarantine Zone** dành cho hàng lỗi QC.

### Tech Stack Bất Di Bất Dịch
* **Backend:** Spring Boot 3.4.5 + Java 21 (Maven)
* **Frontend:** React 18 (Vite, Tailwind CSS 3.x, Zustand)
* **Database & Migration:** PostgreSQL 18 + Flyway
* **ORM:** Spring Data JPA / Hibernate (Sử dụng `@Version` cho Optimistic Locking)
* **Auth:** JWT + bcrypt (cost factor ≥ 12)
* **API Docs:** OpenAPI / Swagger

---

## 2. Tiêu Chuẩn Thiết Kế Spec (Speckit & EARS)

Mỗi tài liệu đặc tả tính năng (Feature Specification) trong hệ thống được cấu trúc đồng bộ gồm **9 thành phần cốt lõi** và sử dụng cú pháp **EARS (Easy Approach to Requirements Syntax)** cho các yêu cầu chức năng.

### 9 Thành Phần Của Mỗi Spec
1. **Context and Goal:** Ngữ cảnh nghiệp vụ và mục tiêu của spec.
2. **Actors:** Các tác nhân tham gia vào quy trình và vai trò cụ thể.
3. **Functional Requirements (EARS):** Yêu cầu chức năng viết theo cú pháp EARS.
4. **Non-functional Requirements:** Yêu cầu phi chức năng (hiệu năng, bảo mật, thời gian phản hồi).
5. **Data Model:** Thiết kế thực thể, trường dữ liệu, khóa ngoại và ràng buộc dữ liệu.
6. **API Spec:** Danh sách REST endpoints cụ thể kèm phương thức HTTP và quyền truy cập.
7. **Error Handling:** Bảng mã lỗi HTTP, mã lỗi hệ thống và điều kiện kích hoạt.
8. **Acceptance Criteria:** Các kịch bản kiểm thử nghiệm thu chi tiết (định dạng Gherkin).
9. **Out of Scope:** Các chức năng bị loại trừ khỏi phạm vi triển khai của spec.

### Quy Tắc Viết Requirements Theo Cú Pháp EARS
* **Ubiquitous (Mọi lúc/Mặc định):** *The system SHALL [behavior].*
* **Event-driven (Khi có sự kiện):** *WHEN [trigger], the system SHALL [behavior].*
* **State-driven (Khi ở trạng thái):** *WHILE [state is active], the system SHALL [behavior].*
* **Optional (Khi có điều kiện tùy chọn):** *WHERE [feature is enabled], the system SHALL [behavior].*

---

## 3. Các Quy Tắc Nghiệp Vụ Cốt Lõi (Domain Rules)

Mọi spec và mã nguồn trong dự án phải tuân thủ tuyệt đối các quy tắc nghiệp vụ bất di bất dịch dưới đây:

### 3.1 Quy Tắc Quản Lý Tồn Kho (Inventory Rules)
1. **Ràng buộc tồn kho không âm:** `inventories.total_qty >= 0` và `total_qty - reserved_qty >= 0` phải luôn đúng trước và sau mọi thao tác. Được kiểm soát bằng DB Constraint (`CHECK (total_qty >= 0)`) và application-level validation.
2. **Quy tắc FEFO (First Expiry First Out):** Đối với sản phẩm có hạn sử dụng (`has_expiry = true`), hệ thống bắt buộc tự động chọn lô hàng (batch) có hạn dùng gần nhất còn hợp lệ khi xuất kho.
3. **Quy tắc FIFO (First In First Out):** Đối với sản phẩm không có hạn sử dụng (`has_expiry = false`), hệ thống bắt buộc tự động chọn lô hàng có ngày nhập (`received_date`) cũ nhất khi xuất kho.
4. **Không cập nhật tồn kho trực tiếp:** Mọi biến động tồn kho phải thông qua các quy trình nghiệp vụ chính thức: nhập kho (receipts), xuất kho (delivery_orders), điều chuyển (transfers), điều chỉnh (adjustments) hoặc kiểm kê (stock_takes). Tuyệt đối không thực hiện sửa đổi trực tiếp trường số lượng tồn kho trên thực thể Inventory.
5. **Khóa lạc quan (Optimistic Locking):** Mọi thao tác cập nhật tồn kho phải sử dụng cơ chế `@Version` trong bảng `inventories` để ngăn ngừa ghi đè dữ liệu cạnh tranh. Nếu xảy ra xung đột, hệ thống trả về lỗi `HTTP 409 Conflict` và thực hiện retry.
6. **Số lượng khả dụng:** Số lượng hàng khả dụng để bán được tính theo công thức: `available = total_qty - reserved_qty`. Số lượng này phải luôn `≥ 0`. Hệ thống phải kiểm tra số lượng khả dụng trước khi xuất kho.

### 3.2 Quy Tắc Lô Hàng & Kệ Lưu Trữ (Batch & Bin Rules)
1. **Lô hàng đơn cấp chất lượng:** Mỗi lô hàng (batches) chỉ chứa đúng 1 loại chất lượng sản phẩm (Grade A hoặc B hoặc C). Hàng hóa có chất lượng khác nhau phải được chia vào các batches khác nhau.
2. **Truy vết mã Serial:** Đối với sản phẩm được cấu hình `has_serial = true`, nhân viên bắt buộc phải nhập mã serial chi tiết cho từng đơn vị sản phẩm khi thực hiện cả hai quy trình Nhập kho và Xuất kho.
3. **Sức chứa của Kệ (Bin Capacity):** Quy trình cất hàng vào kệ (Putaway) bắt buộc phải kiểm tra thể tích tối đa (`capacity_m3`) và khối lượng tối đa (`capacity_kg`) của vị trí kệ (`warehouse_locations`). Hệ thống sẽ chặn và cảnh báo nếu số lượng hàng mới vượt quá sức chứa còn lại.
4. **Lô hàng hết hạn:** Các lô hàng đã hết hạn sử dụng (`expiry_date < current_date`) bắt buộc phải bị loại trừ khỏi quy trình xuất kho thông thường. Hàng hết hạn chỉ có thể xuất thông qua luồng xử lý/tiêu hủy đặc biệt được phê duyệt.

### 3.3 Quy Tắc QC & Cách Ly (QC & Quarantine Rules)
1. **Cổng kiểm soát chất lượng bắt buộc:** Hàng nhập kho phải đi qua cổng QC Inbound. Hàng xuất kho phải đi qua QC Outbound trước khi giao hàng cho tài xế.
2. **Khu vực cách ly (Quarantine Zone):** Tất cả hàng hóa không đạt tiêu chuẩn chất lượng (Fail QC) hoặc hàng hoàn trả bị lỗi phải được đưa vào các vị trí cách ly (`warehouse_locations.is_quarantine = true`). Tồn kho trong khu vực cách ly không được tính vào tồn kho khả dụng để bán và bị chặn hoàn toàn khỏi các luồng xuất thông thường.

### 3.4 Quy Tắc Điều Chuyển Nội Bộ (Transfer Rules)
1. **Kho ảo In-Transit:** Quy trình điều chuyển hàng giữa các kho vật lý bắt buộc phải đi qua trạng thái trung gian là kho ảo **In-Transit**. Tồn kho tại kho nguồn giảm → tồn kho In-Transit tăng → xe di chuyển → kho đích xác nhận nhận hàng → tồn kho In-Transit giảm → tồn kho khả dụng tại kho đích tăng.
2. **Xử lý chênh lệch điều chuyển:** Bất kỳ chênh lệch nào giữa số lượng xuất đi (`sent_qty`) và số lượng nhận thực tế (`received_qty`) tại kho đích phải được ghi nhận lý do rõ ràng (`discrepancy_reason`), tạo biên bản chênh lệch và tự động tạo phiếu điều chỉnh (`adjustments` type = 'TRANSFER_DISCREPANCY') hoặc Audit trail tương ứng.

### 3.5 Quy Tắc Phê Duyệt Phân Hạn Mức (Approval Threshold Rules)
Đối với các thao tác điều chỉnh tồn kho (do kiểm kê lệch hoặc tiêu hủy hàng lỗi từ khu cách ly), hệ thống áp dụng bảng định mức phê duyệt động Maker-Checker:
* **Giá trị lệch < 5,000,000 VND:** Hệ thống tự động duyệt nếu có xác nhận từ QC hoặc biên bản kiểm kê.
* **Giá trị lệch từ 5,000,000 VND đến 100,000,000 VND:** Yêu cầu **Trưởng kho** phê duyệt.
* **Giá trị lệch > 100,000,000 VND** hoặc nguyên nhân do lỗi cá nhân nhân viên: Yêu cầu **CEO** phê duyệt.

### 3.6 Quy Tắc Xóa Mềm (Soft Delete Rules)
* **Dữ liệu danh mục (Master Data):** Không xóa vật lý bản ghi. Sử dụng thuộc tính `is_active = false` để ẩn danh mục (Product, Warehouse, warehouse_locations, Dealer, Supplier, Vehicle, Driver). Hệ thống chặn việc tạo giao dịch mới liên quan đến danh mục bị vô hiệu hóa nhưng giữ nguyên lịch sử giao dịch cũ.
* **Dữ liệu giao dịch (Transaction Data):** Không xóa vật lý chứng từ. Sử dụng trạng thái chứng từ thích hợp hoặc hủy bỏ thông qua trạng thái (ví dụ: `status = cancelled` cho `delivery_orders`, `transfers`, `stock_takes`, `invoices`, và `receipts`).

---

## 4. Danh Sách 10 Domain Specs & 80 Features Chi Tiết

Dưới đây là tổng hợp 10 Domain Specifications chứa 26 User Stories gốc được phân rã chi tiết:

### Spec 001: Xác thực, Phân quyền & Hoạt động (Auth & RBAC)
* **Mã Spec:** [001-security-auth-rbac-audit](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/001-security-auth-rbac-audit/spec.md)
* **Mục tiêu:** Xây dựng nền tảng quản trị phân quyền (RBAC) theo Vai trò (Role) và phạm vi chi nhánh Kho vật lý gán cho nhân viên; lưu vết Audit Log cho mọi thay đổi hệ thống.
* **Các User Stories:**
  * **US-WMS-01 (P1):** [Cấu hình Tham số Hệ thống & Định mức Phê duyệt động (Maker-Checker)](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/001-security-auth-rbac-audit/features/feature-admin-system-config.md).
  * **US-WMS-21 (P1):** [Phân quyền người dùng theo Chi nhánh Kho và Vai trò (RBAC)](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/001-security-auth-rbac-audit/features/feature-admin-auth-rbac.md). Hệ thống chặn nhân viên kho truy cập báo cáo tài chính và chặn nhân viên kho này can thiệp kho khác. (Xem thêm: [Xác thực Người dùng](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/001-security-auth-rbac-audit/features/feature-user-auth.md))
  * **Audit Log (P1):** [Nhật ký Hoạt động (Audit Log)](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/001-security-auth-rbac-audit/features/feature-system-audit-logging.md). Tự động ghi lại log mọi thao tác CREATE, UPDATE, DELETE, APPROVE, REJECT, CANCEL trên thực thể nghiệp vụ (ai làm, làm gì, trước/sau thay đổi, thời gian).
* **Actors:** System Admin, CEO, Mọi người dùng.
* **Endpoints chính:** `/api/v1/auth/login`, `/api/v1/admin/users`, `/api/v1/admin/system-config`, `/api/v1/audit-logs`.

### Spec 002: Danh Mục Nền Tảng (Master Data)
* **Mã Spec:** [002-master-data-management](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/002-master-data-management/spec.md)
* **Mục tiêu:** Quản lý tập trung các thực thể nền tảng làm xương sống cho toàn bộ quy trình kho bãi và logistics.
* **Các User Stories:**
  * **US-WMS-19 (P1):** [Quản lý SKU và danh mục sản phẩm](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/002-master-data-management/features/feature-admin-products.md) (hỗ trợ quy đổi đơn vị Thùng → Cái, quản lý thuộc tính `has_serial`, `has_expiry`).
  * **US-WMS-20 (P2):** [Cấu hình Vị trí kho](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/002-master-data-management/features/feature-admin-warehouses.md) (Zone → Bin) và kiểm tra sức chứa Bin (m3/kg) khi Putaway (`warehouse_locations`).
  * **US-WMS-22 (P1):** [Quản lý Danh mục Đối tác](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/002-master-data-management/features/feature-accountant-partners.md) (Đại lý & Nhà cung cấp). Kế toán trưởng thiết lập Credit Limit cho từng Đại lý.
  * **US-WMS-23 (P2):** [Quản lý Danh mục Xe tải & Tài xế Nội bộ](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/002-master-data-management/features/feature-dispatcher-fleet-drivers.md) (xe: Rảnh/Đang đi chuyến/Bảo trì; tài xế: Rảnh/Đang đi chuyến/Không khả dụng).
* **Actors:** Planner, Kế toán viên, Kế toán trưởng, System Admin, Dispatcher, Thủ kho kiêm QC, Trưởng kho.
* **Endpoints chính:** `/api/v1/products`, `/api/v1/warehouses`, `/api/v1/bin-locations`, `/api/v1/dealers`, `/api/v1/suppliers`, `/api/v1/vehicles`, `/api/v1/drivers`.

### Spec 003: Nhập Hàng & QC Inbound
* **Mã Spec:** [003-inbound-receipt-qc](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/003-inbound-receipt-qc/spec.md)
* **Mục tiêu:** Quản lý toàn bộ quy trình nhập kho từ tiếp nhận lệnh nhập thô đến kiểm hàng thực tế, thực hiện QC kiểm chất lượng và duyệt nhập chính thức để tăng tồn kho khả dụng.
* **Các User Stories:**
  * **US-WMS-02 (P1):** [Lập Lệnh nhập kho thủ công](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/003-inbound-receipt-qc/features/feature-planner-receipt-drafting.md) từ nguồn Email/Zalo (Trạng thái ban đầu: `Pending Receipt`).
  * **US-WMS-03 (P1):** [Đếm hàng thực tế](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/003-inbound-receipt-qc/features/feature-storekeeper-receipt-receive.md) và [Kiểm QC Inbound](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/003-inbound-receipt-qc/features/feature-qc-inbound-inspection.md) (Phân loại Đạt → cất vào warehouse_locations đạt chuẩn; Lỗi → bắt buộc chuyển sang Quarantine Zone).
  * **US-WMS-04 (P1 - Sub-flow):** [Phê duyệt hàng lỗi trong Quarantine Zone](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/003-inbound-receipt-qc/features/feature-manager-quarantine-handling.md) (Nếu chọn Trả NCC → trừ quarantine, thông báo Kế toán lập Debit Note; Nếu chọn Tiêu hủy → áp dụng định mức phê duyệt).
  * **US-WMS-05 (P1):** [Duyệt nhập kho](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/003-inbound-receipt-qc/features/feature-manager-receipt-approval.md) (Trưởng kho đối chiếu kết quả QC và ký duyệt Phiếu nhập kho để tăng tồn kho khả dụng thực tế).
* **Actors:** Planner, Thủ kho kiêm QC, Nhân viên kho, Trưởng kho, Kế toán viên.
* **Endpoints chính:** `/api/v1/receipts`, `/api/v1/receipts/{id}/qc`, `/api/v1/receipts/{id}/approve`, `/api/v1/receipts/{id}/rtv`, `/api/v1/receipts/{id}/dispose`.

### Spec 004: Xuất Hàng & Giao Hàng (Outbound)
* **Mã Spec:** [004-outbound-delivery-pod](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/spec.md)
* **Mục tiêu:** Quản lý quy trình bán hàng xuất kho tích hợp kiểm tra công nợ Đại lý tự động, giữ chỗ (reserve) tồn kho khả dụng, soạn hàng, kiểm QC Outbound đóng gói, gom chuyến xe và ký nhận giao hàng POD.
* **Các User Stories:**
  * **US-WMS-06 (P1):** [Tiếp nhận yêu cầu & Lập Đơn xuất hàng](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-planner-delivery-order.md) (Delivery Order). Hệ thống tự động Credit Check (Chặn nếu nợ quá hạn >30 ngày hoặc vượt Credit Limit) và tự động giữ chỗ (Reserve) tồn kho khả dụng. Giải phóng Reserved khi đơn bị hủy hoặc chuyển sang trạng thái In-Transit.
  * **US-WMS-07 (P1):** [Soạn hàng tại kệ (Picking)](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-storekeeper-picking.md) & [Kiểm QC đóng gói](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-qc-outbound-inspection.md) (đúng SKU, số lượng, đóng thùng chống sốc).
  * **US-WMS-08 (P1):** [Lập Chuyến xe (Trip Log)](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-dispatcher-trip-dispatch.md) nội bộ, gán tài xế, sắp xếp Stop Order. Trừ tồn kho vật lý tại thời điểm xe xuất phát rời kho (status đổi sang In-Transit).
  * **US-WMS-09 (P1):** [Giao diện Web di động cho Tài xế](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-driver-mobile-pod.md) để xem chuyến xe và xác nhận giao hàng bằng POD (ký nhận trực tiếp + chụp ảnh hàng hóa bàn giao + ghi timestamp). Nếu giao thất bại, tự động chuyển hàng hoàn vào Quarantine Zone.
  * **US-WMS-10 (P1):** [Thông báo Kế toán](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/004-outbound-delivery-pod/features/feature-accountant-billing-notification.md) để lập hóa đơn bán hàng ngay khi đơn chuyển trạng thái sang `Delivered`.
* **Actors:** Planner, Thủ kho kiêm QC, Nhân viên kho, Dispatcher, Tài xế, Kế toán viên, Kế toán trưởng, Trưởng kho.
* **Endpoints chính:** `/api/v1/delivery-orders`, `/api/v1/delivery-orders/{id}/pick`, `/api/v1/delivery-orders/{id}/qc-outbound`, `/api/v1/trips`, `/api/v1/trips/{id}/depart`, `/api/v1/trips/{id}/confirm-delivery`.

### Spec 005: Điều Chuyển Kho Nội Bộ
* **Mã Spec:** [005-inter-warehouse-transfer](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/005-inter-warehouse-transfer/spec.md)
* **Mục tiêu:** Cân bằng nguồn hàng giữa 3 kho miền thông qua kho ảo In-Transit và đội xe nội bộ Phúc Anh.
* **Các User Stories:**
  * **US-WMS-11 (P2):** [Planning Dashboard](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/005-inter-warehouse-transfer/features/feature-planner-transfer-planning.md) gợi ý tự động các lệnh điều chuyển tối ưu dựa trên định mức tồn kho tối thiểu.
  * **US-WMS-12 (P1):** [Lập, Duyệt và Xác nhận Phiếu Điều chuyển Kho](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/005-inter-warehouse-transfer/features/feature-storekeeper-transfer-ship.md). Hàng xuất phát → giảm kho nguồn, tăng kho ảo In-Transit. Kho đích nhận hàng → giảm In-Transit, tăng kho đích. Nếu có chênh lệch, bắt buộc ghi lý do và tự động tạo phiếu điều chỉnh. (Xem thêm: [Thủ kho Đích Nhận hàng](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/005-inter-warehouse-transfer/features/feature-storekeeper-transfer-receive.md))
* **Actors:** Planner, Trưởng kho (Kho nguồn), Thủ kho (Kho nguồn), Trưởng kho (Kho đích), Nhân viên kho, Tài xế.
* **Endpoints chính:** `/api/v1/transfers`, `/api/v1/transfers/{id}/approve`, `/api/v1/transfers/{id}/ship`, `/api/v1/transfers/{id}/receive`, `/api/v1/planning/suggestions`.

### Spec 006: Kiểm Kê & Điều Chỉnh Tồn Kho
* **Mã Spec:** [006-stocktake-adjustment](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/006-stocktake-adjustment/spec.md)
* **Mục tiêu:** Quy trình đối chiếu và điều chỉnh số liệu hệ thống khớp với số đếm thực tế của thủ kho định kỳ.
* **Các User Stories:**
  * **US-WMS-13 (P1):** [Kiểm kê định kỳ](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/006-stocktake-adjustment/features/feature-storekeeper-stocktake-count.md): Lập phiếu kiểm kê, khóa sổ kệ tạm thời, nhập số lượng đếm thực tế, tự động tính chênh lệch (Variance). Khi được duyệt, cập nhật tồn kho hệ thống và ghi Audit Log. (Xem thêm: [Duyệt chênh lệch kiểm kê](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/006-stocktake-adjustment/features/feature-manager-stocktake-approval.md))
* **Actors:** Thủ kho, Trưởng kho, CEO.
* **Endpoints chính:** `/api/v1/stocktakes`, `/api/v1/stocktakes/{id}/start`, `/api/v1/stocktakes/{id}/count`, `/api/v1/stocktakes/{id}/complete`, `/api/v1/stocktakes/{id}/approve`.

### Spec 007: Bảng Giá & Giá Vốn (Pricing & COGS)
* **Mã Spec:** [007-pricing-cogs-management](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/007-pricing-cogs-management/spec.md)
* **Mục tiêu:** Quản lý bảng giá bán và giá vốn theo kỳ kinh doanh hiệu lực để tính giá trị giao dịch và COGS chính xác.
* **Các User Stories:**
  * **US-WMS-14 (P1):** [Quản lý bảng giá theo kỳ](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/007-pricing-cogs-management/features/feature-accountant-pricing-management.md) (Maker-Checker: Kế toán viên lập, Kế toán trưởng duyệt). Hệ thống bắt buộc lưu lịch sử giá vào `price_history`. Khi xuất hàng, hệ thống tự động tra cứu giá vốn/bán. (Xem thêm: [Tự động tính COGS](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/007-pricing-cogs-management/features/feature-system-cogs-calculation.md))
* **Actors:** Kế toán viên, Kế toán trưởng.
* **Endpoints chính:** `/api/v1/price-lists`, `/api/v1/price-lists/{id}/approve`, `/api/v1/products/{id}/price-history`.

### Spec 008: Tài Chính & Công Nợ Đại Lý
* **Mã Spec:** [008-finance-billing-closing](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/008-finance-billing-closing/spec.md)
* **Mục tiêu:** Quản lý toàn bộ hóa đơn bán hàng, ghi nhận thanh toán, cấn trừ công nợ, phân tích nợ quá hạn và thực hiện chốt sổ tháng.
* **Các User Stories:**
  * **US-WMS-10 (P1):** [Lập Hóa đơn bán hàng](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/008-finance-billing-closing/features/feature-accountant-customer-invoicing.md) từ đơn Delivered kèm kỳ hạn thanh toán (Net 30 / Net 60), tự động cộng dồn công nợ Đại lý.
  * **US-WMS-15 (P1):** [Ghi nhận Thanh toán & Quản lý vòng đời công nợ](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/008-finance-billing-closing/features/feature-accountant-payment-collection.md) (Kế toán viên tạo phiếu thu `payment_receipts` cấn trừ hóa đơn). Tự động mở khóa tín dụng chuyển Đại lý về `ACTIVE` nếu `current_balance < credit_limit * 0.8`.
  * **US-WMS-16 (P1):** [Báo cáo Công nợ Phân kỳ (Aging Report)](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/008-finance-billing-closing/features/feature-accountant-credit-aging-report.md) theo các mốc: trong hạn, quá hạn 1-30 ngày, 31-60 ngày, >60 ngày.
  * **US-WMS-17 (P1):** [Chốt sổ Kế toán](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/008-finance-billing-closing/features/feature-accountant-period-closing.md) & Khóa cứng kỳ quá khứ (Kế toán trưởng thực hiện hàng tháng trên `accounting_periods`).
* **Actors:** Kế toán viên, Kế toán trưởng, Hệ thống (Daily Batch Job).
* **Endpoints chính:** `/api/v1/invoices`, `/api/v1/payments`, `/api/v1/credit/aging-report`, `/api/v1/accounting/periods/{period}/close`.

### Spec 009: Hàng Hoàn Trả & Tiêu Hủy
* **Mã Spec:** [009-returns-scrap-disposal](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/009-returns-scrap-disposal/spec.md)
* **Mục tiêu:** Quy trình tiếp nhận hàng trả về từ đại lý và thanh lý, tiêu hủy hàng hỏng trong khu cách ly.
* **Các User Stories:**
  * **US-WMS-24 (P2):** Xử lý hàng hoàn trả từ Đại lý (Inbound Returns). Thủ kho lập phiếu nhận hàng hoàn (`receipts` type = 'RETURN') → Thủ kho kiểm QC chất lượng (Hàng tốt → nhập lại kho thường; Hàng lỗi → chuyển Quarantine Zone). Kế toán tạo Credit Note tương ứng để trừ công nợ cho Đại lý. [Chi tiết](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/009-returns-scrap-disposal/features/feature-storekeeper-customer-returns.md)
  * **US-WMS-04 (P1 - Disposal sub-flow):** Tiêu hủy hàng lỗi từ Quarantine Zone. Áp dụng bảng định mức phê duyệt: <5tr tự động, 5-100tr Trưởng kho duyệt, >100tr CEO duyệt (tạo `adjustments` type = 'DISPOSAL' và `damage_reports`). Được duyệt thì giảm tồn quarantine, ghi biên bản hủy. [Chi tiết](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/009-returns-scrap-disposal/features/feature-manager-scrap-disposal.md)
* **Actors:** Thủ kho kiêm QC, Nhân viên kho, Trưởng kho, Kế toán viên, CEO.
* **Endpoints chính:** `/api/v1/returns`, `/api/v1/returns/{id}/qc`, `/api/v1/returns/{id}/credit-note`, `/api/v1/disposal`, `/api/v1/disposal/{id}/approve`.

### Spec 010: Báo Cáo & Cảnh Báo (Reporting & Alerts)
* **Mã Spec:** [010-reports-dashboards-alerts](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/010-reports-dashboards-alerts/spec.md)
* **Mục tiêu:** Tổng hợp thông tin quản trị thời gian thực cho CEO và đưa ra cảnh báo tồn kho thấp cho ban vận hành.
* **Các User Stories:**
  * **US-WMS-18 (P1):** Dashboard báo cáo quản trị cấp cao cho CEO & Kế toán trưởng: tổng giá trị tồn kho 3 miền theo giá vốn, Top đại lý nợ nhiều nhất, báo cáo Lãi/Lỗ (P&L = Doanh thu - COGS - Chi phí vận hành), tỷ lệ QC lỗi, tỷ lệ giao hàng đúng hạn (OTD). Bắt buộc ghi log mỗi lượt xem báo cáo vào `audit_logs`. [Chi tiết](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/010-reports-dashboards-alerts/features/feature-ceo-management-dashboard.md)
  * **US-WMS-26 (P1):** Cảnh báo tự động tồn kho dưới định mức tối thiểu. Hệ thống bắn thông báo High Priority in-app cho Trưởng kho và Planner; đánh dấu đỏ sản phẩm thiếu hụt trên dashboard (lưu tại bảng `stock_alerts`). [Chi tiết](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/010-reports-dashboards-alerts/features/feature-manager-low-stock-alerts.md)
  * **US-WMS-25 (P3):** Báo cáo năng suất & sản lượng nhân viên kho (số đơn bốc xếp/di chuyển của nhân viên, số đơn soạn và QC của thủ kho, số chuyến của tài xế) để làm căn cứ HRM tính lương sản phẩm. [Chi tiết](file:///d:/swp/Manager-warehouse-sdd/.sdd/specs/010-reports-dashboards-alerts/features/feature-manager-productivity-report.md)
* **Actors:** CEO, Kế toán trưởng, Trưởng kho, Planner.
* **Endpoints chính:** `/api/v1/dashboard/ceo`, `/api/v1/reports/inventory-valuation`, `/api/v1/reports/productivity`, `/api/v1/alerts/low-stock`.

---

## 5. Phạm Vi Out of Scope Tổng Thể Dự Án (Sprint 1)

Nhằm đảm bảo sự tập trung vào các nghiệp vụ cốt lõi, các thành phần sau được xác định nằm ngoài phạm vi triển khai của Sprint 1:
1. **SSO / OAuth2:** Chưa tích hợp hệ thống xác thực tập trung bên ngoài; sử dụng JWT nội bộ.
2. **IP Whitelisting & 2FA:** Không áp dụng giới hạn IP truy cập hoặc xác thực 2 lớp.
3. **Mã Barcode / QR Code:** Chưa tích hợp máy quét hoặc camera quét mã vị trí kệ/sản phẩm (nhập liệu thủ công trên giao diện).
4. **Logistics 3PL:** Không quản lý chi phí vận chuyển bên thứ ba; toàn bộ quy trình giao hàng sử dụng đội xe nội bộ Phúc Anh.
5. **Cổng B2B / B2C:** Không xây dựng cổng thông tin cho đại lý tự đặt hàng trực tuyến; mọi đơn xuất hàng được Planner tiếp nhận và nhập thủ công.
6. **Định tuyến giao hàng thời gian thực:** Không có chức năng GPS tracking trực tuyến của tài xế; chuyến xe chỉ quản lý trạng thái tĩnh và vị trí POD tại điểm giao.
7. **Sổ cái General Ledger (GL):** Hệ thống không thực hiện kế toán kép đầy đủ, không báo cáo thuế VAT, chỉ quản lý công nợ đại lý, hóa đơn, giá vốn hàng bán và doanh thu bán hàng nội bộ.
8. **Thuật toán tự động xếp kệ (Putaway Slotting):** Putaway dựa trên sức chứa thô của Bin, hệ thống kiểm tra và gợi ý thủ công thay vì sử dụng thuật toán tối ưu hóa vị trí lưu trữ thông minh.
