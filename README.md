# 🏭 Hệ Thống Quản Lý Kho (WMS) — Công Ty Phúc Anh

> **Warehouse Management System v1.0** — Giải pháp quản lý kho tập trung cho doanh nghiệp thương mại, thay thế quy trình thủ công (giấy tờ, Excel) bằng hệ thống kỹ thuật số thống nhất.

---

## 📋 Mục Lục

- [Tổng quan dự án](#tổng-quan-dự-án)
- [Tech Stack](#tech-stack)
- [Kiến trúc hệ thống](#kiến-trúc-hệ-thống)
- [Các Actors & Phân quyền](#các-actors--phân-quyền)
- [User Stories](#user-stories)
- [Các quy trình nghiệp vụ chính](#các-quy-trình-nghiệp-vụ-chính)
- [Cấu trúc thư mục](#cấu-trúc-thư-mục)
- [Cài đặt & Chạy dự án](#cài-đặt--chạy-dự-án)
- [Quy tắc & Conventions](#quy-tắc--conventions)
- [Domain Rules](#domain-rules)
- [Git Conventions](#git-conventions)
- [Definition of Done](#definition-of-done)

---

## Tổng Quan Dự Án

**Công ty:** Phúc Anh  
**Phạm vi:** 3 kho vật lý — Hải Phòng · Hà Nội · Hồ Chí Minh  
**Giai đoạn:** Sprint 1 — Core Warehouse Operations

### Mục tiêu chiến lược

Xây dựng giải pháp phần mềm quản lý kho tập trung, thay thế các phương thức thủ công bằng quy trình kỹ thuật số thống nhất, đảm bảo nghiệp vụ nhập, xuất, điều chuyển, kiểm kê và truy vết tồn kho được thực thi chính xác, kiểm soát được và có audit trail đầy đủ.

### Phạm vi hệ thống

| ✅ Bao gồm | ❌ Không bao gồm |
|---|---|
| Quản lý kho (WMS) | Quản lý sản xuất (Manufacturing) |
| Kế toán nội bộ kho | HR / HRM |
| Điều phối vận tải nội bộ | Barcode / QR Scanner |
| Kinh doanh & công nợ Đại lý | Cổng B2B / B2C Portal |
| | Tích hợp hệ thống bên ngoài |

> ⚠️ **Lưu ý quan trọng:** Hệ thống **CHỈ dùng xe nội bộ** của Phúc Anh. KHÔNG phát sinh chi phí 3PL, KHÔNG có luồng Duyệt chi vận tải.

### Scale hệ thống

- **1,000+** sản phẩm (SKU)
- **50+** Đại lý
- **1,000+** giao dịch / tháng

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Backend** | Spring Boot 3.4.5 + Java 21 (Maven) |
| **Frontend** | React 18 + JavaScript |
| **Database** | PostgreSQL 18 |
| **ORM** | Spring Data JPA / Hibernate |
| **Auth** | JWT + bcrypt (cost factor ≥ 12) |
| **Styling** | Tailwind CSS 3.x |
| **Testing** | JUnit 5 + Mockito (backend), Jest (frontend) |
| **API Docs** | OpenAPI / Swagger |
| **DB Migration** | Flyway |

---

## Kiến Trúc Hệ Thống

### Sơ đồ tổng quan

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         FRONTEND (React 18)                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐ │
│  │Dashboard │  │Inventory │  │Receipt/  │  │Transfer  │  │Delivery  │ │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘ │
└───────┼─────────────┼─────────────┼─────────────┼─────────────┼───────┘
        │             │ REST API    │             │             │
┌─────────────────────────────────────────────────────────────────────────┐
│                    BACKEND (Spring Boot 3.4.5)                          │
│  REST API Layer → Service Layer → Repository Layer → Entity Layer       │
└─────────────────────────────────────────────────────────────────────────┘
                              │
        ┌──────────────────────┴──────────────────────┐
        ▼                                             ▼
┌─────────────────┐                          ┌─────────────────┐
│   PostgreSQL    │                          │   File Storage  │
│   (Primary DB)  │                          │   (/uploads)    │
└─────────────────┘                          └─────────────────┘
```

### Layer Architecture (Backend)

```
Controller (@RestController)  →  Input validation, HTTP status, DTOs
    ↓
Service (@Service)            →  Business logic, FIFO, Audit logging
    ↓
Repository (@Repository)      →  JPA/Hibernate queries (KHÔNG dùng raw SQL)
    ↓
Entity (@Entity)              →  Database table mapping, JPA annotations
```

### Các quyết định kiến trúc (ADR)

| ADR | Quyết định | Lý do |
|---|---|---|
| ADR-001 | Spring Boot 3.4.5 + Java 21 | Team có kinh nghiệm Java enterprise, type safety mạnh |
| ADR-002 | Spring Data JPA/Hibernate | Type safety, relational integrity, Flyway migration |
| ADR-003 | JWT + bcrypt (cost 12) | Stateless, scalable, industry standard |
| ADR-004 | Kho ảo "In-Transit Location" | Track inventory khi đang vận chuyển giữa các kho |
| ADR-005 | Quarantine Zone cho QC-failed | Tách hàng lỗi, không tính vào available inventory |
| ADR-006 | Kế toán nội bộ chạy chung DB | Đơn giản hóa kiến trúc, đảm bảo ACID transactions |

---

## Các Actors & Phân Quyền

Hệ thống có **10 Actors** chia thành 3 tầng theo mô hình **Maker-Checker**:

### Tầng 1: Quản trị

| Actor | Loại | Trách nhiệm chính |
|---|---|---|
| **CEO** | Checker cấp cao | Dashboard chiến lược, cấu hình hệ thống, phê duyệt yêu cầu điều chuyển liên kho do Trưởng kho đề xuất |
| **System Admin** | Admin | Quản lý tài khoản, phân quyền RBAC, cấu hình tham số hệ thống |

### Tầng 2: Quản lý

| Actor | Loại | Trách nhiệm chính |
|---|---|---|
| **Trưởng kho** | Checker | Duyệt nhập/xuất/điều chuyển, đề xuất điều chuyển khi kho mình thiếu hàng, xử lý chênh lệch 5M–100M VNĐ, duyệt biên bản xử lý hàng lỗi |
| **Kế toán trưởng** | Checker | Duyệt bảng giá, thiết lập Credit Limit, chốt sổ tháng, P&L / Aging Report |

### Tầng 3: Nghiệp vụ (Maker)

| Actor | Trách nhiệm chính |
|---|---|
| **Planner** | Tiếp nhận yêu cầu xuất/nhập kho từ Công ty mẹ hoặc bên thứ ba, nhập yêu cầu lên hệ thống, kiểm tra Credit Check + tồn kho |
| **Dispatcher** | Lập chuyến xe nội bộ Phúc Anh, gán tài xế, tối ưu lộ trình giao hàng |
| **Thủ kho kiêm QC** | Quản lý SKU/danh mục sản phẩm, tiếp nhận hàng, kiểm QC inbound/outbound, soạn hàng, kiểm kê, cất Bin, xác nhận điều chuyển |
| **Nhân viên kho (Bốc xếp)** | Bốc xếp hàng hóa, hỗ trợ di chuyển hàng hóa, di chuyển hàng lỗi vào Quarantine theo chỉ dẫn của Thủ kho |
| **Kế toán viên** | Quản lý hồ sơ Nhà cung cấp, theo dõi invoice/công nợ tự động, xử lý thanh toán trong luồng tài chính riêng, quản lý bảng giá |
| **Tài xế** | Nhận chuyến (smartphone), upload `goodsImage`/`signDocumentImage`, nhập OTP Đại lý, báo giao thất bại, xác nhận xe về kho |

> **Phân biệt Dispatcher vs Planner:** Planner = nhận đơn từ Công ty mẹ & lập Delivery Order; Dispatcher = điều phối xe & tài xế giao hàng — **hai vai trò hoàn toàn khác nhau**.

---

## User Stories

Hệ thống có **27 User Stories** chia thành 9 nhóm nghiệp vụ:

### Nhóm 1: Quản trị hệ thống & Cấu hình (Admin & CEO)

| US | Tên | Priority |
|---|---|---|
| US-WMS-01 | Cấu hình Tham số Hệ thống & Định mức Phê duyệt động | P1 |

### Nhóm 2: Quy trình nhập hàng (Inbound & QC)

| US | Tên | Priority |
|---|---|---|
| US-WMS-02 | Tiếp nhận thông tin & Lập Lệnh nhập kho | P1 |
| US-WMS-03 | Kiểm hàng thực tế & Kiểm QC Inbound | P1 |
| US-WMS-04 | Phê duyệt hàng lỗi & Ra quyết định xử lý | P1 |
| US-WMS-05 | Ký duyệt Nhập kho chính thức | P1 |

### Nhóm 3: Xuất hàng & Giao hàng (Outbound & Delivery)

| US | Tên | Priority |
|---|---|---|
| US-WMS-06 | Tiếp nhận yêu cầu & Lập Đơn xuất hàng (Credit Check tự động) | P1 |
| US-WMS-07 | Soạn hàng & Kiểm QC đóng gói | P1 |
| US-WMS-08 | Lập Chuyến xe & Điều phối Vận tải Nội bộ | P1 |
| US-WMS-09 | Giao diện Web mobile cho Tài xế & POD thời gian thực | P1 |
| US-WMS-10 | Tự động tạo Hóa đơn bán hàng & Cộng công nợ Đại lý | P1 |

### Nhóm 4: Điều chuyển nội bộ (Replenishment & Transfer)

| US | Tên | Priority |
|---|---|---|
| US-WMS-11 | Planner nhập lệnh điều chuyển thủ công từ Công ty mẹ/bộ phận điều phối | P2 |
| US-WMS-11A | Trưởng kho đề xuất điều chuyển từ tồn kho liên kho và CEO duyệt | P1 |
| US-WMS-12 | Lập, Duyệt & Xác nhận Phiếu Điều chuyển Kho Nội bộ | P1 |

### Nhóm 5: Kiểm kê & Quản lý giá (Inventory, Price & Audit)

| US | Tên | Priority |
|---|---|---|
| US-WMS-13 | Kiểm kê kho định kỳ & Điều chỉnh chênh lệch | P1 |
| US-WMS-14 | Thiết lập Bảng giá & Lịch sử biến động giá | P1 |

### Nhóm 6: Tài chính & Kỳ kế toán (Finance & Closing)

| US | Tên | Priority |
|---|---|---|
| US-WMS-15 | Ghi nhận Thanh toán & Quản lý Vòng đời Công nợ Đại lý | P1 |
| US-WMS-16 | Báo cáo Công nợ Phân kỳ (Aging Report) | P1 |
| US-WMS-17 | Chốt sổ Kế toán & Khóa cứng kỳ quá khứ | P1 |

### Nhóm 7: Báo cáo & Dashboard (Reporting)

| US | Tên | Priority |
|---|---|---|
| US-WMS-18 | Dashboard Báo cáo Quản trị cấp cao | P1 |

### Nhóm 8: Danh mục nền tảng (Master Data)

| US | Tên | Priority |
|---|---|---|
| US-WMS-19 | Quản lý Danh mục Sản phẩm & SKU tập trung | P1 |
| US-WMS-20 | Cấu hình Vị trí kho & Kiểm tra Sức chứa Kệ (Bin Location) | P2 |
| US-WMS-21 | Phân quyền theo Chi nhánh Kho & Vai trò (RBAC) | P1 |
| US-WMS-22 | Quản lý Danh mục Đối tác (Đại lý & Nhà cung cấp) | P1 |
| US-WMS-23 | Quản lý Danh mục Xe tải & Tài xế Nội bộ | P2 |

### Nhóm 9: Quy trình phụ trợ (Supporting Processes)

| US | Tên | Priority |
|---|---|---|
| US-WMS-24 | Xử lý hàng hoàn trả từ Đại lý (Inbound Returns) | P2 |
| US-WMS-25 | Báo cáo Năng suất & Sản lượng Nhân viên Kho | P3 |
| US-WMS-26 | Cảnh báo tự động Tồn kho dưới định mức | P1 |

---

## Các Quy Trình Nghiệp Vụ Chính

### 1. Quy trình Nhập hàng (Inbound)

```
Công ty mẹ (Zalo/Email)
    → Planner lập Lệnh nhập [PENDING_RECEIPT]
    → Thủ kho đếm hàng thực tế và kiểm QC → [QC_COMPLETED]
        ├── Hàng Lỗi → Quarantine Zone → Trưởng kho tạo RTV [Trả NCC] → Thủ kho xác nhận trả đủ NCC rồi mới trừ Quarantine
        └── Hàng Đạt → Trưởng kho Duyệt nhập [APPROVED]
            └── Nếu Trưởng kho từ chối → [RETURN_TO_SUPPLIER_PENDING] → Thủ kho bàn giao xe NCC [RETURNED_TO_SUPPLIER]
    → Thủ kho cất vào Bin Location sau duyệt
    → Hệ thống cộng tồn kho khả dụng sau putaway
```

Feature 003 chỉ xử lý RTV bằng nút "Trả NCC". Luồng tiêu hủy hàng lỗi nằm trong Spec 009.

### 2. Quy trình Xuất hàng & Giao hàng (Outbound & Delivery)

```
Công ty mẹ gửi yêu cầu xuất hàng
    → Planner kiểm tra [Credit Check + Tồn kho]
        ├── Vi phạm Credit → Chặn cứng, hiển thị lý do
        └── Hợp lệ → Lập Đơn xuất [NEW] + Reserve tồn kho
    → Thủ kho lập kế hoạch lấy hàng từ một hoặc nhiều batch/bin/zone FIFO [WAITING_PICKING]
    → Nhân viên kho lấy hàng theo kế hoạch và ghi QC đúng 1 lần theo item/allocation/batch/bin/zone; hàng fail vào Quarantine + phiếu điều chỉnh, xóa reserve allocation fail [QC_PENDING_APPROVAL]
        (nếu pass chưa đủ, Thủ kho xử lý replacement từ QC_PENDING_APPROVAL rồi DO quay lại WAITING_PICKING)
    → Thủ kho duyệt chất lượng khi đủ hàng đạt [QC_COMPLETED]
    → Trưởng kho duyệt xuất [WAREHOUSE_APPROVED]
    → Dispatcher lập Chuyến xe nội bộ `trip_type = DELIVERY` cùng kho, gán xe/tài xế thuộc kho, kiểm tra cân nặng và kiểm tra thể tích nếu xe có cấu hình thể tích
    → Tài xế xác nhận nhận hàng → [IN_TRANSIT] → Hệ thống chuyển hàng từ outbound staging sang kho ảo In-Transit
    → Tài xế upload goodsImage + signDocumentImage cho delivery attempt hiện tại, Đại lý đọc OTP email 6 số
        (ảnh < 5MB; OTP hiệu lực 5 phút; raw OTP không lưu DB; chỉ 1 row OTP/attempt; resend khi còn hạn bị chặn; sai 3 lần cần Admin reset)
    → Giao full DO: hệ thống chỉ trừ In-Transit của DO được xác nhận, tự động tạo Hóa đơn (Invoice), cộng công nợ và chuyển DO [COMPLETED]
    → Nếu Đại lý không nhận: Tài xế chuyển DO [RETURNED], hàng vẫn ở In-Transit cho luồng hoàn hàng riêng
    → Khi xe quay lại kho, Tài xế bấm xác nhận xe đã về và mọi DO trong chuyến đã COMPLETED/RETURNED → Trip [COMPLETED]
    → Luồng tài chính/thanh toán riêng xử lý thu tiền, cấn trừ công nợ và chuyển DO [CLOSED]
```

### 3. Quy trình Điều chuyển Kho Nội bộ

```
Nguồn lệnh điều chuyển:
    ├── Công ty mẹ/bộ phận điều phối gửi lệnh ngoài cho Planner
    └── Trưởng kho kho thiếu xem tồn kho liên kho read-only
        → Tạo yêu cầu điều chuyển, CEO duyệt
        → Planner kho nguồn/trung tâm nhận mẫu đã duyệt
    → Planner nhập Phiếu điều chuyển thủ công kèm mã lệnh ngoài hoặc yêu cầu đã duyệt [MỚI]
    → Trưởng kho nguồn kiểm tra tồn khả dụng FIFO eligible → Duyệt [ĐÃ DUYỆT]
    → Dispatcher lập chuyến xe nội bộ riêng, gán xe và Tài xế, kiểm tra xe/tài xế không trùng lịch, tải trọng/thể tích xe
    → Thủ kho nguồn soạn hàng, outbound QC bằng mắt/đối chiếu phiếu, chụp ảnh xác nhận, xuất hàng lên xe và chụp ảnh bàn giao cho tài xế
        ├── Phải ghi đúng số lượng đã duyệt; không cho xuất thừa/thiếu
        └── Nếu cần hủy sau khi đã lên xe nhưng chưa rời kho → Unship/Unload trước rồi mới hủy
    → Tài xế xác nhận đã nhận hàng và xe rời kho
        → Hệ thống: Trừ tồn Kho nguồn, Cộng Kho ảo In-Transit [ĐANG VẬN CHUYỂN]
    → Tài xế ghi nhận xe đến kho nhận và kho nhận ghi nhận handover
    → Công nhân kho đích blind count số lượng thực nhận
    → Thủ kho đích kiểm tra lại số lượng, nhập QC, điều chỉnh số nếu cần, kiểm tra sức chứa Bin và chọn vị trí nhập
    → Trưởng kho đích xác nhận cuối
        ├── Khớp + QC đạt → Hệ thống: Trừ In-Transit, Cộng tồn Kho đích [HOÀN THÀNH]
        ├── Thiếu → Ghi lý do + incident/discrepancy + Tạo Phiếu điều chỉnh bù trừ; không tạo hàng Quarantine cho phần thiếu
        ├── QC lỗi/hư hỏng → Phần lỗi vào Quarantine theo nguồn INTERNAL_TRANSFER, chỉ đi luồng tiêu hủy Spec 009; không tạo RTV/Debit Note NCC
        ├── Gửi nhầm SKU còn nguyên → Thủ kho đích báo cáo line expected/actual SKU + số lượng + lý do/ảnh nếu có, Trưởng kho đích duyệt xe quay về kho nguồn; tài xế ghi return departure/source arrival/handover
        ├── Trip quá hạn khi còn In-Transit → Chặn nhận tại kho đích và yêu cầu vai trò có thẩm quyền kích hoạt Return to Source với lý do, kèm ảnh nếu có
        └── Nhận thừa → Chặn nhập regular inventory và ghi discrepancy hold/incident
```

### 4. Quy trình Công nợ & Kế toán (Finance Cycle)

```
[Phát sinh nợ]
POD + OTP hợp lệ cho full DO → Hệ thống tự động lập Invoice theo giá snapshot trên DO tại thời điểm Thủ kho soạn/lập picking plan (`issue_date` = ngày local hiện tại, `due_date = issue_date + 30 ngày`) → current_balance += giá trị đơn → DO [COMPLETED]
    → IF current_balance > credit_limit → CREDIT_HOLD (chặn đơn mới; bằng hạn mức vẫn cho phép)
    → Daily Job: IF invoice quá hạn > 30 ngày → CREDIT_HOLD + cảnh báo Kế toán trưởng

[Thu nợ]
Luồng tài chính/thanh toán riêng: Đại lý trả tiền → Kế toán tạo Phiếu thu → current_balance -= số tiền thu
    → IF current_balance < credit_limit * 0.8 → ACTIVE (mở khóa, buffer 20%)

[Chốt sổ — Cuối tháng]
Kế toán trưởng kiểm tra điều kiện → Chốt sổ kỳ T → CLOSED
    → Khóa cứng mọi chứng từ có transaction_date trong kỳ T
    → Chứng từ trễ hạn → Hạch toán vào kỳ hiện tại
    → Sai sót phát hiện sau → Tạo Adjustment Voucher tại kỳ mở (có link tham chiếu)
```

### Luồng trạng thái đơn xuất hàng

| Trạng thái | Người chuyển |
|---|---|
| **New** | Planner |
| **Waiting Picking** | Thủ kho (đã lập kế hoạch lấy hàng đầy đủ từ một hoặc nhiều batch/bin/zone FIFO) |
| **QC Pending Approval** | Nhân viên kho (sau khi nhập kết quả lấy hàng/QC một lần theo item/allocation/batch/bin/zone cho toàn bộ kế hoạch hiện tại; kể cả khi pass chưa đủ để Thủ kho xử lý replacement) |
| **QC Completed** | Thủ kho (đã duyệt chất lượng, đủ hàng đạt) |
| **Warehouse Approved** | Trưởng kho (phê duyệt xuất kho sau QC) |
| **In-Transit** ⚠️ *Tồn kho bị trừ tại đây* | Tài xế (xác nhận nhận hàng) |
| **Returned** | Tài xế (delivery attempt thất bại; attempt hiện tại là `FAILED`, hàng vẫn ở In-Transit cho đến khi luồng hoàn hàng tiếp nhận) |
| **Completed** | Hệ thống (POD + OTP hợp lệ, delivery attempt `DELIVERED`, invoice/công nợ đã tự động tạo) |
| **Closed** | Hệ thống/Kế toán (đã tất toán hoặc khóa theo kỳ kế toán) |
| **Rejected** | Trưởng kho (từ chối xuất kho sau QC; hàng đạt QC trả về bin gốc và ghi audit return-to-bin) |
| **Cancelled** | Planner / Trưởng kho |

**Sửa picking plan sau khi đã có kết quả lấy/QC:** Storekeeper vẫn dùng `PUT /api/v1/delivery-orders/{id}/picking-plan`. Payload `allocations[]` là kế hoạch lấy hàng đầy đủ mới; `returnToBinRecords[]` chỉ bắt buộc cho allocation đã pick và bị remove/reduce trong lần sửa. Allocation đã pick nhưng giữ nguyên không cần trả về bin. Mỗi lần trả hàng về bin gốc tạo audit `PICKED_GOODS_RETURN_TO_BIN`.

**Ghi nhận lấy hàng/QC outbound:** Warehouse staff ghi nhận theo đúng định danh nguồn `doItemId + allocationId + batchId + locationId + zoneId`; `batchId`, `locationId`, `zoneId` trong payload phải khớp allocation đã được Thủ kho lập kế hoạch.

**Reject sau QC:** Khi Trưởng kho reject ở `QC_COMPLETED`, tổng `returnedQty` trong `returnToBinRecords[]` phải bằng toàn bộ hàng đạt QC đang ở outbound staging. Hàng pass được cộng lại tồn hợp lệ tại bin gốc, release reservation và ghi audit `PICKED_GOODS_RETURN_TO_BIN`; hàng fail vẫn ở quarantine.

### Cơ chế Credit Check (Kiểm soát công nợ)

**Điều kiện CREDIT_HOLD** — kích hoạt khi vi phạm **BẤT KỲ** điều kiện nào:
1. `current_balance + giá_trị_đơn_mới > credit_limit` _(khi Planner tạo đơn; bằng hạn mức vẫn cho phép)_
2. `current_balance > credit_limit` _(sau khi lập hóa đơn; bằng hạn mức vẫn cho phép)_
3. Đại lý có ≥ 1 hóa đơn quá hạn **> 30 ngày** _(Daily Job)_

**Điều kiện mở khóa (ACTIVE):** `current_balance < credit_limit × 0.8` _(buffer 20%)_

### Phê duyệt điều chỉnh tồn kho & hủy hàng

| Giá trị lệch / Giá trị hủy | Người duyệt |
|---|---|
| 5 – 100 triệu VNĐ | Trưởng kho |
| > 100 triệu VNĐ **hoặc** lỗi do nhân viên | CEO |

---

## Cấu Trúc Thư Mục

```
Manager-warehouse-sdd/
├── AGENTS.md                         # Agent policy, tech stack, forbidden patterns
├── CLAUDE.md                         # Kiến trúc, workflow, conventions, swimlane diagrams
├── DESIGN.md                         # UI design tokens (Apple design system)
├── Kiến trúc phân tầng các Actors.md # 10 Actors, nghiệp vụ, quy trình chi tiết
├── Userstory.md                      # 27 User Stories đầy đủ
├── README.md                         # File này
│
├── backend/                          # Spring Boot 3.4.5 + Java 21
│   └── src/main/java/com/wms/
│       ├── controller/               # REST controllers (@RestController)
│       ├── service/                  # Business logic (@Service)
│       ├── repository/               # JPA repositories (@Repository)
│       ├── entity/                   # JPA entities (@Entity)
│       ├── dto/                      # Data transfer objects
│       ├── config/                   # Security, JPA config
│       ├── exception/                # GlobalExceptionHandler
│       ├── event/                    # Domain events
│       └── util/                     # FIFOSelector
│
├── frontend/                         # React 18 + Tailwind CSS
│   └── src/
│       ├── components/               # React components (PascalCase)
│       ├── pages/                    # Page components
│       ├── hooks/                    # Custom hooks (camelCase)
│       ├── services/                 # API calls
│       ├── stores/                   # State management (Zustand)
│       └── utils/                    # Utility functions
│
├── specs/                            # Feature specifications (SDD)
│   └── 001-featurename-us-wh/
│       └── spec.md
│
└── test/                             # Test plans and guidance
```

---

## Cài Đặt & Chạy Dự Án

### Yêu cầu

- Java 21+
- Node.js 18+
- PostgreSQL 18
- Maven 3.9+

### Backend (Spring Boot)

```bash
# Di chuyển vào thư mục backend
cd backend

# Build project
mvn clean compile

# Chạy ứng dụng
mvn spring-boot:run

# Chạy tests
mvn test
```

### Frontend (React)

```bash
# Di chuyển vào thư mục frontend
cd frontend

# Cài đặt dependencies
npm install

# Chạy dev server
npm run dev

# Build production
npm run build
```

### Cấu hình Database

```yaml
# backend/src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/wms_db
    username: your_username
    password: your_password
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
```

---

## Quy Tắc & Conventions

### ✅ Luôn làm

| Quy tắc | Mô tả |
|---|---|
| **Input validation** | Dùng Jakarta Validation annotations |
| **Audit logging** | Log mọi thao tác kho: who, when, what |
| **Inventory check** | Luôn kiểm tra tồn kho TRƯỚC khi xuất |
| **Structured logging** | Dùng SLF4J — KHÔNG dùng `console.log` / `System.out` |
| **Unit tests** | Tối thiểu 80% coverage cho services |
| **API docs** | Cập nhật OpenAPI/Swagger khi thêm/sửa endpoint |
| **Error handling** | HTTP status codes đúng chuẩn |
| **Comments** | Giải thích WHY, không giải thích WHAT |

### ❌ Tuyệt đối không làm

| Quy tắc | Mô tả |
|---|---|
| **No secrets** | Không lưu password/keys trong source code |
| **No raw SQL** | Luôn dùng JPA/Hibernate |
| **No negative inventory** | Không cho phép tồn kho âm dưới mọi hoàn cảnh |
| **No skip QC** | Luôn kiểm QC trước khi nhập kho |
| **No skip audit** | Log đầy đủ mọi thao tác kho |
| **No TODO** | Xóa hết TODO trước khi merge |
| **No direct commit** | Không commit thẳng vào `main`/`production` |

### Code Quality Limits

| Chỉ số | Giới hạn |
|---|---|
| Max function length | 40 lines |
| Max file length | 300 lines |
| Min test coverage | 80% (services) |
| Max PR size | 400 lines changed |

### Naming Conventions

#### Java (Backend)

| Loại | Convention | Ví dụ |
|---|---|---|
| Classes | PascalCase | `WarehouseService.java` |
| Packages | lowercase | `com.wms.service` |
| Tables | snake_case | `warehouse_staff` |
| Constants | UPPER_SNAKE | `MAX_BATCH_QUANTITY` |
| Methods | camelCase | `findByWarehouseId()` |

#### React (Frontend)

| Loại | Convention | Ví dụ |
|---|---|---|
| Components | PascalCase | `WarehouseDashboard.jsx` |
| Hooks | camelCase | `useInventory.js` |
| Utils | camelCase | `formatCurrency.js` |

#### API Endpoints

- Dùng **kebab-case**: `/api/v1/warehouse-stock`, `/api/v1/batch-management`
- Prefix: `/api/v1/[resource]`

---

## Domain Rules

Domain hàng hóa hiện tại của Phúc Anh là đồ gia dụng như nồi, chảo, đồ nhựa. Các mặt hàng này không quản lý serial từng sản phẩm, không quản lý hạn sử dụng, và không phân cấp chất lượng để bán lại. FIFO theo ngày nhận hàng là nguyên tắc xuất kho mặc định.

### Inventory Rules

```
1. inventories.total_qty >= 0, inventories.reserved_qty >= 0, and total_qty - reserved_qty >= 0 — luôn đúng trước và sau mọi thao tác
2. FIFO: chọn batch có received_date cũ nhất cho domain hàng gia dụng
3. Điều chỉnh tồn kho chỉ đi qua adjustments — không sửa trực tiếp
4. Kiểm tra version trước UPDATE để tránh ghi đè cạnh tranh
5. available = total - reserved >= 0 (kiểm tra trước khi xuất kho)
```

### Batch Rules

```
1. Batch gom hàng theo sản phẩm, nguồn nhập/chứng từ và ngày nhận; không tách theo serial, hạn sử dụng hoặc grade
2. Putaway phải kiểm tra bin_capacity trước khi đặt hàng vào bin
3. Hàng lỗi QC đi Quarantine để RTV/disposal; không phân loại lại thành cấp chất lượng khác
```

### QC & Transfer Rules

```
- Hàng fail QC → bắt buộc vào Quarantine Zone, tạo phiếu điều chỉnh tồn kho, trừ tồn kho hợp lệ và không tính vào available inventory
- Điều chuyển phải đi qua In-Transit location cho đến khi kho đích xác nhận
- Chênh lệch quantity_sent vs quantity_received → tạo adjustment/audit record
- Điều chuyển phải reserve FIFO eligible stock, loại Quarantine/inactive/locked location, và không cho tồn âm/reserved âm
- Outbound QC + load handover bắt buộc trước khi tài xế departure và xác nhận bằng ảnh; không yêu cầu Barcode/QR trong Sprint 1
- Hàng điều chuyển fail QC/hư hỏng vào Quarantine với origin INTERNAL_TRANSFER; chỉ xử lý tiêu hủy theo Spec 009, không tạo RTV/Debit Note NCC
- Thiếu hàng điều chuyển là chênh lệch số lượng, không phải hàng vật lý trong Quarantine
- Wrong-SKU phải báo theo line expected SKU/actual SKU/số lượng/lý do/ảnh nếu có; approved return phải có return departure/source arrival/handover
- Nhận thừa bị chặn khỏi regular inventory và phải ghi discrepancy hold/incident
- Putaway hàng đạt từ điều chuyển vào Bin phải kiểm tra bin capacity; chuyến xe điều chuyển phải kiểm tra tải trọng/thể tích xe
- Mutation transfer/request/trip/resource/inventory phải có version/concurrency guard; GET/list/detail không được mutate trạng thái
- Migration đã apply không được sửa/rename/xóa; schema fix phải dùng migration mới
```

### Core Business Entities

```
Warehouse         → Zone → Bin Location (sức chứa m³, kg)
Product           → SKU, PriceHistory (effective_date, end_date)
Batch             → receivedDate, source document/receipt, quantity
Inventory         → warehouse + product + batch + location (NEVER negative)
Receipt           → Lệnh nhập kho / Phiếu nhập kho
Issue             → Đơn xuất hàng / Phiếu xuất kho
Transfer          → Phiếu điều chuyển (qua In-Transit virtual warehouse)
Invoice           → Hóa đơn bán hàng
PaymentReceipt    → Phiếu thu tiền
CreditNote        → Phiếu ghi giảm công nợ (hàng hoàn trả)
DebitNote         → Phiếu đòi bồi hoàn (hàng lỗi trả NCC)
```

### Audit Log — Cấu trúc bắt buộc

Mọi thao tác nghiệp vụ **phải** ghi Audit Log:

| Field | Ý nghĩa |
|---|---|
| `timestamp` | Thời gian chính xác đến ms |
| `actor_id` | ID người thực hiện |
| `actor_role` | Vai trò người thực hiện |
| `action` | Tạo mới / Cập nhật / Phê duyệt / Hủy |
| `entity_type` | Phiếu xuất kho / Hóa đơn / Phiếu thu |
| `entity_id` | Mã đối tượng (DO-2026-001) |
| `old_value` | Giá trị trước thay đổi |
| `new_value` | Giá trị sau thay đổi |

---

## Git Conventions

### Branch Naming

```
feat/[feature-name]    # Tính năng mới (e.g., feat/inventory-fifo)
fix/[bug-name]         # Sửa lỗi (e.g., fix/negative-stock)
spec/[feature-name]    # Specification work
chore/[short-name]     # Maintenance tasks
```

### Commit Format

```
[type]([scope]): [description]

Types:  feat | fix | docs | style | refactor | test | chore
Scopes: inventory | receipt | issue | transfer | batch | delivery | ...

Ví dụ:
feat(inventory): add FIFO batch selection logic
fix(batch): correct received date ordering
docs(api): update warehouse-stock endpoint docs
```

### Pull Request Rules

- Tối thiểu **1 approval** trước khi merge
- Max **400 lines** changed per PR
- Tất cả CI checks phải pass
- Không còn TODO comments

---

## Definition of Done

Mỗi task hoàn thành khi đáp ứng **tất cả** các điều kiện sau:

- [ ] Unit tests viết và pass (min 80% coverage cho services)
- [ ] Integration tests cho tất cả API endpoints (happy + error paths)
- [ ] Không có linting/type errors (`mvn compile`, `eslint`)
- [ ] API endpoint được document trong OpenAPI/Swagger
- [ ] Error cases xử lý với proper HTTP status codes
- [ ] Audit log entry được tạo cho warehouse operations
- [ ] Không còn TODO comments trong code
- [ ] FIFO logic được test cho batch management

---

## Tài Liệu Tham Khảo

| Tài liệu | Nội dung |
|---|---|
| [AGENTS.md](./AGENTS.md) | Agent policy, tech stack, forbidden patterns, domain rules |
| [CLAUDE.md](./CLAUDE.md) | Kiến trúc chi tiết, workflow, swimlane diagrams, anti-patterns |
| [DESIGN.md](./DESIGN.md) | UI design tokens (Apple design system) |
| [Kiến trúc phân tầng các Actors.md](./Kiến%20trúc%20phân%20tầng%20các%20Actors.md) | 10 Actors, nghiệp vụ chi tiết, quy trình |
| [Userstory.md](./Userstory.md) | 27 User Stories đầy đủ với tiêu chí nghiệm thu |
| [specs/](./specs/) | Feature specifications (SDD) |

---

*Phiên bản: 1.0 | Cập nhật: 2026-05-29 | Dự án: WMS Phúc Anh — Sprint 1*
