# Requirement & Design Specification

**Warehouse Management System (WMS) — Công ty Phúc Anh**

**Version: 1.0**

– Hà Nội, 2026 –

# Record of Changes

| Version | Date       | A\*M, D | In charge    | Change Description                                                                                                                  |
| ------- | ---------- | ------- | ------------ | ----------------------------------------------------------------------------------------------------------------------------------- |
| V1.0    | 2026-07-14 | A       | WMS Dev Team | Khởi tạo tài liệu RDS dựa trên `.sdd/specs/001` → `010`, `CLAUDE.md`, `README.md`, `AGENTS.md`, `Kiến trúc phân tầng các Actors.md` |
| V1.1    | 2026-07-15 | M       | WMS Dev Team | Bỏ phân cấp phê duyệt theo giá trị (AUTO/<5tr, Trưởng kho/5-100tr, CEO/>100tr) cho chênh lệch kiểm kê và tiêu hủy hàng lỗi; đồng bộ về quy tắc phẳng "Trưởng kho phê duyệt trực tiếp, không phân cấp giá trị" theo quyết định team và các tài liệu gốc đã cập nhật |
| V1.2    | 2026-07-15 | M       | WMS Dev Team | Đồng bộ catalog spec 001–012; xác định 011–012 là yêu cầu chất lượng xuyên suốt, không thay đổi actor/RBAC nghiệp vụ. |

_A - Added M - Modified D - Deleted_

---

# I. Overview

## 1. User Requirements

### 1.1 Actors

Hệ thống WMS có **10 Actors**, chia thành 3 tầng theo mô hình **Maker-Checker**.

| #   | Actor                                 | Description                                                                                                                                                                                                                                                                            |
| --- | ------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | **CEO**                               | Checker cấp cao. Xem Dashboard chiến lược (tồn kho, công nợ, P&L, tỷ lệ lỗi QC, OTD). Phê duyệt thay đổi cấu hình hệ thống quan trọng và yêu cầu điều chuyển liên kho do Trưởng kho đề xuất. |
| 2   | **System Admin**                      | Admin. Tạo/vô hiệu hóa tài khoản người dùng, phân quyền theo Role + Chi nhánh Kho (RBAC), cấu hình tham số hệ thống (hạn mức công nợ mặc định, tồn kho tối thiểu mặc định, kỳ hạn thanh toán mặc định, ngày khóa kỳ kế toán).                                                          |
| 3   | **Trưởng kho (Warehouse Manager)**    | Checker. Phê duyệt phiếu nhập/xuất/điều chuyển kho, phê duyệt mọi chênh lệch kiểm kê và phiếu xuất hủy hàng lỗi trực tiếp (không phân cấp theo giá trị), phê duyệt biên bản xử lý hàng lỗi (RTV/tiêu hủy), xem tồn kho liên kho read-only để đề xuất điều chuyển khi kho mình thiếu hàng. |
| 4   | **Kế toán trưởng (Chief Accountant)** | Checker. Duyệt bảng giá do Kế toán viên trình, thiết lập Hạn mức tín dụng (Credit Limit) và kỳ hạn thanh toán (Net 30/60) cho Đại lý, chốt sổ kế toán hàng tháng, xem Aging Report và P&L Report.                                                                                      |
| 5   | **Planner**                           | Maker. Tiếp nhận yêu cầu nhập/xuất hàng từ Công ty mẹ, lập Lệnh nhập kho và Đơn xuất hàng (kiểm tra Credit Check + tồn kho tự động), nhập Phiếu điều chuyển kho nội bộ theo lệnh ngoài hoặc yêu cầu đã được CEO duyệt.                                                                 |
| 6   | **Dispatcher**                        | Maker. Lập Chuyến xe nội bộ Phúc Anh (Trip), gán xe/tài xế, tối ưu Stop Order, kiểm tra tải trọng/thể tích xe, đảm bảo không phát sinh chi phí 3PL.                                                                                                                                    |
| 7   | **Thủ kho kiêm QC (Storekeeper)**     | Maker. Quản lý SKU/danh mục sản phẩm, kiểm QC inbound/outbound, lập kế hoạch soạn hàng, kiểm kê, cất hàng vào Bin Location, xác nhận điều chuyển ở cả kho nguồn và kho đích.                                                                                                           |
| 8   | **Nhân viên kho (Warehouse Staff)**   | Maker. Đếm hàng thực tế khi nhận hàng (`PENDING_RECEIPT → DRAFT`), ghi nhận kết quả QC inbound/outbound, bốc xếp và di chuyển hàng hóa vào Bin/Quarantine theo chỉ dẫn của Thủ kho.                                                                                                    |
| 9   | **Kế toán viên (Accountant)**         | Maker. Quản lý hồ sơ Nhà cung cấp, theo dõi/đối chiếu hóa đơn bán hàng và công nợ Đại lý (invoice do hệ thống tự động tạo — xem mục Non-UI Functions), ghi nhận Phiếu thu, quản lý bảng giá, lập Credit Note cho hàng hoàn trả.                                                        |
| 10  | **Tài xế (Driver)**                   | Maker. Giao diện Web mobile-friendly. Nhận chuyến, xác nhận nhận hàng rời kho, upload `goodsImage`/`signDocumentImage`, nhập OTP xác nhận giao hàng, báo giao thất bại, xác nhận xe về kho.                                                                                            |

### 1.2 Use Cases

#### a. Diagram(s)

Hệ thống có 10 nhóm nghiệp vụ tương ứng spec `.sdd/specs/001` → `010`. Spec `011-backend-test-sonarqube` và `012-frontend-testing` là yêu cầu chất lượng xuyên suốt (Developer/QA/CI), không phải nhóm use case vận hành và không thay đổi mô hình 10 actor. Quan hệ tổng quát giữa actor và nhóm use case:

```
CEO ────────────────► [Dashboard & Reports] [Transfer Request Approval]
System Admin ───────► [RBAC & System Config] [Audit Log]
Trưởng kho ─────────► [Receipt Approval] [DO Approval] [Transfer Approval] [Stocktake Approval] [Disposal Approval]
Kế toán trưởng ─────► [Price Approval] [Credit Limit Setup] [Period Closing] [Aging/P&L Report]
Planner ────────────► [Receipt Drafting] [Delivery Order Creation] [Transfer Planning]
Dispatcher ─────────► [Trip Dispatch (Delivery)] [Trip Dispatch (Transfer)]
Thủ kho kiêm QC ────► [QC Inbound/Outbound] [Picking Plan] [Putaway] [Stocktake Count] [Transfer Ship/Receive]
Nhân viên kho ──────► [Receipt Counting] [Picking + QC Execution] [Load/Unload]
Kế toán viên ───────► [Customer Invoicing] [Payment Collection] [Price Entry] [Supplier Management] [Credit Note]
Tài xế ─────────────► [Mobile POD] [Delivery OTP] [Transfer Handover]
```

_(Sơ đồ trên mô tả quan hệ Actor → Nhóm Use Case ở mức tổng quan; sơ đồ UC chi tiết dạng UML cho từng spec nên được vẽ riêng khi cần trình bày trực quan, sử dụng danh sách Use Case tại mục b bên dưới làm input.)_

#### b. Descriptions

| ID    | Feature                   | Use Case                                      | Use Case Description                                                                                                         |
| ----- | ------------------------- | --------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| UC-01 | 001 – System Config       | Configure System Parameters                   | System Admin cấu hình hạn mức công nợ mặc định, tồn kho tối thiểu mặc định, kỳ hạn thanh toán mặc định, ngày khóa kỳ kế toán |
| UC-02 | 001 – RBAC                | Manage Users & Role/Warehouse Assignment      | System Admin tạo/vô hiệu hóa tài khoản, gán Role + Chi nhánh Kho cho user                                                    |
| UC-03 | 001 – Auth                | Login (JWT)                                   | User đăng nhập bằng email/password, hệ thống trả JWT access/refresh token                                                    |
| UC-04 | 001 – Audit Log           | View Audit Log                                | System Admin tra cứu nhật ký hoạt động hệ thống (append-only, không cho sửa/xóa)                                             |
| UC-05 | 002 – Products            | Manage Product/SKU Catalog                    | Thủ kho kiêm QC tạo/cập nhật SKU, đơn vị tính, quy cách đóng gói                                                             |
| UC-06 | 002 – Warehouses          | Configure Warehouse Zones & Bin Locations     | Trưởng kho/Admin cấu hình Zone, Bin Location và sức chứa (capacity)                                                          |
| UC-07 | 002 – Partners            | Manage Dealer/Supplier & Credit Limit         | Kế toán viên quản lý hồ sơ NCC/Đại lý; Kế toán trưởng thiết lập Credit Limit                                                 |
| UC-08 | 002 – Fleet               | Manage Vehicles & Drivers                     | Dispatcher/Admin quản lý danh mục xe tải và tài xế nội bộ                                                                    |
| UC-09 | 003 – Receipt Drafting    | Create Purchase Receipt (Pending)             | Planner lập Lệnh nhập kho `PENDING_RECEIPT` từ thông tin Công ty mẹ gửi                                                      |
| UC-10 | 003 – Receipt Counting    | Record Physical Receive Count                 | Nhân viên kho đếm hàng thực tế, cập nhật `actual_qty`/`over_received_qty`, chuyển `DRAFT`                                    |
| UC-11 | 003 – QC Inbound          | Perform Inbound QC Inspection                 | Nhân viên kho/Thủ kho ghi nhận kết quả QC Đạt/Lỗi, chuyển `QC_COMPLETED`/`QC_FAILED`                                         |
| UC-12 | 003 – Quarantine          | Handle Quarantine & RTV                       | Trưởng kho tạo RTV "Trả NCC" cho hàng lỗi QC, Thủ kho xác nhận bàn giao NCC                                                  |
| UC-13 | 003 – Receipt Approval    | Approve/Reject Receipt & Putaway              | Trưởng kho duyệt phiếu nhập → mở khóa putaway; Thủ kho cất hàng vào Bin                                                      |
| UC-14 | 004 – Delivery Order      | Create Delivery Order (Credit Check)          | Planner lập Đơn xuất hàng, hệ thống tự Credit Check + reserve tồn kho                                                        |
| UC-15 | 004 – Picking Plan        | Create Picking Plan (FIFO)                    | Thủ kho lập kế hoạch lấy hàng từ batch/bin/zone theo FIFO                                                                    |
| UC-16 | 004 – Picking & QC        | Execute Picking & Outbound QC                 | Nhân viên kho lấy hàng theo kế hoạch, ghi QC; hàng fail vào Quarantine                                                       |
| UC-17 | 004 – Warehouse Approval  | Approve/Reject Delivery Order                 | Trưởng kho duyệt xuất kho sau khi đủ hàng QC đạt                                                                             |
| UC-18 | 004 – Trip Dispatch       | Dispatch Delivery Trip                        | Dispatcher lập Chuyến xe (`trip_type=DELIVERY`), gán xe/tài xế cùng kho                                                      |
| UC-19 | 004 – Driver Mobile POD   | Confirm Delivery (POD + OTP)                  | Tài xế upload ảnh hàng/chữ ký, nhập OTP; hệ thống chuyển DO `COMPLETED`                                                      |
| UC-20 | 004 – Auto Invoice        | Auto-create Invoice on Delivery               | Hệ thống tự tạo Invoice + cộng công nợ Đại lý khi DO giao thành công                                                         |
| UC-21 | 005 – Transfer Request    | View Cross-Warehouse Stock & Request Transfer | Trưởng kho kho thiếu xem tồn liên kho read-only, tạo yêu cầu điều chuyển gửi CEO                                             |
| UC-22 | 005 – Transfer Planning   | Create Transfer Order (TRF-\*)                | Planner lập Phiếu điều chuyển theo lệnh ngoài hoặc request đã CEO duyệt                                                      |
| UC-23 | 005 – Transfer Approval   | Approve/Reject Transfer & Reserve Stock       | Trưởng kho nguồn duyệt phiếu, giữ chỗ FIFO-eligible stock                                                                    |
| UC-24 | 005 – Transfer Ship       | Dispatch Trip & Ship Goods                    | Dispatcher lập chuyến `TTR-*`; Thủ kho nguồn QC + xuất hàng lên xe                                                           |
| UC-25 | 005 – Transfer Receive    | Receive & Confirm Transfer at Destination     | Công nhân đếm, Thủ kho kiểm/QC, Trưởng kho đích xác nhận cuối                                                                |
| UC-26 | 006 – Stocktake Count     | Create Stocktake & Record Count               | Thủ kho tạo phiếu kiểm kê, đếm thực tế, hệ thống tính Variance                                                               |
| UC-27 | 006 – Stocktake Approval  | Approve Inventory Adjustment                  | Trưởng kho duyệt hoặc từ chối điều chỉnh chênh lệch tồn kho trực tiếp, không phân cấp theo giá trị                          |
| UC-28 | 007 – Price Entry         | Create Price List (Cost + Selling)            | Kế toán viên nhập bảng giá theo kỳ hiệu lực, hỗ trợ import Excel                                                             |
| UC-29 | 007 – Price Approval      | Approve Price List                            | Kế toán trưởng duyệt bảng giá trước khi có hiệu lực                                                                          |
| UC-30 | 007 – COGS Calculation    | Auto-calculate COGS                           | Hệ thống tự tính giá vốn hàng bán từ `price_history` snapshot                                                                |
| UC-31 | 008 – Customer Invoicing  | Track & Reconcile Invoice                     | Kế toán viên đối chiếu `billing_notifications` với Invoice tự động tạo                                                       |
| UC-32 | 008 – Payment Collection  | Record Payment Receipt                        | Kế toán viên ghi Phiếu thu, cấn trừ hóa đơn, hệ thống mở khóa tín dụng theo buffer 20%                                       |
| UC-33 | 008 – Aging Report        | View Credit Aging Report                      | Kế toán trưởng xem báo cáo phân kỳ công nợ                                                                                   |
| UC-34 | 008 – Period Closing      | Close Accounting Period                       | Kế toán trưởng chốt sổ kỳ, khóa cứng chứng từ                                                                                |
| UC-35 | 009 – Customer Returns    | Process Dealer Return (Credit Note)           | Thủ kho tiếp nhận hàng hoàn trả từ Đại lý, Kế toán viên lập Credit Note                                                      |
| UC-36 | 009 – Scrap Disposal      | Approve & Execute Disposal                    | Trưởng kho duyệt tiêu hủy hàng lỗi từ Quarantine                                                                             |
| UC-37 | 010 – CEO Dashboard       | View Management Dashboard                     | CEO xem KPI tồn kho, công nợ, P&L, tỷ lệ lỗi QC, OTD                                                                         |
| UC-38 | 010 – Low Stock Alert     | Trigger/Resolve Low Stock Alert               | Hệ thống tự cảnh báo khi tồn khả dụng dưới `reorder_point`                                                                   |
| UC-39 | 010 – Productivity Report | View/Export Productivity Report               | Trưởng kho xem/xuất báo cáo năng suất nhân viên kho                                                                          |

## 2. Overall Functionalities

### 2.1 Screens Flow

Luồng màn hình chính theo từng nhóm nghiệp vụ (mô tả dạng text; nên vẽ lại bằng công cụ diagram khi trình bày chính thức):

```
[Login] ──► [Home/Dashboard theo Role]
   │
   ├─► [Inbound]: Receipt List ──► Receipt Detail (Draft) ──► Count Form ──► QC Form
   │        └─► Quarantine List ──► RTV Detail (pop-up) ──► Receipt Approval Detail ──► Putaway Form
   │
   ├─► [Outbound]: DO List ──► DO Detail (Create) ──► Picking Plan (multi-tab: Allocation/QC)
   │        └─► Warehouse Approval Detail ──► Trip Dispatch List ──► Trip Detail
   │        └─► Driver Mobile: Trip List (mobile) ──► POD Upload (pop-up camera) ──► OTP Entry (pop-up)
   │
   ├─► [Transfer]: Cross-Warehouse Stock (read-only) ──► Transfer Request Form
   │        └─► Transfer List ──► Transfer Detail (multi-tab: Approve/Ship/Receive)
   │
   ├─► [Stocktake]: Stocktake List ──► Stocktake Count Form ──► Variance Approval Detail
   │
   ├─► [Pricing]: Price List ──► Price Detail Form ──► Price Approval Detail
   │        └─► Excel Import (pop-up)
   │
   ├─► [Finance]: Billing Notification Worklist ──► Invoice Detail ──► Payment Receipt Form
   │        └─► Aging Report (screen with export) ──► Period Closing (pop-up confirm)
   │
   ├─► [Returns/Disposal]: Customer Return Form ──► Credit Note Detail
   │        └─► Quarantine Disposal List ──► Disposal Approval Detail (pop-up)
   │
   ├─► [Reports]: CEO Dashboard (multi-tab: Inventory/Credit/P&L/QC/OTD)
   │        └─► Low Stock Alerts List ──► Productivity Report (screen with export)
   │
   └─► [Admin]: User List ──► User Detail Form
            └─► System Config Form
            └─► Audit Log List (read-only, filter by time/warehouse)
```

Quy ước: màn hình dạng oval/pop-up (VD: RTV Detail, OTP Entry, Excel Import, Period Closing Confirm) là các thao tác xác nhận nhanh không rời khỏi ngữ cảnh màn hình cha. Màn hình có nhiều tab (VD: Picking Plan, Transfer Detail, CEO Dashboard) chứa nhiều nhóm thông tin liên quan trên cùng một đối tượng nghiệp vụ.

### 2.2 Screen Descriptions

| #   | Feature           | Screen                        | Description                                                                    |
| --- | ----------------- | ----------------------------- | ------------------------------------------------------------------------------ |
| 1   | Inbound Receipt   | Receipt List                  | Danh sách phiếu nhập theo trạng thái, filter theo kho/Planner/khoảng thời gian |
| 2   | Inbound Receipt   | Receipt Detail (Count)        | Nhân viên kho nhập số lượng đếm thực tế cho từng dòng hàng                     |
| 3   | Inbound Receipt   | QC Inbound Form               | Ghi nhận kết quả QC Đạt/Lỗi kèm lý do chi tiết cho từng dòng                   |
| 4   | Inbound Receipt   | Quarantine/RTV Detail         | Trưởng kho tạo RTV, Thủ kho xác nhận bàn giao NCC                              |
| 5   | Inbound Receipt   | Receipt Approval Detail       | Trưởng kho duyệt/từ chối phiếu nhập; Thủ kho putaway vào Bin                   |
| 6   | Outbound Delivery | Delivery Order List           | Danh sách DO theo trạng thái, filter theo Đại lý/kho                           |
| 7   | Outbound Delivery | Delivery Order Detail         | Planner tạo DO, hệ thống hiển thị kết quả Credit Check                         |
| 8   | Outbound Delivery | Picking Plan Screen           | Thủ kho chọn batch/bin/zone theo FIFO cho từng dòng DO                         |
| 9   | Outbound Delivery | Picking & QC Execution        | Nhân viên kho ghi nhận kết quả lấy hàng/QC theo allocation                     |
| 10  | Outbound Delivery | Warehouse Approval Detail     | Trưởng kho duyệt/từ chối xuất kho                                              |
| 11  | Outbound Delivery | Trip Dispatch Screen          | Dispatcher gom DO vào chuyến, gán xe/tài xế, sắp Stop Order                    |
| 12  | Outbound Delivery | Driver Trip List (Mobile)     | Tài xế xem danh sách chuyến/DO được gán                                        |
| 13  | Outbound Delivery | POD Upload (Mobile)           | Tài xế chụp/chọn ảnh hàng + ảnh chữ ký                                         |
| 14  | Outbound Delivery | OTP Entry (Mobile)            | Tài xế nhập OTP do Đại lý đọc để xác nhận giao                                 |
| 15  | Transfer          | Cross-Warehouse Stock View    | Trưởng kho xem tồn khả dụng liên kho read-only                                 |
| 16  | Transfer          | Transfer Request Form         | Trưởng kho tạo yêu cầu điều chuyển gửi CEO duyệt                               |
| 17  | Transfer          | Transfer Detail (Approve)     | Trưởng kho nguồn duyệt/từ chối phiếu `TRF-*`                                   |
| 18  | Transfer          | Transfer Ship Screen          | Thủ kho nguồn QC + ghi nhận xuất hàng lên xe                                   |
| 19  | Transfer          | Transfer Receive Screen       | Công nhân đếm → Thủ kho kiểm/QC → Trưởng kho đích xác nhận                     |
| 20  | Stocktake         | Stocktake Count Screen        | Thủ kho nhập số lượng đếm thực tế theo Bin                                     |
| 21  | Stocktake         | Variance Approval Detail      | Trưởng kho/CEO duyệt điều chỉnh tồn kho theo hạn mức                           |
| 22  | Pricing           | Price List Screen             | Kế toán viên tạo/sửa bảng giá theo kỳ hiệu lực                                 |
| 23  | Pricing           | Price Approval Detail         | Kế toán trưởng duyệt bảng giá                                                  |
| 24  | Pricing           | Excel Import Popup            | Kế toán viên import bảng giá hàng loạt từ file mẫu                             |
| 25  | Finance           | Billing Notification Worklist | Kế toán viên xem danh sách DO đã giao chờ đối chiếu invoice                    |
| 26  | Finance           | Invoice Detail                | Xem chi tiết hóa đơn, thông tin đối chứng POD/OTP                              |
| 27  | Finance           | Payment Receipt Form          | Kế toán viên ghi nhận Phiếu thu, cấn trừ hóa đơn                               |
| 28  | Finance           | Aging Report Screen           | Kế toán trưởng xem báo cáo phân kỳ công nợ, export                             |
| 29  | Finance           | Period Closing Popup          | Kế toán trưởng xác nhận chốt sổ kỳ                                             |
| 30  | Returns/Disposal  | Customer Return Form          | Thủ kho ghi nhận hàng hoàn trả từ Đại lý                                       |
| 31  | Returns/Disposal  | Disposal Approval Detail      | Trưởng kho duyệt tiêu hủy hàng lỗi từ Quarantine                               |
| 32  | Reports           | CEO Dashboard                 | CEO xem KPI tổng hợp toàn hệ thống                                             |
| 33  | Reports           | Low Stock Alerts List         | Trưởng kho/Planner xem cảnh báo tồn kho thấp                                   |
| 34  | Reports           | Productivity Report Screen    | Trưởng kho xem/xuất báo cáo năng suất nhân viên                                |
| 35  | Admin             | User List / User Detail       | System Admin quản lý tài khoản, gán role + kho                                 |
| 36  | Admin             | System Config Screen          | System Admin cấu hình tham số hệ thống                                         |
| 37  | Admin             | Audit Log Screen              | System Admin tra cứu nhật ký hoạt động                                         |

### 2.3 Screen Authorization

| Screen Group                  | CEO         | System Admin | Trưởng kho  | Kế toán trưởng   | Planner    | Dispatcher | Thủ kho     | Nhân viên kho | Kế toán viên    | Tài xế |
| ----------------------------- | ----------- | ------------ | ----------- | ---------------- | ---------- | ---------- | ----------- | ------------- | --------------- | ------ |
| System Config / RBAC          |             | X            |             |                  |            |            |             |               |                 |        |
| Audit Log (query all)         |             | X            |             |                  |            |            |             |               |                 |        |
| Product/SKU Catalog           |             |              | X (view)    |                  |            |            | X           |               |                 |        |
| Warehouse/Bin Config          | X (approve) | X            | X           |                  |            |            |             |               |                 |        |
| Dealer/Supplier Mgmt          |             |              |             | X (Credit Limit) |            |            |             |               | X               |        |
| Vehicle/Driver Catalog        |             | X            |             |                  |            | X          |             |               |                 |        |
| Receipt Drafting              |             |              |             |                  | X          |            |             |               |                 |        |
| Receipt Counting              |             |              |             |                  |            |            | X (view)    | X             |                 |        |
| QC Inbound                    |             |              |             |                  |            |            | X           | X             |                 |        |
| Quarantine/RTV                |             |              | X           |                  |            |            | X (confirm) |               |                 |        |
| Receipt Approval/Putaway      |             |              | X           |                  |            |            | X (putaway) |               |                 |        |
| Delivery Order Create         |             |              |             |                  | X          |            |             |               |                 |        |
| Picking Plan                  |             |              |             |                  |            |            | X           |               |                 |        |
| Picking & Outbound QC         |             |              |             |                  |            |            | X (view)    | X             |                 |        |
| Warehouse Approval (DO)       |             |              | X           |                  |            |            |             |               |                 |        |
| Trip Dispatch                 |             |              |             |                  |            | X          |             |               |                 |        |
| Driver Mobile POD/OTP         |             |              |             |                  |            |            |             |               |                 | X      |
| Auto Invoice (system)         |             |              |             |                  |            |            |             |               | X (view)        |        |
| Cross-WH Stock (read-only)    | X           |              | X           |                  |            |            |             |               |                 |        |
| Transfer Request → CEO        | X (approve) |              | X (create)  |                  |            |            |             |               |                 |        |
| Transfer Planning             |             |              |             |                  | X          |            |             |               |                 |        |
| Transfer Approval             |             |              | X           |                  |            |            |             |               |                 |        |
| Transfer Ship                 |             |              |             |                  |            | X (trip)   | X (QC/ship) | X (load)      |                 |        |
| Transfer Receive              |             |              | X (final)   |                  |            |            | X (QC)      | X (count)     |                 |        |
| Stocktake Count               |             |              |             |                  |            |            | X           |               |                 |        |
| Stocktake/Adjustment Approval |             |              | X           |                  |            |            |             |               |                 |        |
| Price Entry/Import            |             |              |             |                  |            |            |             |               | X               |        |
| Price Approval                |             |              |             | X                |            |            |             |               |                 |        |
| Customer Invoicing (view)     |             |              |             |                  |            |            |             |               | X               |        |
| Payment Collection            |             |              |             |                  |            |            |             |               | X               |        |
| Aging Report / P&L            | X (view)    |              |             | X                |            |            |             |               |                 |        |
| Period Closing                |             |              |             | X                |            |            |             |               |                 |        |
| Customer Returns              |             |              |             |                  |            |            | X           |               | X (Credit Note) |        |
| Scrap Disposal Approval       |             |              | X           |                  |            |            |             |               |                 |        |
| CEO Dashboard                 | X           |              |             | X (financial)    |            |            |             |               |                 |        |
| Low Stock Alerts              |             |              | X (own WH)  |                  | X (all WH) |            |             |               |                 |        |
| Productivity Report           |             |              | X           |                  |            |            |             |               |                 |        |

_Ghi chú: mọi API/screen thuộc phạm vi kho (warehouse-scoped) phải kiểm tra CẢ role LẪN warehouse assignment của user (không chỉ role) — xem `AGENTS.md` mục WMS Domain Rules._

### 2.4 Non-UI Functions

| #   | Feature           | System Function                  | Description                                                                                                                                                                                     |
| --- | ----------------- | -------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | Outbound Delivery | Auto-Invoice Creation            | Event-driven service (`AutoInvoiceService`), kích hoạt ngay khi Tài xế xác nhận POD + OTP hợp lệ cho full DO: tạo `invoices`, cộng `dealers.current_balance`, chuyển DO sang `COMPLETED`.       |
| 2   | Finance           | Daily Credit-Hold Job            | Cron job cuối ngày quét toàn bộ hóa đơn `UNPAID`/`PARTIALLY_PAID` quá hạn thanh toán > `CREDIT_HOLD_OVERDUE_DAYS` (mặc định 30 ngày), tự động cập nhật `dealers.credit_status = 'CREDIT_HOLD'`. |
| 3   | Reports & Alerts  | Low Stock Alert Trigger/Resolve  | Service tính lại tồn khả dụng mỗi khi có biến động (nhập/xuất/điều chuyển/điều chỉnh); tạo `stock_alerts` khi dưới `reorder_point`, tự đóng khi hồi phục.                                       |
| 4   | Outbound Delivery | Delivery OTP Email Sending       | Service gửi OTP 6 số qua email cho Đại lý khi tài xế yêu cầu xác thực giao hàng; hiệu lực 5 phút, không lưu raw OTP.                                                                            |
| 5   | Finance           | Monthly Period Closing           | Batch job hỗ trợ Kế toán trưởng khóa cứng toàn bộ chứng từ có `transaction_date` trong kỳ đã đóng.                                                                                              |
| 6   | Master Data       | COGS Auto-Calculation            | Service snapshot giá vốn/giá bán từ `price_history` khi Planner tạo Delivery Order.                                                                                                             |
| 7   | Transfer          | Trip Overdue Detection           | Service tính overdue flag cho `TTR-*` khi quá hạn dự kiến còn `IN_TRANSIT`, chặn receive tại kho đích cho tới khi có Return to Source.                                                          |
| 8   | Security          | JWT Refresh & Session Validation | Filter xác thực JWT trước mọi request (trừ `/auth/login`, `/auth/refresh`, Swagger).                                                                                                            |

## 3. System High Level Design

### 3.1 Database Design

#### a. Database Schema

Schema chính chia theo 10 domain: `Security & Audit`, `Master Data`, `Inbound`, `Outbound & Delivery`, `Transfer`, `Stocktake`, `Pricing`, `Finance`, `Returns/Disposal`, `Reporting/Alerts`. Toàn bộ migration quản lý bằng Flyway tại `backend/src/main/resources/db/migration/`; entity JPA tương ứng tại `backend/src/main/java/com/wms/entity/`. Chi tiết ERD nên được vẽ bằng công cụ diagram riêng dựa trên bảng mô tả bên dưới; đây là các nhóm quan hệ chính:

```
users ──< user_warehouse_assignments >── warehouses ──< warehouse_locations
warehouses ──< receipts ──< receipt_items >── products ──< batches
warehouses ──< delivery_orders ──< delivery_order_items >── delivery_order_item_allocations
delivery_orders ──< deliveries ──< delivery_otp_attempts
warehouses ──< transfers ──< transfer_items
warehouses ──< inventories >── products >── batches
warehouses ──< stocktakes ──< stocktake_items
products ──< price_history
dealers ──< invoices ──< invoice_lines
dealers ──< payment_receipts
dealers ──< credit_notes
receipts/transfers ──< adjustments
receipts ──< debit_notes (RTV)
transfers/receipts ──< damage_reports (disposal)
all mutating tables ──> audit_logs (append-only)
```

#### b. Table Descriptions

| No  | Table                             | Description                                                                                                                                                                                   |
| --- | --------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `users`                           | Tài khoản người dùng.<br>- Primary keys: `id`<br>- Foreign keys: none                                                                                                                         |
| 02  | `user_warehouse_assignments`      | Gán user vào 1+ kho vật lý cho RBAC warehouse-scope.<br>- PK: `id`<br>- FK: `user_id`, `warehouse_id`                                                                                         |
| 03  | `warehouses`                      | 3 kho vật lý (Hải Phòng/Hà Nội/HCM) + kho ảo `IN_TRANSIT`.<br>- PK: `id`<br>- FK: `manager_id` (Trưởng kho)                                                                                   |
| 04  | `warehouse_locations`             | Zone + Bin Location, có `is_quarantine`, `capacity`.<br>- PK: `id`<br>- FK: `warehouse_id`                                                                                                    |
| 05  | `products`                        | Danh mục SKU (đồ gia dụng — không serial, không hạn dùng, không grade).<br>- PK: `id`<br>- FK: none                                                                                           |
| 06  | `price_history`                   | Giá vốn/giá bán theo kỳ hiệu lực.<br>- PK: `id`<br>- FK: `product_id`                                                                                                                         |
| 07  | `batches`                         | Lô hàng theo sản phẩm + nguồn nhập + ngày nhận (FIFO key).<br>- PK: `id`<br>- FK: `product_id`, `receipt_id`                                                                                  |
| 08  | `inventories`                     | Tồn kho theo warehouse + product + batch + location; invariant `total_qty >= 0`, `reserved_qty >= 0`.<br>- PK: `id`<br>- FK: `warehouse_id`, `product_id`, `batch_id`, `location_id`          |
| 09  | `receipts`                        | Phiếu nhập kho (`PURCHASE`) và hoàn trả Đại lý (`RETURN`).<br>- PK: `id`<br>- FK: `warehouse_id`, `dealer_id`                                                                                 |
| 10  | `receipt_items`                   | Dòng hàng phiếu nhập, gồm `expected_qty`/`actual_qty`/`over_received_qty`.<br>- PK: `id`<br>- FK: `receipt_id`, `product_id`                                                                  |
| 11  | `adjustments`                     | Điều chỉnh tồn kho (DISPOSAL, RETURN_TO_VENDOR, TRANSFER_DISCREPANCY, STOCKTAKE...).<br>- PK: `id`<br>- FK: `warehouse_id`, `product_id`, tùy loại: `receipt_id`/`transfer_id`/`stocktake_id` |
| 12  | `debit_notes`                     | Chứng từ đòi bồi hoàn NCC khi RTV.<br>- PK: `id`<br>- FK: `receipt_id`, `supplier_id`                                                                                                         |
| 13  | `delivery_orders`                 | Đơn xuất hàng (DO), trạng thái `NEW → ... → COMPLETED/CLOSED`.<br>- PK: `id`<br>- FK: `warehouse_id`, `dealer_id`, `created_by`                                                               |
| 14  | `delivery_order_items`            | Dòng hàng của DO, chứa `unit_price` snapshot.<br>- PK: `id`<br>- FK: `do_id`, `product_id`                                                                                                    |
| 15  | `delivery_order_item_allocations` | Kế hoạch lấy hàng theo batch/bin/zone (FIFO).<br>- PK: `id`<br>- FK: `do_item_id`, `batch_id`, `location_id`                                                                                  |
| 16  | `trips`                           | Chuyến xe nội bộ (`DELIVERY` hoặc `TRANSFER`).<br>- PK: `id`<br>- FK: `vehicle_id`, `driver_id`, `warehouse_id`                                                                               |
| 17  | `deliveries`                      | Lần giao vật lý (attempt) của 1 DO.<br>- PK: `id`<br>- FK: `do_id`, `trip_id`, `driver_id`                                                                                                    |
| 18  | `delivery_otp_attempts`           | OTP xác thực giao hàng, chỉ lưu hash/verifier.<br>- PK: `id`<br>- FK: `delivery_id`                                                                                                           |
| 19  | `transfers`                       | Phiếu điều chuyển kho nội bộ (`TRF-*`).<br>- PK: `id`<br>- FK: `source_warehouse_id`, `destination_warehouse_id`                                                                              |
| 20  | `transfer_items`                  | Dòng hàng phiếu điều chuyển.<br>- PK: `id`<br>- FK: `transfer_id`, `product_id`, `batch_id`                                                                                                   |
| 21  | `stocktakes`                      | Phiếu kiểm kê định kỳ.<br>- PK: `id`<br>- FK: `warehouse_id`, `created_by`                                                                                                                    |
| 22  | `stocktake_items`                 | Dòng đếm thực tế, tính Variance.<br>- PK: `id`<br>- FK: `stocktake_id`, `product_id`                                                                                                          |
| 23  | `dealers`                         | Đại lý — công nợ, Credit Limit, kỳ hạn thanh toán.<br>- PK: `id`<br>- FK: none                                                                                                                |
| 24  | `suppliers`                       | Nhà cung cấp — phục vụ Receipt/RTV/Debit Note.<br>- PK: `id`<br>- FK: none                                                                                                                    |
| 25  | `vehicles`                        | Xe tải nội bộ Phúc Anh — tải trọng, thể tích.<br>- PK: `id`<br>- FK: none                                                                                                                     |
| 26  | `drivers`                         | Tài xế nội bộ — phạm vi kho hoạt động.<br>- PK: `id`<br>- FK: `user_id`, `warehouse_id`                                                                                                       |
| 27  | `invoices`                        | Hóa đơn bán hàng, tạo tự động khi DO `COMPLETED`.<br>- PK: `id`<br>- FK: `do_id`, `dealer_id`                                                                                                 |
| 28  | `invoice_lines`                   | Chi tiết dòng hóa đơn.<br>- PK: `id`<br>- FK: `invoice_id`, `product_id`                                                                                                                      |
| 29  | `payment_receipts`                | Phiếu thu tiền từ Đại lý.<br>- PK: `id`<br>- FK: `dealer_id`                                                                                                                                  |
| 30  | `credit_notes`                    | Ghi giảm công nợ khi hàng hoàn trả.<br>- PK: `id`<br>- FK: `dealer_id`, `receipt_id`                                                                                                          |
| 31  | `billing_notifications`           | Worklist theo dõi DO đã giao chờ đối chiếu invoice (đối chiếu thủ công, không phải bước tạo invoice bắt buộc).<br>- PK: `id`<br>- FK: `do_id`                                                 |
| 32  | `accounting_periods`              | Kỳ kế toán tháng, trạng thái `OPEN`/`CLOSED`.<br>- PK: `id`<br>- FK: none                                                                                                                     |
| 33  | `damage_reports`                  | Biên bản hư hỏng phục vụ tiêu hủy.<br>- PK: `id`<br>- FK: `adjustment_id`                                                                                                                     |
| 34  | `stock_alerts`                    | Cảnh báo tồn kho thấp tự động.<br>- PK: `id`<br>- FK: `warehouse_id`, `product_id`                                                                                                            |
| 35  | `system_configs`                  | Tham số hệ thống (key-value có kiểm soát).<br>- PK: `id`<br>- FK: none                                                                                                                        |
| 36  | `audit_logs`                      | Nhật ký hoạt động, append-only.<br>- PK: `id`<br>- FK: `actor_id` (nullable cho `SYSTEM`)                                                                                                     |

### 3.2 Code Packages

Backend theo layered architecture **Controller → Service → Repository → Entity**, main package `com.wms`.

**_Package descriptions_**

| No  | Package                                                             | Description                                                                                          |
| --- | ------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------- |
| 01  | `com.wms.controller`                                                | REST controllers, validation boundary, Swagger annotations, không chứa business logic                |
| 02  | `com.wms.service` / `com.wms.service.impl`                          | Business logic, transaction management, authorization, audit logging, invariant enforcement          |
| 03  | `com.wms.repository`                                                | Spring Data JPA repositories, không dùng raw SQL                                                     |
| 04  | `com.wms.entity`                                                    | JPA entity mapping database tables và relationships                                                  |
| 05  | `com.wms.dto.request` / `com.wms.dto.response` / `com.wms.dto.auth` | DTO cho request (Jakarta Validation) và response                                                     |
| 06  | `com.wms.enums`                                                     | Domain constants và state machines (VD: `ReceiptStatus`, `UserRole`, `InterWarehouseTransferStatus`) |
| 07  | `com.wms.config`                                                    | Security, JWT filter, Flyway, mail, JPA config, `UploadResourceConfig`                               |
| 08  | `com.wms.exception`                                                 | Typed exceptions và `GlobalExceptionHandler`                                                         |
| 09  | `com.wms.mapper`                                                    | DTO/entity mapping helpers                                                                           |
| 10  | `com.wms.util`                                                      | Focused utilities (VD: `FIFOSelector`), không có hidden business mutation                            |
| 11  | `com.wms.aop`                                                       | Cross-cutting concerns (khi có)                                                                      |

Frontend theo cấu trúc React 18 + Tailwind: `components/` (PascalCase), `pages/`, `hooks/` (camelCase), `services/` (API calls), `stores/` (Zustand), `utils/`.

---

# II. Requirement Specifications

> **Phạm vi phần này:** tài liệu viết đầy đủ chi tiết (Functional Description + Business Rules) cho nhóm nghiệp vụ đại diện **Spec 003 – Inbound Receipt & QC**, theo đúng mẫu template. Các nhóm nghiệp vụ còn lại (Spec 001, 002, 004–010) đã có danh sách Use Case đầy đủ tại mục I.1.2; Spec 011–012 là quality/testing cross-cutting. Khi cần, viết tiếp Functional Description theo cùng mẫu cho từng UC trong danh sách đó.

## 1. Inbound Receipt & QC (Spec 003)

### 1.1 UC-09_Create Purchase Receipt

#### a. Functionalities

**Functional Description Template**

| UC ID and Name:    | UC-09_Create Purchase Receipt                                                                                                                                                                                                                                                                                                                                                                                                                                |                   |            |
| ------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----------------- | ---------- |
| Created By:        | WMS Dev Team                                                                                                                                                                                                                                                                                                                                                                                                                                                 | Date Created:     | 2026-07-14 |
| Primary Actor:     | Planner                                                                                                                                                                                                                                                                                                                                                                                                                                                      | Secondary Actors: | None       |
| Trigger:           | Planner nhận thông tin hàng về từ Công ty mẹ qua Zalo/Email                                                                                                                                                                                                                                                                                                                                                                                                  |                   |            |
| Description:       | Planner nhập thông tin lệnh nhập kho lên hệ thống ở trạng thái chờ tiếp nhận, làm cơ sở để Nhân viên kho đếm hàng thực tế khi hàng đến                                                                                                                                                                                                                                                                                                                       |                   |            |
| Preconditions:     | PRE-1: Planner đã đăng nhập với role `PLANNER`.<br>PRE-2: SKU trong lệnh nhập đã tồn tại trong danh mục sản phẩm.                                                                                                                                                                                                                                                                                                                                            |                   |            |
| Postconditions:    | POST-1: Bản ghi `receipts` được tạo với `status = 'PENDING_RECEIPT'`.<br>POST-2: Các `receipt_items` tương ứng được tạo với `expected_qty` theo lệnh nhập.                                                                                                                                                                                                                                                                                                   |                   |            |
| Normal Flow:       | 9.0 Create Purchase Receipt<br>1. Planner truy cập màn hình Receipt List, chọn "Tạo Lệnh nhập"<br>2. Planner nhập kho nhận hàng, mã lệnh/nguồn gốc (`source_order_code`), danh sách SKU + số lượng dự kiến<br>3. Planner xác nhận tạo<br>4. Hệ thống validate dữ liệu đầu vào (xem 9.0.E1)<br>5. Hệ thống tạo `receipts` (`PENDING_RECEIPT`) và `receipt_items`<br>6. Hệ thống ghi audit log `RECEIPT_CREATE`<br>7. Hệ thống hiển thị Receipt Detail vừa tạo |                   |            |
| Alternative Flows: | None                                                                                                                                                                                                                                                                                                                                                                                                                                                         |                   |            |
| Exceptions:        | 9.0.E1 Dữ liệu đầu vào không hợp lệ (SKU không tồn tại, số lượng ≤ 0)<br>1. Hệ thống trả lỗi `VALIDATION_ERROR` (400)<br>2. Không tạo bản ghi nào                                                                                                                                                                                                                                                                                                            |                   |            |
| Priority:          | Must Have                                                                                                                                                                                                                                                                                                                                                                                                                                                    |                   |            |
| Frequency of Use:  | ~30–50 lệnh nhập/tháng/kho (ước tính theo scale 1000+ giao dịch/tháng toàn hệ thống)                                                                                                                                                                                                                                                                                                                                                                         |                   |            |
| Business Rules:    | BR-INV-01, BR-INV-04                                                                                                                                                                                                                                                                                                                                                                                                                                         |                   |            |
| Other Information: | Không kiểm QC ở bước này.                                                                                                                                                                                                                                                                                                                                                                                                                                    |                   |            |
| Assumptions:       | Planner nhận thông tin chính xác từ Công ty mẹ; hệ thống không xác thực chéo với hệ thống Công ty mẹ trong Sprint 1.                                                                                                                                                                                                                                                                                                                                         |                   |            |

#### b. Business Rules

| ID        | Business Rule              | Business Rule Description                                                                    |
| --------- | -------------------------- | -------------------------------------------------------------------------------------------- |
| BR-INV-01 | Non-negative inventory     | `inventories.total_qty >= 0`, `reserved_qty >= 0`, `total_qty - reserved_qty >= 0` luôn đúng |
| BR-INV-04 | No direct inventory update | Điều chỉnh tồn kho chỉ được đi qua receipt/issue/transfer/adjustment/stocktake flow          |

### 1.2 UC-10_Record Physical Receive Count

#### a. Functionalities

| UC ID and Name:    | UC-10_Record Physical Receive Count                                                                                                                                                                                                                                                                                                                                                                                                                                                            |                   |            |
| ------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------- | ---------- |
| Created By:        | WMS Dev Team                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   | Date Created:     | 2026-07-14 |
| Primary Actor:     | Nhân viên kho (WAREHOUSE_STAFF)                                                                                                                                                                                                                                                                                                                                                                                                                                                                | Secondary Actors: | None       |
| Trigger:           | Hàng hóa thực tế được giao đến kho                                                                                                                                                                                                                                                                                                                                                                                                                                                             |                   |            |
| Description:       | Nhân viên kho đếm số lượng nhận được cho từng dòng hàng và nhập vào phiếu nhận hàng; feature này chỉ đếm số lượng, không QC                                                                                                                                                                                                                                                                                                                                                                    |                   |            |
| Preconditions:     | PRE-1: Receipt đang ở trạng thái `PENDING_RECEIPT`, `DRAFT`, `QC_COMPLETED`, hoặc `QC_FAILED`.<br>PRE-2: User có role `WAREHOUSE_STAFF` và được gán vào kho của Receipt.                                                                                                                                                                                                                                                                                                                       |                   |            |
| Postconditions:    | POST-1: `actual_qty`/`over_received_qty` được tính và lưu cho mọi dòng hàng.<br>POST-2: Receipt chuyển `DRAFT` (nếu từ `PENDING_RECEIPT`) hoặc quay lại `DRAFT` (nếu sửa sau khi đã có QC).                                                                                                                                                                                                                                                                                                    |                   |            |
| Normal Flow:       | 10.0 Record Complete Count<br>1. Nhân viên kho mở Receipt Detail đang `PENDING_RECEIPT`<br>2. Nhân viên kho nhập `counted_qty` cho TẤT CẢ dòng hàng (bắt buộc đủ, không được thiếu dòng)<br>3. Nhân viên kho xác nhận gửi<br>4. Hệ thống validate (xem 10.0.E1, 10.0.E2)<br>5. Hệ thống tính `actual_qty = min(counted_qty, expected_qty)`, `over_received_qty = max(counted_qty - expected_qty, 0)`<br>6. Hệ thống cập nhật `status = 'DRAFT'`<br>7. Hệ thống ghi audit log `RECEIPT_RECEIVE` |                   |            |
| Alternative Flows: | 10.1 Correct Count Before Manager Decision<br>1. Nhân viên kho sửa `counted_qty` khi Receipt chưa `APPROVED`/`RETURN_TO_SUPPLIER_PENDING`<br>2. Nếu Receipt đã có dữ liệu QC, hệ thống xóa `qc_result`/`sample_*`/`qc_failure_reason` và đưa Receipt về `DRAFT`<br>3. Return to step 5 of normal flow                                                                                                                                                                                          |                   |            |
| Exceptions:        | 10.0.E1 Thiếu dòng hàng trong request<br>1. Hệ thống trả `RECEIPT_COUNT_INCOMPLETE` (422), không lưu thay đổi<br>10.0.E2 `counted_qty` không hợp lệ (≤0 hoặc không nguyên)<br>1. Hệ thống trả `INVALID_RECEIPT_COUNT` (422), không lưu thay đổi<br>10.1.E1 Receipt đã `APPROVED`/`RETURN_TO_SUPPLIER_PENDING`<br>1. Hệ thống trả `RECEIPT_ALREADY_FINALIZED` (409)                                                                                                                             |                   |            |
| Priority:          | Must Have                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |                   |            |
| Frequency of Use:  | Mỗi lần nhận hàng thực tế, tương ứng số lượng lệnh nhập/tháng                                                                                                                                                                                                                                                                                                                                                                                                                                  |                   |            |
| Business Rules:    | BR-INV-01                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |                   |            |
| Other Information: | Feature này KHÔNG tạo batch, KHÔNG tăng tồn kho, KHÔNG đưa hàng vào quarantine, KHÔNG putaway.                                                                                                                                                                                                                                                                                                                                                                                                 |                   |            |
| Assumptions:       | Thiết bị quét mã vạch chưa sẵn có; nhập liệu thủ công là chính (LESSON-004).                                                                                                                                                                                                                                                                                                                                                                                                                   |                   |            |

#### b. Business Rules

None ngoài BR-INV-01 đã liệt kê ở trên.

### 1.3 UC-11_Perform Inbound QC Inspection

#### a. Functionalities

| UC ID and Name:    | UC-11_Perform Inbound QC Inspection                                                                                                                                                                                                                                                                    |                   |                       |
| ------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----------------- | --------------------- |
| Created By:        | WMS Dev Team                                                                                                                                                                                                                                                                                           | Date Created:     | 2026-07-14            |
| Primary Actor:     | Nhân viên kho (WAREHOUSE_STAFF)                                                                                                                                                                                                                                                                        | Secondary Actors: | Thủ kho (STOREKEEPER) |
| Trigger:           | Receipt ở trạng thái `DRAFT` sau khi đã đếm hàng xong                                                                                                                                                                                                                                                  |                   |                       |
| Description:       | Nhân viên kho hoặc Thủ kho kiểm tra ngoại quan/chất lượng mẫu và ghi nhận kết quả Đạt/Lỗi cho từng dòng hàng                                                                                                                                                                                           |                   |                       |
| Preconditions:     | PRE-1: Receipt đang `DRAFT` với `actual_qty` đã được ghi nhận đầy đủ.                                                                                                                                                                                                                                  |                   |                       |
| Postconditions:    | POST-1: Receipt chuyển `QC_COMPLETED` (có dòng đạt) hoặc `QC_FAILED` (toàn bộ lỗi), theo kết quả kiểm tra.<br>POST-2: KHÔNG tăng tồn kho thường hoặc tồn kho Quarantine ở bước này (chờ Trưởng kho quyết định).                                                                                        |                   |                       |
| Normal Flow:       | 11.0 Record QC Result<br>1. Nhân viên kho/Thủ kho mở QC Inbound Form của Receipt `DRAFT`<br>2. Nhập kết quả Đạt/Lỗi + lý do chi tiết cho từng dòng hàng<br>3. Xác nhận gửi<br>4. Hệ thống validate và cập nhật `qc_result`, chuyển trạng thái Receipt<br>5. Hệ thống ghi audit log `RECEIPT_QC_RECORD` |                   |                       |
| Alternative Flows: | None                                                                                                                                                                                                                                                                                                   |                   |                       |
| Exceptions:        | 11.0.E1 Receipt không ở trạng thái `DRAFT`<br>1. Hệ thống trả `INVALID_RECEIPT_STATUS` (409)                                                                                                                                                                                                           |                   |                       |
| Priority:          | Must Have                                                                                                                                                                                                                                                                                              |                   |                       |
| Frequency of Use:  | Mỗi Receipt sau khi đếm hàng                                                                                                                                                                                                                                                                           |                   |                       |
| Business Rules:    | BR-QC-01, BR-QC-02                                                                                                                                                                                                                                                                                     |                   |                       |
| Other Information: | Domain hàng gia dụng: không lấy mẫu theo hạn sử dụng/FEFO.                                                                                                                                                                                                                                             |                   |                       |
| Assumptions:       | None                                                                                                                                                                                                                                                                                                   |                   |                       |

#### b. Business Rules

| ID       | Business Rule        | Business Rule Description                                                                          |
| -------- | -------------------- | -------------------------------------------------------------------------------------------------- |
| BR-QC-01 | QC Gate              | Hàng nhập kho phải qua QC trước khi được tính vào available inventory                              |
| BR-QC-02 | Quarantine Exclusion | Hàng fail QC không được tính vào available inventory cho tới khi được xử lý theo flow RTV/disposal |

### 1.4 UC-12_Handle Quarantine & RTV

#### a. Functionalities

| UC ID and Name:    | UC-12_Handle Quarantine & RTV                                                                                                                                                                                                                                                                                     |                   |            |
| ------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------- | ---------- |
| Created By:        | WMS Dev Team                                                                                                                                                                                                                                                                                                      | Date Created:     | 2026-07-14 |
| Primary Actor:     | Trưởng kho                                                                                                                                                                                                                                                                                                        | Secondary Actors: | Thủ kho    |
| Trigger:           | Receipt có dòng hàng `QC_FAILED`                                                                                                                                                                                                                                                                                  |                   |            |
| Description:       | Trưởng kho quyết định trả hàng lỗi cho NCC (RTV); Thủ kho xác nhận đã bàn giao đủ hàng cho xe NCC                                                                                                                                                                                                                 |                   |            |
| Preconditions:     | PRE-1: Receipt có tồn Quarantine liên quan đến dòng `QC_FAILED`.                                                                                                                                                                                                                                                  |                   |            |
| Postconditions:    | POST-1: `debit_notes` được tạo cho NCC.<br>POST-2: `adjustments` loại `RETURN_TO_VENDOR` được tạo, ở trạng thái pending cho tới khi Thủ kho xác nhận.<br>POST-3: Sau xác nhận, tồn Quarantine bị trừ đúng bằng số lượng RTV.                                                                                      |                   |            |
| Normal Flow:       | 12.0 Create & Confirm RTV<br>1. Trưởng kho mở Quarantine/RTV Detail, chọn "Trả NCC"<br>2. Hệ thống tạo `debit_notes` + `adjustments (RETURN_TO_VENDOR)` pending<br>3. Thủ kho xác nhận đã bàn giao ĐỦ số lượng cho xe NCC (xem 12.0.E1)<br>4. Hệ thống trừ tồn Quarantine, ghi audit log `QUARANTINE_RTV_CONFIRM` |                   |            |
| Alternative Flows: | None                                                                                                                                                                                                                                                                                                              |                   |            |
| Exceptions:        | 12.0.E1 Xác nhận bàn giao thiếu/thừa số lượng<br>1. Hệ thống trả lỗi 422, không cho xác nhận một phần                                                                                                                                                                                                             |                   |            |
| Priority:          | Must Have                                                                                                                                                                                                                                                                                                         |                   |            |
| Frequency of Use:  | Theo tỷ lệ hàng lỗi QC thực tế (thường thấp)                                                                                                                                                                                                                                                                      |                   |            |
| Business Rules:    | BR-QC-02, BR-INV-04                                                                                                                                                                                                                                                                                               |                   |            |
| Other Information: | Feature 003 CHỈ xử lý RTV; luồng tiêu hủy hàng lỗi thuộc Spec 009.                                                                                                                                                                                                                                                |                   |            |
| Assumptions:       | None                                                                                                                                                                                                                                                                                                              |                   |            |

#### b. Business Rules

Đã liệt kê ở trên (BR-QC-02, BR-INV-04).

### 1.5 UC-13_Approve/Reject Receipt & Putaway

#### a. Functionalities

| UC ID and Name:    | UC-13_Approve/Reject Receipt & Putaway                                                                                                                                                                                                                                                                                                                                                                      |                   |            |
| ------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------- | ---------- |
| Created By:        | WMS Dev Team                                                                                                                                                                                                                                                                                                                                                                                                | Date Created:     | 2026-07-14 |
| Primary Actor:     | Trưởng kho                                                                                                                                                                                                                                                                                                                                                                                                  | Secondary Actors: | Thủ kho    |
| Trigger:           | Receipt ở trạng thái `QC_COMPLETED`                                                                                                                                                                                                                                                                                                                                                                         |                   |            |
| Description:       | Trưởng kho phê duyệt hoặc từ chối phiếu nhập; sau khi duyệt, Thủ kho cất hàng vào Bin để hoàn tất nhập kho                                                                                                                                                                                                                                                                                                  |                   |            |
| Preconditions:     | PRE-1: Receipt đang `QC_COMPLETED`.                                                                                                                                                                                                                                                                                                                                                                         |                   |            |
| Postconditions:    | POST-1 (Approve): Receipt chuyển `APPROVED`, mở khóa putaway (chưa cộng tồn).<br>POST-2 (Putaway): Sau khi Thủ kho cất Bin, `inventories.total_qty` tăng tương ứng.<br>POST-3 (Reject): Receipt chuyển `RETURN_TO_SUPPLIER_PENDING`, lưu lý do; không tạo inventory/batch/RTV/Debit Note.                                                                                                                   |                   |            |
| Normal Flow:       | 13.0 Approve & Putaway<br>1. Trưởng kho mở Receipt Approval Detail, xem kết quả QC<br>2. Trưởng kho bấm "Duyệt nhập"<br>3. Hệ thống cập nhật Receipt `APPROVED`, ghi audit `RECEIPT_APPROVE`<br>4. Thủ kho mở Putaway Form, chọn Bin cho từng dòng hàng đạt (kiểm tra bin capacity — xem 13.0.E1)<br>5. Hệ thống tạo/cập nhật `batches`, tăng `inventories.total_qty`, ghi audit `RECEIPT_PUTAWAY_COMPLETE` |                   |            |
| Alternative Flows: | 13.1 Reject Receipt<br>1. Trưởng kho bấm "Từ chối", nhập lý do bắt buộc<br>2. Hệ thống chuyển Receipt `RETURN_TO_SUPPLIER_PENDING`, ghi audit `RECEIPT_REJECT`<br>3. Thủ kho xác nhận đã bàn giao xe NCC → Receipt chuyển `RETURNED_TO_SUPPLIER` (audit `RECEIPT_RETURN_CONFIRM`)                                                                                                                           |                   |            |
| Exceptions:        | 13.0.E1 Bin không đủ sức chứa<br>1. Hệ thống trả lỗi 422, không cho chọn Bin đó                                                                                                                                                                                                                                                                                                                             |                   |            |
| Priority:          | Must Have                                                                                                                                                                                                                                                                                                                                                                                                   |                   |            |
| Frequency of Use:  | Mỗi Receipt sau QC                                                                                                                                                                                                                                                                                                                                                                                          |                   |            |
| Business Rules:    | BR-BAT-02, BR-INV-01, BR-INV-04                                                                                                                                                                                                                                                                                                                                                                             |                   |            |
| Other Information: | Đây là bước duy nhất tăng available inventory trong luồng inbound.                                                                                                                                                                                                                                                                                                                                          |                   |            |
| Assumptions:       | None                                                                                                                                                                                                                                                                                                                                                                                                        |                   |            |

#### b. Business Rules

| ID        | Business Rule      | Business Rule Description                                                                |
| --------- | ------------------ | ---------------------------------------------------------------------------------------- |
| BR-BAT-02 | Bin Capacity Check | Putaway phải kiểm tra `bin_capacity` trước khi đặt hàng vào Bin, không cho vượt sức chứa |

---

# III. Design Specifications

> **Phạm vi phần này:** thiết kế UI/DB Access/SQL cho các màn hình chính của **Spec 003 – Inbound Receipt & QC**, theo đúng mẫu template. Các nhóm nghiệp vụ khác áp dụng cùng mẫu khi triển khai.

## 1. Inbound Receipt & QC

### 1.1 Receipt Management

#### a. Receipt Detail (Count) Screen

Màn hình cho Nhân viên kho đếm số lượng hàng thực tế; liên quan UC-09, UC-10.

Related use cases:

- UC-10_Record Physical Receive Count

UI Design

| Field Name        | Field Type                    | Description                                                  |
| ----------------- | ----------------------------- | ------------------------------------------------------------ |
| Receipt Number    | Text (read-only)              | Mã phiếu nhập, tự sinh khi tạo Receipt                       |
| Warehouse         | Text (read-only)              | Kho nhận hàng                                                |
| Status Badge      | Label                         | Hiển thị trạng thái hiện tại (`PENDING_RECEIPT`/`DRAFT`/...) |
| Item Table        |                               |                                                              |
| SKU               | Text (read-only)              | Mã sản phẩm                                                  |
| Expected Qty      | Integer (read-only)           | Số lượng dự kiến theo lệnh nhập                              |
| Counted Qty\*     | Text Box, Integer (>0)        | Số lượng đếm thực tế Nhân viên kho nhập                      |
| Over-received Qty | Integer (read-only, computed) | = max(Counted Qty − Expected Qty, 0)                         |
| Submit Count      | Button                        | Gửi toàn bộ số lượng đã đếm cho tất cả dòng hàng             |

Database Access

| Table           | CRUD | Description                                                   |
| --------------- | ---- | ------------------------------------------------------------- |
| `receipts`      | RU   | Đọc thông tin phiếu, cập nhật `status` sau khi đếm xong       |
| `receipt_items` | RU   | Đọc `expected_qty`, cập nhật `actual_qty`/`over_received_qty` |
| `audit_logs`    | C    | Ghi log `RECEIPT_RECEIVE` với before/after                    |

**_SQL Commands_**

1/ Đọc phiếu nhập và các dòng hàng

```sql
SELECT r.id, r.receipt_number, r.status, r.warehouse_id
FROM receipts r WHERE r.id = ?

SELECT ri.id, ri.product_id, ri.expected_qty, ri.actual_qty, ri.over_received_qty
FROM receipt_items ri WHERE ri.receipt_id = ?
```

2/ Cập nhật số lượng đếm và trạng thái phiếu

```sql
UPDATE receipt_items
SET actual_qty = ?, over_received_qty = ?
WHERE id = ? AND receipt_id = ?

UPDATE receipts
SET status = 'DRAFT', updated_at = NOW()
WHERE id = ? AND status IN ('PENDING_RECEIPT', 'DRAFT', 'QC_COMPLETED', 'QC_FAILED')
```

#### b. QC Inbound Form

UI Design

| Field Name     | Field Type          | Description                            |
| -------------- | ------------------- | -------------------------------------- |
| Item Table     |                     |                                        |
| SKU            | Text (read-only)    | Mã sản phẩm                            |
| Actual Qty     | Integer (read-only) | Số lượng đã đếm                        |
| QC Result\*    | Radio (Đạt / Lỗi)   | Kết quả kiểm tra ngoại quan/chất lượng |
| Failure Reason | Text Area           | Bắt buộc khi QC Result = Lỗi           |
| Submit QC      | Button              | Gửi kết quả QC cho toàn bộ dòng hàng   |

Database Access

| Table           | CRUD | Description                                                 |
| --------------- | ---- | ----------------------------------------------------------- |
| `receipt_items` | RU   | Đọc `actual_qty`, cập nhật `qc_result`, `qc_failure_reason` |
| `receipts`      | RU   | Cập nhật `status` sang `QC_COMPLETED`/`QC_FAILED`           |
| `audit_logs`    | C    | Ghi log `RECEIPT_QC_RECORD`                                 |

**_SQL Commands_**

```sql
UPDATE receipt_items
SET qc_result = ?, qc_failure_reason = ?
WHERE id = ? AND receipt_id = ?

UPDATE receipts
SET status = ?, updated_at = NOW()
WHERE id = ? AND status = 'DRAFT'
```

#### c. Receipt Approval Detail

UI Design

| Field Name    | Field Type              | Description                                           |
| ------------- | ----------------------- | ----------------------------------------------------- |
| QC Summary    | Table (read-only)       | Tổng hợp số lượng Đạt/Lỗi theo từng SKU               |
| Decision\*    | Radio (Duyệt / Từ chối) | Quyết định của Trưởng kho                             |
| Reject Reason | Text Area               | Bắt buộc khi Decision = Từ chối                       |
| Approve       | Button                  | Xác nhận duyệt, mở khóa putaway                       |
| Reject        | Button                  | Xác nhận từ chối, chuyển `RETURN_TO_SUPPLIER_PENDING` |

Database Access

| Table        | CRUD | Description                                                    |
| ------------ | ---- | -------------------------------------------------------------- |
| `receipts`   | RU   | Cập nhật `status` sang `APPROVED`/`RETURN_TO_SUPPLIER_PENDING` |
| `audit_logs` | C    | Ghi log `RECEIPT_APPROVE`/`RECEIPT_REJECT`                     |

**_SQL Commands_**

```sql
UPDATE receipts
SET status = 'APPROVED', approved_by = ?, approved_at = NOW()
WHERE id = ? AND status = 'QC_COMPLETED'

UPDATE receipts
SET status = 'RETURN_TO_SUPPLIER_PENDING', reject_reason = ?
WHERE id = ? AND status = 'QC_COMPLETED'
```

#### d. Putaway Form

UI Design

| Field Name      | Field Type                | Description                                                  |
| --------------- | ------------------------- | ------------------------------------------------------------ |
| Item Table      |                           |                                                              |
| SKU             | Text (read-only)          | Mã sản phẩm cần cất                                          |
| Qty to Putaway  | Integer (read-only)       | Số lượng đạt QC cần cất Bin                                  |
| Bin Location\*  | Combo Box (Single-Choice) | Danh sách Bin còn đủ sức chứa trong kho, loại Quarantine bin |
| Confirm Putaway | Button                    | Xác nhận cất hàng, cộng tồn kho                              |

Database Access

| Table                 | CRUD | Description                                                 |
| --------------------- | ---- | ----------------------------------------------------------- |
| `warehouse_locations` | R    | Kiểm tra `capacity` còn trống của Bin                       |
| `batches`             | CR   | Tạo/tìm batch theo product + receipt + received_date        |
| `inventories`         | CU   | Cộng `total_qty` cho warehouse + product + batch + location |
| `audit_logs`          | C    | Ghi log `RECEIPT_PUTAWAY_COMPLETE`                          |

**_SQL Commands_**

1/ Kiểm tra sức chứa Bin

```sql
SELECT wl.id, wl.capacity, wl.is_quarantine,
       COALESCE(SUM(i.total_qty), 0) AS current_qty
FROM warehouse_locations wl
LEFT JOIN inventories i ON i.location_id = wl.id
WHERE wl.id = ? AND wl.is_quarantine = false
GROUP BY wl.id, wl.capacity, wl.is_quarantine
```

2/ Cộng tồn kho sau putaway (optimistic locking qua `version`)

```sql
UPDATE inventories
SET total_qty = total_qty + ?, version = version + 1
WHERE warehouse_id = ? AND product_id = ? AND batch_id = ? AND location_id = ? AND version = ?
```

---

# IV. Appendix

## 1. Assumptions & Dependencies

- AS-1: Thiết bị quét mã vạch/QR chưa sẵn có trong Sprint 1; toàn bộ nhập liệu là thủ công (LESSON-004, CLAUDE.md).
- AS-2: Đội xe và tài xế nội bộ Phúc Anh đủ nguồn lực đáp ứng khối lượng ~1000+ giao dịch/tháng trên cả 3 kho.
- AS-3: Công ty mẹ gửi thông tin lệnh nhập/xuất qua Zalo/Email; hệ thống không tích hợp trực tiếp với hệ thống Công ty mẹ trong Sprint 1.
- DE-1: Hệ thống phụ thuộc PostgreSQL 18 (có thể dùng Supabase Postgres cho môi trường dev/test chia sẻ — không phải kiến trúc production, không dùng làm file storage).
- DE-2: Toàn bộ ảnh POD/QC lưu tại `/uploads` nội bộ trên server backend, không dùng object storage bên thứ ba.

## 2. Limitations & Exclusions

Theo `README.md`/`.specify/memory/constitution.md`, hệ thống **KHÔNG** bao gồm:

- Quản lý sản xuất (Manufacturing).
- HR / HRM (báo cáo năng suất chỉ export Excel, không tích hợp trực tiếp hệ thống nhân sự/lương).
- Barcode / QR Scanner (dự kiến tích hợp sau).
- Cổng B2B / B2C Portal.
- Tích hợp hệ thống bên ngoài (Công ty mẹ, hệ thống kế toán ngoài...).
- Vận tải thuê ngoài (3PL) — chỉ dùng xe nội bộ Phúc Anh, không có luồng Duyệt chi vận tải.
- Serial-level tracking, hạn sử dụng (expiry/FEFO), phân cấp chất lượng (grade A/B/C) cho hàng gia dụng.

## 3. Business Rules

| ID        | Category     | Rule Definition                                                                                                                                                                  |
| --------- | ------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| BR-INV-01 | Constraints  | `inventories.total_qty >= 0`, `reserved_qty >= 0`, `total_qty - reserved_qty >= 0` luôn đúng trước/sau mọi thao tác                                                              |
| BR-INV-02 | Facts        | FIFO theo `received_date` là nguyên tắc xuất kho mặc định và duy nhất cho domain hàng gia dụng                                                                                   |
| BR-INV-03 | Constraints  | Mọi UPDATE inventory phải dùng optimistic locking (`@Version`); conflict → HTTP 409                                                                                              |
| BR-INV-04 | Constraints  | Điều chỉnh tồn kho chỉ đi qua receipt/issue/transfer/adjustment/stocktake flow, không UPDATE trực tiếp                                                                           |
| BR-QC-01  | Constraints  | Hàng nhập/xuất kho phải qua QC trước khi tính vào available inventory                                                                                                            |
| BR-QC-02  | Constraints  | Hàng fail QC vào Quarantine, loại khỏi available inventory, chỉ rời Quarantine qua RTV/disposal đã duyệt                                                                         |
| BR-BAT-01 | Facts        | Batch gom theo sản phẩm + nguồn nhập/chứng từ + ngày nhận; không tách theo serial/hạn dùng/grade                                                                                 |
| BR-BAT-02 | Constraints  | Putaway phải kiểm tra `bin_capacity` trước khi đặt hàng vào Bin                                                                                                                  |
| BR-TRF-01 | Constraints  | Điều chuyển kho phải đi qua kho ảo `IN_TRANSIT` cho tới khi kho đích xác nhận nhận hàng                                                                                          |
| BR-TRF-02 | Constraints  | Chênh lệch `quantity_sent` vs `quantity_received` phải tạo `adjustment` + audit record                                                                                           |
| BR-FIN-01 | Computations | CREDIT*HOLD khi `current_balance + giá_trị*đơn_mới > credit_limit`HOẶC`current_balance > credit_limit` sau hóa đơn (bằng hạn mức vẫn cho phép) HOẶC có hóa đơn quá hạn > 30 ngày |
| BR-FIN-02 | Computations | Mở khóa tín dụng khi `current_balance < credit_limit × 0.8` (buffer 20%)                                                                                                         |
| BR-FIN-03 | Facts        | Chốt sổ kỳ khóa cứng chứng từ có `transaction_date` trong kỳ đã đóng; sửa sai chỉ qua Adjustment Voucher ở kỳ mở                                                                 |
| BR-SEC-01 | Constraints  | Authorization phải kiểm tra CẢ role LẪN warehouse assignment cho mọi thao tác phạm vi kho                                                                                        |
| BR-SEC-02 | Constraints  | Audit log là bằng chứng bất biến — append-only, không ai (kể cả System Admin) được sửa/xóa                                                                                       |
| BR-DEL-01 | Constraints  | Master data soft-delete bằng `is_active = false`; transaction data cancel bằng `status = CANCELLED`; không xóa vật lý                                                            |

## 4. Ghi chú mở rộng tài liệu

Danh sách Use Case đầy đủ cho các nhóm nghiệp vụ còn lại (Spec 001, 002, 004–010) đã có tại mục **I.1.2.b**. Spec 011–012 quy định test/quality gate và không tạo UI/DB use case nghiệp vụ. Khi cần viết chi tiết Functional Description + UI/DB Design cho một nhóm cụ thể, áp dụng đúng mẫu tại **Phần II mục 1** và **Phần III mục 1** của tài liệu này, lấy nội dung nghiệp vụ từ file spec tương ứng trong `.sdd/specs/<số>-<tên>/`.

---

# II. Functional Specifications

> **Phạm vi phần này:** Viết chi tiết Functional Description (Use Cases) + Business Rules cho các nhóm nghiệp vụ còn lại (Spec 001–002, 004–010), theo đúng mẫu template của Spec 003. Mỗi spec dùng mục riêng; trong mỗi mục liệt kê các UC chi tiết với điều kiện, luồng, ngoại lệ và bảng business rules đi kèm.

## 2. Spec 001: Authentication, RBAC & System Config (US-WMS-01, US-WMS-21)

### 2.1 UC-01: User Login

#### a. Functionalities

| UC ID and Name:    | UC-01_User Login                                    |                   |            |
| ------------------ | --------------------------------------------------- | ----------------- | ---------- |
| Created By:        | WMS Dev Team                                        | Date Created:     | 2026-07-14 |
| Primary Actor:     | Any User (email + password)                         | Secondary Actors: | System     |
| Trigger:           | User accesses the login page and submits credentials |                   |            |
| Description:       | User authenticates via email/password; system verifies credentials, hashes password with bcrypt cost ≥ 12, and issues JWT tokens (access: 15m, refresh: 7d) |                   |            |
| Preconditions:     | PRE-1: User account exists and is active (`is_active = true`)<br>PRE-2: User has not exceeded login attempts (rate limiting).     |                   |            |
| Postconditions:    | POST-1: Access token issued with TTL 15 minutes<br>POST-2: Refresh token issued with TTL 7 days and stored server-side (`users.refresh_token_hash`)<br>POST-3: User profile returned with assigned warehouses |                   |            |
| Normal Flow:       | 1. User enters email and password on login form<br>2. System validates email format and password strength (≥8 chars, mixed case + number)<br>3. System looks up user by email<br>4. System compares submitted password against bcrypt hash in DB<br>5. IF match: generate access + refresh tokens, store refresh_token_hash, return tokens + profile<br>6. IF no match: return HTTP 401 `INVALID_CREDENTIALS` |                   |            |
| Alternative Flows: | 1.1 Account Inactive<br>   • IF user.is_active = false, return HTTP 401 `ACCOUNT_INACTIVE` without revealing username |                   |            |
| Exceptions:        | E1: User email not found → HTTP 401 `INVALID_CREDENTIALS`<br>E2: Password mismatch → HTTP 401 `INVALID_CREDENTIALS`<br>E3: Too many failed login attempts in 15 min → HTTP 429 (rate limit)<br>E4: Validation error (bad email format) → HTTP 400 |                   |            |
| Priority:          | Must Have                                           |                   |            |
| Frequency of Use:  | Every user login, ~100–200 times/day across all warehouses |                   |            |
| Business Rules:    | BR-SEC-01, BR-SEC-04 (bcrypt cost ≥ 12), BR-SEC-05 (JWT strength) |                   |            |
| Other Information: | None                                                |                   |            |
| Assumptions:       | SMTP is configured for password reset emails. 2FA not in Sprint 1. |                   |            |

#### b. Business Rules

| ID | Rule | Description |
|----|------|-------------|
| BR-AUTH-01 | Password hashing | All user passwords MUST be hashed with bcrypt cost factor ≥ 12 |
| BR-AUTH-02 | Token TTL | Access token TTL = 15 minutes; Refresh token TTL = 7 days |
| BR-AUTH-03 | Refresh token server-side | Refresh tokens stored as hashed values on `users.refresh_token_hash`; invalidation on logout |
| BR-AUTH-04 | Rate limiting | Max 5 failed login attempts per 15 min per IP; HTTP 429 if exceeded |

---

### 2.2 UC-02: Refresh Access Token

| UC ID and Name:    | UC-02_Refresh Access Token          |                   |            |
| ------------------ | ----------------------------------- | ----------------- | ---------- |
| Created By:        | WMS Dev Team                        | Date Created:     | 2026-07-14 |
| Primary Actor:     | Any authenticated user              | Secondary Actors: | System     |
| Trigger:           | Access token nearing expiry or expired |                   |            |
| Description:       | User provides a valid refresh token to obtain a new access token without re-entering password |                   |            |
| Preconditions:     | PRE-1: Refresh token exists in `users.refresh_token_hash` and is not expired<br>PRE-2: Refresh token has not been invalidated (logout) |                   |            |
| Postconditions:    | POST-1: New access token issued with TTL 15 minutes<br>POST-2: Refresh token remains unchanged |                   |            |
| Normal Flow:       | 1. Client sends `POST /api/v1/auth/refresh` with `refreshToken`<br>2. System hashes submitted token and compares to `users.refresh_token_hash`<br>3. System checks expiry via `users.refresh_token_expires_at`<br>4. IF valid and not expired: issue new access token, return HTTP 200<br>5. IF not matching or expired: return HTTP 401 `TOKEN_EXPIRED` or `TOKEN_INVALID` |                   |            |
| Exceptions:        | E1: Token hash mismatch → HTTP 401 `TOKEN_INVALID`<br>E2: Token expired → HTTP 401 `TOKEN_EXPIRED`<br>E3: Account deactivated → HTTP 401 `ACCOUNT_INACTIVE` |                   |            |
| Priority:          | Must Have                           |                   |            |
| Frequency of Use:  | Every 15 min per active user session |                   |            |
| Business Rules:    | BR-AUTH-02, BR-AUTH-03              |                   |            |

#### b. Business Rules

Covered by UC-01 rules.

---

### 2.3 UC-03: Logout & Invalidate Token

| UC ID and Name:    | UC-03_Logout                        |                   |            |
| ------------------ | ----------------------------------- | ----------------- | ---------- |
| Created By:        | WMS Dev Team                        | Date Created:     | 2026-07-14 |
| Primary Actor:     | Any authenticated user              | Secondary Actors: | System     |
| Trigger:           | User clicks logout button or session timeout      |                   |            |
| Description:       | User logs out; system invalidates refresh token to prevent future reuse |                   |            |
| Preconditions:     | PRE-1: User has valid access token  |                   |            |
| Postconditions:    | POST-1: `users.refresh_token_hash` cleared to NULL<br>POST-2: `users.refresh_token_expires_at` cleared |                   |            |
| Normal Flow:       | 1. User calls `POST /api/v1/auth/logout` with access token in header<br>2. System verifies access token (not expired, signature valid)<br>3. System extracts user_id from token<br>4. System sets `users.refresh_token_hash = NULL`, `refresh_token_expires_at = NULL`<br>5. Return HTTP 204 No Content |                   |            |
| Exceptions:        | E1: Invalid access token → HTTP 401 `TOKEN_INVALID`<br>E2: Expired access token → HTTP 401 `TOKEN_EXPIRED` |                   |            |
| Priority:          | Must Have                           |                   |            |
| Business Rules:    | BR-AUTH-03                          |                   |            |

---

### 2.4 UC-04: Password Reset (Forgot Password → OTP Verification)

| UC ID and Name:    | UC-04_Password Reset                |                   |            |
| ------------------ | ----------------------------------- | ----------------- | ---------- |
| Created By:        | WMS Dev Team                        | Date Created:     | 2026-07-14 |
| Primary Actor:     | User (unauthenticated)              | Secondary Actors: | System, Email |
| Trigger:           | User clicks "Forgot Password" on login page |                   |            |
| Description:       | Two-step flow: (1) User requests password reset via email → system generates + sends OTP; (2) User verifies OTP + sets new password |                   |            |
| Preconditions:     | PRE-1: User account exists (inactive accounts receive no email) |                   |            |
| Postconditions:    | POST-1: User password updated and hashed with bcrypt<br>POST-2: OTP invalidated after use<br>POST-3: Audit log recorded |                   |            |
| Normal Flow (Step 1 — Forgot Password):       | 1. Unauthenticated user calls `POST /api/v1/auth/forgot-password` with email<br>2. System looks up user by email<br>3. IF found AND is_active=true: generate 6-digit OTP, hash it, store in `users.otp_hash`, set `users.otp_expires_at = NOW + 10min`, send email via SMTP<br>4. Always return HTTP 200 (no email enumeration)<br>5. Ghi audit log `PASSWORD_RESET_REQUEST` |                   |            |
| Normal Flow (Step 2 — Verify OTP): | 1. User calls `POST /api/v1/auth/verify-otp` with email, otp, newPassword<br>2. System looks up user by email<br>3. System hashes submitted OTP, compares to `users.otp_hash`<br>4. IF match AND otp_expires_at > NOW: hash new password with bcrypt, update `users.password_hash`, clear `otp_hash` + `otp_expires_at`, return HTTP 200<br>5. Ghi audit log `PASSWORD_RESET_COMPLETE` |                   |            |
| Exceptions:        | E1 (forgot-password): Email not found → HTTP 200 (silent)<br>E2 (verify-otp): OTP invalid → HTTP 400 `OTP_INVALID`<br>E3 (verify-otp): OTP expired → HTTP 400 `OTP_EXPIRED` |                   |            |
| Priority:          | Must Have                           |                   |            |
| Business Rules:    | BR-AUTH-01, BR-SEC-01               |                   |            |

---

### 2.5 UC-05: System Admin Manages Users & Warehouse Assignment

| UC ID and Name:    | UC-05_User & Warehouse Management   |                   |            |
| ------------------ | ----------------------------------- | ----------------- | ---------- |
| Created By:        | WMS Dev Team                        | Date Created:     | 2026-07-14 |
| Primary Actor:     | System Admin (ADMIN role)           | Secondary Actors: | CEO approval (new warehouse) |
| Trigger:           | System Admin accesses User Management or Warehouse Config screen |                   |            |
| Description:       | System Admin creates/updates user accounts, assigns roles (CEO, ADMIN, MANAGER, STOREKEEPER, etc.), and assigns users to specific warehouses for data isolation |                   |            |
| Preconditions:     | PRE-1: Actor has ADMIN role        |                   |            |
| Postconditions:    | POST-1: User created or updated with role + warehouse assignment<br>POST-2: Audit log recorded with old/new values |                   |            |
| Normal Flow:       | 1. Admin opens User List, clicks "Create User"<br>2. Admin fills: email (unique check), username (unique), password (strength validated), fullName, phone, jobTitle, role (dropdown), assigned_warehouses (multi-select)<br>3. Admin submits<br>4. System validates: unique email, unique username, password ≥8 chars, role valid<br>5. System hashes password with bcrypt (cost ≥12)<br>6. System creates `users` record with is_active=true<br>7. System links user to selected warehouses via `user_warehouse_assignments` table (many-to-many)<br>8. Ghi audit log `USER_CREATED` |                   |            |
| Alternative Flows: | A.1 Update User<br>   • Admin modifies role or warehouse assignment<br>   • System updates `user_warehouse_assignments` (add/remove as needed)<br>   • Ghi audit log `USER_UPDATED` with before/after state<br>A.2 Deactivate User<br>   • Admin clicks Delete (soft-delete)<br>   • System sets is_active = false<br>   • System prevents future login for this user |                   |            |
| Exceptions:        | E1: Email already exists → HTTP 400 `DUPLICATE_EMAIL`<br>E2: Invalid role → HTTP 400 `INVALID_ROLE`<br>E3: No warehouses assigned → HTTP 400 `NO_WAREHOUSES_ASSIGNED` (for non-ADMIN/CEO roles) |                   |            |
| Priority:          | Must Have                           |                   |            |
| Business Rules:    | BR-SEC-01 (warehouse scope check), BR-DEL-01 (soft delete) |                   |            |

---

## 3. Spec 002: Master Data Management (US-WMS-19, US-WMS-20)

### 3.1 UC-06: Product Master Data Management

| UC ID and Name:    | UC-06_Product Management            |                   |            |
| ------------------ | ----------------------------------- | ----------------- | ---------- |
| Created By:        | WMS Dev Team                        | Date Created:     | 2026-07-14 |
| Primary Actor:     | Thủ kho (STOREKEEPER role)          | Secondary Actors: | System Admin |
| Trigger:           | Storekeeper accesses Product Master Data screen |                   |            |
| Description:       | Manage SKU (product code), name, unit, packaging conversion (thùng→cái), weight, volume, reorder point. No serial tracking or expiry fields for household goods |                   |            |
| Preconditions:     | PRE-1: Actor has STOREKEEPER or ADMIN role |                   |            |
| Postconditions:    | POST-1: Product record created/updated<br>POST-2: SKU uniqueness guaranteed |                   |            |
| Normal Flow:       | 1. Storekeeper opens Product List, clicks "Create Product"<br>2. Fills: SKU (unique check), name, unit (e.g., cái, thùng), optional: description, weight_kg, volume_m3, unit_per_pack (conversion), reorder_point<br>3. Submits<br>4. System validates: unique SKU, positive weight/volume if provided, positive unit_per_pack if provided<br>5. System creates `products` record with is_active=true<br>6. Ghi audit log `PRODUCT_CREATED` |                   |            |
| Alternative Flows: | A.1 Update Product<br>   • Storekeeper modifies name, unit, weight, volume, reorder_point<br>   • Cannot modify SKU once created<br>   • Ghi audit log `PRODUCT_UPDATED`<br>A.2 Deactivate Product<br>   • Storekeeper clicks Delete (soft-delete)<br>   • System sets is_active = false<br>   • System prevents new transactions (receipt, issue, transfer) for this product |                   |            |
| Exceptions:        | E1: SKU already exists → HTTP 400 `DUPLICATE_SKU`<br>E2: Invalid unit → HTTP 400 `INVALID_UNIT`<br>E3: Negative weight/volume → HTTP 400 `INVALID_DIMENSION` |                   |            |
| Priority:          | Must Have                           |                   |            |
| Business Rules:    | BR-BAT-01 (no serial, no expiry), BR-DEL-01 (soft delete) |                   |            |

---

### 3.2 UC-07: Warehouse Configuration & Bin Location Setup

| UC ID and Name:    | UC-07_Warehouse & Bin Management    |                   |            |
| ------------------ | ----------------------------------- | ----------------- | ---------- |
| Created By:        | WMS Dev Team                        | Date Created:     | 2026-07-14 |
| Primary Actor:     | System Admin, CEO (for new warehouse) | Secondary Actors: | Thủ kho    |
| Trigger:           | Admin opens Warehouse Config or Bin Location screens |                   |            |
| Description:       | Create/update physical warehouses (HP, HN, HCM) and virtual warehouse (IN_TRANSIT); define zones (e.g., receiving, storage, picking, quarantine) and bins with capacity constraints (m3, kg) |                   |            |
| Preconditions:     | PRE-1: For new warehouse: CEO approval<br>PRE-2: For bins: parent zone must exist in same warehouse |                   |            |
| Postconditions:    | POST-1: Warehouse/Zone/Bin created with globally unique codes<br>POST-2: Bin capacity constraints configured<br>POST-3: IN_TRANSIT warehouse has default zone + bin auto-created |                   |            |
| Normal Flow (Warehouse): | 1. Admin/CEO opens Warehouse List, clicks "Create Warehouse"<br>2. Fills: code (e.g., HP, HN, HCM, IN_TRANSIT), name, address, phone, manager_id (FK to user with MANAGER role), type (PHYSICAL or IN_TRANSIT)<br>3. Submits<br>4. System validates: unique code, valid manager role, type is PHYSICAL or IN_TRANSIT<br>5. System creates `warehouses` record with is_active=true<br>6. IF type=IN_TRANSIT: auto-create default zone + bin<br>7. Ghi audit log `WAREHOUSE_CREATED` |                   |            |
| Normal Flow (Zone): | 1. Admin opens Bin Location List, clicks "Create Zone"<br>2. Selects warehouse, enters zone_code (e.g., A, B, QUARANTINE)<br>3. System auto-generates globally unique code = `{warehouse_code}.{zone_code}` (e.g., HP.A)<br>4. System creates `warehouse_locations` record with type='ZONE', is_quarantine=(true if code contains 'QUARANTINE'), is_active=true<br>5. Ghi audit log `ZONE_CREATED` |                   |            |
| Normal Flow (Bin): | 1. Admin opens Bin Location List, clicks "Create Bin"<br>2. Selects warehouse + parent zone (must match warehouse)<br>3. Enters bin_code (e.g., 01.1.01), capacity_m3, capacity_kg<br>4. System auto-generates code = `{warehouse_code}.{zone_code}.{bin_code}` (e.g., HP.A.01.1.01)<br>5. System validates: parent zone exists and same warehouse, capacity positive<br>6. System creates `warehouse_locations` record with type='BIN', parent_id=zone.id, is_active=true<br>7. Ghi audit log `BIN_CREATED` |                   |            |
| Exceptions:        | E1: Zone code not unique within warehouse → HTTP 400 `DUPLICATE_ZONE_CODE`<br>E2: Bin parent zone not in same warehouse → HTTP 400 `ZONE_WAREHOUSE_MISMATCH`<br>E3: Negative capacity → HTTP 400 `INVALID_CAPACITY`<br>E4: Cannot deactivate location with stock → HTTP 409 `LOCATION_HAS_STOCK` |                   |            |
| Priority:          | Must Have                           |                   |            |
| Business Rules:    | BR-BAT-02 (bin capacity check), BR-DEL-01 (soft delete), BR-TRF-01 (IN_TRANSIT auto-create) |                   |            |

---

## 4. Business Rules (Spec 001 + 002)

| ID | Category | Rule Description |
|----|----------|------------------|
| BR-AUTH-01 | Security | All user passwords MUST be hashed with bcrypt cost factor ≥ 12; no plain text storage |
| BR-AUTH-02 | Token Management | Access token TTL = 15 minutes; Refresh token TTL = 7 days |
| BR-AUTH-03 | Revocation | Refresh tokens stored as hashed values server-side; invalidated on logout |
| BR-AUTH-04 | Rate Limiting | Max 5 failed login attempts per 15 min per IP; enforce HTTP 429 |
| BR-SEC-01 | RBAC | Authorization MUST check BOTH role AND warehouse assignment for all warehouse-scoped operations |
| BR-SEC-02 | Audit | All user/role/warehouse changes MUST create audit log entries (append-only, never modify/delete) |
| BR-BAT-01 | Product Domain | Batch gom theo sản phẩm + nguồn nhập + ngày nhận; KHÔNG serial, KHÔNG hạn sử dụng, KHÔNG grade |
| BR-BAT-02 | Bin Capacity | Putaway MUST check `bin_capacity` trước khi đặt hàng vào bin (check both m3 + kg) |
| BR-DEL-01 | Soft Delete | Master data: `is_active = false`; Transaction data: `status = CANCELLED`; NO physical deletion |
| BR-TRF-01 | Virtual Warehouse | IN_TRANSIT warehouse auto-creates default zone + bin; bypass capacity checks for this location |

---

# III. Spec 004: Outbound & Delivery (US-WMS-06..10)

**Compact UC Summary:**

| UC | Actor | Description |
|----|----|-------------|
| UC-14 | Planner | Lập Đơn xuất hàng (DO); system kiểm Credit Check + tồn kho khả dụng; reserve cấp warehouse/product |
| UC-15 | Thủ kho | Lập picking plan FIFO; chọn batch/bin/zone hợp lệ từ kho; snapshot giá unit_price |
| UC-16 | Nhân viên kho | Lấy hàng + QC outbound (pass/fail); hàng fail → Quarantine + adjustment |
| UC-17 | Thủ kho | Duyệt chất lượng; nếu fail chưa đủ → chọn replacement từ batch khác |
| UC-18 | Trưởng kho | Duyệt xuất kho; DO chuyển WAREHOUSE_APPROVED |
| UC-19 | Dispatcher | Lập trip; gán xe/tài xế; tối ưu lộ trình; kiểm tải trọng/thể tích |
| UC-20 | Tài xế | Nhận hàng (xe rời kho) → In-Transit; giao hàng + POD + OTP email xác nhận |
| UC-21 | System | Auto create invoice + cộng công nợ khi giao thành công |

**Business Rules:**
- BR-OUT-01: Credit Check auto → block if `balance + DO > limit` OR overdue > 30d
- BR-OUT-02: Reserve + release tối ưu (không double-book)
- BR-OUT-03: FIFO + optimistic locking cho inventory updates
- BR-OUT-04: POD = goodsImage + signDocumentImage + OTP verified
- BR-OUT-05: Invoice auto-create khi delivery attempt = DELIVERED (không manual)
- BR-OUT-06: OTP 6-digit, TTL 5min, max 3 attempts

---

# IV. Spec 005: Inter-warehouse Transfer (US-WMS-11..13)

**Compact UC Summary:**

| UC | Actor | Description |
|----|----|-------------|
| UC-22 | Planner | Nhập phiếu điều chuyển (TRF); không auto-suggest; chỉ từ lệnh ngoài hoặc CEO-approved request |
| UC-23 | Trưởng kho | Duyệt phiếu (kho nguồn); FIFO-eligible reserve; chờ dispatcher lập trip |
| UC-24 | Dispatcher | Lập trip outbound; gán xe/tài xế nguồn; QC outbound ảnh + soạn hàng |
| UC-25 | Tài xế | Departure (xe rời kho nguồn) → trừ kho nguồn, cộng In-Transit |
| UC-26 | Tài xế | Arrival (xe đến kho đích) → blind count; receiving handover ảnh |
| UC-27 | Thủ kho | Receive-check + QC + bin-capacity; final confirm → trừ In-Transit, cộng kho đích |
| UC-28 | System | Tính discrepancy + adjust nếu thiếu/thừa |

**Business Rules:**
- BR-TRF-01: Transfer → In-Transit location mandatory
- BR-TRF-02: Outbound QC + handover ảnh bắt buộc (không Barcode/QR trong Sprint 1)
- BR-TRF-03: Chênh lệch qty → adjustment loại TRANSFER_DISCREPANCY
- BR-TRF-04: Wrong-SKU → Return-to-Source; quá hạn → override với lý do + audit
- BR-TRF-05: Version/concurrency guard trên transfer + trip mutations
- BR-TRF-06: Audit transfer đầy đủ: header, items, allocation, QC, inventory movement

---

# V. Business Rules Summary (Spec 004+005)

| ID | Category | Rule |
|----|----|------|
| BR-OUT-01 | Credit Control | Auto-block DO if `balance + value > limit` OR overdue >30d; CREDIT_HOLD state |
| BR-OUT-02 | Reservation | Reserve tại DO create (warehouse-level); concrete allocation khi picking plan |
| BR-OUT-03 | Inventory | FIFO batch selection + optimistic locking (@Version) on `inventories` |
| BR-OUT-04 | QC Outbound | Pass → staging; Fail → Quarantine + adjustment; replacement nếu fail chưa đủ |
| BR-OUT-05 | Invoice | Auto-create khi delivery DELIVERED; snapshot unit_price từ picking plan |
| BR-OUT-06 | POD & OTP | goodsImage + signDocumentImage bắt buộc; OTP 6-digit, 5min TTL, max 3 fail |
| BR-OUT-07 | Trip | Concurrent task check (xe/tài xế overlap); tải trọng/thể tích validate |
| BR-TRF-01 | Virtual Location | Transfer → In-Transit; released khi dest receive confirmed |
| BR-TRF-02 | QC Transfer | Outbound QC + ảnh handover; Inbound blind count + receive QC + bin-capacity check |
| BR-TRF-03 | Discrepancy | qty_sent ≠ qty_received → TRANSFER_DISCREPANCY adjustment |
| BR-TRF-04 | Wrong-SKU | Line-level expected/actual SKU + Return-to-Source mech |
| BR-TRF-05 | Overdue Transfer | Trip overdue → block receive; override chỉ với CEO/MANAGER + lý do + photo refs |
| BR-TRF-06 | Audit | Transfer mutations phải ghi đầy đủ audit trail: header/items/allocation/QC/inventory |

---

# VI. Spec 006: Stocktake & Variance (US-WMS-13)

**Compact UC Summary:**

| UC | Actor | Description |
|----|----|-------------|
| UC-29 | Thủ kho | Tạo phiếu kiểm kê; exclude Quarantine; lock locations khi IN_PROGRESS |
| UC-30 | Thủ kho | Nhập số đếm thực tế; tính variance_qty = actual - system |
| UC-31 | Thủ kho | Hoàn tất đếm → PENDING_APPROVAL (route Trưởng kho) |
| UC-32 | Trưởng kho | Duyệt/từ chối chênh lệch; không phân cấp theo giá trị |
| UC-33 | System | Update inventory khi duyệt; release location lock |

**Business Rules:**
- BR-STK-01: Phiếu không bao gồm Quarantine zones (WHERE is_quarantine = false)
- BR-STK-02: Lock locations (is_locked = true) từ IN_PROGRESS → release khi APPROVED/REJECTED
- BR-STK-03: Variance = actual_qty - system_qty; value = variance × cost_price
- BR-STK-04: Mọi chênh lệch → Trưởng kho duyệt trực tiếp (KHÔNG phân cấp)
- BR-STK-05: Inventory update + optimistic locking khi approval

---

# VII. Spec 007: Pricing & COGS (US-WMS-14)

**Compact UC Summary:**

| UC | Actor | Description |
|----|----|-------------|
| UC-34 | Kế toán viên | Nhập bảng giá (cost_price, selling_price); có hiệu lực từ ngày |
| UC-35 | Kế toán trưởng | Duyệt bảng giá; không phân cấp |
| UC-36 | System | COGS = cost_price tại ngày xuất (từ price_history) |

**Business Rules:**
- BR-PRICE-01: Price history lưu cost_price + selling_price + effective_date + end_date
- BR-PRICE-02: COGS lookup qua price_history.effective_date ≤ issue_date
- BR-PRICE-03: Giá bán snapshot ở picking plan → invoiced qua unit_price column

---

# VIII. Spec 008: Finance & Billing (US-WMS-15..18)

**Compact UC Summary:**

| UC | Actor | Description |
|----|----|-------------|
| UC-37 | System | Auto-create invoice khi delivery DELIVERED; cộng receivable |
| UC-38 | Kế toán viên | Ghi nhận thanh toán; trừ receivable |
| UC-39 | Kế toán trưởng | Chốt sổ kỳ; block nếu còn chứng từ tồn đọng |
| UC-40 | System | P&L + Aging Report; xem Debit/Credit balances |

**Business Rules:**
- BR-FIN-01: Invoice auto-create (không manual) → Kế toán chỉ xem + reconcile
- BR-FIN-02: Monthly closing → lock kỳ trước; late docs → Correction Voucher ở kỳ mở
- BR-FIN-03: Aging = invoice.due_date so với TODAY; >30d auto CREDIT_HOLD
- BR-FIN-04: COGS not null + immutable; balance = invoice - payment

---

# IX. Spec 009: Returns & Disposal (US-WMS-24..25)

**Compact UC Summary:**

| UC | Actor | Description |
|----|----|-------------|
| UC-41 | Trưởng kho | Trả hàng NCC (RTV) cho fail QC origin RECEIPT → tạo Debit Note |
| UC-42 | Trưởng kho | Tiêu hủy hàng lỗi từ Quarantine → adjustment loại DISPOSAL (không phân cấp) |
| UC-43 | Kế toán viên | Ghi nhận Credit Note cho hàng hoàn trả (customer); tính returned_value |

**Business Rules:**
- BR-RET-01: RTV (Spec 003) → Debit Note cho NCC; Disposal (Spec 009) → no NCC Debit
- BR-RET-02: Hàng transfer fail → DISPOSAL (không RTV)
- BR-RET-03: Tiêu hủy mọi giá trị → Trưởng kho duyệt trực tiếp
- BR-RET-04: Credit Note (customer return) → trừ receivable

---

# X. Spec 010: Reports & Dashboards (US-WMS-22..23)

**Compact UC Summary:**

| UC | Actor | Description |
|----|----|-------------|
| UC-44 | CEO | Dashboard: tồn kho (value), công nợ (aging), P&L, OTD%, QC fail rate |
| UC-45 | Kế toán trưởng | Aging Report (Due/OD 1-30/31-60/>60) + cash flow forecast |
| UC-46 | System | Low stock alert khi available < reorder_point; send email |

**Business Rules:**
- BR-RPT-01: Report view → ghi REPORT_VIEW audit log (AUD-04)
- BR-RPT-02: Dashboard tính toán real-time từ inventories + invoices
- BR-RPT-03: Alert (low stock) → trigger email tới Planner/Storekeeper

---

# Summary: All 10 Specs Complete

| Spec | UC Count | Key Rules |
|------|----------|-----------|
| 001 | 5 | JWT (15m/7d), bcrypt ≥12, RBAC warehouse-scoped |
| 002 | 3 | Master data soft-delete, SKU unique, bin capacity |
| 003 | 5 | Receipt → QC → Putaway; quarantine RTV path |
| 004 | 8 | Credit Check auto, FIFO picking, POD + OTP auto-invoice |
| 005 | 7 | Transfer via In-Transit, blind count, discrepancy adjust |
| 006 | 5 | Stocktake lock locations, flat approval (Trưởng kho) |
| 007 | 3 | Price history, COGS snapshot, markup tracking |
| 008 | 4 | Monthly closing, Aging 1-30/31-60/>60, P&L |
| 009 | 3 | RTV → Debit Note (supplier), Disposal flat approval |
| 010 | 3 | Dashboard KPI, Low stock alert, REPORT_VIEW audit |
