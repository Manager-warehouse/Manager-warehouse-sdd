# CLAUDE.md — Warehouse Management System (WMS) v1.0
## Hệ Thống Quản Lý Kho cho doanh nghiệp thương mại

---

## TL;DR (Đọc trước — 60 giây)

> **Đây là hệ thống quản lý kho hàng (Warehouse Management System)**
> 
> **Backend**: Spring Boot 3.4.5 + Java 21 + PostgreSQL 18 + JPA/Hibernate
> **Frontend**: React 18 + TypeScript + Tailwind CSS 3.x
> **Auth**: JWT + bcrypt (cost factor 12)
> **Integration**: Message queue cho Accounting events (Kafka/RabbitMQ)
> 
> **3 warehouses**: Hải Phòng, Hà Nội, Hồ Chí Minh
> **Scale**: 1000+ products, 50+ dealers, 1000+ transactions/month

### Đọc trước
1. `AGENTS.md` → Project context đầy đủ (Tech stack, forbidden patterns, domain model)
2. `CONSTITUTION.md` → Development principles và team agreements
3. File này → Workflow, patterns, và conventions

---

## KIẾN TRÚC HỆ THỐNG

### Sơ đồ tổng quan
```
┌─────────────────────────────────────────────────────────────────────────┐
│                         FRONTEND (React 18)                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐ │
│  │Dashboard │  │Inventory │  │Receipt/  │  │Transfer  │  │Delivery  │ │
│  │          │  │          │  │Issue     │  │          │  │          │ │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘ │
└───────┼─────────────┼─────────────┼─────────────┼─────────────┼───────┘
        │             │             │             │             │
        ▼             ▼             ▼             ▼             ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    BACKEND (Spring Boot 3.4.5)                          │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    REST API Layer (@RestController)              │   │
│  │  /api/warehouse-stock  /api/batch-management  /api/receipt     │   │
│  │  /api/issue  /api/transfer  /api/delivery  /api/sale-order      │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                              │                                          │
│                              ▼                                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                 Service Layer (@Service)                        │   │
│  │  WarehouseService  BatchService  InventoryService  ReceiptService│   │
│  │  IssueService  TransferService  DeliveryService  SaleOrderService│   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                              │                                          │
│                              ▼                                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │              Repository Layer (Spring Data JPA)                  │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   PostgreSQL    │  │  Message Queue   │  │   File Storage  │
│   (Primary DB)  │  │ (Kafka/RabbitMQ) │  │   (/uploads)    │
└─────────────────┘  │   → Accounting   │  └─────────────────┘
                     └─────────────────┘
```

### Layer Architecture (Backend)
```
┌─────────────────────────────────────────┐
│          Controller Layer               │  @RestController
│   - Input validation                    │  - Handle HTTP requests
│   - Response formatting                 │  - Return DTOs
│   - HTTP status codes                   │  
└─────────────────┬─────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│           Service Layer                  │  @Service
│   - Business logic                      │  - Transaction management
│   - FEFO/FIFO selection                 │  - Audit logging
│   - Authorization checks                │  
└─────────────────┬─────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│          Repository Layer                │  @Repository
│   - JPA/Hibernate queries               │  - Never raw SQL
│   - Entity mapping                      │  
└─────────────────┬─────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│           Entity Layer                   │  @Entity
│   - Database table mapping              │  - JPA annotations
│   - Relationships                       │  
└─────────────────────────────────────────┘
```

### Repository Structure
```
/Users/haison/Documents/GitHub/Manager-warehouse-sdd
├── .agents/             # Agent and skill definitions
├── .git/                # Git metadata
├── .specify/            # Spec generation workspace
├── AGENTS.md            # Agent policy and rules
├── CLAUDE.md            # Project overview, workflow, and conventions
├── CONSTITUTION.md      # Development principles and agreements
├── README.md            # Project summary and user stories
└── test/                # Test plans and guidance
```

---

## QUYẾT ĐỊNH KIẾN TRÚC QUAN TRỌNG (ADR)

### ADR-001: Spring Boot 3.4.5 + Java 21 thay vì Node.js
**Quyết định**: Chọn Java ecosystem cho backend
**Lý do**: Team có kinh nghiệm Java enterprise, type safety mạnh, ecosystem ổn định
**Trade-off**: Verbose hơn Node.js, compile time dài hơn
**Status**: ✅ Approved

### ADR-002: Spring Data JPA/Hibernate cho ORM
**Quyết định**: Không dùng raw SQL, luôn dùng JPA/Hibernate
**Lý do**: Type safety, relational integrity, migration support qua Flyway
**Trade-off**: Complex queries có thể chậm hơn raw SQL
**Status**: ✅ Approved

### ADR-003: JWT + bcrypt (cost factor 12) cho Auth
**Quyết định**: Stateless authentication với hashed passwords
**Lý do**: Scalable, stateless, industry standard
**Trade-off**: Token management, refresh mechanism needed
**Status**: ✅ Approved

### ADR-004: Tách Kho ảo "In-Transit Location"
**Quyết định**: Virtual warehouse cho hàng đang di chuyển giữa các kho
**Lý do**: Track inventory during transfer, không mất visibility
**Trade-off**: Complex queries khi join với real warehouses
**Status**: ✅ Approved

### ADR-005: Quarantine Zone cho QC-failed goods
**Quyết định**: Tách zone riêng cho hàng không đạt QC
**Lý do**: Không tính vào available inventory, track rejects separately
**Trade-off**: Thêm complexity trong inventory calculations
**Status**: ✅ Approved

### ADR-006: Message Queue cho Accounting Events
**Quyết định**: Gửi events tới Accounting qua Kafka/RabbitMQ
**Lý do**: Async processing, decouple systems, không block warehouse ops
**Trade-off**: Complexity cao hơn sync HTTP calls
**Status**: ✅ Approved (Pending integration specs)

---

## NHỮNG GÌ ĐÃ KHÔNG HOẠT ĐỘNG (Lessons Learned)

### LESSON-001: Never allow negative inventory
**Biến cố**: [TBD] Đã từng để xảy ra tồn kho âm → conflict inventory
**Giải pháp**: Luôn check stock TRƯỚC khi issue, dùng database constraint
**Áp dụng**: Tất cả issue operations phải validate quantity

### LESSON-002: Batch tied to ONE grade only
**Biến cố**: [TBD] Mixed grade trong batch → FEFO selection confusion
**Giải pháp**: Mỗi batch chỉ có một grade (A/B/C), không mix
**Áp dụng**: Receipt validation, batch creation

### LESSON-003: Phân quyền phải check BOTH role AND warehouse
**Biến cố**: [TBD] Chỉ check role → staff access kho không được assign
**Giải pháp**: Authorization = role permission + warehouse assignment
**Áp dụng**: All warehouse operations

### LESSON-004: Manual entry workflow
**Biến cố**: [TBD] Thiết bị quét không hiện tại, nên nhập liệu thủ công là chính
**Giải pháp**: Thiết kế giao diện nhập nhanh, kiểm tra dữ liệu tại chỗ và cho phép tìm kiếm sản phẩm bằng mã/SKU
**Áp dụng**: Receipt, Issue, Transfer screens

---

## FILE STRUCTURE

### Backend (Spring Boot)
```
backend/
├── src/main/java/com/wms/
│   ├── controller/         # REST controllers
│   │   └── WarehouseController.java
│   ├── service/           # Business logic
│   │   └── WarehouseService.java
│   ├── repository/         # JPA repositories
│   │   └── WarehouseRepository.java
│   ├── entity/             # JPA entities
│   │   └── Warehouse.java
│   ├── dto/               # Data transfer objects
│   │   ├── WarehouseDTO.java
│   │   └── CreateWarehouseRequest.java
│   ├── config/            # Configuration classes
│   │   ├── SecurityConfig.java
│   │   └── JpaConfig.java
│   ├── exception/         # Custom exceptions
│   │   ├── GlobalExceptionHandler.java
│   │   └── InsufficientStockException.java
│   ├── event/             # Domain events
│   │   └── WarehouseEvent.java
│   └── util/              # Utilities
│       └── FEFOSelector.java
├── src/main/resources/
│   ├── db/migration/      # Flyway migrations
│   │   └── V1__init_schema.sql
│   └── application.yml
├── src/test/java/         # Unit tests (mirrors main structure)
└── CLAUDE.md              # Backend-specific patterns
```

### Frontend (React + TypeScript)
```
frontend/
├── src/
│   ├── components/        # React components (PascalCase)
│   │   ├── warehouse/
│   │   │   ├── WarehouseList.tsx
│   │   │   └── WarehouseCard.tsx
│   │   └── common/
│   │       └── Button.tsx
│   ├── pages/             # Page components
│   │   └── WarehousePage.tsx
│   ├── hooks/             # Custom hooks (camelCase)
│   │   └── useWarehouse.ts
│   ├── services/          # API calls
│   │   └── warehouseApi.ts
│   ├── stores/            # State management (Zustand/Redux)
│   ├── utils/             # Utility functions
│   │   └── formatCurrency.ts
│   └── types/             # TypeScript types
│       └── warehouse.types.ts
├── public/
└── CLAUDE.md              # Frontend-specific patterns
```

---

## DEVELOPMENT WORKFLOW

### Standard Flow
```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│   /spec  →  /plan  →  /build  →  /test  →  /review  →  /deploy     │
│   Define    Plan     Build     Verify    Review     Deploy         │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### Workflow Commands

| Phase | Command | Purpose |
|-------|---------|---------|
| **Define** | `speckit-specify` | Create/update feature specification |
| **Plan** | `speckit-plan` | Decompose into tasks with acceptance criteria |
| **Build** | `speckit-implement` | Execute tasks incrementally |
| **Verify** | `tdd` skill | TDD cycle (RED-GREEN-REFACTOR) |
| **Review** | `code-review` skill | Five-axis code review |
| **Deploy** | `deploy` skill | Build, test, deploy |

### Supporting Commands

| Command | Use When |
|---------|----------|
| `/debug` | Systematic error diagnosis |
| `/simplify` | Reduce complexity without changing behavior |
| `speckit-analyze` | Check spec consistency |
| `speckit-git-validate` | Validate branch naming |

---

## RULES & GUIDELINES

### ✅ ALWAYS DO

| Rule | Description |
|------|-------------|
| **Input validation** | Use Jakarta Validation annotations |
| **Audit logging** | Log all warehouse operations (who, when, what) |
| **Inventory check** | Always check stock BEFORE issue |
| **Structured logging** | Use SLF4J (no console.log/system.out) |
| **Unit tests** | Min 80% coverage for services |
| **API docs** | Document in OpenAPI/Swagger |
| **Error handling** | Proper HTTP status codes |
| **Comments** | Explain WHY not WHAT |

### ❌ NEVER DO

| Rule | Description |
|------|-------------|
| **No secrets** | Never store passwords/keys in plain text |
| **No raw SQL** | Always use JPA/Hibernate |
| **No negative inventory** | Prevent under all circumstances |
| **No skip QC** | Always check before warehouse receipt |
| **No skip audit** | Log all warehouse operations |
| **No TODO left** | Remove before merge |
| **No deprecated libs** | Without team approval |

### Code Quality

| Metric | Limit |
|--------|-------|
| Max function length | 40 lines |
| Max file length | 300 lines |
| Min test coverage | 80% (services) |

---

## NAMING CONVENTIONS

### Java (Backend)

| Type | Convention | Example |
|------|------------|---------|
| Classes | PascalCase | `WarehouseService.java` |
| Packages | lowercase | `com.wms.service` |
| Tables | snake_case | `warehouse_staff` |
| Constants | UPPER_SNAKE | `MAX_BATCH_QUANTITY` |
| Methods | camelCase | `findByWarehouseId()` |

### React (Frontend)

| Type | Convention | Example |
|------|------------|---------|
| Components | PascalCase | `WarehouseDashboard.tsx` |
| Hooks | camelCase | `useInventory.ts` |
| Utils | camelCase | `formatCurrency.ts` |
| Types | PascalCase | `WarehouseType.ts` |

### API Routes

| Type | Convention | Example |
|------|------------|---------|
| Endpoints | kebab-case | `/api/warehouse-stock` |
| HTTP methods | lowercase | `GET /api/batch-management` |

---

## GIT CONVENTIONS

### Branch Naming
```
feat/[feature-name]      # New features (e.g., feat/inventory-FEFO)
fix/[bug-name]           # Bug fixes (e.g., fix/negative-stock)
spec/[feature-name]      # Specification work
chore/                   # Maintenance tasks
```

### Commit Format
```
[type]: [scope] - [description]

Types: feat, fix, docs, style, refactor, test, chore
Scopes: inventory, receipt, issue, transfer, batch, etc.

Examples:
feat(inventory): add FEFO batch selection logic
fix(batch): correct expiry date calculation
docs(api): update warehouse-stock endpoint docs
```

### Pull Request Rules
- Minimum 1 approval before merge
- Max 400 lines changed
- All CI checks must pass
- No TODO comments left

---

## GITNEXUS INTEGRATION

### Current Setup
- ✅ Repository: `Manager-warehouse-sdd`
- ✅ Indexed: 115 symbols, 111 relationships
- ✅ Repository-scoped query enabled

### Available Commands

```bash
# Query the current repo
gitnexus query "warehouse inventory" --limit 10

# Impact analysis (what breaks if I change X)
gitnexus impact --target WarehouseService --repo Manager-warehouse-sdd

# Sync index after code changes
gitnexus sync --repo Manager-warehouse-sdd

# Check status
gitnexus status --repo Manager-warehouse-sdd
```

### MCP Tools (when GitNexus MCP server is running)

| Tool | Purpose |
|------|---------|
| `gitnexus_query` | Find execution flows for a concept |
| `gitnexus_impact` | Blast radius analysis |
| `gitnexus_context` | Full symbol context (callers, callees) |
| `gitnexus_detect_changes` | Map git changes to affected flows |

### Workflow Integration

```python
# BEFORE editing a symbol:
1. Run gitnexus_impact({target: "symbolName", direction: "upstream"})
2. Report blast radius to user
3. If HIGH/CRITICAL risk → get confirmation before proceeding

# BEFORE committing:
1. Run gitnexus_detect_changes({scope: "staged"})
2. Verify only expected symbols affected
3. If unexpected → investigate before committing
```

---

## QUICK REFERENCE

### Core Entities

```
Warehouse (3 locations: Hải Phòng, Hà Nội, Hồ Chí Minh)
├── Zones (receiving, storage, picking, shipping, quarantine)
└── Locations (aisle-rack-shelf)

Product (1000+ items)
├── SKU, barcode, name, unit
└── Prices (cost, retail, dealer)

Batch (Lô hàng - tied to ONE grade)
├── batchNumber, receivedDate, expDate
├── grade (A/B/C)
└── quantity

Inventory (tồn kho)
├── warehouse + product + batch + location
└── quantity (NEVER negative)

Receipt (Phiếu nhập kho)
├── receiptNumber, type (purchase/return)
├── warehouse, supplier
└── items (product + quantity)

Issue (Phiếu xuất kho)
├── issueNumber, type (sale/delivery/adjustment)
├── warehouse, customer/dealer
└── items

Transfer (Điều chuyển)
├── source → destination warehouses
├── status (pending/in_transit/completed)
└── In-Transit virtual location

SaleOrder → Issue → Delivery
Dealer (50+ accounts)
WarehouseStaff
```

### Key Business Rules

| Rule | Implementation |
|------|----------------|
| No negative inventory | `@Column(check = "quantity >= 0")` + validation |
| Single grade per batch | `grade` is immutable after creation |
| FEFO for expiring products | `FEFOSelector` picks batch by expDate ASC |
| FIFO for non-expiring | `FIFOSelector` picks by receivedDate ASC |
| Quarantine excluded | WHERE clause filters `zone != 'QUARANTINE'` |
| In-Transit tracking | Virtual warehouse `IN_TRANSIT` for transfers |

### Current Sprint

- **Sprint 1**: Core Warehouse Operations
- **Focus**: Inventory, Receipt, Issue, Transfer
- **Active specs**: `specs/001-warehouse-management-system/spec.md`
- **Pending**: Accounting/HRM/Sale API integration specs

---

## MCP ARCHITECTURE OVERVIEW

### Data Flow
```
User Input → AI Model → MCP Client → MCP Server → External Service
                    ↑                     ↓
                    └───── JSON-RPC ─────┘
```

### Security Principles

| Principle | Application |
|-----------|-------------|
| **Least Privilege** | Only request necessary scopes |
| **Token Rotation** | Rotate MCP tokens regularly |
| **Audit Logging** | Log all MCP tool usage |
| **Validation** | Validate all inputs before use |

---

<!-- Claude Code auto-appends entries below when working -->
<!-- Last updated: 2026-05-25 -->

---

## SWIMLANE DIAGRAMS

### Business Process Swimlanes

#### Receipt Process (Phiếu nhập kho)
```
┌─────────────────────────────────────────────────────────────────────────────┐
│ RECEIPT PROCESS SWIMLANES                                                   │
├───────────────┬───────────────┬───────────────┬───────────────┬────────────┤
│    STAFF      │    SYSTEM     │    QC TEAM    │   ACCOUNTING   │  SUPPLIER  │
├───────────────┼───────────────┼───────────────┼───────────────┼────────────┤
│               │               │               │               │            │
│ Create receipt│               │               │               │            │
│ ─────────────►│               │               │               │            │
│               │               │               │               │            │
│ Enter SKU/code│               │               │               │            │
│ ─────────────►│               │               │               │            │
│               │               │               │               │            │
│ Submit for QC │               │               │               │            │
│ ─────────────►│───────────────│───────────────│               │            │
│               │               │               │               │            │
│               │               │ QC Check      │               │            │
│               │               │ ─────────────►│               │            │
│               │               │               │               │            │
│ Pass: Stock   │ Update inventory               │               │            │
│ Fail: Quarantine              │               │               │            │
│               │               │               │               │            │
│               │               │               │ Send invoice  │            │
│               │               │               │ ─────────────►│            │
│               │               │               │               │            │
└───────────────┴───────────────┴───────────────┴───────────────┴────────────┘
```

#### Issue Process (Phiếu xuất kho)
```
┌─────────────────────────────────────────────────────────────────────────────┐
│ ISSUE PROCESS SWIMLANES                                                     │
├───────────────┬───────────────┬───────────────┬───────────────┬────────────┤
│    STAFF      │    SYSTEM     │   PICKING     │   DELIVERY     │  CUSTOMER  │
├───────────────┼───────────────┼───────────────┼───────────────┼────────────┤
│               │               │               │               │            │
│ Create issue  │               │               │               │            │
│ ─────────────►│               │               │               │            │
│               │               │               │               │            │
│               │ FEFO Selection│               │               │            │
│               │ ─────────────►│               │               │            │
│               │               │               │               │            │
│               │               │ Pick items    │               │            │
│               │               │ ─────────────►│               │            │
│               │               │               │               │            │
│               │               │ Confirm pick  │               │            │
│               │◄──────────────│               │               │            │
│               │               │               │               │            │
│               │ Update stock  │               │               │            │
│               │ ◄─────────────│               │               │            │
│               │               │               │               │            │
│               │               │               │ Dispatch      │            │
│               │               │               │ ─────────────►│            │
│               │               │               │               │            │
└───────────────┴───────────────┴───────────────┴───────────────┴────────────┘
```

#### Transfer Process (Điều chuyển kho)
```
┌─────────────────────────────────────────────────────────────────────────────┐
│ TRANSFER PROCESS SWIMLANES                                                  │
├───────────────┬───────────────┬───────────────┬───────────────┬────────────┤
│   SOURCE WH    │    SYSTEM     │    TRANSIT    │  DEST WH      │   DRIVER  │
├───────────────┼───────────────┼───────────────┼───────────────┼────────────┤
│               │               │               │               │            │
│ Create transfer              │               │               │            │
│ ─────────────►│               │               │               │            │
│               │               │               │               │            │
│ Pick & pack   │               │               │               │            │
│ ─────────────►│               │               │               │            │
│               │               │               │               │            │
│               │ Move to IN_TRANSIT             │               │            │
│               │ ◄─────────────│               │               │            │
│               │               │               │               │            │
│               │               │ In Transit     │               │            │
│               │               │ ◄──────────────│               │            │
│               │               │               │               │            │
│               │               │               │ Confirm receipt              │
│               │◄──────────────│               │               │            │
│               │               │               │               │            │
│               │ Update dest stock             │               │            │
│               │               │               │               │            │
└───────────────┴───────────────┴───────────────┴───────────────┴────────────┘
```

#### Sale Order Process (Đơn hàng Sale)
```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│ SALE ORDER PROCESS SWIMLANES                                                        │
├──────────────┬──────────────┬──────────────┬──────────────┬──────────────┬──────────┤
│   DEALER     │    SALE      │    SYSTEM    │  ACCOUNTING  │  WAREHOUSE   │  DELIVERY│
├──────────────┼──────────────┼──────────────┼──────────────┼──────────────┼──────────┤
│              │              │              │              │              │          │
│ Contact Sale│              │              │              │              │          │
│ ────────────►│              │              │              │              │          │
│              │              │              │              │              │          │
│              │ Create order │              │              │              │          │
│              │ ─────────────►│              │              │              │          │
│              │              │              │              │              │          │
│              │              │ Status: CHỜ_KETOAN_DUYET │              │          │
│              │◄─────────────│              │              │              │          │
│              │              │              │              │              │          │
│              │              │              │ Review order│              │          │
│              │              │              │ ────────────►│              │          │
│              │              │              │              │              │          │
│              │              │              │ Upload contract photo      │          │
│              │              │              │ ◄────────────│              │          │
│              │              │              │              │              │          │
│              │              │ Approve: Status=CHỜ_KHO_DUYET          │          │
│              │              │ ◄────────────│              │              │          │
│              │              │              │              │              │          │
│              │              │              │              │ Review order│          │
│              │              │              │              │ ────────────►│          │
│              │              │              │              │              │          │
│              │              │              │              │ Approve: DANG_CHUAN_BI     │
│              │              │              │              │ ◄────────────│          │
│              │              │              │              │              │          │
│              │              │              │              │ Prepare goods             │
│              │              │              │              │ ────────────►│          │
│              │              │              │              │              │          │
│              │              │              │              │ Confirm shipped           │
│              │              │              │◄─────────────│              │          │
│              │              │              │              │              │          │
│              │              │ Sync to Accounting (công nợ)│              │          │
│              │              │ ────────────►│              │              │          │
│              │              │              │              │              │          │
│              │ Notify dealer│              │              │              │          │
│              │ ◄────────────│              │              │              │          │
│              │              │              │              │              │          │
└──────────────┴──────────────┴──────────────┴──────────────┴──────────────┴──────────┘

Status Flow:
CHỜ_KETOAN_DUYET → CHỜ_KHO_DUYET → DA_DUYET → DANG_CHUAN_BI → DA_XUAT_KHO → HOAN_THANH/DA_HUY
```

#### Delivery Process (Vận chuyển & Giao hàng)
```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│ DELIVERY PROCESS SWIMLANES                                                         │
├──────────────┬──────────────┬──────────────┬──────────────┬──────────────┬──────────┤
│   WAREHOUSE  │    SYSTEM    │   FLEET MGMT │    DRIVER    │   CUSTOMER   │ ACCTNG   │
├──────────────┼──────────────┼──────────────┼──────────────┼──────────────┼──────────┤
│              │              │              │              │              │          │
│ Create delivery              │              │              │              │          │
│ ────────────►│              │              │              │              │          │
│              │              │              │              │              │          │
│              │ Assign vehicle & driver      │              │              │          │
│              │ ────────────►│              │              │              │          │
│              │              │              │              │              │          │
│              │              │ Assign driver│              │              │          │
│              │◄─────────────│              │              │              │          │
│              │              │              │              │              │          │
│              │ Status: CHỜ_GIAO            │              │              │          │
│              │              │              │              │              │          │
│              │              │              │ Receive delivery          │          │
│              │              │              │ ◄─────────────│          │          │
│              │              │              │              │              │          │
│              │              │              │ Start delivery│          │          │
│              │              │              │ ─────────────►│          │          │
│              │              │              │              │              │          │
│              │ Update: ĐANG_GIAO            │              │              │          │
│              │◄─────────────│              │              │              │          │
│              │              │              │              │              │          │
│              │              │              │              │ GPS tracking │          │
│              │              │              │              │ ◄─────────────│          │
│              │              │              │              │              │          │
│              │              │              │              │ Attempt delivery        │
│              │              │              │              │ ────────────►│          │
│              │              │              │              │              │          │
│         ┌────┴────┐         │              │              │              │          │
│         │ SUCCESS │         │              │              │              │          │
│         └────┬────┘         │              │              │              │          │
│              │              │              │              │              │          │
│              │              │              │ Collect POD (signature + photo)│     │
│              │              │              │ ◄─────────────│              │          │
│              │              │              │              │              │          │
│              │              │              │ Confirm delivery complete     │
│              │◄─────────────│              │              │              │          │
│              │              │              │              │              │          │
│              │ Status: HOAN_THANH│         │              │              │          │
│              │◄─────────────│              │              │              │          │
│              │              │              │              │              │          │
│              │              │              │              │ Send notification        │
│              │◄─────────────│              │              │              │          │
│              │              │              │              │              │          │
│              │ Sync: Đơn hoàn thành         │              │              │          │
│              │              │              │              │ ────────────►│          │
│              │              │              │              │              │          │
│         ┌────┴────┐         │              │              │              │          │
│         │  FAIL   │         │              │              │              │          │
│         └────┬────┘         │              │              │              │          │
│              │              │              │              │              │          │
│              │              │              │ Record failure reason        │
│              │              │              │ ◄─────────────│              │          │
│              │              │              │              │              │          │
│              │ Status: GIAO_THAT_BAI       │              │              │          │
│              │              │              │              │              │          │
│              │ Return goods to warehouse   │              │              │          │
│              │ ◄─────────────│              │              │              │          │
│              │              │              │              │              │          │
│              │              │              │              │ Notify Sale │          │
│              │◄─────────────│              │              │              │          │
│              │              │              │              │              │          │
└──────────────┴──────────────┴──────────────┴──────────────┴──────────────┴──────────┘

Status Flow:
CHỜ_GIAO → ĐANG_GIAO → HOAN_THANH / GIAO_THAT_BAI
```

#### StockTake Process (Kiểm kê)
```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│ STOCKTAKE PROCESS SWIMLANES                                                        │
├──────────────┬──────────────┬──────────────┬──────────────┬──────────────┬──────────┤
│  WH MANAGER  │    SYSTEM    │   STAFF      │   ACCOUNTING │    QC TEAM   │  AUDIT  │
├──────────────┼──────────────┼──────────────┼──────────────┼──────────────┼──────────┤
│              │              │              │              │              │          │
│ Create stocktake          │              │              │              │          │
│ ────────────►│              │              │              │              │          │
│              │              │              │              │              │          │
│              │ Generate product list      │              │              │          │
│              │ ◄─────────────│              │              │              │          │
│              │              │              │              │              │          │
│              │              │ Count physical              │              │          │
│              │◄─────────────│              │              │              │          │
│              │              │              │              │              │          │
│              │ Compare: System vs Physical │              │              │          │
│              │ ◄─────────────│              │              │              │          │
│              │              │              │              │              │          │
│              │ Generate variance report    │              │              │          │
│              │ ────────────►│              │              │              │          │
│              │              │              │              │              │          │
│              │              │ Review & confirm variance   │              │          │
│              │◄─────────────│              │              │              │          │
│              │              │              │              │              │          │
│         ┌────┴────┐         │              │              │              │          │
│         │  NORMAL │         │              │              │              │          │
│         │ VARIANCE│         │              │              │              │          │
│         └────┬────┘         │              │              │              │          │
│              │              │              │              │              │          │
│              │              │              │ Review variance report     │
│              │              │              │ ◄─────────────│              │          │
│              │              │              │              │              │          │
│ Approve adjustment       │              │              │              │          │
│ ────────────►│              │              │              │              │          │
│              │              │              │              │              │          │
│              │ Update inventory           │              │              │          │
│              │ ────────────►│              │              │              │          │
│              │              │              │              │              │          │
│              │              │              │              │              │ Log adjustment
│              │              │              │              │ ─────────────►│
│              │              │              │              │              │          │
│              │              │              │ Sync to Accounting         │
│              │              │              │ ◄─────────────│              │
│              │              │              │              │              │          │
│              │              │              │              │              │          │
│         ┌────┴────┐         │              │              │              │          │
│         │ DAMAGE/ │         │              │              │              │          │
│         │ LOSS    │         │              │              │              │          │
│         └────┬────┘         │              │              │              │          │
│              │              │              │              │              │          │
│              │              │              │              │ Create damage report      │
│              │              │              │              │ ◄─────────────│          │
│              │              │              │              │              │          │
│              │              │ Move to QUARANTINE          │              │          │
│              │◄─────────────│              │              │              │          │
│              │              │              │              │              │          │
│              │              │              │              │ Notify WH Manager        │
│              │◄─────────────│              │              │              │          │
│              │              │              │              │              │          │
└──────────────┴──────────────┴──────────────┴──────────────┴──────────────┴──────────┘

Status Flow:
PENDING → IN_PROGRESS → PENDING_APPROVAL → APPROVED → COMPLETED
```

#### Return Process (Hoàn hàng từ Đại lý)
```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│ RETURN PROCESS SWIMLANES                                                            │
├──────────────┬──────────────┬──────────────┬──────────────┬──────────────┬──────────┤
│    DEALER    │    SALE      │    SYSTEM    │   ACCOUNTING │      QC      │  STORE  │
├──────────────┼──────────────┼──────────────┼──────────────┼──────────────┼──────────┤
│              │              │              │              │              │          │
│ Request return              │              │              │              │          │
│ ────────────►│              │              │              │              │          │
│              │              │              │              │              │          │
│              │ Confirm return request     │              │              │          │
│              │ ────────────►│              │              │              │          │
│              │              │              │              │              │          │
│              │              │ Create Return Receipt     │              │          │
│              │              │ ─────────────►│              │              │          │
│              │              │              │              │              │          │
│              │              │              │              │ Receive goods            │
│              │              │              │              │ ◄─────────────│          │
│              │              │              │              │              │          │
│              │              │              │              │ QC inspection            │
│              │              │              │              │ ─────────────►│          │
│              │              │              │              │              │          │
│              │              │         ┌────┴────┐         │              │          │
│              │              │         │  PASS   │         │              │          │
│              │              │         └────┬────┘         │              │          │
│              │              │              │              │              │          │
│              │              │              │              │ Update stock (normal)    │
│              │              │              │              │ ◄─────────────│          │
│              │              │              │              │              │          │
│              │              │ Send credit note request   │              │          │
│              │              │ ─────────────►│              │              │          │
│              │              │              │              │              │          │
│              │              │              │ Create credit note│          │          │
│              │              │              │ ◄─────────────│              │          │
│              │              │              │              │              │          │
│              │ Notify dealer (credit processed)         │              │          │
│              │ ◄─────────────│              │              │              │          │
│              │              │              │              │              │          │
│              │              │         ┌────┴────┐         │              │          │
│              │              │         │  FAIL   │         │              │          │
│              │              │         └────┬────┘         │              │          │
│              │              │              │              │              │          │
│              │              │              │              │ Move to QUARANTINE        │
│              │              │              │              │ ◄─────────────│          │
│              │              │              │              │              │          │
│              │              │              │              │ Create damage report     │
│              │              │              │              │ ─────────────►│          │
│              │              │              │              │              │          │
│              │              │              │              │ Notify WH Manager        │
│              │              │◄─────────────│              │              │          │
│              │              │              │              │              │          │
│              │              │              │              │              │ Mark for disposal
│              │              │              │              │ ◄─────────────│          │
│              │              │              │              │              │          │
└──────────────┴──────────────┴──────────────┴──────────────┴──────────────┴──────────┘

Status Flow:
PENDING → RECEIVED → QC_PASS / QC_FAIL → PROCESSED
```

#### Inventory Adjustment Process (Điều chỉnh tồn kho)
```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│ INVENTORY ADJUSTMENT PROCESS SWIMLANES                                              │
├──────────────┬──────────────┬──────────────┬──────────────┬──────────────────────────┤
│   STOREKEEPER│    SYSTEM    │  WH MANAGER │   ACCOUNTING │        AUDIT            │
├──────────────┼──────────────┼──────────────┼──────────────┼──────────────────────────┤
│              │              │              │              │                         │
│ Detect discrepancy          │              │              │                         │
│ ────────────►│              │              │              │                         │
│              │              │              │              │                         │
│              │ Create adjustment request │              │                         │
│              │ ────────────►│              │              │                         │
│              │              │              │              │                         │
│              │              │ Review request│              │                         │
│              │              │ ◄─────────────│              │                         │
│              │              │              │              │                         │
│         ┌────┴────┐         │              │              │                         │
│         │ MINOR   │         │              │              │                         │
│         │ (< 5%) │         │              │              │                         │
│         └────┬────┘         │              │              │                         │
│              │              │              │              │                         │
│              │              │ Approve & update stock     │
│              │              │ ◄─────────────│              │                         │
│              │              │              │              │                         │
│              │ Update inventory           │              │                         │
│              │ ◄─────────────│              │              │                         │
│              │              │              │              │                         │
│              │              │              │              │ Log adjustment           │
│              │              │              │              │ ◄─────────────│          │
│              │              │              │              │                         │
│              │              │              │ Sync to Accounting│                     │
│              │              │              │ ◄─────────────│          │          │
│              │              │              │              │                         │
│              │              │              │              │                         │
│         ┌────┴────┐         │              │              │                         │
│         │ MAJOR   │         │              │              │                         │
│         │ (> 5%) │         │              │              │                         │
│         └────┬────┘         │              │              │                         │
│              │              │              │              │                         │
│              │              │ Require investigation       │
│              │              │ ─────────────►│              │                         │
│              │              │              │              │                         │
│              │              │ Submit investigation report│              │                         │
│              │◄─────────────│              │              │                         │
│              │              │              │              │                         │
│              │              │ Final approval│              │                         │
│              │              │ ◄─────────────│              │                         │
│              │              │              │              │                         │
│              │ Update inventory           │              │                         │
│              │ ◄─────────────│              │              │                         │
│              │              │              │              │                         │
│              │              │              │              │ Log with investigation  │
│              │              │              │              │ ◄─────────────│          │
│              │              │              │              │                         │
└──────────────┴──────────────┴──────────────┴──────────────┴──────────────────────────┘

Status Flow:
PENDING → REVIEWING → APPROVED → COMPLETED
```

---

## ANTI-PATTERNS (Tránh xa)

### Code Anti-Patterns

| Anti-Pattern | Description | How to Avoid |
|--------------|-------------|--------------|
| **God Class/Service** | Một class/service quá lớn, làm mọi thứ | Max 300 lines/file, 40 lines/function |
| **Anemic Domain Model** | Entities chỉ có getters/setters, không có behavior | Move logic vào domain methods |
| **Circular Dependencies** | A → B → C → A | Use interfaces, dependency injection |
| **Premature Optimization** | Tối ưu sớm, phức tạp hóa code không cần thiết | YAGNI - You Ain't Gonna Need It |
| **Magic Numbers** | Hard-coded numbers không có constant | Use named constants (e.g., `MAX_RETRY = 3`) |
| **Dead Code** | Code không bao giờ được gọi | Remove unused code, keep clean |
| **Shotgun Surgery** | Một thay đổi cần sửa nhiều file không liên quan | Keep related code together |
| **Commented-out Code** | Code cũ để comment thay vì xóa | Use git history instead, delete dead code |
| **Long Parameter List** | Method có >4 parameters | Use DTO or parameter object |
| **Feature Envy** | Method dùng nhiều data của class khác | Move method to that class |
| **Speculative Generality** | Code cho "sau này có thể cần" | Build when needed, not before |

### Database Anti-Patterns

| Anti-Pattern | Description | How to Avoid |
|--------------|-------------|--------------|
| **N+1 Query Problem** | 1 query, rồi N queries cho mỗi item | Use JOIN FETCH, @EntityGraph |
| **Missing Indexes** | Chậm khi query lớn | Analyze queries, add indexes |
| **Denormalization Abuse** | Quá nhiều denormalized tables | Balance read vs write performance |
| **Soft Deletes Everywhere** | Xóa mềm mọi thứ | Only soft delete audit-critical data |
| **EAV (Entity-Attribute-Value)** | Dynamic attributes = performance nightmare | Use proper columns, JSON for true dynamic |
| **One Big Table** | Tất cả columns vào 1 table | Split into related entities |
| **Missing Foreign Keys** | Không có FK constraint | Always define FK relationships |
| **Nullable Everything** | Tất cả columns nullable | Use proper constraints, NOT NULL where needed |
| **Storing JSON in TEXT** | Unstructured data làm khó query | Use proper schema, JSONB only for true flexibility |
| **Using ENUM as VARCHAR** | Store enum values as string không check | Use DB enum type or FK to lookup table |

### Spring Boot Anti-Patterns

| Anti-Pattern | Description | How to Avoid |
|--------------|-------------|--------------|
| **Anemic Services** | Service chỉ gọi repository, không có logic | Add business logic to service layer |
| **God Controllers** | Controller xử lý quá nhiều, validate, transform | Use DTOs, validation annotations |
| **Transaction Failures** | @Transactional không used đúng | Understand propagation, isolation levels |
| **N+1 in JPA** | Lazy loading gây N+1 | Use @Fetch(FetchMode.JOIN) or JOIN FETCH |
| **Oversized @Entity** | Entity có 50+ columns | Split into smaller entities/Value Objects |
| **Missing @Version** | Không có optimistic locking | Add @Version for concurrent updates |
| **Not Using DTO** | Return entity trực tiếp từ controller | Always map to DTO, hide internal structure |
| **Catching Exception Silently** | try-catch không xử lý gì | Either handle properly or let it propagate |
| **Missing @Transactional Rollback** | Không rollback khi exception xảy ra | Use proper rollbackFor, no checked exceptions |
| **Using @Autowired Field** | Field injection không test được | Use constructor injection with @RequiredArgsConstructor |
| **Not Using Validation** | Skip input validation at API | Use @Valid + Jakarta Validation annotations |

### React Anti-Patterns

| Anti-Pattern | Description | How to Avoid |
|--------------|-------------|--------------|
| **Prop Drilling** | Pass props qua nhiều levels không cần | Use Context or Zustand store |
| **Memory Leaks** | Unmounted component still listens | Cleanup in useEffect return |
| **Inline Functions in JSX** | `<button onClick={() => handleClick()}>` | Define outside render or use useCallback |
| **Overfetching** | Fetch entire data when only need summary | Use GraphQL fields or API filters |
| **God Components** | One component 1000+ lines | Split into smaller, focused components |
| **Mutating State Directly** | state.push() thay vì setState | Always use functional setState, immer for complex |
| **Not Handling Loading State** | UI freeze khi fetch data | Always show loading skeleton/spinner |
| **Not Handling Error State** | Crash khi API fail | Always wrap in try-catch, show error message |
| **Using any Type** | :any everywhere mất type safety | Define proper interfaces, use unknown for external |
| **Not Memoizing Expensive Computations** | Recalculate heavy data every render | useMemo for expensive computations |
| **Not Using Key in List** | Missing key prop gây re-render issues | Always provide unique key in .map() |

### WMS-Specific Anti-Patterns (QUAN TRỌNG)

| Anti-Pattern | Description | How to Avoid |
|--------------|-------------|--------------|
| **Allow Negative Inventory** | Tồn kho âm được phép | DB constraint + validation trước khi UPDATE |
| **Mixed Grade in Batch** | Một batch có nhiều grade A, B, C | Mỗi batch chỉ 1 grade duy nhất |
| **Skip QC Check** | Nhập kho không qua QC | Bắt buộc QC trước khi nhập kho thường |
| **Skip Audit Logging** | Không ghi log warehouse operations | Luôn log: ai, khi nào, làm gì |
| **FEFO Not Applied** | Xuất kho không theo FEFO | Always select batch by expiry date ASC |
| **Skip Capacity Check** | Putaway không kiểm tra bin capacity | Check zone/rack/shelf capacity trước putaway |
| **Skip Reservation Check** | Xuất kho không check reserved quantity | Ensure (available = total - reserved) >= 0 |
| **Skip Version Check** | Update inventory không check version | Use optimistic locking with @Version |
| **Hardcode Warehouse IDs** | Magic number warehouse ID | Use configuration, enum, or DB lookup |
| **Skip Status Validation** | Transition trạng thái không validate | Validate status flow: PENDING → APPROVED → etc |
| **Not Handling Partial Transfer** | Điều chuyển quantity_sent ≠ quantity_received | Always create adjustment khi chênh lệch |
| **Skip Approval Workflow** | Bypass duyệt đơn hàng | Strictly follow: Sale → Kế toán → Kho |
| **Not Validating Delivery POD** | Giao hàng không có bằng chứng | Bắt buộc chữ ký/ảnh trước khi complete delivery |
| **Skip Credit Limit Check** | Tạo đơn không kiểm tra công nợ | Check dealer.currentDebt < creditLimit |

### Security Anti-Patterns

| Anti-Pattern | Description | How to Avoid |
|--------------|-------------|--------------|
| **Plain Text Secrets** | Password/API key trong code | Use environment variables, secrets manager |
| **Hardcoded Credentials** | Username/password trong config | Use Vault or environment-specific configs |
| **No Input Validation** | User input không được validate | Validate all inputs at API boundary |
| **SQL Injection Risk** | Raw SQL với string concatenation | Always use parameterized queries via JPA |
| **Missing Authorization** | Chỉ check authentication, không check authorization | Check BOTH user AND warehouse assignment |
| **Logging Sensitive Data** | Log password, token, card info | Never log sensitive data, mask in logs |
| **Not Using HTTPS** | Production API không dùng TLS | Enforce HTTPS in all environments |
| **Missing Rate Limiting** | API không giới hạn request | Implement rate limiting, especially auth endpoints |
| **Storing Passwords in Plain** | Không hash passwords | Always use bcrypt with cost factor >= 12 |

### API Design Anti-Patterns

| Anti-Pattern | Description | How to Avoid |
|--------------|-------------|--------------|
| **Not Using HTTP Methods Correctly** | GET cho mutation, POST cho read | Use GET=read, POST=create, PUT=replace, PATCH=update, DELETE=remove |
| **Returning Different Status Codes** | Same error → different codes | Standardize: 400=bad request, 404=not found, 500=server error |
| **Naked Primitives** | Return raw String/Integer | Always wrap in JSON object with proper structure |
| **Not Versioning API** | Breaking changes without version | Use /api/v1/, /api/v2/ for major changes |
| **Chatty APIs** | Nhiều round-trips cho 1 operation | Return related data in single response |
| **Not Documenting Errors** | Consumer không biết error codes | Document in OpenAPI/Swagger, provide error response schema |
| **Missing Pagination** | Return all records without limit | Always paginate large datasets |
| **Not Using HATEOAS** | No links to related resources | Include _links for discoverability |
| **Inconsistent Naming** | /getUsers vs /user/list | Follow kebab-case, consistent naming |

### Integration Anti-Patterns

| Anti-Pattern | Description | How to Avoid |
|--------------|-------------|--------------|
| **Chatty Integration** | Nhiều API calls nhỏ thay vì batch | Batch operations, composite APIs |
| **Synchronous Everything** | Blocking calls for async operations | Use events, queues for long-running ops |
| **Ignoring Failures** | Catch exception, do nothing | Always handle exceptions properly |
| **Hard-coded Timeouts** | Magic numbers for timeouts | Use configuration with sensible defaults |
| **Not Handling Retry** | Failed call không retry | Implement exponential backoff retry |
| **Fire and Forget** | Gửi message không confirm | Wait for acknowledgment, handle failures |
| **Not Using Circuit Breaker** | Cascade failures khi downstream down | Use circuit breaker pattern |
| **Tight Coupling** | Direct dependency với external service | Use message queue, adapter pattern |
| **Not Handling Idempotency** | Retry gửi duplicate message | Use idempotency key for critical operations |

### Performance Anti-Patterns

| Anti-Pattern | Description | How to Avoid |
|--------------|-------------|--------------|
| **Query in Loop** | N+1 queries trong for-loop | Use batch query, JOIN FETCH |
| **Not Using Connection Pool** | Mỗi request tạo connection mới | Configure HikariCP pool size properly |
| **Missing Caching** | Query lặp lại không cache | Use @Cacheable for read-heavy data |
| **Large Payload** | Return quá nhiều data không cần | Paginate, filter fields, use GraphQL |
| **Not Using Index** | Full table scan cho common queries | Analyze queries, add composite indexes |
| **Synchronous File I/O** | Blocking I/O trong request thread | Use async, non-blocking I/O |
| **Memory Leak** | Cache không eviction | Set TTL, max size for caches |
| **Not Using Compression** | Large JSON responses not compressed | Enable gzip, use binary formats for large data |

### Naming Anti-Patterns

| Anti-Pattern | Description | How to Avoid |
|--------------|-------------|--------------|
| **Inconsistent Naming** | warehouseId vs warehouse_id vs wh_id | Follow conventions: camelCase=Java, snake_case=DB |
| **Single Letter Names** | i, x, y không mô tả gì | Use meaningful names: index, quantity, total |
| **Hungarian Notation** | strName, iCount trong Java | Let type system handle types |
| **Abbreviations Confusion** | cnt vs count vs number | Use full words or consistent abbreviations |
| **Boolean Naming** | isActive vs active vs enabled | Use positive, clear names: isActive, hasPermission |
| **Plural Confusion** | user vs users cho single entity | Use singular for single entity: /user/{id} |
| **Inconsistent Date Naming** | createdAt vs createDate vs timestamp | Standardize: createdAt, updatedAt for audit |

### Testing Anti-Patterns

| Anti-Pattern | Description | How to Avoid |
|--------------|-------------|--------------|
| **Test for Coverage** | Viết test chỉ để pass coverage metric | Write meaningful tests that verify behavior |
| **Brittle Selectors** | Test break when refactor UI | Use data-testid attributes, semantic selectors |
| **No Assertion** | Tests that only execute code | Always assert expected outcomes |
| **Shared Mutable State** | Tests affect each other | Each test = independent, clean up state |
| **Slow Tests** | Unit tests hitting real DB | Mock dependencies, use testcontainers |
| **Not Testing Error Cases** | Chỉ test happy path | Test invalid inputs, edge cases, exceptions |
| **Hardcoded Test Data** | Test phụ thuộc specific DB state | Use factories, builders, clean state |
| **Not Testing Business Rules** | Test mock data, không test FEFO/approval | Test actual business logic, not just service calls |
| **Skipping Integration Tests** | Chỉ có unit tests | Cover critical paths with integration tests |
| **Not Using Given-When-Then** | Tests không rõ structure | Follow arrange-act-assert pattern |

---


