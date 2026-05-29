# Version: 4.0 | Updated: 2026-05-26 | Project: Warehouse Management System (WMS)

## 1. PROJECT OVERVIEW

Name: Warehouse Management System (WMS)
Type: Full-stack Web Application + REST API
Domain: Warehouse / Inventory / Logistics Operations
Stage: Development (Sprint 1)

Bạn là một kỹ sư phần mềm senior trong dự án WMS.
Mục tiêu chính: Xây dựng hệ thống quản lý kho cho doanh nghiệp thương mại với 3 kho vật lý tại Hải Phòng, Hà Nội, và Hồ Chí Minh; đảm bảo nghiệp vụ nhập, xuất, điều chuyển, kiểm kê, và truy vết tồn kho được thực thi chính xác, kiểm soát được, và có audit trail đầy đủ.

Đọc trước:

1. `CLAUDE.md` — kiến trúc hệ thống, workflow, patterns, conventions
2. `CONSTITUTION.md` — development principles và team agreements
3. File này — quy tắc vận hành cụ thể cho agent

## 2. TECH STACK (STRICT — do not deviate)

Backend: Spring Boot 3.4.5 + Java 21 maven
Frontend: React 18
Database: PostgreSQL 18
ORM: Spring Data JPA
Auth: JWT + bcrypt (cost factor >= 12)
Testing: JUnit 5 + Mockito (backend), Jest (frontend)
Styling: Tailwind CSS 3.x

## 3. ARCHITECTURE PRINCIPLES

- Follow layered architecture: Controller -> Service -> Repository -> Entity
- API style: REST with `/api/v1/[resource]` prefix where applicable
- Error handling: centralized exception handling with proper HTTP status codes
- All database access goes through Spring Data JPA / Hibernate; no raw SQL in application code
- No `System.out` or `console.log` in production code; use project logging conventions
- All warehouse mutations must create audit log entries with actor, action, timestamp, and before/after state where relevant
- Business rules around inventory, batch, QC, reservation, and transfer state are mandatory system invariants, not optional validations

## 4. FILE NAMING & STRUCTURE

Java classes: PascalCase (e.g. `InventoryService.java`)
TypeScript React components: PascalCase (e.g. `ReceiptTable.tsx`)
TypeScript utilities/hooks: camelCase (e.g. `formatQuantity.ts`, `useTransferFilters.ts`)
API endpoints: kebab-case resource naming (e.g. `/api/v1/warehouse-stock`)
Database tables: snake_case (e.g. `inventory_transactions`)
Specs: `specs/[number]-[feature-name]/`

## 5. PHẠM VI HOẠT ĐỘNG

### Được phép

- Đọc và chỉnh sửa code trong các module Backend, Frontend
- Chạy: Maven build, npm build, pytest, docker compose
- Tạo branch mới theo pattern: `feat/*`, `fix/*`, `spec/*`, `chore/*`

### Cấm tuyệt đối

- KHÔNG được xóa migration files hoặc dữ liệu trong `/data`, `/uploads`
- KHÔNG được commit trực tiếp vào `main` hoặc `production`
- KHÔNG được đọc: `.env`, `credentials`, `secrets`
- KHÔNG được bỏ qua input validation trên API endpoints
- KHÔNG được cho phép tồn kho âm (negative inventory)
- KHÔNG được bỏ qua QC check trước khi nhập kho
- KHÔNG được bỏ qua audit logging cho các thao tác kho

## 6. FORBIDDEN PATTERNS

- NEVER store secrets, passwords, or API keys in source control or committed `.env` files
- NEVER bypass Jakarta Validation or request DTO validation on write endpoints
- NEVER update inventory directly when the operation should go through adjustment, receipt, issue, or transfer flows
- NEVER mix multiple grades (A/B/C) inside a single batch
- NEVER bypass optimistic locking / version checks on inventory updates
- NEVER hardcode warehouse IDs, role assumptions, or approval state transitions without clear domain constants or lookup rules
- NEVER soft-delete transaction history by physical deletion; use status-based cancellation rules
- NEVER leave TODO comments in completed task code

## 7. WMS DOMAIN RULES

### Inventory rules

1. `inventory.quantity >= 0` luôn đúng trước và sau mọi thao tác
2. FEFO: chọn batch có hạn dùng gần nhất còn hợp lệ cho sản phẩm có expiry
3. FIFO: chọn batch có `received_date` cũ nhất cho sản phẩm không có expiry
4. Điều chỉnh tồn kho chỉ đi qua adjustments, không sửa trực tiếp inventory
5. Phải kiểm tra version trước `UPDATE` để tránh ghi đè cạnh tranh
6. Phải kiểm tra reserved quantity trước khi xuất kho: `available = total - reserved >= 0`

### Batch rules

1. Mỗi batch chỉ có 1 grade (A/B/C); khác grade phải tạo batch mới
2. Sản phẩm `has_serial = true` phải nhập serial khi nhập và xuất
3. Putaway phải kiểm tra `bin_capacity` trước khi đặt hàng vào bin
4. Batch hết hạn không được chọn cho flow xuất kho thông thường

### QC and transfer rules

- Hàng fail QC phải vào quarantine zone và không được tính vào available inventory
- Điều chuyển phải đi qua In-Transit location cho đến khi kho đích xác nhận nhận hàng
- Chênh lệch giữa `quantity_sent` và `quantity_received` phải tạo adjustment/audit record phù hợp

### Soft delete rules

- Master data: `is_active = false`
- Transaction data: `status = cancelled`
- Không xóa vĩnh viễn dữ liệu nghiệp vụ

## 8. CODE & QUALITY RULES

- Java theo conventions của Spring Boot project hiện tại; ưu tiên constructor injection
- TypeScript strict mode; không dùng `any` nếu không có lý do rất rõ ràng
- Max function length: 40 lines khi khả thi; max file length: 300 lines theo `CONSTITUTION.md`
- Comments giải thích `why`, không giải thích `what`
- Test coverage tối thiểu: 80% cho service/business logic mới
- Không bỏ qua Swagger/OpenAPI update khi thêm hoặc sửa endpoint

## 9. XỬ LÝ LỖI & AN TOÀN THAO TÁC

- Nếu yêu cầu mơ hồ hoặc thiếu domain context quan trọng, hỏi lại thay vì đoán
- Luôn kiểm tra tác động nghiệp vụ trước khi sửa flow inventory, receipt, issue, transfer, adjustment
- Trước thay đổi có rủi ro cao, phải đọc code liên quan trong `CLAUDE.md`, spec hiện hành, và module lân cận
- Với thao tác có thể phá hủy dữ liệu hoặc thay đổi rộng, phải nêu rõ rủi ro trước khi thực hiện
- Nếu không thể chạy công cụ phân tích bắt buộc của repo trong môi trường hiện tại, phải nêu rõ giới hạn đó trong báo cáo

## 10. DEFINITION OF DONE (per task)

- [ ] Unit tests written and passing (min 80% coverage cho services)
- [ ] Integration tests cho all API endpoints (happy + error paths)
- [ ] No linting/type errors (`maven compile`, `eslint`)
- [ ] API endpoint documented in OpenAPI/Swagger
- [ ] Error cases handled với proper HTTP status codes
- [ ] Audit log entry created cho warehouse operations
- [ ] No TODO comments left in code
- [ ] FEFO/FIFO logic tested cho batch management

## 11. GIT CONVENTIONS

### Branch naming

`feat/[feature-name]` — tính năng mới
`fix/[bug-name]` — sửa lỗi
`spec/[feature-name]` — viết spec
`chore/[short-name]` — cập nhật nhỏ

### Commit format

`[type]([scope]): [description]`

Example:
`feat(inventory): add FEFO batch selection logic`

### PR rules

- Min 1 approval before merge
- Max 400 lines changed; larger work should be split
- Never commit trực tiếp vào `main`/`production`

## 12. CURRENT SPRINT CONTEXT

Sprint: Sprint 1
Focus: Core Warehouse Operations — Inventory, Receipt, Issue, Transfer
Active specs: `specs/001-warehouse-management-system/spec.md`
Pending: Integration specs for Accounting/HRM/Sale APIs

## 13. PROJECT CONTEXT REFERENCES

- `CLAUDE.md` — hệ thống kiến trúc, workflow, lessons learned, anti-patterns
- `CONSTITUTION.md` — project law, testing requirements, immutable stack rules
- `specs/001-warehouse-management-system/spec.md` — active product spec cho sprint hiện tại

## 14. GITNEXUS INTEGRATION

### Always do

- MUST run impact analysis before editing any symbol
- MUST run `gitnexus_detect_changes()` before committing
- MUST warn if impact analysis returns HIGH or CRITICAL risk

### Never do

- NEVER edit without running `gitnexus_impact` first
- NEVER ignore HIGH or CRITICAL risk warnings
- NEVER rename with find-and-replace; use `gitnexus_rename`

### Resources

| Resource                             | Use for              |
| ------------------------------------ | -------------------- |
| `gitnexus://repo/document/context`   | Codebase overview    |
| `gitnexus://repo/document/clusters`  | All functional areas |
| `gitnexus://repo/document/processes` | All execution flows  |

Note: Nếu môi trường hiện tại không có GitNexus tooling, agent phải báo rõ không thể thực thi automation này trước khi tiếp tục các thay đổi thủ công.
