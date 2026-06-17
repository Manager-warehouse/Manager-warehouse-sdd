# Hiến Pháp Dự Án — Warehouse Management System (WMS)

<!--
  Sync Impact Report
  Version change: N/A → v1.0.0
  Modified principles: (none — initial creation)
  Added sections: All (initial creation)
  Removed sections: (none)
  Templates requiring updates: N/A (no .sdd/templates/ yet)
  Follow-up TODOs: (none)
-->

**Phiên bản:** v1.0.0
**Ngày phê chuẩn:** 2026-05-29
**Ngày sửa đổi cuối:** 2026-05-29
**Trạng thái:** Có hiệu lực

---

## Lời Nói Đầu

Dự án **Warehouse Management System (WMS) — Công ty Phúc Anh** được xây dựng
nhằm số hóa quy trình quản lý kho tập trung cho 3 kho vật lý tại Hải Phòng,
Hà Nội, và Hồ Chí Minh. Hệ thống thay thế quy trình thủ công (giấy tờ, Excel)
bằng nền tảng kỹ thuật số thống nhất, đảm bảo các nghiệp vụ nhập, xuất, điều
chuyển, kiểm kê và truy vết tồn kho được thực thi chính xác, kiểm soát được,
và có audit trail đầy đủ.

Hiến pháp này là bộ luật bất di bất dịch của dự án. Mọi thành viên trong nhóm
phát triển — bao gồm con người và agent — MUST tuân thủ tuyệt đối. Bất kỳ đề
xuất sửa đổi nào phải đi qua quy trình Amendment được định nghĩa tại mục
Governance.

---

## 1. Phạm Vi Hệ Thống

### 1.1 Bao gồm

- Quản lý kho (WMS): nhập, xuất, điều chuyển, kiểm kê, tồn kho
- Kế toán nội bộ kho: giá vốn, chênh lệch tồn kho, điều chỉnh
- Điều phối vận tải nội bộ (chỉ xe của Phúc Anh, KHÔNG có 3PL)
- Kinh doanh & công nợ Đại lý

### 1.2 KHÔNG bao gồm

- Quản lý sản xuất (Manufacturing)
- HR / HRM
- Barcode / QR Scanner (sẽ tích hợp sau)
- Cổng B2B / B2C Portal
- Tích hợp hệ thống bên ngoài (sẽ tích hợp sau)

---

## 2. Technology Stack (Bất Di Bất Dịch)

| Layer            | Công nghệ                           |
| ---------------- | ----------------------------------- |
| **Backend**      | Spring Boot 3.4.5 + Java 21 (Maven) |
| **Frontend**     | React 18 + JavaScript               |
| **Database**     | PostgreSQL 18                       |
| **ORM**          | Spring Data JPA / Hibernate         |
| **Auth**         | JWT + bcrypt (cost factor ≥ 12)     |
| **Styling**      | Tailwind CSS 3.x                    |
| **Testing BE**   | JUnit 5 + Mockito                   |
| **Testing FE**   | Jest + React Testing Library        |
| **API Docs**     | OpenAPI / Swagger                   |
| **DB Migration** | Flyway                              |
| **Build BE**     | Maven                               |
| **Build FE**     | Vite                                |

**Quy tắc:** KHÔNG được thay đổi công nghệ nền tảng mà không có RFC và
approval từ toàn bộ nhóm. Thư viện bổ trợ (Lombok, MapStruct, Jackson,
Zustand, React Hook Form, Axios, Lucide Icons) có thể thêm mới miễn không
xung đột với stack chính.

---

## 3. Kiến Trúc & Nguyên Tắc Thiết Kế

### 3.1 Layered Architecture

Mọi module backend MUST tuân theo thứ tự:
**Controller → Service → Repository → Entity**

- **Controller**: nhận HTTP request, validation đầu vào, format response, gán
  HTTP status code. Không chứa business logic.
- **Service**: business logic, transaction management, audit logging,
  authorization checks. Gọi repository để truy xuất dữ liệu.
- **Repository**: Spring Data JPA interface. KHÔNG raw SQL trong application
  code.
- **Entity**: JPA entity mapping với database table.

### 3.2 REST API

- Base URL: `/api/v1/[resource]`
  Ví dụ: `/api/v1/warehouse-stock`, `/api/v1/receipts`, `/api/v1/transfers`
- Tài nguyên đặt tên bằng kebab-case: `delivery-orders`, `batch-management`
- HTTP status codes chuẩn: 200, 201, 204, 400, 401, 403, 404, 409, 422, 500
- Error response format chuẩn (timestamp, status, error, message, path, details)

### 3.3 Xử Lý Lỗi Tập Trung

- Sử dụng `@ControllerAdvice` / `@ExceptionHandler` để bắt và format lỗi
- Business rule violations → HTTP 422 (Unprocessable Entity)
- Validation errors → HTTP 400 (Bad Request)
- Version conflict → HTTP 409 (Conflict)

### 3.4 Audit Log Bắt Buộc

Mọi thao tác ghi dữ liệu trên kho MUST tạo audit log với:

- actor: user hiện tại (từ JWT)
- action: loại thao tác (CREATE, UPDATE, DELETE, APPROVE, REJECT, CANCEL)
- timestamp: thời điểm thực hiện
- before/after state: trạng thái trước và sau (nếu relevant)
- warehouse_id: mã kho (nếu có)
- reference_id: ID của bản ghi liên quan

---

## 4. Domain Rules (Luật Nghiệp Vụ Bất Di Bất Dịch)

### 4.1 Inventory Rules

1. **INV-01 (Non-negative inventory):** `inventory.quantity >= 0` MUST luôn
   đúng trước và sau mọi thao tác. Áp dụng DB constraint (`CHECK (quantity >= 0)`)
   VÀ application-level validation.
2. **INV-02 (FIFO default):** Domain hiện tại là hàng gia dụng như nồi, chảo,
   đồ nhựa và không quản lý hạn sử dụng. Batch được chọn ưu tiên theo ngày nhập
   cũ nhất (First In First Out).
3. **INV-03 (No expiry in current scope):** FEFO và expiry date không thuộc phạm
   vi domain hiện tại. Nếu sau này mở rộng sang nhóm hàng có hạn sử dụng thì phải
   tạo spec và migration riêng trước khi áp dụng.
4. **INV-04 (No direct inventory update):** Mọi thay đổi tồn kho MUST đi qua
   receipt, issue, transfer, adjustment, hoặc stocktake flows. KHÔNG được
   UPDATE/SET trực tiếp quantity trên entity Inventory.
5. **INV-05 (Optimistic locking):** Mọi UPDATE inventory MUST dùng
   `@Version` annotation. Conflict → HTTP 409 + retry.
6. **INV-06 (Available calculation):** `available = total - reserved >= 0`.
   Kiểm tra reserved quantity trước khi xuất kho. Không cho phép available âm.
7. **INV-07 (Adjustment thresholds):**
   - Chênh lệch 5-100 triệu: Trưởng kho duyệt
   - Chênh lệch > 100 triệu: CEO duyệt
   - Chênh lệch < 5 triệu: tự động duyệt nếu có QC/kiểm kê xác nhận

### 4.2 Batch Rules

1. **BAT-01 (Single grade):** Mỗi batch chỉ có 1 grade (A/B/C). Khác grade
   MUST tạo batch mới.
2. **BAT-02 (Bin capacity):** Putaway MUST kiểm tra `bin_capacity` trước khi
   đặt hàng vào bin. Không cho phép vượt quá sức chứa.
3. **BAT-03 (No expired batch handling):** Domain hàng gia dụng hiện tại không
   yêu cầu hạn sử dụng, không chọn theo FEFO, và không có nghiệp vụ batch hết hạn
   trong flow xuất kho thông thường.

### 4.3 QC & Quarantine Rules

1. **QC-01 (QC gate):** Hàng nhập kho MUST qua QC Inbound trước khi nhập
   chính thức. Hàng xuất kho MUST qua QC Outbound trước khi giao.
2. **QC-02 (Quarantine):** Hàng fail QC MUST được đánh dấu chờ xử lý
   quarantine/RTV và chỉ được ghi vào quarantine inventory sau xác nhận của
   người có thẩm quyền. Hàng trong quarantine MUST NOT được tính vào available
   inventory.
3. **QC-03 (QC result):** Hàng đạt QC → chờ phê duyệt nhập available inventory.
   Hàng lỗi → chờ xử lý quarantine/RTV. Hàng hỏng hoàn toàn → hủy theo quy
   trình được phê duyệt.

### 4.4 Transfer Rules

1. **TRF-01 (In-Transit):** Điều chuyển MUST đi qua In-Transit location.
   Inventory kho nguồn giảm → In-Transit tăng → Kho đích nhận → In-Transit
   giảm → Inventory kho đích tăng.
2. **TRF-02 (Discrepancy handling):** Chênh lệch giữa `quantity_sent` và
   `quantity_received` MUST tạo adjustment và audit record.
3. **TRF-03 (Confirmation):** Chỉ cập nhật inventory kho đích khi kho đích
   xác nhận nhận hàng.

### 4.5 Soft Delete Rules

1. **DEL-01 (Master data):** Soft delete bằng `is_active = false`.
   KHÔNG xóa vĩnh viễn bản ghi master data (Product, Warehouse, User, Dealer,
   Batch, BinLocation).
2. **DEL-02 (Transaction data):** Cancel transaction bằng `status = cancelled`.
   KHÔNG xóa vĩnh viễn transaction history (Receipt, Issue, Transfer,
   Adjustment, StockTake, DeliveryOrder, Trip, Invoice, Payment).
3. **DEL-03 (No physical deletion):** Tuyệt đối không DELETE rows khỏi bảng
   nghiệp vụ. Foreign key integrity MUST được duy trì.

---

## 5. Forbidden Patterns

MUST NOT:

1. Lưu secrets, passwords, hoặc API keys trong source control hoặc file `.env`
   đã commit.
2. Bỏ qua Jakarta Validation / request DTO validation trên write endpoints.
3. UPDATE inventory trực tiếp khi thao tác phải đi qua adjustment, receipt,
   issue, hoặc transfer flows.
4. Trộn lẫn nhiều grade (A/B/C) trong cùng một batch.
5. Bỏ qua optimistic locking / version check trên inventory updates.
6. Hardcode warehouse IDs, role assumptions, hoặc approval state transitions
   mà không có domain constants hoặc lookup rules.
7. Soft-delete transaction history bằng physical deletion; dùng status-based
   cancellation rules.
8. Để TODO comments trong completed task code.
9. Dùng `System.out` / `console.log` trong production code.
10. Commit trực tiếp vào `main` hoặc `production` branch.

---

## 6. Development Standards

### 6.1 Code Style & Structure

- Java classes: PascalCase (ví dụ: `InventoryService.java`)
- React components (JS): PascalCase (ví dụ: `ReceiptTable.jsx`)
- React utilities/hooks (JS): camelCase (ví dụ: `formatQuantity.js`,
  `useTransferFilters.js`)
- API endpoints: kebab-case (ví dụ: `/api/v1/warehouse-stock`)
- Database tables: snake_case (ví dụ: `inventory_transactions`)
- Constructor injection ưu tiên (field injection tránh dùng)
- Max function length: 40 lines
- Max file length: 300 lines
- Comments giải thích **why**, không giải thích **what**

### 6.2 Database Access

- Tất cả database access MUST qua Spring Data JPA / Hibernate
- KHÔNG raw SQL trong application code
- Flyway cho DB migration
- Migration files không được xóa sau khi đã áp dụng

### 6.3 Security

- JWT authentication với `Authorization: Bearer {token}` header
- bcrypt password encoding (cost factor ≥ 12)
- RBAC: kiểm tra cả role VÀ warehouse scope
- Mọi endpoint (trừ `/auth/login`, `/auth/refresh`) MUST check authentication

### 6.4 Frontend Conventions

- React functional components với hooks
- State management qua Zustand (global) và React Hook Form (form-local)
- HTTP client: Axios với interceptor cho JWT refresh
- Styling: Tailwind CSS utility classes, không dùng CSS modules trừ khi
  thực sự cần thiết

---

## 7. Testing Requirements

1. **Coverage:** Service/Business logic coverage MUST ≥ 80% (lines + branches)
2. **Unit tests:** MUST viết cho tất cả service methods (happy path + error
   paths)
3. **Integration tests:** MUST viết cho tất cả API endpoints (happy path +
   error paths)
4. **Batch logic:** FIFO allocation logic MUST có test riêng
5. **Inventory boundary:** Negative inventory, reserved > total, version
   conflict MUST có test
6. **Frontend:** Component test với Jest + React Testing Library cho các
   component có business logic
7. **No linting errors:** `maven compile` và `eslint` MUST pass
8. **Swagger/OpenAPI:** MUST update khi thêm hoặc sửa endpoint

---

## 8. Git & PR Conventions

### 8.1 Branch Naming

| Prefix   | Usage                     |
| -------- | ------------------------- |
| `feat/`  | Tính năng mới             |
| `fix/`   | Sửa lỗi                   |
| `spec/`  | Viết spec / tài liệu      |
| `chore/` | Cập nhật nhỏ, maintenance |

### 8.2 Commit Format

```
[type]([scope]): [description]
```

Ví dụ: `feat(inventory): add FIFO batch allocation logic`

### 8.3 PR Rules

- Min 1 approval trước khi merge
- Max 400 lines changed (larger work split thành nhiều PR)
- KHÔNG commit trực tiếp vào `main` / `production`

---

## 9. Definition of Done (DoD)

Một task chỉ được coi là **hoàn thành** khi tất cả các điều kiện sau được
đáp ứng:

- [ ] Unit tests written and passing (min 80% coverage cho services)
- [ ] Integration tests cho all API endpoints (happy + error paths)
- [ ] No linting/type errors (`maven compile`, `eslint`)
- [ ] API endpoint documented in OpenAPI/Swagger
- [ ] Error cases handled với proper HTTP status codes
- [ ] Audit log entry created cho warehouse operations
- [ ] No TODO comments left in code
- [ ] FIFO allocation logic tested cho batch management

---

## 10. Governance & Amendment

### 10.1 Amendment Procedure

1. Bất kỳ thành viên nào trong nhóm có thể đề xuất sửa đổi Hiến pháp bằng
   cách tạo PR với label `constitution-amendment`.
2. PR MUST bao gồm lý do sửa đổi và phân tích tác động lên các artifacts
   liên quan (templates, specs, plan).
3. Amendment cần tối thiểu 2 approvals từ nhóm phát triển.
4. Sau khi được merge, phiên bản Hiến pháp MUST được cập nhật theo quy tắc
   semantic versioning.

### 10.2 Versioning Policy

- **MAJOR**: Thay đổi backward-incompatible (xóa hoặc định nghĩa lại nguyên
  tắc nền tảng).
- **MINOR**: Thêm nguyên tắc mới hoặc mở rộng hướng dẫn hiện có.
- **PATCH**: Làm rõ wording, sửa lỗi chính tả, tinh chỉnh không thay đổi
  ngữ nghĩa.

### 10.3 Compliance Review

- Đầu mỗi sprint, nhóm MUST review Hiến pháp để đảm bảo không có xung đột
  với mục tiêu sprint hiện tại.
- Mọi RFC cho thay đổi kiến trúc MUST được đối chiếu với Hiến pháp trước
  khi phê duyệt.
- Violation của Hiến pháp MUST được ghi nhận và khắc phục trước khi coi
  task là done.

### 10.4 File Location & Maintenance

- Hiến pháp được lưu tại `.sdd/constitution.md`
- Template cho spec: `.sdd/templates/spec-template.md` (nếu có)
- Template cho plan: `.sdd/templates/plan-template.md` (nếu có)
- Template cho tasks: `.sdd/templates/tasks-template.md` (nếu có)
- Khi Hiến pháp thay đổi, các template trên MUST được kiểm tra và cập nhật
  nếu cần

---

_Hiến pháp này có hiệu lực kể từ ngày phê chuẩn. Mọi code và artifact trong
dự án MUST tuân thủ._

---

## Phụ Lục A: Danh sách Feature Specs

Toàn bộ hệ thống WMS Phúc Anh được phân rã thành **70 features** trong **10 domain specs**:

| #   | Spec                     | Đường dẫn                                          | Features |   P1   |
| --- | ------------------------ | -------------------------------------------------- | :------: | :----: |
| 001 | Quản trị & Phân quyền    | `.sdd/specs/001-security-auth-rbac-audit/spec.md`  |    7     |   5    |
| 002 | Danh mục nền tảng        | `.sdd/specs/002-master-data-management/spec.md`    |    9     |   5    |
| 003 | Nhập hàng & QC Inbound   | `.sdd/specs/003-inbound-receipt-qc/spec.md`        |    9     |   9    |
| 004 | Xuất hàng & Giao hàng    | `.sdd/specs/004-outbound-delivery-pod/spec.md`     |    6     |   6    |
| 005 | Điều chuyển nội bộ       | `.sdd/specs/005-inter-warehouse-transfer/spec.md`  |    6     |   5    |
| 006 | Kiểm kê & Điều chỉnh     | `.sdd/specs/006-stocktake-adjustment/spec.md`      |    4     |   4    |
| 007 | Bảng giá & Giá vốn       | `.sdd/specs/007-pricing-cogs-management/spec.md`   |    5     |   5    |
| 008 | Tài chính & Công nợ      | `.sdd/specs/008-finance-billing-closing/spec.md`   |    10    |   10   |
| 009 | Hàng hoàn trả & Tiêu hủy | `.sdd/specs/009-returns-scrap-disposal/spec.md`    |    3     |   2    |
| 010 | Báo cáo & Cảnh báo       | `.sdd/specs/010-reports-dashboards-alerts/spec.md` |    3     |   2    |
|     | **Tổng cộng**            |                                                    |  **70**  | **61** |

Mỗi spec sử dụng cấu trúc 9 thành phần: Context & Goal, Actor, Functional Requirements (EARS),
Non-functional Requirements, Data Model, API Spec, Error Handling, Acceptance Criteria, Out of Scope.
