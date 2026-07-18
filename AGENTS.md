# Version: 5.0 | Updated: 2026-05-30 | Project: Warehouse Management System (WMS)

## 1. PROJECT OVERVIEW

Name: Warehouse Management System (WMS)
Type: Full-stack Web Application + REST API
Domain: Warehouse / Inventory / Logistics Operations
Stage: Development (Sprint 1)

Bạn là một kỹ sư phần mềm senior trong dự án WMS.
Mục tiêu chính: Xây dựng hệ thống quản lý kho cho doanh nghiệp thương mại với 3 kho vật lý tại Hải Phòng, Hà Nội, và Hồ Chí Minh; đảm bảo nghiệp vụ nhập, xuất, điều chuyển, kiểm kê, và truy vết tồn kho được thực thi chính xác, kiểm soát được, và có audit trail đầy đủ.
Domain hàng hóa hiện tại: đồ gia dụng như nồi, chảo, đồ nhựa; không quản lý serial từng sản phẩm, không quản lý hạn sử dụng, và không phân cấp chất lượng để bán lại. FIFO theo ngày nhận hàng là nguyên tắc xuất kho mặc định.

Đọc trước:

1. `CLAUDE.md` — kiến trúc hệ thống, workflow, patterns, conventions
2. `.specify/memory/constitution.md` — canonical constitution, development principles và team agreements
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
React components (JavaScript): PascalCase (e.g. `ReceiptTable.jsx`)
React utilities/hooks (JavaScript): camelCase (e.g. `formatQuantity.js`, `useTransferFilters.js`)
API endpoints: kebab-case resource naming (e.g. `/api/v1/warehouse-stock`)
Database tables: snake_case (e.g. `inventory_transactions`)
Specs: `specs/[number]-[feature-name]/`
Specs (SDD): `.sdd/specs/[number]-[feature-name]/`

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
- NEVER add per-unit tracking, expiry-date tracking, or quality-tier requirements for household-goods inventory unless the business model is explicitly changed in a future approved spec
- NEVER bypass optimistic locking / version checks on inventory updates
- NEVER hardcode warehouse IDs, role assumptions, or approval state transitions without clear domain constants or lookup rules
- NEVER soft-delete transaction history by physical deletion; use status-based cancellation rules
- NEVER leave TODO comments in completed task code

## 7. WMS DOMAIN RULES

### Inventory rules

1. `inventories.total_qty >= 0`, `inventories.reserved_qty >= 0`, và `total_qty - reserved_qty >= 0` luôn đúng trước và sau mọi thao tác
2. Domain hiện tại là hàng gia dụng không serial, không hạn sử dụng, không grade; FIFO là nguyên tắc xuất kho mặc định
3. Điều chỉnh tồn kho chỉ đi qua adjustments, không sửa trực tiếp inventory
4. Phải kiểm tra version trước `UPDATE` để tránh ghi đè cạnh tranh
5. Phải kiểm tra reserved quantity trước khi xuất kho: `available = total - reserved >= 0`

### Batch rules

1. Batch dùng để gom hàng theo sản phẩm, nguồn nhập/chứng từ và ngày nhận; không tách batch theo serial, hạn sử dụng hoặc grade
2. Putaway phải kiểm tra `bin_capacity` trước khi đặt hàng vào bin
3. Hàng lỗi QC đi Quarantine để xử lý trả NCC/tiêu hủy theo flow phê duyệt; không phân loại lại thành cấp chất lượng khác

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
- JavaScript clean code; tránh lạm dụng các biến không khai báo hoặc cấu trúc lỏng lẻo
- Max function length: 40 lines khi khả thi; max file length: 300 lines theo `.specify/memory/constitution.md`
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
- [ ] FIFO, negative inventory, reserved quantity, and version conflict paths tested when touched

## 11. GIT CONVENTIONS

### Branch naming

`feat/[feature-name]` — tính năng mới
`fix/[bug-name]` — sửa lỗi
`spec/[feature-name]` — viết spec
`chore/[short-name]` — cập nhật nhỏ

### Commit format

`[type]([scope]): [description]`

Example:
`feat(inventory): add FIFO batch selection logic`

### PR rules

- Min 1 approval before merge
- Max 400 lines changed; larger work should be split
- Never commit trực tiếp vào `main`/`production`

## 12. CURRENT SPRINT CONTEXT

Sprint: Sprint 1
Focus: Core Warehouse Operations — Inventory, Receipt, Issue, Transfer
Active specs:

### .sdd/specs/ (10 feature specs)
| # | Spec | Features | Priority |
|---|------|----------|----------|
| 001 | 001-security-auth-rbac-audit | Auth, RBAC, Audit Log, System Config | P1 |
| 002 | 002-master-data-management | Products, Warehouses, Bins, Dealers, Suppliers, Vehicles, Drivers | P1/P2 |
| 003 | 003-inbound-receipt-qc | Receiving, QC Inbound, Putaway, Quarantine | P1 |
| 004 | 004-outbound-delivery-pod | DO, Picking, QC Outbound, Trip, POD | P1 |
| 005 | 005-inter-warehouse-transfer | Transfer, In-Transit, Planning | P1 |
| 006 | 006-stocktake-adjustment | StockTake, Variance, Approval | P1 |
| 007 | 007-pricing-cogs-management | Price List, COGS, Price History | P1 |
| 008 | 008-finance-billing-closing | Invoice, Payment, Credit, Closing | P1 |
| 009 | 009-returns-scrap-disposal | Returns, Credit Note, Disposal | P2 |
| 010 | 010-reports-dashboards-alerts | Dashboard, Low Stock Alerts | P1 |


## 13. PROJECT CONTEXT REFERENCES

- `CLAUDE.md` — hệ thống kiến trúc, workflow, lessons learned, anti-patterns
- `.specify/memory/constitution.md` — canonical project law, testing requirements, immutable stack rules
- .sdd/specs/ (10 specs, 35 features, 30 P1) — feature specs chi tiết theo từng domain
- `Userstory.md` — 27 user stories được định nghĩa

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

<!-- SPECKIT START -->
<!-- Active Plan: .sdd/specs/012-frontend-testing/plan.md -->
<!-- SPECKIT END -->

<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **Manager-warehouse-sdd** (11711 symbols, 29938 relationships, 300 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> If any GitNexus tool warns the index is stale, run `npx gitnexus analyze` in terminal first.

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `gitnexus_impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `gitnexus_detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `gitnexus_query({query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `gitnexus_context({name: "symbolName"})`.

## Never Do

- NEVER edit a function, class, or method without first running `gitnexus_impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `gitnexus_rename` which understands the call graph.
- NEVER commit changes without running `gitnexus_detect_changes()` to check affected scope.

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/Manager-warehouse-sdd/context` | Codebase overview, check index freshness |
| `gitnexus://repo/Manager-warehouse-sdd/clusters` | All functional areas |
| `gitnexus://repo/Manager-warehouse-sdd/processes` | All execution flows |
| `gitnexus://repo/Manager-warehouse-sdd/process/{name}` | Step-by-step execution trace |

## CLI

| Task | Read this skill file |
|------|---------------------|
| Understand architecture / "How does X work?" | `.claude/skills/gitnexus/gitnexus-exploring/SKILL.md` |
| Blast radius / "What breaks if I change X?" | `.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?" | `.claude/skills/gitnexus/gitnexus-debugging/SKILL.md` |
| Rename / extract / split / refactor | `.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| Tools, resources, schema reference | `.claude/skills/gitnexus/gitnexus-guide/SKILL.md` |
| Index, status, clean, wiki CLI commands | `.claude/skills/gitnexus/gitnexus-cli/SKILL.md` |

<!-- gitnexus:end -->
