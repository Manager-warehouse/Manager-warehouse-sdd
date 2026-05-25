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
