# Feature Specification: 3-Week Sprint Plan - Core WMS Operations

**Feature Branch**: `002-3-week-sprint-plan`

**Created**: 2026-05-29

**Status**: Draft

**Input**: User description: "Lập kế hoạch code 3 tuần cho WMS – Core Warehouse Operations (Sprint 1)"

## User Scenarios & Testing

### User Story 1 - Inbound: Nhập hàng & QC (Priority: P1)

Planner tiếp nhận thông tin hàng về từ Công ty mẹ → lập Lệnh nhập kho → Thủ kho nhập hàng thực tế → Nhân viên kho kiểm QC Inbound → Hàng lỗi vào Quarantine Zone → Trưởng kho duyệt nhập kho chính thức → Hệ thống cộng tồn.

**Why this priority**: Quy trình nhập hàng là đầu vào của toàn bộ hệ thống tồn kho. Không có nhập hàng thì không có gì để quản lý.

**Independent Test**: Planner tạo Pending Receipt → Employee nhập số lượng QC Đạt/Lỗi → Trưởng kho duyệt → Inventory tăng chính xác.

**Acceptance Scenarios**:
1. **Given** hàng chưa nhập kho, **When** Planner tạo lệnh nhập và Thủ kho xác nhận số lượng thực tế + QC đạt, **Then** tồn kho + available tăng đúng số lượng QC đạt, hàng lỗi vào quarantine.
2. **Given** lệnh nhập đã duyệt, **When** user cố nhập lại cùng lệnh, **Then** hệ thống báo lỗi duplicate.

---

### User Story 2 - Outbound: Xuất hàng & Delivery (Priority: P1)

Planner nhận yêu cầu từ Công ty mẹ → Credit Check tự động → Lập Đơn xuất hàng → Thủ kho soạn hàng → QC Outbound → Ready to Ship → Dispatcher lập Chuyến xe → Tài xế giao hàng → POD.

**Why this priority**: Xuất hàng là quy trình tạo doanh thu. Toàn bộ chuỗi credit check → soạn hàng → giao hàng phải vận hành trơn tru.

**Independent Test**: Planner tạo DO → Thủ kho soạn hàng → QC duyệt → Dispatcher giao → POD.

**Acceptance Scenarios**:
1. **Given** Đại lý có công nợ quá hạn > 30 ngày, **When** Planner lập Đơn xuất, **Then** hệ thống tự động chặn với trạng thái CREDIT_HOLD.
2. **Given** Đơn xuất đã duyệt, **When** Thủ kho soạn hàng đủ số lượng, **Then** available inventory giảm đúng.

---

### User Story 3 - Transfer: Điều chuyển nội bộ (Priority: P1)

Planner tạo phiếu điều chuyển → Trưởng kho nguồn duyệt → Inventory chuyển sang In-Transit → Kho đích xác nhận nhận hàng → Inventory cập nhật tại kho đích.

**Why this priority**: Điều chuyển giữa 3 kho là nghiệp vụ cốt lõi. In-Transit tracking là điểm khác biệt.

**Independent Test**: Planner tạo transfer → Checker duyệt → Kho nguồn xuất → Kho đích nhận → Tồn kho 2 kho thay đổi chính xác.

**Acceptance Scenarios**:
1. **Given** tồn kho nguồn đủ, **When** Planner tạo transfer và Trưởng kho duyệt, **Then** inventory nguồn giảm, in-transit tăng.
2. **Given** hàng đang in-transit, **When** kho đích xác nhận nhận, **Then** in-transit giảm, inventory đích tăng.

---

### User Story 4 - Inventory: Kiểm kê & Điều chỉnh (Priority: P1)

Thủ kho tạo phiếu kiểm kê → Đếm thực tế → Nhập kết quả → Hệ thống tính chênh lệch → Trưởng kho duyệt (5-100M) / CEO duyệt (>100M) → Điều chỉnh inventory.

**Why this priority**: Kiểm kê đảm bảo tính chính xác của inventory. Chênh lệch cần được xử lý và audit.

**Independent Test**: Thủ kho tạo StockTake → Nhập số lượng thực tế → Checker duyệt → Inventory điều chỉnh.

**Acceptance Scenarios**:
1. **Given** tồn kho hệ thống khác thực tế, **When** Thủ kho nhập số liệu kiểm kê và chênh lệch được duyệt, **Then** inventory quantity cập nhật bằng số thực tế.
2. **Given** chênh lệch > 100M, **When** Trưởng kho cố duyệt, **Then** hệ thống từ chối và yêu cầu lên CEO.

---

### User Story 5 - Admin: Quản trị hệ thống & RBAC (Priority: P1)

System Admin tạo tài khoản, phân quyền Role + Warehouse RBAC → User login với JWT → Chỉ thao tác được trên kho được phân quyền.

**Why this priority**: Bảo mật và phân quyền là yêu cầu nền tảng. Không có auth thì không có hệ thống.

**Independent Test**: Admin tạo user với role Thủ kho, chỉ assign kho Hà Nội → User login → Chỉ thấy và thao tác trên kho Hà Nội.

**Acceptance Scenarios**:
1. **Given** user không có quyền trên kho HCM, **When** user gọi API update inventory kho HCM, **Then** 403 Forbidden.

---

### User Story 6 - Finance: Kế toán nội bộ & Công nợ Đại lý (Priority: P2)

Đơn hàng Delivered → Kế toán lập Invoice → Cộng dồn công nợ → Credit Check tự động trên đơn mới → Đại lý thanh toán → Cấn trừ công nợ → Mở khóa tín dụng.

**Why this priority**: Sprint 1 tập trung WMS core. Kế toán cơ bản cho phép vận hành khép kín, nhưng có thể delayed nếu WMS core chưa vững.

**Independent Test**: Invoice được tạo → Balance tăng → Thanh toán → Balance giảm → Tín dụng mở lại.

**Acceptance Scenarios**:
1. **Given** Đại lý vượt credit limit, **When** Planner tạo đơn mới, **Then** hệ thống HOLD.
2. **Given** Đại lý thanh toán đủ về dưới 80% limit, **When** hệ thống kiểm tra, **Then** trạng thái về ACTIVE.

---

### Edge Cases

- Hàng nhập về nhưng QC fail 100%: Toàn bộ vào Quarantine → không tăng available inventory → chờ xử lý (hủy/trả NCC).
- Số lượng nhận điều chuyển khác số lượng gửi: Tự động tạo adjustment record cho phần chênh lệch.
- Tồn kho available = 0 nhưng reserved > 0: Không thể xuất thêm.
- Batch hết hạn: Bị exclude khỏi FEFO selection, chỉ available nếu có luồng đặc biệt.
- Credit limit thay đổi khi đã có đơn CREDIT_HOLD: Đơn cũ vẫn HOLD, chỉ ảnh hưởng đơn mới.
- Kiểm kê đang tiến hành: Các thao tác nhập/xuất trên bin đó bị tạm khóa (lock).

## Requirements

### Functional Requirements

- **FR-001**: System MUST authenticate users via JWT (access + refresh tokens) with bcrypt password hashing (cost >= 12).
- **FR-002**: System MUST enforce RBAC with both Role-based and Warehouse-scoped permissions.
- **FR-003**: System MUST support full Inbound flow: Pending Receipt → QC → Putaway → Inventory update.
- **FR-004**: System MUST support full Outbound flow: Delivery Order → Picking → QC Outbound → Loading → Delivery → POD.
- **FR-005**: System MUST support Transfer flow with In-Transit virtual warehouse tracking.
- **FR-006**: System MUST enforce FEFO expiry-based batch selection for goods with expiry, FIFO for goods without.
- **FR-007**: System MUST enforce `available = total - reserved >= 0` before any issue operation.
- **FR-008**: System MUST create audit log entries for ALL inventory mutations with actor, action, timestamp, before/after state.
- **FR-009**: System MUST enforce optimistic locking (@Version) on all inventory UPDATE operations.
- **FR-010**: System MUST support StockTake: creation → counting → variance calc → approval → adjustment.
- **FR-011**: System MUST split failed QC goods into Quarantine Zone, excluded from available inventory.
- **FR-012**: System MUST support Dealer credit management: limit config, auto credit check, CREDIT_HOLD, payment, balance tracking.
- **FR-013**: System MUST support Dispatcher flow: trip creation, driver assignment, stop ordering, POD confirmation.
- **FR-014**: System MUST support basic Accounting: Invoice creation, payment recording, balance tracking.
- **FR-015**: All API endpoints MUST be documented in OpenAPI/Swagger.

### Key Entities

- **Warehouse**: 3 physical warehouses (Hai Phong, Ha Noi, HCMC) + virtual In-Transit warehouse.
- **Product/SKU**: Master product data with category, unit, has_serial, has_expiry flags.
- **Batch**: Lot/batch tracking, expiry date, single grade (A/B/C), linked to inventory.
- **Inventory**: Bin-level stock with quantity, reserved_qty, version for optimistic locking.
- **Bin Location**: Physical storage location within warehouse with capacity, zone, type.
- **Receipt**: Pending receipt → QC → Putaway → Completed flow.
- **DeliveryOrder**: Sales order → Picking → QC → Ready to Ship → Delivered flow.
- **TransferOrder**: Inter-warehouse transfer with In-Transit → Received flow.
- **StockTake**: Periodic count with variance and approval workflow.
- **User/Account**: System users with role and warehouse assignments.
- **AuditLog**: Immutable log of all business operations.
- **Dealer**: Customer/dealer with credit limit, current balance, credit status.
- **Invoice**: Billing document linked to delivery.
- **Payment**: Payment record linked to dealer balance.
- **Trip**: Delivery trip with vehicle, driver, stops, status.

## Success Criteria

### Measurable Outcomes

- **SC-001**: All P1 user stories (Nhap, Xuat, Dieu chuyen, Kiem ke, Admin) fully implemented and tested end-to-end by end of Week 3.
- **SC-002**: Backend API test coverage >= 80% for all service-layer business logic.
- **SC-003**: All inventory mutations create audit log entries with actor, action, timestamp, before/after state.
- **SC-004**: All API endpoints include proper Jakarta Validation, centralized error handling, Swagger docs.
- **SC-005**: All 3 warehouse physical locations configured and functional with RBAC isolation.
- **SC-006**: FEFO/FIFO batch selection logic tested and verified.

## Assumptions

- Team co 1 senior BE developer + 1 FE developer lam full-time trong 3 tuan.
- Backend code duoc uu tien truoc, Frontend dung mock API truoc.
- Moi truong Dev dung PostgreSQL local, Flyway cho migration.
- Git feature branch workflow: moi US la mot nhanh nho, merge vao 002-3-week-sprint-plan.
- Maven build, npm build, Docker Compose co san.
- Khong tich hop barcode/QR scanner trong Sprint 1.
- UI design dung Apple Design System tokens tu DESIGN.md.
