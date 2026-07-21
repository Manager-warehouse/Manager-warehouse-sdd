# TỔNG HỢP FEATURE & QUY TẮC NGHIỆP VỤ (WMS PHÚC ANH)

> **Phiên bản:** v1.1.0
> **Dự án:** Warehouse Management System (WMS) - Công ty Phúc Anh
> **Đồng bộ lần cuối:** 2026-07-15
> **Nguồn chuẩn:** các `spec.md` trong [`.sdd/specs/`](.sdd/specs/), sau đó là [constitution](.specify/memory/constitution.md). Tài liệu này là bản tổng hợp, không thay thế spec chi tiết.

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
2. **Quy tắc FIFO (First In First Out):** Với domain hàng gia dụng Sprint 1, hệ thống bắt buộc tự động chọn lô hàng có ngày nhập (`received_date`) cũ nhất khi xuất kho.
3. **Không quản lý hạn sử dụng:** Sản phẩm và batch hàng gia dụng không yêu cầu hạn dùng hoặc chọn lô theo hạn dùng.
4. **Không cập nhật tồn kho trực tiếp:** Mọi biến động tồn kho phải thông qua các quy trình nghiệp vụ chính thức: nhập kho (receipts), xuất kho (delivery_orders), điều chuyển (transfers), điều chỉnh (adjustments) hoặc kiểm kê (stock_takes). Tuyệt đối không thực hiện sửa đổi trực tiếp trường số lượng tồn kho trên thực thể Inventory.
5. **Khóa lạc quan (Optimistic Locking):** Mọi thao tác cập nhật tồn kho phải sử dụng cơ chế `@Version` trong bảng `inventories` để ngăn ngừa ghi đè dữ liệu cạnh tranh. Nếu xảy ra xung đột, hệ thống trả về lỗi `HTTP 409 Conflict` và thực hiện retry.
6. **Số lượng khả dụng:** Số lượng hàng khả dụng để bán được tính theo công thức: `available = total_qty - reserved_qty`. Số lượng này phải luôn `≥ 0`. Hệ thống phải kiểm tra số lượng khả dụng trước khi xuất kho.

### 3.2 Quy Tắc Lô Hàng & Kệ Lưu Trữ (Batch & Bin Rules)
1. **Lô hàng không phân cấp chất lượng:** Batch chỉ gom theo SKU, nguồn nhập/chứng từ và ngày nhận; không tách hàng gia dụng thành các cấp chất lượng để bán lại.
2. **Không truy vết serial từng sản phẩm:** Đơn hàng lớn trong domain hàng gia dụng chỉ quản lý theo SKU, số lượng, batch/ngày nhận và vị trí kho.
3. **Sức chứa của Kệ (Bin Capacity):** Quy trình cất hàng vào kệ (Putaway) bắt buộc phải kiểm tra thể tích tối đa (`capacity_m3`) và khối lượng tối đa (`capacity_kg`) của vị trí kệ (`warehouse_locations`). Hệ thống sẽ chặn và cảnh báo nếu số lượng hàng mới vượt quá sức chứa còn lại.
4. **Hàng lỗi QC:** Hàng lỗi không được phân loại lại thành cấp chất lượng khác; phải vào Quarantine và xử lý trả NCC hoặc tiêu hủy theo luồng phê duyệt.

### 3.3 Quy Tắc QC & Cách Ly (QC & Quarantine Rules)
1. **Cổng kiểm soát chất lượng bắt buộc:** Hàng nhập kho phải đi qua cổng QC Inbound. Hàng xuất kho phải đi qua QC Outbound trước khi giao hàng cho tài xế.
2. **Khu vực cách ly (Quarantine Zone):** Tất cả hàng hóa không đạt tiêu chuẩn chất lượng (Fail QC) hoặc hàng hoàn trả bị lỗi phải được đưa vào các vị trí cách ly (`warehouse_locations.is_quarantine = true`). Tồn kho trong khu vực cách ly không được tính vào tồn kho khả dụng để bán và bị chặn hoàn toàn khỏi các luồng xuất thông thường.

### 3.4 Quy Tắc Điều Chuyển Nội Bộ (Transfer Rules)
1. **Kho ảo In-Transit:** Quy trình điều chuyển hàng giữa các kho vật lý bắt buộc phải đi qua trạng thái trung gian là kho ảo **In-Transit**. Tồn kho tại kho nguồn giảm → tồn kho In-Transit tăng → xe di chuyển → kho đích xác nhận nhận hàng → tồn kho In-Transit giảm → tồn kho khả dụng tại kho đích tăng.
2. **Xử lý chênh lệch điều chuyển:** Bất kỳ chênh lệch nào giữa số lượng xuất đi (`sent_qty`) và số lượng nhận thực tế (`received_qty`) tại kho đích phải được ghi nhận lý do rõ ràng (`discrepancy_reason`), tạo biên bản chênh lệch và tự động tạo phiếu điều chỉnh (`adjustments` type = 'TRANSFER_DISCREPANCY') hoặc Audit trail tương ứng.

### 3.5 Quy Tắc Phê Duyệt Điều Chỉnh Tồn Kho (Approval Rules)
Đối với các thao tác điều chỉnh tồn kho (do kiểm kê lệch hoặc tiêu hủy hàng lỗi từ khu cách ly), hệ thống áp dụng cơ chế Maker-Checker: Thủ kho đếm/đề xuất, **Trưởng kho** phê duyệt hoặc từ chối trực tiếp trên hệ thống — không phân cấp theo giá trị chênh lệch.

### 3.6 Quy Tắc Xóa Mềm (Soft Delete Rules)
* **Dữ liệu danh mục (Master Data):** Không xóa vật lý bản ghi. Sử dụng thuộc tính `is_active = false` để ẩn danh mục (Product, Warehouse, warehouse_locations, Dealer, Supplier, Vehicle, Driver). Hệ thống chặn việc tạo giao dịch mới liên quan đến danh mục bị vô hiệu hóa nhưng giữ nguyên lịch sử giao dịch cũ.
* **Dữ liệu giao dịch (Transaction Data):** Không xóa vật lý chứng từ. Sử dụng trạng thái chứng từ thích hợp hoặc hủy bỏ thông qua trạng thái (ví dụ: `status = cancelled` cho `delivery_orders`, `transfers`, `stock_takes`, `invoices`, và `receipts`).

---

## 4. Danh Sách 12 Specs Hiện Hành

Dưới đây là tổng hợp 10 spec nghiệp vụ (001–010) và 2 spec chất lượng kỹ thuật (011–012). Mã/tên feature và acceptance criteria phải tra cứu tại từng `spec.md` vì một số feature có artifact con độc lập.

### Spec 001: Xác thực, Phân quyền & Hoạt động (Auth & RBAC)
* **Mã Spec:** [001-security-auth-rbac-audit](.sdd/specs/001-security-auth-rbac-audit/spec.md)
* **Mục tiêu:** Xây dựng nền tảng quản trị phân quyền (RBAC) theo Vai trò (Role) và phạm vi chi nhánh Kho vật lý gán cho nhân viên; lưu vết Audit Log cho mọi thay đổi hệ thống.
* **Các User Stories:**
  * **US-WMS-01 (P1):** [Cấu hình Tham số Hệ thống & Định mức Phê duyệt động (Maker-Checker)](.sdd/specs/001-security-auth-rbac-audit/features/feature-admin-system-config.md).
  * **US-WMS-21 (P1):** [Phân quyền người dùng theo Chi nhánh Kho và Vai trò (RBAC)](.sdd/specs/001-security-auth-rbac-audit/features/feature-admin-auth-rbac.md). Hệ thống chặn nhân viên kho truy cập báo cáo tài chính và chặn nhân viên kho này can thiệp kho khác. (Xem thêm: [Xác thực Người dùng](.sdd/specs/001-security-auth-rbac-audit/features/feature-user-auth.md))
  * **Audit Log (P1):** [Nhật ký Hoạt động (Audit Log)](.sdd/specs/001-security-auth-rbac-audit/features/001-audit-logging/feature-system-audit-logging.md). Tự động ghi lại log mọi thao tác CREATE, UPDATE, DELETE, APPROVE, REJECT, CANCEL trên thực thể nghiệp vụ (ai làm, làm gì, trước/sau thay đổi, thời gian).
* **Actors:** System Admin, CEO, Mọi người dùng.
* **Endpoints chính:** `/api/v1/auth/login`, `/api/v1/admin/users`, `/api/v1/admin/system-config`, `/api/v1/audit-logs`.

### Spec 002: Danh Mục Nền Tảng (Master Data)
* **Mã Spec:** [002-master-data-management](.sdd/specs/002-master-data-management/spec.md)
* **Mục tiêu:** Quản lý tập trung các thực thể nền tảng làm xương sống cho toàn bộ quy trình kho bãi và logistics.
* **Các User Stories:**
  * **US-WMS-19 (P1):** [Quản lý SKU và danh mục sản phẩm](.sdd/specs/002-master-data-management/features/feature-admin-products.md) (hỗ trợ quy đổi đơn vị Thùng → Cái; không quản lý serial/hạn sử dụng cho hàng gia dụng Sprint 1).
  * **US-WMS-20 (P2):** [Cấu hình Vị trí kho](.sdd/specs/002-master-data-management/features/feature-admin-warehouses.md) (Zone → Bin) và kiểm tra sức chứa Bin (m3/kg) khi Putaway (`warehouse_locations`).
  * **US-WMS-22 (P1):** [Quản lý Danh mục Đối tác](.sdd/specs/002-master-data-management/features/feature-accountant-partners/feature-accountant-partners.md) (Đại lý & Nhà cung cấp). Kế toán trưởng thiết lập Credit Limit cho từng Đại lý.
  * **US-WMS-23 (P2):** [Quản lý Danh mục Xe tải & Tài xế Nội bộ](.sdd/specs/002-master-data-management/features/feature-dispatcher-fleet-drivers.md) (xe: Rảnh/Đang đi chuyến/Bảo trì; tài xế: Rảnh/Đang đi chuyến/Không khả dụng).
* **Actors:** Planner, Kế toán viên, Kế toán trưởng, System Admin, Dispatcher, Thủ kho kiêm QC, Trưởng kho.
* **Endpoints chính:** `/api/v1/products`, `/api/v1/warehouses`, `/api/v1/bin-locations`, `/api/v1/dealers`, `/api/v1/suppliers`, `/api/v1/vehicles`, `/api/v1/drivers`.

### Spec 003: Nhập Hàng & QC Inbound
* **Mã Spec:** [003-inbound-receipt-qc](.sdd/specs/003-inbound-receipt-qc/spec.md)
* **Mục tiêu:** Quản lý toàn bộ quy trình nhập kho từ tiếp nhận lệnh nhập thô đến kiểm hàng thực tế, thực hiện QC kiểm chất lượng, duyệt nhập chính thức để mở khóa putaway, và chỉ tăng tồn kho khả dụng sau khi putaway hoàn tất.
* **Các User Stories:**
  * **US-WMS-02 (P1):** [Lập Lệnh nhập kho thủ công](.sdd/specs/003-inbound-receipt-qc/features/feature-planner-receipt-drafting/feature-planner-receipt-drafting.md) từ nguồn Email/Zalo (Trạng thái ban đầu: `Pending Receipt`).
  * **US-WMS-03 (P1):** [Đếm hàng thực tế](.sdd/specs/003-inbound-receipt-qc/features/feature-warehouse-staff-receipt-counting/feature-warehouse-staff-receipt-counting.md) và [Kiểm QC Inbound](.sdd/specs/003-inbound-receipt-qc/features/feature-qc-inbound-inspection.md) (Phân loại Đạt → cất vào warehouse_locations đạt chuẩn; Lỗi → bắt buộc chuyển sang Quarantine Zone).
  * **US-WMS-04 (P1 - RTV sub-flow):** [Phê duyệt hàng lỗi trong Quarantine Zone](.sdd/specs/003-inbound-receipt-qc/features/feature-manager-quarantine-handling.md) (Feature 003 chỉ hiển thị "Trả NCC": Trưởng kho tạo RTV request + hệ thống tự tạo Debit Note; Thủ kho phải xác nhận giao trả đủ toàn bộ số lượng Quarantine thì mới trừ quarantine). Tiêu hủy hàng lỗi được map riêng trong Spec 009.
  * **US-WMS-05 (P1):** [Duyệt nhập kho](.sdd/specs/003-inbound-receipt-qc/features/feature-manager-receipt-approval.md) (Trưởng kho đối chiếu kết quả QC, ký duyệt Phiếu nhập kho để mở khóa putaway; nếu từ chối thì chờ xe NCC đến nhận và Thủ kho xác nhận `RETURNED_TO_SUPPLIER`; sau khi putaway xong mới tăng tồn kho khả dụng thực tế).
* **Actors:** Planner, Thủ kho kiêm QC, Nhân viên kho, Trưởng kho, Kế toán viên.
* **Endpoints chính:** `/api/v1/receipts`, `/api/v1/receipts/{id}/qc`, `/api/v1/receipts/{id}/approve`, `/api/v1/receipts/{id}/reject`, `/api/v1/receipts/{id}/return-to-supplier/confirm`, `/api/v1/receipts/{id}/rtv`, `/api/v1/receipts/{id}/rtv/confirm`.

### Spec 004: Xuất Hàng & Giao Hàng (Outbound)
* **Mã Spec:** [004-outbound-delivery-pod](.sdd/specs/004-outbound-delivery-pod/spec.md)
* **Mục tiêu:** Quản lý quy trình bán hàng xuất kho tích hợp kiểm tra công nợ Đại lý tự động, giữ chỗ (reserve) tồn kho khả dụng, soạn hàng, kiểm QC Outbound đóng gói, gom chuyến xe và xác nhận giao hàng bằng OTP.
* **Các User Stories:**
  * **US-WMS-06 (P1):** [Tiếp nhận yêu cầu & Lập Đơn xuất hàng](.sdd/specs/004-outbound-delivery-pod/features/feature-planner-delivery-order/feature-planner-delivery-order.md) (Delivery Order). Hệ thống tự động Credit Check (Chặn nếu nợ quá hạn >30 ngày hoặc vượt Credit Limit) và tự động giữ chỗ (Reserve) tồn kho khả dụng. Giải phóng Reserved khi đơn bị hủy hoặc chuyển sang trạng thái In-Transit.
  * **US-WMS-07 (P1):** [Soạn hàng tại kệ (Picking)](.sdd/specs/004-outbound-delivery-pod/features/feature-storekeeper-picking-plan/feature-storekeeper-picking-plan.md) & [Kiểm QC đóng gói](.sdd/specs/004-outbound-delivery-pod/features/feature-warehouse-staff-picking-qc/feature-warehouse-staff-picking-qc.md) (đúng SKU, số lượng, đóng thùng chống sốc).
  * **US-WMS-08 (P1):** [Lập Chuyến xe (Trip Log)](.sdd/specs/004-outbound-delivery-pod/features/feature-dispatcher-trip-dispatch/feature-dispatcher-trip-dispatch.md) nội bộ, gán tài xế, sắp xếp Stop Order. Trừ tồn kho vật lý tại thời điểm xe xuất phát rời kho (status đổi sang In-Transit).
  * **US-WMS-09 (P1):** [Giao diện Web di động cho Tài xế](.sdd/specs/004-outbound-delivery-pod/features/feature-driver-mobile-pod/feature-driver-mobile-pod.md) để xem chuyến xe và xác nhận giao hàng bằng OTP tại điểm giao. Mỗi lần giao là một `deliveries` attempt riêng; OTP chỉ lưu hash/verifier trong `delivery_otp_attempts`, không lưu raw OTP. Nếu giao thất bại, attempt hiện tại chuyển `FAILED`, DO chuyển `RETURNED`, hàng vẫn ở kho ảo In-Transit cho đến khi luồng hoàn hàng riêng tiếp nhận và phân loại.
  * **US-WMS-10 (P1 - Notification sub-flow):** Thông báo Kế toán ngay khi đơn chuyển trạng thái sang `Delivered`. Kế toán dùng invoice candidates worklist để không sót DO đã giao chưa lập hóa đơn; tạo hóa đơn xong thì DO chuyển `COMPLETED`. Ghi nhận công nợ và credit hold được map đầy đủ trong [Spec 008](.sdd/specs/008-finance-billing-closing/spec.md).
* **Actors:** Planner, Thủ kho kiêm QC, Nhân viên kho, Dispatcher, Tài xế, Kế toán viên, Kế toán trưởng, Trưởng kho.
* **Endpoints chính:** `/api/v1/delivery-orders`, `/api/v1/delivery-orders/{id}/pick`, `/api/v1/delivery-orders/{id}/qc-outbound`, `/api/v1/trips`, `/api/v1/trips/{id}/depart`, `/api/v1/trips/{tripId}/delivery-orders/{doId}/pod-evidence`, `/api/v1/trips/{tripId}/delivery-orders/{doId}/delivery-otp`, `/api/v1/trips/{tripId}/delivery-orders/{doId}/confirm-delivery`, `/api/v1/trips/{tripId}/delivery-orders/{doId}/fail-delivery`, `/api/v1/accounting/invoice-candidates`, `/api/v1/accounting/invoice-candidates/{doId}/invoice`.

### Spec 005: Điều Chuyển Kho Nội Bộ
* **Mã Spec:** [005-inter-warehouse-transfer](.sdd/specs/005-inter-warehouse-transfer/spec.md)
* **Mục tiêu:** Cân bằng nguồn hàng giữa 3 kho miền thông qua kho ảo In-Transit và đội xe nội bộ Phúc Anh.
* **Các User Stories:**
  * **US-WMS-11 (P2):** [Planner nhập lệnh điều chuyển thủ công](.sdd/specs/005-inter-warehouse-transfer/features/feature-planner-transfer-planning.md) dựa trên lệnh từ Công ty mẹ/bộ phận điều phối trung tâm. Sprint 1 không tự sinh gợi ý điều chuyển; mỗi phiếu bắt buộc có mã lệnh ngoài để truy vết.
  * **US-WMS-12 (P1):** [Lập, Duyệt và Xác nhận Phiếu Điều chuyển Kho](.sdd/specs/005-inter-warehouse-transfer/features/feature-storekeeper-transfer-ship.md). Trưởng kho nguồn duyệt thì khóa hàng ngay. Mỗi phiếu điều chuyển gắn đúng một chuyến xe nội bộ riêng do Dispatcher lập; Thủ kho nguồn phải xuất đúng số đã duyệt, muốn hủy sau khi hàng đã lên xe thì phải unship/unload trước; Tài xế xác nhận rời kho mới chuyển `IN_TRANSIT`. Kho đích nhận hàng theo 3 bước: công nhân nhập số lượng, Thủ kho kiểm/QC/chọn vị trí nhập, Trưởng kho xác nhận cuối; phần QC lỗi vào Quarantine, nhận thừa bị chặn, thiếu thì tạo `TRANSFER_DISCREPANCY`. (Xem thêm: [Kho Đích Tiếp nhận](.sdd/specs/005-inter-warehouse-transfer/features/feature-storekeeper-transfer-receive.md))
* **Actors:** Planner, Trưởng kho (Kho nguồn), Dispatcher, Thủ kho (Kho nguồn), Nhân viên kho/Công nhân kho đích, Thủ kho (Kho đích), Trưởng kho (Kho đích), Nhân viên kho, Tài xế.
* **Endpoints chính:** `/api/v1/transfers`, `/api/v1/transfers/{id}`, `/api/v1/transfers/{id}/cancel`, `/api/v1/transfers/{id}/approve`, `/api/v1/transfers/{id}/reject`, `/api/v1/transfers/{id}/trip`, `/api/v1/transfers/{id}/ship`, `/api/v1/transfers/{id}/unship`, `/api/v1/transfers/{id}/depart`, `/api/v1/transfers/{id}/receive-count`, `/api/v1/transfers/{id}/receive-check`, `/api/v1/transfers/{id}/receive`.

### Spec 006: Kiểm Kê & Điều Chỉnh Tồn Kho
* **Mã Spec:** [006-stocktake-adjustment](.sdd/specs/006-stocktake-adjustment/spec.md)
* **Mục tiêu:** Quy trình đối chiếu và điều chỉnh số liệu hệ thống khớp với số đếm thực tế của thủ kho định kỳ.
* **Các User Stories:**
  * **US-WMS-13 (P1):** [Kiểm kê định kỳ](.sdd/specs/006-stocktake-adjustment/features/feature-storekeeper-stocktake-count.md): Lập phiếu kiểm kê, khóa sổ kệ tạm thời, nhập số lượng đếm thực tế, tự động tính chênh lệch (Variance). Khi được duyệt, cập nhật tồn kho hệ thống và ghi Audit Log. (Xem thêm: [Duyệt chênh lệch kiểm kê](.sdd/specs/006-stocktake-adjustment/features/feature-manager-stocktake-approval.md))
* **Actors:** Thủ kho, Trưởng kho, CEO.
* **Endpoints chính:** `/api/v1/stocktakes`, `/api/v1/stocktakes/{id}/start`, `/api/v1/stocktakes/{id}/count`, `/api/v1/stocktakes/{id}/complete`, `/api/v1/stocktakes/{id}/approve`.

### Spec 007: Bảng Giá & Giá Vốn (Pricing & COGS)
* **Mã Spec:** [007-pricing-cogs-management](.sdd/specs/007-pricing-cogs-management/spec.md)
* **Mục tiêu:** Quản lý bảng giá bán và giá vốn theo kỳ kinh doanh hiệu lực để tính giá trị giao dịch và COGS chính xác.
* **Các User Stories:**
  * **US-WMS-14 (P1):** [Quản lý bảng giá theo kỳ](.sdd/specs/007-pricing-cogs-management/features/feature-accountant-price-entry-management.md) (Maker-Checker: Kế toán viên lập, Kế toán trưởng duyệt). Hệ thống bắt buộc lưu lịch sử giá vào `price_history`. Khi xuất hàng, hệ thống tự động tra cứu giá vốn/bán. (Xem thêm: [Tự động tính COGS](.sdd/specs/007-pricing-cogs-management/features/feature-system-cogs-calculation.md))
* **Actors:** Kế toán viên, Kế toán trưởng.
* **Endpoints chính:** `/api/v1/price-lists`, `/api/v1/price-lists/{id}/approve`, `/api/v1/products/{id}/price-history`.

### Spec 008: Tài Chính & Công Nợ Đại Lý
* **Mã Spec:** [008-finance-billing-closing](.sdd/specs/008-finance-billing-closing/spec.md)
* **Mục tiêu:** Quản lý toàn bộ hóa đơn bán hàng, ghi nhận thanh toán, cấn trừ công nợ, phân tích nợ quá hạn và thực hiện chốt sổ tháng.
* **Các User Stories:**
  * **US-WMS-10 (P1):** [Lập Hóa đơn bán hàng](.sdd/specs/008-finance-billing-closing/features/feature-accountant-customer-invoicing.md) từ đơn Delivered kèm kỳ hạn thanh toán (Net 30 / Net 60), tự động cộng dồn công nợ Đại lý.
  * **US-WMS-15 (P1):** [Ghi nhận Thanh toán & Quản lý vòng đời công nợ](.sdd/specs/008-finance-billing-closing/features/feature-accountant-payment-collection.md) (Kế toán viên tạo phiếu thu `payment_receipts` cấn trừ hóa đơn). Tự động mở khóa tín dụng chuyển Đại lý về `ACTIVE` nếu `current_balance < credit_limit * 0.8`.
  * **US-WMS-16 (P1):** [Báo cáo Công nợ Phân kỳ (Aging Report)](.sdd/specs/008-finance-billing-closing/features/feature-accountant-credit-aging-report.md) theo các mốc: trong hạn, quá hạn 1-30 ngày, 31-60 ngày, >60 ngày.
  * **US-WMS-17 (P1):** [Chốt sổ Kế toán](.sdd/specs/008-finance-billing-closing/features/feature-accountant-period-closing.md) & Khóa cứng kỳ quá khứ (Kế toán trưởng thực hiện hàng tháng trên `accounting_periods`).
* **Actors:** Kế toán viên, Kế toán trưởng, Hệ thống (Daily Batch Job).
* **Endpoints chính:** `/api/v1/invoices`, `/api/v1/payments`, `/api/v1/credit/aging-report`, `/api/v1/accounting/periods/{period}/close`.

### Spec 009: Hàng Hoàn Trả & Tiêu Hủy
* **Mã Spec:** [009-returns-scrap-disposal](.sdd/specs/009-returns-scrap-disposal/spec.md)
* **Mục tiêu:** Quy trình tiếp nhận hàng trả về từ đại lý và thanh lý, tiêu hủy hàng hỏng trong khu cách ly.
* **Các User Stories:**
  * **US-WMS-24 (P2):** Xử lý hàng hoàn trả từ Đại lý (Inbound Returns). Thủ kho lập phiếu nhận hàng hoàn (`receipts` type = 'RETURN') → Thủ kho kiểm QC chất lượng (Hàng tốt → nhập lại kho thường; Hàng lỗi → chuyển Quarantine Zone). Kế toán tạo Credit Note tương ứng để trừ công nợ cho Đại lý. [Chi tiết](.sdd/specs/009-returns-scrap-disposal/features/feature-storekeeper-customer-returns.md)
  * **US-WMS-04 (P1 - Disposal sub-flow):** Tiêu hủy hàng lỗi từ Quarantine Zone. Trưởng kho đề xuất và phê duyệt trực tiếp, không phân cấp theo giá trị (tạo `adjustments` type = 'DISPOSAL' và `damage_reports`). Được duyệt thì giảm tồn quarantine, ghi biên bản hủy. [Chi tiết](.sdd/specs/009-returns-scrap-disposal/features/feature-manager-scrap-disposal.md)
* **Actors:** Thủ kho kiêm QC, Nhân viên kho, Trưởng kho, Kế toán viên.
* **Endpoints chính:** `/api/v1/returns`, `/api/v1/returns/{id}/qc`, `/api/v1/returns/{id}/credit-note`, `/api/v1/disposal`, `/api/v1/disposal/{id}/approve`.

### Spec 010: Báo Cáo & Cảnh Báo (Reporting & Alerts)
* **Mã Spec:** [010-reports-dashboards-alerts](.sdd/specs/010-reports-dashboards-alerts/spec.md)
* **Mục tiêu:** Tổng hợp thông tin quản trị thời gian thực cho CEO và đưa ra cảnh báo tồn kho thấp cho ban vận hành.
* **Các User Stories:**
  * **US-WMS-18 (P1):** Dashboard báo cáo quản trị cấp cao cho CEO & Kế toán trưởng: tổng giá trị tồn kho 3 miền theo giá vốn, Top đại lý nợ nhiều nhất, báo cáo Lãi/Lỗ (P&L = Doanh thu - COGS - Chi phí vận hành), tỷ lệ QC lỗi, tỷ lệ giao hàng đúng hạn (OTD). Bắt buộc ghi log mỗi lượt xem báo cáo vào `audit_logs`. [Chi tiết](.sdd/specs/010-reports-dashboards-alerts/features/feature-ceo-management-dashboard.md)
  * **US-WMS-26 (P1):** Cảnh báo tự động tồn kho dưới định mức tối thiểu. Hệ thống bắn thông báo High Priority in-app cho Trưởng kho và Planner; đánh dấu đỏ sản phẩm thiếu hụt trên dashboard (lưu tại bảng `stock_alerts`). [Chi tiết](.sdd/specs/010-reports-dashboards-alerts/features/feature-manager-low-stock-alerts.md)
* **Actors:** CEO, Kế toán trưởng, Trưởng kho, Planner.
* **Endpoints chính:** `/api/v1/dashboard/ceo`, `/api/v1/reports/inventory-valuation`, `/api/v1/alerts/low-stock`.

### Spec 011: Kiểm thử Backend & SonarQube
* **Mã Spec:** [011-backend-test-sonarqube](.sdd/specs/011-backend-test-sonarqube/spec.md)
* **Mục tiêu:** Chuẩn hóa hạ tầng test backend, test core services/phân quyền và test nghiệp vụ WMS; thiết lập báo cáo JaCoCo/SonarQube cùng QA sign-off.
* **Quy tắc đồng bộ:** Quality Gate 80% chỉ áp dụng cho *new code* trong PR; không commit secrets vào test; dùng JUnit 5 parameterized tests cho nhiều bộ dữ liệu.
* **Actors:** Developer, Tech Lead/QA, QA Engineer, CI/CD Runner.

### Spec 012: Kiểm thử Frontend
* **Mã Spec:** [012-frontend-testing](.sdd/specs/012-frontend-testing/spec.md)
* **Mục tiêu:** Kiểm thử utility/form validation, route/RBAC UI, state component và chống double-submit bằng Vitest + React Testing Library.
* **Quy tắc đồng bộ:** ưu tiên kiểm thử hành vi người dùng; dùng test parameterized cho các bộ dữ liệu biên; không thay thế kiểm thử backend/integration API.
* **Actors:** Frontend Developer, Tech Lead/QA, CI/CD Runner.

---

## 5. Phạm Vi Out of Scope Tổng Thể Dự Án (Sprint 1)

Nhằm đảm bảo sự tập trung vào các nghiệp vụ cốt lõi, các thành phần sau được xác định nằm ngoài phạm vi triển khai của Sprint 1:
1. **SSO / OAuth2:** Chưa tích hợp hệ thống xác thực tập trung bên ngoài; sử dụng JWT nội bộ.
2. **IP Whitelisting & 2FA:** Không áp dụng giới hạn IP truy cập hoặc xác thực 2 lớp.
3. **Mã Barcode / QR Code:** Chưa tích hợp máy quét hoặc camera quét mã vị trí kệ/sản phẩm (nhập liệu thủ công trên giao diện).
4. **Logistics 3PL:** Không quản lý chi phí vận chuyển bên thứ ba; toàn bộ quy trình giao hàng sử dụng đội xe nội bộ Phúc Anh.
5. **Cổng B2B / B2C:** Không xây dựng cổng thông tin cho đại lý tự đặt hàng trực tuyến; mọi đơn xuất hàng được Planner tiếp nhận và nhập thủ công.
6. **Định tuyến giao hàng thời gian thực:** Không có chức năng GPS tracking trực tuyến của tài xế; chuyến xe chỉ quản lý trạng thái tĩnh và xác nhận OTP tại điểm giao.
7. **Sổ cái General Ledger (GL):** Hệ thống không thực hiện kế toán kép đầy đủ, không báo cáo thuế VAT, chỉ quản lý công nợ đại lý, hóa đơn, giá vốn hàng bán và doanh thu bán hàng nội bộ.
8. **Thuật toán tự động xếp kệ (Putaway Slotting):** Putaway dựa trên sức chứa thô của Bin, hệ thống kiểm tra và gợi ý thủ công thay vì sử dụng thuật toán tối ưu hóa vị trí lưu trữ thông minh.
