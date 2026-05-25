# AGENTS.md — Project Context for AI Agents
# Version: 1.0 | Updated: 2026-05-24 | Project: Warehouse Management System (WMS)

## 1. PROJECT OVERVIEW
Name: Hệ Thống Quản Lý Kho (Warehouse Management System)
Type: Enterprise Web Application
Domain: Logistics/Warehouse Management cho doanh nghiệp thương mại
Stage: Development
Target: 3 warehouses (Hải Phòng, Hà Nội, Hồ Chí Minh), 1000+ products, 50+ dealers

## 2. TECH STACK (STRICT — do not deviate)
Backend: Spring Boot 3.4.5, Java 21.0.10
Frontend: React 18 + TypeScript
Database: PostgreSQL 18
ORM: Spring Data JPA / Hibernate
Auth: JWT + bcrypt (min cost 12)
Testing: JUnit 5 + Mockito (backend), Jest (frontend)
Styling: Tailwind CSS 3.x
API: RESTful JSON API

## 3. ARCHITECTURE PRINCIPLES
- Follow Layered Architecture (Controller → Service → Repository)
- API style: RESTful với proper HTTP status codes
- Error handling: centralized exception handler với typed errors
- No raw SQL — always use JPA/Hibernate ORM
- No console.log/system.out in production code — use SLF4J structured logging
- Max function length: 40 lines (refactor if longer)
- Max file length: 300 lines (split if longer)
- Comments: explain WHY not WHAT. Remove TODO before merge.
- Audit logging for all warehouse operations (who, when, what)

## 4. FILE NAMING & STRUCTURE
Java Classes: PascalCase (e.g., WarehouseService.java, ProductController.java)
Packages: lowercase (com.wms.service, com.wms.repository)
Database Tables: snake_case (e.g., warehouse_staff, product_categories)
API Routes: kebab-case (e.g., /api/warehouse-stock, /api/batch-management)
React Components: PascalCase (e.g., WarehouseDashboard.tsx, ProductList.tsx)
React Hooks/Utils: camelCase (e.g., useInventory.ts, formatCurrency.ts)
DTOs: PascalCase with DTO suffix (e.g., WarehouseDTO.java, ProductDTO.java)

## 5. FORBIDDEN PATTERNS
- NEVER store secrets/passwords/API keys in plain text or committed .env files
- NEVER skip input validation on API endpoints — use Jakarta Validation annotations
- NEVER use deprecated libraries without team approval
- NEVER delete files in /data or /uploads without user confirmation
- NEVER allow negative inventory (ton kho am)
- NEVER skip QC check before warehouse receipt
- NEVER skip audit logging for warehouse operations

## 6. DEFINITION OF DONE (per task)
- [ ] Unit tests written and passing (min 80% coverage for services)
- [ ] Integration tests for all API endpoints (happy + error paths)
- [ ] No linting/type errors (maven compile, eslint)
- [ ] API endpoint documented in OpenAPI/Swagger
- [ ] Error cases handled with proper HTTP status codes
- [ ] Audit log entry created for warehouse operations
- [ ] No TODO comments left in code
- [ ] FEFO/FIFO logic tested for batch management

## 6.1 NON-FUNCTIONAL REQUIREMENTS
- Performance: API response time < 3s for 50 concurrent users on core operations.
- Reliability: 99.5% availability for inventory queries and transaction endpoints.
- Security: JWT auth + role-based access, input validation, no secrets in source.
- Maintainability: code follows project conventions, max 40-line functions, max 300-line files.
- Scalability: design supports 3 warehouses, 1000+ products, 1000+ transactions/month.
- Usability: manual data entry workflow with quick SKU/code lookup and validation.
- Auditability: all warehouse operations must generate audit logs with who/when/what.

## 7. GIT CONVENTIONS
Branch: feat/[feature-name] | fix/[bug-name] | spec/[feature-name] | chore/
Commit: [type]: [scope] - [description]
Example: feat(inventory): add FEFO batch selection logic
PR rules: Min 1 approval before merge
PR size: Max 400 lines changed (split larger PRs)

## 8. CORE ENTITIES (Domain Model)
- Warehouse (Kho): id, code, name, address, phone, manager, zones
- Product (Sản phẩm): id, sku, name, unit, barcode, costPrice, retailPrice, dealerPrice
- Batch (Lô hàng): id, batchNumber, product, warehouse, receivedDate, expDate, grade, quantity
- Inventory (Tồn kho): warehouse, product, batch, location, quantity, capacity
- Receipt (Phiếu nhập kho): id, receiptNumber, date, type, warehouse, supplier, status, items
- Issue (Phiếu xuất kho): id, issueNumber, date, type, warehouse, customer, status, items
- Transfer (Phiếu điều chuyển): id, transferNumber, sourceWh, destWh, status, items
- SaleOrder (Đơn hàng Sale): id, orderNumber, customer, items, status, desiredDeliveryDate
- Delivery (Vận đơn): id, deliveryNumber, issue, vehicle, driver, status, pod (proof of delivery)
- WarehouseStaff: id, name, warehouse, role, shifts
- Dealer (Đại lý): id, code, name, contactPerson, phone, address, creditLimit, currentDebt, status

## DATABASE
Database schema and table design have been moved to `database.md`.
See `database.md` for full schema, indexes, and structure details.

## 5. Quy Tắc Nghiệp Vụ

### 5.1 Tồn kho
1. CHECK: inventory.quantity >= 0
2. quantity >= reserved_quantity luôn đúng
3. FEFO: chọn batch expiry gần nhất còn hạn
4. FIFO: chọn batch received_date cũ nhất
5. Optimistic locking: kiểm tra version trước UPDATE
6. Điều chỉnh: chỉ qua adjustments, không sửa trực tiếp inventory

### 5.2 Batch & Grade
1. 1 Grade/Lô: mỗi batch 1 grade, khác grade phải tạo batch mới
2. Putaway: kiểm tra bin_capacity trước khi đặt
3. Serial: sản phẩm has_serial=true phải nhập serial khi nhập/xuất

### 5.3 Đơn hàng
1. sale_order.status=confirmed → tạo inventory_reservation
2. Hết hạn giữ → auto release
3. Partial allocation: cho phép giao 1 phần

### 5.4 Điều chuyển
1. status=approved → giảm tồn nguồn
2. status=received → tăng tồn đích
3. quantity_sent != quantity_received → tạo adjustment

### 5.5 Duyệt duyệt
1. Dưới ngưỡng auto_approve_threshold → tự duyệt
2. Mỗi level cần duyệt bởi role tương ứng
3. Reject: ghi rejected_reason, quay lại sửa

### 5.6 Soft delete
- Master data: is_active = false
- Transaction data: status = cancelled
- Không xóa vĩnh viễn

---

## 6. Chức Năng Chính

### 6.1 Quản lý kho
- CRUD kho vật lý (3 kho)
- Quản lý vị trí kệ (zone/rack/shelf/bin)
- Theo dõi sức chứa kho

### 6.2 Quản lý hàng hóa
- CRUD sản phẩm (SKU, barcode, hình ảnh)
- Phân loại sản phẩm (danh mục cây)
- Quản lý đơn vị tính + quy đổi
- Quản lý batch (FEFO/FIFO)
- Tracking serial number (optional)
- Lịch sử giá

### 6.3 Nhập kho
- Tạo và duyệt PO
- Nhận hàng NCC
- QC (pass/fail/quarantine)
- Hoàn hàng từ đại lý
- Putaway: chọn vị trí kệ

### 6.4 Xuất kho
- Tạo đơn từ Sale
- Duyệt đơn hàng
- Xác nhận tồn kho (allocation)
- Tạo phiếu xuất kho
- Picking: chọn batch FEFO

### 6.5 Điều chuyển
- Tạo phiếu điều chuyển giữa 3 kho
- Duyệt điều chuyển
- Theo dõi in_transit
- Xác nhận nhận hàng + ghi chú chênh lệch

### 6.6 Giao hàng
- Tạo vận đơn từ phiếu xuất
- Gán tài xế + xe
- Theo dõi GPS
- Nhiều lần giao thử
- Chụp ảnh + chữ ký POD

### 6.7 Kiểm kê
- Tạo phiếu kiểm kê (định kỳ/tháng/năm)
- Nhập số đếm thực tế
- Auto tính chênh lệch
- Duyệt và tạo điều chỉnh

### 6.8 Báo cáo
- Tồn kho theo kho/sản phẩm
- Hàng sắp hết hạn
- Hàng dưới điểm tái đặt
- Đơn hàng theo trạng thái
- Hiệu suất giao hàng

### 6.9 Cảnh báo
- Reorder: tồn dưới reorder_point
- Expiry: batch sắp hết hạn (30 ngày)
- QC fail
- Delivery fail

---

## 7. Phân Quyền Theo Role

| Role | Quyền |
|------|-------|
| admin | Full access, quản lý user, cấu hình |
| warehouse_manager | Quản lý kho, duyệt điều chuyển, báo cáo |
| storekeeper | Nhập/xuất kho, kiểm kê, điều chỉnh |
| sale | Tạo đơn hàng, xem báo cáo |
| accountant | Xem báo cáo tài chính, công nợ |

---


## 9. KEY BUSINESS RULES
- Inventory cannot go negative (must check before issue)
- Batch is tied to ONE grade only (Grade A, B, or C)
- FEFO for products with expiry date, FIFO for products without
- Quarantine Zone for QC-failed goods (not counted in available inventory)
- In-Transit Location (virtual warehouse) for transfers in progress
- All warehouse operations trigger events to Accounting via message queue
- Sale Orders auto-create warehouse preparation tasks

## 10. CURRENT SPRINT CONTEXT
Sprint: Sprint 1
Focus: Core Warehouse Operations — Inventory, Receipt, Issue, Transfer
Active specs: specs/001-warehouse-management-system/spec.md
Pending: Integration specs for Accounting/HRM/Sale APIs

## 11. ADDITIONAL CONTEXT
- Mobile App: Driver app for delivery status updates and POD (proof of delivery)
- Dealer quick-create: Allowed in Sale Order form for new dealers
- Scale: 1000+ products, 50+ dealers, 1000+ transactions/month
- Integration: Message queue (Kafka/RabbitMQ) for Accounting events

<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **document** (115 symbols, 111 relationships, 0 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

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
| `gitnexus://repo/document/context` | Codebase overview, check index freshness |
| `gitnexus://repo/document/clusters` | All functional areas |
| `gitnexus://repo/document/processes` | All execution flows |
| `gitnexus://repo/document/process/{name}` | Step-by-step execution trace |

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
